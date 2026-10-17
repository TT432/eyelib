# ADR-0002: Module Boundaries

**Status:** Accepted (amended by [ADR-0014](0014-flat-merge.md) for single-project merge; further amended by [ADR-0016](0016-bridge-extraction-standard.md) for bridge extraction — version-specific MC moved from `attachment/`/`network/`/`client/`/`capability/`/`common/` into `bridge/`)  
**Context:** As Eyelib grew, clear module boundaries were needed to prevent dependency cycles and ownership ambiguity.  
**Decision:** Establish explicit ownership boundaries by package, with root owning cross-module orchestration.  
**Consequences:** Module boundaries are enforced by package naming convention and review, not by Gradle project isolation. All boundary-crossing changes require updating this document.

---

# Eyelib Module Boundaries

> ADR-0014 取消了 12 个 Gradle 子项目，所有源码合并到 root 单 Gradle 项目，统一到 `io.github.tt432.eyelib.<module>` 命名空间。模块边界现在由包名约定 + review 维护，不再由 Gradle project 隔离。

## 模块清单与归属

顶层模块清单、职责与数量由 [:MODULES.md:](../../MODULES.md) 自动生成（源：各 `*/package-info.java` 首段），是唯一事实源；本 ADR 不再复制该清单（历史上复制过，导致 `attachment/` 等已删除模块长期残留）。下方只记录**非显然的跨模块归属决策**：

- `importer/` 拥有 schema/codec 定义；`particle/runtime/ParticleDefinition` 与 `ParticleDefinitionAdapter` 是运行时定义与适配缝，允许 particle → importer 的单向依赖。
- `material/` 拥有 Bedrock 材质语义与 GL 状态；MC `RenderType` 的实际构造在 `bridge/material/`。
- `bridge/` 是六边形 adapter 层：实现 domain Port、承载所有 `//?`、Forge 注册（`@EventBusSubscriber` / `DeferredRegister` / `ICapabilityProvider`）以及 blaze3d pipeline/platform/systems 的使用（ADR-0016）。
- `client/` 是版本无关的 application 层（渲染、GUI、loader、manager、tooling）；Forge API/Hooks 在 `bridge/client/`。
- `capability/` 持有 attachment 运行时数据容器；capability 注册与运行时钩子在 `bridge/capability/`。
- `common/` 是版本无关的共享行为/运行时逻辑；服务端命令与 Forge 集成调试在 `bridge/common/`。
- `client/molang/` 持有与 root 耦合的 `MolangQuery`；Molang 引擎本身（`molang/`）零 MC 依赖，平台绑定在 `bridge/molang/`。
- `clientsmoke/` 是外部客户端烟雾测试框架，作为独立 Stonecutter 子模块经 `mavenLocal` 产物消费，不参与主构建（无 `includeBuild`）。

## Existing Patterns To Preserve
- Manager pattern in `src/main/java/io/github/tt432/eyelib/client/manager/`.
- Loader pattern in `src/main/java/io/github/tt432/eyelib/client/loader/`.
- Visitor pattern in `src/main/java/io/github/tt432/eyelib/client/render/visitor/`.
- Codec-heavy serialization approach across model, animation, particle, and Molang-related types.

## Boundary Rules
- Package boundaries define the real architecture. **Domain 层**模块包（`importer` / `behavior` / `util` / `model` / `molang` / `material` / `animation` / `particle` / `wintersky` / `snowstorm`）不得依赖编排层（`client`、`common`、`network`、`capability`、`track`、`bridge`、`mixin`、`smoke`、`debug`）。谓词与例外以 `ArchitectureTest` 为准（ADR-0016）。
- Root owns cross-module orchestration; version-specific Forge wiring (`EntityRenderSystem`, capability wiring, network registration, command registration, loader/reload lifecycle hooks, manager store infrastructure, server-side behavior-pack publication/synchronization) is hosted in `bridge/` per ADR-0016 §5 (application layer is version-agnostic).
- Bedrock behavior packs are server-side runtime data: root common loads and publishes them on server lifecycle events, while the client receives only network-synchronized behavior state needed for rendering/Molang queries.
- Feature-specific packets belong in feature packages where dependency edges allow.
- Runtime executors (animation, controller, particle) are runtime implementation details that stay with their owning package.
- Importer owns schema/codec definitions; root owns runtime adaptation and `NativeImage`/texture upload.
- Particle module loading/publication is owned by `ParticleDefinitionRegistry` and `ParticleResourcePublication`, keyed by `ParticleDefinition.identifier()`. Particle packet types live under `io.github.tt432.eyelib.bridge.network.particle`（需要 MC 类型，故在 ACL 层）。
- Particle command/network integration routes through `particle/runtime/ParticleCommandRuntime`（平台无关整形）；平台接线在 `bridge/`。
- Domain isolation (no `net.minecraft.*` imports in domain packages, no version-specific MC outside `bridge/`/`mixin/`/`smoke/`/`debug/`) is enforced by the ArchUnit freeze baseline committed at `archunit_store/<node>/` (see [ADR-0010](0010-hexagonal-architecture.md), [ADR-0014](0014-flat-merge.md), [ADR-0016](0016-bridge-extraction-standard.md); the `test` task fails if any rule's baseline grows).

## Breaking Refactor Rule
- Do not add new singleton reach-through methods to top-level `Eyelib`.
- Migrate remaining callers to domain-local read seams, then delete the legacy reach-through methods.
