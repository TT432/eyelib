# T5 · 旧包名事实偏差修正

## 判断标准

每处 `eyelib-<module>` 旧子项目名按二分法判定：

| 类别 | 判定 | 处理 |
|---|---|---|
| **当前陈述/操作指导** | 指导读者"现在应该怎么做" | 修正为包名 |
| **历史决策记录** | ADR Context/Decision 描述当时状态 | 保留（ADR 不可变） |
| **已标注的历史事件** | 标注日期 / 删除线 / "ADR-0014 前/后" | 保留 |
| **通用教学假想示例** | 讲解通用模式用的虚构结构 | 保留 |

## 修改清单（13 文件）

### 验证命令（1）
- `docs/molang/refactor-plan/README.md:54` — 删 `:eyelib-importer:test :eyelib-preprocessing:test`（ADR-0014 后无子项目 task）

### 当前操作指导包名修正（10）
- `rc-materials-discrepancies.md:19` — `eyelib-molang 包` → `molang 包`
- `bone-level-material-rendering.md:16` — 同上
- `hexagonal-port-extraction.md:51` — `放在 eyelib-util` → `放在 util 包`
- `ecs-architecture.md:30` — `// eyelib-bridge:` → `// bridge.molang 包:`（ComponentStore 实际在 bridge/molang/）
- `eyelib-debug/SKILL.md:188` — "新建 Forge 子项目" → "根模块"（flat-merge 后无子项目）
- `eyelib-debug/SKILL.md:190` — `移至 eyelib-util` → `移至 util 包`
- `eyelib-domain-extraction/SKILL.md` — 5 处：前置条件(line21)、任务示例(29)、枚举检查(43)、返回值类型(134)、踩坑标注(190 加删除线)
- `behavior-component-pitfalls.md:3` — `eyelib-behavior 组件` → `behavior 模块组件`
- `behavior-component-spec-location.md:5` — `eyelib-behavior 模块` → `behavior 模块`
- `behavior-runtime-testing.md:3` — 同上
- `domain-extraction-pitfalls.md:7,9,11` — Port 放置指导 + 编译依赖说明

### 过期结构数据/建议更新（2）
- `repomix-module-packing.md:20-42` — 删 14 子项目时代 token 表 → 保留一句估算（465K tokens）
- `repomix-module-packing.md:92,93` — delegate_task 示例旧名 → 当前
- `stonecutter-multiversion-patterns.md:148` — "已是 10 子模块" → "ADR-0014 后 flat-merge 单 project"

### 历史快照标注（1）
- `eyelib-hexagonal-gates/SKILL.md:120-129` — 验收矩阵加"2026-06 历史快照"标题 + ArchUnit 已由 ADR-0014 删除说明；模块名 eyelib-xxx → xxx

## 保留项（历史记录，不改）

- **ADR（0008/0009/0010/0014）**：旧名是决策时的正确事实，ADR 不可变
- **标注日期的历史 bug**：behavior-component-pitfalls.md:89,93（"2026-06-08 已修复"）
- **删除线标记**：hexagonal-port-extraction.md:15,16 / domain-extraction-pitfalls.md:35 / port-extraction-lessons.md:27
- **历史迁移事件**：hexagonal-port-extraction.md:53（"PortStringRepresentable 从...移到..."）
- **历史踩坑**：hexagonal-port-extraction.md:57 / port-extraction-lessons.md:37 / forge-transformer-network-channel.md:8
- **正确说明**：ROADMAP.md:198 / design/README.md:39 / domain-extraction:86 / hexagonal-gates:57 / domain-extraction-pitfalls:57
- **通用教学假想示例**：stonecutter-multiversion-patterns.md:133,181（第5节"大型项目"分层隔离教学，eyelib-api/core/forge 是虚构结构）

## 验收

- grep `.opencode/` 剩余命中全部为上述保留类别，无未处理的当前陈述错误
- ADR 原文未改动（历史完整性）
