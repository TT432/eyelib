# ADR-0042：实体 pass 状态的单一权威（alpha test / 深度写 / cull）

> **状态**：已实施（2026-11-06）
> **关联**：ADR-0015（跨版本差异分级）、ADR-0016（DDD + bridge 是 `//?` 唯一栖息地）、
> ADR-0040（26.1 LIGHTMAP_ONLY）、ADR-0041（Iris ShaderKey 精确登记）
> **触发**：用户报告「监守者/幻翼的 cutout 片与水面冲突，透过片看不到水」+「幻翼近距离
> z 冲突式闪烁」在既有修复后仍存在

---

## 1. 问题：同一 Bedrock 材质被三套构造函数各自重建渲染状态

`PortRenderPass`（domain）只携带 **透明度 + cull** 两维。实体几何的其余渲染状态在三个地方
各自「重新发明」：

| # | 路径 | 位置 | 补全的状态 |
|---|------|------|-----------|
| 1 | `<26.1` 回退 RenderType | `RenderPassAdapter.toRenderType` 的 switch | 只映射了部分 (透明度 × cull) 组合；SOLID/TRANSLUCENT_EMISSIVE/ADDITIVE 分支**丢弃** `disableCulling`，ADDITIVE 还退化成 alpha 混合 |
| 2 | `>=26.1` 回退 RenderType | `BrRenderTypeFactory.entityPassState` | 自造 `WriteMask(true, !blend)`（半透明**不写**深度，注释称「与 vanilla entityTranslucent 一致」——实测相反）；cull 取自 pass |
| 3 | 材质驱动（两代都用） | `BrRenderTypeFactory.custom` / `buildPipeline` | 按 Bedrock `DisableDepthWrite` 直译（默认写深度）；26.1 **只在 CUTOUT surface class** 加 `ALPHA_CUTOUT` define |

三套规则的差异带来同一条链路上的三处不守恒：

- **alpha test 缺失**：26.1 的 `core/entity.fsh` 只在 `#ifdef ALPHA_CUTOUT` 时
  `if (color.a < ALPHA_CUTOUT) discard`。路径 2/3 对 BLEND / TRANSLUCENT_EMISSIVE / ADDITIVE
  都不加该 define，于是**贴图里 alpha≈0 的 texel 照样光栅化**。
- **深度写口径相反**：路径 2 让半透明不写深度；vanilla 26.1 `ENTITY_TRANSLUCENT` 是
  `DepthStencilState.DEFAULT`（写），只有 `ENTITY_TRANSLUCENT_EMISSIVE` 是
  `DepthStencilState(LESS_THAN_OR_EQUAL, false)`（不写）。路径 3 又按 Bedrock 默认写。
- **cull 丢失**：路径 1 的 SOLID 分支固定 `entitySolid`（cull），`entity_nocull` 材质在
  1.20.1/1.21.1 背面被剔除；TRANSLUCENT_EMISSIVE 分支固定 `entityTranslucentEmissive`（NO_CULL），
  cull 组合无法表达。

## 2. 症状机理（为什么表现为「透过片看不到水」和「z 冲突闪烁」）

26.1 帧图顺序（`LevelRenderer.addMainPass`）：不透明地形 → solid features →
`copyDepthFrom(main → translucent)` → translucent features → 半透明地形（水）→ 半透明粒子；
最后 `post/transparency.fsh` 把 main/translucent/particles/… 各层按**各自的深度**逐像素排序混合。

- 透明 texel 写了深度 ⇒ `MainDepthSampler` 在该像素取到「片的深度」，比水更近 ⇒ 合成时把
  main 层判成前排、水判成后排 ⇒ **透过片的透明区域看不到水**（底下的地形/天空反而盖住水）。
- 同模型多层半透明片：一层写深度、一层不写（路径 2 vs 3），加上 `sortOnUpload` 的排序键是
  顶点距离而非相机距离 ⇒ 层间遮挡关系随姿态/视角翻转 ⇒ 表现为**近距离 z 冲突式闪烁**。

`<26.1` 的实体着色器把 0.1 discard 写死在 `rendertype_entity_translucent(_cull)` /
`..._translucent_emissive` / `..._cutout` 里，只有 `rendertype_entity_solid` 不 discard，
所以同一份 Bedrock 材质在 1.20.1/1.21.1 与 26.1 上表现不同——这正是「跨版本行为不一致」的
工程根因，而不是某一个版本的孤立 bug。

### 数据侧证据（工作单元：静态审计 `work/alpha_audit2.py`）

按渲染器真实的双线性 UV 采样 cospack 贴图：

| 实体 | 材质 | 通道 | 深度写 | 透明 texel 占比 |
|---|---|---|---|---|
| minecraft:phantom | `translucent` | BLEND | 写 | 25.5% |
| minecraft:warden | `doki_warden_glow_nocull` | BLEND+EMISSIVE | 写 | 21.2% |
| minecraft:creeper | `charged_creeper` | ADDITIVE | 写 | 16.7% |
| minecraft:cow/pig/mooshroom | `entity` | SOLID | 写 | 3~9%（含图集岛间隙） |

幻翼与监守者的片状特效正是前两行：透明 texel 占比高、且走 blend 通道。

## 3. 决定

**实体 pass 的全部状态由一处 policy 决定，所有构造函数共用；不允许任何一维被近似或丢弃。**

1. `BrRenderTypeFactory.PassKind { ENTITY, PARTICLE }` 显式区分通道语义。
2. `BrRenderTypeFactory.alphaTest(state, kind)`：
   - `ENTITY`：**非不透明 pass 一律 0.1**（OPAQUE/EMISSIVE/GLINT 除外）。这是两代 vanilla
     实体通道的共同契约：<26.1 写在着色器里，26.1 由 `ALPHA_CUTOUT=0.1` 显式定义
     （`ENTITY_CUTOUT(_CULL)`、`ENTITY_TRANSLUCENT(_CULL)`、`ENTITY_TRANSLUCENT_EMISSIVE`、
     `ARMOR_TRANSLUCENT`、`BREEZE_WIND`、`ENERGY_SWIRL` 全部定义；`ENTITY_SOLID`/`GLINT` 不定义）。
     **不透明 pass 保持不 discard**（与 vanilla 一致；对 SOLID 贴图的岛间隙做 discard 会露出接缝）。
   - `PARTICLE`：只有材质显式 `ALPHA_TEST` 才 0.1（26.1 `TRANSLUCENT_PARTICLE` 不定义
     `ALPHA_CUTOUT`，软 alpha 粒子必须保留）。
3. `BrRenderTypeFactory.effectiveWriteDepth(state, kind)`：
   - `ENTITY` 且 surfaceClass 为 `TRANSLUCENT_EMISSIVE` ⇒ **不写**（vanilla 两代一致）；
   - 其余尊重 Bedrock `DisableDepthWrite`（默认写），即半透明/加法实体**写**深度，
     与 `ENTITY_TRANSLUCENT` 对齐，消除「层间顺序翻转」的闪烁源。
4. `>=26.1` 的 pipeline 用 policy 决定 `ALPHA_CUTOUT` define 与深度写；`<26.1` 的 custom
   RenderType 用 policy 决定 writeMask。`PIPELINE_CACHE`/`CACHE` 键与 pipeline location 都带
   `PassKind`（否则同一 state 的两种通道会互相覆盖，Iris 按 pipeline 身份登记时会串程序）。
5. `RenderPassAdapter`（`<26.1`）改为**全组合映射**：vanilla 类型能逐维等价表达时才用
   （SOLID+cull、ALPHA_TEST±cull、TRANSLUCENT±cull、TRANSLUCENT_EMISSIVE(nocull)），
   其余（SOLID+nocull、TRANSLUCENT_EMISSIVE+cull、ADDITIVE）一律走
   `BrRenderTypeFactory.createForEntityPass`（同一份 canonical 状态表）。
6. `createForEntityPass` 的 canonical 状态表按 vanilla 26.1 补全：cutout/translucent 写深度、
   translucent_emissive 不写、ADDITIVE 用 SRC_ALPHA/ONE 混合（原来退化成了 alpha 混合）。
7. 26.1 实体 RenderType 设 `AFFECTS_OUTLINE`（vanilla 全部 entity 类型都设；发光描边依赖它）。

## 4. 不变量

- **I1（通道契约）**：实体通道里「写深度 ⇒ 透明 texel 必须 discard」。新增任何 entity pass 必须
  经过 `alphaTest`/`effectiveWriteDepth`，不得直接写 `ALPHA_CUTOUT`/writeMask。
- **I2（全组合）**：`PortRenderPass` 的 (透明度, cull) 两维在三个版本上都可精确表达；
  无法精确表达时走 custom，禁止退化成近似类型。
- **I3（跨版本一致）**：同一 Bedrock 材质在三版本的 alpha / blend / 深度写语义一致。
- **I4（缓存键完整）**：RenderType/pipeline 缓存键必须包含所有影响产物的输入（含 `PassKind`）。

## 5. 已知未修（显式登记，不是遗漏）

`ImmediateRenderSink.flush()` 在每个 ModelComponent 之后执行 `BufferSource.endBatch()`，
因此 `<26.1` 的绘制顺序是 **E1-solid → E1-blend → E2-solid → E2-blend**；vanilla 是
**全部实体 solid/cutout 先 flush（`LevelRenderer` 实体循环后）→ 全部实体 blend 在
半透明地形之前 flush**。后果：编号靠后的实体若位于靠前实体半透明片之后，其不透明几何会被
已写入的深度剔除（该实体看起来缺一块）。

- 与本次症状无关（它们都由 alpha test 缺失解释），故本轮不改，避免同时改动绘制时机。
- 修法：`ImmediateRenderSink` 只对非混合 pass 立即 flush，混合 pass 入队，在
  `RenderLevelStageEvent` 的 `AFTER_BLOCK_ENTITIES`（1.20.1 Forge）/等价 stage
  （1.21.1 NeoForge）一次性 flush；GPU 蒙皮会话同步入队。需三版本实机验证绘制时机。

## 6. 验证

> 单元：实体 pass 状态统一（alpha test / 深度写 / cull 全组合） / 本 ADR + `BrRenderTypeFactory`/`RenderPassAdapter` + 单测 + 审计脚本 / 三节点编译 + `BrRenderTypePassPolicyTest` 全绿 + 26.1 `RenderPipeline` 前后探针 + 1.20.1 实机同场截图

**静态/单元**

- `scripts/alpha_pass_audit.py`：按渲染器真实双线性 UV 采样 cospack 贴图，给出每个实体材质各
  pass 的透明 texel 占比。关键结果：`minecraft:warden` 的 `doki_warden_glow_nocull`
  （BLEND+EMISSIVE，写深度）透明占比 0.21，其中特效骨骼 `groundwave_*`/`wind_*`/`light_*`/`ghost*`
  达 0.95~1.00；`minecraft:phantom` 的 `translucent`（BLEND，写深度）0.25，`mouth_*`/`eyesbase`
  0.67~0.83；`minecraft:creeper` 的 `charged_creeper`（ADDITIVE）0.17。
- 单测 `BrRenderTypePassPolicyTest`（三节点全绿）：entity/particle 的 alpha test 表与深度写表。
- `:1.20.1/:1.21.1/:26.1.2:test --tests material.* --tests bridge.material.*`：三节点通过；
  26.1.2 仅剩 HEAD 既有基线失败（`BrRenderStateLightingModelTest`、`BrRenderStateSpecTest`，
  见 2026-11-06 滞留反馈），无新增。

**实机（26.1.2，`/eval` 读真实 `RenderPipeline`）**

| 对象 | 修复前（21.1.29） | 修复后（21.1.30） |
|---|---|---|
| 幻翼翼 `TRANSLUCENT` | `defines={}`、`depthWrite=true`、`blend=true`、`cull=false` | `ALPHA_CUTOUT=0.1`、`depthWrite=true`、`blend=true`、`cull=false` |
| 监守者 5×`TRANSLUCENT_EMISSIVE` | 无 define、`depthWrite=true` | `ALPHA_CUTOUT=0.1`、`depthWrite=false` |
| 监守者 `ALPHA_TEST` | `ALPHA_CUTOUT=0.1` | 不变 |
| 监守者 `SOLID`（nocull） | 无 define、`depthWrite=true`、`cull=false` | 不变（与 vanilla `ENTITY_SOLID` 对齐） |

**实机（1.20.1）**：监守者（含发光层/光环）+ 幻翼 + 牛 + 水墙同场截图，无实体消失、无黑片，
片状几何后方水面正常渲染（`dokimobs/versions/1.20.1/run/alpha_ab/screenshots/`）。

**未做成的对照（诚实记录）**：幻翼/监守者「修复前 vs 修复后」的像素级 A/B 被两项噪声否决——
幻翼动画持续摆动（同 run 相邻帧 mean|Δ|≈11），两次独立启动的整帧亮度不同（同 ms 帧全屏
mean|Δ|≈57~77）。因此本轮的可证伪证据取**管线状态本身**（上表），而不是截图统计。
