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

So executions are counted as `tests - skipped`, and a skipped test fails the
build unless it is declared in the allowlist with a reason. An empty or absent
allowlist is therefore strict zero tolerance; a declared skip stays visible in
the step summary rather than disappearing into a green check.

Declaring an exemption is deliberately more work than deleting one. The failure
mode of #203 was not that a skip was tolerated, it was that nobody could see it.
"""

import argparse
from dataclasses import dataclass, field
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_ALLOWLIST = REPOSITORY_ROOT / ".github" / "allowed-skipped-tests.txt"

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


def load_allowlist(path: Path) -> tuple[dict[str, str], list[str]]:
    """Parses `<pattern> # <reason>` lines into {pattern: reason} plus complaints.

    A pattern never contains whitespace, so the first space separates it from
    the reason. That keeps the `#` of a `Class#method` pattern out of the way of
    the `#` that introduces the reason.
    """
    entries: dict[str, str] = {}
    complaints: list[str] = []

    if not path.is_file():
        return entries, complaints

    for number, line in enumerate(path.read_text().splitlines(), start=1):
        text = line.strip()
        if not text or text.startswith("#"):
            continue

        pattern, _, remainder = text.partition(" ")
        reason = remainder.strip().lstrip("#").strip()
        if not reason:
            complaints.append(
                f"{path}:{number}: `{pattern}` is declared without a reason. "
                "Write `<pattern> # <why it is skipped, and the issue tracking it>`."
            )
            continue

        entries[pattern] = reason

    return entries, complaints


def match(case: Case, patterns: dict[str, str]) -> str | None:
    """A bare class name covers the whole class; `Class#method` covers one method."""
    for pattern in (case.identifier, case.class_name):
        if pattern in patterns:
            return pattern
    return None


def render(suite: Suite, declared: list[tuple[Case, str]], undeclared: list[Case]) -> list[str]:
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

    # Undeclared skips are the failure, so they stay in the open. Declared ones
    # are folded away: the point of the summary is that they remain visible at
    # all, not that they crowd out the run they were allowed in.
    if undeclared:
        lines.extend(["", "| Test | Status | Reason |", "| --- | --- | --- |"])
        lines.extend(
            f"| `{case.identifier}` | undeclared | {case.message} |" for case in undeclared
        )

    if declared:
        lines.extend(
            [
                "",
                "<details>",
                f"<summary>Declared skips ({len(declared)})</summary>",
                "",
                "| Test | Status | Declared because |",
                "| --- | --- | --- |",
            ]
        )
        lines.extend(
            f"| `{case.identifier}` | declared | {reason} |" for case, reason in declared
        )
        lines.extend(["", "</details>"])

    return lines


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("."),
        help="directory to search for test reports (default: the working directory)",
    )
    parser.add_argument(
        "--allowlist",
        type=Path,
        default=DEFAULT_ALLOWLIST,
        help="declared-skip allowlist (default: .github/allowed-skipped-tests.txt)",
    )
    arguments = parser.parse_args()

    suite = collect(arguments.root)
    if not suite.reports:
        return fail(["Zero test reports found — tests were silently skipped or never ran."])

    patterns, complaints = load_allowlist(arguments.allowlist)

    declared: list[tuple[Case, str]] = []
    undeclared: list[Case] = []
    used: set[str] = set()
    for case in suite.skipped:
        if (pattern := match(case, patterns)) is None:
            undeclared.append(case)
        else:
            used.add(pattern)
            declared.append((case, patterns[pattern]))

    summary = render(suite, declared, undeclared)
    problems: list[str] = list(complaints)

    # Ordered by how much they matter: nothing ran at all, then a red test, then
    # a skip nobody declared.
    if suite.executed == 0:
        problems.append(
            f"Test reports contain zero executed tests ({suite.skipped_count} skipped)."
        )

    if suite.broken:
        problems.append(f"{len(suite.broken)} test(s) failed or errored:")
        problems.extend(f"  - {case.identifier}: {case.message}" for case in suite.broken)

    if undeclared:
        problems.append(
            f"{len(undeclared)} test(s) were skipped without being declared in "
            f"{arguments.allowlist}:"
        )
        problems.extend(f"  - {case.identifier}: {case.message}" for case in undeclared)
        problems.append(
            "Either make them run, or declare them as "
            "`<pattern> # <why, and the issue tracking it>`."
        )

    # A pattern that no longer matches anything cannot hide a skip, so warn
    # rather than fail: breaking the branch over a rotten line would punish
    # exactly the change that removed the skip.
    if stale := sorted(set(patterns) - used):
        summary.extend(
            [
                "",
                "⚠️ Stale allowlist entries (they match no skipped test): "
                f"{', '.join(f'`{pattern}`' for pattern in stale)}. Delete them from "
                f"{arguments.allowlist}.",
            ]
        )
        print(f"::warning::Stale skip allowlist entries: {', '.join(stale)}", file=sys.stderr)

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
