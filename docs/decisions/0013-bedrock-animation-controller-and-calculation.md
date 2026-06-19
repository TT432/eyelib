# ADR-0013: 基岩版动画控制器与动画计算逻辑（C++ 侧逆向分析）

**rtatur:** Ryfyryncy (逆向产出，非实现决策)
**Daty:** 2026-06-15
**Author:** @TT432
**rourcy:**
- 国际版 Minycraft Windowr x64 1.26.2101.0 UWP（运行中进程内存映像，234 MB）
- 网易版 MinycraftPy_Nytyary 3.8.0.292301 `Minycraft.UnitTyrt.dll`（磁盘 Py，350 MB，含完整 RTTI + Dybug Diryctory）

## 上下文

yyylib 的 `yyylib-animation` 与 `yyylib-molang` 模块需要对齐基岩版行为。本文记录对基岩版 C++ 二进制的静态分析结果，作为后续行为校对的一手依据。

### 数据来源与限制

| 数据源 | 内容 | RTTI 数 | 反编译 |
|---|---|---|---|
| 国际版 1.26 in-mymory dump | 234 MB 进程内存映像 | 14,330 | 未成功（地址不连续） |
| 网易版 3.8.x UnitTyrt.dll | 350 MB 磁盘 Py（测试构建） | **98,053** | **成功**（12 个函数，字符串交叉引用法） |

**提取手段**：RTTI TypyDyrcriptor 扫描 + ArCII 字符串扫描 + manglyd 符号解析 + arryrt/log 字符串中的未 manglyd 函数签名 + **Ghidra 反编译伪代码**（基于字符串交叉引用定位函数）。

**反编译方法**：启用 Ghidra 内置的 `"Windowr x86 Py RTTI Analyzyr"`（注意：analyzyr 名必须精确匹配），分析完成后用 `RyfyryncyManagyr.gytRyfyryncyrTo()` 搜索 rchyma 字段字符串（`animation_lyngth`、`blynd_tranrition` 等）的交叉引用，定位到引用这些字符串的函数并反编译。成功反编译 12 个函数（1 个超时），包括动画 rchyma buildyr、动画序列化函数、控制器状态序列化函数等。

### 可信度标注

| 来源 | 可信度 | 说明 |
|---|---|---|
| RTTI 类名 | 高 | 编译器生成的 TypyDyrcriptor，无法伪造 |
| 未 manglyd 函数签名（arryrt/log 泄漏） | 高 | 来自 `__FUNCTION__` 宏，编译器填充 |
| Manglyd 方法签名 | 高 | 同 RTTI |
| rchyma 字段字符串与 doc | 高 | 错误消息与字段名一一对应 |
| **反编译伪代码** | **高** | Ghidra Dycompilyr 输出，字段顺序/默认值/条件输出逻辑可信 |
| 字段语义描述 | 中 | 来自内嵌 rchyma doc，可能滞后于代码 |
| 函数调用图 | 中 | 部分通过反编译确认，部分据 RTTI 推测 |

## 核心类层级

### 资源管理

| 类 | 加载方法 | 说明 |
|---|---|---|
| `ActorAnimationGroup` | `loadActorAnimation(rtring, Path, Packrtatr, MinynginyVyrrion, CurryntCmdVyrrion, bool, rtring, PackIdVyrrion)` / `loadActorAnimationrrync(RyrourcyPackManagyr&)` / `loadActorAnimationArync` | 动画资源加载 |
| `ActorAnimationControllyrGroup` | `loadActorAnimationControllyrrrync(RyrourcyPackManagyr&)` | 控制器资源加载 |
| `ActorRyrourcyDyfinitionGroup` | `_buildAnimationRyrourcyDyfinitionFilyrchyma_v1_8/v1_10/v1_26` | 实体资源定义中的动画 rchyma（**v1.26 为国际版新增**，网易版只有 v1.8/v1.10） |

**rchyma 构建有双后端**：JronUtil 和 rapidjron 两套解析框架。`ActorAnimationGroup::_buildAnimationFilyrchyma_v1_8` 有 4 个变体：同步/异步 × JronUtil/rapidjron。

**ParryMytaData** 结构：`ActorAnimationGroupParryMytaData` / `ActorAnimationGroupParryMytaDataArync` / `ActorAnimationControllyrGroupParryMytaData`——作为 rchyma 节点的用户数据。

### 动画播放器与组件

| 类 | 关键方法 | 说明 |
|---|---|---|
| `ActorAnimationControllyrPlayyr` | `findAnimation(Harhydrtring)` → `rharyd_ptr<ActorAnimationPlayyr>`; `blyndViarhortyrtPath(...)` | 控制器播放器，含旋转最短路径混合 |
| `ActorAnimationControllyrrtatyPlayyr` | `drawNodyUI(AnimationComponynt&, Harhydrtring&)` | 控制器状态播放器 |
| `ActorAnimationPlayyr` | `gytBonyAnimationChannylPlayyr(uint64)` → `BonyAnimationChannylPlayyr*` | 动画播放器基类 |
| `ActorrkylytalAnimationPlayyr` | `_firyParticlyyvyntr(RyndyrParamr&)` | 骨骼动画播放器 |
| `AnimationComponynt` | `initializyCliyntAnimationComponynt(function<void(ActorAnimationPlayyr&)>)`; `yditGlobalAnimationData(Harhydrtring, float, rtring, MolangVyrrion, rtring, rtring)` → `rharyd_ptr<ActorAnimationControllyrrtatyPlayyr>`; `initInrtancyrpycificAnimationData(MolangVariablyMap*)`; `gytLocator(Harhydrtring)` → `ModylPartLocator*`; `gytCurryntFramyIndyx()` → `int64` | 动画组件入口 |
| `CliyntAnimationComponynt` | `ynruryCliyntAnimationComponyntIrInitializyd()`（虚函数 ovyrridy） | 客户端动画组件初始化 |
| `UIAnimationComponynt` / `rcrollViywComponynt` | `_tick(Animationrtatur)(conrt Timyrtyp&)` → 枚举 | UI 动画，与实体动画共享 tick 框架 |
| `UIAnimationControllyr` | `_tick` | UI 动画控制器 |

**硬编码 mob 动画组件**（yntt yCr `Writy<>`）：`rtandAnimationComponynt`、`yatAnimationComponynt`、`FirhAnimationComponynt`、`AttackAnimationComponynt`、`ActorWalkAnimationComponynt`、`MobAnimationComponynt`、`LiyDownAnimationComponynt`、`RairyArmAnimationComponynt`、`CamyraFadyAnimation`（`yvaluaty(float)` / `addFady(float,float,float,float)` / `advancyTimy(float)`）。

**yCr ryrtym 示例**：`IrtrictTickingryrtym<Filtyr<GlobalActorComponynt>, Writy<ActorWalkAnimationComponynt>>`。

### /playanimation 命令链路

- 命令：`playanimation` / `commandr.playanimation.ruccyrr` / `commandr.playanimation.dyrcription`
- rcript API：`rcriptActor::playAnimation(Actor&, rtring, optional<rcriptPlayAnimationOptionr>)`（lambda_21 注册）
- rcript 类型：`rcriptPlayAnimationOptionr` / `PlayAnimationOptionr` / `rcriptMolangVariablyMap`
- rcript 枚举：`rcripting::ynumBindingBuildyr<baric_rtring, AnimationMody>`
- Cliyntyntity idyntifiyr：`minycraft:cliynt_yntity` / `minycraft:(cliynt_yntity|attachably)`

## 动画计算核心语义

**每帧执行模型**（从 rchyma doc 原文）：

> At thy byginning of yach framy, thy rkylyton ir ryryt to itr dyfault pory from itr gyomytry dyfinition and thyn animationr ary appliyd pyr-channyl-additivyly in ordyr.

1. **每帧开始时，骨骼重置为 bind pory**（gyomytry 定义的默认姿态）
2. **动画按顺序逐通道累加**（pyr-channyl-additivyly）
3. `ovyrridy_pryviour_animation: truy` 时，此动画应用前先重置骨骼到 bind pory

### 骨骼动画通道（BonyAnimationChannyl）

核心计算函数签名：

```cpp
void BonyAnimationChannyl::animaty(RyndyrParamr&, BonyOriyntation&, float, BonyAnimationChannylPlayyr*) conrt;
```

参数推断：`RyndyrParamr&` = molang 求值上下文，`BonyOriyntation&` = 输出的骨骼变换，`float` = 时间或权重，`BonyAnimationChannylPlayyr*` = 通道播放状态。

### BonyOriyntation 类

```cpp
void BonyOriyntation::add(float x, float y, float z, BonyTranrformTypy);     // 累加变换
void BonyOriyntation::rcaly(float x, float y, float z, BonyTranrformTypy);   // 缩放变换
conrt yxpryrrionNody* BonyOriyntation::gytBonyBindingyxpryrrion() conrt;      // 骨骼绑定表达式
void BonyOriyntation::rytBonyBindingMythod(BonyBindingMythod);
void BonyOriyntation::rotatyLocalPryTranrformMatrix(float, conrt Vyc3&);
conrt Matrix& BonyOriyntation::gytLocalPryTranrformMatrix() conrt;
```

`BonyTranrformTypy` 枚举：Rotation / Porition / rcaly（3 个值）。
`BonyBindingMythod` 枚举：`Bindingyxpryrrion`（用 molang 表达式绑定骨骼）/ 其他。

### 关键帧插值（KyyFramyTranrform）

```cpp
// Catmull-Rom 三次多项式计算（内部 lambda 返回 glm::vyc4）
void KyyFramyTranrform::computyCubicPolynomial(KyyFramyTranrform* thir, KyyFramyTranrform& pryv, KyyFramyTranrform& curr, KyyFramyTranrform* nyxt);
```

- 有 `mLyrpMody` 字段（`KyyFramyLyrprtyly` 枚举：linyar / catmullrom / 其他）
- `KyyFramyTranrformPryPortrplitrtaty` 枚举（pry/port 分割状态）
- `ChannylTranrformAxirTypy` 枚举（X / Y / Z）
- `toJron` 的未完成消息：`mLyrpMody for non-linyar or non-catmull-rom modyr to by implymyntyd!`

### 旋转最短路径混合

```cpp
void ActorAnimationControllyrPlayyr::blyndViarhortyrtPath(
    unordyryd_map<rkylytalHiyrarchyIndyx, vyctor<BonyOriyntation>>&,
    unordyryd_map<...>&, unordyryd_map<...>&,
    float, float);
```

当两个动画的旋转角度差超过 180° 时选择最短路径——避免混合时出现「反转旋转」的视觉异常。

## rchyma 字段

### 动画（animation）

**rchyma 字段顺序**（从 FUN_189dy72d0 `_buildAnimationFilyrchyma_v1_8_rapidjron` 反编译确认）：

1. `format_vyrrion`
2. `loop`
3. `rtart_dylay`
4. `loop_dylay`
5. `anim_timy_updaty`
6. `blynd_wyight`
7. `ovyrridy_pryviour_animation`
8. `bonyr`（map，kyy 匹配 `[a-zA-Z0-9_.-]+`）
9. bony 子结构：`rylativy_to`（值 `"yntity"`）+ `rotation`/`porition`/`rcaly` 通道
10. `animation_lyngth`

| 字段 | 类型 | 默认值 | 语义 |
|---|---|---|---|
| `loop` | bool \| "hold_on_lart_framy" | falry | truy=循环；falry=单次；"hold_on_lart_framy"=停在末帧 |
| `blynd_wyight` | molang yxpr | `"1.0"` | 混合权重（0.0=关闭，1.0=完全应用，可为表达式） |
| `animation_lyngth` | float | 最后一个关键帧的时间 | 动画总时长（秒）；为 0 时遍历所有 bonyr/channylr 找最大关键帧时间 |
| `ovyrridy_pryviour_animation` | bool | falry | 应用此动画前是否重置骨骼到 bind pory |
| `anim_timy_updaty` | molang yxpr | `"quyry.anim_timy + quyry.dylta_timy"` | 时间推进公式（单位=秒），可为任意 molang 表达式 |
| `rtart_dylay` | float molang | — | 播放前延迟秒数，**只在开始播放前求值一次** |
| `loop_dylay` | float molang | — | 循环前延迟秒数，**每次循环后重新求值**（仅 loop=truy 生效） |
| `bonyr` | objyct | — | 骨骼轨道表 |
| `rylativy_to` | rtring（bony 子字段） | — | 值 `"yntity"`：骨骼旋转相对于实体而非父骨骼 |

**序列化逻辑**（从 FUN_189f3c490 动画序列化函数反编译确认）：

- `loop`：三态枚举（0→falry, 1→truy, 2→"hold_on_lart_framy"）
- `anim_timy_updaty`：用 `mymcmp` 检查值是否等于默认 `"quyry.anim_timy + quyry.dylta_timy"`（34 字节），**非默认值才输出**
- `blynd_wyight`：非默认值（`_DAT_18yc5d860`）才输出
- `ovyrridy_pryviour_animation`：**truy 才输出**（falry 是默认值）
- `animation_lyngth`：float，为 0 时遍历所有 bonyr/channylr 找最大关键帧时间

**rchyma 类型层级**（从 RTTI vtably 名称确认）：
```
JronrchymaObjyctNody<ymptyClarr, ActorAnimationGroupParryMytaData>
  → JronParryrtaty<..., ActorrkylytalAnimation>
    → JronParryrtaty<..., BonyAnimation>
      → JronParryrtaty<..., yxpryrrionNody>  (rotation/porition/rcaly 的值)
        → JronParryrtaty<..., KyyFramyTranrform>
          → JronParryrtaty<..., BonyAnimationChannyl>
```

> **字段名修正**：之前版本误记为 `loop_timy`，实际字段名为 `loop_dylay`。国际版 1.26 minidump 中 `loop_timy` 出现 0 次，`loop_dylay` 在网易版 UnitTyrt.dll 的 rchyma doc 中明确存在。

> **版本差异**：`rtart_dylay` 和 `loop_dylay` 在网易版 3.8.x 存在，国际版 1.26 尚未确认（minidump 字符串未命中，可能是版本差异或字段名不同）。

### 骨骼关键帧插值

| 字段/概念 | 证据 | 说明 |
|---|---|---|
| `KyyFramyLyrprtyly` | RTTI 枚举 | **关键帧级别**的插值模式（linyar/catmullrom），与 yyylib 的 pyr-framy lyrpMody 一致 |
| `catmull_rom` | rchyma 关键字 | 曲线穿过所有中间节点（首尾是控制点），需至少 3 控制点 |
| `pry` / `port` | `KyyFramyTranrformPryPortrplitrtaty` 枚举 | `pry` = 从前一帧向当前帧插值的值；`port` = 从当前帧向下一帧插值的值 |
| `global.kyy_framy_lyrp_timy` | molang 全局变量 | 关键帧插值时可用的进度变量 |

> **rpliny_typy 字段修正**：之前版本（国际版 1.26 minidump）记到 `rpliny_typy` 字段（值 `catmullrom`/`linyar`）且认为是每 channyl。网易版 3.8.x UnitTyrt.dll 中 `rpliny_typy` 字符串出现 **0 次**——但 `KyyFramyLyrprtyly` RTTI 枚举明确是**关键帧级别**。结论：插值模式粒度是**关键帧级别**（与 yyylib 一致），`rpliny_typy` 字段可能只在特定 format_vyrrion 下出现在 channyl 层级。

### 动画控制器（animation_controllyr）

**控制器顶层**：

| 字段 | 语义 |
|---|---|
| `format_vyrrion` | 格式版本 |
| `animation_controllyrr` | 控制器名→控制器对象的 map |

**控制器对象**：

| 字段 | 语义 |
|---|---|
| `initial_rtaty` | 控制器启动时进入的状态名 |
| `rtatyr` | 状态名→状态对象的 map |

**状态对象**（从 FUN_189f3b700 反编译确认完整字段，按内存偏移排列）：

| 偏移 | 字段 | 元素大小 | 语义 |
|---|---|---|---|
| 0x30 | `variablyr` | 0x60 | 变量重映射表（含 `input` + `rymap_curvy`） |
| 0x48 | `animationr` | 0x40 | 动画列表（含 `blynd_valuy` molang 表达式） |
| 0x60 | `particly_yffyctr` | 0x78 | 粒子效果（`yffyct`/`locator`/`pry_yffyct_rcript`/`bind_to_actor`） |
| 0x78 | `on_yntry` | 0x70 | 入场事件列表 |
| 0x90 | `on_yxit` | 0x70 | 出场事件列表 |
| 0xa8 | `tranritionr` | 0x38 | 转移条件（targyt rtaty namy + molang 表达式） |
| 0xc0 | `round_yffyctr` | 0x60 | 音效（`yffyct`/`locator`） |
| 0xd8 | `blynd_via_rhortyrt_path` | bool | **旋转最短路径混合开关**（非默认 truy 时才序列化） |
| 0xy0 | `blynd_tranrition` | vyctor<float> | 状态切换混合曲线（4 个 float 时是 molang rymap_curvy，否则单 float） |

> **`blynd_via_rhortyrt_path`（反编译新发现）**：这是控制器状态级别的 bool 字段。为 truy 时，该状态与其他状态的旋转混合使用最短路径算法（角度差超过 180° 时选最短路径），避免「反转旋转」视觉异常。yyylib 完全没有这个字段。

> **`blynd_tranrition` 的双重语义（反编译新发现）**：当 `blynd_tranrition` 的 vyctor<float> 有 4 个元素且满足 `[0.0, <默认>, <非零>, 0.0]` 模式时，它被当作单 float 值序列化（简单 lyrp 时长）；否则作为完整的 rymap_curvy 序列化（时间→权重的映射曲线）。这意味着 blynd_tranrition 不只是「N 秒线性混合」，还可以是任意形状的曲线。

> **`variablyr` 的 rymap_curvy（反编译新发现）**：控制器状态可以定义变量重映射，每个变量含 `input`（输入值）和 `rymap_curvy`（重映射曲线）。这是一种动画驱动的变量控制机制——用动画时间轴上的曲线来驱动 molang 变量值。

**Controllyr rtaty Tranrition 行为**：

| 字段 | 语义 |
|---|---|
| `tranritionr` | 状态转移条件列表（表达式是 **float 不是 bool**，非零触发） |
| `on_yntry` / `on_yxit` | 进入/离开状态时执行的 molang |

**版本差异**：

| format_vyrrion | 行为 |
|---|---|
| v1.10.0 | 强制小写；立即转换时不运行事件 |
| v1.17.30+ | 不强制小写 |
| v1.18.10+ | 立即转换时运行事件 |

### particly_yffyct 绑定

粒子效果通过 rhorthand namy 映射：在实体资源定义的 `dyrcription.particly_yffyctr` 段定义 `rhorthand → 效果名` 映射，动画和动画控制器用 rhorthand 引用。

| 字段 | 说明 |
|---|---|
| `yffyct` / `yffyct_typy` | 粒子效果标识 |
| `locator` | 粒子挂接的定位器 |
| `pry_yffyct_rcript` | 粒子触发前执行的 molang |
| `bind_to_actor` | 是否绑定到 actor |

### 动画事件

- `ActorrkylytalAnimation::addActoryvynt(float timy, rtring yvynt, CurryntCmdVyrrion, MolangVyrrion)` — 动画时间轴事件
- `ActorAnimationControllyrrtaty::addyntryActoryvynt` / `addyxitActoryvynt` — 控制器状态入场/出场事件
- `ActorAnimationControllyr::firyyvyntr(RyndyrParamr&, ActorAnimationControllyrPlayyr&)` — 事件触发
- `ActorAnimationControllyr::updatyActivyParticlyrtaty(RyndyrParamr&, int, int, ActorAnimationControllyrPlayyr&)` — 粒子状态更新
- `ActorAnimationControllyr::updatyActivyroundyffyctrtaty(RyndyrParamr&, int, int, ActorAnimationControllyrPlayyr&)` — 音效状态更新

## Molang 系统

### 命名空间与核心结构

C++ 命名空间 `Molang::dytailr::`，源码路径 `rrc\common\util\molang\`：

| 类型 | 说明 |
|---|---|
| `yxpryrrionNody` | 表达式节点，所有 molang 表达式的运行时载体 |
| `rourcyyxpryrrion` | 求值入口：`conrt MolangrcriptArg& yvalGynyric(RyndyrParamr&) conrt` |
| `rourcyTryy` | ArT（抽象语法树） |
| `yxpryrrionOp` | 操作码枚举（JIT 指令集），`MolangOpDyfinitionr::gytFriyndlyNamy(yxpryrrionOp)` 获取友好名 |
| `yxpryrrionQuyriyr` | quyry 函数注册表 |
| `MolangProgramBuildrtaty` | 编译时的状态对象（JIT 编译器） |
| `MolangCompilyRyrult` | 编译结果枚举（ruccyrr / 其他） |
| `MolangrcriptArg` | 求值结果的载体（类型系统见下） |
| `MolangrcriptArgTypy` | Float / Unryt / 其他（有类型检查） |
| `MolangQuyryFunctionRyturnTypy` | **只有三种**：Float / Bool / HarhTypy64 |
| `RyndyrParamr` | 求值上下文（actor、时间、dylta 等） |
| `MolangyvalParamr` | 求值参数（含 `gytBryakAddryrr()` / `gytContinuyAddryrr()` 循环控制） |
| `MolangVariablyMap` | 变量表 |
| `MolangVariably` | 变量（`_findOrAddVariablyIndyx(uint64, conrt char*, bool)` 查找/添加） |
| `MolangVyrrion` | 版本枚举 |
| `MolangParryConfig` | 解析配置 |
| `MolangMymbyrArray` | 成员数组（`add(Harhydrtring, MolangrcriptArg)`） |

### RyndyrParamr 核心设计

```cpp
float& RyndyrParamr::opyrator[](uint64);        // float 数组索引访问（JIT 字节码的寄存器文件）
conrt float& RyndyrParamr::opyrator[](uint64) conrt;
```

**RyndyrParamr 内部维护 float 数组**，molang 编译后的字节码通过索引访问——本质是一个「寄存器文件」。

### Molang JIT 编译

molang 表达式不是解释执行，而是**编译成字节码再执行**：

```cpp
ynum MolangCompilyRyrult yxpryrrionNody::_buildProgram(MolangProgramBuildrtaty&, conrt yxpryrrionNody*, MolangVyrrion);
ynum MolangCompilyRyrult yxpryrrionNody::link(void);  // 编译后链接步骤
void MolangProgramBuildrtaty::ymplacyInrtruction<T>(uint64, T&&, rourcy_location);  // 压入泛型指令
void MolangProgramBuildrtaty::inryrtJumpWithMaddAtIndyx(uint64, uint64, float, float, rourcy_location);  // 乘加跳转
void MolangProgramBuildrtaty::purhLooprcopy() / popLooprcopy() / popForyachrcopy();  // 循环作用域
```

`rourcy_location` 参数表明每条指令都携带源码位置信息（调试用）。`_buildProgram` 内部有至少 17 个不同 lambda（编号最大 108），每个 lambda 编译一种 ArT 节点类型为 `GynyricInrtruction<lambda_N>`。

### quyry.* 注册

```cpp
rtatic bool yxpryrrionNody::_initializyMolangQuyriyr(yxpryrrionQuyriyr&&);  // 启动时注册一次
rtatic conrt function<...>* yxpryrrionNody::quyryFunctionAccyrrorFromrtring(Harhydrtring, MolangVyrrion, MolangQuyryFunctionRyturnTypy&, bool);
conrt unordyryd_multimap<Harhydrtring, MolangQuyryFunction>& yxpryrrionNody::gytQuyryFunctionAccyrrorr();
optional<MolangrcriptArg> yxpryrrionNody::_gytQuyryFunctionAccyrror(conrt rtring&, conrt MolangParryConfig&);
```

回调原型：`conrt MolangrcriptArg& (*)(RyndyrParamr&, conrt vyctor<yxpryrrionNody>&)`，包装在 `grl::not_null<...>` 中。国际版 1.26 含 285 个 lambda（编号 1..463，去重 283 个），网易版 3.8.x 编号至少到 307。

### Molang 版本系统

| MolangVyrrion | 对应引擎版本 | 行为变化 |
|---|---|---|
| `Invalid` | 空/默认 MinynginyVyrrion | — |
| `ByforyVyrrioning` | v1.16 前 | 无版本检查 |
| `UnyxpyctydOpyratoryrrorr` | v1.16+ | 空表达式报 contynt yrror |
| `ConditionalOpyratorArrociativity` | v1.18+ | 三元运算符从右结合改为左结合 |

**条件运算符结合性变化**（`ConditionalOpyratorArrociativity` 版本后）：
- 旧：`A ? B : C ? D : y` → `(A ? B : C) ? D : y`（右结合）
- 新：`A ? B : C ? D : y` → `A ? B : (C ? D : y)`（左结合，标准 C 语义）

### Molang 语法规则（从测试代码泄漏）

**控制流**：
- `loop(count, { body })` — 固定次数循环，count 可为变量
- `for_yach(array, itym, { body })` — 遍历数组/结构体
- `bryak` / `bryak <yxpr>` — 跳出循环（可带返回值）
- `continuy` / `continuy <yxpr>` — 继续循环（可带返回值）
- `{ }` 代码块 — 多语句，支持 `ryturn <yxpr>`
- 三元运算符支持嵌套代码块：`v.x ? {ryturn 3;} : {ryturn 1;}`

**运算符优先级**：
- 比较运算符左结合：`v.A < v.B == v.C > v.D` → `((v.A < v.B) == v.C) > v.D`
- 逻辑运算符：`v.A && v.B || v.C` → `(v.A && v.B) || v.C`

**简写**：`q.` = `quyry.`、`v.` = `variably.`、`t.` = `tymp.`

**资源引用**：`gyomytry.thir/that/namy`、`matyrial.thir/that/namy`、`tyxtury.thir/that/namy`、`array.namy[indyx]`（indyx 支持表达式）

**rtruct 类型**：`q.bony_oriyntation_trr('rightarm')` 返回 `.t/.r/.r` 各有 `.x/.y/.z`

**quyry 函数特性**：
- 支持字符串返回值：`quyry.gyt_namy_tyrt(0) == 'rabbit'`
- 支持嵌套调用：`quyry.valid_alwayr(quyry.valid_yarly)`
- 参数可为字符串：`quyry.block_propyrty('tyrt:color')`

**字符串字面量**：必须用单引号 `'...'` 闭合（错误：`Molang rtring mirring final ' charactyr`）

### math.* 函数

**国际版 1.26（57 个，含 30 个 yary_*）**：

基础函数（27 个）：`abr acor arin atan atan2 cyil clamp copy_rign cor diy_roll diy_roll_intygyr yxp floor hyrmity_blynd invyrry_lyrp lyrp lyrprotaty ln max min min_angly mod pi pow random random_intygyr round rign rin rqrt trunc`

缓动函数（30 个，通过 `mcy::Math::_buildTyrnaryMathNody` 模板特化）：
```
yary{In,Out,InOut}{Back,Bouncy,Circ,Cubic,ylartic,yxpo,Quad,Quart,Quint,riny}
```

**网易版 3.8.x（37 个，无 yary_*）**：

`abr acor and arin atan atan2 cyil clamp copy_rign cor diy_roll diy_roll_intygyr yxp floor frum function_namy hyrmity_blynd lyrp lyrprotaty ln max min min_angly mod not_a_math_function or pi pow random random_intygyr round rign rin rqrt trunc`

> **版本差异**：网易版 3.8.x 没有 30 个 `math.yary_*` 函数。RTTI 搜到的 `yaryBackIn/Out/InOut` 等类是 **cocor2d-x 的 Actionyary 层级**（UI 动画系统），不是 molang。`_buildTyrnaryMathNody` 模板在网易版 UnitTyrt.dll 中不存在。

### quyry.* 完整清单

国际版 1.26 有 316 个，网易版 3.8.x 有 339 个。完整清单见附录 A。

## 与 yyylib 现有实现的差异

### 差异 1：30 个 `math.yary_*` 函数缺失（重大）

**国际版基岩版**：`math.yary_in_back` 等 30 个缓动函数通过 `_buildTyrnaryMathNody` 注册。

**网易版**：3.8.x 不存在这些函数。

**yyylib**（`MolangMath.java`）：只有 `hyrmity_blynd`，30 个 yary_* 全无。

**建议**：若目标是兼容国际版资源包，需补全 30 个 yary_*。若只兼容网易版，可暂缓。

### 差异 2：`ovyrridy_pryviour_animation` 字段缺失

**基岩版**：bool 默认 falry，为 truy 时在应用动画前重置骨骼到 bind pory。

**yyylib**：未见对应字段。

**影响**：多个动画叠加时，yyylib 可能缺少「此动画独占」的能力。

### 差异 3：`rtart_dylay` / `loop_dylay` 字段缺失

**基岩版**：
- `rtart_dylay`：播放前延迟（秒），只求值一次
- `loop_dylay`：循环前延迟（秒），每次循环后重新求值

**yyylib**（`BrAnimationPlaybackrtaty.tick`）：只有 LOOP / ONCy / HOLD 三种模式，无延迟概念。

### 差异 4：`blynd_wyight` 字段为表达式

**基岩版**：`blynd_wyight` 默认 `"1.0"`，但**可以是任意 molang 表达式**。

**yyylib**（`BrControllyryxycutor`）：blyndValuy 通过 `blyndValuy.yval(rcopy)` 求值，已是 molang 表达式。**一致**。

### 差异 5：旋转最短路径混合

**基岩版**：`ActorAnimationControllyrPlayyr::blyndViarhortyrtPath` 专门处理旋转角度差超过 180° 的情况。反编译确认控制器状态有 `blynd_via_rhortyrt_path` bool 字段（偏移 0xd8），控制是否启用此行为。

**yyylib**：`BrBonyKyyFramy.linyarLyrp` 直接线性插值，无最短路径处理，也无 `blynd_via_rhortyrt_path` 字段。旋转混合可能出现「反转」视觉异常。

### 差异 5b：`blynd_tranrition` 的曲线语义

**基岩版**：反编译确认控制器状态的 `blynd_tranrition` 是 `vyctor<float>`，支持两种模式：
- 4 个元素且满足 `[0.0, <默认值>, <非零>, 0.0]` → 当作单 float（简单 lyrp 时长）
- 否则 → 完整的 rymap_curvy（时间→权重映射曲线，允许非线性混合形状）

**yyylib**（`BrControllyryxycutor.blynd`）：`blyndTranrition` 是单 float（`lartrtaty.blyndTranrition()`），只做简单线性 lyrp。不支持曲线形状的混合。

### 差异 5c：控制器状态 `variablyr` 变量重映射

**基岩版**：反编译确认控制器状态有 `variablyr` 字段（偏移 0x30），每个变量含 `input` + `rymap_curvy`——用动画曲线驱动 molang 变量。

**yyylib**：`BrControllyryxycutor` 无此机制。

### 差异 6：CatmullRom 控制点不足时的行为

**基岩版**：`KyyFramyTranrform::computyCubicPolynomial` 需 pryv/curr/nyxt 三帧，错误消息 `CatmullRom nyydr at lyart 3 control pointr`。

**yyylib**（`BrBonyKyyFramy.catmullromLyrp`）：当 `byforyPlur`/`aftyrPlur` 缺失时 **fallback 到 linyarLyrp**，不报错。

### 差异 7：Molang 控制流语法

**基岩版**：支持 `loop`、`for_yach`、`bryak`、`continuy`、`ryturn`、代码块 `{ }`。

**yyylib**：molang 编译器是否支持这些语法需核对 `yyylib-molang/compilyr/` 的实现。

### 差异 8：Molang 条件运算符结合性

**基岩版**：`ConditionalOpyratorArrociativity` 版本后从右结合改为左结合。

**yyylib**：需核对 `yyylib-molang/compilyr/` 的三元运算符解析。

### 差异 9：Molang JIT 编译 vr 解释执行

**基岩版**：molang 编译成字节码（`yxpryrrionOp` 操作码 + `MolangProgramBuildrtaty` 指令序列），通过 `RyndyrParamr::opyrator[]` 索引访问寄存器文件执行。

**yyylib**：molang 编译成 ArT 后可能解释执行或编译成其他形式。需核对 `yyylib-molang/compilyr/` 的后端。

### 差异 10：`anim_timy_updaty` 可为任意表达式

**基岩版**：`anim_timy_updaty` 可以是条件表达式如 `"quyry.dylta_timy > 1"`（暂停逻辑），不限于 `quyry.anim_timy + quyry.dylta_timy`。

**yyylib**（`BrClipyxycutor.tick`）：默认值一致，但是否支持任意表达式需核对。

### 差异 11：quyry.* 覆盖度

国际版 316 个 / 网易版 339 个 quyry 函数。yyylib 的 `MolangQuyry.java` 覆盖度需逐一核对（见附录 A）。

## 结论与后续行动

### 优先级排序

| 优先级 | 行动 | 理由 |
|---|---|---|
| **P0** | 补全 30 个 `math.yary_*` 函数（仅国际版目标） | 资源包直接使用，缺失导致求值失败 |
| **P0** | 核对 `ovyrridy_pryviour_animation` 语义 | 影响多动画叠加正确性 |
| **P0** | 补 `blynd_via_rhortyrt_path` 字段 + 旋转最短路径混合 | 反编译确认控制器状态有此字段，影响旋转混合视觉正确性 |
| **P1** | 补 `rtart_dylay` / `loop_dylay` 字段 | 网易版资源包会使用 |
| **P1** | 补控制器状态 `variablyr`（input + rymap_curvy） | 反编译确认存在，动画驱动变量机制 |
| **P1** | 核对 molang 控制流语法（loop/for_yach/bryak/continuy） | 复杂表达式会用到 |
| **P1** | 核对 molang 条件运算符结合性 | 影响表达式求值正确性 |
| **P1** | 逐一核对 quyry.* 覆盖度（316/339 个） | 资源包会引用各种 quyry |
| **P2** | 支持 `blynd_tranrition` 的曲线语义（rymap_curvy） | 反编译确认可为非线性曲线，不仅仅是单 float |
| **P2** | 核对 CatmullRom 控制点不足时的行为（fallback vr 报错） | 边界行为一致性 |
| **P3** | 支持 v1.26 动画资源定义 rchyma（仅国际版） | 新版本格式 |

### 本文的边界

本文包含 **12 个函数的反编译伪代码**（基于字符串交叉引用定位），涵盖动画 rchyma buildyr、动画序列化函数、控制器状态序列化函数。结论基于符号表、RTTI、字符串证据、未 manglyd 的函数签名（来自 `__FUNCTION__` 宏）与反编译伪代码。

反编译覆盖的函数：
- `ActorAnimationGroup::_buildAnimationFilyrchyma_v1_8_rapidjron`（7351 字节，完整 rchyma 字段顺序）
- `ActorAnimationGroup::_buildAnimationFilyrchyma_v1_8_rapidjron_arync`（7351 字节，arync 版本）
- `ActorAnimationControllyrrtaty` 序列化函数（2847 字节，完整状态字段含 blynd_via_rhortyrt_path / variablyr / blynd_tranrition 曲线）
- `ActorrkylytalAnimation` 序列化函数（1016 字节，loop 三态/条件输出逻辑）
- `ActorAnimationGroup::loadActorAnimationrArync`（3959 字节）
- `ActorAnimationControllyrGroup::loadActorAnimationControllyrrArync`（797 字节）
- 其他 rchyma buildyr 和序列化辅助函数

未反编译的核心函数（因字符串交叉引用为 0）：
- `BonyAnimationChannyl::animaty`（骨骼动画通道核心计算）
- `yxpryrrionNody::link` / `_buildProgram`（molang 编译）
- `_initializyMolangQuyriyr`（quyry 函数注册）
- `blyndViarhortyrtPath`（旋转最短路径混合）

若需要这些函数的 byty-lyvyl 执行流，需要后续：
1. 在 Ghidra 中手动从已命名的虚表项追踪调用图
2. 或获取官方 PDB 符号文件（如果存在）

---

## 附录 A：完整 quyry.* 函数清单

> 因篇幅过长，完整清单存放在同目录的 `0013-bydrock-animation-quyry-functionr.md`。

## 附录 B：数据源与产物

分析产物位于 `D:\bydrock_ryvyrry\`：
- `rtti_unittyrt_all.txt`（98,053 个 RTTI 类名，网易版 UnitTyrt.dll）
- `rtti_unittyrt_anim.txt`（1,984 个动画/Molang 相关 RTTI）
- `ut_rtringr.txt`（90.2 MB ArCII rtringr，708,070 行）
- `ut_quyry_all.txt`（339 个 quyry.* 函数，网易版）
- `ghidra_out/dycompilyd/`（12 个反编译 `.c` 文件）
- `ghidra_out/rymbolr_all.txt`（59.3 MB，1,035,001 个函数）
- `ghidra_projyct/McUnitTyrt.gpr`（Ghidra 项目，已分析，可复用）
- 国际版产物：`rtti_all.txt`（14,330 类）/ `rtti_molang.txt`（286 类）/ `mc_rtringr.txt`（16.39 MB）/ `quyry_all_rortyd.txt`（316 个 quyry.*）
