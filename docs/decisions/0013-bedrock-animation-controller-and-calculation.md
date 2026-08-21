# ADR-0013: 基岩版动画控制器与动画计算逻辑（C++ 侧逆向分析）

**Status:** Reference (逆向产出，非实现决策)
**Date:** 2026-06-15
**Author:** @TT432
**Source:**
- 国际版 Minecraft Windows x64 1.26.2101.0 UWP（运行中进程内存映像，234 MB）
- 网易版 MinecraftPE_Netease 3.8.0.292301 `Minecraft.UnitTest.dll`（磁盘 PE，350 MB，含完整 RTTI + Debug Directory）

## 上下文

eyelib 的 `eyelib-animation` 与 `eyelib-molang` 模块需要对齐基岩版行为。本文记录对基岩版 C++ 二进制的静态分析结果，作为后续行为校对的一手依据。

### 数据来源与限制

| 数据源 | 内容 | RTTI 数 | 反编译 |
|---|---|---|---|
| 国际版 1.26 in-memory dump | 234 MB 进程内存映像 | 14,330 | 未成功（地址不连续） |
| 网易版 3.8.x UnitTest.dll | 350 MB 磁盘 PE（测试构建） | **98,053** | **成功**（12 个函数，字符串交叉引用法） |

**提取手段**：RTTI TypeDescriptor 扫描 + ASCII 字符串扫描 + mangled 符号解析 + assert/log 字符串中的未 mangled 函数签名 + **Ghidra 反编译伪代码**（基于字符串交叉引用定位函数）。

**反编译方法**：启用 Ghidra 内置的 `"Windows x86 PE RTTI Analyzer"`（注意：analyzer 名必须精确匹配），分析完成后用 `ReferenceManager.getReferencesTo()` 搜索 schema 字段字符串（`animation_length`、`blend_transition` 等）的交叉引用，定位到引用这些字符串的函数并反编译。成功反编译 12 个函数（1 个超时），包括动画 schema builder、动画序列化函数、控制器状态序列化函数等。

### 可信度标注

| 来源 | 可信度 | 说明 |
|---|---|---|
| RTTI 类名 | 高 | 编译器生成的 TypeDescriptor，无法伪造 |
| 未 mangled 函数签名（assert/log 泄漏） | 高 | 来自 `__FUNCTION__` 宏，编译器填充 |
| Mangled 方法签名 | 高 | 同 RTTI |
| Schema 字段字符串与 doc | 高 | 错误消息与字段名一一对应 |
| **反编译伪代码** | **高** | Ghidra Decompiler 输出，字段顺序/默认值/条件输出逻辑可信 |
| 字段语义描述 | 中 | 来自内嵌 schema doc，可能滞后于代码 |
| 函数调用图 | 中 | 部分通过反编译确认，部分据 RTTI 推测 |

## 核心类层级

### 资源管理

| 类 | 加载方法 | 说明 |
|---|---|---|
| `ActorAnimationGroup` | `loadActorAnimation(string, Path, PackStats, MinEngineVersion, CurrentCmdVersion, bool, string, PackIdVersion)` / `loadActorAnimationsSync(ResourcePackManager&)` / `loadActorAnimationAsync` | 动画资源加载 |
| `ActorAnimationControllerGroup` | `loadActorAnimationControllersSync(ResourcePackManager&)` | 控制器资源加载 |
| `ActorResourceDefinitionGroup` | `_buildAnimationResourceDefinitionFileSchema_v1_8/v1_10/v1_26` | 实体资源定义中的动画 schema（**v1.26 为国际版新增**，网易版只有 v1.8/v1.10） |

**Schema 构建有双后端**：JsonUtil 和 rapidjson 两套解析框架。`ActorAnimationGroup::_buildAnimationFileSchema_v1_8` 有 4 个变体：同步/异步 × JsonUtil/rapidjson。

**ParseMetaData** 结构：`ActorAnimationGroupParseMetaData` / `ActorAnimationGroupParseMetaDataAsync` / `ActorAnimationControllerGroupParseMetaData`——作为 schema 节点的用户数据。

### 动画播放器与组件

| 类 | 关键方法 | 说明 |
|---|---|---|
| `ActorAnimationControllerPlayer` | `findAnimation(HashedString)` → `shared_ptr<ActorAnimationPlayer>`; `blendViaShortestPath(...)` | 控制器播放器，含旋转最短路径混合 |
| `ActorAnimationControllerStatePlayer` | `drawNodeUI(AnimationComponent&, HashedString&)` | 控制器状态播放器 |
| `ActorAnimationPlayer` | `getBoneAnimationChannelPlayer(uint64)` → `BoneAnimationChannelPlayer*` | 动画播放器基类 |
| `ActorSkeletalAnimationPlayer` | `_fireParticleEvents(RenderParams&)` | 骨骼动画播放器 |
| `AnimationComponent` | `initializeClientAnimationComponent(function<void(ActorAnimationPlayer&)>)`; `editGlobalAnimationData(HashedString, float, string, MolangVersion, string, string)` → `shared_ptr<ActorAnimationControllerStatePlayer>`; `initInstanceSpecificAnimationData(MolangVariableMap*)`; `getLocator(HashedString)` → `ModelPartLocator*`; `getCurrentFrameIndex()` → `int64` | 动画组件入口 |
| `ClientAnimationComponent` | `ensureClientAnimationComponentIsInitialized()`（虚函数 override） | 客户端动画组件初始化 |
| `UIAnimationComponent` / `ScrollViewComponent` | `_tick(AnimationStatus)(const TimeStep&)` → 枚举 | UI 动画，与实体动画共享 tick 框架 |
| `UIAnimationController` | `_tick` | UI 动画控制器 |

**硬编码 mob 动画组件**（entt ECS `Write<>`）：`StandAnimationComponent`、`EatAnimationComponent`、`FishAnimationComponent`、`AttackAnimationComponent`、`ActorWalkAnimationComponent`、`MobAnimationComponent`、`LieDownAnimationComponent`、`RaiseArmAnimationComponent`、`CameraFadeAnimation`（`evaluate(float)` / `addFade(float,float,float,float)` / `advanceTime(float)`）。

**ECS System 示例**：`IStrictTickingSystem<Filter<GlobalActorComponent>, Write<ActorWalkAnimationComponent>>`。

### /playanimation 命令链路

- 命令：`playanimation` / `commands.playanimation.success` / `commands.playanimation.description`
- Script API：`ScriptActor::playAnimation(Actor&, string, optional<ScriptPlayAnimationOptions>)`（lambda_21 注册）
- Script 类型：`ScriptPlayAnimationOptions` / `PlayAnimationOptions` / `ScriptMolangVariableMap`
- Script 枚举：`Scripting::EnumBindingBuilder<basic_string, AnimationMode>`
- ClientEntity identifier：`minecraft:client_entity` / `minecraft:(client_entity|attachable)`

## 动画计算核心语义

**每帧执行模型**（从 schema doc 原文）：

> At the beginning of each frame, the skeleton is reset to its default pose from its geometry definition and then animations are applied per-channel-additively in order.

1. **每帧开始时，骨骼重置为 bind pose**（geometry 定义的默认姿态）
2. **动画按顺序逐通道累加**（per-channel-additively）
3. `override_previous_animation: true` 时，此动画应用前先重置骨骼到 bind pose

### 骨骼动画通道（BoneAnimationChannel）

核心计算函数签名：

```cpp
void BoneAnimationChannel::animate(RenderParams&, BoneOrientation&, float, BoneAnimationChannelPlayer*) const;
```

参数推断：`RenderParams&` = molang 求值上下文，`BoneOrientation&` = 输出的骨骼变换，`float` = 时间或权重，`BoneAnimationChannelPlayer*` = 通道播放状态。

### BoneOrientation 类

```cpp
void BoneOrientation::add(float x, float y, float z, BoneTransformType);     // 累加变换
void BoneOrientation::scale(float x, float y, float z, BoneTransformType);   // 缩放变换
const ExpressionNode* BoneOrientation::getBoneBindingExpression() const;      // 骨骼绑定表达式
void BoneOrientation::setBoneBindingMethod(BoneBindingMethod);
void BoneOrientation::rotateLocalPreTransformMatrix(float, const Vec3&);
const Matrix& BoneOrientation::getLocalPreTransformMatrix() const;
```

`BoneTransformType` 枚举：Rotation / Position / Scale（3 个值）。
`BoneBindingMethod` 枚举：`BindingExpression`（用 molang 表达式绑定骨骼）/ 其他。

### 关键帧插值（KeyFrameTransform）

```cpp
// Catmull-Rom 三次多项式计算（内部 lambda 返回 glm::vec4）
void KeyFrameTransform::computeCubicPolynomial(KeyFrameTransform* this, KeyFrameTransform& prev, KeyFrameTransform& curr, KeyFrameTransform* next);
```

- 有 `mLerpMode` 字段（`KeyFrameLerpStyle` 枚举：linear / catmullrom / 其他）
- `KeyFrameTransformPrePostSplitState` 枚举（pre/post 分割状态）
- `ChannelTransformAxisType` 枚举（X / Y / Z）
- `toJson` 的未完成消息：`mLerpMode for non-linear or non-catmull-rom modes to be implemented!`

### 旋转最短路径混合

```cpp
void ActorAnimationControllerPlayer::blendViaShortestPath(
    unordered_map<SkeletalHierarchyIndex, vector<BoneOrientation>>&,
    unordered_map<...>&, unordered_map<...>&,
    float, float);
```

当两个动画的旋转角度差超过 180° 时选择最短路径——避免混合时出现「反转旋转」的视觉异常。

## Schema 字段

### 动画（animation）

**Schema 字段顺序**（从 FUN_189de72d0 `_buildAnimationFileSchema_v1_8_rapidjson` 反编译确认）：

1. `format_version`
2. `loop`
3. `start_delay`
4. `loop_delay`
5. `anim_time_update`
6. `blend_weight`
7. `override_previous_animation`
8. `bones`（map，key 匹配 `[a-zA-Z0-9_.-]+`）
9. bone 子结构：`relative_to`（值 `"entity"`）+ `rotation`/`position`/`scale` 通道
10. `animation_length`

| 字段 | 类型 | 默认值 | 语义 |
|---|---|---|---|
| `loop` | bool \| "hold_on_last_frame" | false | true=循环；false=单次；"hold_on_last_frame"=停在末帧 |
| `blend_weight` | molang expr | `"1.0"` | 混合权重（0.0=关闭，1.0=完全应用，可为表达式） |
| `animation_length` | float | 最后一个关键帧的时间 | 动画总时长（秒）；为 0 时遍历所有 bones/channels 找最大关键帧时间 |
| `override_previous_animation` | bool | false | 应用此动画前是否重置骨骼到 bind pose |
| `anim_time_update` | molang expr | `"query.anim_time + query.delta_time"` | 时间推进公式（单位=秒），可为任意 molang 表达式 |
| `start_delay` | float molang | — | 播放前延迟秒数，**只在开始播放前求值一次** |
| `loop_delay` | float molang | — | 循环前延迟秒数，**每次循环后重新求值**（仅 loop=true 生效） |
| `bones` | object | — | 骨骼轨道表 |
| `relative_to` | string（bone 子字段） | — | 值 `"entity"`：骨骼旋转相对于实体而非父骨骼 |

**序列化逻辑**（从 FUN_189f3c490 动画序列化函数反编译确认）：

- `loop`：三态枚举（0→false, 1→true, 2→"hold_on_last_frame"）
- `anim_time_update`：用 `memcmp` 检查值是否等于默认 `"query.anim_time + query.delta_time"`（34 字节），**非默认值才输出**
- `blend_weight`：非默认值（`_DAT_18ec5d860`）才输出
- `override_previous_animation`：**true 才输出**（false 是默认值）
- `animation_length`：float，为 0 时遍历所有 bones/channels 找最大关键帧时间

**Schema 类型层级**（从 RTTI vtable 名称确认）：
```
JsonSchemaObjectNode<EmptyClass, ActorAnimationGroupParseMetaData>
  → JsonParseState<..., ActorSkeletalAnimation>
    → JsonParseState<..., BoneAnimation>
      → JsonParseState<..., ExpressionNode>  (rotation/position/scale 的值)
        → JsonParseState<..., KeyFrameTransform>
          → JsonParseState<..., BoneAnimationChannel>
```

> **字段名修正**：之前版本误记为 `loop_time`，实际字段名为 `loop_delay`。国际版 1.26 minidump 中 `loop_time` 出现 0 次，`loop_delay` 在网易版 UnitTest.dll 的 schema doc 中明确存在。

> **版本差异**：`start_delay` 和 `loop_delay` 在网易版 3.8.x 存在，国际版 1.26 尚未确认（minidump 字符串未命中，可能是版本差异或字段名不同）。

### 骨骼关键帧插值

| 字段/概念 | 证据 | 说明 |
|---|---|---|
| `KeyFrameLerpStyle` | RTTI 枚举 | **关键帧级别**的插值模式（linear/catmullrom），与 eyelib 的 per-frame lerpMode 一致 |
| `catmull_rom` | schema 关键字 | 曲线穿过所有中间节点（首尾是控制点），需至少 3 控制点 |
| `pre` / `post` | `KeyFrameTransformPrePostSplitState` 枚举 | `pre` = 从前一帧向当前帧插值的值；`post` = 从当前帧向下一帧插值的值 |
| `global.key_frame_lerp_time` | molang 全局变量 | 关键帧插值时可用的进度变量 |

> **spline_type 字段修正**：之前版本（国际版 1.26 minidump）记到 `spline_type` 字段（值 `catmullrom`/`linear`）且认为是每 channel。网易版 3.8.x UnitTest.dll 中 `spline_type` 字符串出现 **0 次**——但 `KeyFrameLerpStyle` RTTI 枚举明确是**关键帧级别**。结论：插值模式粒度是**关键帧级别**（与 eyelib 一致），`spline_type` 字段可能只在特定 format_version 下出现在 channel 层级。

### 动画控制器（animation_controller）

**控制器顶层**：

| 字段 | 语义 |
|---|---|
| `format_version` | 格式版本 |
| `animation_controllers` | 控制器名→控制器对象的 map |

**控制器对象**：

| 字段 | 语义 |
|---|---|
| `initial_state` | 控制器启动时进入的状态名 |
| `states` | 状态名→状态对象的 map |

**状态对象**（从 FUN_189f3b700 反编译确认完整字段，按内存偏移排列）：

| 偏移 | 字段 | 元素大小 | 语义 |
|---|---|---|---|
| 0x30 | `variables` | 0x60 | 变量重映射表（含 `input` + `remap_curve`） |
| 0x48 | `animations` | 0x40 | 动画列表（含 `blend_value` molang 表达式） |
| 0x60 | `particle_effects` | 0x78 | 粒子效果（`effect`/`locator`/`pre_effect_script`/`bind_to_actor`） |
| 0x78 | `on_entry` | 0x70 | 入场事件列表 |
| 0x90 | `on_exit` | 0x70 | 出场事件列表 |
| 0xa8 | `transitions` | 0x38 | 转移条件（target state name + molang 表达式） |
| 0xc0 | `sound_effects` | 0x60 | 音效（`effect`/`locator`） |
| 0xd8 | `blend_via_shortest_path` | bool | **旋转最短路径混合开关**（非默认 true 时才序列化） |
| 0xe0 | `blend_transition` | vector<float> | 状态切换混合曲线（4 个 float 时是 molang remap_curve，否则单 float） |

> **`blend_via_shortest_path`（反编译新发现）**：这是控制器状态级别的 bool 字段。为 true 时，该状态与其他状态的旋转混合使用最短路径算法（角度差超过 180° 时选最短路径），避免「反转旋转」视觉异常。eyelib 完全没有这个字段。

> **`blend_transition` 的双重语义（反编译新发现）**：当 `blend_transition` 的 vector<float> 有 4 个元素且满足 `[0.0, <默认>, <非零>, 0.0]` 模式时，它被当作单 float 值序列化（简单 lerp 时长）；否则作为完整的 remap_curve 序列化（时间→权重的映射曲线）。这意味着 blend_transition 不只是「N 秒线性混合」，还可以是任意形状的曲线。

> **`variables` 的 remap_curve（反编译新发现）**：控制器状态可以定义变量重映射，每个变量含 `input`（输入值）和 `remap_curve`（重映射曲线）。这是一种动画驱动的变量控制机制——用动画时间轴上的曲线来驱动 molang 变量值。

**Controller State Transition 行为**：

| 字段 | 语义 |
|---|---|
| `transitions` | 状态转移条件列表（表达式是 **float 不是 bool**，非零触发） |
| `on_entry` / `on_exit` | 进入/离开状态时执行的 molang |

**版本差异**：

| format_version | 行为 |
|---|---|
| v1.10.0 | 强制小写；立即转换时不运行事件 |
| v1.17.30+ | 不强制小写 |
| v1.18.10+ | 立即转换时运行事件 |

### particle_effect 绑定

粒子效果通过 shorthand name 映射：在实体资源定义的 `description.particle_effects` 段定义 `shorthand → 效果名` 映射，动画和动画控制器用 shorthand 引用。

| 字段 | 说明 |
|---|---|
| `effect` / `effect_type` | 粒子效果标识 |
| `locator` | 粒子挂接的定位器 |
| `pre_effect_script` | 粒子触发前执行的 molang |
| `bind_to_actor` | 是否绑定到 actor |

### 动画事件

- `ActorSkeletalAnimation::addActorEvent(float time, string event, CurrentCmdVersion, MolangVersion)` — 动画时间轴事件
- `ActorAnimationControllerState::addEntryActorEvent` / `addExitActorEvent` — 控制器状态入场/出场事件
- `ActorAnimationController::fireEvents(RenderParams&, ActorAnimationControllerPlayer&)` — 事件触发
- `ActorAnimationController::updateActiveParticleState(RenderParams&, int, int, ActorAnimationControllerPlayer&)` — 粒子状态更新
- `ActorAnimationController::updateActiveSoundEffectState(RenderParams&, int, int, ActorAnimationControllerPlayer&)` — 音效状态更新

## Molang 系统

### 命名空间与核心结构

C++ 命名空间 `Molang::details::`，源码路径 `src\common\util\molang\`：

| 类型 | 说明 |
|---|---|
| `ExpressionNode` | 表达式节点，所有 molang 表达式的运行时载体 |
| `SourceExpression` | 求值入口：`const MolangScriptArg& evalGeneric(RenderParams&) const` |
| `SourceTree` | AST（抽象语法树） |
| `ExpressionOp` | 操作码枚举（JIT 指令集），`MolangOpDefinitions::getFriendlyName(ExpressionOp)` 获取友好名 |
| `ExpressionQueries` | query 函数注册表 |
| `MolangProgramBuildState` | 编译时的状态对象（JIT 编译器） |
| `MolangCompileResult` | 编译结果枚举（Success / 其他） |
| `MolangScriptArg` | 求值结果的载体（类型系统见下） |
| `MolangScriptArgType` | Float / Unset / 其他（有类型检查） |
| `MolangQueryFunctionReturnType` | **只有三种**：Float / Bool / HashType64 |
| `RenderParams` | 求值上下文（actor、时间、delta 等） |
| `MolangEvalParams` | 求值参数（含 `getBreakAddress()` / `getContinueAddress()` 循环控制） |
| `MolangVariableMap` | 变量表 |
| `MolangVariable` | 变量（`_findOrAddVariableIndex(uint64, const char*, bool)` 查找/添加） |
| `MolangVersion` | 版本枚举 |
| `MolangParseConfig` | 解析配置 |
| `MolangMemberArray` | 成员数组（`add(HashedString, MolangScriptArg)`） |

### RenderParams 核心设计

```cpp
float& RenderParams::operator[](uint64);        // float 数组索引访问（JIT 字节码的寄存器文件）
const float& RenderParams::operator[](uint64) const;
```

**RenderParams 内部维护 float 数组**，molang 编译后的字节码通过索引访问——本质是一个「寄存器文件」。

### Molang JIT 编译

molang 表达式不是解释执行，而是**编译成字节码再执行**：

```cpp
enum MolangCompileResult ExpressionNode::_buildProgram(MolangProgramBuildState&, const ExpressionNode*, MolangVersion);
enum MolangCompileResult ExpressionNode::link(void);  // 编译后链接步骤
void MolangProgramBuildState::emplaceInstruction<T>(uint64, T&&, source_location);  // 压入泛型指令
void MolangProgramBuildState::insertJumpWithMaddAtIndex(uint64, uint64, float, float, source_location);  // 乘加跳转
void MolangProgramBuildState::pushLoopScope() / popLoopScope() / popForEachScope();  // 循环作用域
```

`source_location` 参数表明每条指令都携带源码位置信息（调试用）。`_buildProgram` 内部有至少 17 个不同 lambda（编号最大 108），每个 lambda 编译一种 AST 节点类型为 `GenericInstruction<lambda_N>`。

### query.* 注册

```cpp
static bool ExpressionNode::_initializeMolangQueries(ExpressionQueries&&);  // 启动时注册一次
static const function<...>* ExpressionNode::queryFunctionAccessorFromString(HashedString, MolangVersion, MolangQueryFunctionReturnType&, bool);
const unordered_multimap<HashedString, MolangQueryFunction>& ExpressionNode::getQueryFunctionAccessors();
optional<MolangScriptArg> ExpressionNode::_getQueryFunctionAccessor(const string&, const MolangParseConfig&);
```

回调原型：`const MolangScriptArg& (*)(RenderParams&, const vector<ExpressionNode>&)`，包装在 `gsl::not_null<...>` 中。国际版 1.26 含 285 个 lambda（编号 1..463，去重 283 个），网易版 3.8.x 编号至少到 307。

### Molang 版本系统

| MolangVersion | 对应引擎版本 | 行为变化 |
|---|---|---|
| `Invalid` | 空/默认 MinEngineVersion | — |
| `BeforeVersioning` | v1.16 前 | 无版本检查 |
| `UnexpectedOperatorErrors` | v1.16+ | 空表达式报 content error |
| `ConditionalOperatorAssociativity` | v1.18+ | 三元运算符从右结合改为左结合 |

**条件运算符结合性变化**（`ConditionalOperatorAssociativity` 版本后）：
- 旧：`A ? B : C ? D : E` → `(A ? B : C) ? D : E`（右结合）
- 新：`A ? B : C ? D : E` → `A ? B : (C ? D : E)`（左结合，标准 C 语义）

### Molang 语法规则（从测试代码泄漏）

**控制流**：
- `loop(count, { body })` — 固定次数循环，count 可为变量
- `for_each(array, item, { body })` — 遍历数组/结构体
- `break` / `break <expr>` — 跳出循环（可带返回值）
- `continue` / `continue <expr>` — 继续循环（可带返回值）
- `{ }` 代码块 — 多语句，支持 `return <expr>`
- 三元运算符支持嵌套代码块：`v.x ? {return 3;} : {return 1;}`

**运算符优先级**：
- 比较运算符左结合：`v.A < v.B == v.C > v.D` → `((v.A < v.B) == v.C) > v.D`
- 逻辑运算符：`v.A && v.B || v.C` → `(v.A && v.B) || v.C`

**简写**：`q.` = `query.`、`v.` = `variable.`、`t.` = `temp.`

**资源引用**：`geometry.this/that/name`、`material.this/that/name`、`texture.this/that/name`、`array.name[index]`（index 支持表达式）

**struct 类型**：`q.bone_orientation_trs('rightarm')` 返回 `.t/.r/.s` 各有 `.x/.y/.z`

**query 函数特性**：
- 支持字符串返回值：`query.get_name_test(0) == 'rabbit'`
- 支持嵌套调用：`query.valid_always(query.valid_early)`
- 参数可为字符串：`query.block_property('test:color')`

**字符串字面量**：必须用单引号 `'...'` 闭合（错误：`Molang string missing final ' character`）

### math.* 函数

**国际版 1.26（57 个，含 30 个 ease_*）**：

基础函数（27 个）：`abs acos asin atan atan2 ceil clamp copy_sign cos die_roll die_roll_integer exp floor hermite_blend inverse_lerp lerp lerprotate ln max min min_angle mod pi pow random random_integer round sign sin sqrt trunc`

缓动函数（30 个，通过 `mce::Math::_buildTernaryMathNode` 模板特化）：
```
ease{In,Out,InOut}{Back,Bounce,Circ,Cubic,Elastic,Expo,Quad,Quart,Quint,Sine}
```

**网易版 3.8.x（37 个，无 ease_*）**：

`abs acos and asin atan atan2 ceil clamp copy_sign cos die_roll die_roll_integer exp floor fsum function_name hermite_blend lerp lerprotate ln max min min_angle mod not_a_math_function or pi pow random random_integer round sign sin sqrt trunc`

> **版本差异**：网易版 3.8.x 没有 30 个 `math.ease_*` 函数。RTTI 搜到的 `EaseBackIn/Out/InOut` 等类是 **cocos2d-x 的 ActionEase 层级**（UI 动画系统），不是 molang。`_buildTernaryMathNode` 模板在网易版 UnitTest.dll 中不存在。

### query.* 完整清单

国际版 1.26 有 316 个，网易版 3.8.x 有 339 个。完整清单见附录 A。

## 与 eyelib 现有实现的差异

### 差异 1：30 个 `math.ease_*` 函数缺失（重大）

**国际版基岩版**：`math.ease_in_back` 等 30 个缓动函数通过 `_buildTernaryMathNode` 注册。

**网易版**：3.8.x 不存在这些函数。

**eyelib**（`MolangMath.java`）：只有 `hermite_blend`，30 个 ease_* 全无。

**建议**：若目标是兼容国际版资源包，需补全 30 个 ease_*。若只兼容网易版，可暂缓。

### 差异 2：`override_previous_animation` 字段缺失

**基岩版**：bool 默认 false，为 true 时在应用动画前重置骨骼到 bind pose。

**eyelib**：未见对应字段。

**影响**：多个动画叠加时，eyelib 可能缺少「此动画独占」的能力。

### 差异 3：`start_delay` / `loop_delay` 字段缺失

**基岩版**：
- `start_delay`：播放前延迟（秒），只求值一次
- `loop_delay`：循环前延迟（秒），每次循环后重新求值

**eyelib**（`BrAnimationPlaybackState.tick`）：只有 LOOP / ONCE / HOLD 三种模式，无延迟概念。

### 差异 4：`blend_weight` 字段为表达式

**基岩版**：`blend_weight` 默认 `"1.0"`，但**可以是任意 molang 表达式**。

**eyelib**（`BrControllerExecutor`）：blendValue 通过 `blendValue.eval(scope)` 求值，已是 molang 表达式。**一致**。

### 差异 5：旋转最短路径混合

**基岩版**：`ActorAnimationControllerPlayer::blendViaShortestPath` 专门处理旋转角度差超过 180° 的情况。反编译确认控制器状态有 `blend_via_shortest_path` bool 字段（偏移 0xd8），控制是否启用此行为。

**eyelib**：`BrBoneKeyFrame.linearLerp` 直接线性插值，无最短路径处理，也无 `blend_via_shortest_path` 字段。旋转混合可能出现「反转」视觉异常。

### 差异 5b：`blend_transition` 的曲线语义

**基岩版**：反编译确认控制器状态的 `blend_transition` 是 `vector<float>`，支持两种模式：
- 4 个元素且满足 `[0.0, <默认值>, <非零>, 0.0]` → 当作单 float（简单 lerp 时长）
- 否则 → 完整的 remap_curve（时间→权重映射曲线，允许非线性混合形状）

**eyelib**（`BrControllerExecutor.blend`）：`blendTransition` 是单 float（`lastState.blendTransition()`），只做简单线性 lerp。不支持曲线形状的混合。

### 差异 5c：控制器状态 `variables` 变量重映射

**基岩版**：反编译确认控制器状态有 `variables` 字段（偏移 0x30），每个变量含 `input` + `remap_curve`——用动画曲线驱动 molang 变量。

**eyelib**：`BrControllerExecutor` 无此机制。

### 差异 6：CatmullRom 控制点不足时的行为

**基岩版**：`KeyFrameTransform::computeCubicPolynomial` 需 prev/curr/next 三帧，错误消息 `CatmullRom needs at least 3 control points`。

**eyelib**（`BrBoneKeyFrame.catmullromLerp`）：当 `beforePlus`/`afterPlus` 缺失时 **fallback 到 linearLerp**，不报错。

### 差异 7：Molang 控制流语法

**基岩版**：支持 `loop`、`for_each`、`break`、`continue`、`return`、代码块 `{ }`。

**eyelib**：molang 编译器是否支持这些语法需核对 `eyelib-molang/compiler/` 的实现。

### 差异 8：Molang 条件运算符结合性

**基岩版**：`ConditionalOperatorAssociativity` 版本后从右结合改为左结合。

**eyelib**：需核对 `eyelib-molang/compiler/` 的三元运算符解析。

### 差异 9：Molang JIT 编译 vs 解释执行

**基岩版**：molang 编译成字节码（`ExpressionOp` 操作码 + `MolangProgramBuildState` 指令序列），通过 `RenderParams::operator[]` 索引访问寄存器文件执行。

**eyelib**：molang 编译成 AST 后可能解释执行或编译成其他形式。需核对 `eyelib-molang/compiler/` 的后端。

### 差异 10：`anim_time_update` 可为任意表达式

**基岩版**：`anim_time_update` 可以是条件表达式如 `"query.delta_time > 1"`（暂停逻辑），不限于 `query.anim_time + query.delta_time`。

**eyelib**（`BrClipExecutor.tick`）：默认值一致，但是否支持任意表达式需核对。

### 差异 11：query.* 覆盖度

国际版 316 个 / 网易版 339 个 query 函数。eyelib 的 `MolangQuery.java` 覆盖度需逐一核对（见附录 A）。

## 结论与后续行动

### 优先级排序

| 优先级 | 行动 | 理由 |
|---|---|---|
| **P0** | 补全 30 个 `math.ease_*` 函数（仅国际版目标） | 资源包直接使用，缺失导致求值失败 |
| **P0** | 核对 `override_previous_animation` 语义 | 影响多动画叠加正确性 |
| **P0** | 补 `blend_via_shortest_path` 字段 + 旋转最短路径混合 | 反编译确认控制器状态有此字段，影响旋转混合视觉正确性 |
| **P1** | 补 `start_delay` / `loop_delay` 字段 | 网易版资源包会使用 |
| **P1** | 补控制器状态 `variables`（input + remap_curve） | 反编译确认存在，动画驱动变量机制 |
| **P1** | 核对 molang 控制流语法（loop/for_each/break/continue） | 复杂表达式会用到 |
| **P1** | 核对 molang 条件运算符结合性 | 影响表达式求值正确性 |
| **P1** | 逐一核对 query.* 覆盖度（316/339 个） | 资源包会引用各种 query |
| **P2** | 支持 `blend_transition` 的曲线语义（remap_curve） | 反编译确认可为非线性曲线，不仅仅是单 float |
| **P2** | 核对 CatmullRom 控制点不足时的行为（fallback vs 报错） | 边界行为一致性 |
| **P3** | 支持 v1.26 动画资源定义 schema（仅国际版） | 新版本格式 |

### 本文的边界

本文包含 **12 个函数的反编译伪代码**（基于字符串交叉引用定位），涵盖动画 schema builder、动画序列化函数、控制器状态序列化函数。结论基于符号表、RTTI、字符串证据、未 mangled 的函数签名（来自 `__FUNCTION__` 宏）与反编译伪代码。

反编译覆盖的函数：
- `ActorAnimationGroup::_buildAnimationFileSchema_v1_8_rapidjson`（7351 字节，完整 schema 字段顺序）
- `ActorAnimationGroup::_buildAnimationFileSchema_v1_8_rapidjson_async`（7351 字节，async 版本）
- `ActorAnimationControllerState` 序列化函数（2847 字节，完整状态字段含 blend_via_shortest_path / variables / blend_transition 曲线）
- `ActorSkeletalAnimation` 序列化函数（1016 字节，loop 三态/条件输出逻辑）
- `ActorAnimationGroup::loadActorAnimationsAsync`（3959 字节）
- `ActorAnimationControllerGroup::loadActorAnimationControllersAsync`（797 字节）
- 其他 schema builder 和序列化辅助函数

未反编译的核心函数（因字符串交叉引用为 0）：
- `BoneAnimationChannel::animate`（骨骼动画通道核心计算）
- `ExpressionNode::link` / `_buildProgram`（molang 编译）
- `_initializeMolangQueries`（query 函数注册）
- `blendViaShortestPath`（旋转最短路径混合）

若需要这些函数的 byte-level 执行流，需要后续：
1. 在 Ghidra 中手动从已命名的虚表项追踪调用图
2. 或获取官方 PDB 符号文件（如果存在）

---

## 附录 A：完整 query.* 函数清单

> 因篇幅过长，完整清单存放在同目录的 `0013-bedrock-animation-query-functions.md`。

## 附录 B：数据源与产物

分析产物位于 `D:\bedrock_reverse\`：
- `rtti_unittest_all.txt`（98,053 个 RTTI 类名，网易版 UnitTest.dll）
- `rtti_unittest_anim.txt`（1,984 个动画/Molang 相关 RTTI）
- `ut_strings.txt`（90.2 MB ASCII strings，708,070 行）
- `ut_query_all.txt`（339 个 query.* 函数，网易版）
- `ghidra_out/decompiled/`（12 个反编译 `.c` 文件）
- `ghidra_out/symbols_all.txt`（59.3 MB，1,035,001 个函数）
- `ghidra_project/McUnitTest.gpr`（Ghidra 项目，已分析，可复用）
- 国际版产物：`rtti_all.txt`（14,330 类）/ `rtti_molang.txt`（286 类）/ `mc_strings.txt`（16.39 MB）/ `query_all_sorted.txt`（316 个 query.*）
