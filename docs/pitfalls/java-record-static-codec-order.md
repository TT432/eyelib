# Java Record 内 static Codec 字段初始化顺序导致 NullPointerException

## 症状

给 `BrClientEntity` record 新增字段后，所有相关测试抛出 `ExceptionInInitializerError`，
栈底是 `NullPointerException` 在 `lambda$wrapDescription$X`（Codec lambda 内）。

```
java.lang.ExceptionInInitializerError
    at BrClientEntityCodecTest.parsesClientEntitySchema...(BrClientEntityCodecTest.java:29)
Caused by: java.lang.NullPointerException
    at BrClientEntity.lambda$wrapDescription$8(BrClientEntity.java:120)
```

## 原因

Java 按声明顺序初始化类的 `static` 字段。如果 `CODEC`/`ATTACHABLE_CODEC` 用到了
`ITEM_FIELD_CODEC`，但 `ITEM_FIELD_CODEC` 定义在 `CODEC` 之后，则 `CODEC` 构造时
`ITEM_FIELD_CODEC` 还是 `null`。

```java
// 错误顺序——CODEC 初始化时 ITEM_FIELD_CODEC 为 null
public static final Codec<BrClientEntity> CODEC = wrapDescription("minecraft:client_entity");  // line 53
public static final Codec<BrClientEntity> ATTACHABLE_CODEC = wrapDescription("minecraft:attachable");

private static final Codec<Map<String, String>> ITEM_FIELD_CODEC = Codec.either(...);  // line 56

// 正确顺序——ITEM_FIELD_CODEC 在前
private static final Codec<Map<String, String>> ITEM_FIELD_CODEC = Codec.either(...);

public static final Codec<BrClientEntity> CODEC = wrapDescription("minecraft:client_entity");
public static final Codec<BrClientEntity> ATTACHABLE_CODEC = wrapDescription("minecraft:attachable");
```

## 正确做法

新增的 `static` Codec 字段定义必须放在所有引用它的 `static` Codec 字段**之前**。
用完 `private static final` 声明且放在最前面。
