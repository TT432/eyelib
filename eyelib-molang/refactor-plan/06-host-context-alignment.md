# P6：HostContext 设计对齐 — 解决 Class<?> vs HostRole<T> 鸿沟

## 问题类型

**设计-实现鸿沟**：19份设计文档明确规定 `HostRole<T>` 为规范语义术语，明确指出 "Java Class<?> is a publication aid, not the user-visible contract"。但实现中 `HostContext` 使用 `Class<?>` 原始查找。

## 可证明证据

### 证据 E1 — 设计文档的规定

文件：`eyelib-molang/design/shared-vocabulary-and-phase-ownership-draft.md`

关键引用（需验证原文）：
- `HostRole<T>` 被定义为规范语义术语
- `HostContext` 被定义为持有 `HostRole` → value 的映射
- `Class<?>` 被明确称为 "publication aid, not the user-visible contract"

### 证据 E2 — 实现违背设计

文件：`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/HostContext.java`

```java
public final class HostContext {
    private final ConcurrentHashMap<Class<?>, Object> store = new ConcurrentHashMap<>();

    public <T> Optional<T> get(Class<T> key) {
        return Optional.ofNullable(key.cast(store.get(key)));
    }

    public <T> void put(Class<T> key, T value) {
        store.put(key, value);
    }
}
```

**观察**：
- 使用 `Class<?>` 作为 key（设计文档明确反对的做法）
- 无 `HostRole<T>` 类型
- 无类型安全的角色区分能力

### 证据 E3 — MolangScope 仍保留旧的 owner 模式

文件：`eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/MolangScope.java`

包含：
```java
@Deprecated(forRemoval = true)
public MolangObject owner() {
    return MolangNull.INSTANCE;
}
```

以及：
```java
private final ConcurrentHashMap<Class<?>, Object> hostContextStore = new ConcurrentHashMap<>();
```

### 证据 E4 — MolangOwnerSet 已删除

`MolangOwnerSet.java` 不存在于磁盘。ROADMAP 第113行称迁移"未开始"，但文件已被删除。

## 问题分类

这不是纯技术缺陷，而是**架构决策待定**。有两个方向：

### 方向 A：实现设计文档中的 `HostRole<T>` 模式

按设计文档实现类型安全的 `HostRole<T>` 系统，替换 `Class<?>` 原始查找。

**优点**：
- 类型安全（编译时验证）
- 与全部19份设计文档一致
- 支持同一类型的多个角色（如多个 String 类型的角色）

**缺点**：
- 需要定义 `HostRole<T>` 类型和注册机制
- 需要迁移所有现有 `Class<?>` 调用方
- 工作量较大

### 方向 B：接受 `Class<?>` 为最终方案，更新设计文档

将当前 `Class<?>` 实现标记为最终方案，更新设计文档移除 `HostRole<T>` 引用。

**优点**：
- 零代码改动
- 与现有 MolangScope 兼容

**缺点**：
- 丢弃了类型安全
- 无法区分同一 Java 类型的多个角色
- 与19份设计文档矛盾

## 业已验证的解决模式

### 模式：类型安全异构容器（Effective Java Item 33）

**来源**：Joshua Bloch, Effective Java 第3版

```java
// 类型安全的 key，携带值类型信息
public final class HostRole<T> {
    private final String name;
    private final Class<T> type;
    
    private HostRole(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }
    
    public static <T> HostRole<T> of(String name, Class<T> type) {
        return new HostRole<>(name, type);
    }
    
    public Class<T> type() { return type; }
    public String name() { return name; }
}

// 类型安全的容器
public final class HostContext {
    private final Map<HostRole<?>, Object> store = new ConcurrentHashMap<>();
    
    public <T> Optional<T> get(HostRole<T> role) {
        return Optional.ofNullable(role.type().cast(store.get(role)));
    }
    
    public <T> void put(HostRole<T> role, T value) {
        store.put(role, value);
    }
}
```

**关键优势**：`HostRole<String> PLAYER_NAME` 和 `HostRole<String> ENTITY_TYPE` 是两个不同的 key，即使它们的值类型都是 `String`。这是 `Class<?>` 做不到的。

### 模式：渐进式迁移

不需要一次性替换全部 `Class<?>`。可以同时支持两种 API，逐步迁移：

```java
public final class HostContext {
    // 旧的 Class<?> API（过渡期保留）
    @Deprecated
    public <T> Optional<T> get(Class<T> key) { ... }
    
    // 新的 HostRole<T> API（推荐）
    public <T> Optional<T> get(HostRole<T> role) { ... }
}
```

## 执行计划

### Step 1：确认设计意图

需要决策：**方向 A（实现 HostRole）还是方向 B（接受 Class）？**

**推荐方向 A**，理由：
1. 19份设计文档的一致性
2. 类型安全优势
3. 同一类型多角色的支持能力
4. 与 Phase 4 host/query bridge 设计的兼容性

### Step 2（如选方向A）：定义 `HostRole<T>`

在 `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/mapping/api/` 下创建：

```java
package io.github.tt432.eyelibmolang.mapping.api;

/**
 * 类型安全的宿主角色标识符。
 * 
 * 不同于 {@link Class}，HostRole 允许同一 Java 类型的多个不同角色
 * （例如两个 HostRole<String> 可以有不同的语义）。
 *
 * @param <T> 该角色关联的值类型
 */
public final class HostRole<T> {
    // 见上方的完整实现
}
```

### Step 3（如选方向A）：扩展 `HostContext` 支持双 API

在现有 `HostContext` 中添加 `HostRole` 重载方法，保留 `Class` 版本标记 `@Deprecated`。

### Step 4（如选方向A）：迁移调用方

逐步将 `Class<?>` 调用方迁移到 `HostRole<T>`：
- `MolangScope.hostContextStore` 
- `MolangRuntimeSupport.computeAvailableHostRoles()`
- 所有 `mapping/api/` 下的消费者

### Step 5（如选方向B）：更新设计文档

在 `shared-vocabulary-and-phase-ownership-draft.md` 中：
- 移除 `HostRole<T>` 引用
- 将 `Class<?>` 标记为最终方案
- 更新 `host-injection-api-draft.md` 相应章节

### Step 6：更新 ROADMAP

将 ROADMAP 第113行从：
```
Phase 4 MolangOwnerSet→HostContext migration (deferred, not yet performed)
```
改为准确描述当前状态。

### Step 7：运行验证

```bash
jetbrain_run_gradle_tasks :eyelib-molang:test
```

## Check-list

- [ ] Step 1：确认方向 A 或 B
- [ ] 如选A → Step 2：创建 `HostRole<T>` 类
- [ ] 如选A → Step 3：扩展 `HostContext` 双 API
- [ ] 如选A → Step 4：迁移调用方
- [ ] 如选B → Step 5：更新设计文档
- [ ] Step 6：更新 ROADMAP.md MolangOwnerSet 状态
- [ ] Step 7：`jetbrain_run_gradle_tasks :eyelib-molang:test` 通过
- [ ] 确认 `MolangScope.owner()` 是否可移除
