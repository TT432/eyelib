# MC Impl Common Entity Runtime Hooks

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/common/entity/`
- Owns Forge event subscriptions and Minecraft entity observation for common entity attachment behavior.

## Key Files
- `EntityExtraDataRuntimeHooks.java`: observes mob goal state, applies pure `ExtraEntityDataUpdater` policy, mutates the extra-entity attachment locally, and dispatches attachment sync.

## Boundary Reminder
- Keep deterministic state update policy in `common/runtime/`.
- Keep attachment storage contracts and codecs with the attachment/data owners.
- Keep Forge event wiring, mob-goal inspection, and server sync dispatch in this runtime observer until the attachment feature owns the full path end-to-end.
