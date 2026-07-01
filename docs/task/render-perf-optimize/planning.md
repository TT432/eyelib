# 渲染性能优化规划

> 任务：降低稳态渲染的帧时间峰值（low 帧 / 卡顿），提升帧数稳定性。
> 方法：spark profiler 驱动的迭代优化（采集 → 分析 → 优化 → 复测对比）。
> 基线参照：`docs/perf/spark-baseline-and-optimizations.md`（Opt1/Opt2/Opt3 已合入）。

## 优化问题三要素

### 目标函数
- 主指标：稳态渲染（T2 场景）每帧 Render thread 的 CPU 峰值时间下降，即降低 low 帧卡顿。
- 代理指标（spark 可测）：
  - eyelib 代码在 Render thread 的 self-time 占比下降。
  - 当前最大热点函数 self-time 下降（归零或数量级降低）。
- 用户诉求原话：「稳定帧数、low 帧」。

### 决策变量
- eyelib 渲染热路径代码：`RenderControllerRuntime`、`MolangMappingTree`、`RenderControllerEntry.setupModel`、动画求值、part_visibility 求值等。
- 候选改动须经 spark 实测确认是热点，不做无证据的"凭感觉"优化。

### 约束条件
1. **语义保持**：不破坏 Bedrock 规范复刻的正确性（参照已合入优化的语义保持约定，如 Opt1 的 putIfAbsent 先注册优先、Opt2 的 startsWith 语义对齐）。
2. **多版本兼容**：Stonecutter 三节点（1.20.1 / 1.21.1 / 26.1.2），版本特定代码用 `//?` 放 `versions/<ver>/`。
3. **架构边界**：不破坏六边形（domain 不反向依赖 bridge）、不静默简化设计。
4. **编译**：JetBrains MCP build exit 0。
5. **不掩盖问题**：异常路径要查根因，不靠吞异常/改参数绕过。

## 迭代过程

每个迭代是一个子任务（TODO 一项），遵循优化任务流程图：

```
对迭代做执行计划 → 执行 → 验证(编译 + spark 复测)
  → 有效：提交 + 更新基线文档 + 压缩上下文
  → 无效：回滚 + 记录失败原因 + 下次迭代
```

### 验证方法（每个迭代）
1. **编译**：JetBrains MCP `jetbrain_build_project` exit 0（先决条件）。
2. **复测对比**：重跑 sparkc profiler（相同负载、相同 60s 采样窗口），对比目标热点 self-time。
   - 优化生效判定：目标热点 self-time 数量级下降或归零，且无新热点因优化而上升超过原值。
3. **正确性**：smoke / 单测覆盖改动点（若存在），或 progressive-exploration 目视确认。

### 结束条件（满足其一）
- 当前 Render thread 热点已转为 OpenGL 驱动 / vanilla 固有成本（eyelib 无可优化空间）。
- 剩余 eyelib 热点优化收益 < 成本（如需破坏架构或语义才能拿到微小收益）。
- 用户中止。

## 子任务清单（随迭代推进填充）

- **SubTask1**：采集当前稳态渲染基线（sparkc 60s），分析识别当前最大热点。→ 见 `SubTask1/`
- **SubTask2..N**：每个热点一个迭代，子任务目录 `SubTaskN/`。

## 工具链
- 采集：`/sparkc profiler`（经 /eval 反射 ClientCommandHandler，配方见 `docs/perf/spark-profiling-recipe.md`）。
- 分析：`scripts/analyze_render_profile.py`、`scripts/trace_hotspots.py`。
- 负载：`Actions-and-Stuff-1.10-v2.mcpack`（113 实体定义），已在 `run/resourcepacks/`。
- 调试客户端：eyelib-debug MCP（launch / enter_world / send_command / execute）。

## 关键踩坑（继承自 spark-profiling-recipe）
- 用 `/sparkc`（client），不用 `/spark`（server）。
- 不加 `--thread "Render thread"`（引号被 brigadier 吞，采样为空）。
- 采集前 `mc.options.pauseOnLostFocus = false` + `mc.setScreen(null)`，防失焦暂停冻结 Render thread。
- URL 取自 `versions/1.20.1/run/config/spark/activity.json`。
