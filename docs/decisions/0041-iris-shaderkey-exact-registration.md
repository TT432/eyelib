# ADR-0041：26.1 自定义实体 pipeline 必须按 ShaderKey 精确登记——光影下不透明实体整体消失

## 状态

已接受（2026-11-05）。修复 26.1.2 + Iris 1.11.4 + Complementary Reimagined 下不透明材质实体（cospack 材质
`entity`）光影开启后完全不可见。

## 背景

症状（dokimobs 消费方实机，26.1.2）：开启光影后，cospack 中材质为 `entity` 的鸡、猪、牛、哞菇**只剩地上的
阴影**，模型本体完全不渲染；同场景材质为 `cutout` / `translucent` 的羊、村民正常。关闭光影一切正常。

复现（26.1.2 dev 客户端 + Iris 1.11.4 + Sodium 0.9.2 + ComplementaryReimagined r5.9.3，同机位 A/B）
确认：`packInUse=true` 时 4 只 `entity` 材质实体全灭，对照组正常；关闭光影后 6 只全部可见。

### 根因（实证）

1. 26.1 的实体材质走 `BrRenderTypeFactory.createForEntityPass` 构造自定义 `RenderPipeline`（ADR-0040），
   再由 `IrisPipelineCompat.assign` 调 `IrisApi.assignPipeline(pipeline, IrisProgram.ENTITIES)` 登记。
2. 该 API 内部是 `IrisPipelines.assignPipeline(pipeline, ShaderKey.findBestMatch(pipeline, programId))`。
   `findBestMatch` 的「okay」分支条件为 *program 相同 + vertex format 相同*，并**按枚举声明顺序返回第一个**
   命中的键：
   ```
   ENTITIES_ALPHA(AlphaTests.VERTEX_ALPHA)
   ENTITIES_SOLID(AlphaTests.OFF)        ← 本应命中
   ...
   ENTITIES_CUTOUT(ONE_TENTH_ALPHA)
   ```
   `ENTITIES_ALPHA` 声明在 `ENTITIES_SOLID` 之前，因此**任何不带 `ALPHA_CUTOUT` define 的实体 pipeline**
   都会拿到 `ENTITIES_ALPHA`。
3. `AlphaTests.VERTEX_ALPHA = new AlphaTest(AlphaTestFunction.NEVER, Float.MAX_VALUE)`：片元 alpha 测试
   永不通过 → 全部片元被丢弃 → 模型不绘制。阴影通道走另一个键（`SHADOW_ENTITIES_CUTOUT`，alpha 阈值
   0.1），所以影子照常出现。
4. 为什么只有 `entity` 材质中招：cospack 里只有它没有 defines/states（→ `Transparency.NONE` → OPAQUE），
   `buildPipeline` 不加 `ALPHA_CUTOUT`；`cutout` 材质带 `ALPHA_CUTOUT`，走 `findBestMatch` 的「perfect」
   分支命中 `ENTITIES_CUTOUT`；`translucent` 材质程序为 `ENTITIES_TRANSLUCENT`（`EntitiesTrans` 下唯一
   ENTITY 格式键），也不受键顺序影响。

日志实证（26.1.2 dev，修复前）：

```
[Render thread/WARN] [Iris/]: Found okay program match for eyelib:material_b5ac51ac: ENTITIES_ALPHA      ← 不透明材质
[Render thread/WARN] [Iris/]: Found perfect program match for eyelib:material_8dd53f83: ENTITIES_CUTOUT  ← 镂空材质
```

## 决策

`IrisPipelineCompat` 不再依赖 `IrisApi.assignPipeline` 的模糊匹配，改为反射调用 Iris 内部
`IrisPipelines.assignPipeline(RenderPipeline, ShaderKey)` / `assignPipelineShadow(RenderPipeline, ShaderKey)`
直接指定键：

| surfaceClass | 主通道键 |
|---|---|
| GLINT | `GLINT` |
| EMISSIVE_CUTOUT / TRANSLUCENT_EMISSIVE | `ENTITIES_EYES_TRANS` |
| EMISSIVE | `ENTITIES_EYES` |
| TRANSLUCENT / ADDITIVE | `ENTITIES_TRANSLUCENT` |
| CUTOUT | `ENTITIES_CUTOUT` |
| 其余（OPAQUE） | `ENTITIES_SOLID` |

阴影通道统一 `SHADOW_ENTITIES_CUTOUT`（`ShadowEntities` 程序下唯一的 ENTITY 格式键）。

键映射与 Iris 给 vanilla pipeline 的映射（`IrisPipelines.getSolid/getCutout/getTranslucent`）对齐，
使自定义 pipeline 与同材质 vanilla RenderType 在光影里走同一程序。

内部句柄（`IrisPipelines` 类、`ShaderKey` 枚举）以**惰性 + 可重试**方式解析（`ExactKeys` 嵌套类），
不在接口静态字段里一锤定音：`ShaderKey` 的静态初始化会拉入 Iris 顶点格式，在管线未就绪的时机被
触发会失败，而静态字段一旦为 null 就永久回退到 `IrisApi`（实证：同一构建下曾整会话静默退化，
日志只有 `Found okay program match ... ENTITIES_ALPHA`，画面即不显示）。解析用
`Class.forName(name, false, loader)` 避免提前初始化，`Enum.valueOf` 时再触发。

内部 API 不可用时（Iris 改类名/方法签名）退回原 `IrisApi.assignPipeline` 路径并打 warn，行为与修复前一致，
不会崩溃。

## 后果

- 26.1 光影下不透明实体恢复正常渲染；cutout/translucent/emissive 材质的键与修复前一致（`ENTITIES_CUTOUT` /
  `ENTITIES_TRANSLUCENT` / `ENTITIES_EYES_TRANS`），无观感变化。
- 依赖 Iris 内部类名与枚举常量名（`net.irisshaders.iris.pipeline.IrisPipelines`、
  `net.irisshaders.iris.pipeline.programs.ShaderKey`）。Iris 变更时由回退路径兜底，届时会退化回
  `findBestMatch` 的歧义行为——需要跟踪 Iris 版本升级时复查。
- 1.20.1 / 1.21.1 不受影响（该桥接类只在 `>=26.1` 编译；1.21.1 的 Iris 按 ShaderInstance getter 接管，
  实测 6 只实体在光影开关两种状态下均正常）。

## 验证

26.1.2 dev 客户端（Iris 1.11.4 + Sodium 0.9.2 + ComplementaryReimagined r5.9.3，`spawn_chance=1.0`，
固定机位同场景：鸡/猪/牛/哞菇 + 对照羊/村民 + 村民/狼/掠夺者/烈焰人/监守者）：

| 条件 | 结果 |
|---|---|
| 修复前（eyelib 21.1.27），光影开 | 4 只 `entity` 材质实体只剩地上阴影；同排 cutout 材质实体正常 |
| 修复后（21.1.28），光影开 | 同机位 5 只模型全部可见（`doki26_fixed_21128.png`） |
| 修复后日志 | `program match for eyelib` 计数为 0（精确键路径生效），无 `[compat]` 警告 |
| 修复后，光影关 | 全部实体正常（回归对照） |
| 1.21.1（Iris 1.8.12 + MakeUp-UltraFast）光影开 | 6 只实体（含 4 只 `entity` 材质）全部可见；该版本不走本桥接类 |

## 未覆盖

- 1.20.1 + Oculus 实机（本机无该实例）；该节点不编译本桥接类，渲染路径与 1.21.1 同构。
- 其他光影包（BSL / photon / MakeUp）在 26.1.2 上的逐包复验：本次只验证 Complementary Reimagined；
  该修复作用于管线登记层，与具体光影包无关。
- 与本修复无关的既有缺陷（已记录为反馈，未在本次处理）：26.1.2 + Iris 1.11.4 下进入**特定世界**
  （大量原版实体 + 光影）偶发 `IllegalStateException: Missing sampler Sampler1`
  （`GlCommandEncoder.trySetup`）。修复前（21.1.27）与修复后（21.1.28）均可复现，非本次回归。
