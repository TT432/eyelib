# ADR-0034：Wintersky 基岩粒子运行时的 as-is Java 移植

## 状态

已接受（2026-09-29，用户决策）。用户指令原文："对 wintersky 进行 as-is 的翻译、迁移，
迁移完成后再设计如何融入 eyelib。不限制预算，质量优先"。融入方案（渲染后端、molang
引擎替换、与既有 `eyelib.particle` 模块的关系）明确**不属于本 ADR 范围**，在迁移完成
验证后另行设计（见"后续工作"节）。

## 背景

- [wintersky](https://github.com/JannisX11/wintersky) 1.3.3 是 Blockbench 与 Snowstorm
  共用的基岩版粒子预览运行时（MIT），建立在 three.js（r134）+ molangjs（1.6.6）+
  tinycolor2（1.4.2）之上，共 8 个源文件 ~66KB（`build/_wintersky_src`，commit
  `a94add6`）。
- eyelib 已有规范驱动的 `eyelib.particle` 模块（Codec+组件化、JOML 数学），与
  wintersky 架构完全不同。本移植不与该模块共享代码，作为独立参考实现存在。
- 价值：wintersky 是社区事实标准的"基岩粒子在游戏外应该如何表现"的实现，拥有它
  的逐行为等价 Java 版，可作为既有 particle 模块的对照 oracle，也为融入提供候选
  运行时。

## 决策

### 1. 模块归属与边界

新顶层模块 `io.github.tt432.eyelib.wintersky`，**零 MC/Forge 依赖**（ArchUnit
domain 隔离天然合规），可纯 JUnit 测试。包布局：

| 包 | 内容 | 上游来源 |
|---|---|---|
| `wintersky.molang` | molangjs 1.6.6 逐字移植（`Molang`、`MathUtil`） | `molangjs/src/{molang,math}.js` |
| `wintersky.tinycolor` | tinycolor2 1.4.2 子集逐字移植（hex 解析、`toRgb`、`mix`、`toHex8String`） | `tinycolor2/tinycolor.js` |
| `wintersky.three` | three.js r134 最小子集逐字移植（仅 wintersky 触达的 API） | `three@0.134.0/src` |
| `wintersky`（根包） | wintersky 1.3.3 本体：`Scene`/`Config`/`Emitter`/`Particle`/`EventClass`/`MathUtil`/`Normals`/shader 常量/内置纹理资源 | `wintersky/src` |

three 子集清单（由 `grep THREE\.` 实证）：`Vector2/Vector3/Quaternion/Euler/Object3D/
Plane/Line3/Sphere/SplineCurve/CubicBezierCurve/BufferAttribute/PlaneGeometry/Mesh/
ShaderMaterial/Texture` + 常量（`NormalBlending/AdditiveBlending/FrontSide/DoubleSide/
NearestFilter`）；`Matrix4` 与 `Curve` 作为上述类的内部依赖一并移植。

### 2. as-is 的判定标准

- 类名、方法名、字段名、控制流、运算符优先级、默认值与 JS 源逐行对应；注释保留
  关键原文。仅做语言惯用法的最小改写（`let`→局部变量、对象字面量→小记录类/Map、
  原型方法→实例方法）。
- JS number ↔ Java `double`；`THREE.BufferAttribute` 的 `Float32Array` ↔ `float[]`
  （与 three.js 内部精度分层一致：场景图数学 double、顶点属性 float32）。
- **不**接入 `eyelib.molang`：molangjs 求值语义（角度制三角、分配持久化、
  `variableHandler` 回退、`??` 经 `found_unassigned_variable` 实现、查询函数经
  context 函数/variableHandler 兜底等）与 eyelib 编译型引擎存在可观察差异，替换
  即破坏 as-is。molangjs 本体仅 ~560 行，逐字移植成本低于等价性论证。引擎替换
  留作融入阶段的显式决策项。

### 3. 随机数注入

JS 侧随机源是全局 `Math.random()`（wintersky 出生点/初速/事件 randomize，
molangjs `math.random`/`die_roll` 系列）。Java 侧收敛为可注入的
`java.util.random.RandomGenerator`（`wintersky.WinterskyRandom` 持有者），默认
`new Random()`（语义等价），测试注入 mulberry32 实现与 Node oracle 对齐。这是
对 as-is 的唯一有意偏离，目的是确定性验证；所有调用点签名不变。

### 4. 验证：Node golden oracle

- `scripts/wintersky-oracle/`（纳入版本控制）：Node 脚本以
  `wintersky@1.3.3 + three@0.134.0 + molangjs@1.6.6 + tinycolor2@1.4.2` 运行，
  覆写 `Math.random` 为 mulberry32(seed)，对夹具粒子 JSON 逐 tick dump
  发射器/粒子状态（位置、旋转、UV、颜色、存活数、emitter age）为 JSON。
- Java 侧 JUnit 5：注入同一 mulberry32 算法逐 tick 运行移植版，与 dump 比对，
  容差 1e-4（float32 属性截断 + 末位 ulp 差异）。
- 夹具：Mojang `bedrock-samples` vanilla particle 定义（官方一手数据，优先级
  见 eyelib skill 测试 oracle 排序）+ 手工夹具补 vanilla 未覆盖分支
  （bezier_chain、events sequence/randomize、collision、parametric motion、
  各 facing 模式）。
- 分层验证顺序：molang 表达式求值对照 → three 数学/曲线对照 → wintersky 全链路
  逐 tick 对照。

### 5. 许可证

wintersky、three.js、molangjs、tinycolor2 均为 MIT。移植文件头部保留上游
版权声明与来源链接；模块 `package-info.java` 注明第三方来源汇总。

## 后果

- 新模块与既有 `eyelib.particle` 零耦合，可独立演进、独立测试；融入阶段前不影响
  任何现有行为。
- 保真度由 golden oracle 保证，任何"翻译笔误"类偏差会在逐 tick 比对中暴露。
- three 子集只含 wintersky 触达的 API——若未来融入设计需要更多 three 表面，按
  同一逐字原则增量移植。
- 已知有意偏离登记（本 ADR 为权威清单，实现中新增偏离必须回写此处）：
  1. `Math.random()` → 可注入 `RandomGenerator`（决策 3）。
  2. 内置纹理 PNG（`assets/*.png`）作为 classpath 资源加载；JS 侧 rollup
     base64 内联 ↔ Java `getResourceAsStream`，字节内容一致。
  3. JS 宽松类型通道（如 config 值可以是 number 或表达式字符串）以 `Object`/
     小密封类型承载，运行时分支与 JS 一致。

## 后续工作（本 ADR 不覆盖）

- 融入设计：渲染后端（three 网格/材质表面 → MC 渲染管线/bridge）、纹理获取钩子
  （`fetchTexture`/`fetchParticleFile` → MC 资源管理器）、molang 引擎是否切换
  `eyelib.molang`、与既有 `eyelib.particle` 运行时的关系（oracle/替换/并存）。
- 输出物：`docs/concepts/` 或新 ADR，迁移验证完成后进行。

## 验证

（移植完成后回填：三版本 compileJava、`:1.20.1:test` 含 oracle 对照、
nullawayMain、MODULES.md 重新生成。）
