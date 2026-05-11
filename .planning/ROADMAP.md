# Roadmap: Eyelib Module Separation

## Milestones

- ✅ **v1.0** — Phases 1-4 (shipped 2026-05-07)
- ✅ **v1.1 ClientSmoke 全自动化** — Phases 5-7 (shipped 2026-05-08)
- ✅ **v1.2 真正实现 eyelib-particle 的模块分离** — Phases 8-14 (shipped 2026-05-09)
- ✅ **v1.3 分离 eyelib-util 模块** — Phases 15-21 (shipped 2026-05-10)
- 🚧 **v1.4 结构清理** — Phases 22-26 (planning)

## Phases

<details>
<summary>✅ v1.0 — Phases 1-4 (SHIPPED 2026-05-07)</summary>

Full details: `.planning/milestones/v1.0-ROADMAP.md`

- [x] **Phase 1: Module Scaffolding + Config + Annotation Discovery**
- [x] **Phase 2: State Machine + World Lifecycle + Stabilization**
- [x] **Phase 3: Screenshot Capture + Auto-Exit**
- [x] **Phase 4: Test Execution + Report Generation**

</details>

<details>
<summary>✅ v1.1 ClientSmoke 全自动化 — Phases 5-7 (SHIPPED 2026-05-08)</summary>

Full details: `.planning/milestones/v1.1-ROADMAP.md`

- [x] **Phase 5: Gradle Run Configuration & Classpath** (1/1 plan) — completed 2026-05-08
- [x] **Phase 6: Config Override Bridge & State Machine Fixes** (2/2 plans) — completed 2026-05-08
- [x] **Phase 7: Verification & Polish** (2/2 plans) — completed 2026-05-08

</details>

<details>
<summary>✅ v1.2 真正实现 eyelib-particle 的模块分离 — Phases 8-14 (SHIPPED 2026-05-09)</summary>

Full details: `.planning/milestones/v1.2-ROADMAP.md`

- [x] **Phases 8-14:** `:eyelib-particle` module boundary, runtime extraction, loading/publication rewire, command/network integration, final verification.

</details>

<details>
<summary>✅ v1.3 分离 eyelib-util 模块 — Phases 15-21 (SHIPPED 2026-05-10)</summary>

Full details: `.planning/milestones/v1.3-ROADMAP.md`

- [x] **Phases 15-21:** `:eyelib-util` leaf Forge module extracted root/core shared utilities, centralized selected submodule helpers, drained root/core util Java sources, and passed final audit.

</details>

### 🚧 v1.4 结构清理 (In Progress)

**Milestone Goal:** 消除残留的非模块化代码放置、纠正命名语义、清理无效接口和过时文档，使模块边界与代码实际归属一致。

- [ ] **Phase 22: Analysis & Quick Wins** — 删除无效接口、修正数据库路径，建立干净基线
- [ ] **Phase 23: Module Rename** — 重命名 eyelib-processor → eyelib-preprocessing 并转为 Forge 模块
- [ ] **Phase 24: Data Relocation** — bake 代码、controller 数据类、其余纯数据类归位到正确模块
- [ ] **Phase 25: Capability Migration** — capability 数据/codec 类型迁移至 eyelib-attachment
- [ ] **Phase 26: Documentation & Final Verification** — 重写过时文档并通过全部验证关卡

## Phase Details

### Phase 22: Analysis & Quick Wins
**Goal**: 删除零引用的无效接口并修正数据库路径，建立干净的结构清理基线  
**Depends on**: Nothing (v1.4 first phase)  
**Requirements**: CODEQ-01, CODEQ-02  
**Success Criteria** (what must be TRUE):
  1. 所有删除的接口经 ide_find_references + ide_find_implementations 确认零引用（项目内和依赖库均无引用）
  2. 全量项目 rebuild 通过（jetbrain_build_project rebuild=true，零编译错误）
  3. 全部已有 54+ 测试通过（jetbrain_run_gradle_tasks ["test"]，零失败）
  4. InstrumentDatabase 路径指向 `.cache` 目录，项目根目录不再创建数据库文件
  5. 被删除接口的名称在全量 .java、.json、.toml 文件中出现次数为零（G5 import purge）
**Plans**: TBD

### Phase 23: Module Rename
**Goal**: 原子重命名 eyelib-processor → eyelib-preprocessing 并完成 plain-JVM → Forge 模块转换，解除 bake 代码迁移的前置阻塞  
**Depends on**: Phase 22  
**Requirements**: MOD-01, MOD-02  
**Success Criteria** (what must be TRUE):
  1. settings.gradle 显示 `include("eyelib-preprocessing")`，无 `eyelib-processor` 引用
  2. 所有 build.gradle 的依赖声明全部指向 `:eyelib-preprocessing`（root 4处 + eyelib-importer 1处 + 自身 archivesName）
  3. .idea/ 项目文件反映新模块名；Gradle sync 成功（jetbrain_sync_gradle_projects 正常完成）
  4. 全量项目 rebuild（G2）通过
  5. `eyelib-processor` 字符串在所有 .gradle、.xml、.java 文件中出现次数为零（G5 import purge）
**Plans**: TBD

### Phase 24: Data Relocation
**Goal**: 将 bake 代码、controller 数据定义和其余位置不正确的纯数据类移至对应功能模块  
**Depends on**: Phase 23  
**Requirements**: DATA-01, DATA-02, DATA-03  
**Success Criteria** (what must be TRUE):
  1. `client/model/bake/` 目录为空；所有 bake 文件位于 `eyelib-preprocessing` 模块内
  2. 基岩版 animation controller 数据定义（BrAc* 类型）位于 `eyelib-importer` 模块内
  3. 所有已识别的错位纯数据类位于正确功能模块；旧路径下无残留 .java 文件（G6 file purge）
  4. 全量项目 rebuild（G2）通过，零陈旧 import 报错
  5. 全部测试通过（G4）
**Plans**: TBD

### Phase 25: Capability Migration
**Goal**: 将 capability 数据/codec 类型迁移至 eyelib-attachment，保持 Forge 运行时 wiring 在 root mc/impl/ 不动  
**Depends on**: Phase 24  
**Requirements**: MOD-03  
**Success Criteria** (what must be TRUE):
  1. capability 数据/codec 类型位于 `eyelib-attachment`，使用独立命名空间 `io.github.tt432.eyelibattachment`（避免 split package）
  2. Forge capability 运行时 wiring（event subscription、provider registration）保留在 root `mc/impl/` 且功能正常
  3. capability 相关测试全部通过（AnimationComponentSerializableInfoTest、RenderControllerComponentTextureStateTest、AnimationComponentRuntimeInvalidationTest、CommonRuntimeUpdaterTest 等）
  4. 全量项目 rebuild（G2）通过；零陈旧 `import io.github.tt432.eyelib.capability.` 引用（G5 import purge）
  5. 不存在 split package — capability 包仅存在于单一模块
**Plans**: TBD

### Phase 26: Documentation & Final Verification
**Goal**: 重写过时文档以反映 v1.4 模块拓扑，并通过全部验证关卡  
**Depends on**: Phase 25  
**Requirements**: DOCS-01  
**Success Criteria** (what must be TRUE):
  1. 根 README.md 描述当前 v1.4 模块拓扑，模块名正确（含 `eyelib-preprocessing`）
  2. 所有包级 README 与实际文件系统状态一致；空包目录不留 README
  3. MODULES.md 更新为最新模块清单与依赖关系（含 `eyelib-preprocessing` 和 capability 迁移后的 attachment 职责）
  4. 全量验证关卡序列通过：G2（rebuild）+ G3（NullAway）+ G4（全部测试）
  5. docs/architecture/01-module-boundaries.md 反映所有边界变更
**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1-4 | v1.0 | 10/10 | Complete | 2026-05-07 |
| 5-7 | v1.1 | 5/5 | Complete | 2026-05-08 |
| 8-14 | v1.2 | 22/22 | Complete | 2026-05-09 |
| 15-21 | v1.3 | 24/24 | Complete | 2026-05-10 |
| 22. Analysis & Quick Wins | v1.4 | 0/TBD | Not started | — |
| 23. Module Rename | v1.4 | 0/TBD | Not started | — |
| 24. Data Relocation | v1.4 | 0/TBD | Not started | — |
| 25. Capability Migration | v1.4 | 0/TBD | Not started | — |
| 26. Documentation & Final Verification | v1.4 | 0/TBD | Not started | — |
