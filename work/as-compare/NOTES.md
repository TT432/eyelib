# A&S 复刻对比工作笔记（2026-07-19 状态快照）

目标：以基岩版（同一份 Actions-and-Stuff-1.10-v2.mcpack，SP2 Full 档）为基准，逐生物修正 eyelib 实现到正确复刻。

## 一、对比装置与工具链（全部可用）

### 摄影棚（双侧同坐标）
- 外壳：`fill 4 200 4 12 208 12 quartz_block hollow`（空中，遮阳光防亡灵自燃 + 均匀背景）
- 光源：内顶 `fill 5 207 5 11 207 11 light`（JE）/ `light_block_15`（BE）
- 相机：观察者 (8.5, 201, 5.5) yaw=0 pitch=8；实体排 z=11.5，面向相机（JE `Rotation:[180f,0f]`，BE `tp @e[...] x y z facing 8.5 201 5.5`）
- JE FOV 必须设 60（BE 默认 60，JE 默认 70）
- 规则：双侧 doMobSpawning=false、doDaylightCycle=false、time set 6000、weather clear
- JE /summon 不给默认装备（BE 会给，如骷髅弓）→ 对比前需手动配装

### JE 侧（eyelib-debug MCP，端口 25999）
- 客户端工作目录 = `versions/1.20.1/run/`（不是根 run/！）
- 截图：`Screenshot.grab(new File("E:/_ideaProjects/qylEyelib/work/as-compare/shots"), "name.png", minecraft.getMainRenderTarget(), c->{})` → 输出在 `<dir>/screenshots/name.png`，抓的是上一帧，改状态后需等 1 帧
- /eval 每次重启后需重设：`fov 60`、`pauseOnLostFocus=false`（否则失焦自动暂停出菜单）、`hideGui=true`
- 运行时探针套路：`RenderData.getComponent(e)` → `cap.getScope()`（cache 字段反射列 molang 变量）、`cap.getModelComponents()`、`scope.set("variable.xxx", v)` 钉变体、animate 表逐条换 `MolangValue("0")` 归零二分定位
- System.out 不进 latest.log，插桩日志用 slf4j

### BE 侧（基岩版，用户客户端）
- 驱动：`work/as-compare/ws_server.py`（hub 进程 `bedrock-ws`，WS:19199 / HTTP:19200，`POST /cmd` 同步返回执行 JSON，`GET /status`）。用户在聊天栏 `/wsserver localhost:19199` 连入；世界重进需重连
- 要点：WS 服务必须双栈监听（localhost→::1 优先）；hub `send` 对 pty:false 进程 stdin 不可达（必须用 HTTP 口）；UWP 环回豁免已存在
- 截图：`python work/as-compare/capture_bedrock.py out.png`（WGC 后台截被遮挡窗口，最小化不行）
- 定身：`effect @e[...] slowness 99999 255 true`（BE 无 NoAI）；`hud @s hide all` 不遮第一人称手；长时间无操作会弹 AFK 遮罩需用户按键

### A&S 包结构
- `__brarchive/*.brarchive`：magic 0x267052A0B125277D，256B/entry 名记录 + 串接 JSON 文档，文件名哈希混淆（Marketplace 包）
- entity.brarchive → 113 种实体注册进 ClientEntityManager（玩家未实现）
- 子包：SP0 Custom≈空；SP1 Vanilla（原版贴图+自定义动画）；SP2 Full（全量重贴图）——对比基准=SP2（用户已切换）
- A&S 大量变体靠 `Math.random*`（initialize 一次抽样）：对比的是"变体系统与分布"而非个体一一对应
- molang 743 处 `this`；大量 `texture_meshes`（体素化平面）；包设置 query（stghmf/iocufj 等）

## 二、已完成修复（按提交顺序）

| commit | 内容 | 验证 |
|---|---|---|
| 4b3f9786 | `q.is_on_ground` 静止实体恒 false → EntityPortAdapter.isOnGround 加包围盒下移 1mm 碰撞兜底 | 僵尸 q.is_on_ground=1.0 ✓ |
| da9f49b1 | `is_name_any` 子串匹配→区分大小写精确匹配（'Chicken' 误中 'chick' 致成年鸡变雏鸡）；补 `q.is_in_ui` 恒 0 | 成年鸡=白色母鸡 ✓ |
| 待提交 | 见下"未提交工作" | |

## 三、未提交工作（14 文件，已构建验证，待 smoke 后提交）

1. **molang `this` 实现**（ModelRuntimeData/BrAnimator/BrClipExecutor/BrBoneKeyFrame/BrBoneAnimation/BrBoneAnimationSampler/MolangValue3/EntityRenderOrchestrator/AttachableItemRenderSetup/ItemInHandRendererMixin/ItemRendererMixin）
   - 官方 syntax-guide：`this` = 表达式最终写入目标的当前值（bind + 已累积动画，逐轴）
   - ModelRuntimeData 挂 bindBones 表；BrClipExecutor 按通道换算 this 值；全部 lerp/catmullrom 线程化逐轴 scope.set("this")
   - 验证：`-this(=5)=-5.0` ✓
2. **`q.modified_move_speed` 零除 NaN 修复**（MolangBuiltInQuery）：基础移速属性=0 时 0/0=NaN 沿动画链扩散致上半身消失；speed<=1e-6 返回 0。验证：瞄准骷髅骨骼数值恢复 ✓
3. **texture_meshes 体素化**（TwoSideModelBakeInfo）：官方文档 texture_mesh=贴图像素转体素（texel→1px 深 voxel）。getBakeInfo 缓存 TexImage 像素副本，bake 时逐不透明 texel 生 1×1×1 六面体素。弓从"完全不渲染"→"形状渲染出来" ✓（颜色/姿态仍待校准）
4. **`q.is_attached` 实现**（MolangBuiltInQuery.ATTACHED HostRole + AttachableItemRenderSetup.getOrPrepare markAttached）：修复 A&S 弓第二层误选不透明黑 1×1（cdx）被 merge 成全黑
5. **1×1 纹理放行条件**（TextureManagerMixin）：vanilla 能解析才放行（保留 c535b789 史莱姆修复），否则注册 1×1 DynamicTexture——修复 bge.png 透明占位图触发 FileNotFoundException 中断整个 attachable 渲染

## 四、待办问题（按优先级）

1. **弓姿态/颜色未校准**：体素化后形状出现但角度/颜色不对（当前呈大绿色面片，疑似 outline/overlay 组件渲染错位）；merge（complex:）与多 pass 语义需按 Mojang 文档定夺（RC `textures` 数组=多图层，文档原话 "what textures to use on and in which layer"）
2. **内存泄漏**（feedback fb_mrqjf0zjwp2l）：JE 帧数随时间下降，已实证 OutOfMemoryError 崩溃（crash-2026-07-19_06.36.50-client.txt）。线索：AttachableItemRenderSetup.CACHE 在客户端 ItemStack 实例更换时重复建 rd（观测到 5→10 组件重复）；collectBindBones 每帧每实体分配新 map
3. **未实现 query/功能**（A&S 用到）：`rotation_to_camera`、`math.ease_in_out_back`、`query.any`、`entity_biome_has_any_identifier`、`q.is_pack_setting_selected/enabled`（现为恒 false 桩，A&S 包设置全部走默认分支——注意与 BE 非默认设置用户产生差异）、`relative_to`（骨骼动画相对实体）、`c.owning_entity->`、`q.main_hand_item_use_duration`
4. **服务端行为侧** query 注册表与渲染侧分离，服务端缺 is_item_name_any 等（日志有 MolangRuntimeSupport 告警）；VanillaBehaviorEntityLoader 部分事件解析失败（has_component/has_biome_tag）
5. **JE 蜘蛛消失过一次**（未复现）
6. **子包 key 错位**（docs/gap-analysis/brarchive-subpack-key-mismatch.md）：遇症状再处理
7. **色调差**：JE 画面整体比 BE 亮（引擎级，暂不归档为 bug）

## 五、实体进度

| 实体 | 状态 | 备注 |
|---|---|---|
| zombie | ✅ parity（含变体系统） | 举臂=随机变体非 bug |
| chicken | ✅ 修复后 parity | is_name_any 修复 |
| pig/cow/creeper | ✅ 外观 parity | |
| sheep | ≈ | 白/米黄=引擎色调差 |
| skeleton | ⚠️ 部分 | 弓可渲染但姿态/颜色未校准（见待办 1） |
| spider | ❓ | 消失过一次未复现 |
| 其余 ~105 种 | 未对比 | |

## 六、方法论沉淀（重要）

1. **先排除随机性再判 bug**（僵尸举臂=随机变体教训）
2. **分层看门狗定位 NaN/错误生产者**：BrAnimator 顶层 + BrControllerExecutor blend 层各插桩一层，比猜快
3. **归因套路**：运行时 animate 表逐条清零 → 二分定位 → 回查 brarchive 数据 → 对照 Mojang 文档（oracle 优先级：creator 文档 > mcpack 数据 > bedrock-wiki > 内部 ADR）
4. **BE 侧对照技巧**：变异体用"一排 N 只看分布"；状态依赖（瞄准）用生存+抗性5+单只（防互射/被秒）
