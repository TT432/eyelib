# Draft: Render Regression Diff 839b9e5b

## Requirements (confirmed)
- rendering has errors after a refactor
- known-good commit is `839b9e5b`
- need a diff-based check to identify where the problem likely came from
- concrete symptom: texture/UV mapping is wrong
- affected model type: `BrModel`
- render path: `EntityRenderSystem`

## Technical Decisions
- compare current HEAD against `839b9e5b`
- focus investigation on rendering-adjacent modules first: `client/render`, `client/model`, `client/loader`, capability data used by rendering, `util/client`, and render-state sync
- produce a decision-complete work plan rather than code changes
- prioritize texture composition / UV repack / render-controller material selection over sync-side investigation

## Research Findings
- `docs/architecture/01-module-boundaries.md`: render-related refactor pressure points include loader publication changes, lookup seams, and reduced `Eyelib.java` reach-through
- `docs/architecture/02-side-boundaries.md`: render-state sync should flow through dedicated client runtime services, not direct packet/GUI coupling
- `src/main/java/io/github/tt432/eyelib/client/README.md`: core client runtime domains are render/model/animation/particle with loader and manager hotspots
- `839b9e5b..HEAD` high-risk diff clusters: `client/render/visitor/*`, `client/render/RenderHelper.java`, `client/render/RenderParams.java`, `client/model/Model.java`, `client/model/ModelRuntimeData.java`, `capability/component/AnimationComponent.java`, `network/EyelibNetworkManager.java`, `network/NetClientHandlers.java`, `client/loader/*` registry publication changes, `util/client/Textures.java`
- likely regression classes from diff: model interface→record conversion, runtime bone-data structure rewrite, render visitor signature simplification, `BoneRenderInfos` removal, loader publication moved to `ClientAssetRegistry`, attachment system moved to custom `DataAttachmentType`, packet transport rewritten to `SimpleChannel`
- verification infrastructure: no effective `src/test` tree, GameTest configured but apparently unused, primary validation path is `./gradlew compileJava` then `./gradlew runClient`; RenderDoc path exists via `./gradlew runWithRenderDoc`
- symptom narrowing: because the issue is mapping-only on `BrModel` through `EntityRenderSystem`, the most likely regression surface is `util/client/Textures.java` repack logic, then `client/render/controller/RenderControllerEntry.java`, then render visitor/params fallback behavior rather than attachment/network state
- narrowed investigation result: `Textures.repackModels()` is only referenced by `BBModel`, so it is not on the primary `BrModel -> EntityRenderSystem` path
- primary suspected regression for `BrModel` UV mapping is in `src/main/java/io/github/tt432/eyelib/client/model/bedrock/BrCube.java`: the `down` face box-UV mapping changed between `839b9e5b` and `HEAD` from a vertically flipped layout to a horizontally shifted/flipped layout
- concrete diff signal in `BrCube.parse(...)`: old `down` mapping used `new Vector2f(uv.x + uvSizeX + uvSizeZ, uv.y + uvSizeZ), new Vector2f(uvSizeX, -uvSizeZ)`; current code uses `new Vector2f(uv.x + uvSizeX + uvSizeZ + uvSizeX, uv.y), new Vector2f(-uvSizeX, uvSizeZ)`
- because `BrCube` feeds `BrBone.createBone()` -> `BrModelEntry.createModel()` -> `ModelComponent.getModel()` -> `EntityRenderSystem.renderComponents()`, this is on the exact live path the user described

## Open Questions
- whether the user wants only root-cause localization or also a rollback/fix strategy in the plan

## Scope Boundaries
- INCLUDE: diff analysis, likely regression surface, verification paths, investigation plan
- EXCLUDE: source-code edits, direct bug fix, runtime mutation
