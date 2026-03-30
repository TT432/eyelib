# Bedrock And Blockbench Model Import Design

## Goal

Add a unified client-side model import pipeline that supports both Bedrock geometry JSON and Blockbench `.bbmodel`, then converts both sources into Eyelib's existing intermediate `io.github.tt432.eyelib.client.model.Model` without expanding the runtime model layer.

## Scope

- Support two source formats:
  - Bedrock `geometry/*.json`
  - Blockbench `.bbmodel`
- Treat `../blockbench` as the behavioral reference for Blockbench source parsing and baseline source-model interpretation where Eyelib is intentionally compatible with it
- Convert both formats into the existing `Model`
- Preserve fields already representable by `Model` and already relevant to runtime behavior:
  - bone hierarchy
  - pivot
  - rotation
  - cube geometry
  - per-face UV
  - `ModelLocator`
  - visible box
- Keep multi-texture Blockbench support by performing texture repack as an importer post-process instead of during source parse
- Keep runtime publication through `ClientAssetRegistry`

## Non-Goals

- Do not add a new long-lived runtime intermediate model before `Model`
- Do not mirror source-format-only editor metadata into runtime types
- Do not redesign `Model.Face` in this change
- Do not push source-format knowledge into manager, registry, or render runtime layers

## Affected Modules

- Client model domain
- Client loader / manager tooling
- Client registry seam

These module responsibilities stay the same at a high level, but the model domain gains a clearer import boundary and the manager tooling layer stops owning format-specific conversion details.

## Current Problems

1. `client/model/bbmodel/BBModel.java` mixes source parsing, conversion, texture splitting, and texture repack.
2. `BBModel.from(...)` performs runtime-oriented work during source object construction.
3. `ManagerResourceImportPlanner` routes model files directly to a format-specific loader, so import orchestration and format conversion are too tightly coupled.
4. Multi-texture Blockbench support exists, but the current implementation embeds the workaround inside the source model layer instead of as an explicit post-process.
5. Bedrock and Blockbench imports do not clearly share a single conversion boundary into `Model`.
6. The current design draft did not yet state how Eyelib should preserve behavioral consistency with the sibling `../blockbench` implementation.

## Compatibility Goal

For `.bbmodel` parsing and source interpretation, Eyelib should stay behaviorally aligned with the sibling `../blockbench` project unless Eyelib explicitly chooses a different runtime-oriented rule during conversion into `Model`.

That means the implementation should separate two kinds of compatibility:

- source compatibility
  - parsing the same `.bbmodel` file successfully
  - interpreting the same source fields with the same meaning where practical
- runtime normalization
  - converting parsed source data into Eyelib's runtime `Model`
  - applying Eyelib-specific post-processing such as texture repack or runtime-oriented locator construction

Any intentional divergence from `../blockbench` should be documented inline in code comments or tests at the conversion boundary.

## Design

### 1. Import Boundary

Introduce a dedicated importer area under `client/model/` that owns conversion from source models into runtime `Model`.

Recommended structure:

- `client/model/bbmodel/`
  - source records and source parsing only
- `client/model/bedrock/`
  - source records and source parsing only
- `client/model/importer/`
  - shared import entrypoints
  - format adapters
  - shared `Model` construction logic
  - texture repack post-process

The importer boundary is responsible for:

- adapting source-specific objects into a shared construction input
- performing import-time normalization
- running optional texture repack for Blockbench multi-texture sources
- constructing final `Model` instances

The importer boundary is not responsible for:

- file watching
- registry replacement logic
- runtime manager ownership
- rendering behavior

### 2. Source Parsing Responsibilities

`bbmodel` and Bedrock source packages should only parse files into typed source objects.

Required behavior:

- file/JSON parsing
- codec-backed validation of raw source structure
- no `Model` creation
- no texture repack
- no runtime registry publication

For Blockbench specifically:

- reuse field semantics already established by `../blockbench`
- prefer keeping source record shape and parse behavior compatible with `../blockbench` unless Eyelib has a concrete reason to differ
- use `../blockbench` sample models and parser expectations as regression fixtures where practical

This means `BBModel.from(...)` should stop constructing runtime `Model` data and should no longer perform `Textures.repackModels(...)` as part of source object creation.

### 3. Shared Construction Flow

The unified import flow should be:

1. Parse source file into a source model object.
2. Adapt source model object into importer-local construction input.
3. Run post-processing required by import semantics.
4. Build final `Model`.
5. Publish through `ClientAssetRegistry`.

The importer-local construction input should be private to the importer package. It may be expressed as package-private records or simple internal builder data structures. It should not become a new public runtime model layer.

That construction input needs to carry only data the final `Model` builder actually needs:

- model name
- visible box
- bones
- parent-child relationships
- pivot and rotation
- cube geometry
- per-face UVs
- locator data
- optional material-related fields already supported by `Model`
- texture identity needed for repack before final face UV remap

### 4. Locator Handling

`ModelLocator` is part of the runtime intermediate model and must be preserved where source data can support it.

Importer rules:

- preserve locator-relevant structure during conversion
- build the final `ModelLocator` tree as part of final `Model` construction
- do not silently replace source locator data with an always-empty locator tree

### 5. Texture Mesh Handling

If source data contains texture-mesh-like geometry that maps naturally into Eyelib runtime rendering, convert it into normal importer bone/cube input rather than expanding `Model`.

Recommended handling:

- normalize such input into a dedicated synthetic or grouped bone containing many cubes when necessary
- keep this as importer logic
- avoid adding a new runtime representation solely for this import path

### 6. Blockbench Multi-Texture Repack

Blockbench can assign multiple textures to a single source model. Eyelib should continue to support this by repacking at load time so the final runtime model can render through one texture flow without introducing extra runtime model naming or synchronized multi-model complexity.

The repack strategy should move from source parsing into importer post-processing.

Required behavior:

1. Parse the full Blockbench model without runtime side effects.
2. Convert all source geometry into importer-local bone/cube/face data, preserving which source texture each face uses.
3. Run texture repack after conversion.
4. Remap face UVs into the repacked texture space.
5. Construct the final `Model` from the repacked result.

This keeps the workaround, but places it at the correct boundary.

### 7. Manager Import Integration

`ManagerResourceImportPlanner` should no longer route model imports straight into a format-specific runtime loader.

Instead it should:

- detect the source file type
- call the unified importer entrypoint
- publish the resulting `Map<String, Model>` through `ClientAssetRegistry`

This keeps import orchestration in the manager tooling seam while moving conversion logic into the model domain.

### 8. Error Handling

Importer failures should be grouped by stage so tooling logs and future UI feedback are useful.

Stages:

- parse error
  - invalid JSON
  - invalid codec structure
- conversion error
  - invalid parent references
  - invalid geometry relationships
  - unsupported source constructs that cannot be mapped safely
- post-process error
  - texture repack failure
  - UV remap failure
- publish error
  - unexpected registry/publication failure

The planner should log stage-aware failures instead of collapsing everything into generic loader exceptions.

### 9. Testing Strategy

Tests should focus on importer behavior first, not the full manager/reload flow.

Required coverage:

- Blockbench to `Model`
  - minimal hierarchy
  - pivot and rotation
  - cube geometry
  - per-face UV
- Bedrock to `Model`
  - same runtime-shape expectations as Blockbench where equivalent
- locator preservation
- visible box handling
- multi-texture Blockbench repack
  - texture merge succeeds
  - UVs are remapped correctly
- compatibility coverage against `../blockbench`
  - shared `.bbmodel` fixtures parse successfully in both projects
  - important parsed source fields match expected interpretation before Eyelib runtime conversion
- failure paths
  - bad JSON
  - invalid texture references
  - invalid parent hierarchy
  - post-process failures

The preferred TDD sequence is:

1. write failing importer tests for current `bbmodel` regressions
2. add compatibility tests using `../blockbench` fixtures and expected source semantics
3. replace the implementation behind those tests
4. add Bedrock parity tests
5. add repack regression tests

### 10. Documentation Impact

Update docs if paths or responsibilities become materially clearer:

- `MODULES.md`
- package README files under touched model/import paths
- manager tooling README if its import responsibility description changes

At minimum, the docs should reflect that model source parsing and model conversion are separate responsibilities.

## Implementation Notes

- Prefer narrow edits and keep format-specific logic contained to source adapters.
- Preserve the existing manager/registry publication seam.
- Do not introduce new `Eyelib.java` singleton reach-throughs.
- Keep generated-code rules untouched.

## Verification

- Run focused importer tests first.
- Run the relevant Gradle test task for the added importer coverage.
- Run a project build step with exit code `0` before claiming completion.
- If null-safety-sensitive code changes are introduced, also run `./gradlew nullawayMain`.
