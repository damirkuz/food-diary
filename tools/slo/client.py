"""Small same-origin client for the public form-login and diary API contracts."""

import datetime
import http.client
import http.cookies
import json
import math
import socket
import queue
import threading
import time
from html.parser import HTMLParser
from urllib.parse import urlencode, urlsplit


class ScenarioError(Exception):
    """Only a bounded failure code: never a password, cookie or response body."""


class CsrfParser(HTMLParser):
    token = None

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'input' and attrs.get('name') == '_csrf':
            self.token = attrs.get('value')


def remaining(deadline):
    value = deadline - time.monotonic()
    if value <= 0:
        raise ScenarioError('timeout')
    return value


class DiaryClient:
    def __init__(self, base_url, email, password, host=None):
        parsed = urlsplit(base_url)
        if (parsed.scheme not in ('http', 'https') or not parsed.hostname
                or parsed.username or parsed.password or parsed.query or parsed.fragment
                or parsed.path not in ('', '/')):
            raise ValueError('base URL must be an http(s) origin without credentials')
        if not email or not password:
            raise ValueError('technical account credentials are required')
        self.origin = parsed
        self.email, self.password, self.host = email, password, host
        self.cookies = {}
        self._worker = None
        self._active_socket = None

    def request(self, method, path, deadline, form=None):
        connection_type = (http.client.HTTPSConnection if self.origin.scheme == 'https'
                           else http.client.HTTPConnection)
        conn = connection_type(self.origin.hostname, self.origin.port, timeout=remaining(deadline))
        headers = {'User-Agent': 'food-diary-synthetic/1.0', 'Accept': 'application/json, text/html'}
        if self.host:
            headers['Host'] = self.host
        if self.cookies:
            headers['Cookie'] = '; '.join(f'{k}={v}' for k, v in self.cookies.items())
        body = None
        response = None
        if form is not None:
            body = urlencode(form).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
        try:
            conn.connect()
            conn.sock.settimeout(remaining(deadline))
            conn.request(method, path, body=body, headers=headers)
            sock = conn.sock
            self._active_socket = sock
            sock.settimeout(remaining(deadline))
            response = conn.getresponse()
            data = bytearray()
            while not response.isclosed():
                sock.settimeout(remaining(deadline))
                chunk = response.read1(65536)
                if not chunk:
                    break
                data.extend(chunk)
                if len(data) > 1024 * 1024:
                    raise ScenarioError('response_too_large')
            remaining(deadline)
            for key, value in response.getheaders():
                if key.lower() == 'set-cookie':
                    cookie = http.cookies.SimpleCookie()
                    cookie.load(value)
                    for name, item in cookie.items():
                        if item['max-age'] == '0':
                            self.cookies.pop(name, None)
                        else:
                            self.cookies[name] = item.value
            return response.status, dict((k.lower(), v) for k, v in response.getheaders()), bytes(data)
        except (socket.timeout, TimeoutError) as error:
            raise ScenarioError('timeout') from error
        except (OSError, http.client.HTTPException) as error:
            raise ScenarioError('network') from error
        finally:
            if response is not None:
                response.close()
            conn.close()
            self._active_socket = None

    def csrf(self, path, deadline):
        status, _, body = self.request('GET', path, deadline)
        if status != 200:
            raise ScenarioError('login_page_http')
        parser = CsrfParser()
        parser.feed(body.decode('utf-8', errors='replace'))
        if not parser.token:
            raise ScenarioError('csrf_missing')
        return parser.token

    def login(self, deadline):
        self.cookies.clear()
        token = self.csrf('/login', deadline)
        status, headers, _ = self.request('POST', '/login', deadline,
                                          {'email': self.email, 'password': self.password, '_csrf': token})
        location = urlsplit(headers.get('location', '')).path
        if status not in (302, 303) or location != '/profile':
            raise ScenarioError('login_failed')

    def read_diary(self, deadline):
        day = datetime.date.today().isoformat()
        status, headers, body = self.request('GET', '/api/diary?' + urlencode({'date': day}), deadline)
        if status != 200:
            raise ScenarioError('diary_http')
        if 'application/json' not in headers.get('content-type', ''):
            raise ScenarioError('diary_content_type')
        try:
            data = json.loads(body)
            valid = (isinstance(data, dict) and data.get('date') == day
                     and isinstance(data.get('entries'), list)
                     and isinstance(data.get('totals'), dict)
                     and all(isinstance(data['totals'].get(key), (int, float))
                             and not isinstance(data['totals'][key], bool)
                             and math.isfinite(data['totals'][key])
                             for key in ('calories', 'proteins', 'fats', 'carbohydrates')))
        except (ValueError, TypeError):
            valid = False
        if not valid:
            raise ScenarioError('diary_contract')

    def check_diary(self, deadline):
        # DNS and HTTP header parsing do not honor an absolute socket deadline.
        # At most one daemon worker per client; never queue checks behind a stuck resolver.
        if self._worker is not None and self._worker.is_alive():
            raise ScenarioError('previous_check_busy')
        results = queue.Queue(maxsize=1)

        def scenario():
            try:
                self.login(deadline)
                self.read_diary(deadline)
                results.put(None)
            except Exception as error:
                results.put(error)

        self._worker = threading.Thread(target=scenario, daemon=True)
        self._worker.start()
        try:
            result = results.get(timeout=remaining(deadline))
        except queue.Empty:
            if self._active_socket is not None:
                try:
                    self._active_socket.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass
            raise ScenarioError('timeout') from None
        self._worker.join(timeout=remaining(deadline))
        if result is not None:
            raise result

    def lookup(self, name, deadline):
        status, _, _ = self.request('GET', '/api/products/nutrition-lookup?' +
                                    urlencode({'name': name}), deadline)
        if status not in (200, 404):
            raise ScenarioError('lookup_http')
