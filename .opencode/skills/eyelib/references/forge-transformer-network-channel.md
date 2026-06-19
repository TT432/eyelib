# Forge TRANSFORMER Classloader 网络 Channel 分裂诊断

> ⚠️ **ADR-0014 后状态**: 本文档描述的根因(multi-project 时代 TRANSFORMER classloader 为每个子模块 JAR 分别加载 `EyelibNetworkTransport`)已**结构性消失**——单 project 单 JAR 单 classloader 不再产生此问题。文档保留作为历史记录与反射诊断脚本的可复用参考。如未来网络编码再出 ClassCastException,先排查其他根因(packet 注册顺序、NetworkRegistry 重复注册、线程竞争等)。

## 背景(ADR-0014 前)

Forge dev 环境中每个子模块 JAR 由独立的 TRANSFORMER classloader 加载。当 `EyelibNetworkTransport`
包含 `static` 字段（`CHANNEL`、`discriminator`）且该类在 `eyelib-network` 模块中时，
不同模块的 classloader 可能各自加载一份 `EyelibNetworkTransport`，导致多份 `static` 状态。

## 症状

- 服务器 tick 时 `IndexedMessageCodec.tryEncode:121` 抛 `ClassCastException: ExtraEntityUpdateDataPacket cannot be cast to AnimationComponentSyncPacket`
- 崩溃发生在 `sendToTrackedAndSelf` → `CHANNEL.send` → `build()` → `types.get(message.getClass())`
- Forge 源码分析：`build()` 调用 `types.get(message.getClass())`（`Object2ObjectArrayMap`，用 `Objects.equals` 线性搜索），找到的 handler 其 encoder 类型不匹配
- 解码侧走 `indicies.get(discriminator)` 不受影响（index 查找，非 class 查找）

## 运行时诊断脚本

通过 `/eval` 反射检查运行时的 `types` map 状态：

```java
// 反射访问 EyelibNetworkTransport → CHANNEL → indexedCodec → types
Class<?> _tc = io.github.tt432.eyelib.network.EyelibNetworkTransport.class;
java.lang.reflect.Field _cf = _tc.getDeclaredField("CHANNEL"); _cf.setAccessible(true);
Object _ch = _cf.get(null);
java.lang.reflect.Field _if2 = _ch.getClass().getDeclaredField("indexedCodec"); _if2.setAccessible(true);
Object _co = _if2.get(_ch);
java.lang.reflect.Field _tf = _co.getClass().getDeclaredField("types"); _tf.setAccessible(true);
Object _tm = _tf.get(_co);
Class<?> _mc = _tm.getClass();
java.lang.reflect.Field _szF = _mc.getDeclaredField("size"); _szF.setAccessible(true);
int _sz = _szF.getInt(_tm);
java.lang.reflect.Field _keyF = _mc.getDeclaredField("key"); _keyF.setAccessible(true);
java.lang.reflect.Field _valF = _mc.getDeclaredField("value"); _valF.setAccessible(true);
Object[] _ks = (Object[]) _keyF.get(_tm);
Object[] _vs = (Object[]) _valF.get(_tm);
java.lang.reflect.Field _idxF = _vs[0] != null ? _vs[0].getClass().getDeclaredField("index") : null;
if (_idxF != null) _idxF.setAccessible(true);
StringBuilder _sb = new StringBuilder();
for (int _i = 0; _i < _sz; _i++) {
    int _idx = _idxF != null ? _idxF.getInt(_vs[_i]) : -1;
    String _kn = _ks[_i] == null ? "NULL" : ((Class)_ks[_i]).getSimpleName();
    _sb.append("[").append(_i).append("] disc=").append(_idx).append(" key=").append(_kn).append(" | ");
}
return _sb.toString();
```

### 预期正常输出

```
size=10, 所有 key 非 NULL, discriminator 连续 1-10 对应正确的 packet 类
```

### 分裂症状

```
[0] disc=-1  key=ModelComponentSyncPacket      ← discriminator 为 -1
[1] disc=1   key=NULL                           ← NULL key
[2] disc=3   key=ExtraEntityUpdateDataPacket    ← discriminator 错位（应为 2）
[3] disc=4   key=AnimationComponentSyncPacket   ← discriminator 错位（应为 8）
...
```

## 根因

`EyelibAttachmentMod`（eyelibattachment classloader）调 `registerClientPacket` 用的是它自己那份
`EyelibNetworkTransport` 实例。`EyelibNetworkManager.register()`（root classloader）用的是另一份。
每份有自己的 `static discriminator`、`CHANNEL`、`types` map。

当 `ExtraEntityUpdateDataRuntimeHooks.onLivingTick`（eyelibattachment classloader）调
`sendToTrackedAndSelf` 时，用的是 eyelibattachment 的 channel。而通过 `/eval`（root classloader）
读取到的是 root 的 channel。两个 map 不一致。

更重要的是：`NetworkRegistry.newSimpleChannel("eyelib:networking", ...)` 可能被调用了多次
（每个 classloader 一次），Forge 可能接受了重复的 channel name 或行为未定义。

## 修复方向

所有 packet 注册集中在 root 模块的 `EyelibNetworkManager.register()` 中。
子模块（eyelibattachment）的 `registerNetworkPackets()` 逻辑迁移到 root 模块，
确保所有注册通过同一个 `EyelibNetworkTransport` 实例。
