from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]


def job_block(workflow: str, job_name: str) -> str:
    marker = f"  {job_name}:\n"
    start = workflow.index(marker)
    remainder = workflow[start + len(marker) :]
    next_job = re.search(r"\n  [a-zA-Z0-9_-]+:\n", remainder)
    return remainder if next_job is None else remainder[: next_job.start()]


class RequiredCiContractTest(unittest.TestCase):
    def test_maven_verify_remains_a_hard_gate(self):
        action = (ROOT / ".github/actions/maven-build/action.yml").read_text()
        self.assertIn("./mvnw -B verify", action)
        self.assertNotIn("continue-on-error:", action)
        self.assertNotIn("if ! ./mvnw", action)
        self.assertIn("Zero test reports", action)

    def test_pull_request_has_one_truthful_required_conclusion(self):
        workflow = (ROOT / ".github/workflows/ci-pull-request.yml").read_text()
        validate = job_block(workflow, "validate")
        aggregate = job_block(workflow, "required-ci")

        self.assertIn("name: test, build and Docker validation", validate)
        self.assertNotIn("continue-on-error:", validate)
        self.assertIn("needs: [validate, contract-tests]", aggregate)
        self.assertIn("if: always()", aggregate)
        self.assertIn("name: required PreDev CI", aggregate)
        self.assertIn("needs.validate.result", aggregate)
        self.assertIn("needs.contract-tests.result", aggregate)
        # Reading a result into the environment is not the same as acting on
        # it: the conclusion itself must consider every required job.
        self.assertIn('"${CONTRACT_RESULT}" != success', aggregate)

    def test_ci_contract_tests_are_executed_by_ci(self):
        # These assertions are worthless unless something runs them. Without a
        # job that invokes pytest, tests/ci is dead weight that can drift out
        # of sync with the workflows it claims to protect.
        workflow = (ROOT / ".github/workflows/ci-pull-request.yml").read_text()
        contract = job_block(workflow, "contract-tests")

        self.assertIn("pytest", contract)
        self.assertIn("tests/ci", contract)
        self.assertNotIn("continue-on-error:", contract)


if __name__ == "__main__":
    unittest.main()
