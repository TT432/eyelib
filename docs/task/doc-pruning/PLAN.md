# 文档剪枝规划

> 任务：对仓库文档系统整体优化剪枝，保持架构不退化。
> 解决 4 类问题：① 演化遗留不应持久化 ② 前后不一致 ③ 代码/注释已表达但文档重复 ④ 与事实有偏差。

## 问题建模

仓库经历了 ADR-0010(六边形) → 0014(扁平合并) → 0015(多版本) → 0016(库隔离) → 0018(IQF) 连续架构变迁。每次变迁产生三类熵：
- **过渡产物**：handoff / audit / refactor-plan（一次性，完成后变过期快照）
- **事实漂移**：旧包名(`eyelib-xxx` 子项目)、旧路径、旧模块数残留在当前陈述里
- **索引滞后**：ADR 索引、Skill 索引、版本表述未跟上实际

剪枝 = 删过渡产物 + 修事实漂移 + 补索引滞后，以**当前代码/构建事实**为唯一基准。

## 基准事实（执行依据，非待核实）

| 项 | 当前事实 | 来源 |
|---|---|---|
| 模块数 | 18 | MODULES.md(自动生成) |
| 版本节点 | 1.20.1(active) / 1.21.1 / 26.1.2 三节点 | settings.gradle；ADR-0015 |
| 26.1.2 状态 | ADR-0015 Phase 4「声明未实现」 | ADR-0015 §9, Status |
| ADR 总数 | 19 文件(0001-0018 + 0013a) | docs/decisions/ |
| 项目 Skill 总数 | 14 | .opencode/skills/ |
| Port 实际位置 | `<module>/port/`(material/port, molang/port) | glob 确认 |
| ADR-0015 设计位置 | `bridge/<feature>/`(设计稿，与实现不同) | ADR-0015 §3 |

**关键张力**：Port 实际在 `<module>/port/`，但 ADR-0015 §3 设计是 `bridge/<feature>/`，design/README 说在 `bridge/molang/`。三者不一致——以**代码实际位置**为准，文档向代码对齐。

## 子任务划分

每个子任务文件集互不重叠、自带引用同步，互相孤立。

### T1 · 过期快照剪枝
**文件集**：`docs/audits/2026-06-19-documentation-taste-review.md`、`docs/stonecutter-migration-handoff.md`
- audit：发现已全部修复，现为过期快照，自贡献全部 10 处 .hermes 残留。
- handoff：2026-06-18 交接快照，被 `docs/README.md:19` 导航引用。
- **决策点 D1**：audit 作为「曾做过审计」的记录是否保留？→ 否。其价值在发现，发现已落地修复，保留过期快照误导大于告知。
- **决策点 D2**：handoff 的 Stonecutter 迁移状态信息是否仍有增量？→ 核实 ADR-0015 是否已内联其全部有效信息；是则删，否则迁移增量到 ADR-0015 后删。
- 同步：删 handoff 后移除 `docs/README.md:19` 导航行。
- 验收：grep 全仓库无对两文件的 `.md)` 引用残留。

### T2 · 已完成计划与孤儿草稿清理
**文件集**：`docs/molang/refactor-plan/`(P1-P6 全✅)、`docs/molang/design/` 部分草稿、`docs/gap-analysis/` 零引用项、`docs/tests/` 零引用项、`references/repomix-module-tokens.md`
- refactor-plan：P1-P6 全 ✅(ROADMAP Refactor Record 确认)，结论已入 ROADMAP。
- **决策点 D3**：design 草稿删除安全性——ROADMAP 不引用草稿文件名，但草稿承载「Phase 5/6 为何 superseded」的推理。→ 逐个核实：ROADMAP Phase 5/6 段落是否自洽说明否决理由；自洽则删 superseded 对应草稿，否则保留该草稿或把理由迁入 ROADMAP。
- superseded 对应候选(Phase 5 policy/specialization)：compatibility-policy-pack / policy-pack-selection / runtime-specialization / specialization-cache / compatibility-semantics-matrix。
- removed 对应候选(Phase 6 host injection)：host-injection-api / host-adapter-registry。
- **孤儿**：molang-formal-model.md(refs=0)。
- **零引用 gap-analysis**：behavior_entities、loot-trading-spawn-structure(核实引用)。
- **零引用 test-plan**：particle / molang-importer-material / animation。
- repomix-module-tokens.md：skill 自述「结构数据已过期」。
- 同步：refactor-plan/06 引用 design/shared-vocabulary——两文件集同时清理时此引用随 refactor-plan 一起消失，无断链。
- 验收：grep 无断链；ROADMAP/ADR 未引用被删文件。

### T3 · 索引一致性修正
**文件集**：`docs/concepts/architecture.md`、`docs/README.md`、`AGENTS.md`
- architecture.md ADR 索引补 0016/0017(现仅 0001-0015+0018)。
- architecture.md 模块数 19→18。
- docs/README.md Skill 索引补全 7 个缺失(codec-design/mixin-writing/molang-refactor-supervisor/progressive-exploration/smoke-test/testing/unit-test)。
- AGENTS.md Repository Shape「当前 1.20.1 legacyforge + 1.21.1 neoforge」→ 补 26.1.2 为第三计划节点(Phase 4 pending)。
- 验收：三索引与基准事实表逐项一致。

### T4 · domain-module-map.md 修正
**文件集**：`docs/architecture/domain-module-map.md`
- 通篇旧子项目名(eyelib-bridge/material/molang/util/model/animation/behavior/particle)→ 当前包名。
- Port 路径以代码实际位置(`<module>/port/`)对齐，标注与 ADR-0015 §3 设计(bridge/<feature>/)的差异。
- 提取状态表更新(标 2026-06-08 已过期)。
- **决策点 D4**：若 architecture.md 已自洽覆盖模块/Port 信息，本文件是否冗余？→ 若冗余则删并改 architecture.md 的指向(line 95)；若仍有 Port 清单独有价值则重写保留。
- 同步：architecture.md:17、docs/README.md:17 指向本文件。
- 验收：路径与 glob 结果一致；包名与 MODULES.md 一致。

### T5 · 旧包名事实偏差修正
**文件集**：references 中「当前陈述」类文档、相关 ADR 正文
- 区分「历史排查记录(保留)」vs「当前事实陈述(改)」：reference 里「当时排查时包名叫 X」是历史，保留；「当前结构是 X」是当前陈述，改。
- 候选：forge-transformer-network-channel.md(12)、network-cce-classloader-root-cause.md(8)、cross-classloader-network-diagnostics.md(5)。
- 0014-flat-merge.md(18) 描述改名本身，保留。
- 验收：当前陈述句无旧子项目名；历史叙述句保留。

### T6 · 重复内容排查
**文件集**：抽样
- 已确认：design/README.md:47 ≈ molang-ast-and-semantics-draft.md:21(bridge/molang/ 路径重复)。
- 抽样原则：reference 文档若纯复述 package-info/代码签名则违反 AGENTS.md「信息增量」原则。
- 产出：重复清单 + 处理(去重，保留信息增量更高的版本)。
- 验收：抽样文件无整段重复。

### T7 · 最终验证
- grep 全仓库 `.md)` 引用无断链。
- ADR 索引(docs/README + architecture.md)与 decisions/ 一致。
- MODULES.md 模块数与 architecture.md 一致。
- Skill 索引与 .opencode/skills/ 一致。
- 文档路径引用全部 resolve。

## 决策记录

执行阶段遇模棱两可时，选择更好的方案并记入对应子任务文档 `docs/task/doc-pruning/<子任务>/DECISION.md`。

## 执行顺序

T1 → T2(依赖 D2 对 handoff 结论) → T3 → T4 → T5 → T6 → T7。
T3/T4/T5 可在 T1/T2 后并行，但为控制上下文采用串行。
