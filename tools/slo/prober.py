"""Scenario prober; one fresh login and read-only diary check per interval."""

import json
import math
import os
import signal
import threading
import time

from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram, start_http_server
from client import DiaryClient, ScenarioError


class Probe:
    def __init__(self, client, registry, timeout):
        self.client, self.timeout = client, timeout
        self.success = Gauge('food_diary_probe_success', 'Last completed scenario succeeded', registry=registry)
        self.completed = Gauge('food_diary_probe_last_completed_timestamp_seconds',
                               'Unix time of the last completed scenario', registry=registry)
        self.checks = Counter('food_diary_probe_checks', 'Completed scenarios', ['result'], registry=registry)
        self.duration = Histogram('food_diary_probe_duration_seconds', 'Whole scenario duration',
                                  buckets=(0.1, 0.25, 0.5, 1, 2, 5, 10), registry=registry)
        for result in ('success', 'failure'):
            self.checks.labels(result)

    def run_once(self):
        started = time.monotonic()
        success, error = False, None
        try:
            self.client.check_diary(started + self.timeout)
            success = True
        except ScenarioError as failure:
            error = str(failure)
        except Exception:
            # Keep the prober observable even on an unexpected client failure.
            error = 'internal'
        duration = time.monotonic() - started
        self.success.set(int(success))
        self.checks.labels('success' if success else 'failure').inc()
        self.duration.observe(duration)
        self.completed.set(time.time())
        print(json.dumps({'event': 'probe_completed', 'success': success,
                          'duration_seconds': round(duration, 3), 'error': error}), flush=True)
        return success


def main():
    interval = float(os.getenv('PROBE_INTERVAL_SECONDS', '15'))
    timeout = float(os.getenv('PROBE_TIMEOUT_SECONDS', '5'))
    if not (math.isfinite(interval) and math.isfinite(timeout) and 0 < timeout < interval):
        raise ValueError('require finite 0 < timeout < interval')
    client = DiaryClient(os.environ['PROBE_BASE_URL'], os.environ['PROBE_EMAIL'],
                         os.environ['PROBE_PASSWORD'], os.getenv('PROBE_HOST'))
    registry = CollectorRegistry()
    probe = Probe(client, registry, timeout)
    start_http_server(int(os.getenv('PROBE_METRICS_PORT', '8000')), registry=registry)
    stop = threading.Event()
    for signum in (signal.SIGTERM, signal.SIGINT):
        signal.signal(signum, lambda *_: stop.set())
    while not stop.is_set():
        started = time.monotonic()
        probe.run_once()
        stop.wait(max(0, interval - (time.monotonic() - started)))


if __name__ == '__main__':
    main()
