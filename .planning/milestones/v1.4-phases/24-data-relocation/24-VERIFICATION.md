# Phase 24 Verification

## Checks

1. `src/main/java/io/github/tt432/eyelib/client/model/bake/` -> no `.java` files remain
2. Controller pure definitions now live under `eyelib-importer/.../animation/bedrock/controller/`
3. `jetbrain_run_gradle_tasks [":eyelib-importer:test"]` -> exit code 0
4. `jetbrain_run_gradle_tasks [":eyelib-preprocessing:test"]` -> exit code 0
5. `jetbrain_run_gradle_tasks ["test"]` -> exit code 0
6. `jetbrain_build_project rebuild=true` -> success

## Outcome

- `DATA-01` satisfied
- `DATA-02` satisfied
- `DATA-03` satisfied for the Phase 24 identified pure-data set
- Phase 25 can proceed
