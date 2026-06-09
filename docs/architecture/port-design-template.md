# Port 接口设计规范

> 给子代理和人类开发者用的参考模板。所有 Port 接口必须遵守此规范。

## 设计原则

### 1. 粒度假说

> Port 只在被**至少两个 MC 代码路径**调用时才创建。单一 use case 不抽象——直接暴露具体类型给 bridge 内部。

反例：
```java
// ❌ 只有一个调用方——不要创建 Port
// BrShaderMapping.getShaderNames() 只在 BrRenderTypeFactory 中调用
// 直接整个文件迁移到 bridge
```

正例：
```java
// ✅ 5 个文件都依赖 StringRepresentable → 值得创建 Port
public interface PortStringRepresentable {
    String getSerializedName();
}
```

### 2. 语义驱动

Port 表达 domain 的**需求语义**，不是 MC API 的镜像。

```java
// ❌ 直接翻译 MC API——过细
public interface PortMinecraft {
    Level getLevel();
    Camera getCamera();
    float getPartialTick();
}

// ✅ 表达实际需求
public interface PortRenderContext {
    long dayTime();
    long gameTime();
    float cameraYaw();
    float cameraPitch();
}
```

### 3. 不依赖 MC 类型

Port 方法的参数和返回值只能用：Java 标准库、`eyelib-util` 中的类型、或同一个 domain 模块中的类型。

```java
// ❌ Port 引用了 MC 类型——等于没拆
public interface PortRenderPass {
    RenderType toRenderType(); // ← net.minecraft 类型
}

// ✅ 返回 domain 自有语义
public interface PortRenderPass {
    boolean requiresAlphaTest();
    boolean requiresBlending();
    boolean requiresCulling();
}
```

### 4. 返回值用 sealed 类型

需要返回多种类型的 Port 用 Java sealed class，而非 `Object`。

```java
// ❌ Object 丢失类型信息
public interface PortQuery {
    Object get(String key);
}

// ✅ 用 Java 标准类型，不额外封装
public interface PortEntity {
    Map<String, Object> getQueryProperties();
}
```

## 命名规范

| 位置 | 格式 | 示例 |
|------|------|------|
| 包路径 | `<模块>/port/` | `eyelib-material/src/main/java/.../port/` |
| 接口名 | `Port<语义>` | `PortRenderPass`, `PortEntity` |
| 实现类名（在 bridge 中） | `<语义>Adapter` | `RenderPassAdapter`（在 `eyelib-bridge` 中） |

## 接口模板

```java
/**
 * {一句话说清 Port 提供的抽象能力}。
 *
 * @author TT432
 */
@NullMarked
public interface PortXxx {

    // === 查询方法 ===

    /** {简短说明返回值语义} */
    ReturnType someProperty();

    /** {说明 null 约定} */
    @Nullable ReturnType optionalProperty();  // 可能返回 null

    // === 动作方法 ===

    /** {说明副作用} */
    void doSomething(InputType input);

    // === 工厂/静态辅助 ===

    static PortXxx of(Args args) { ... }
}
```

### 注解规则

- 所有 Port 接口加 `@NullMarked`
- 只有可能返回 null 的方法加 `@Nullable`（用在返回值上）
- 不使用 `@NotNull`（`@NullMarked` 下默认非 null）
- 简单 getter 不加 Javadoc（代码自明）

## 禁止项

| 禁止 | 原因 | 正确做法 |
|------|------|----------|
| Port 接口 import `net.minecraft.*` | 破坏 domain 隔离 | 用 Java 标准库或 domain 自有类型 |
| Port 方法返回 `Object` | 类型不安全 | 用 `sealed` 类型或泛型 |
| Port 接口放在 bridge 模块 | 依赖反向 | Port 由 domain 定义 |
| 一个 Port 接口超过 10 个方法 | 职责过宽 | 拆成多个窄接口 |
| 为每个 MC 类创建一个 Port | 机械翻译 | 按语义分组 |

## 现有 Port 接口参考

| 接口 | 位置 | 方法数 | 说明 |
|------|------|--------|------|
| `PortStringRepresentable` | eyelib-util | 1 + fromEnum | 枚举序列化 |
| `PortResourceLocation` | eyelib-util | 2 + parse | 资源路径标识 |
| `PortRenderPass` | eyelib-material/port/ | 3 + of | 渲染 pass 语义 |
| `PortEntity` | eyelib-molang/port/ | 1 | 实体属性查询 |
| `PortLevel` | eyelib-molang/port/ | 4 | 世界属性查询 |
| `PortItemStack` | eyelib-molang/port/ | 2 | 物品栈属性 |
| `PortFriendlyByteBuf` | eyelib-util | 0（接口保留） | 缓冲区抽象（StreamCodec 硬要求 MC 类型） |

## 特殊模式：fromEnum() 辅助方法

Port 接口如果用于替代枚举实现的 `StringRepresentable`，必须提供 `fromEnum()` 静态辅助方法用于 CODEC 创建：

```java
@NullMarked
public interface PortStringRepresentable {

    String getSerializedName();

    static <T extends Enum<T> & PortStringRepresentable> Codec<T> fromEnum(Supplier<T[]> values) {
        return Codec.STRING.xmap(
                name -> Arrays.stream(values.get())
                        .filter(e -> e.getSerializedName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown enum name: " + name)),
                PortStringRepresentable::getSerializedName
        );
    }
}
```

**关键点：**
- 泛型 `T extends Enum<T> & PortStringRepresentable` 确保类型安全
- `Supplier<T[]>` 而非 `T[]`，因为 Java 的 `T::values` 每次返回新数组
- CODEC 定义从 `StringRepresentable.fromEnum(T::values)` 改为 `PortStringRepresentable.fromEnum(T::values)`
