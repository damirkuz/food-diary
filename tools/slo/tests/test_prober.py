import sys
import time
import unittest
from pathlib import Path
from prometheus_client import CollectorRegistry

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prober import Probe
from client import ScenarioError


class ProbeTest(unittest.TestCase):
    def test_initial_state_is_unknown_not_success(self):
        registry = CollectorRegistry()
        Probe(None, registry, 1)
        self.assertEqual(registry.get_sample_value('food_diary_probe_last_completed_timestamp_seconds'), 0)
        self.assertEqual(registry.get_sample_value('food_diary_probe_success'), 0)

    def test_failure_and_recovery_update_actual_results(self):
        class Client:
            failed = True

            def check_diary(self, deadline):
                if self.failed:
                    raise ScenarioError('diary_http')

        client = Client()
        registry = CollectorRegistry()
        probe = Probe(client, registry, 1)
        self.assertFalse(probe.run_once())
        self.assertEqual(registry.get_sample_value('food_diary_probe_checks_total', {'result': 'failure'}), 1)
        self.assertEqual(registry.get_sample_value('food_diary_probe_success'), 0)
        client.failed = False
        self.assertTrue(probe.run_once())
        self.assertEqual(registry.get_sample_value('food_diary_probe_checks_total', {'result': 'success'}), 1)
        self.assertEqual(registry.get_sample_value('food_diary_probe_success'), 1)
        stamp = registry.get_sample_value('food_diary_probe_last_completed_timestamp_seconds')
        time.sleep(0.01)
        self.assertEqual(registry.get_sample_value('food_diary_probe_last_completed_timestamp_seconds'), stamp,
                         'Scraping must not create a new check')


if __name__ == '__main__':
    unittest.main()
