# Brarchive Subpack Model Key 不匹配 — 骨骼名错位

## 症状

Bedrock addon 使用 subpack 覆盖实体模型，但渲染时模型骨骼名与 RenderController 中引用的骨骼模式不一致，导致 per-bone 材质分配失败。具体表现：部分骨骼拿不到正确材质，或整个实体渲染异常。

## 根因

`BedrockAddonLoader.loadBrarchive()` line 439:

```java
String name = entry.lowerEffectivePath().replace("__brarchive/", "");
```

对 subpack 路径如 `subpacks/SP0/__brarchive/models/entity.brarchive`，此 `replace` 只移除了 `__brarchive/` 前缀，得到 `subpacks/SP0/models/entity.brarchive`——与 root 的 `models/entity.brarchive` **是不同的 key**。

merge 逻辑（line 513-520）通过 `effectivePath()` 做 key 查找：

```java
BedrockImportedModels previous = acc.modelFiles.get(entry.effectivePath());
```

由于 key 不同，subpack 模型存储在新 key 下而**不会覆盖 root 模型**。运行时 consumer（如 `BedrockAddonRuntimeBridge`）只查 `models/entity.brarchive`，拿到的是 root 版本。

## Actions-and-Stuff Vex 案例

- **Root brarchive** (`__brarchive/models/entity.brarchive`, 8.4MB) 中 `geometry.oreville_ans.atvfvf` 骨骼名：`ja89l62j`, `d67l62j`, `kl2j6`, `3dafc`, `d680`, ...
- **SP0 brarchive** (`subpacks/SP0/__brarchive/models/entity.brarchive`, 225KB) 中同 geometry 骨骼名：`ja89loaf8`, `d67loaf8`, ...（翼骨骼更宽大）
- **RC patterns** 引用的是 SP0 的骨骼名：`ja89loaf8*`, `d67loaf8*`
- **运行时加载的模型**来自 root → 骨骼名 `ja89l62j`, `d67l62j` → RC pattern 匹配失败

## 修复方向

`loadBrarchive` 中 name 的生成需要统一 subpack 条目和 root 条目的 effective path——移除 `subpacks/SPx/` 前缀使两者聚合到同一 key。或者 BedrockAddonRuntimeBridge 在查模型时需要同时查 subpack key 下的条目并做覆盖合并。

## 验证方法

```python
# 对比 root 和 subpack brarchive 中同 geometry 的骨骼名
import zipfile
mcpack = 'run/resourcepacks/Actions-and-Stuff-1.10-v2.mcpack'
with zipfile.ZipFile(mcpack) as zf:
    root = zf.read('__brarchive/models/entity.brarchive').decode('utf-8', errors='replace')
    sp0 = zf.read('subpacks/SP0/__brarchive/models/entity.brarchive').decode('utf-8', errors='replace')
    # 搜索 geometry.oreville_ans.atvfvf 附近的 bones 数组，对比骨骼名
```
