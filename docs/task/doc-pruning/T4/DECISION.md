# T4 决策记录 · domain-module-map.md 修正

## D4 决策：修正（非删除）

### 背景
domain-module-map.md 是 architecture.md:95 明确指向的"单一事实源"，记录 Port 清单、复用矩阵和提取状态。删除会迫使 architecture.md 重新内联这些信息，且 Port 复用矩阵的 quick-reference 价值无法被 package-info.java 替代。

### 修正项

| 问题 | 修正 |
|------|------|
| 通篇 `eyelib-xxx` 旧子项目名（~15 处） | → flat-merge 后包名（material/molang/util 等） |
| ArchUnit 列全标 ✅ | → 移除该列；表前加说明（ADR-0014 删除，ADR-0015 Phase 2+ 待恢复，当前文档约定+PR review） |
| Port 清单 util 行位置写 `eyelib-util` | → `util`（3 个 Port 直接在 util 包下，非 port/ 子目录） |
| "2026-06-08 更新" 单一行 | → 拆分：ArchUnit 说明 + MC 文件数/Spec 测试标注"2026-06-08 快照" |
| 依赖方向 "root → bridge（通过 build.gradle 显式依赖）" | → "root 编排包（client/common）→ bridge"（flat-merge 后无子项目 build.gradle 依赖） |
| ASCII 图用旧子项目名 | → 包名 |

### 保留项（标注日期，不静默删除）
- MC 文件数、Spec 测试数：2026-06-08 快照，横向对比参考价值，标注日期避免误导
- 剩余工作列：六边形提取 todo，保留

### 事实核实
- 7 个 Port 接口位置全部 grep 确认：util 包 3 个（PortStringRepresentable/PortResourceLocation/PortFriendlyByteBuf）、material/port/ 1 个（PortRenderPass）、molang/port/ 3 个（PortEntity/PortLevel/PortItemStack）
- bridge 包存在（`io.github.tt432.eyelib.bridge`，MODULES.md 确认）
- Port 清单 7 个与代码一致

### 验收
- grep `eyelib-(bridge|material|molang|model|animation|behavior|particle|importer|util)` 零命中
- architecture.md:95 引用 `../architecture/domain-module-map.md` 仍有效（文件在原位）
