#!/usr/bin/env python3

"""Renders JaCoCo line and branch coverage as a step-summary section.

The run summary already reports how many tests executed. It does not say how
much of the service those tests actually reach, so a shrinking covered share
stayed invisible between merges. JaCoCo already runs in the build; this turns
its CSV into the same table the summary shows for the suite inventory.

Coverage is measured over the unit suite only. CI runs `mvnw verify`, so the
integration suite appends to the same execution data after this report has been
written; the figure here is a floor rather than the whole picture.
"""

import argparse
import csv
from pathlib import Path
import sys


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_REPORT = REPOSITORY_ROOT / "target" / "site" / "jacoco" / "jacoco.csv"

COUNTERS = ("LINE", "BRANCH")

# JaCoCo emits one row per class. Both breakdowns are collapsed behind <details>
# so neither buries the totals: packages answer "which area is thin", classes
# answer "which file do I open". Both are ordered least-covered first, so the
# rows worth acting on are the ones you see without scrolling.
#
# A step summary is capped at 1 MiB by GitHub; past that the whole section is
# dropped rather than truncated, which would lose the totals too. At roughly
# 90 bytes a row that is ~11k classes, far above this service's ~700, but the
# cap below keeps a monorepo-sized report from silently costing the summary.
# When it bites it says so in the rendered output — a table that quietly stops
# short reads as "that is all of them".
MAX_CLASS_ROWS = 2000


class Coverage:
    def __init__(self) -> None:
        self.covered = {counter: 0 for counter in COUNTERS}
        self.missed = {counter: 0 for counter in COUNTERS}

    def add(self, row: dict) -> None:
        for counter in COUNTERS:
            self.covered[counter] += int(row[f"{counter}_COVERED"])
            self.missed[counter] += int(row[f"{counter}_MISSED"])

    def total(self, counter: str) -> int:
        return self.covered[counter] + self.missed[counter]

    def percentage(self, counter: str) -> float:
        total = self.total(counter)
        # A class with no branches at all is not 0% covered, it is not
        # applicable. Reporting 100 keeps such packages from dragging the
        # ordering below genuinely uncovered ones.
        return self.covered[counter] / total * 100.0 if total else 100.0


def collect(report: Path) -> tuple[Coverage, dict[str, Coverage], dict[str, Coverage]]:
    overall = Coverage()
    packages: dict[str, Coverage] = {}
    classes: dict[str, Coverage] = {}

    with report.open(newline="") as handle:
        for row in csv.DictReader(handle):
            overall.add(row)
            packages.setdefault(row["PACKAGE"], Coverage()).add(row)
            # Fully qualified: JaCoCo repeats short class names across packages
            # (several `Mapper`s, several `Config`s), and a bare name would both
            # collide in this dict and be useless to look up.
            classes.setdefault(f"{row['PACKAGE']}.{row['CLASS']}", Coverage()).add(row)

    return overall, packages, classes


def coverage_rows(entries: dict[str, Coverage], label: str) -> list[str]:
    """Least covered first; within equal coverage the larger unit leads.

    Sorting by percentage alone would put a 0%-covered one-line class above a
    0%-covered 300-line service, which inverts what a reader should look at.
    """
    rows = [
        f"| {label} | Lines | Line % | Branch % |",
        "| --- | ---: | ---: | ---: |",
    ]
    ordered = sorted(
        entries.items(),
        key=lambda item: (item[1].percentage("LINE"), -item[1].total("LINE")),
    )
    for name, coverage in ordered[:MAX_CLASS_ROWS]:
        rows.append(
            f"| `{name}` | {coverage.total('LINE'):,} | "
            f"{coverage.percentage('LINE'):.1f}% | "
            f"{coverage.percentage('BRANCH'):.1f}% |"
        )

    omitted = len(ordered) - MAX_CLASS_ROWS
    if omitted > 0:
        # Named, not silent: the omitted rows are the best-covered ones, but a
        # table that just stops reads as though it were complete.
        rows.extend(
            [
                "",
                f"{omitted:,} better-covered rows omitted to keep the summary "
                f"within GitHub's size limit; the full detail is in the uploaded "
                f"HTML report.",
            ]
        )
    return rows


def render_markdown(
    overall: Coverage,
    packages: dict[str, Coverage],
    classes: dict[str, Coverage],
) -> str:
    lines = [
        "| Metric | Covered | Total | Coverage |",
        "| --- | ---: | ---: | ---: |",
    ]
    for counter, label in (("LINE", "Lines"), ("BRANCH", "Branches")):
        lines.append(
            f"| {label} | {overall.covered[counter]:,} | {overall.total(counter):,} | "
            f"**{overall.percentage(counter):.2f}%** |"
        )

    lines.extend(
        [
            "",
            f"Measured over the unit suite across {len(packages):,} packages "
            f"and {len(classes):,} classes. CI runs `mvnw verify`, so the "
            "integration suite appends to the same execution data after this "
            "report is written and is not included.",
            "",
            "<details>",
            "<summary>Per-package breakdown (least covered first)</summary>",
            "",
        ]
    )
    lines.extend(coverage_rows(packages, "Package"))
    lines.extend(
        [
            "",
            "</details>",
            "",
            "<details>",
            "<summary>Per-class breakdown (least covered first)</summary>",
            "",
        ]
    )
    lines.extend(coverage_rows(classes, "Class"))
    lines.extend(["", "</details>"])
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--report",
        type=Path,
        default=DEFAULT_REPORT,
        help="JaCoCo CSV report (default: target/site/jacoco/jacoco.csv)",
    )
    arguments = parser.parse_args()

    # A build that fails before the report goal leaves no CSV. Say so plainly
    # rather than failing the step: the suite inventory already decides whether
    # the run passes, and a missing report is a symptom of that, not a cause.
    if not arguments.report.is_file():
        print(f"No JaCoCo report at {arguments.report}.")
        print()
        print("Coverage is produced by the `report` goal during the `test` phase.")
        return 0

    overall, packages, classes = collect(arguments.report)
    if not packages:
        print(f"JaCoCo report at {arguments.report} contains no classes.")
        return 0

    # Trailing blank line: the next summary section's heading would otherwise sit
    # directly under this section's closing </details> and stop rendering. It is
    # printed here rather than by the workflow step, because an `echo` after the
    # script inside the step's `{ ... } | tee` group would replace the script's
    # exit status with the echo's and mask a failing step.
    print(render_markdown(overall, packages, classes))
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
