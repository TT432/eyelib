# Engineering Conventions

## Gradle Execution

- **ALL Gradle commands** must use JetBrains MCP (`jetbrain_run_gradle_tasks`) or IDE MCP tools.
- **Never** run `./gradlew ...` directly in shell.
- **Allowed MCP tools**: `jetbrain_run_gradle_tasks`, `jetbrain_build_project`, `jetbrain_sync_gradle_projects`
- If MCP is unavailable: **stop and ask the user** to re-enable MCP before continuing.

## Commit Message Convention

### Format

```
<phase>: <imperative summary>
```

### Phase Tags

| Tag | Meaning |
|---|---|
| `phase0:` | Overview, boundaries, docs |
| `phase1:` | Corpus, harness, test infrastructure |
| `phase2:` | Parser, AST, frontend |
| `phase3:` | Binder, diagnostics, normalization |
| `phase4:` | Host/query bridge, mapping, contracts |
| `phase5:` | Execution, runtime semantics |
| `phase6:` | Policy, specialization, cache, cutover |
| `infra:` | Build, CI, tooling, .sisyphus |
| `docs:` | Documentation-only changes |

### Examples

```
phase3: add ForEachExpr explicit binder branch
phase4: update ROADMAP status from Blocked to Current/partial
infra: add per-session KPI tracking framework
phase3: widen BindDeferredNote.Reason enum with NEEDS_HOST_SHAPE
```

### Rules

1. One commit per independently verifiable slice.
2. If ROADMAP status changes, include the update in the same commit.
3. Do not mix unrelated phase work in one commit.
