# T3 · 索引一致性修正

## 改动清单

### concepts/architecture.md
1. **模块数**：line 34 `共 19 个` → `共 18 个`（对齐 MODULES.md 实际 18 个模块包）
2. **ADR 索引补全**：在 0015 与 0018 之间补入 0016（库隔离标准）、0017（移除 migrate26Renames），索引现为 0001-0018 完整

### docs/README.md
1. **删 tests/ 导航行**：T2 删除了 docs/tests/ 全部内容，导航表 `| 测试计划 | tests/ |` 成断链，删除
2. **Skill 索引补全**：原仅列 7 个 eyelib-* skill，补入缺失 7 个：codec-design / mixin-writing / molang-refactor-supervisor / progressive-exploration / smoke-test / testing / unit-test（共 14 个，与 `.opencode/skills/` 实际目录一致）

### docs/molang/ROADMAP.md
1. **line 65 Phase 0 evidence**：删除 `refactor-plan/00-overview-and-boundaries.md` 引用（该文件不存在；Phase 0 boundaries 由同行的 ADR-0002 覆盖）
2. **line 96 M4**：`refactor-plan/04-host-and-query-bridge.md` → `refactor-plan/06-host-context-alignment.md`（实际 04 是 documentation-verification；refactor-plan 中 host 相关的是 06-host-context-alignment）

### AGENTS.md
1. **Repository Shape（line 11）**：`当前 1.20.1 legacyforge + 1.21.1 neoforge` → `注册 1.20.1(legacyforge,active)、1.21.1(neoforge)、26.1.2 三节点,详见 ADR-0015`
2. **Tooling Restrictions（line 139）**：version node 列表 `:1.20.1、:1.21.1` → `:1.20.1、:1.21.1、:26.1.2`

## 决策要点

### AGENTS.md 版本表述选词：用"注册"而非"当前维护"
ADR-0015 Status 明确"Phase 1 implemented — 1.20.1 node live; Phase 2+ pending"。1.21.1 和 26.1.2 均非完全 live。但 settings.gradle 已注册三节点，且 git log 有 26.1. EventBusSubscriber 三路条件化代码。选用"注册三节点"既反映 settings.gradle 事实，又不误导为"全部 live"。各节点 Phase 状态由 ADR-0015 承载，AGENTS.md 仅指向。

### ROADMAP line 96 指向 06 而非删除
`04-host-and-query-bridge.md` 不存在，但 M4 语境是"keep roadmap aligned with Phase 4 decisions"。refactor-plan 中唯一 host 相关文件是 `06-host-context-alignment.md`（P10 HostContext 对齐）。修正指向而非删除引用，保留 ROADMAP 与 refactor-plan 的交叉链接价值。

## 发现的后续项
- **→ T5**：`refactor-plan/README.md:54` 验证命令用旧子项目名 `:eyelib-importer:test :eyelib-preprocessing:test`（ADR-0014 flat-merge 后这些子项目不存在）
- **→ T7**：ADR-0011 目录树（line 30-44）整体过期（T2 已发现）
