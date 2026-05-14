# Eyelib Material Module

## Scope
- Path: `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/`
- `:eyelib-material` is a legacyForge subproject for Bedrock material definitions, GL state management, shader pipeline, and material-specific rendering utilities.
- Consumed by root render controllers and particle renderer through Gradle project dependency.

## Current Responsibilities
- `material/`: Bedrock material definition records and texture binding contracts.
- `gl/`: GL state management and pipeline helpers.
- `render/`: Material rendering execution and pipeline wiring.
- `shader/`: Shader program and uniform management.
- `shared/`: Shared pure-data types consumed across the module.
- `smoke/`: Material-specific `@ClientSmoke` test fixtures for the `clientsmoke` framework.
- `util/`: Material-local utility helpers.
- `bootstrap/`: Forge module bootstrap marker class.

## Dependency Direction
- Root runtime depends on `:eyelib-material` through `modImplementation project(':eyelib-material')`.
- `:eyelib-material` depends on `:eyelib-util` for codec infrastructure.
- Must not depend on root runtime packages or create reverse dependencies back into `:`.

## Editing Rules
- Prefer keeping material rendering logic self-contained within this module.
- Material smoke tests in `smoke/` must not depend on feature modules.
- Do not import root runtime packages from this module.
