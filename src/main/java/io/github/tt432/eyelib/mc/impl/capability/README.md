# MC Impl Capability Runtime Hooks

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/capability/`
- Owns Forge-side lifecycle/event hook wiring for capability runtime components that still live under legacy `capability/**` packages.

## Current owner
- `CapabilityComponentRuntimeHooks.java`: subscribes to Forge client events and forwards invalidation signals to runtime component seams (`AnimationComponent`, `RenderControllerComponent`).

## Boundary rule
- Keep direct Forge event-bus subscription wiring in this package.
- Keep component-local state contracts/invalidation methods in their runtime owner classes unless/until those owners are fully relocated.
