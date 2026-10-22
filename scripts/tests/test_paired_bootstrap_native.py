import copy
import importlib.util
import hashlib
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).parents[1]
SPEC = importlib.util.spec_from_file_location('paired_native_driver', ROOT / 'ci/scenario-c-context-native.py')
driver = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(driver)
OBS = importlib.util.spec_from_file_location('paired_native_observation', ROOT / 'test-bootstrap-context-roundtrip.py')
helper = importlib.util.module_from_spec(OBS)
OBS.loader.exec_module(helper)
POSITIVE = 'ownedTenantCreationUsesTheSharedV2ProofAtTheMaterializedDtoSendSeam'
PAIRED = 'aLaterTenantFailureRetiresOnlyItsIssuedAttemptAndReusesTheExactReservation'


class PairedNativeInventoryAndPrivacyControls(unittest.TestCase):
    def counts(self, number=2):
        return {'tests': number, 'failures': 0, 'errors': 0, 'skipped': 0}

    def cases(self):
        return [{'name': POSITIVE, 'outcome': 'pass'}, {'name': PAIRED, 'outcome': 'pass'}]

    def test_only_the_exact_original_and_paired_case_inventory_can_pass(self):
        self.assertTrue(driver.exact_native_inventory(self.counts(), self.cases(), 2))
        self.assertTrue(driver.exact_native_inventory(self.counts(1), self.cases()[:1], 1))
        red = self.cases()
        red[1]['outcome'] = 'failure'
        counts = self.counts()
        counts['failures'] = 1
        self.assertTrue(driver.exact_native_inventory(counts, red, 2))

    def test_missing_duplicated_foreign_cases_error_skip_and_boolean_counts_refuse(self):
        for cases in (self.cases()[:1], self.cases() + self.cases()[:1],
                      [self.cases()[0], self.cases()[0]],
                      [self.cases()[0], {'name': 'unowned', 'outcome': 'pass'}]):
            self.assertFalse(driver.exact_native_inventory(self.counts(), cases, 2))
        for key, value in (('tests', True), ('errors', 1), ('skipped', 1), ('failures', 1)):
            counts = self.counts()
            counts[key] = value
            self.assertFalse(driver.exact_native_inventory(counts, self.cases(), 2))
        for number in (0, 3, True, None):
            self.assertFalse(driver.exact_native_inventory(self.counts(), self.cases(), number))

    def test_malformed_xml_inventory_cannot_raise_or_gain_acceptance(self):
        for counts, cases in ((None, self.cases()), ([], self.cases()),
                              (self.counts(), None), (self.counts(), [None, {}]),
                              (self.counts(), [{'name': 7}, self.cases()[1]])):
            self.assertFalse(driver.exact_native_inventory(counts, cases, 2))

    def observation(self):
        return {'receiverStatus': 200, 'tenantStatus': 200,
                'receiverRequestObserved': True, 'allocationStatus': 'ASSIGNED'}

    def test_original_observation_remains_valid_and_unknown_or_private_values_refuse(self):
        value = self.observation()
        self.assertEqual(value, helper.safe_observation(value))
        for key, private in (('unexpected', 'synthetic-private-config'),
                             ('allocationStatus', 'https://synthetic-private-link'),
                             ('receiverStatus', 'eyJsynthetic.private.signature'),
                             ('receiverRequestObserved', 'synthetic-private-proof')):
            with self.subTest(key=key):
                changed = dict(value)
                changed[key] = private
                with self.assertRaises(helper.FixtureError) as error:
                    helper.safe_observation(changed)
                self.assertNotIn(private, str(error.exception))

    def test_unknown_nested_control_or_bearer_fields_never_reach_a_receipt(self):
        value = self.observation()
        value['paired'] = {'tenantFailureStatus': 400, 'createStatus': 200,
                           'compensationStatus': -1, 'allocationRestored': False,
                           'beforeDelayedRelease': {'Authorization': 'synthetic-private-bearer'}}
        with self.assertRaises(helper.FixtureError) as error:
            helper.safe_observation(value)
        self.assertNotIn('synthetic-private-bearer', str(error.exception))

    def successful_pair(self):
        stage = {'sequence': 20, 'state': 'ACK', 'actualInsertCalls': 1,
                 'conditionalWriteAttempts': 0, 'conditionalWritesBlocked': 0,
                 'unconditionalSaveAttempts': 0, 'staleUpdateRefusals': 0,
                 'retiredReadChecks': 0, 'retiredReadsHidden': False,
                 'awareConditionalWriteAttempted': False, 'awareConditionalWriteBlocked': False,
                 'unawareConditionalWriteAttempted': False, 'unawareConditionalWriteBlocked': False,
                 'liveTypes': 0, 'terminalTypes': 1,
                 'primaryInsertReached': True, 'delayedInsertReached': True,
                 'primaryInsertCompleted': True, 'delayedInsertDuplicate': False,
                 'staleCaptured': True, 'ordinaryPreserved': False,
                 'newAttemptDistinct': False, 'newResultDistinct': False,
                 'counterUnchangedSinceCapture': True, 'counterNeverRewound': True,
                 'counterAdvancedExactlyOnce': False}
        retired = dict(stage, actualInsertCalls=2, delayedInsertDuplicate=True)
        stale = dict(retired, conditionalWriteAttempts=2, conditionalWritesBlocked=2,
                     staleUpdateRefusals=2, retiredReadChecks=20, retiredReadsHidden=True,
                     awareConditionalWriteAttempted=True, awareConditionalWriteBlocked=True,
                     unawareConditionalWriteAttempted=True, unawareConditionalWriteBlocked=True)
        newer = dict(stale, actualInsertCalls=3, liveTypes=1, ordinaryPreserved=True,
                     newAttemptDistinct=True, newResultDistinct=True,
                     counterAdvancedExactlyOnce=True, counterUnchangedSinceCapture=False)
        value = self.observation()
        value['paired'] = {'tenantFailureStatus': 400, 'createStatus': 200,
                           'compensationStatus': 204, 'delayedReplyStatus': 409,
                           'newTenantStatus': 200, 'oldReplayStatus': 409,
                           'allocationRestored': True, 'allocationRestoredThenReassigned': True,
                           'beforeDelayedRelease': stage, 'terminalRace': retired,
                           'staleWrites': stale, 'newAttempt': newer}
        return value

    def test_real_delayed_insert_and_conditional_write_evidence_are_required(self):
        value = self.successful_pair()
        self.assertEqual(value, helper.safe_observation(value))
        self.assertTrue(driver.paired_success(value))
        for section, field, wrong in (
                ('beforeDelayedRelease', 'delayedInsertReached', False),
                ('terminalRace', 'delayedInsertDuplicate', False),
                ('terminalRace', 'actualInsertCalls', 1),
                ('staleWrites', 'conditionalWriteAttempts', 0),
                ('staleWrites', 'conditionalWritesBlocked', 0),
                ('staleWrites', 'unconditionalSaveAttempts', 1),
                ('staleWrites', 'awareConditionalWriteAttempted', False),
                ('staleWrites', 'awareConditionalWriteBlocked', False),
                ('staleWrites', 'unawareConditionalWriteAttempted', False),
                ('staleWrites', 'unawareConditionalWriteBlocked', False),
                ('staleWrites', 'retiredReadChecks', 19),
                ('staleWrites', 'retiredReadsHidden', False),
                ('newAttempt', 'newAttemptDistinct', False),
                ('newAttempt', 'newResultDistinct', False),
                ('newAttempt', 'counterNeverRewound', False),
                ('newAttempt', 'ordinaryPreserved', False)):
            changed = copy.deepcopy(value)
            changed['paired'][section][field] = wrong
            self.assertFalse(driver.paired_success(changed), (section, field))

    def test_missing_phase_unknown_shape_boolean_count_or_auth_only_reply_refuses(self):
        for section in ('terminalRace', 'staleWrites', 'newAttempt'):
            changed = self.successful_pair()
            del changed['paired'][section]
            self.assertFalse(driver.paired_success(changed))
        for field, wrong in (('delayedReplyStatus', 403), ('oldReplayStatus', 403),
                             ('compensationStatus', -1), ('allocationRestored', False)):
            changed = self.successful_pair()
            changed['paired'][field] = wrong
            self.assertFalse(driver.paired_success(changed))
        for value in (-1, True, 'synthetic-private-proof'):
            changed = self.successful_pair()
            changed['paired']['terminalRace']['actualInsertCalls'] = value
            self.assertFalse(driver.paired_success(changed))
            with self.assertRaises(helper.FixtureError):
                helper.safe_observation(changed)

    def test_actual_nested_fixture_bytes_and_present_jar_are_bound_without_private_paths(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            compiled = root / 'target/test-classes/SyntheticFixture.class'
            compiled.parent.mkdir(parents=True)
            compiled.write_bytes(b'\xca\xfe\xba\xbeparent')
            nested = compiled.with_name('SyntheticFixture$Control.class')
            nested.write_bytes(b'\xca\xfe\xba\xbenested')
            jar = root / 'synthetic-dependency.jar'
            jar.write_bytes(b'synthetic-dependency')
            cp = root / 'private-classpath'
            cp.write_text(str(jar))
            result = helper.compiled_fixture_inventory(root, compiled, cp)
            self.assertEqual({'target/test-classes/SyntheticFixture.class',
                              'target/test-classes/SyntheticFixture$Control.class'}, set(result['fixture']))
            self.assertEqual(hashlib.sha256(nested.read_bytes()).hexdigest(),
                             result['fixture']['target/test-classes/SyntheticFixture$Control.class'])
            self.assertEqual([{'jar': 'synthetic-dependency.jar',
                               'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
                               'bytes': len(b'synthetic-dependency')}], result['dependencies'])
            self.assertNotIn(directory, str(result))
            nested.write_bytes(b'not-compiled')
            with self.assertRaises(helper.FixtureError):
                helper.compiled_fixture_inventory(root, compiled, cp)
            nested.unlink()
            jar.unlink()
            with self.assertRaises(helper.FixtureError):
                helper.compiled_fixture_inventory(root, compiled, cp)


if __name__ == '__main__':
    unittest.main()
