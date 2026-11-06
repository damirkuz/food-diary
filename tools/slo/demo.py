"""Real outage/recovery demonstration with finally restoration of original replicas."""

import argparse
import datetime
import json
from pathlib import Path
import signal
import subprocess
import time
import urllib.parse

from run_load import start_load


VM = '/api/v1/namespaces/metrics/services/http:vmsingle-victoria-metrics-single-server:8428/proxy'


def kube(*args):
    return subprocess.check_output(['kubectl', '--request-timeout=20s', *args], timeout=30)


def query(expression):
    data = json.loads(kube('get', '--raw', VM + '/api/v1/query?' + urllib.parse.urlencode({'query': expression})))
    if data.get('status') != 'success':
        raise RuntimeError('VictoriaMetrics query failed')
    return [float(r['value'][1]) for r in data['data']['result']]


def scale(kind, name, replicas):
    kube('-n', 'food-diary-prod', 'scale', kind + '/' + name, '--replicas=' + str(replicas))


def restore_replicas(scale_fn, originals):
    errors = []
    for kind, name in [('statefulset', 'postgres'), ('deployment', 'food-diary')]:
        try:
            scale_fn(kind, name, originals[name])
        except (Exception, KeyboardInterrupt) as error:
            errors.append(f'{kind}/{name}: {type(error).__name__}')
    return errors


def wait_ready(kind, name):
    subprocess.run(['kubectl', '-n', 'food-diary-prod', 'rollout', 'status',
                    kind + '/' + name, '--timeout=300s'], check=True, timeout=310)


def wait_probe(after, timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        success = query('max(food_diary_probe_success{namespace="metrics"})')
        stamp = query('max(food_diary_probe_last_completed_timestamp_seconds{namespace="metrics"})')
        if success == [1.0] and stamp and stamp[0] > after:
            return
        time.sleep(5)
    raise RuntimeError('No fresh successful scenario after recovery')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--phase-seconds', type=int, default=75)
    parser.add_argument('--rate', type=float, default=2)
    parser.add_argument('--with-lookup', action='store_true', help='Also call the real nutrition provider/cache')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    if args.phase_seconds < 60:
        parser.error('phase length must be at least 60 seconds for minute indicators')
    originals = {name: json.loads(kube('-n', 'food-diary-prod', 'get', kind, name, '-o', 'json'))['spec']['replicas']
                 for kind, name in [('statefulset', 'postgres'), ('deployment', 'food-diary')]}
    if not all(count > 0 for count in originals.values()):
        raise SystemExit('Start the application and PostgreSQL before running the demonstration')
    wait_probe(time.time() - 30)
    output = args.output or Path('docs/experiments/2026-11-06-slo') / (
        'demo-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S') + '.jsonl')
    output.parent.mkdir(parents=True, exist_ok=True)
    stream = output.open('x')
    jobs = []

    def event(kind, **data):
        row = {'time': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'event': kind, **data}
        line = json.dumps(row)
        print(line, flush=True)
        stream.write(line + '\n')
        stream.flush()

    def phase(name):
        event('phase', name=name)
        end = time.monotonic() + args.phase_seconds
        while time.monotonic() < end:
            snapshot = {}
            for key, expression in {
                'probe_success': 'max(food_diary_probe_success{namespace="metrics"})',
                'probe_age': 'time() - max(food_diary_probe_last_completed_timestamp_seconds{namespace="metrics"})',
                'pg_up': 'min(pg_up{namespace="food-diary-prod"})',
            }.items():
                snapshot[key] = query(expression)
            event('sample', phase=name, values=snapshot)
            time.sleep(min(15, max(0, end - time.monotonic())))

    def interrupt(*_):
        raise KeyboardInterrupt

    signal.signal(signal.SIGTERM, interrupt)
    try:
        event('start', original_replicas=originals)
        jobs.append(start_load(args.rate, args.phase_seconds * 5 + 600, 4))
        if args.with_lookup:
            jobs.append(start_load(0.1, args.phase_seconds * 5 + 600, 1, 'lookup'))
        event('load_started', jobs=jobs)
        phase('baseline')
        scale('statefulset', 'postgres', 0)
        phase('postgres_down')
        restored_at = time.time()
        scale('statefulset', 'postgres', originals['postgres'])
        wait_ready('statefulset', 'postgres')
        wait_probe(restored_at)
        phase('postgres_recovered')
        scale('deployment', 'food-diary', 0)
        phase('application_down')
        restored_at = time.time()
        scale('deployment', 'food-diary', originals['food-diary'])
        wait_ready('deployment', 'food-diary')
        wait_probe(restored_at)
        phase('application_recovered')
        event('completed')
    finally:
        # Once shutdown has started, a repeated Ctrl-C/SIGTERM must not skip restoration.
        signal.signal(signal.SIGINT, signal.SIG_IGN)
        signal.signal(signal.SIGTERM, signal.SIG_IGN)
        errors = restore_replicas(scale, originals)
        event('restore_requested', errors=errors)
        for job in jobs:
            # Let the generator print its final counters before deleting its Pod.
            subprocess.run(['kubectl', '-n', 'metrics', 'exec', 'job/' + job,
                            '--', '/bin/sh', '-c', 'kill -TERM 1'],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30, check=False)
            subprocess.run(['kubectl', '-n', 'metrics', 'wait', '--for=condition=complete',
                            'job/' + job, '--timeout=30s'],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=35, check=False)
            logs = subprocess.run(['kubectl', '-n', 'metrics', 'logs', 'job/' + job], capture_output=True,
                                  text=True, timeout=30)
            (output.parent / (job + '.log')).write_text(logs.stdout)
            subprocess.run(['kubectl', '-n', 'metrics', 'delete', 'job', job, '--wait=false'],
                           stdout=subprocess.DEVNULL, check=False, timeout=30)
        stream.close()
        if errors:
            raise RuntimeError('RESTORE FAILED: ' + '; '.join(errors))
    wait_ready('statefulset', 'postgres')
    wait_ready('deployment', 'food-diary')
    print('Restored original replicas. Evidence:', output, flush=True)


if __name__ == '__main__':
    main()
