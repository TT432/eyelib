# C1 GPU 蒙皮 ≤26.1（1.20.1 / 1.21.1）结果

> 日期：2026-08-27。前置：26.1.2 P1（c1-gpu-skinning-26.1.2.md）；设计：work/c1-gpu-skinning/DESIGN-P2.md。
> 实现：LegacySkinningManager/LegacySkinningSession/LegacySkinnedGeometry（`bridge/client/render/skinning/adapter/`，`//? if <26.1`）。

## 实现要点（与 26.1.2 的差异）

- palette = `uniform mat4 BonePose[96]/BoneNormal[96]` + TintColor/OverlayUV/LightUV 散装 uniform（vanilla `Uniform` MAT4 数组 → glUniformMatrix4fv）；逐 draw 同步上传，池化 float[1536]×2/会话。
- 骨骼索引顶点属性复用 vanilla **UV1 元素**（SHORT×2，Usage.UV 非 FLOAT → glVertexAttribIPointer），shader `in ivec2 BoneIndex`，取 .x。**零自定义元素注册、零裸 GL**（ADR-0032 兼容）。
- 几何展开三角形（6 顶点/quad，27B stride），VertexBuffer STATIC，序列索引（AutoStorageIndexBuffer 自动 SHORT/INT）。
- partVisibility：不可见骨骼 pose 写退化矩阵（旋转缩放 0、w=1）→ 零面积三角形被光栅化丢弃。
- 绘制：ImmediateRenderSink.flush（endBatch 后）逐会话 `routingType.setupRenderState()` → 蒙皮 uniform → `VertexBuffer.drawWithShader(RenderSystem MV/Proj, shader)` → `clearRenderState()`。材质状态单一事实源仍是 BrRenderTypeFactory；shader 变体在 `custom()` 创建期登记（与 shaderState 的 vanilla shader 选择一一对应），GLINT/未知类型回退 CPU。
- FS 复用 vanilla `rendertype_entity_*` fsh（ShaderInstance 支持命名空间引用）；VS 逐行复刻 vanilla 母版（1.20.1 与 1.21.1 母版不同：1.21.1 `fog_distance(pos, shape)` 2 参、无 IViewRotMat/normal 输出 → 两版资产分开）。
- 能力探测：注册期 `GlStateManager._getInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS)` ≥ 3136（本机 4096），不足则整体禁用回退 CPU。
- 开关：`-Deyelib.gpuSkinning=false`（与 26.1.2 同一开关）。

## 正确性验证

- 单测：1.20.1 全绿（planSlots 重构由既有 SkinningGeometryPackerTest 覆盖）。
- 运行时（1.20.1 + 1.21.1，A&S 1.10 v2）：牛/蜘蛛/僵尸/史莱姆正确渲染，蜘蛛**发光红眼**（EMISSIVE 变体）昼夜正确；蒙皮路径活跃（GEOMETRIES>0、数组池复用无泄漏、OFF run 无 shader 注册日志）。截图：work/c1-gpu-skinning/shots/p2_mc_day.png（1.20.1）、p2_mc_121d.png（1.21.1）、p2_mc_120_ivec2.png（ivec2 修正后回归）。
- 修复的缺陷：①VertexBuffer.upload 前必须 bind VAO（属性指针/索引缓冲记录在 VAO 状态；缺失 → glDrawElements 驱动崩溃，vanilla 同模式 ChunkRenderDispatcher.uploadChunkLayer）②BoneIndex shader 声明须为 ivec2（UV1 SHORT×2 的实际类型；uint 在 NVIDIA 上巧合可用但属类型不匹配）。

## Benchmark（1.20.1，mixed 场景，RTX 4070 Laptop，15s warmup + 30s measure，fresh JVM，ON/OFF 交错）

n96（slime/zombie/skeleton/cow 各 24）：

| run | 场景 | FPS | fi_p50 | fi_p99 | rw_p50 | rw_p99 |
|---|---|---:|---:|---:|---:|---:|
| ON1 | fbo | 126.7 | 7.818 | 10.205 | 7.439 | 9.406 |
| OFF2 | fbo | 128.3 | 7.705 | 9.937 | 7.318 | 9.178 |
| ON3 | fbo | 116.9 | 8.263 | 12.692 | 7.782 | 11.312 |
| OFF4 | fbo | 125.8 | 7.818 | 10.449 | 7.426 | 9.864 |
| ON1 | world | 38.7 | 25.737 | 30.111 | 24.847 | 28.518 |
| OFF2 | world | 38.3 | 25.937 | 31.943 | 25.015 | 30.547 |
| ON3 | world | 37.7 | 26.387 | 31.333 | 25.452 | 29.827 |
| OFF4 | world | 38.3 | 26.091 | 29.273 | 25.170 | 28.217 |

n384（各 96；world 场景 actual 305/309 ≠ configured 384，按有效性规则 world 数据仅作趋势参考）：

| run | 场景 | FPS | fi_p50 | rw_p50 |
|---|---|---:|---:|---:|
| ON | fbo | 62.5 | 15.869 | 15.260 |
| OFF | fbo | 64.0 | 15.496 | 14.891 |
| ON | world | 13.7 | 71.472 | 68.291 |
| OFF | world | 13.7 | 72.911 | 69.993 |

（benchmark 运行于 `in uint BoneIndex` 版本；ivec2 修正只改属性读取声明路径，不影响性能结论。）

## 结论

> **⚠ 已废弃（2026-08-28）**：本节 ON/OFF 对比无效——彼时存在覆盖率空洞（默认状态材质从未登记蒙皮变体，ON 实际也走 CPU 蒙皮），所谓"性能中性"测的是 CPU 对 CPU。真实结论见文末「跨实体合批与覆盖率修复」章。

**1.20.1 上 C1 性能中性**（FBO ON 慢 ~1–3%，world 持平，噪声 ±3–5%）。结构性解释：vanilla 1.20.1 把同 RenderType 几何合批进共享缓冲（全帧每类型一次 draw），蒙皮路径换成逐实体×组件 draw + 状态机 setup/clear + 24KB uniform 上传；省下的顶点 CPU（Opt1-9 后本已不大）被状态切换与上传开销抵消。

**价值定位与后续**：与 26.1.2 结论一致——C1 的直接收益不在当前 OpenGL 帧时间，而在：①GPU 带宽消除（未测）；②ADR-0032 基建（自有 shader/几何通道三版本贯通）；③26.2+ Vulkan compute 路径的落点。若要把 ≤26.1 做到正收益，下一步是 **P2.5 按 RenderType 跨实体状态去重**（阶段化收集 + 每类型一次 setupRenderState，26.1.2 的阶段 flush 同构），把逐实体状态切换从 O(实体数) 降到 O(RenderType 数)。

## Compute 蒙皮路径（GL≥4.6，2026-08-27 增补）

**动机**：用户决策——在检测到 OpenGL ≥ 4.6 时用 compute shader 再做一次尝试。vanilla GLFW hints 请求 3.2 core，但桌面驱动实际返回最高可用 core 版本（本机 `4.6.0 NVIDIA 595.97`），GL4.3+ 函数指针可直接使用（`GL.getCapabilities()` 双重判定）。

**实现**（`LegacySkinningManager.Mode`：`OFF / VERTEX_SHADER / COMPUTE`，`-Deyelib.gpuSkinning.mode=vs|compute|auto`，auto = compute 优先）：

- `skin.comp`（GLSL 430，local_size_x=64）：输入 SSBO（绑定姿态 VertIn 48B/顶点：posSlot.xyz+slot 位模式 / nrm / uv）× palette SSBO（`mat4 BonePose[96]+BoneNormal[96]` 12KB，STREAM_DRAW 逐 draw 两次 subData）→ 输出 SSBO（VertOut 48B）。蒙皮数学与 VS 路径逐行同构；法线归一化守卫仍留在 VS 侧。
- `ComputeSkinnedGeometry`：输入 SSBO STATIC + 输出 SSBO STREAM + 直通绘制 VAO（属性 0/1/2 = Position/UV0/Normal，stride 48）。多实体共享同一几何的输出缓冲——flush 内 draw 渲染线程严格串行且 GL 命令保序，无竞争。
- 法线在打包期做 byte 量化（`(byte)(clamp(x)*127)/127f`），与 VS 路径 BufferBuilder.normal() 上传精度逐位一致。
- 直通 vsh（`entity_skinned_compute*` 4 变体 × 2 版本）= 蒙皮 vsh 去掉 BonePose/BoneNormal/BoneIndex；FS 仍复用 vanilla。属性位置由 vanilla ShaderInstance 按 VertexFormat 元素顺序链接前绑定（`glBindAttribLocation(programId, l++, name)`，源码实证）——`COMPUTE_FORMAT` 声明顺序即契约。
- 绘制序列照抄 vanilla `VertexBuffer._drawWithShader`（1.20.1 手动逐字段，1.21.1 调 `setDefaultUniforms`）+ `glDrawArrays(TRIANGLES)`（展开三角形无索引缓冲）。dispatch 后 `glMemoryBarrier(SHADER_STORAGE | VERTEX_ATTRIB_ARRAY)`。
- partVisibility、overlay/light/tint 逐实体 uniform 与 VS 路径完全同一份会话数据，语义不变。

**健壮性（实证驱动）**：着色器注册/编译失败逐级降级 COMPUTE → VS → OFF，**绝不向 RegisterShadersEvent 事件总线抛异常**——2026-08-27 实证：抛异常中断事件会让 vanilla 着色器全部缺失，LevelRenderer NPE 必崩。

**正确性验证**：1.20.1 三路径（compute/vs/cpu）同场景截图目检一致——A&S 牛/蜘蛛（发光红眼）/僵尸/史莱姆模型完整无畸变。单测 1.20.1/1.21.1 全绿，两版本编译绿。

**Benchmark**（1.20.1，slime-only n96/n384，RTX 4070 Laptop，15s warmup + 30s measure，fresh JVM，每模式单次）：

| 场景 | CPU | VS 蒙皮 | compute 蒙皮 |
|---|---:|---:|---:|
| fbo n96 | 127.0 | 128.7 | 127.3 |
| fbo n384 | 65.1 | 66.1 | 65.2 |
| world n96 | 82.8 | 83.4 | 83.9 |
| world n384 | 29.9 | 30.3 | 30.6 |

> **⚠ 已废弃（2026-08-28）**：同上——覆盖率空洞使三路径实际同为 CPU 蒙皮，本节基准表不构成 GPU 蒙皮的性能证据；但"瓶颈在提交路径而非蒙皮数学"的结构性诊断被后续工作证实并解决（合批删除逐实体 draw 后收益兑现）。

## 跨实体合批与覆盖率修复（2026-08-28，含结论勘误）

### 覆盖率空洞（此前所有 ≤26.1 C1 基准无效的根因）

`needsCustomRenderType=false` 的材质经 `RenderPassAdapter` 映射为 **vanilla RenderType**
（entitySolid/entityCutout/entityTranslucent/entityTranslucentEmissive），蒙皮变体登记只在
`BrRenderTypeFactory.custom()` 内——默认材质从未登记，`createSession` 恒返回 null 回退 CPU。
实证：COMPUTE 模式 + A&S 实体渲染中反射读 `VARIANTS=0 / GEOMETRIES=0`。
此前 VS/compute 的 ON/OFF"性能中性"结论测的都是 CPU 对 CPU，**不构成 GPU 蒙皮的性能证据**。

修复（d6ad17c8）：`RenderPassAdapter` <26.1 分支为 5 种 vanilla entity RenderType 登记变体
（SOLID→solid、ALPHA_TEST→cutout、TRANSLUCENT/ADDITIVE→translucent、TRANSLUCENT_EMISSIVE→emissive），
与 vanilla shader 语义一一对应。修复后反射实证 VARIANTS/GEOMETRIES >0，截图渲染正确。

### 跨实体合批 compute（P2.5 落地，cf6258fa + 19b84534）

- `skin_batch.comp`（GLSL 430）：2D dispatch（x=顶点，y=子批内实体），meta SSBO（64B/实体）携带
  逐实体 palette 基址/输出顶点基址/tint/light/overlay；palette 按实体 slotCount 紧凑打包。
- 输出顶点 80B（pos/nrm/uv/light/overlay/tint），直通 vsh 把原逐实体 uniform 改为逐顶点属性。
- `BatchSkinningDispatcher`：帧内会话收集 → 按 routing RenderType 分组（首见序/提交序）→
  单 drain 完成 palette/meta 上传（绝对下标批量拷贝）+ 每几何子批一次 dispatch + 每组一次 draw。
- 窗口语义：level 实体相（Forge/NeoForge `RenderLevelStageEvent` AFTER_SKY→AFTER_ENTITIES）内延迟合批；
  窗口外（GUI/预览/FBO 手动渲染）submit 即 drain，等价逐实体旧行为。FBO benchmark 显式开窗。
- 关键不变量：RenderSystem MV 在实体相为常量（实体变换在 palette 根姿态），合批 draw 取 drain 时 MV 同值。
- 旧逐实体 compute 路径整体删除（干净切换）；VS 路径不变，作为 GL<4.6 回退。

### 配套 molang 求值链优化（c95e1277，JFR 驱动）

JFR（fbo n384 渲染线程）实证：molang 求值链 ~53%、动画采样 ~17%、蒙皮提交 ~0.1%（合批后）。
三处修复：①`clearTempVariables` 空集短路（CHM 遍历清理 ~13%）；②编译表达式去
bindTo+dropArguments 的 MethodHandle 包装（Invokers.checkCustomized ~9% + evaluate ~10%）；
③RenderData scope 改单线程变体（4 个并发结构退化 HashMap/HashSet）。

### Benchmark（1.20.1，slime n96/n384，RTX 4070 Laptop，15s warmup + 30s measure，fresh JVM）

同一代码基线（含 molang 修复）下 GPU 蒙皮 OFF（真 CPU 基线）vs 合批 compute（真生效）：

| 场景 | CPU（molang 修复后） | 合批 compute | 提升 |
|---|---:|---:|---:|
| fbo n96 | 157.2 | 191.3–203.2 | **+22–29%** |
| fbo n384 | 72.7 | 110.4–116.9 | **+52–61%** |
| world n96 | 95.2 | 104.7–109.2 | **+10–15%** |
| world n384 | 33.4 | 39.0–40.4 | **+17–21%** |

（合批 compute 列为多次 run 区间；fbo n96 小帧场景 run 间方差 ±6%。）

相对最初未修复基线（127.0/65.1/82.8/29.9），当前总收益：fbo n384 **+75%**，world n384 **+33%**。

### 正确性验证（真生效状态）

- 1.20.1/1.21.1 运行时截图：史莱姆半透明（外层/内核/面部层重叠正确）、蜘蛛发光红眼、牛/僵尸正常；
  反射实证 VARIANTS/GEOMETRIES>0 且几何为预期类型（compute=ComputeSkinnedGeometry，vs=LegacySkinnedGeometry）。
- 1.20.1 单测全绿；两版本编译绿。
- 截图：work/c1-batch/{batch_it4_scene2.png（1.20.1 合批）、batch_121_engaged.png（1.21.1 合批）、vs_120_engaged.png（1.20.1 VS）}。

### 已知余项

- FBO benchmark 的 detached 实体每帧重走 setupClientEntity（isStale 恒真）——benchmark 环境假象，
  世界路径实体无此行为；FBO 场景的 molang 份额被其放大，真实世界收益以上表 world 行为准。
- TickStage 对全部 level 实体（含视锥外）逐帧 tick——保持 BE 动画连续性语义，未动。
- world 场景在 mob 密集时渲染线程含 vanilla 客户端实体 tick（pushEntities/getEntities AABB 扫描）
  背景成本，与蒙皮无关。
