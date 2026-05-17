# Key Architecture Decisions

Historical architectural decisions from the module separation milestones.

| Decision | Rationale | Validated In |
|----------|-----------|-------------|
| @ClientSmoke annotation | Decouple test from business code | v1.0 |
| iris-tutorial-mod auto-start pattern | Proven reference implementation | v1.0 |
| Independent Gradle subproject for each seam | Build isolation | v1.0 |
| Runtime dependency, not compile | Prevent framework leakage | v1.0 |
| Unconditional localRuntime | No Gradle property required; runtime control via isEnabled() | v1.1 |
| System property override bridge | isEnabled()/shouldExitAfterSmoke() check System.getProperty first, ForgeConfigSpec fallback | v1.1 |
| JUnit XML alongside JSON | Standard CI integration format | v1.1 |
| Conditional halt(0)/halt(1) | Gradle exit code propagation | v1.1 |
| `eyelib-particle` as real module boundary | Particle responsibilities spread across root runtime, importer schema, command/network integration, manager publication | v1.2 |
| `eyelib-util` as Forge module | May depend on MC/Forge; not artificially constrained to be pure Java | v1.3 |
| `io.github.tt432.eyelibutil` namespace | Avoid split packages with root and sibling modules | v1.3 |
| Single-consumer utility routing | Domain-specific code belongs to functional owners, not the shared util module | v1.3 |
| v1.4 Phase ordering: Analysis→Rename→Data→Capability→Docs | Research-backed dependency chain | v1.4 |
| Atomic module rename | settings.gradle + build.gradle + .idea/ + directory in one operation before Gradle sync | v1.4 |
| Capability split strategy | Data/codec types move to attachment; runtime owners stay in root with distinct namespace | v1.4 |
| ModelBakeInvalidationHooks bridge | Prevents preprocessing→root reverse dependency | v1.4 |
| Full-suite verification | test + nullawayMain + rebuild replaces standalone Nyquist per phase | v1.4 |
| eyelib-model as canonical model data module | Extract Model, GlobalBoneIdHandler, VisibleBox, locator tree, and model tree interfaces from eyelib-importer into a dedicated Forge subproject to separate data ownership from parsing responsibility | v1.5 |
| Dissolve eyelib-preprocessing | Move bake/reload/loader/animation-bridge classes to their natural functional owners (root and eyelib-animation); delete dead ParticleFlipbook and BakedModels code | v1.5 |
