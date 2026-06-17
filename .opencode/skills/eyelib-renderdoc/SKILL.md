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

# Eyelib GPU 调试 (RenderDoc)

RenderDoc v1.44，路径 `E:\RenderDoc\renderdoccmd.exe`。

## 已知可用路径

| 路径 | 状态 |
|---|---|
| `renderdoccmd capture gradlew.bat` | ❌ Gradle 代理丢失，TLS 握手失败 |
| `renderdoccmd capture runClient.cmd` | ✅ 全链路验证通过 |
| `renderdoccmd inject --PID=<pid>` | ❌ DLL 加载但 endCapture() 失败 |
| `RenderDocCapturer` 程序化触发 | ✅ startCapture() + endCapture() 返回 true |

## capture 模式启动

```bash
cd /mnt/e/_ideaProjects/qylEyelib && unset JAVA_HOME && WSLENV= \
  /mnt/e/RenderDoc/renderdoccmd.exe capture \
  -c eyelib_capture --opt-hook-children \
  E:\\_ideaProjects\\qylEyelib\\build\\moddev\\runClient.cmd &
```

**前置**: `build/moddev/runClient.cmd` 需先由 `createLaunchScripts` 生成。

推荐用 `eyelib_debug_launch()`（MCP 工具），自动完成上述步骤。

## renderdoccmd CLI 限制

`renderdoccmd` CLI 只能操作嵌入式数据段（thumb、extract、convert）——**不能分析帧内的 draw call、shader、pipeline state、uniform 值**。截帧后必须用 RenderDoc Python 重放 API 才能做深度分析。

缩略图（仅预览，无 alpha）:
```bash
/mnt/e/RenderDoc/renderdoccmd.exe thumb --out snapshot.png --format png capture.rdc
```

## RenderDoc Python replay — Windows 方案（已验证）

WSL 下 GLES 3.x 不支持 replay。必须走 Windows 侧：

1. 安装 Python 3.13：`winget install Python.Python.3.13`
2. 安装 renderdoc-mcp：`C:\Users\<user>\AppData\Local\Programs\Python\Python313\python.exe -m pip install renderdoc-mcp`
3. renderdoc-mcp 的 lib 目录下有 `renderdoc.pyd`，导入路径：
```python
import sys
sys.path.insert(0, r"C:\Users\<user>\AppData\Local\Programs\Python\Python313\Lib\site-packages\renderdoc_mcp\lib")
import renderdoc as rd
```
4. API 差异（renderdoc-mcp 0.2.7 vs RenderDoc 原生）：
   - `cap.OpenCapture(opts, None)` 返回 `(result, controller)` 元组
   - controller 方法：`GetRootActions()`, `GetPostVSData()`, `SetFrameEvent()`, `GetFrameInfo()`
   - Action 属性：`eventId`, `flags`, `children`, `numIndices`, `numInstances`
   - Vertex data 通过 `ctrl.GetPostVSData(0, 0, rd.MeshDataStage.VSOut)` 获取
5. 从 WSL 调 Windows Python：`cmd.exe /c C:\Users\<user>\AppData\...\python.exe E:\path\to\script.py`
6. 注意 bash 转义——推荐先 `write_file` 写 Python 脚本再 `cmd.exe /c` 执行

## renderdoc-mcp：Headless .rdc 分析

[renderdoc-mcp](https://pypi.org/project/renderdoc-mcp/) 捆绑 renderdoc.so，通过 MCP 协议提供 42 个 headless 分析工具，支持 WSL2，无需 GUI。

已在 qyleyelib profile 的 `mcp_servers.renderdoc` 中配置，工具以 `mcp_renderdoc_*` 前缀注册。

核心工具：`open_capture`、`get_frame_overview`、`list_actions`、`find_draws`、`get_draw_call_state`、`get_pipeline_state`、`disassemble_shader`、`save_texture`、`export_mesh`、`pixel_history`。

⚠️ **WSL 下 GLES 3.x 抓帧无法回放**。唯一可行路径：Windows 侧 Python replay。

## RenderDocCapturer API

```java
RenderDocCapturer.isAvailable()          // boolean
RenderDocCapturer.setCaptureFilePathTemplate("E:\\...\\run\\frame")
RenderDocCapturer.startCapture()         // void
RenderDocCapturer.endCapture()           // boolean
```
