"""Read-only check of VictoriaMetrics' built-in Kubernetes scraping."""

import argparse
import datetime
import json
import re
import subprocess
import sys
import time
import urllib.parse


VM_PROXY = (
    '/api/v1/namespaces/metrics/services/'
    'http:vmsingle-victoria-metrics-single-server:8428/proxy'
)


def kube(*args):
    return json.loads(subprocess.check_output(
        ['kubectl', '--request-timeout=20s', *args], timeout=30,
    ))


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def query(expression):
    data = kube('get', '--raw', VM_PROXY + '/api/v1/query?' +
                urllib.parse.urlencode({'query': expression}))
    require(data.get('status') == 'success', f'Query failed: {data}')
    return data['data']['result']


def fresh(selector, minimum, started_at):
    rows = query(selector)
    require(len(rows) >= minimum, f'Missing {selector}: {len(rows)} series')
    age = query(f'max(time() - timestamp({selector}))')
    require(age and float(age[0]['value'][1]) < 60, f'Stale {selector}: {age}')
    oldest = query(f'min(timestamp({selector}))')
    require(oldest and float(oldest[0]['value'][1]) > started_at,
            f'{selector} has samples from before VM startup')
    return rows


def check():
    values = json.loads(subprocess.check_output(
        ['helm', 'get', 'values', 'vmsingle', '-n', 'metrics', '-o', 'json'],
        timeout=30,
    ))
    require(values.get('server', {}).get('scrape', {}).get('enabled') is True,
            'VM built-in scraping is disabled')
    vm = kube('-n', 'metrics', 'get', 'statefulset',
              'vmsingle-victoria-metrics-single-server', '-o', 'json')
    require(vm['spec']['replicas'] == 1 and vm['status'].get('readyReplicas') == 1,
            'Expected one Ready VictoriaMetrics instance')
    require(any(a.startswith(('-promscrape.config=', '--promscrape.config='))
                for c in vm['spec']['template']['spec']['containers']
                for a in c.get('args', [])), 'VM has no scrape config argument')

    collectors = kube('-n', 'metrics', 'get', 'pods', '-l',
                      'app.kubernetes.io/instance=otel-metrics', '-o', 'json')['items']
    require(not any(p['status'].get('phase') not in ('Succeeded', 'Failed')
                    for p in collectors), 'OTel metrics Pod still exists')
    deployments = kube('-n', 'metrics', 'get', 'deployments', '-o', 'json')['items']
    require(not any(d['metadata']['name'] == 'otel-metrics'
                    and d['spec'].get('replicas', 1) > 0 for d in deployments),
            'OTel metrics Deployment still owns scraping')

    vm_pods = kube('-n', 'metrics', 'get', 'pods', '-l',
                   'app.kubernetes.io/instance=vmsingle', '-o', 'json')['items']
    running = [p for p in vm_pods if not p['metadata'].get('deletionTimestamp')
               and p['status'].get('phase') == 'Running']
    require(len(running) == 1, 'Expected one running VM Pod')
    statuses = running[0]['status']['containerStatuses']
    started_at = max(datetime.datetime.fromisoformat(
        s['state']['running']['startedAt'].replace('Z', '+00:00')
    ).timestamp() for s in statuses)

    targets = kube('get', '--raw', VM_PROXY + '/api/v1/targets')['data']['activeTargets']
    require(targets, 'VictoriaMetrics has no active targets')
    failed = [(t.get('labels'), t.get('lastError'))
              for t in targets if t.get('health') != 'up']
    require(not failed, f'Target failures: {failed}')
    jobs = {t['labels']['job'] for t in targets}
    expected_jobs = {'victoriametrics', 'kubernetes-pods', 'kubernetes-nodes',
                     'kubernetes-nodes-cadvisor', 'kubernetes-apiservers'}
    require(expected_jobs <= jobs, f'Missing jobs: {expected_jobs - jobs}')
    identities = [(t['labels']['job'], t['labels']['instance']) for t in targets]
    require(len(set(identities)) == len(identities), 'Duplicate target identities')

    pods = kube('-n', 'food-diary-prod', 'get', 'pods', '-o', 'json')['items']
    annotated = {p['metadata']['name']: p for p in pods
                 if not p['metadata'].get('deletionTimestamp')
                 and p['metadata'].get('annotations', {}).get('prometheus.io/scrape') == 'true'}
    require(annotated, 'No annotated prod Pods')
    prod_targets = [t for t in targets if t['labels'].get('job') == 'kubernetes-pods'
                    and t['labels'].get('namespace') == 'food-diary-prod']
    require({t['labels']['pod'] for t in prod_targets} == set(annotated),
            'Discovered prod targets do not match annotated Pods')
    require(len(prod_targets) == len(annotated), 'Multiple scrape ports per prod Pod')
    for target in prod_targets:
        annotations = annotated[target['labels']['pod']]['metadata']['annotations']
        endpoint = urllib.parse.urlparse(target['scrapeUrl'])
        require(endpoint.port == int(annotations['prometheus.io/port'])
                and endpoint.path == annotations['prometheus.io/path'],
                f'Wrong scrape endpoint: {target["scrapeUrl"]}')

    app_pods = {name for name, p in annotated.items()
                if p['metadata'].get('labels', {}).get('app') == 'food-diary'}
    require(app_pods, 'No application Pods found')
    # Replaced Pods can remain inside VM's lookback window. Verify current SD owners.
    pod_pattern = json.dumps('|'.join(re.escape(name) for name in sorted(app_pods)))
    instances = {t['labels']['instance'] for t in prod_targets if t['labels']['pod'] in app_pods}
    instance_pattern = json.dumps('|'.join(re.escape(value) for value in sorted(instances)))
    app_selector = ('namespace="food-diary-prod",job="kubernetes-pods",'
                    f'pod=~{pod_pattern},instance=~{instance_pattern}')
    for metric, per_pod in [
        ('food_diary_registrations_total', 1),
        ('food_diary_products_creations_total', 1),
        ('food_diary_entries_creations_total', 1),
        ('food_diary_nutrition_lookups_total', 5),
        ('jvm_memory_used_bytes', 1), ('http_server_requests_seconds_count', 1),
    ]:
        rows = fresh(f'{metric}{{{app_selector}}}',
                     len(app_pods) * per_pod, started_at)
        require({r['metric'].get('pod') for r in rows} == app_pods,
                f'Missing or unexpected application Pods in {metric}')
        require(all('job' in r['metric'] and 'instance' in r['metric'] for r in rows),
                f'Missing target labels in {metric}')
        print(f'PASS {metric}: {len(rows)} recent series')

    postgres_target = next(t for t in prod_targets if t['labels']['pod'] == 'postgres-0')
    postgres_instance = json.dumps(postgres_target['labels']['instance'])
    postgres = fresh('pg_up{namespace="food-diary-prod",job="kubernetes-pods",'
                     f'instance={postgres_instance}' + '}', 1, started_at)
    require(all(float(r['value'][1]) == 1 for r in postgres),
            'Exporter cannot connect to PostgreSQL')
    cad = fresh('container_memory_working_set_bytes{namespace="food-diary-prod",'
                f'container="food-diary",pod=~{pod_pattern}' + '}', len(app_pods), started_at)
    require({r['metric'].get('pod') for r in cad} == app_pods,
            'Missing application cAdvisor series')
    require(all(r['metric'].get('pod_name') == r['metric'].get('pod')
                and r['metric'].get('container_name') == 'food-diary' for r in cad),
            'Missing or incorrect cAdvisor aliases')
    print(f'PASS pg_up=1; cAdvisor aliases; {len(targets)} UP targets; '
          'VM owns scraping; OTel metrics is stopped')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--wait', type=int, default=0, help='Retry budget in seconds')
    args = parser.parse_args()
    deadline = time.monotonic() + args.wait
    while True:
        try:
            check()
            break
        except (AssertionError, subprocess.SubprocessError, KeyError, ValueError,
                OSError) as error:
            if time.monotonic() >= deadline:
                print(f'FAIL {error}', file=sys.stderr)
                sys.exit(1)
            print(f'WAIT {error}', file=sys.stderr, flush=True)
            time.sleep(5)
