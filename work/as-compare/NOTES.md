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
| 42f07bd5 | molang `this` 实现（官方 syntax-guide：bind+已累积动画逐轴）；`q.modified_move_speed` 零除 NaN 修复；texture_meshes 体素化初版；`q.is_attached`；1×1 纹理放行 | 瞄准骷髅骨骼数值恢复 ✓；弓形状出现 ✓ |
| ad99accb | NaN 语义：比较运算含 NaN 除 != 外恒 false（fcmpl/fcmpg 分工，同 javac）；`main_hand_item_use_duration` 零除 NaN 返回 0 | MolangSpecTest NaN 用例 ✓ |
| d6fbe0a7 | render_controller 内联条件下沉 BrClientEntity codec（attachables brarchive 分支漏 normalize 致 5 RC 无条件全渲染）；删 normalizeRenderControllers | 弓组件 5→1(主RC) ✓ |
| 8407d893 | RC textures 数组改多 pass 分层（官方/wiki：图层顺序渲染，删 TextureLayerMerger 合并）；texture_mesh 体素化贴图按实体纹理表短名解析（dhw 而非图层贴图）；手持附着补骨骼 pivot 平移（收集姿态不含 pivot，曾致弓沉到实体原点）；texture_mesh 转换对齐 Blockbench 实测 | 弓挂手侧、形状/姿态正确 ✓；clientsmoke 9/9 ✓ |
| 1c45073d | RC 条件逐 tick 动态重估（BE 逐帧语义），实体+attachable 两侧 | 骷髅拉弓箭层 2→3、射击后回落 ✓ |
| 11288a7d | 单采样材质多图层 RC 只渲染第 0 层（BE 语义：分层需 multitexture/masked 多采样材质） | 弓由全绿→棕色，与 BE 截图一致 ✓ |

## 四、待办问题（按优先级）

1. **附魔弓发光层未验证**：zxhwjj/aegbne（v.is_enchanted）的调色板发光层用单采样加法混合材质（dgvqwe One/One），现按单采样规则只渲染第 0 层；BE 附魔弓实际表现需附魔场景对照（优先级低）
2. **内存泄漏**（feedback fb_mrqjf0zjwp2l）：JE 帧数随时间下降，已实证 OutOfMemoryError 崩溃（crash-2026-07-19_06.36.50-client.txt）。线索：AttachableItemRenderSetup.CACHE 在客户端 ItemStack 实例更换时重复建 rd（观测到 5→10 组件重复）；collectBindBones 每帧每实体分配新 map
3. **未实现 query/功能**（A&S 用到）：`rotation_to_camera`、`math.ease_in_out_back`、`query.any`、`entity_biome_has_any_identifier`、`q.is_pack_setting_selected/enabled`（现为恒 false 桩，A&S 包设置全部走默认分支——注意与 BE 非默认设置用户产生差异）、`relative_to`（骨骼动画相对实体）、`c.owning_entity->`、`q.main_hand_item_use_duration`
4. **服务端行为侧** query 注册表与渲染侧分离，服务端缺 is_item_name_any 等（日志有 MolangRuntimeSupport 告警）；VanillaBehaviorEntityLoader 部分事件解析失败（has_component/has_biome_tag）
5. **JE 蜘蛛消失过一次**（未复现）
6. **子包 key 错位**（docs/gap-analysis/brarchive-subpack-key-mismatch.md）：遇症状再处理
7. **色调差**：JE 画面整体比 BE 亮（引擎级，暂不归档为 bug）
8. ~~BE 并排对照~~（2026-07-19 已连 BE 完成）：`bow_je_vs_be.png` 并排实证——弓=棕色木弓、挂右手侧、形状/弦纹一致；残余差异=持弓倾斜角（BE 两只骷髅倾斜角本身不同，判定为 A&S idle 摆臂相位差，非 bug）

## 五、实体进度

| 实体 | 状态 | 备注 |
|---|---|---|
| zombie | ✅ parity（含变体系统） | 举臂=随机变体非 bug |
| chicken | ✅ 修复后 parity | is_name_any 修复 |
| pig/cow/creeper | ✅ 外观 parity | |
| sheep | ≈ | 白/米黄=引擎色调差 |
| skeleton | ✅ parity | 弓=棕色（BE 截图实证）、挂手侧垂下、拉弓出箭、射击回落、持弓臂姿（varargs 修复后） |
| spider | ✅ 非 bug | "消失"两起均破案：JE=summon 整数坐标吸附方块中心致 1.4 宽蜘蛛卡墙窒息（LivingDeathEvent src=inWall 实证）；BE=狼咬死（隔离实验实证） |
| villager | ✅ 修复后 parity | 修复链：villager_v2 别名、clamp 保留全透明、q.any/q.skin_id、varargs off-by-one；平原型=bsc 黄脸绿眼棕袍 ✓ |
| 其余 ~105 种 | 未对比 | |

## 六、方法论沉淀（重要）

1. **先排除随机性再判 bug**（僵尸举臂=随机变体教训）
2. **分层看门狗定位 NaN/错误生产者**：BrAnimator 顶层 + BrControllerExecutor blend 层各插桩一层，比猜快
3. **归因套路**：运行时 animate 表逐条清零 → 二分定位 → 回查 brarchive 数据 → 对照 Mojang 文档（oracle 优先级：creator 文档 > mcpack 数据 > bedrock-wiki > 内部 ADR）
4. **BE 侧对照技巧**：变异体用"一排 N 只看分布"；状态依赖（瞄准）用生存+抗性5+单只（防互射/被秒）
5. **Blockbench 活 codec 作权威 oracle**（texture_mesh 转换定案）：web.blockbench.net 控制台直接 `newProject(Formats.bedrock); Codecs.bedrock.load(json, file)`，读内部字段 = BE JSON→内部空间映射真值。实测映射：position=(-x,-y,z)、rotation=(-rx,-ry,rz)、local_pivot=(x,y,-z)、scale 不变；网格片元布局：图右→-x、图下→+z、厚度-y；Blockbench 内部空间 == eyelib JE 几何空间（骨 pivot.x 取反同约定）
6. **收集的骨骼姿态不含 pivot 平移**：applyBoneTranslate 是 T(pos)×T(pivot)×R×S×T(-pivot)，做附着点（attachable/locator）必须自行补 T(pivot)，否则附着物沉到实体原点
7. **JE 传送必须用服务端命令**：客户端 `player.teleportTo` 会被服务端橡皮筋回弹（getOrPrepare 探针看到的位置是旧的）
8. **brarchive 解码**：8B magic(0x267052A0B125277D) + 4B count + 4B version + count×256B 记录（名+0xFC 处长度）+ 串接 JSON；`__brarchive/models/entity.brarchive`=几何，`materials.brarchive` 无 payload（.material 是普通 zip 条目）
9. **BE 分层语义定案**：RC textures 数组=多图层绑定采样器，材质决定是否合成——multitexture/masked 族（entity_multitexture_*、villager_v2_masked、MASKED_MULTITEXTURE define）才分层；单采样材质只渲染第 0 层（wiki 分层教程+A&S 弓实证：调色板层不生效、弓=棕色）
10. **无头截 BE 窗口**：`capture_bedrock.py`（WGC）不依赖 WS 连接，AFK 遮罩下也能拿到实体外观证据——WS 断了先截图再谈
11. **宽体实体召唤要留墙距**：/summon 整数坐标吸附方块中心，蜘蛛(1.4宽)在 8×8 棚里贴墙放会卡墙窒息（约 1s 死亡，LivingDeathEvent 可查 src=inWall）
12. **实体离奇死亡三板斧**：LivingDeathEvent（src）→ 服务端 getSingleplayerServer 查存活 → 隔离实验（逐个排除杀手）；BE 侧 querytarget 追踪位置漂移
13. **molang 未实现 query 返回 MolangNull 而非 0**：`null+number=null` → 三元/公式静默产 null → 数组索引回退 0，变体选错但不报错；排查变体问题先查链上每个 query 是否为 null
14. **BE 群系决定实体变体**：村民脸=群系变体纹理（taiga=绿眼 bsh）；双侧对比前必须对齐群系/实体类型，否则把变体差异误判为渲染 bug
