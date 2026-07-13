# 实体渲染验证工作流

## 核心原则

- **全量，不抽样**：用户明确要求所有 entity 都要测
- **GPU 侧优先**：管线卫生（材质继承链、RenderType 路由）正确不代表渲染正确。真正的 bug 在"数据经过管线处理后变成了什么"——顶点坐标、UV 值、颜色值
- **先 MVP 后规模化**：先建基础设施（RenderDoc replay、/eval 可查询），再逐批验证
- **不猜根因，先查事实**：看到蓝色方块不要猜"是 fallback"，用工具确认是什么

## 诊断分层

| 层 | 问题 | 诊断工具 |
|---|---|---|
| 1 | 模型加载了吗 | `/eval`: `ModelComponent.getModel()` → null? |
| 2 | 骨骼可见吗 | `/eval`: `getPartVisibility()` → 数量 |
| 3 | vertices 提交到 GPU 了吗 | RenderDoc: `GetPostVSData()` → 顶点数 |
| 4 | GPU vertices 和 geometry 一致吗 | RenderDoc vs geometry JSON diff |
| 5 | 纹理加载了吗 | `/eval`: `getTexture()` → 路径检查 |
| 6 | 动画驱动了吗 | `/eval`: `AnimationComponent.tickedInfos` |

## RenderDoc Windows replay 设置

WSL 不支持 GLES 3.x replay。走 Windows 侧：

```python
# 1. winget install Python.Python.3.13
# 2. pip install renderdoc-mcp
import sys
sys.path.insert(0, r"C:\Users\<user>\AppData\Local\Programs\Python\Python313\Lib\site-packages\renderdoc_mcp\lib")
import renderdoc as rd

cap = rd.OpenCaptureFile()
cap.OpenFile(rdc_path, "", None)
result, ctrl = cap.OpenCapture(rd.ReplayOptions(), None)  # returns (result, controller)

# 遍历 draw calls
root = ctrl.GetRootActions()
# 递归收集所有 draw call
# ctrl.GetPostVSData(0, 0, rd.MeshDataStage.VSOut) → MeshFormat
# ctrl.GetBufferData(vertexResourceId, offset, size) → raw bytes
```
