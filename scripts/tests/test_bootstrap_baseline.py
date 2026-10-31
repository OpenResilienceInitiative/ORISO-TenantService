import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SOURCE = Path(__file__).resolve().parents[1] / "ci/scenario-c-context-native.py"
spec = importlib.util.spec_from_file_location("bootstrap_baseline_driver", SOURCE)
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)
ROUTING = "src/main/java/com/vi/tenantservice/api/service/consultingtype/ConsultingTypeService.java"
FACADE = "src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java"


class ExactBaselineControls(unittest.TestCase):
    def fixture(self, directory):
        host = Path(directory)
        roots = {role: host / role for role in ("tenant", "cts")}
        original = {}
        for role, root in roots.items():
            original[role] = {}
            for name in (ROUTING, "signer.java", FACADE):
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes((role + name).encode())
                original[role][name] = hashlib.sha256(path.read_bytes()).hexdigest()
        catalog = host / "scripts/ci"
        catalog.mkdir(parents=True)
        (catalog / "scenario-c-bootstrap-v1-inputs.json").write_text(json.dumps(original))
        routed = json.loads(json.dumps(original))
        (roots["tenant"] / ROUTING).write_bytes(b"routing correction only")
        routed["tenant"][ROUTING] = hashlib.sha256(b"routing correction only").hexdigest()
        (catalog / "scenario-c-bootstrap-routed-v1-inputs.json").write_text(json.dumps(routed))
        return host, roots, routed

    def test_routing_delta_requires_the_explicit_separate_baseline(self):
        with tempfile.TemporaryDirectory() as directory:
            host, roots, _ = self.fixture(directory)
            with patch.object(driver, "HOST", host):
                self.assertFalse(driver.verify_v1(roots))
                self.assertTrue(driver.verify_v1(roots, "routed"))
                self.assertFalse(driver.verify_v1(roots, "unknown"))

    def test_any_other_source_change_refuses_the_routed_baseline(self):
        with tempfile.TemporaryDirectory() as directory:
            host, roots, _ = self.fixture(directory)
            (roots["cts"] / "signer.java").write_bytes(b"changed authority")
            with patch.object(driver, "HOST", host):
                self.assertFalse(driver.verify_v1(roots, "routed"))

    def test_compensator_delta_requires_its_own_explicit_baseline(self):
        with tempfile.TemporaryDirectory() as directory:
            host, roots, routed = self.fixture(directory)
            (roots["tenant"] / FACADE).write_bytes(b"reviewed downstream denial compensation")
            compensated = json.loads(json.dumps(routed))
            compensated["tenant"][FACADE] = hashlib.sha256(
                (roots["tenant"] / FACADE).read_bytes()).hexdigest()
            (host / "scripts/ci/scenario-c-bootstrap-compensated-routed-v1-inputs.json").write_text(
                json.dumps(compensated))
            with patch.object(driver, "HOST", host):
                self.assertFalse(driver.verify_v1(roots, "original"))
                self.assertFalse(driver.verify_v1(roots, "routed"))
                self.assertTrue(driver.verify_v1(roots, "compensated-routed"))

    def test_compensator_catalog_cannot_change_routing_or_receiver_authority(self):
        for role, name in (("tenant", ROUTING), ("cts", "signer.java")):
            with self.subTest(role=role, name=name), tempfile.TemporaryDirectory() as directory:
                host, roots, routed = self.fixture(directory)
                compensated = json.loads(json.dumps(routed))
                for changed_role, changed_name in (("tenant", FACADE), (role, name)):
                    path = roots[changed_role] / changed_name
                    path.write_bytes(b"unexpected additional change" + changed_name.encode())
                    compensated[changed_role][changed_name] = hashlib.sha256(path.read_bytes()).hexdigest()
                (host / "scripts/ci/scenario-c-bootstrap-compensated-routed-v1-inputs.json").write_text(
                    json.dumps(compensated))
                with patch.object(driver, "HOST", host):
                    self.assertFalse(driver.verify_v1(roots, "compensated-routed"))

    def policy_fixture(self, directory):
        host, roots, routed = self.fixture(directory)
        compensated = json.loads(json.dumps(routed))
        facade = roots['tenant'] / FACADE
        facade.write_bytes(b'reviewed compensation')
        compensated['tenant'][FACADE] = hashlib.sha256(facade.read_bytes()).hexdigest()
        (host / 'scripts/ci/scenario-c-bootstrap-compensated-routed-v1-inputs.json').write_text(json.dumps(compensated))
        facade.write_bytes(b'reviewed compensation plus narrow policy projection')
        policy = json.loads(json.dumps(compensated))
        policy['tenant'][FACADE] = hashlib.sha256(facade.read_bytes()).hexdigest()
        (host / 'scripts/ci/scenario-c-bootstrap-compensated-routed-policy-v1-inputs.json').write_text(json.dumps(policy))
        return host, roots, policy

    def test_policy_composition_requires_its_own_explicit_baseline(self):
        with tempfile.TemporaryDirectory() as directory:
            host, roots, _ = self.policy_fixture(directory)
            with patch.object(driver, 'HOST', host):
                self.assertFalse(driver.verify_v1(roots, 'compensated-routed'))
                self.assertTrue(driver.verify_v1(roots, 'compensated-routed-policy'))

    def test_policy_catalog_cannot_rebaseline_any_second_path_or_receiver_signer(self):
        for role, name in (('tenant', ROUTING), ('cts', 'signer.java'), ('cts', FACADE)):
            with self.subTest(role=role, name=name), tempfile.TemporaryDirectory() as directory:
                host, roots, policy = self.policy_fixture(directory)
                path = roots[role] / name
                path.write_bytes(b'unreviewed authority change')
                policy[role][name] = hashlib.sha256(path.read_bytes()).hexdigest()
                (host / 'scripts/ci/scenario-c-bootstrap-compensated-routed-policy-v1-inputs.json').write_text(json.dumps(policy))
                with patch.object(driver, 'HOST', host):
                    self.assertFalse(driver.verify_v1(roots, 'compensated-routed-policy'))

    def test_an_empty_missing_or_expanded_catalog_cannot_weaken_the_guard(self):
        for change in ("empty", "missing", "extra", "changed"):
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                host, roots, routed = self.fixture(directory)
                if change == "empty":
                    routed = {}
                elif change == "missing":
                    routed["cts"].pop(FACADE)
                elif change == "extra":
                    routed["cts"]["extra.java"] = "0" * 64
                else:
                    routed["cts"]["signer.java"] = "0" * 64
                (host / "scripts/ci/scenario-c-bootstrap-routed-v1-inputs.json").write_text(
                    json.dumps(routed))
                with patch.object(driver, "HOST", host):
                    self.assertFalse(driver.verify_v1(roots, "routed"))
