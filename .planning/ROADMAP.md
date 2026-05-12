# Roadmap: Eyelib Module Separation

## Milestones

- ✅ **v1.0** — Phases 1-4 (shipped 2026-05-07)
- ✅ **v1.1 ClientSmoke 全自动化** — Phases 5-7 (shipped 2026-05-08)
- ✅ **v1.2 真正实现 eyelib-particle 的模块分离** — Phases 8-14 (shipped 2026-05-09)
- ✅ **v1.3 分离 eyelib-util 模块** — Phases 15-21 (shipped 2026-05-10)
- ✅ **v1.4 结构清理** — Phases 22-26 (shipped 2026-05-12)

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

<details>
<summary>✅ v1.4 结构清理 — Phases 22-26 (SHIPPED 2026-05-12)</summary>

Full details: `.planning/milestones/v1.4-ROADMAP.md`

- [x] **Phase 22: Analysis & Quick Wins** — 删除无效接口、修正数据库路径，建立干净基线
- [x] **Phase 23: Module Rename** — 重命名 eyelib-processor → eyelib-preprocessing 并转为 Forge 模块
- [x] **Phase 24: Data Relocation** — bake 代码、controller 数据类、其余纯数据类归位到正确模块
- [x] **Phase 25: Capability Migration** — capability 数据/codec 类型迁移至 eyelib-attachment
- [x] **Phase 26: Documentation & Final Verification** — 重写过时文档并通过全部验证关卡

</details>

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1-4 | v1.0 | 10/10 | Complete | 2026-05-07 |
| 5-7 | v1.1 | 5/5 | Complete | 2026-05-08 |
| 8-14 | v1.2 | 22/22 | Complete | 2026-05-09 |
| 15-21 | v1.3 | 24/24 | Complete | 2026-05-10 |
| 22. Analysis & Quick Wins | v1.4 | 1/1 | Complete | 2026-05-11 |
| 23. Module Rename | v1.4 | 1/1 | Complete | 2026-05-12 |
| 24. Data Relocation | v1.4 | 1/1 | Complete | 2026-05-12 |
| 25. Capability Migration | v1.4 | 1/1 | Complete | 2026-05-12 |
| 26. Documentation & Final Verification | v1.4 | 1/1 | Complete | 2026-05-12 |
