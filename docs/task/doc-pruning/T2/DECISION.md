# T2 决策记录 · 已完成计划与孤儿草稿清理

## D3 决策：design 草稿删除安全性

ROADMAP Phase 5/6 否决理由**自洽**（line 70-71, 113, 182-192）：
- Phase 5 "Superseded — replaced by unified bytecode compiler"
- Phase 6 "Superseded — removed as over-engineered"

否决理由已自洽记录在 ROADMAP，不依赖 design 草稿。删除对应草稿安全。

## PLAN.md 三处事实错误修正

执行阶段发现 PLAN.md 对 T2 的规划基于错误事实，按长任务流程修正：

1. **host-injection/adapter 草稿对应 Phase 4（Blocked 活跃），不是 Phase 6（Removed）**。PLAN line 47 错误归类。Phase 4 是 Host and query bridge，仍活跃。→ 保留这两个草稿。
2. **refactor-plan/ 被活跃引用**：`molang-refactor-supervisor/SKILL.md:12` + ROADMAP 6 处引用（line 7,15,27,65,80,96）。且 Phase 1/3/4 仍活跃（Current/partial / Blocked）。→ 不删 refactor-plan/。
3. **design 被 ROADMAP 列为第三级权威源**（line 28 "design drafts for rationale and vocabulary"）。保守删除。

## 实际删除清单（11 文件 + 1 空目录）

### 零引用孤儿（7 文件）
- `docs/tests/eyelib-particle-test-plan.md`
- `docs/tests/eyelib-molang-importer-material-test-plan.md`
- `docs/tests/eyelib-animation-test-plan.md`
- `docs/tests/`（空目录）
- `.opencode/skills/eyelib/references/repomix-module-tokens.md`（skill 自述过期）
- `docs/gap-analysis/GAP-ANALYSIS-behavior_entities.md`（0005 引用的是 vanilla 概念名，非文件引用）
- `docs/gap-analysis/GAP-ANALYSIS-loot-trading-spawn-structure.md`
- `docs/molang/design/molang-formal-model.md`（孤儿，未被 README 索引，refs=0）

### Phase 6 superseded 草稿（4 文件）
- `docs/molang/design/compatibility-policy-pack-draft.md`
- `docs/molang/design/policy-pack-selection-configuration-draft.md`
- `docs/molang/design/runtime-specialization-contract-draft.md`
- `docs/molang/design/specialization-cache-contract-draft.md`

保留：compatibility-semantics-matrix.md（兼容性参考材料，跨 Phase 价值）；corpus-reporter/linter（Phase 1 活跃）；host-injection/adapter（Phase 4 Blocked）。

## 级联同步（4 文件）

- `design/README.md`：Documents 删 4 行 + Reading Order 删 4 条重编号（19→15）；corpus-linter-runner 描述去掉 "policy packs"。
- `corpus-linter-runner-draft.md:10`：删 `compatibility-policy-pack-draft.md` 引用行。
- `corpus-reporter-output-format-draft.md:10`：删 `policy-pack-selection-configuration-draft.md` 引用行。
- `shared-vocabulary-and-phase-ownership-draft.md:10`：删 `runtime-specialization-contract-draft.md` 引用。

## 验收结果

- grep 确认 4 个 Phase 6 草稿 + formal-model 在活跃文档中零引用残留（仅 PLAN.md 记录性提及）。
- design/README.md Documents(15) 与 Reading Order(15) 一致。
- tests/ 文件名仅 PLAN.md 引用（任务文档，符合预期）。
- gap-analysis/ 目录仍存在（保留 STRUCTURE_FORMAT_COMPARISON.md + blocks-items-recipes.md），docs/README.md:15 目录链接不断。

## 发现的后续项

### → T3：ROADMAP refactor-plan 断链
ROADMAP 引用了不存在的 refactor-plan 文件名：
- line 65: `refactor-plan/00-overview-and-boundaries.md`（实际无 00- 文件）
- line 96: `refactor-plan/04-host-and-query-bridge.md`（实际是 `04-documentation-verification.md`）

### → T7：ADR-0011 目录树整体过期
`0011-documentation-design-baseline.md:30-44` 目录树多项与现实不符（reference/ 位置、guides/ 不存在、tests/ 已删、module-map.md 路径、0001-0011 范围）。这是独立于 T2 的预存问题，需在 T7 决定是整体重写还是加"当前结构见 docs/README.md"标注。
