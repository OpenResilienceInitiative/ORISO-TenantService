# AGENTS.md — ORISO-TenantService

Load workspace parent `../AGENTS.md` first (`PROJECT_ORISO_ROOT` = parent of this repo).

## Stack

Java **21**, Spring Boot **4.0.1**, Maven Wrapper **3.9.15**. Owns tenants, tenant settings, multitenancy resolution (`/service/tenant*`).

## Commands

```bash
./mvnw -B test
./mvnw -B package -DskipTests
./mvnw -B spotless:check
```

From workspace: `REPO=ORISO-TenantService ../scripts/harness/verify-fast.sh` (or `verify-full.sh`).

CI: `./mvnw -B test` then `./mvnw -B package -DskipTests` on Java 21.

## Context

- Integration branch: `dev` when used for ORISO feature work.
- Tenant model ops guide: `../Deployment/guides/tenant/TENANT_AND_MULTITENANCY_GUIDE.md`.
- Skim `.understand-anything/` before non-trivial changes; README Java version may lag `pom.xml` — trust the POM.
- Changing tenant resolution affects Admin and other services — call out cross-repo impact.

## Done

Touched tests pass; package succeeds for PR-bound work; no secrets in the diff. Task notes: `docs/agent-tasks/YYYY-MM-DD_short-name/` if needed.

## AI agent delivery rules

Binding for every AI coding agent working in this repository. Canonical text and
rationale: `ORISO-Docs/oriso-platform/coding-standards.mdx` (section "AI agent
delivery rules"). Summary:

- **An agent never merges its own pull request.** Not on green CI, not on "finish
  it", not for chores or test-only changes. Delivery ends at: verified → PR open
  with evidence and a reviewer test plan → reviewers requested → issue
  `In review`. Merge only on an explicit, per-PR instruction naming that PR.
- **Request reviewers in the same step that opens the PR.** A PR without
  requested reviewers is not open for review.
- **"Pre-Dev is free" means the server, not the branch.** Deploying images,
  mutating config or data and running E2E on the Pre-Dev server needs no
  approval; the `dev` *branch* is review-gated like any shared branch.
- **Restore what you borrowed.** Record image reference *and* `imagePullPolicy`
  before swapping anything on Pre-Dev, put both back before reporting done, and
  say so in the report.
- **State where it was verified** in every PR body — environment and image, or
  plainly "local only".

## Writing issues and pull requests

Binding for every AI agent. These rules govern *where* text goes and *whose*
text may be changed. They do not relax the delivery rules above.

- **Machine detail belongs in fenced code blocks.** Scanner output, dependency
  trees, stack traces, failing job logs, resolved versions, config excerpts: put
  them inside a fenced block. A human skimming the ticket must be able to skip
  the block and still understand the point. Prose outside the block stays short
  and in plain language.
- **Write findings into the description, not into another comment.** A comment
  is for a decision or a question that needs a person. Analysis, cause, status
  and evidence belong in the issue or pull request description, where the next
  reader finds them without scrolling a thread. Prefer updating the description
  over adding a third, fourth, fifth comment.
- **Lead with the business or end-user effect.** Before any technical detail,
  two or three plain sentences: what does not work for whom, and what that
  costs. Write it so a non-engineer stakeholder can act on it. English, short.
- **You may edit descriptions — but not everyone's.**

  | Description author | May an agent rewrite it? |
  | --- | --- |
  | `Storypapst`, `kiodreambau` | Yes — rewrite, restructure, correct, extend |
  | `BjoernLudwig`, `HelenaSKloeckner`, any other human | No — leave their wording untouched; append a clearly separated section below it |

- **Adding an analysis to someone's bug report** — the ticket says "X is
  broken" and you found out why:
  1. Keep the original report as written.
  2. Add the plain-language cause, two or three sentences.
  3. Put the technical evidence under it, in a code block.
  4. Link the pull request, run or ticket that proves it.
  If the description was corrected rather than extended, say so in one line so
  the change is not silent.
- **Link the ticket you found.** If an issue already covers the problem,
  reference it rather than restating it, and add your findings there.
- **Duplicates: decide, never leave both drifting.** Name in the description
  which ticket survives. A ticket that came back from Caritas — raised by
  `BjoernLudwig` or `HelenaSKloeckner` — is the one that stays open. Close the
  agent-created duplicate against it and link the survivor.
