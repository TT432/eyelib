# ADR-0046：client entity 绑定语义二分与解码规范化钩子

> **状态**：已实施（2026-11-17）
> **关联**：ADR-0019（attachable 缓存生命周期）、ADR-0043（装备挂载抽象）
> **触发**：`docs/gap-analysis/downstream-consumer-gaps.md` G5 / G6

---

## 1. 问题

**G5**：`ClientEntityComponent` 用两个布尔（`externallySet`/`resolvedGeneration`）表达绑定来源，语义靠「引用是否相同」推断：
`setupClientEntity(String, cap)` 在 stale 时按 id 重新查表并**覆盖**外部显式设置的实例；`setupClientEntity(BrClientEntity, cap)` 只在引用不同时才写入。同族 API 两种语义，下游只能自建代际监听 + 逐 tick 重放，且「首次特效不渲染、第二次同 id 才生效」正是 String 重载覆盖所致。

**G6**：`Toggles` 的 `hide_armor` 等默认值硬编码在 codec 里，客户端实体有 8 条解码入口（资源包 loader、addon 逐文件/brarchive/聚合、GUI 导入批量与单文件、nodegraph 预览），没有 SPI；下游只能 mixin 改 `Toggles.CODEC` 的 `<clinit>`，库升级即碎。更关键的机制：DFU 的 `optionalFieldOf(name, literalDefault)` 在**编码**侧用「与字面缺省结构相等」决定是否省略键，因此「解码后 hook」无法表达「JSON 未写该键时取 true」——解码后 `false` 与「显式 false」不可区分，而把缺省改成 true 又会让导出把键写回文件。

## 2. 决策

### 2.1 绑定二分（G5）

```java
enum BindingKind { AUTO_BY_ID, INSTANCE }
bindById(String entityId)                    // 参与代际失效；重复同 id 幂等；不清空当前实例（避免闪一帧原版）
bindInstance(@Nullable BrClientEntity ce)    // 永久固定；同引用幂等；null = 显式「无模型」
applyResolved(@Nullable ce, long generation) // 唯一写 resolvedGeneration 的入口；非 AUTO 直接丢弃
needsRebind(long currentGeneration)          // 仅 AUTO 且代际落后
```

- `setupClientEntity(String, cap)` 只对 `AUTO_BY_ID` 生效：**不再覆盖固定实例**。`setupClientEntity(ce, cap)` 在引用变化时转 `INSTANCE`；新增 `setupClientEntity(cap)` 按当前绑定装配（不改绑定、不重解析）——这是下游「已知绑定方式」时应使用的入口。
- 删除 `setClientEntity` / `markResolvedFrom` / `isStale`（编译期暴露全部调用点）。
- 解析宽容度（JE→BE 别名、`ns:path → path`）收敛到 `ClientEntityManager.byId`；`boundId` 永远保存调用方**原始** id，因此后续改变查找规则不影响任何绑定。
- 注册表新增类型化、可注销的变更事件 `Registry.addListener(Consumer<RegistryChange<T>>)`（`ENTRY_PUT` / `ENTRY_REMOVED` / `BULK_REPLACED`，载荷含变更后代际与完整快照）。既有 `ManagerEventPublisher` → Forge 总线通道保持不动。
  **正确性机制仍是代际惰性比较**，事件只用于通知/清理：事件可能因 publisher 未安装（`FMLClientSetupEvent` 之前）、线程乱序或批量无明细而丢，惰性比较不会。

### 2.2 解码规范化与缺省值（G6）

```java
BrClientEntityNormalizer.normalize(decoded) → decoded          // 注册表：register / registerFirst / close
ClientEntityTogglesDefaults.register(HIDE_ARMOR, true)         // 四字段缺省（enable_attachables / hide_armor /
                                                               // held_item_ignores_lighting / held_item_scale）
```

- **唯一收口点 = 两个公开 codec 常量**（`CODEC` / `ATTACHABLE_CODEC`）：整实体归一化包在 codec 外层（`comapFlatMap` 只作用于解码方向，编码恒等）；`Toggles` 的四个字段改为「动态缺省」——解码缺键取注册值，**编码省略集 = 当前生效缺省集**（与 DFU 原生语义同构，「缺省不写回」不变量因此保持，甚至加强）。
- 字段缺省的粒度**只能全局**：`Toggles` 是 description 内的嵌套 `MapCodec`，解码时看不到 `identifier`；按实体 id 的规则放在整实体 normalizer（能读 `identifier`）。
- 注册在客户端初始化完成（早于首次资源重载），资源重载**不清空**注册表（否则缺省失效）。

## 3. 后果

- 下游删除：自建代际重放器、按代际整表重建、`isStale` 幂等检查、`Toggles.CODEC` mixin 与 codec 包装器（改为一行注册）。
- **有意行为变化**：① String/Entity 重载不再覆盖固定实例（今天靠 `externallySet` 偶然成立，现为显式契约；库内无依赖该 bug 的调用点）；② `bindById` 的解析 id 优先取 `boundId` 而非宿主类型 id（新能力，无绑定记录时行为不变）；③ 注册缺省后「显式写出且等于当前缺省」的键在导出时被省略（语义等价）。
- 风险：`INSTANCE` 冻结意味着资源重载后旧实例继续渲染（比自动回落原版更隐蔽）。缓解：`ClientEntityManager.addListener` 供持有者主动清理；`RenderFrame.clientEntity()` 与诊断可用于发现「当前实例不在快照内」。
- 未覆盖：不做按 entity id / 模型 id 的缺省表；不把 `@MolangFunction` 设为唯一注册来源。

## 4. 验证

- 单测：`ClientEntityComponentBindingTest`（代际失效、固定实例冻结、同引用幂等、`bindById` 不清空实例、未请求的解析结果不覆盖）、`RegistryListenerTest`（事件种类/载荷/异常隔离/注销/批量单事件）、`BrClientEntityDefaultsTest`（注册缺省后解码生效、**编码不写回**、显式 false 保留、注销回落）、`BrClientEntityNormalizerTest`（顺序、幂等、空表同一实例）、`BrClientEntityCodecTest` 保持绿。
- 三节点 `mcmcp_build` / `test`；`ArchitectureTest` 零新增冻结（新类全在 domain，无 `net.neoforged.*`）。
- 运行时：`F3+T` 后 `bindById` 的模型自动切换、`bindInstance` 的模型保持；下游 NPC/作物模型不再丢失。
