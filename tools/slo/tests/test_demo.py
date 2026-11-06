import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from demo import restore_replicas


class DemoTest(unittest.TestCase):
    def test_restore_tries_both_components_even_when_first_restore_fails(self):
        calls = []

        def scale(kind, name, count):
            calls.append((kind, name, count))
            if name == 'postgres':
                raise RuntimeError('temporary API failure')

        errors = restore_replicas(scale, {'postgres': 1, 'food-diary': 3})
        self.assertEqual(calls, [('statefulset', 'postgres', 1), ('deployment', 'food-diary', 3)])
        self.assertEqual(len(errors), 1)

    def test_restore_preserves_original_counts(self):
        calls = []
        errors = restore_replicas(lambda *args: calls.append(args), {'postgres': 1, 'food-diary': 2})
        self.assertEqual(calls[-1], ('deployment', 'food-diary', 2))
        self.assertEqual(errors, [])

    def test_interrupt_during_cleanup_does_not_skip_second_component(self):
        calls = []

        def scale(kind, name, count):
            calls.append(name)
            if name == 'postgres':
                raise KeyboardInterrupt

        errors = restore_replicas(scale, {'postgres': 1, 'food-diary': 3})
        self.assertEqual(calls, ['postgres', 'food-diary'])
        self.assertEqual(len(errors), 1)


if __name__ == '__main__':
    unittest.main()
