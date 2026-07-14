# Eyelib Model / Animation LOD 规格

## 目标

为 Eyelib 的 Bedrock 模型渲染与动画求值增加确定性的三级 LOD，并在现有开发态模型预览屏幕中提供模型选择与全局 LOD 强度滑条。

## 业界依据

- Unreal Engine 5.8 Skeletal Mesh LOD：屏幕尺寸阈值、`LOD Hysteresis`、骨骼优先级/移除。https://dev.epicgames.com/documentation/en-us/unreal-engine/skeletal-mesh-lods-in-unreal-engine
- Unreal Engine 5.8 Animation Budget Allocator：按显著性降低动画更新频率，并在降频更新之间插值。https://dev.epicgames.com/documentation/en-us/unreal-engine/animation-budget-allocator-in-unreal-engine
- Unity 6.4 LOD Group：以屏幕高度百分比选择 LOD，并支持过渡区间。https://docs.unity3d.com/6000.4/Documentation/Manual/class-LODGroup.html
- Godot 4.7 Mesh LOD：以屏幕空间像素阈值选择几何细节，默认 1 像素作为感知无损目标；官方同时警告自动简化蒙皮网格可能产生瑕疵。https://docs.godotengine.org/en/stable/tutorials/3d/mesh_lod.html
- Godot 4.7 Visibility Ranges：用迟滞避免阈值附近来回跳变。https://docs.godotengine.org/en/stable/tutorials/3d/visibility_ranges.html
- O3DE Actor LOD：角色 LOD 同时降低网格、叶骨骼和动画采样率。https://docs.o3de.org/docs/user-guide/visualization/animation/using-actor-lods-optimize-game-performance/

Eyelib 模型是逐骨骼的刚性 cube 几何，不是带权重蒙皮网格。项目没有可靠的拓扑简化器或作者提供的 LOD 变体。因此本实现不做运行时边折叠；采用“保留完整骨骼树和 locator，只剔除屏幕贡献低于阈值的骨骼几何”，避免破坏动画传播和挂点语义。

## 优化模型

### 决策变量

- 全局强度 $s \in [0,1]$：`0` 禁用 LOD；`1` 使用最积极的默认阈值。
- 实体屏幕高度比例 $p$：由实体高度、相机距离、垂直 FOV 计算。
- 当前级别 $L \in \{FULL, MEDIUM, LOW\}$。
- 骨骼局部最大包围尺寸 $b$ 与每世界单位屏幕像素数 $q$。
- 动画采样间隔 $n(L) \in \{1,2,4\}$ 帧。

### 目标函数

在不改变动画状态机、Molang、副作用与 locator 语义的前提下，降低：

1. 远距离模型提交的顶点数；
2. 远距离模型骨骼关键帧采样次数；
3. 热路径临时对象分配。

### 约束

1. 每个渲染帧仍执行动画控制器、Molang 条件和 sound/particle/timeline effects；LOD 只能降低姿势采样频率。
2. 骨骼父子结构、骨骼变换与 locator 收集始终完整执行；LOD 只跳过 `BakedBone` 的顶点变换/提交。
3. 级别选择使用屏幕空间指标；必须考虑窗口高度与 FOV。
4. 降级使用 15% 单向迟滞；升级立即发生。
5. `s=0` 时行为等价于未启用 LOD：FULL、每帧姿势采样、不剔除骨骼几何。
6. Domain 类型不得依赖 Minecraft API；相机/FOV 翻译留在 client/bridge 边界。
7. 1.20.1、1.21.1、26.1.2 必须编译；版本差异使用现有 `//?` 模式。

## 功能需求

### FR-1 屏幕空间级别选择

- 默认强度为 `1.0`。
- 强度为 `1.0` 时：
  - FULL → MEDIUM 的基准阈值为屏幕高度 8%；
  - MEDIUM → LOW 的基准阈值为屏幕高度 2.5%。
- 阈值与强度线性缩放。
- 从高细节向低细节切换时，实际阈值再乘 `0.85`；反向升级使用基准阈值。
- 实体距离接近零、没有可用相机或强度为零时必须选择 FULL。

### FR-2 几何 LOD

- 烘焙时为每个 `BakedBone` 预计算局部几何最大轴尺寸，不在逐帧热路径扫描顶点。
- FULL：所有可见骨骼几何照常渲染。
- MEDIUM：骨骼投影最大尺寸小于 1 像素时跳过该骨骼顶点变换和提交。
- LOW：骨骼投影最大尺寸小于 2.5 像素时跳过该骨骼顶点变换和提交。
- 空骨骼不提交顶点，但其变换、子骨骼和 locator 仍处理。

### FR-3 动画更新率 LOD

- FULL 每帧采样姿势；MEDIUM 每 2 帧；LOW 每 4 帧。
- 未采样姿势的帧仍完整推进动画播放状态、控制器转换和 effects。
- 每次得到新姿势后，从当前显示姿势向新姿势按采样间隔线性插值，避免直接跳变；允许最多一个采样间隔的视觉延迟。
- 动画配置更换时清空 LOD 插值状态，禁止把旧动画姿势混入新动画。

### FR-4 Debug 模型预览

- 保留开发态 `V` 键入口与 `.bbmodel` 拖入。
- 可从 `ModelManager` 当前快照中按名称/ID搜索选择模型，并提供上一项/下一项操作。
- 未携带纹理元数据的 Manager 模型使用稳定的 vanilla 白色纹理预览。
- 提供 `LOD Strength: 0%..100%` 滑条；修改时立即更新生产 LOD 控制器的全局强度，并在预览模型上可视化几何剔除强度。
- 界面显示当前模型、强度、预览 LOD 级别、旋转/缩放/平移。
- 搜索无结果时显示明确状态，不保留错误的 bake 引用。

## 非功能需求

- LOD 级别计算每实体每帧一次；渲染组件复用结果。
- 骨骼尺寸预计算一次并随现有 baked model 缓存失效。
- effects-only 动画帧不得创建骨骼 Entry。
- 不引入第三方简化库、运行时网格边折叠、透明 cross-fade 或完整全局预算分配器。
- Debug UI 只在非 production 环境开放。

## 前置条件

- `ClientBootstrap.wire()` 已安装渲染 Port 和 ModelPreviewScreen 工厂。
- 实体已绑定 `RenderData`，且 animation/model manager 数据已经装载。
- `RenderStageEventAdapter` 每渲染帧调用 `EntityRenderOrchestrator.onRenderStage`。

## 后置条件

- 每个 Eyelib 实体在 TickStage 后持有该帧一致的 LOD 状态和可渲染姿势。
- 所有 ModelComponent 在同一帧读取相同 LOD 状态。
- 预览屏幕选择模型后具备匹配的 DFS 与 baked model；滑条值与全局强度一致。

## 不变量

- PoseStack push/pop 平衡。
- `ModelRuntimeData.EMPTY` 不被修改。
- 动画 effects 的处理频率不随 LOD 改变。
- locator/手持物挂点不因几何剔除消失。
- LOD 不改变模型、动画或 capability 的序列化格式。

## 异常行为

- 模型无几何：可选择但不提交顶点，界面不崩溃。
- 模型无纹理元数据：使用白色纹理。
- 搜索空字符串：不改变当前选择。
- 非法强度（NaN/无穷/区间外）：规范化到 `[0,1]`，NaN 视为 `0`。
- bake 失败：清空当前 baked/DFS 状态并显示错误消息；不得终止客户端。

## 副作用

- Debug 滑条修改本客户端进程的全局 LOD 强度；不持久化、不联网。
- LOD 在 AnimationComponent/RenderData 中维护仅客户端运行时状态。
- Manager reload 按现有失效钩子重建 DFS/baked 缓存及骨骼尺寸。

## 验收

1. 纯单元测试覆盖强度边界、三级阈值、单向迟滞、采样间隔、effects-only 不创建姿势、姿势插值。
2. active 1.20.1 build 与相关单测通过。
3. 1.21.1、26.1.2 build 通过。
4. 启动 1.20.1 dev client，打开 ModelPreviewScreen；运行时确认模型可选择、滑条可调、标签与几何变化同步。
5. 关闭客户端后提交全部实现、测试和规格。
