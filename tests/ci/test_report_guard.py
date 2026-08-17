from __future__ import annotations

from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
GUARD = ROOT / "scripts/ci/test-report-guard.py"
MAVEN_BUILD_ACTION = ROOT / ".github/actions/maven-build/action.yml"
ALLOWLIST = ROOT / ".github/allowed-skipped-tests.txt"


def report(
    class_name: str,
    *,
    passed: tuple[str, ...] = (),
    skipped: tuple[str, ...] = (),
    failed: tuple[str, ...] = (),
    errored: tuple[str, ...] = (),
    reason: str = "Environment variable [SOME_VAR] does not exist",
) -> str:
    """Renders a surefire report in the shape the plugin actually writes.

    A class disabled by a JUnit condition is the case that matters: surefire
    still counts its methods in `tests` and repeats them as `<testcase>`
    elements, each carrying a `<skipped>` child.
    """
    cases = [f'<testcase name="{name}" classname="{class_name}" time="0.01"/>' for name in passed]
    cases += [
        f'<testcase name="{name}" classname="{class_name}" time="0.0">'
        f'<skipped message="{reason}"/></testcase>'
        for name in skipped
    ]
    cases += [
        f'<testcase name="{name}" classname="{class_name}" time="0.01">'
        f'<failure message="expected true">boom</failure></testcase>'
        for name in failed
    ]
    cases += [
        f'<testcase name="{name}" classname="{class_name}" time="0.01">'
        f'<error message="NullPointerException">boom</error></testcase>'
        for name in errored
    ]

    total = len(passed) + len(skipped) + len(failed) + len(errored)
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f'<testsuite name="{class_name}" time="0.0" tests="{total}" '
        f'errors="{len(errored)}" skipped="{len(skipped)}" failures="{len(failed)}">'
        + "".join(cases)
        + "</testsuite>\n"
    )


class TestReportGuardTest(unittest.TestCase):
    def run_guard(self, reports: dict[str, str] | None, allowlist: str | None = None):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            if reports is not None:
                directory = root / "target" / "surefire-reports"
                directory.mkdir(parents=True)
                for class_name, body in reports.items():
                    (directory / f"TEST-{class_name}.xml").write_text(body)

            command = [sys.executable, str(GUARD), "--root", str(root)]
            if allowlist is not None:
                path = root / "allowed-skipped-tests.txt"
                path.write_text(allowlist)
                command += ["--allowlist", str(path)]

            return subprocess.run(command, check=False, capture_output=True, text=True)

    # --- the defect this guard exists to catch -------------------------------

    def test_fails_on_a_skipped_test_that_is_not_declared(self):
        """The whole point: `tests="4" skipped="4"` used to read as four executions."""
        result = self.run_guard(
            {
                "com.example.DriftIT": report(
                    "com.example.DriftIT", skipped=("first", "second", "third", "fourth")
                ),
                "com.example.RealTest": report("com.example.RealTest", passed=("works",)),
            }
        )

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("com.example.DriftIT#first", result.stdout)
        self.assertIn("4 skipped", result.stdout)

    def test_counts_skipped_tests_as_not_executed(self):
        result = self.run_guard(
            {
                "com.example.DriftIT": report("com.example.DriftIT", skipped=("a", "b")),
                "com.example.RealTest": report("com.example.RealTest", passed=("x", "y", "z")),
            },
            allowlist="com.example.DriftIT # tracked in #203\n",
        )

        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("| Executed | 3 |", result.stdout)
        self.assertIn("| Skipped | 2 |", result.stdout)

    def test_fails_when_every_reported_test_was_skipped(self):
        """A suite that ran nothing proves nothing, however well declared the skips are."""
        result = self.run_guard(
            {"com.example.DriftIT": report("com.example.DriftIT", skipped=("a", "b"))},
            allowlist="com.example.DriftIT # tracked in #203\n",
        )

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("zero executed tests", result.stdout)

    # --- allowlist semantics -------------------------------------------------

    def test_allows_a_declared_class_and_shows_its_reason(self):
        result = self.run_guard(
            {
                "com.example.DriftIT": report("com.example.DriftIT", skipped=("a",)),
                "com.example.RealTest": report("com.example.RealTest", passed=("x",)),
            },
            allowlist="com.example.DriftIT # needs a real MariaDB, see #203\n",
        )

        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("needs a real MariaDB, see #203", result.stdout)

    def test_a_declared_method_does_not_cover_its_siblings(self):
        result = self.run_guard(
            {
                "com.example.DriftIT": report("com.example.DriftIT", skipped=("a", "b")),
                "com.example.RealTest": report("com.example.RealTest", passed=("x",)),
            },
            allowlist="com.example.DriftIT#a # only this one is known\n",
        )

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("com.example.DriftIT#b", result.stdout)
        self.assertNotIn("`com.example.DriftIT#a` | undeclared", result.stdout)

    def test_rejects_a_declared_entry_that_carries_no_reason(self):
        """An undocumented exemption is the state this guard exists to end."""
        result = self.run_guard(
            {"com.example.RealTest": report("com.example.RealTest", passed=("x",))},
            allowlist="com.example.DriftIT\n",
        )

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("reason", result.stdout)

    def test_reports_a_stale_entry_without_failing_the_build(self):
        """A rotten entry cannot hide anything; failing here would only break the branch."""
        result = self.run_guard(
            {"com.example.RealTest": report("com.example.RealTest", passed=("x",))},
            allowlist="com.example.Gone # deleted long ago\n",
        )

        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("stale", result.stdout.lower())
        self.assertIn("com.example.Gone", result.stdout)

    def test_ignores_comments_and_blank_lines(self):
        result = self.run_guard(
            {
                "com.example.DriftIT": report("com.example.DriftIT", skipped=("a",)),
                "com.example.RealTest": report("com.example.RealTest", passed=("x",)),
            },
            allowlist="# a heading\n\n   \ncom.example.DriftIT # tracked\n",
        )

        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_a_missing_allowlist_means_zero_tolerance(self):
        result = self.run_guard(
            {
                "com.example.DriftIT": report("com.example.DriftIT", skipped=("a",)),
                "com.example.RealTest": report("com.example.RealTest", passed=("x",)),
            },
            allowlist=None,
        )

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("com.example.DriftIT#a", result.stdout)

    # --- the checks that already existed must survive the extraction ---------

    def test_fails_when_no_reports_were_written(self):
        result = self.run_guard(None)

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("Zero test reports", result.stdout)

    def test_fails_on_failures_and_errors_and_names_them(self):
        result = self.run_guard(
            {
                "com.example.RedTest": report(
                    "com.example.RedTest", passed=("ok",), failed=("bad",), errored=("worse",)
                )
            }
        )

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("com.example.RedTest#bad", result.stdout)
        self.assertIn("com.example.RedTest#worse", result.stdout)

    def test_passes_a_fully_green_suite(self):
        result = self.run_guard(
            {"com.example.RealTest": report("com.example.RealTest", passed=("a", "b"))}
        )

        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("| Executed | 2 |", result.stdout)

    def test_reads_failsafe_reports_too(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            directory = root / "target" / "failsafe-reports"
            directory.mkdir(parents=True)
            (directory / "TEST-com.example.FailsafeIT.xml").write_text(
                report("com.example.FailsafeIT", skipped=("a",), passed=("b",))
            )

            result = subprocess.run(
                [sys.executable, str(GUARD), "--root", str(root)],
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("com.example.FailsafeIT#a", result.stdout)

    # --- wiring: the guard is worthless if the build does not run it ---------

    def test_the_maven_build_action_runs_the_guard(self):
        action = MAVEN_BUILD_ACTION.read_text()

        self.assertIn("scripts/ci/test-report-guard.py", action)
        self.assertNotIn("continue-on-error:", action)

    def test_the_checked_in_allowlist_is_accepted_by_the_guard(self):
        """A malformed entry must break the CI contract job, not the next Maven build."""
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            directory = root / "target" / "surefire-reports"
            directory.mkdir(parents=True)
            (directory / "TEST-com.example.RealTest.xml").write_text(
                report("com.example.RealTest", passed=("x",))
            )

            result = subprocess.run(
                [sys.executable, str(GUARD), "--root", str(root), "--allowlist", str(ALLOWLIST)],
                check=False,
                capture_output=True,
                text=True,
            )

        # Entries are expected to be stale against this synthetic report; the
        # assertion is that none of them is rejected as malformed.
        self.assertNotIn("without a reason", result.stdout)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
