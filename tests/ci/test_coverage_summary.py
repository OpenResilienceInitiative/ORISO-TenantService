from __future__ import annotations

from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
COVERAGE_SUMMARY = ROOT / "scripts/ci/coverage-summary.py"
MAVEN_BUILD_ACTION = ROOT / ".github/actions/maven-build/action.yml"

HEADER = (
    "GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,"
    "BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,"
    "METHOD_MISSED,METHOD_COVERED"
)


def row(
    package: str,
    class_name: str,
    line_covered: int,
    line_missed: int,
    branch_covered: int = 0,
    branch_missed: int = 0,
) -> str:
    return (
        f"userservice,{package},{class_name},0,0,{branch_missed},{branch_covered},"
        f"{line_missed},{line_covered},0,0,0,0"
    )


class CoverageSummaryTest(unittest.TestCase):
    def run_summary(self, rows: list[str] | None):
        with tempfile.TemporaryDirectory() as temp_dir:
            report = Path(temp_dir) / "jacoco.csv"
            if rows is not None:
                report.write_text("\n".join([HEADER, *rows]) + "\n")

            return subprocess.run(
                [sys.executable, COVERAGE_SUMMARY, "--report", report],
                check=False,
                capture_output=True,
                text=True,
            )

    def test_reports_line_and_branch_percentages(self):
        result = self.run_summary(
            [
                row("de.example.one", "Foo", line_covered=75, line_missed=25),
                row(
                    "de.example.two",
                    "Bar",
                    line_covered=25,
                    line_missed=75,
                    branch_covered=1,
                    branch_missed=3,
                ),
            ]
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("| Lines | 100 | 200 | **50.00%** |", result.stdout)
        self.assertIn("| Branches | 1 | 4 | **25.00%** |", result.stdout)

    def test_orders_the_breakdown_least_covered_first(self):
        result = self.run_summary(
            [
                row("de.example.covered", "Foo", line_covered=90, line_missed=10),
                row("de.example.bare", "Bar", line_covered=1, line_missed=99),
            ]
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        bare = result.stdout.index("de.example.bare")
        covered = result.stdout.index("de.example.covered")
        self.assertLess(bare, covered, "least covered package must come first")

    def test_aggregates_classes_of_one_package_into_a_single_row(self):
        """A per-class table would bury the totals; the HTML report carries that detail."""
        result = self.run_summary(
            [
                row("de.example", "Foo", line_covered=50, line_missed=0),
                row("de.example", "Bar", line_covered=0, line_missed=50),
            ]
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.count("| `de.example` |"), 1)
        self.assertIn("| `de.example` | 100 | 50.0% |", result.stdout)
        self.assertIn("across 1 packages", result.stdout)

    def test_counts_a_class_without_branches_as_fully_covered(self):
        """Zero of zero branches is not applicable, not uncovered."""
        result = self.run_summary([row("de.example", "Foo", line_covered=10, line_missed=0)])

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("| Branches | 0 | 0 | **100.00%** |", result.stdout)

    def test_renders_a_per_class_table_with_fully_qualified_names(self):
        """The PR contract is a per-class table, not only per-package.

        Names are qualified because JaCoCo repeats short class names across
        packages; a bare `Config` would be ambiguous and unlookupable.
        """
        result = self.run_summary(
            [
                row("de.example.one", "Config", line_covered=1, line_missed=9),
                row("de.example.two", "Config", line_covered=9, line_missed=1),
            ]
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Per-class breakdown (least covered first)", result.stdout)
        self.assertIn("| `de.example.one.Config` |", result.stdout)
        self.assertIn("| `de.example.two.Config` |", result.stdout)

    def test_orders_classes_least_covered_first_then_largest(self):
        """A 0% one-liner must not outrank a 0% service class.

        Percentage alone would surface the trivial file first, inverting what a
        reader should open.
        """
        result = self.run_summary(
            [
                row("de.example", "Tiny", line_covered=0, line_missed=2),
                row("de.example", "Huge", line_covered=0, line_missed=300),
                row("de.example", "Covered", line_covered=10, line_missed=0),
            ]
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        classes = result.stdout.split("Per-class breakdown")[1]
        huge = classes.index("`de.example.Huge`")
        tiny = classes.index("`de.example.Tiny`")
        covered = classes.index("`de.example.Covered`")
        self.assertLess(huge, tiny, "the larger 0% class must lead")
        self.assertLess(tiny, covered, "uncovered classes must precede covered ones")

    def test_names_the_rows_it_omits_when_the_table_is_capped(self):
        """A table that silently stops reads as though it were complete."""
        rows = [
            row("de.example", f"Clazz{index:05d}", line_covered=1, line_missed=0)
            for index in range(2100)
        ]

        result = self.run_summary(rows)

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("better-covered rows omitted", result.stdout)
        self.assertIn("100 better-covered rows omitted", result.stdout)

    def test_explains_a_missing_report_without_failing_the_step(self):
        """A build that fails before the report goal leaves no CSV behind."""
        result = self.run_summary(None)

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("No JaCoCo report at", result.stdout)

    def test_tolerates_a_report_that_contains_no_classes(self):
        result = self.run_summary([])

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("contains no classes", result.stdout)

    def test_coverage_report_is_generated_outside_the_test_phase(self):
        """jacoco:report sits behind surefire in the `test` phase.

        A red suite halts the build before it, so the goal is invoked directly
        against the execution data the agent already wrote.
        """
        action = MAVEN_BUILD_ACTION.read_text()

        self.assertIn("./mvnw -B jacoco:report", action)
        self.assertIn("target/jacoco.exec", action)

    def test_standalone_report_generation_cannot_fail_the_build(self):
        """Coverage is a diagnostic; being unable to produce it is not a build failure.

        Run from the command line the goal resolves the plugin's reporting
        dependencies, which the lifecycle-bound execution never needs and which do
        not reliably resolve from a restored runner cache. A green suite skips the
        invocation entirely, because the test phase has already written the report.
        """
        action = MAVEN_BUILD_ACTION.read_text()

        invocation = action.index("./mvnw -B jacoco:report")
        following = action[invocation : invocation + 200]
        self.assertIn("||", following, "the standalone goal must tolerate its own failure")

        guard = action.index("target/site/jacoco/jacoco.csv")
        self.assertLess(guard, invocation, "an existing report must short-circuit the invocation")

    def test_standalone_report_generation_is_time_bounded(self):
        """`|| echo` catches a nonzero exit, not a hang.

        The goal resolves reporting dependencies over the network and no job sets
        timeout-minutes, so an unbounded stall would run to GitHub's 6h default
        and take the failed-test report and the status gate down with it.
        """
        action = MAVEN_BUILD_ACTION.read_text()

        invocation = action.index("./mvnw -B jacoco:report")
        preceding = action[max(0, invocation - 120) : invocation]
        self.assertIn("timeout", preceding, "the standalone goal must be time-bounded")

    def test_does_not_suppress_test_failures_in_maven(self):
        """testFailureIgnore would hide a red suite from every consumer of the build."""
        action = MAVEN_BUILD_ACTION.read_text()

        self.assertNotIn("maven.test.failure.ignore", action)
        self.assertNotIn("testFailureIgnore", action)


if __name__ == "__main__":
    unittest.main()
