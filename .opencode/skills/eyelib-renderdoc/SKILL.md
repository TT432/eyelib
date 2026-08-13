---
name: eyelib-renderdoc
description: Eyelib GPU 调试——RenderDoc 截帧、headless 回放、Windows Python replay、renderdoc-mcp。Use when capturing or analyzing RenderDoc frames.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, renderdoc, gpu, debugging
  related-skills: eyelib, eyelib-debug
---

# eyelib-renderdoc

Eyelib GPU 调试——RenderDoc 截帧、headless 回放、Windows Python replay、renderdoc-mcp 分析

## When to use
- 需要用 RenderDoc 截帧或分析 .rdc 帧（draw call、shader、pipeline state、uniform、mesh、texture）
- Do NOT use when: 普通客户端调试启动（非截帧）走 mcmcp_launch，不属于本技能

## Rules
- When capture 模式启动:
  - [when 使用 renderdoccmd capture 模式启动客户端时] capture 启动前 runClient.cmd 必须先由 createClientLaunchScript 生成（经 mcmcp_build）
  - [when 截帧同时需要 AIDebugServer 调试端口时] capture 前向 versions/<node>/build/moddev/clientRunVmArgs.txt 追加 -Dai_debug_port=<port>，因为 AIDebugServer 默认不开启
  - [when 非 RenderDoc 截帧的普通调试启动] PREFER 普通调试启动用 mcmcp_launch()，自动经环境变量注入调试端口
- When renderdoc-mcp：Headless .rdc 分析:
  - PREFER headless .rdc 分析优先用已配置的 mcp_renderdoc_* 工具（opencode mcp_servers.renderdoc，无需 GUI）
- When RenderDoc Python replay — Windows 方案（已验证）:
  - GLES 3.x 抓帧的回放只能走 Windows 侧 Python；WSL 下不支持 replay
  - 先用 write_file 把 Python 脚本写到磁盘再调用，避免 shell 转义
- When renderdoccmd CLI 限制:
  - NEVER 用 renderdoccmd CLI 分析帧内内容（draw call、shader、pipeline state、uniform 值）；CLI 只能操作嵌入式数据段（thumb、extract、convert）
  - 截帧后的深度分析必须用 RenderDoc Python 重放 API（或 renderdoc-mcp headless 工具）

## Workflow
1. 运行 mcmcp_build 生成 versions/<node>/build/moddev/runClient.cmd（createClientLaunchScript）
2. 向 versions/<node>/build/moddev/clientRunVmArgs.txt 追加 -Dai_debug_port=<port>
3. renderdoccmd capture -c eyelib_capture --opt-hook-children 启动 runClient.cmd
4. 经 /eval 调用 RenderDocCapturer.setCaptureFilePathTemplate + startCapture/endCapture 程序化触发截帧
5. 只需预览缩略图→renderdoccmd thumb（仅预览，无 alpha）；需要帧内深度分析→headless 回放分析 [decision]
6. 用 mcp_renderdoc_* headless 工具分析 .rdc：open_capture → get_frame_overview → list_actions/find_draws → get_draw_call_state/get_pipeline_state/disassemble_shader/save_texture/export_mesh/pixel_history
7. write_file 写 Python replay 脚本（sys.path 指向 renderdoc_mcp/lib 导入 renderdoc.pyd），再用 Windows 侧 Python313 调用；注意 renderdoc-mcp 0.2.7 与原生 API 差异 [fallback]

<!-- evidence background: sole source of wf-python-replay -->
API 差异（renderdoc-mcp 0.2.7 vs RenderDoc 原生）：
`cap.OpenCapture(opts, None)` 返回 `(result, controller)` 元组
controller 方法：`GetRootActions()`, `GetPostVSData()`, `SetFrameEvent()`, `GetFrameInfo()`
Action 属性：`eventId`, `flags`, `children`, `numIndices`, `numInstances`
Vertex data 通过 `ctrl.GetPostVSData(0, 0, rd.MeshDataStage.VSOut)` 获取
<!-- evidence rationale: sole source of wf-capture-launch -->
| 路径 | 状态 |
|---|---|
| `renderdoccmd capture gradlew.bat` | ❌ Gradle 代理丢失，TLS 握手失败 |
| `renderdoccmd capture runClient.cmd` | ✅ 全链路验证通过 |
| `renderdoccmd inject --PID=<pid>` | ❌ DLL 加载但 endCapture() 失败 |
| `RenderDocCapturer` 程序化触发 | ✅ startCapture() + endCapture() 返回 true |

<!-- locked residual (verbatim, do not edit) -->
RenderDoc v1.44，路径 `E:\RenderDoc\renderdoccmd.exe`。
| 路径 | 状态 |
|---|---|
| `renderdoccmd capture gradlew.bat` | ❌ Gradle 代理丢失，TLS 握手失败 |
| `renderdoccmd capture runClient.cmd` | ✅ 全链路验证通过 |
| `renderdoccmd inject --PID=<pid>` | ❌ DLL 加载但 endCapture() 失败 |
| `RenderDocCapturer` 程序化触发 | ✅ startCapture() + endCapture() 返回 true |
```powershell
Set-Location E:\_ideaProjects\qylEyelib
# AIDebugServer 默认不开启：capture 前向 VM 参数文件追加调试端口
Add-Content versions\1.20.1\build\moddev\clientRunVmArgs.txt "`n-Dai_debug_port=25999"
& "E:\RenderDoc\renderdoccmd.exe" capture `
    -c eyelib_capture --opt-hook-children `
    "E:\_ideaProjects\qylEyelib\versions\1.20.1\build\moddev\runClient.cmd"
```
```powershell
& "E:\RenderDoc\renderdoccmd.exe" thumb --out snapshot.png --format png capture.rdc
```
安装 Python 3.13：`winget install Python.Python.3.13`
安装 renderdoc-mcp：`C:\Users\<user>\AppData\Local\Programs\Python\Python313\python.exe -m pip install renderdoc-mcp`
```python
import sys
sys.path.insert(0, r"C:\Users\<user>\AppData\Local\Programs\Python\Python313\Lib\site-packages\renderdoc_mcp\lib")
import renderdoc as rd
```
API 差异（renderdoc-mcp 0.2.7 vs RenderDoc 原生）：
`cap.OpenCapture(opts, None)` 返回 `(result, controller)` 元组
controller 方法：`GetRootActions()`, `GetPostVSData()`, `SetFrameEvent()`, `GetFrameInfo()`
Action 属性：`eventId`, `flags`, `children`, `numIndices`, `numInstances`
Vertex data 通过 `ctrl.GetPostVSData(0, 0, rd.MeshDataStage.VSOut)` 获取
调用 Python 脚本: `& "C:\Users\<user>\AppData\Local\Programs\Python\Python313\python.exe" E:\path\to\script.py`
核心工具：`open_capture`、`get_frame_overview`、`list_actions`、`find_draws`、`get_draw_call_state`、`get_pipeline_state`、`disassemble_shader`、`save_texture`、`export_mesh`、`pixel_history`。
`io.github.tt432.clientsmoke.debug.RenderDocCapturer`（clientsmoke mod 内，经 /eval 调用）：
```java
RenderDocCapturer.isAvailable()          // boolean
RenderDocCapturer.setCaptureFilePathTemplate("E:\\...\\run\\frame")
RenderDocCapturer.startCapture()         // void
RenderDocCapturer.endCapture()           // boolean
```
缩略图（仅预览，无 alpha）:
