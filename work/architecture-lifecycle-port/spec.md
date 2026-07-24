# 实体离开事件生命周期端口规格

## 问题

`bridge.client.render.adapter.EntityLeaveLevelEventAdapter` 直接调用 application 层 `AttachableItemRenderSetup.clearEntity`，违反 ADR-0016 的 ACL 不得反向依赖 Application 约束，导致 `ArchitectureTest.aclMustNotDependOnApplication` 失败。

## 变更

- 在既有 `ApplicationLifecyclePort` 增加实体离开回调。
- bridge 事件适配器仅把 `LivingEntity` 事件转发给已安装端口。
- application 实现负责调用 `AttachableItemRenderSetup.clearEntity`。

## 不变量

- 仅客户端、仅 `LivingEntity` 离开世界时清理 attachable 派生渲染缓存。
- 端口尚未安装时保持安全空操作。
- 不改变缓存键、清理时机或服务端行为。

## 验证

- `ArchitectureTest.aclMustNotDependOnApplication` 通过。
- qylEyelib 1.21.1 全量测试通过。
- 根项目构建通过。

## 验证结果

- `ArchitectureTest` 定向运行：通过。
- qylEyelib 1.21.1 全量测试：通过，原 ACL 反向依赖违规消失。
- motions4-root 根构建：通过。
