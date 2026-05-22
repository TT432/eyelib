# Eyelib Track Module

## Scope
- Path: `eyelib-track/src/main/java/io/github/tt432/eyelibtrack/`
- 为 ItemStack 提供追踪基础设施：单调递增 ID 分配、NBT 持久化、按 ID 缓存数据的通用容器。
- 处理 ItemStack 拆分/合并/克隆/同步等边缘场景。
- 不持有业务数据——具体缓存类型（如 `RenderData<ItemStack>`）由根模块绑定。

## Layout
- `api/`: 公开 API（`ItemTrackApi` —— 获取/分配/比较追踪 ID）。
- `cache/`: 服务端 ID 生成器（`ItemStackIdCache`）和客户端通用缓存容器（`ItemTrackStateCache<T>`）。
- `mixin/common/`: 混合注入：`ItemStackMixin`（拆分+标签比较）、`AbstractContainerMenuMixin`（克隆+槽位同步）、`LivingEntityMixin`（装备同步）。

## Ownership Rule
- 只提供追踪/缓存基础设施，不持有领域数据（RenderData / AnimationComponent 等由消费方管理）。
- 依赖 `eyelib-animation`、`eyelib-attachment`、`eyelib-util`、`eyelib-molang` 作为工具支撑。
- 根模块创建具体绑定（如 `ItemTrackStateCache<RenderData<ItemStack>>`）并实现渲染。
