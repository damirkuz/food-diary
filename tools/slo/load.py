"""Bounded, paced synthetic API traffic; no diary/product writes."""

import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, wait, FIRST_COMPLETED
import json
import math
import os
import signal
import threading
import time

from client import DiaryClient, ScenarioError


def run(base_url, host, email, password, rate, duration, workers, timeout, scenario, query):
    if not all(math.isfinite(v) and v > 0 for v in (rate, duration, timeout)) or workers < 1:
        raise ValueError('rate, duration, workers and timeout must be positive and finite')
    local = threading.local()
    counts = Counter()
    stop = threading.Event()
    for signum in (signal.SIGINT, signal.SIGTERM):
        signal.signal(signum, lambda *_: stop.set())

    def request():
        started = time.monotonic()
        try:
            if not hasattr(local, 'client'):
                local.client = DiaryClient(base_url, email, password, host)
            deadline = started + timeout
            if not local.client.cookies:
                local.client.login(deadline)
            if scenario == 'diary':
                local.client.read_diary(deadline)
            else:
                local.client.lookup(query, deadline)
            return 'success'
        except ScenarioError:
            # A fresh login on the next iteration recovers expired sessions.
            local.client.cookies.clear()
            return 'failure'

    started = time.monotonic()
    next_request = started
    pending = set()
    with ThreadPoolExecutor(max_workers=workers) as pool:
        while not stop.is_set() and time.monotonic() < started + duration:
            done = {f for f in pending if f.done()}
            for future in done:
                counts[future.result()] += 1
            pending -= done
            delay = next_request - time.monotonic()
            if delay > 0:
                stop.wait(min(delay, max(0, started + duration - time.monotonic())))
            if stop.is_set() or time.monotonic() >= started + duration:
                break
            if len(pending) >= workers * 2:
                counts['not_started_capacity'] += 1
            else:
                pending.add(pool.submit(request))
            # No burst of catch-up requests if the generator itself was delayed.
            next_request = max(next_request + 1 / rate, time.monotonic())
        for future in pending:
            counts[future.result()] += 1
    result = {'scenario': scenario, 'duration_seconds': round(time.monotonic() - started, 3),
              'requested_rate': rate, 'workers': workers, **counts}
    print(json.dumps(result), flush=True)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default=os.getenv('PROBE_BASE_URL'))
    parser.add_argument('--host', default=os.getenv('PROBE_HOST'))
    parser.add_argument('--rate', type=float, default=2)
    parser.add_argument('--duration', type=float, default=120)
    parser.add_argument('--workers', type=int, default=4)
    parser.add_argument('--timeout', type=float, default=5)
    parser.add_argument('--scenario', choices=['diary', 'lookup'], default='diary')
    parser.add_argument('--query', default='banana')
    args = parser.parse_args()
    if not args.base_url:
        parser.error('--base-url or PROBE_BASE_URL is required')
    email, password = os.environ['PROBE_EMAIL'], os.environ['PROBE_PASSWORD']
    run(args.base_url, args.host, email, password, args.rate, args.duration,
        args.workers, args.timeout, args.scenario, args.query)


if __name__ == '__main__':
    main()
