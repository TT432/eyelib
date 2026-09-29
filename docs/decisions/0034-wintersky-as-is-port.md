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

## 补充决策（2026-09-29，根包移植期间发现）

### 6. as-is 权威源：npm dist 1.3.3，而非 git master src

根包移植期间发现 npm 包 wintersky@1.3.3 的 `dist/wintersky.cjs.js` 构建自**更老的
提交**，与同包 `src/`（= git master）存在行为分歧。Blockbench/Snowstorm 通过 npm
依赖消费的是 dist，因此 **dist 是"用户实际看到的预览行为"**，判定为 as-is 权威源。
golden oracle（`build/_oracle` 中 require('wintersky')）走的也是 dist，天然一致。

dist 与 src 的全量逐函数对比（dist 经 js-beautify 美化后逐节核对）确认的分歧共 3 处，
移植已按 dist 实现：

1. `parseColor` 数组分支：dist **直接返回** `tinycolor({r,g,b,a}).toHex8String()`；
   src 会继续走 `'#'+substr(3,6)+substr(1,2)` 重排（影响 gradient 数组色）。
2. 发射器寿命到期方法：dist 名为 `end()`、dispatch `"end"` 事件；src 为
   `expire()`/`"expire"`。
3. shader 字符串：dist 经 glslify 处理，带 `#define GLSLIFY 1\n` 前缀且为单行
   （行为等价，按 dist 原文收录于 `Shaders.java`）。

### 7. 根包 oracle 暴露的下层移植缺陷（已修复并回归）

逐 tick golden 比对暴露出 wu3/wu4 层的三处偏差，均已修复且有冻结用例护住：

1. `JsSemantics.truthy` 未处理 `Boolean.FALSE`（JS `!!false===false`，原实现落到
   默认 true）——导致 `particle_collision_toggle=false` 被当作 true，无碰撞组件的
   粒子被错误地做地面碰撞（mobflame 用例暴露）。
2. `Config.setFromJSON` 的 `comp()` 辅助只认对象值，标量/数组组件
   （`particle_initial_speed`、`particle_kill_plane`、`particle_expire_if_*_blocks`）
   被静默跳过——导致初速度 molang 未求值且随机序列错位（lava_particle 用例暴露）。
3. `TinyColor` 对象输入缺少 tinycolor2 的 isValidCSSUnit 全有或全无门槛：r/g/b
   任一 NaN/Infinity 时 tinycolor2 整体回退黑色，原实现逐通道钳制
   （basic_crit 的 gradient 数组含 molang 字符串元素时暴露）。

另记录两处与 src 阅读易混淆的实现要点（非缺陷）：JS `Array.forEach` 在粒子
`expire→remove` 时会跳过后续一个粒子（`Emitter.tick` 的粒子循环按索引+活列表长度
复刻）；`new Particle()` 构造函数内已 `add()`，`spawnParticles` 会再调一次
`add()`（随机序列消耗两次，as-is 保留）。

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
  4. as-is 权威源为 npm dist（决策 6），与 git master src 的 3 处分歧按 dist 实现。
  5. `setInterval`/`clearInterval`（playLoop 系列）以守护线程
     `ScheduledExecutorService` 承载（`IntervalTimer`）。
  6. `new Image()` 浏览器接缝为 `TextureImage`（src 赋值同步触发 onload）。
  7. JS `substr`/truthiness/`ToNumber` 等语义经 `JsSemantics` 公开方法复用
     （原包私有，为 Config 复用而 widen 4 个方法）。

## 后续工作（本 ADR 不覆盖）

- 融入设计：渲染后端（three 网格/材质表面 → MC 渲染管线/bridge）、纹理获取钩子
  （`fetchTexture`/`fetchParticleFile` → MC 资源管理器）、molang 引擎是否切换
  `eyelib.molang`、与既有 `eyelib.particle` 运行时的关系（oracle/替换/并存）。
- 输出物：`docs/concepts/` 或新 ADR，迁移验证完成后进行。

## 验证

已回填（2026-09-29）：

- `:1.20.1:compileJava` 通过。
- `:1.20.1:test` wintersky 四个 oracle 测试全绿（WinterskyOracleTest 14 用例 ×
  45 tick 与 Node dist golden 逐值比对；MolangOracleTest/ThreeOracleTest/
  TinyColorOracleTest 无回归）。
- 根包 golden：`scripts/wintersky-oracle/wintersky_cases.mjs` 生成
  `src/test/resources/wintersky/wintersky_cases.json`（约 726KB，冻结），
  夹具 11 个 Mojang bedrock-samples vanilla 粒子 + 1 个手工分支补全
  （`fixtures/zz_branches.json`，覆盖 catmull_rom/bezier_chain/creation/
  expiration/sequence/randomize/emitter_bound）。
