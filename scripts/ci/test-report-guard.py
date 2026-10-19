#!/usr/bin/env python3

"""Fails the build when the test suite did not actually prove anything.

The previous inline version of this guard read only `tests`, `failures` and
`errors` from each surefire report. That is blind to the case it was written
for. A class switched off by a JUnit condition still appears in the report:

    <testsuite name="…LiquibaseSchemaDriftIT" tests="4" skipped="4" failures="0" errors="0">

so its four skipped methods were counted as four executions and the gate went
green. `LiquibaseSchemaDriftIT` sat skipped in every run on that basis (#203).
The same blindness defeated the zero-test check, which can never fire while a
fully disabled suite reports its skipped methods in `tests`.

So executions are counted as `tests - skipped`, and every skipped test fails the
build. A required gate has no skip exemption: a test either runs, is repaired,
or is removed from the required suite explicitly rather than producing a green
report that overstates what was executed.
"""

import argparse
from dataclasses import dataclass, field
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


REPORT_GLOBS = (
    "**/target/surefire-reports/TEST-*.xml",
    "**/target/failsafe-reports/TEST-*.xml",
)


@dataclass(frozen=True)
class Case:
    """One `<testcase>`, identified the way a developer would grep for it."""

    class_name: str
    method: str
    message: str

    @property
    def identifier(self) -> str:
        return f"{self.class_name}#{self.method}"


@dataclass
class Suite:
    reports: int = 0
    tests: int = 0
    skipped_count: int = 0
    skipped: list[Case] = field(default_factory=list)
    broken: list[Case] = field(default_factory=list)

    @property
    def executed(self) -> int:
        return self.tests - self.skipped_count


def collect(root: Path) -> Suite:
    suite = Suite()
    for glob in REPORT_GLOBS:
        for report in sorted(root.glob(glob)):
            suite.reports += 1
            element = ET.parse(report).getroot()
            suite.tests += int(element.attrib.get("tests", 0))
            suite.skipped_count += int(element.attrib.get("skipped", 0))

            for case in element.iter("testcase"):
                class_name = case.attrib.get("classname", element.attrib.get("name", "?"))
                method = case.attrib.get("name", "?")

                if (skipped := case.find("skipped")) is not None:
                    suite.skipped.append(
                        Case(class_name, method, skipped.attrib.get("message", "no reason given"))
                    )
                    continue

                for outcome in ("failure", "error"):
                    if (broken := case.find(outcome)) is not None:
                        suite.broken.append(
                            Case(class_name, method, broken.attrib.get("message", outcome))
                        )
                        break

    return suite


def render(suite: Suite) -> list[str]:
    lines = [
        "| Metric | Count |",
        "| --- | ---: |",
        f"| Executed | {suite.executed:,} |",
        f"| Skipped | {suite.skipped_count:,} |",
        f"| Reports | {suite.reports:,} |",
        "",
        f"{suite.executed:,} executed, {suite.skipped_count:,} skipped "
        f"across {suite.reports:,} report(s).",
    ]

    if suite.skipped:
        lines.extend(["", "| Test | Status | Reason |", "| --- | --- | --- |"])
        lines.extend(
            f"| `{case.identifier}` | forbidden skip | {case.message} |"
            for case in suite.skipped
        )

    return lines


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("."),
        help="directory to search for test reports (default: the working directory)",
    )
    arguments = parser.parse_args()

    suite = collect(arguments.root)
    if not suite.reports:
        return fail(["Zero test reports found — tests were silently skipped or never ran."])

    summary = render(suite)
    problems: list[str] = []

    # Ordered by how much they matter: nothing ran at all, then a red test, then
    # any skip in the required suite.
    if suite.executed == 0:
        problems.append(
            f"Test reports contain zero executed tests ({suite.skipped_count} skipped)."
        )

    if suite.broken:
        problems.append(f"{len(suite.broken)} test(s) failed or errored:")
        problems.extend(f"  - {case.identifier}: {case.message}" for case in suite.broken)

    if suite.skipped:
        problems.append(
            f"{len(suite.skipped)} test(s) were skipped; this gate requires zero skips:"
        )
        problems.extend(f"  - {case.identifier}: {case.message}" for case in suite.skipped)

    print("\n".join(summary))
    print()

    if problems:
        return fail(problems, summary_already_printed=True)
    return 0


def fail(problems: list[str], summary_already_printed: bool = False) -> int:
    if summary_already_printed:
        print("**The test gate failed:**")
        print()
    for problem in problems:
        print(problem)
        print(f"::error::{problem.strip()}", file=sys.stderr)
    print()
    return 1


if __name__ == "__main__":
    sys.exit(main())
