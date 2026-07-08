# 渲染架构对齐 26.1.2：规划

## 背景
`archunit-baseline-cleanup` 剩余 2 个 `//?` 文件（EyelibLivingEntityRenderer、RenderParams）是渲染核心，
其 `//?` 根因是 MC 渲染器 API 跨版本根本性分歧。用户决策：**对齐 26.1.2 的先进渲染架构**来消除分歧。

## 现状：两套渲染架构并存

### <26.1（1.20.1 / 1.21.1）—— 编排式
```
EyelibLivingEntityRenderer.render(entity, yaw, partial, PoseStack, MultiBufferSource, light)
  └─ SimpleRenderAction.builder(buffer, poseStack, cap, partial)...build().render()
       └─ EntityRenderOrchestrator.renderEntity(action)
            ├─ setupExtraMolang / setupSyncedBehaviorContext
            └─ renderComponents(data)   ← 组件循环在此
```
- `extends LivingEntityRenderer<T, EmptyEntityModel<T>>`（**2 泛型**）
- `getTextureLocation(T entity)` 返回 ResourceLocation
- `EmptyEntityModel.renderToBuffer(... float r/g/b/a)`
- 入口：`EntityRenderOrchestrator.renderEntities` / `renderEntityFromParams`（也有 AR 路径）

### ≥26.1（26.1.2）—— 直循环式（"先进架构"）
```
EyelibLivingEntityRenderer.submit(state, PoseStack, SubmitNodeCollector, CameraRenderState)
  └─ 手动遍历 cap.getModelComponents()
       └─ RenderParams.builder(ps, rt, isSolid, tex, buffer)...build()
            └─ RenderHelper.start().render(params, model, tickedInfos)
                 └─ submitNodeCollector.submitCustomGeometry(...)
```
- `extends LivingEntityRenderer<T, EyelibEntityRenderState, EmptyEntityModel>`（**3 泛型**，MC 1.21.2+ 引入 RenderState）
- `createRenderState` / `extractRenderState` / `submit`（新 API）
- `getTextureLocation(State)` 返回 Identifier
- `EmptyEntityModel.renderToBuffer(... int color)`
- 已用 `PortRenderPass`（`ModelComponent.getRenderType(portTexture)` → `MaterialPort.toRenderType`）

## 核心矛盾（为何 //? 不可原地清零）
1. **基类泛型元数不同**：2 vs 3。一个 Java 类不能同时是两种基类子类 → 整类必须版本分歧。
2. **入口方法不同**：`render(entity,...)` vs `submit(state,...)`。MC 1.21.2+ 用 RenderState 重写了渲染管线。
3. **渲染路径不同**：编排委托 vs 直循环。
4. RenderType 在 26.1 物理移包（旧路径删除），RenderParams 持有该 MC 类型作字段。

## 对齐方案（待设计确认）
目标：以 26.1.2 直循环 + PortRenderPass + RenderParams 架构为基准，统一渲染核心逻辑，
使版本分歧收敛到**最小外壳**，剩余 `//?` 用合规手段消除。

### 子任务草案
- **ST-A：抽取共享渲染核心**。把 `EntityRenderOrchestrator.renderComponents`（<26.1 的组件循环）
  与 26.1 `submit` 的循环对齐到同一稳定方法（参数化 PoseStack/RenderParams 工厂/提交回调），
  消除两套循环逻辑分叉。
- **ST-B：RenderParams 迁 PortRenderPass**。`renderType` 字段由 MC `RenderType` 改为 `PortRenderPass`
  （26.1.2 已用此模式），`MaterialPort.toRenderType` 物化下沉到 MC 边界（getBuffer/submitCustomGeometry），
  消除 RenderType import `//?`。需梳理所有 `params.renderType()` 消费者。
- **ST-C：asEmissive 26.1 stub**。要么把 ≤1.21.1 的 emissive 逻辑抽到 bridge facade（返回稳定描述，
  RenderParams 应用），要么在 26.1 真正移植 emissive（TextureManager/render-state 差异）。
- **ST-D：渲染器外壳的 `//?` 合规化**。因基类泛型/入口方法不可统一，外壳天然版本分歧。方案二选一：
  - (D1) Stonecutter per-version source set：把 <26.1 与 ≥26.1 渲染器拆成各自源文件（按版本节点包含），
    单文件内无 `//?`。需 build.gradle 配置版本专属源集。
  - (D2) 渲染器壳迁 `bridge/`（`//?` 合法）+ 编排逻辑留 application（通过 Port 回调）——需解决 bridge→application 反向依赖（规则 4）。

## 验证要求
- 三版本 BUILD SUCCESSFUL。
- EyelibLivingEntityRenderer / RenderParams `//?` 清零（或经 D1/D2 合规化）。
- **渲染冒烟验证**（clientsmoke / 实体渲染 arena）：渲染架构改动高风险，必须验证实体渲染、attachable、
  item-in-hand、emissive 在三版本下视觉正确。
- ArchUnit 规则不回退。

## 风险
- 渲染管线跨 3 版本，改动难充分冒烟 → 须用 clientsmoke + RenderDoc 对比。
- MC RenderState API（1.21.2+）与旧 API 行为差异（overlay/light/partial 计算）需逐一核对。
- ST-D2 若走 bridge，规则 4（bridge→application）需 Port 回调设计，增加复杂度。

## 待用户确认
- ST-D 选 D1（版本源集，build 改动）还是 D2（bridge 外壳 + Port 回调）？
- asEmissive：26.1 移植（ST-C 真做）还是 bridge facade 桩（保留 26.1 no-op）？
