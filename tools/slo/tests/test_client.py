import datetime
import http.server
import json
import sys
import threading
import time
import unittest
from pathlib import Path
from urllib.parse import parse_qs, urlparse

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from client import DiaryClient, ScenarioError


class App(http.server.BaseHTTPRequestHandler):
    mode = 'healthy'
    calls = []

    def log_message(self, *_):
        pass

    def reply(self, status, body=b'', **headers):
        self.send_response(status)
        self.send_header('Content-Length', str(len(body)))
        for key, value in headers.items():
            self.send_header(key.replace('_', '-'), value)
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def do_GET(self):
        App.calls.append(('GET', self.path))
        if App.mode == 'slow':
            time.sleep(0.12)
        if self.path == '/login':
            if App.mode == 'slow-headers':
                try:
                    self.wfile.write(b'HTTP/1.0 200 OK\r\nX-Slow: ')
                    self.wfile.flush()
                    for _ in range(15):
                        self.wfile.write(b'a')
                        self.wfile.flush()
                        time.sleep(0.03)
                    self.wfile.write(b'\r\nContent-Length: 0\r\n\r\n')
                except (BrokenPipeError, ConnectionResetError):
                    pass
                return
            self.reply(200, b'<input value="test-token" name="_csrf" type="hidden">',
                       Set_Cookie='SESSION=anonymous; Path=/', Content_Type='text/html')
        elif self.path.startswith('/api/diary'):
            if 'SESSION=logged-in' not in self.headers.get('Cookie', ''):
                self.reply(401)
            elif App.mode == 'html':
                self.reply(200, b'<html>login</html>', Content_Type='text/html')
            elif App.mode == 'wrong-date':
                self.reply(200, b'{"date":"1900-01-01","entries":[],"totals":{}}', Content_Type='application/json')
            elif App.mode == 'server-error':
                self.reply(500)
            else:
                day = parse_qs(urlparse(self.path).query)['date'][0]
                data = {'date': day, 'entries': [], 'totals': {
                    'calories': 0, 'proteins': 0, 'fats': 0, 'carbohydrates': 0},
                    'new_optional_field': 'allowed'}
                self.reply(200, json.dumps(data).encode(), Content_Type='application/json')
        else:
            self.reply(404)

    def do_POST(self):
        App.calls.append(('POST', self.path))
        data = parse_qs(self.rfile.read(int(self.headers['Content-Length'])).decode())
        if App.mode == 'slow':
            time.sleep(0.12)
        valid = (data.get('_csrf') == ['test-token'] and data.get('email') == ['probe@test.local']
                 and data.get('password') == ['secret']
                 and 'SESSION=anonymous' in self.headers.get('Cookie', ''))
        self.reply(302, Location='/profile' if valid else '/login?error',
                   Set_Cookie='SESSION=logged-in; Path=/' if valid else 'SESSION=anonymous; Path=/')


class ClientTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), App)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.url = f'http://127.0.0.1:{cls.server.server_port}'

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def setUp(self):
        App.mode = 'healthy'
        App.calls = []
        self.client = DiaryClient(self.url, 'probe@test.local', 'secret')

    def test_real_csrf_cookie_login_and_read_only_diary(self):
        self.client.check_diary(time.monotonic() + 1)
        self.assertEqual([method for method, _ in App.calls], ['GET', 'POST', 'GET'])
        self.assertEqual([path for method, path in App.calls if method == 'POST'], ['/login'])

    def test_bad_password_is_not_successful_login_redirect(self):
        bad = DiaryClient(self.url, 'probe@test.local', 'wrong-password')
        with self.assertRaises(ScenarioError):
            bad.check_diary(time.monotonic() + 1)

    def test_html_200_does_not_count_as_success(self):
        App.mode = 'html'
        with self.assertRaises(ScenarioError):
            self.client.check_diary(time.monotonic() + 1)

    def test_wrong_diary_date_is_rejected(self):
        App.mode = 'wrong-date'
        with self.assertRaises(ScenarioError):
            self.client.check_diary(time.monotonic() + 1)

    def test_deadline_is_for_the_entire_scenario(self):
        App.mode = 'slow'
        started = time.monotonic()
        with self.assertRaises(ScenarioError):
            self.client.check_diary(started + 0.2)
        self.assertLess(time.monotonic() - started, 0.32)

    def test_server_failure_then_recovery(self):
        App.mode = 'server-error'
        with self.assertRaises(ScenarioError):
            self.client.check_diary(time.monotonic() + 1)
        App.mode = 'healthy'
        self.client.check_diary(time.monotonic() + 1)

    def test_slow_headers_cannot_extend_the_absolute_deadline(self):
        App.mode = 'slow-headers'
        started = time.monotonic()
        with self.assertRaises(ScenarioError):
            self.client.check_diary(started + 0.12)
        self.assertLess(time.monotonic() - started, 0.3)

    def test_slow_dns_cannot_extend_the_absolute_deadline(self):
        import socket
        from unittest.mock import patch
        original = socket.getaddrinfo

        def slow(*args, **kwargs):
            time.sleep(0.45)
            return original(*args, **kwargs)

        started = time.monotonic()
        with patch('socket.getaddrinfo', side_effect=slow):
            with self.assertRaises(ScenarioError):
                self.client.check_diary(started + 0.1)
        self.assertLess(time.monotonic() - started, 0.3)


if __name__ == '__main__':
    unittest.main()
