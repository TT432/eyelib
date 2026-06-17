# 网络 ClassCastException 根因与修复

## 症状

```
ClassCastException: ExtraEntityUpdateDataPacket cannot be cast to AnimationComponentSyncPacket
at IndexedMessageCodec.tryEncode:121
```

触发条件：服务器 tick 时任意实体触发 `ExtraEntityUpdateDataRuntimeHooks.onLivingTick` → `sendToTrackedAndSelf` → Forge 网络编码失败。

## 根因

`EyelibNetworkTransport` 被多个 TRANSFORMER classloader 各自加载了多份：

1. `EyelibAttachmentMod`（eyelibattachment classloader）调用 `registerClientPacket` → 使用 eyelibattachment 自己的 `EyelibNetworkTransport` 实例
2. `EyelibNetworkManager`（root classloader）调用 `registerClientPacket` → 使用 root 的 `EyelibNetworkTransport` 实例
3. 两个实例各有独立的 `static CHANNEL`（SimpleChannel）、`static discriminator` 计数器、各自的 `IndexedMessageCodec.types` 映射

结果：`types` map 出现 NULL key、错位 discriminator。编码时 `build()` 的 class 查找返回错误 handler。

## 验证方法

运行时 dump `types` map（/eval + 反射）：

```java
// types map 应该 size=10, 无 NULL key, discriminator 1-10 序正确
// 错误状态：size=10 但 [1]=NULL, [0]=disc-1, discriminator 错位
```

## 正确修复

所有包注册集中到 `EyelibNetworkManager.register()`（root classloader），`EyelibAttachmentMod` 移除全部网络代码。

```java
// EyelibNetworkManager.java — 集中注册
EyelibNetworkTransport.registerServerPacket(UpdateDestroyInfoPacket.class, ...);
EyelibNetworkTransport.registerClientPacket(ExtraEntityUpdateDataPacket.class, ...);
// ...全部 10 个 packet 注册
```

此后 `sendToTrackedAndSelf` 不再需要 try-catch（原有 try-catch 和 band-aid `ClassCastException` catch 均应移除）。
