import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location("c16_format", Path(__file__).parents[1] / "ci/scenario-c-c16-format-proposal.py")
PROOF = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROOF)


class C16FormatProposalControls(unittest.TestCase):
    def receipt(self):
        return {"sourceStable": True, "nativeResourcesStarted": False, "accepted": False,
                "compileExit": 1, "classification": "COMPILE_OR_FORMAT_SETUP_FAILURE_NO_PRODUCT_RED"}

    def log(self):
        return "The following files had format violations: Run 'mvn spotless:apply'"

    def test_only_confirmed_pre_container_formatter_failure_is_eligible(self):
        self.assertTrue(PROOF.eligible(self.receipt(), self.log()))

    def test_no_feature_failure_compile_success_or_boolean_exit_can_format(self):
        for field, value in (("classification", "TEST_ASSERTION_FAILURE"), ("compileExit", 0),
                             ("compileExit", True), ("accepted", True)):
            with self.subTest(field=field, value=value):
                receipt = self.receipt()
                receipt[field] = value
                self.assertFalse(PROOF.eligible(receipt, self.log()))

    def test_changed_unknown_or_native_sources_cannot_format(self):
        for field, value in (("sourceStable", False), ("sourceStable", None),
                             ("nativeResourcesStarted", True), ("nativeResourcesStarted", None)):
            with self.subTest(field=field, value=value):
                receipt = self.receipt()
                receipt[field] = value
                self.assertFalse(PROOF.eligible(receipt, self.log()))

    def test_arbitrary_compile_error_is_not_format_authority(self):
        self.assertFalse(PROOF.eligible(self.receipt(), "javac failed"))

    def test_late_finalization_cannot_accept_an_otherwise_complete_patch(self):
        for now, accepted in ((599.0, True), (600.0, True), (600.001, False)):
            with self.subTest(now=now):
                receipt = {"state": "MECHANICAL_PROPOSAL_ONLY", "originalSourcesStable": True,
                           "scratchConfirmedAbsent": True}
                self.assertEqual(accepted, PROOF.finish_receipt(receipt, now, 600.0))
                self.assertIs(receipt["totalDeadlineRetained"], accepted)
                if not accepted:
                    self.assertEqual("DEADLINE_FAILURE_NO_ACCEPTANCE", receipt["state"])

    def test_owned_allowlist_is_exactly_six_tenant_and_two_receiver_sources(self):
        self.assertEqual({"tenant", "cts"}, set(PROOF.FILES))
        self.assertEqual(6, len(PROOF.FILES["tenant"]))
        self.assertEqual(2, len(PROOF.FILES["cts"]))
        self.assertEqual(8, len(set(PROOF.FILES["tenant"] + PROOF.FILES["cts"])))
        self.assertTrue(all(path.endswith(".java") for values in PROOF.FILES.values() for path in values))


if __name__ == "__main__":
    unittest.main()
