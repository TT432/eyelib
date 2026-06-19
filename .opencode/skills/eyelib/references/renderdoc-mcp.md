# renderdoc-mcp：Headless RenderDoc .rdc 分析

## 概述

[renderdoc-mcp](https://github.com/Linkingooo/renderdoc-mcp) 基于 RenderDoc Python 重放 API，通过 MCP 协议让 AI 助手直接分析 `.rdc` 帧捕获文件。捆绑 `renderdoc.so`/`renderdoc.pyd`，无需单独安装 RenderDoc。

- **42 个工具**，覆盖完整分析流程
- **Headless** — 不需要 GUI、不需要 DISPLAY
- **WSL2 支持** — Linux x64 兼容
- **Python 3.13 必需** — 捆绑模块只编译给 3.13

## 配置

在 opencode MCP 配置(`opencode.json` / `opencode.jsonc`,详见 `customize-opencode` skill)的 `mcp_servers` 段添加:

```yaml
renderdoc:
  command: uvx
  args:
  - --python
  - '3.13'
  - renderdoc-mcp
  timeout: 60
```

重启 opencode 后工具以 `mcp_renderdoc_*` 前缀注册。

## 核心工具速查

### 会话管理

| 工具 | 说明 |
|------|------|
| `open_capture(path)` | 打开 .rdc（自动关闭上一个） |
| `get_capture_info()` | API、GPU、分辨率、GPU 特性检测 |
| `get_frame_overview()` | 帧级统计：draw count、RT、纹理内存 |

### Draw Call 分析

| 工具 | 说明 |
|------|------|
| `list_actions(filter, event_type)` | 列出所有 action/draw call |
| `find_draws(blend, min_vertices, ...)` | 按渲染状态搜索 |
| `get_draw_call_state(eid)` | **一键获取**：blend/depth/stencil/rasterizer/shader/textures/RT |
| `diff_draw_calls(a, b)` | 对比两个 draw call 差异 + 可读含义 |

### 管线检查

| 工具 | 说明 |
|------|------|
| `get_pipeline_state()` | 拓扑、viewport、blend、depth、stencil |
| `get_vertex_inputs()` | vertex attribute layout + buffer bindings |
| `get_shader_bindings(stage)` | cbuffer/SRV/UAV/sampler 绑定 |

### Shader 分析

| 工具 | 说明 |
|------|------|
| `disassemble_shader(stage)` | 反编译 shader，支持 keyword 搜索 |
| `get_cbuffer_contents(stage, slot)` | 常量缓冲区实际值（uniform 值！） |
| `get_shader_reflection()` | 输入/输出签名、资源绑定 layout |

### 数据导出

| 工具 | 说明 |
|------|------|
| `save_texture(id, path)` | 导出为 PNG/JPG/BMP/TGA/HDR/EXR/DDS |
| `export_draw_textures(eid, dir)` | 批量导出 draw call 绑定的所有纹理 |
| `export_mesh(eid, path)` | 导出 mesh 为 OBJ（post-VS 数据） |
| `save_render_target(eid, path)` | 保存 RT 快照 |

### 调试

| 工具 | 说明 |
|------|------|
| `pixel_history(rt_id, x, y)` | 逐像素修改历史 |
| `debug_shader_at_pixel(eid, x, y)` | 逐像素 shader 调试 |
| `get_post_vs_data(eid)` | post-VS 顶点数据 |

### 诊断

| 工具 | 说明 |
|------|------|
| `diagnose_negative_values()` | 扫描 NaN/Inf/负值 |
| `diagnose_precision_issues()` | R11G11B10/D16/SRGB 问题 |
| `diagnose_mobile_risks()` | 移动端 GPU 风险 |

### 性能

| 工具 | 说明 |
|------|------|
| `analyze_overdraw()` | overdraw 估算 |
| `analyze_state_changes()` | 冗余状态切换 / batching 机会 |
| `get_pass_timing()` | pass 耗时 |

## 典型工作流

### 调试"渲染不对"的 draw call

```
1. open_capture("eyelib_bgfx_capture.rdc")
2. get_frame_overview()  → 确认帧信息
3. find_draws(blend=True) 或 list_actions(filter="slime")
   → 找到目标 draw call 的 eventId
4. get_draw_call_state(eid)
   → blend/depth/shader/textures 全貌
5. get_vertex_inputs()
   → vertex attribute 绑定是否匹配 bgfx shader
6. get_cbuffer_contents(stage="pixel", slot=0)
   → uniform 实际值（FogControl、TileLightColor 等）
7. disassemble_shader(stage="pixel")
   → shader 逻辑是否正确
```

### 验证 vertex format 匹配

```
set_event(drawEid)
get_vertex_inputs()
→ 检查每个 attribute 的 name、format、byteOffset
→ 对比 bgfx shader 的 attribute binding glBindAttribLocation
```

### 验证 uniform 值

```
set_event(drawEid)
get_cbuffer_contents("vertex", 0)   → u_proj、u_view、u_model
get_cbuffer_contents("pixel", 0)    → FogControl、TileLightColor
```

## 已知限制

- **Python 3.13 必需** — `uvx --python 3.13` 自动拉取
- **首次运行下载** — uvx 首次会下载 Python 3.13 + 捆绑的 renderdoc.so，需网络
- **renderdoccmd CLI 不能替代** — CLI 只能操作嵌入式数据段，帧分析必须走 Python API
