# 实体渲染验证场地生成

## MobArenaGenerator

位置：`src/main/java/io/github/tt432/eyelib/debug/MobArenaGenerator.java`

程序化生成全场生物展示场地，所有实体放在多层玻璃围栏中。

### 调用方式

```java
// 从 /eval 通过反射调用（根模块类不在 Janino classpath）
Class.forName("io.github.tt432.eyelib.debug.MobArenaGenerator")
    .getMethod("generate", ServerLevel.class, BlockPos.class)
    .invoke(null, serverLevel, origin);
```

### 设计

- 遍历 BuiltInRegistries.ENTITY_TYPE，跳过末影龙/凋零/玩家
- type.create() → instanceof LivingEntity 过滤
- getBbWidth()/getBbHeight() 计算 cage 尺寸（w+1, h+1）
- 按水生/飞行/小/中/大 分五组
- 4 层阶梯排列 + 水箱
- buildCage: 玻璃地板+四壁（飞行加顶）
- buildWaterTank: 玻璃底+水+四壁+顶
- 按宽度降序排列（大 cage 先放）

### 陷阱

- 某些实体无法在 overworld 创建（Warden 需要 sculk shrieker）→ classificiation 中 try-catch 跳过
- 某些实体 addFreshEntity 时触发 `io.github.tt432.eyelib.behavior` ClassCastException → summonAll 中 try-catch 跳过
- 注意 Janino 访问限制：根模块类不能用 Class.forName()，但反射调用可以（已是加载的 class）
