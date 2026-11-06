"""Start bounded synthetic load as a Kubernetes Job, reusing the prober Secret."""

import argparse
import datetime
import json
import math
import subprocess
import uuid


def start_load(rate=2, duration=120, workers=4, scenario='diary', query='banana'):
    if (not all(math.isfinite(v) and v > 0 for v in (rate, duration))
            or not 1 <= workers <= 32 or scenario not in ('diary', 'lookup')):
        raise ValueError('require positive finite rate/duration, 1..32 workers and a supported scenario')
    deployment = json.loads(subprocess.check_output(
        ['kubectl', '-n', 'metrics', 'get', 'deployment', 'food-diary-prober', '-o', 'json'], timeout=30))
    original = deployment['spec']['template']['spec']['containers'][0]
    name = 'food-diary-load-' + datetime.datetime.now().strftime('%H%M%S') + '-' + uuid.uuid4().hex[:4]
    container = {key: original[key] for key in ('image', 'imagePullPolicy', 'env', 'envFrom', 'securityContext')}
    container.update({'name': 'load', 'command': ['python', 'load.py'], 'args': [
        '--rate', str(rate), '--duration', str(duration), '--workers', str(workers),
        '--scenario', scenario, '--query', query],
        'resources': {'requests': {'cpu': '50m', 'memory': '32Mi'},
                      'limits': {'cpu': '1', 'memory': '128Mi'}}})
    job = {'apiVersion': 'batch/v1', 'kind': 'Job', 'metadata': {'name': name, 'namespace': 'metrics'},
           'spec': {'backoffLimit': 0, 'ttlSecondsAfterFinished': 3600,
                    'activeDeadlineSeconds': math.ceil(duration + 90), 'template': {
                        'metadata': {'labels': {'app': 'food-diary-load'}},
                        'spec': {'restartPolicy': 'Never', 'automountServiceAccountToken': False,
                                 'securityContext': deployment['spec']['template']['spec']['securityContext'],
                                 'containers': [container]}}}}
    subprocess.run(['kubectl', 'create', '-f', '-'], input=json.dumps(job), text=True,
                   stdout=subprocess.DEVNULL, check=True, timeout=30)
    return name


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--rate', type=float, default=2)
    parser.add_argument('--duration', type=float, default=120)
    parser.add_argument('--workers', type=int, default=4)
    parser.add_argument('--scenario', choices=['diary', 'lookup'], default='diary')
    parser.add_argument('--query', default='banana')
    args = parser.parse_args()
    name = start_load(args.rate, args.duration, args.workers, args.scenario, args.query)
    print(name)
    print(f'kubectl -n metrics logs -f job/{name}')
