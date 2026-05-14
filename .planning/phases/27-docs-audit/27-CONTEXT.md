# Phase 27: DOCS — 文档审计与修正 - Context

**Gathered:** 2026-05-12
**Status:** Ready for planning
**Mode:** Smart Discuss (autonomous — batch proposal)

<domain>
## Phase Boundary

项目结构文档准确反映当前模块拓扑，无过时引用，无无效文档，缺失模块有对应说明。

### Requirements
- DOCS-01: 全文搜索 `eyelib-processor`（不含 `eyelib-preprocessing` 的完整匹配）返回 0 条生产代码结果
- DOCS-02: 全部 48 个 README.md 均已审计，空目录下无残留无效 README
- DOCS-03: 所有缺失结构文档的模块均已补充准确说明
- DOCS-04: MODULES.md 和 docs/ 架构文档准确反映 v1.5 完成后的模块拓扑
</domain>

<decisions>
## Implementation Decisions

### DOCS-01: eyelib-processor 引用清理
- **Production code**: 已验证 `src/` 下所有源文件已正确使用 `eyelib-preprocessing`
- **`.planning/` + `.sisyphus/`**: 规划文件和历史笔记保留旧名称不修改（历史记录）
- **`docs/architecture/migration/`**: 迁移文档记录历史过程，保留旧引用
- **`bin/`**: 已编译副本（随源码自动更新）
- **决策**: 生产代码已满足要求，仅需确认 `docs/architecture/` 主要文件使用新名称

### DOCS-02: README.md 审计
- 批量读取并审计 48 个README.md，确认内容准确
- 检查是否存在空目录残留无用 README
- 验证路径引用正确性

### DOCS-03: 缺失模块文档补充
- 对照 MODULES.md 清单，识别有代码无文档的模块
- 为缺失的模块创建准确的结构文档

### DOCS-04: MODULES.md + docs/ 同步
- MODULES.md 已提前更新过，需要确认完整性
- `docs/architecture/` 主要架构文档已使用 `eyelib-preprocessing`
- 确认无遗漏项
</decisions>

<code_context>
## Existing Code Insights

- Phase 23 已完成 `eyelib-processor` → `eyelib-preprocessing` 原子重命名
- MODULES.md 完整记录了所有模块，共51个模块条目
- `docs/architecture/01-module-boundaries.md` 准确反映当前边界
- `docs/index/repo-map.md` 是项目导航入口，需要核实
- 48 个 README.md 分布在 repo 各处
</code_context>

<specifics>
## Specific Ideas

### 需要检查的文档领域
1. `docs/architecture/` 下所有架构文档（00-control-spec, 02-side-boundaries, 03-generated-code-policy, 04-mc-debt-ledger, ARCHITECTURE-BLUEPRINT）
2. `docs/index/` 下所有索引（repo-map, client, molang, network, util）
3. 各子项目 README（eyelib-preprocessing, eyelib-importer, eyelib-molang, eyelib-material, eyelib-particle, eyelib-attachment, eyelib-util）
4. Root 级文档（AGENTS.md, README.md, MODULES.md）
</specifics>

<deferred>
## Deferred Ideas

None — all DOCS requirements are in scope for this phase.
</deferred>
