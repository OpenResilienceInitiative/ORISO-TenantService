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

    def test_owned_allowlist_is_exactly_the_reviewed_c16_sources(self):
        self.assertEqual({"tenant", "cts"}, set(PROOF.FILES))
        self.assertEqual(16, len(PROOF.FILES["tenant"]))
        self.assertEqual(13, sum(path.startswith("src/main/") for path in PROOF.FILES["cts"]))
        self.assertEqual(5, sum(path.startswith("src/test/") for path in PROOF.FILES["cts"]))
        self.assertEqual(34, len(set(PROOF.FILES["tenant"] + PROOF.FILES["cts"])))
        self.assertTrue(all(path.endswith(".java") for values in PROOF.FILES.values() for path in values))

    def test_held_replay_formatter_extension_is_only_its_two_owned_java_paths(self):
        for path in ("src/main/java/com/vi/tenantservice/api/service/TenantHeldBootstrap.java",
                     "src/test/java/com/vi/tenantservice/api/service/TenantHeldBootstrapIT.java"):
            with self.subTest(path=path):
                self.assertTrue(PROOF.owned_delta("tenant", [path], ""))
                self.assertFalse(PROOF.owned_delta("cts", [path], ""))
                self.assertFalse(PROOF.owned_delta("tenant", [path.replace("TenantHeldBootstrap", "OtherAuthority")], ""))
                self.assertFalse(PROOF.owned_delta("tenant", [path, "src/main/java/UnownedPermission.java"], ""))
                self.assertFalse(PROOF.owned_delta("tenant", [path], "untracked.java"))

    def test_only_the_literal_current_cts_grouped_fixture_can_join_formatter(self):
        path = "src/test/java/de/caritas/cob/consultingtypeservice/api/controller/GroupedTaskServiceIdentityAuthorizationIT.java"
        self.assertTrue(PROOF.owned_delta("cts", [path], ""))
        self.assertFalse(PROOF.owned_delta("tenant", [path], ""))
        self.assertFalse(PROOF.owned_delta("cts", [path.replace("GroupedTaskServiceIdentityAuthorizationIT", "OtherAuthorityIT")], ""))
        self.assertFalse(PROOF.owned_delta("cts", [path, "src/main/java/UnownedPermission.java"], ""))
        self.assertFalse(PROOF.owned_delta("cts", [path], "untracked.java"))

    def test_exact_audience_effect_extension_excludes_authority_neighbors(self):
        paths = {
            "tenant": ["src/main/java/com/vi/tenantservice/config/security/TaskServiceActor.java",
                       "src/main/java/com/vi/tenantservice/config/security/TaskServiceIdentity.java",
                       "src/test/java/com/vi/tenantservice/config/security/WizardPolicyV2ContextTest.java",
                       "src/test/java/com/vi/tenantservice/api/controller/AccountPolicyPurposeAuthorizationIT.java",
                       "src/test/java/com/vi/tenantservice/api/controller/GroupedTaskServiceIdentityAuthorizationIT.java"],
            "cts": ["src/main/java/de/caritas/cob/consultingtypeservice/config/security/TaskServiceActor.java",
                    "src/main/java/de/caritas/cob/consultingtypeservice/config/security/TaskServiceIdentity.java"],
        }
        for role, owned in paths.items():
            for path in owned:
                with self.subTest(role=role, path=path):
                    self.assertTrue(PROOF.owned_delta(role, [path], ""))
                    self.assertFalse(PROOF.owned_delta("cts" if role == "tenant" else "tenant", [path], ""))
                    neighbor = path.rsplit("/", 1)[0] + "/UnownedAuthority.java"
                    self.assertFalse(PROOF.owned_delta(role, [path, neighbor], ""))
                    self.assertFalse(PROOF.owned_delta(role, [path], "untracked.java"))

    def test_audience_and_startup_extension_is_only_four_explicit_owned_tests(self):
        packages = {"tenant": "com/vi/tenantservice", "cts": "de/caritas/cob/consultingtypeservice"}
        for role, package in packages.items():
            for name in ("TaskServiceIdentityTest", "CreationContextKeyTest"):
                path = "src/test/java/" + package + "/config/security/" + name + ".java"
                with self.subTest(role=role, path=path):
                    self.assertTrue(PROOF.owned_delta(role, [path], ""))
                    self.assertFalse(PROOF.owned_delta("cts" if role == "tenant" else "tenant", [path], ""))
                    self.assertFalse(PROOF.owned_delta(role, [path.replace(name, "OtherAuthorityTest")], ""))
                    self.assertFalse(PROOF.owned_delta(role, [path, "src/main/java/UnownedPermission.java"], ""))
                    self.assertFalse(PROOF.owned_delta(role, [path], "untracked.java"))

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

    def test_native_fixture_formatting_cannot_include_a_neighbor_or_wrong_repository(self):
        tenant = "src/test/java/com/vi/tenantservice/api/controller/BootstrapV2CallerReceiverIT.java"
        receiver = "src/test/java/de/caritas/cob/consultingtypeservice/api/controller/BootstrapV2ReceiverFixture.java"
        for role, path, other in (("tenant", tenant, "cts"), ("cts", receiver, "tenant")):
            self.assertTrue(PROOF.owned_delta(role, [path], ""))
            self.assertFalse(PROOF.owned_delta(other, [path], ""))
            self.assertFalse(PROOF.owned_delta(role, [path.replace(".java", "Neighbor.java")], ""))
            self.assertFalse(PROOF.owned_delta(role, [path, "src/main/java/Permission.java"], ""))
            self.assertFalse(PROOF.owned_delta(role, [path], "untracked.java"))

    def test_compensation_formatter_extension_is_only_the_seven_owned_java_paths(self):
        paths = [
            "api/controller/ConsultingTypeController.java", "api/model/ConsultingTypeEntity.java",
            "api/consultingtypes/ConsultingTypeGroupRepositoryImpl.java",
            "api/consultingtypes/ConsultingTypeMongoRepositoryService.java",
            "api/consultingtypes/ConsultingTypeMongoTenantAwareRepositoryService.java",
            "api/consultingtypes/ConsultingTypeRepository.java",
            "api/consultingtypes/ConsultingTypeTenantAwareRepository.java"]
        for suffix in paths:
            path = "src/main/java/de/caritas/cob/consultingtypeservice/" + suffix
            with self.subTest(path=path):
                self.assertTrue(PROOF.owned_delta("cts", [path], ""))
                self.assertFalse(PROOF.owned_delta("tenant", [path], ""))
                self.assertFalse(PROOF.owned_delta("cts", [path.replace(".java", "Other.java")], ""))
                self.assertFalse(PROOF.owned_delta("cts", [path, "src/main/java/Permission.java"], ""))
                self.assertFalse(PROOF.owned_delta("cts", [path], "untracked.java"))


if __name__ == "__main__":
    unittest.main()
