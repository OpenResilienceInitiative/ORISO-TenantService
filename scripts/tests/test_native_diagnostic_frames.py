import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "native_diagnostic_frames", Path(__file__).parents[1] / "test-bootstrap-context-roundtrip.py")
proof = importlib.util.module_from_spec(spec)
spec.loader.exec_module(proof)


class NativeDiagnosticFramesPrivacyControl(unittest.TestCase):
    def test_only_exception_classes_and_application_locations_survive_private_values(self):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "caller.log"
            log.write_text(
                "private-body private-proof private-password private-code private-link\n"
                "java.lang.IllegalStateException: private-body private-proof\n"
                "\tat com.vi.tenantservice.api.facade.TenantServiceFacade.createTenant(TenantServiceFacade.java:215)\n"
                "Authorization: Bearer private-token\n")
            result = proof.safe_java_frames(log)
            self.assertEqual({"log": "caller.log", "exceptionTypes": ["java.lang.IllegalStateException"],
                              "applicationFrames": [{"method": "com.vi.tenantservice.api.facade.TenantServiceFacade.createTenant",
                                                     "file": "TenantServiceFacade.java", "line": 215}]}, result)
            encoded = json.dumps(result)
            self.assertNotIn("private-", encoded)
            self.assertNotIn(directory, encoded)


if __name__ == "__main__":
    unittest.main()
