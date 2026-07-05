# 跨 ClassLoader 网络注册诊断

> ⚠️ **ADR-0014 后状态**: 本文档描述的根因(multi-project 时代 TRANSFORMER classloader 分裂)已**结构性消失**。`EyelibNetworkTransport` 现在在单 project 单 JAR 中,所有调用方共享同一份 class 与 static 状态。文档保留作为历史记录与反射诊断脚本的可复用参考。

## 问题特征(ADR-0014 前)

`ClassCastException: ExtraEntityUpdateDataPacket cannot be cast to AnimationComponentSyncPacket`
发生在 `IndexedMessageCodec.tryEncode:121`（`encoder.accept(message, target)`）。

## 根因

Forge TRANSFORMER 为每个 JAR 创建独立 classloader。若 `EyelibNetworkTransport`（含 `static CHANNEL`、`static discriminator`）被多个 classloader 分别加载，每份独立实例各自维护自己的 `types` map 和 discriminator 计数器。Forge 底层用 fastutil 的 `Object2ObjectArrayMap`（`Objects.equals` 比较 Class key），key 数组出现 NULL 和错位 discriminator 时 class 查找返回错误 handler。

## 诊断步骤

### 1. 读 Forge 源码确认 crash 位置

`IndexedMessageCodec.java`（Forge `net.minecraftforge.network.simple` 包）：

```java
// build() line 129: types.get(message.getClass()) → class-based lookup
// tryEncode() line 121: encoder.accept(message, target) → CCE 发生点
```

### 2. 字节码确认

```bash
javap -c -p IndexedMessageCodec.class
```

`lambda$tryEncode$4` 签名为 `(FriendlyByteBuf, MessageHandler, Object, BiConsumer)V` — 全是擦除类型，CCE 来自 encoder 内部的 CHECKCAST。

### 3. /eval 运行时检查 types map

```java
// 反射链: EyelibNetworkTransport → CHANNEL → indexedCodec → types
Class<?> tc = EyelibNetworkTransport.class;
Field cf = tc.getDeclaredField("CHANNEL"); cf.setAccessible(true);
Object ch = cf.get(null);
Field if2 = ch.getClass().getDeclaredField("indexedCodec"); if2.setAccessible(true);
Object co = if2.get(ch);
Field tf = co.getClass().getDeclaredField("types"); tf.setAccessible(true);
Object tm = tf.get(co);
// Object2ObjectArrayMap 字段: key[], value[], size
Field szF = tm.getClass().getDeclaredField("size"); szF.setAccessible(true);
int sz = szF.getInt(tm);
Field keyF = tm.getClass().getDeclaredField("key"); keyF.setAccessible(true);
Object[] ks = (Object[]) keyF.get(tm);
Field valF = tm.getClass().getDeclaredField("value"); valF.setAccessible(true);
Object[] vs = (Object[]) valF.get(tm);
// MessageHandler.index 字段确认 discriminator
Field idxF = vs[0].getClass().getDeclaredField("index"); idxF.setAccessible(true);
for (int i = 0; i < sz; i++) {
    int idx = idxF.getInt(vs[i]);
    String kn = ks[i] == null ? "NULL" : ((Class)ks[i]).getSimpleName();
    // 输出: disc1=UpdateDestroyInfoPacket disc2=ExtraEntityUpdateDataPacket ...
}
```

### 4. 判断标准

| 正常 | 异常 |
|---|---|
| `size=10`，全部 discriminator 1-10 有序 | NULL key、discriminator 错位、size ≠ 10 |
| 所有 key 非 null | `size=10` 但 key[1]=NULL |

## 修复方案

所有包注册集中到 root classloader 的 `EyelibNetworkManager.register()`，禁止子模块 `@Mod` 类触碰 `EyelibNetworkTransport`。

## 反模式

- try-catch 掩盖 CCE（packet 静默丢弃，不报错但行为错误）
- 自行维护 encoder map 绕过 Forge class 查找（治标不治本）
- 子模块各自调用 `registerClientPacket`（重蹈覆辙）
