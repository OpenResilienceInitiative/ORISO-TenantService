import importlib.util
from pathlib import Path
import unittest

DRIVER = Path(__file__).resolve().parents[1] / 'ci/scenario-c-account-policy-native.py'
spec = importlib.util.spec_from_file_location('policy_driver', DRIVER)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)


class PolicyReceiptControls(unittest.TestCase):
    def receipt(self):
        run = 'a' * 32
        return {'state': 'PASS', 'sourceUnchanged': True, 'runId': run,
                'observed': {'platformRuntimeRead': True},
                'counts': {'tests': 5, 'failures': 0, 'errors': 0, 'skipped': 0},
                'cases': [{'name': name, 'outcome': 'pass'} for name in driver.CASES],
                'cleanup': [
                    {'kind': 'network', 'name': 'oriso-policy-' + run[:12], 'confirmedAbsent': True, 'runtime': None},
                    {'kind': 'container', 'name': 'oriso-policy-kc-' + run[:12], 'confirmedAbsent': True,
                     'runtime': {'oomKilled': False, 'restartCount': 0}}],
                'processCleanup': {'processGroup': 106, 'confirmedAbsent': True},
                'compiledClasses': {role: {'fixture': {'fixture.class': 'f' * 64}, 'classpathSHA256': 'c' * 64}
                                    for role in ('tenant', 'user')}}

    def groups(self):
        return [{'processGroup': x, 'confirmedAbsent': True} for x in range(101, 106)]

    def test_complete_actual_native_receipt(self):
        self.assertTrue(driver.complete(self.receipt(), self.groups(), 0))

    def test_platform_native_read_must_be_an_explicit_true_observation(self):
        for observed in ({}, {"platformRuntimeRead": False}, {"platformRuntimeRead": 1},
                         {"platformRuntimeRead": "true"}):
            value = self.receipt()
            value["observed"] = observed
            self.assertFalse(driver.complete(value, self.groups(), 0))

    def test_safe_absence_does_not_supply_missing_runtime(self):
        value = self.receipt()
        value['cleanup'][1]['runtime'] = None
        self.assertFalse(driver.complete(value, self.groups(), 0))

    def test_unknown_boolean_restart_oom_and_actual_restart_refuse(self):
        for state in ({'oomKilled': False, 'restartCount': False},
                      {'oomKilled': None, 'restartCount': 0},
                      {'oomKilled': True, 'restartCount': 0},
                      {'oomKilled': False, 'restartCount': 1}):
            value = self.receipt()
            value['cleanup'][1]['runtime'] = state
            self.assertFalse(driver.complete(value, self.groups(), 0))

    def test_missing_duplicate_or_alive_process_group_refuse(self):
        for groups in (self.groups()[:-1], self.groups() + self.groups()[:1],
                       self.groups()[:-1] + [{'processGroup': 105, 'confirmedAbsent': False}]):
            self.assertFalse(driver.complete(self.receipt(), groups, 0))

    def test_actual_server_group_cannot_duplicate_a_finished_leader(self):
        value = self.receipt()
        value['processCleanup']['processGroup'] = 101
        self.assertFalse(driver.complete(value, self.groups(), 0))

    def test_skipped_missing_duplicated_setup_or_failed_cases_refuse(self):
        for change in ('skip', 'missing', 'duplicate', 'fixture', 'error'):
            value = self.receipt()
            if change == 'skip': value['counts']['skipped'] = 1
            if change == 'missing': value['cases'].pop()
            if change == 'duplicate': value['cases'][-1] = value['cases'][0]
            if change == 'fixture': value['state'] = 'FIXTURE_FAILURE'
            if change == 'error': value['counts']['errors'] = 1
            self.assertFalse(driver.complete(value, self.groups(), 0))

    def test_source_change_nonzero_exit_and_missing_classpath_refuse(self):
        value = self.receipt()
        value['sourceUnchanged'] = False
        self.assertFalse(driver.complete(value, self.groups(), 0))
        self.assertFalse(driver.complete(self.receipt(), self.groups(), 1))
        value = self.receipt()
        value['compiledClasses']['user'].pop('classpathSHA256')
        self.assertFalse(driver.complete(value, self.groups(), 0))


if __name__ == '__main__':
    unittest.main()
