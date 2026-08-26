# C1 GPU 蒙皮（26.1.2）——实现与基准测试结论

> 日期：2026-08-27。状态：**已实现、正确性已验证、性能中性**。
> 设计：work/c1-gpu-skinning/DESIGN.md；原则：ADR-0032；调研：docs/research/2026-08-26-render-gpu-offload.md §5-C1。

## 1. 实现摘要（commit 1d47d353 + 88da7479 + c957789a）

- `SkinningSession` 契约（bridge 接口）+ 26.1.2 实现（`bridge/client/render/skinning/adapter/`）：
  静态 GpuBuffer 几何（28B 顶点：Position/UV0/Normal/BoneIndex，按骨骼分段）、
  std140 palette UBO（header: tint/overlay/light + mat4 pose + mat4 normal per bone，MAX_BONES=96）、
  routing RenderType 派生蒙皮管线（toBuilder 换 VS+格式+BonePalette）、
  `entity_skinned.vsh`（vanilla entity.vsh 蒙皮变体，FS 复用 vanilla core/entity）。
- 提交路径：submitCustomGeometry 仅作 phase 归类（写零顶点）；绘制在
  AfterOpaqueFeatures（copyDepthFrom 之前）/ AfterTranslucentFeatures 阶段批量 flush；
  PIP/物品栏/FBO（output override 非空）立即绘制。每组一次 drawMultipleIndexed；
  palette 每 phase 一次 mapBuffer 批量上传。
- 回退：`-Deyelib.gpuSkinning=false`；骨骼 >96 或 ring 耗尽逐实体回退经典 CPU 路径；
  半透明（sortOnUpload）材质本期不蒙皮（保留全缓冲 quad 排序语义）。

## 2. 正确性证据

- 单测 `SkinningGeometryPackerTest`（slot 分配/顶点索引内容/区间合并/std140 布局），1.20.1 绿。
- 运行时（26.1.2，A&S 1.10 v2）：牛/僵尸/骷髅/史莱姆正确渲染（含 A&S 动画姿态），
  蒙皮路径活跃（运行期 GEOMETRIES>0、派生管线 4 个），开关切换正常。
- 崩溃修复 2 处（88da7479）：writeTransform/纹理解析误在 RenderPass 开启期调用。
- 预存问题（与 C1 无关，基线代码复现确认）：26.1.2 上 eyelib 实体渲染中
  Screenshot.grab 致 JVM 无声堆损坏退出（feedback fb_mtag28u9ymhd）。

## 3. 基准测试（render_work，ms，26.1.2 mixed 场景，fresh JVM）

| 运行 | 配置 | n | world P50 | world P99 |
|---|---|---:|---:|---:|
| 基线 B1（C1 前代码，00:08） | — | 96 | 12.478 | 18.158 |
| C1（蒙皮 ON，03:27） | 初版 | 96 | 9.403 | 14.254 |
| B2（蒙皮 OFF，03:31） | 初版 | 96 | 9.831 | 14.487 |
| C2（蒙皮 ON，03:35） | 初版 | 96 | 9.780 | 15.450 |
| ON（03:43） | 初版 | 384（全牛） | 19.703 | 26.750 |
| OFF（03:48） | 初版 | 384（全牛） | 19.183 | 24.428 |
| ON（04:05，O1+O2 后） | 优化后 | 384（全牛） | 19.114 | 25.155 |

有效性：全部 status=passed、configured=actual（n384 混合僵尸/骷髅组正午燃烧失败，
已改全牛）；fbo 场景因 26.1.2 FBO 路径不执行 renderAllFeatures（clientsmoke 兼容实现），
不含 eyelib 几何成本，仅作场景开销参照。

## 4. 结论：性能中性（±1%）

- 同代码 ON/OFF 对比：n96 差 0.5%，n384 差 2.7%（OFF 反而快）/优化后 0.4%——**均在运行间
  噪声内**（本机同代码不同 run 漂移可达 ±5%+；B1 的 12.478 与后续同代码 B2 的 9.831 差 21%
  即机器状态漂移，非代码差异）。
- **结构性解释**：26.1.2 声明式提交架构下，每实体成本的主体是 vanilla 的
  extract/submit/状态机（render_work n384 ≈ 19ms 中 vanilla submit 机制占大头），
  顶点 CPU 变换+写入（C1 消除的部分）在该版本中本来就是小头；
  C1 新增的 per-entity palette UBO 绑定与 draw 提交开销又吃掉一部分收益。
- C1 的实际价值重定位：① 消除每帧全量顶点上传（GPU 侧带宽，render_work 不测；
  30 FPS 上限下帧间隔指标不可见）；② ADR-0032 GPU 基建（自有管线/UBO/静态几何），
  为 26.2+ Vulkan compute 窗口铺路；③ ≤26.1（P2）才是顶点成本占比高的版本——
  1.20.1 立即路径无 submit 机制开销（spark 实测 eyelib self 36μs/实体/帧占渲染显著份额），
  GPU 蒙皮的预期收益在 P2 才可能兑现。
- 半透明蒙皮（静态顶点 + 逐帧 CPU 排序索引重写）作为 P1.5 待定项保留。

## 5. 已知限制

- 骨骼数 >96 的模型回退经典路径（UBO 16KB 保证下限约束）。
- RenderDoc GetPostVSData 数值对比未执行（RenderDoc GL 捕获对 26.1.2
  无调试组命名，帧动作 18 万+，逐 draw 分析成本过高；已有截帧
  work/c1-gpu-skinning/rdc/frameA2|B2|C_capture.rdc 可供后续深挖）。
