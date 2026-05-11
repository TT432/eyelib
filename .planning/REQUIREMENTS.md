# Requirements: Eyelib Module Separation

**Defined:** 2026-05-11
**Core Value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。

## v1.4 Requirements

结构清理——消除非模块化残留、纠正命名语义、清理无效接口和过时文档。

### Module

- [ ] **MOD-01**: eyelib-processor 重命名为 eyelib-preprocessing（含 namespace 变更）
- [ ] **MOD-02**: eyelib-preprocessing 转为 Forge 模块以容纳 MC 依赖的 bake 代码
- [ ] **MOD-03**: capability 包（`io.github.tt432.eyelib.capability`）全量迁移至 `:eyelib-attachment`，含 RenderData 和 Components

### Data

- [ ] **DATA-01**: `client/model/bake` 包迁移至 `eyelib-preprocessing`
- [ ] **DATA-02**: `render/controller` 下纯数据定义（BrAc* 等 bedrock animation controller 定义类）迁移至 `:eyelib-importer`
- [ ] **DATA-03**: 其余位置不正确的纯数据类归位到对应功能模块

### CodeQ

- [ ] **CODEQ-01**: 删除 `client/animation` 下零引用的无效接口
- [ ] **CODEQ-02**: 数据库文件创建路径从项目根目录改为 `.cache` 目录

### Docs

- [ ] **DOCS-01**: 重构过时的 README.md 为现状描述，无实质内容的空文件夹删除 README

## v2 Deferred

- Controller 独立模块拆分（deferred — v1.4 仅迁数据类，完整分拆需进一步分析）
- Instrument 子系统删除（deferred — 仅改数据库路径，模块功能保留）

## Out of Scope

| Feature | Reason |
|---------|--------|
| Controller 独立 Gradle 模块 | v1.4 仅迁数据定义到 importer，runtime 保留 root |
| Instrument 子系统删除 | 用户确认只改路径，模块功能继续保留 |
| 新增功能逻辑 | 纯结构性变更，不改变现有实现 |
| Molang parser 重构 | 另有独立 roadmap 和里程碑 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| MOD-01 | Phase 23 | Pending |
| MOD-02 | Phase 23 | Pending |
| MOD-03 | Phase 25 | Pending |
| DATA-01 | Phase 24 | Pending |
| DATA-02 | Phase 24 | Pending |
| DATA-03 | Phase 24 | Pending |
| CODEQ-01 | Phase 22 | Pending |
| CODEQ-02 | Phase 22 | Pending |
| DOCS-01 | Phase 26 | Pending |

**Coverage:**
- v1.4 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-11*
*Last updated: 2026-05-11 after milestone v1.4 requirements definition*
