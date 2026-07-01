# SubTask1：采集当前稳态渲染基线

> 采集 Opt1/Opt2/Opt3 已合入后的当前稳态渲染 spark 基线，识别当前最大 eyelib 渲染热点，为后续迭代确定目标。

## 目标
产出当前代码的 spark Render thread 60s 采样 + 热点 top-N 分析，确定下一个迭代要优化的具体函数。

## 执行步骤
1. launch 1.20.1 客户端（eyelib-debug）。
2. enter_world（已有世界）。
3. 防失焦暂停：`mc.options.pauseOnLostFocus = false` + `mc.setScreen(null)`。
4. 验证 eyelib 接管渲染：`RenderData.getComponent` 读 `useBuiltInRenderSystem`，并统计在场景中走 eyelib 路径的实体数。
5. 启动 profiler：经 /eval 反射 `ClientCommandHandler.runCommand("sparkc profiler start --timeout 60")`。**不加 `--thread`**。
6. 等待 ~65s 采集完成。
7. 读 `versions/1.20.1/run/config/spark/activity.json` 取 URL。
8. 下载 `metadata.json` + `profile.bin`（curl）。
9. /eval 内用 SparkSamplerProtos 解析 protobuf，导出 stacknodes CSV。
10. `python scripts/analyze_render_profile.py` 算 self/total time + eyelib 分类；`trace_hotspots.py` 追溯业务调用者。
11. 热点报告写入本目录。

## 规格说明
- **前置**：客户端 idle；负载 `Actions-and-Stuff-1.10-v2.mcpack` 已在 `run/resourcepacks/`；端口 25999 空闲。
- **后置**：spark URL + profile.bin + stacknodes.csv + 热点 top-N 报告存档于本目录。
- **验收**：
  - 采样非空（protobuf > 50KB，threads 字段存在）。
  - 识别出 ≥1 个 eyelib 可优化热点（self-time 显著），**或**确认热点已全为 OpenGL/vanilla 固有成本（无可优化空间 → 终止整体任务）。
