# RenderDoc Windows GLES replay

## 环境搭建

```bash
# 1. 安装 Python 3.13 on Windows
winget install Python.Python.3.13 --accept-source-agreements --accept-package-agreements

# 2. 安装 renderdoc-mcp
C:\Users\<user>\AppData\Local\Programs\Python\Python313\python.exe -m pip install renderdoc-mcp
```

## API 导入

```python
import sys
sys.path.insert(0, r"C:\Users\<user>\AppData\Local\Programs\Python\Python313\Lib\site-packages\renderdoc_mcp\lib")
import renderdoc as rd
```

## API 差异（renderdoc-mcp 0.2.7 vs RenderDoc 原生 Python API）

- `cap.OpenCapture(opts, None)` 返回 `(result, controller)` 元组
- controller 方法：`GetRootActions()`, `GetPostVSData()`, `SetFrameEvent()`, `GetFrameInfo()`, `GetBufferData()`, `GetPipelineState()`
- Action 属性：`eventId`, `flags`, `children`, `numIndices`, `numInstances`
- `GetPostVSData(0, 0, rd.MeshDataStage.VSOut)` 返回 `MeshFormat`（顶点格式描述），不是顶点数据
- 读取顶点：`ctrl.GetBufferData(mf.vertexResourceId, mf.vertexByteOffset, size)`

## 调试脚本：对比 eyelib ON vs OFF draw call 数量

```python
import sys, struct
sys.path.insert(0, r"C:\Users\<user>\AppData\Local\Programs\Python\Python313\Lib\site-packages\renderdoc_mcp\lib")
import renderdoc as rd

def count_draws(rdc_path, label):
    cap = rd.OpenCaptureFile()
    cap.OpenFile(rdc_path, "", None)
    result, ctrl = cap.OpenCapture(rd.ReplayOptions(), None)
    
    root = ctrl.GetRootActions()
    def collect(acts, all_acts):
        for a in acts:
            all_acts.append(a)
            if a.children:
                collect(a.children, all_acts)
    all_acts = []
    collect(root, all_acts)
    
    draws = [a for a in all_acts if a.flags & rd.ActionFlags.Drawcall]
    print(f"{label}: total={len(draws)}")
    
    ctrl.Shutdown()
    cap.Shutdown()
    return len(draws)

d_on  = count_draws(r"E:\...\capture_on.rdc",  "eyelib ON")
d_off = count_draws(r"E:\...\capture_off.rdc", "eyelib OFF")
print(f"\neyelib contributed: {d_off - d_on} draws (cancelled vanilla)")
```

## 从 WSL 调用 Windows Python

```powershell
# 先 write_file 写脚本，再执行
& "C:\Users\<user>\AppData\Local\Programs\Python\Python313\python.exe" E:\path\to\script.py
```

推荐先 `write_file` 写 Python 脚本到 `E:\...`,再用上面的命令执行。
