---
name: regression-check
description: Final verification pass over touched code after the goal-loop converges. Runs targeted then broader checks and records evidence. Use before PR prep, or when the user asks "did this break anything".
---

# Regression Check

Run after the loop exits green, before PR prep. Record results in `04-test-evidence.md`.

Order (stop escalating when confidence is sufficient):

1. `./mvnw -B test` for touched modules
2. `./mvnw -B package -DskipTests` for PR-bound work
3. Spotless/checkstyle only when this repo’s `pom.xml` defines them and you touched Java sources

Evidence format in `04-test-evidence.md` — one line per check:

```markdown
- `<command>` → pass|fail (<counts or short failure summary>)
```

If a check fails on something unrelated to this change, say so precisely and note the pre-existing failure; do not silently skip it.
