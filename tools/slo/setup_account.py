"""Create one ordinary technical account and a Kubernetes Secret, without printing credentials."""

import argparse
import base64
import json
import secrets
import subprocess
import time

from client import DiaryClient, ScenarioError


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:18080')
    parser.add_argument('--host', default='food-diary.local')
    parser.add_argument('--namespace', default='metrics')
    args = parser.parse_args()
    raw = subprocess.run(['kubectl', '-n', args.namespace, 'get', 'secret', 'food-diary-prober',
                          '--ignore-not-found', '-o', 'json'], capture_output=True, text=True, check=True, timeout=30)
    if raw.stdout.strip():
        secret = json.loads(raw.stdout)
        email, password = (base64.b64decode(secret['data'][key]).decode()
                           for key in ['PROBE_EMAIL', 'PROBE_PASSWORD'])
    else:
        email, password = 'slo-probe@food-diary.local', secrets.token_urlsafe(32)
    client = DiaryClient(args.base_url, email, password, args.host)
    try:
        client.check_diary(time.monotonic() + 10)
        print('Technical account already works; credentials preserved')
    except ScenarioError as error:
        if str(error) != 'login_failed':
            raise SystemExit('Application is unavailable; account was not changed') from None
        deadline = time.monotonic() + 10
        token = client.csrf('/register', deadline)
        status, _, _ = client.request('POST', '/register', deadline,
                                      {'email': email, 'password': password, '_csrf': token})
        if status not in (302, 303):
            raise SystemExit('Registration failed; existing accounts were not overwritten')
        client.check_diary(time.monotonic() + 10)
        print('Created ordinary technical account; read-only diary scenario passed')
    document = {'apiVersion': 'v1', 'kind': 'Secret', 'metadata': {
        'name': 'food-diary-prober', 'namespace': args.namespace}, 'type': 'Opaque',
        'data': {key: base64.b64encode(value.encode()).decode() for key, value in {
            'PROBE_EMAIL': email, 'PROBE_PASSWORD': password}.items()}}
    subprocess.run(['kubectl', 'apply', '-f', '-'], input=json.dumps(document), text=True,
                   stdout=subprocess.DEVNULL, check=True, timeout=30)
    print('Secret food-diary-prober is ready; no credentials written to project files')


if __name__ == '__main__':
    main()
