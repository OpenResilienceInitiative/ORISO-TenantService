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
        self.assertEqual(4, sum(path.startswith("src/main/") for path in PROOF.FILES["cts"]))
        self.assertEqual(1, sum(path.startswith("src/test/") for path in PROOF.FILES["cts"]))
        self.assertEqual(11, len(set(PROOF.FILES["tenant"] + PROOF.FILES["cts"])))
        self.assertTrue(all(path.endswith(".java") for values in PROOF.FILES.values() for path in values))

    def test_only_the_explicit_owned_receiver_test_can_join_the_formatter_delta(self):
        test = "src/test/java/de/caritas/cob/consultingtypeservice/config/security/TenantBootstrapV2ContextTest.java"
        self.assertTrue(PROOF.owned_delta("cts", [test], ""))
        for role, changed, untracked in (
            ("tenant", [test], ""),
            ("cts", [test.replace("ContextTest.java", "OtherTest.java")], ""),
            ("cts", [test, "src/main/java/UnownedPermission.java"], ""),
            ("cts", [test], "untracked.java"),
        ):
            with self.subTest(role=role, changed=changed, untracked=untracked):
                self.assertFalse(PROOF.owned_delta(role, changed, untracked))

    def test_only_the_explicit_owned_schema_validator_can_join_the_formatter_delta(self):
        validator = "src/main/java/de/caritas/cob/consultingtypeservice/api/consultingtypes/ConsultingTypeValidator.java"
        self.assertTrue(PROOF.owned_delta("cts", [validator], ""))
        for role, changed, untracked in (
            ("tenant", [validator], ""),
            ("cts", [validator.replace("Validator.java", "Converter.java")], ""),
            ("cts", [validator, "src/main/java/UnownedPermission.java"], ""),
            ("cts", [validator], "untracked.java"),
        ):
            with self.subTest(role=role, changed=changed, untracked=untracked):
                self.assertFalse(PROOF.owned_delta(role, changed, untracked))


    def test_only_the_explicit_owned_claim_model_can_join_the_formatter_delta(self):
        model = "src/main/java/de/caritas/cob/consultingtypeservice/api/model/TenantBootstrapClaim.java"
        self.assertTrue(PROOF.owned_delta("cts", [model], ""))
        for role, changed, untracked in (
            ("tenant", [model], ""),
            ("cts", [model.replace("Claim.java", "Entity.java")], ""),
            ("cts", [model, "src/main/java/UnownedPermission.java"], ""),
            ("cts", [model], "untracked.java"),
        ):
            with self.subTest(role=role, changed=changed, untracked=untracked):
                self.assertFalse(PROOF.owned_delta(role, changed, untracked))


if __name__ == "__main__":
    unittest.main()
