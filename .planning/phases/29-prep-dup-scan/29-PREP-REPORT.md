# Phase 29: PREP — 预处理归属扫描报告

**Date:** 2026-05-12
**Status:** Read-only analysis — no code modified

## 1. 当前 `:eyelib-preprocessing` 已拥有包

| Package | Key Classes | Role |
|---------|------------|------|
| `loader/` | `LoaderParsingOps` | 解析/翻译 helpers，零 `ResourceLocation` |
| `manager/reload/` | `ManagerResourceReloadPlan`, `ManagerResourceBatchPlanner` | 文件分类/批处理 |
| `animation/baked/` | `BoneAnimationBaker`, `BakedBoneKeyFrame` | 骨骼动画烘焙 |
| `model/bake/` | `ModelBakeInfo`, bake helpers | 模型烘焙数据 |
| `particle/` | `ParticleFlipbookSummaryOps` | 粒子翻书摘要 |

## 2. 迁移候选

### 2.1 强烈建议迁移: `client/model/Models.java`
- **路径:** `src/main/java/io/github/tt432/eyelib/client/model/Models.java`
- **功能:** 纯数据转换 — 合并和差异 Model 对象的骨骼/立方体树
- **MC/Forge 导入:** **零** — 仅使用 `eyelibimporter.model.Model`、`fastutil`
- **消费者:** Root 渲染管线（model merge before rendering）
- **理由:** 纯数据变换在 importer 类型上，零运行时依赖。与已有的 `ModelBakeInfo` 等预处理类模式一致。
- **建议:** **迁移** 到 `eyelib-preprocessing/src/main/java/io/github/tt432/eyelibpreprocessing/model/Models.java`

### 2.2 弱候选: `client/model/bbmodel/BBBone.java`
- **功能:** 不可变数据记录（33 行），Blockbench 骨骼节点
- **MC/Forge 导入:** 零
- **理由:** 太小（33行），命名空间迁移成本大于纯度收益，仅 Blockbench 导入流使用
- **建议:** **保留** — 除非计划更广泛的 `bbmodel` 包迁移

## 3. 必须保留的类别（含根依赖链说明）

### 3.1 `client/loader/` — 全部保留
所有类都继承 `BrResourcesLoader` → `SimpleJsonWithSuffixResourceReloadListener` → `SimplePreparableReloadListener`（Minecraft Forge 重载生命周期）。由 `ClientLoaderLifecycleHooks` 通过 Forge `@SubscribeEvent` 注册。深度 MC 耦合。

| 类 | 保留原因 |
|----|----------|
| `BrResourcesLoader`, `SimpleJsonWithSuffixResourceReloadListener` | Forge 重载桥接基类 |
| `BrAnimationLoader`, `BrModelLoader`, `BrMaterialLoader`, `BrParticleLoader`, `BrRenderControllerLoader`, `BrClientEntityLoader`, `BrAttachableLoader`, `BrAnimationControllerLoader` | 领域特定重载监听器，发布到 root 注册表 |
| `BedrockAddonRuntimeBridge` | 零 MC 导入但通过 root 注册表发布 |

### 3.2 `client/model/` — Models.java 除外全部保留
| 类 | 保留原因 |
|----|----------|
| `ModelBakeInvalidationHooks` | **MODULES.md 明确保留**: Forge 事件驱动失效桥接 |
| `ModelRuntimeData` | 可变运行时状态容器（动画位置/旋转/缩放） |
| `ModelPartModel`, `RootModelPartModel` | 深度 MC: `ModelPart`, `PartPose` |
| `DFSModel` | 深度 MC 渲染: `PoseStack`, `RenderParams` |
| `ModelLookup` | 运行时读访问缝合面 |
| `importer/*` | 薄委托门面到 `:eyelib-importer` |

### 3.3 `client/gui/manager/reload/` — 全部保留
管理屏幕 UI/工具类，编排运行时导入、文件监视和纹理上传
| 类 | 保留原因 |
|----|----------|
| `ManagerResourceImportPlanner` | UI 编排: `NativeImage`, `MinecraftForge.EVENT_BUS`, 发布到 root 注册表 |
| `ManagerFolderSession` | UI 会话: `RenderSystem.recordRenderCall`, `FileDialogService` |
| `ManagerResourceFolderWatcher` | 纯 Java 文件监视但对管理屏幕强作用域 |
| `ManagerImportActions` | UI 对话框导入 |

### 3.4 `client/animation/` — 全部保留
整个动画包是运行时动画系统。预处理已拥有纯烘焙逻辑（`BoneAnimationBaker`），root 类消费烘焙数据并添加: codecs 序列化、MolangScope 依赖采样、MC/Forge 效果分发（声音、粒子）、运行时回放状态机。

## 4. 迁移/保留汇总表

| 类别 | 类数 | 判定 | 关键原因 |
|------|------|------|----------|
| `client/loader/` | 11 | 保留 | Forge 重载监听器生命周期 |
| `client/model/Models.java` | 1 | **迁移** | 纯数据变换，零 MC |
| `client/model/` 其余 | 12 | 保留 | MC 耦合或运行时状态 |
| `client/gui/manager/reload/` | 5 | 保留 | UI 编排，root 注册表发布 |
| `client/animation/` | 34 | 保留 | 运行时动画系统 |
