# ECS 架构：ComponentStore 模式

基岩版 ECS → eyelib 复刻。

## 核心

```
ComponentStore  ← 所有 System 的唯一数据交汇点
    │
    ├── S₁: EventExecutor     → 写入 C（minecraft:variant = 2）
    ├── S₂: MolangQuery       → 读取 C（q.variant → store.get("minecraft:variant")）
    └── S₃: ...
```

System 之间只通过 Component 通信。S₂ 不需要知道 S₁ 的存在——它只知道"读取 minecraft:variant.value"，值从哪来的不关心。

## 测试策略

| ECS 层 | eyelib 示例 | 测试？ |
|---|---|---|
| E (Entity) | BrClientEntity, BrBehaviorEntityFile | 不测 |
| C (Component) | Variant, Health, Attack... | 不测（纯数据 record） |
| S (System) | 事件执行器, Molang 查询, 渲染管线 | **必须测** |

S 测试：往 ComponentStore 塞数据 → 调 System → 断言输出。不需要真实 entity 对象。

## Component 存储

```java
// bridge.molang 包: ComponentStore.java
public final class ComponentStore {
    private final Map<String, Object> values = new HashMap<>();
    public void put(String key, Object value) { values.put(key, value); }
    public <T> T get(String key) { return (T) values.get(key); }
}
```

key 是 Bedrock component 名（如 "minecraft:variant"），value 是 typed component 的值（如 Integer(4)）。

## S₁ 示例：spawn 事件

```java
// EntityJoinLevelEvent handler → 读取行为包事件
// → 随机选 component_group → 解析组件 → 写入 ComponentStore
ComponentStore store = new ComponentStore();
store.put("minecraft:variant", variant.value());
scope.getHostContext().put(MolangEntityContext.class, new MolangEntityContext(store));
```

## S₂ 示例：Molang 查询

```java
@MolangFunction("variant")
public static float variant(MolangEntityContext ctx) {
    ComponentStore store = ctx.componentStore();
    Number v = store.get("minecraft:variant");
    return v != null ? v.floatValue() : 0f;
}
```