# Phase 25 Verification

## Checks

1. `jetbrain_run_gradle_tasks [":eyelib-attachment:test"]` -> exit code 0
2. `jetbrain_run_gradle_tasks [":test"]` -> exit code 0
3. `jetbrain_build_project rebuild=true` -> success
4. `grep 'io.github.tt432.eyelib.capability.(ExtraEntityData|ExtraEntityUpdateData|EntityStatistics)'` -> no matches

## Outcome

- Attachment-side payload/data/codec extraction completed without reverse dependency from `:eyelib-attachment` back to root
- Phase 26 can proceed
