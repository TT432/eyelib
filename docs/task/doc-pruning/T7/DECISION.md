# T7 · 最终验证

## 验证项与结果

### 1. 断链检查（13 个被删文件名）✅
grep 所有被删文件名在活跃文档中的引用：
- `documentation-taste-review|migration-handoff|particle-test-plan|molang-importer-material-test-plan|animation-test-plan|repomix-module-tokens|molang-formal-model|compatibility-policy-pack-draft|policy-pack-selection-configuration-draft|runtime-specialization-contract-draft|specialization-cache-contract-draft`
- 2 个匹配均为术语/字段名（`behavior_entities` 是 Bedrock 规范术语 + JSON 字段名），非文件引用。
- 活跃文档零断链。

### 2. 索引一致性 ✅
| 检查项 | 期望 | 实际 |
|---|---|---|
| architecture.md ADR 索引 | 0001-0018 + 0013a | 完整 ✅ |
| architecture.md 模块数 | 18 | `共 18 个` ✅ |
| docs/README.md Skill 索引 | 14 | 14 ✅ |
| AGENTS.md 版本表述 | 三节点 | `注册 1.20.1、1.21.1、26.1.2 三节点` ✅ |
| design/README Documents | 15 | 15 ✅ |
| design/README Reading Order | 15 | 15 ✅ |

### 3. Markdown 链接 resolve ✅
提取 docs/ + .opencode/skills/ + AGENTS.md + MODULES.md 中 36 个唯一 .md 相对链接，全部 resolve。

### 4. ADR-0011 目录树（预存问题）✅
ADR-0011 Decision 部分的目录树（line 29-45）是 2026-06-09 设计快照，与当前结构严重不符（`reference/` 迁移、`pitfalls/`/`guides/`/`tests/` 移除、`module-map.md` 重命名）。核心决策（Diátaxis 四象限）仍有效，不应改原文。处理：在目录树代码块后加 `> **注**` 标注，说明是历史快照，指向 `docs/README.md` 和 `MODULES.md` 为当前权威源。

### 5. PLAN.md 问题清单覆盖率 ✅
| PLAN 问题 | 处理任务 | 状态 |
|---|---|---|
| A1 audit | T1 | ✅ 删除 |
| A2 handoff | T1 | ✅ 删除 + 增量迁移 |
| A3 design superseded | T2 | ✅ 删 4 个 Phase6 草稿 |
| A4 molang-formal-model | T2 | ✅ 删除 |
| A5 gap-analysis 零引用 | T2 | ✅ 删 2 个 |
| A6 tests/ | T2 | ✅ 删 3 个 + 空目录 |
| A7 repomix-module-tokens | T2 | ✅ 删除 |
| A8 refactor-plan | T2 | ✅ 保留（活跃引用） |
| B1 ADR 索引漏 0016/0017 | T3 | ✅ 补入 |
| B2 模块数 19→18 | T3 | ✅ 修正 |
| B3 domain-module-map 旧名 | T4 | ✅ 全文重写 |
| B4 Skill 索引 7→14 | T3 | ✅ 补 7 个 |
| C 重复内容 | T6 | ✅ design/README 去重 |
| D1 Port 路径 | T4 | ✅ 修正 |
| D2 旧包名残留 | T5 | ✅ 13 文件修正 |

## 改动清单（本任务 T7）
- `docs/decisions/0011-documentation-design-baseline.md`：目录树代码块后加历史快照标注。

**T7 状态: ✅ 完成。全部子任务 T1-T7 完成。**
