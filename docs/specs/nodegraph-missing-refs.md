# 缺失引用检查与粒子/音效预览

> 状态：已实现（2026-08-09）。实机验证记录见 §6。

## 1. 需求

1. ref.* 节点引用的资产不存在时是 ERROR（不是静默或 warning）；
2. 编辑器里缺失引用的节点要有红色高亮；
3. ref.particle / ref.sound 节点带播放键预览。

## 2. MISSING_REFERENCE（检查 24）

- 域接缝：`RefExistenceChecker`（nodegraph 包）——`exists(refNodeTypeId, identifier)`；
  域不依赖客户端注册表，`validate(library)` 默认全放行。
- `GraphValidator.validate(library, checker)` 逐图逐 ref 节点检查：
  ref.geometry/ref.texture/ref.material/ref.animation/ref.ac/ref.particle/ref.sound/ref.rc
  的引用值选项（identifier/path/material）；**选项无实例值（example 占位）跳过**。
- 客户端实现 `MissingRefCheck`（client.nodegraph）：各通道注册表判定
  （ModelManager / NodeAssetPreview.hasUsableTexture / MaterialManager /
  AnimationAssetRegistry schema 文档 / ParticleDefinitionRegistry /
  SoundAssetRegistry / RenderControllerManager），实体声明表值集兜底。
- 接线：Ldlib2NodegraphEditor（打开/保存）与 NodegraphBuildService（构建）。

## 3. 红色高亮（MissingRefOverlay）

- 与 BadgeOverlay 同通道：GraphView.canvas 最上层、屏幕空间、不拦截鼠标。
- 每帧枚举当前图节点（ICustomNodeModel → EvmNodeBase）的 ref 选项值，
  MissingRefCheck 判定不存在即在节点外缘画红框（500ms 记忆）；
  占位 ref（选项值 = NodeOptionDef 默认值）不报；可视区外裁剪。
- 挂载：Ldlib2Workbench（BadgeOverlay 同点）。

## 4. 播放键预览

- ref.particle / ref.sound 节点启用 NodePreviewModel 面板：棋盘格底 +
  「播放」（资产缺失画「未找到」）；左键点击播放（事件终止传播）。
- 粒子：ParticlePort.getSpawnAdapter().spawn 于玩家眼前 3m（每次新 UUID spawnId）。
- 音效（仅 <26.1）：AddonSoundPack（内存 PackResources）把 SoundAssetRegistry 的
  音频字节与 sound_definitions 以资源包形态暴露：
  - `assets/eyelibaddon/sounds/**` 服务 .ogg 字节（.fsb 不支持，定义在包外 vanilla 的
    条目（如 mob.strider.*）不收录——408 定义中 74 条因此正确丢弃，334 事件入册）；
  - `assets/eyelibaddon/sounds.json` 现场合成：事件键 = `toEventKey(id)`
    （"ns:path" → "ns/path"——MC 事件键 = 当前命名空间+键原文，键含 ':' 判非法；
    资源管理器命名空间集在资源重载时固化，故事件统一落 eyelibaddon 稳定命名空间）；
    category 映射 MC 合法集（非法归 neutral）；volume/pitch/stream 透传；
  - 包注册：mod 总线 AddPackFindersEvent（required pack）；pack.mcmeta 经
    getMetadataSection 提供（缺则 readMetaAndCreate 返回 null → 启动 NPE，实证）；
  - addon 替换后 AddonSoundPort.triggerSoundReload：走**监听式** reload
    （无参 reload() 只重启 SoundEngine 不重读 sounds.json，字节码实证）；
  - 播放：variable-range SoundEvent + SimpleSoundInstance.forUI（免 SoundEvent 注册）。
- 运行时桥接：BedrockAddonLoader 的 loadBrarchive 补 `sounds.` 分支
  （sound_definitions.json 内嵌 brarchive 的解析此前不存在，全部落 unmanaged）。
- 26.1 降级：AddonSoundPort 两方法 no-op；预览面板画「(26.1 不支持播放预览)」。

## 5. 架构约束（ADR-0016/0018 合规）

- 具体实现收 `bridge.client.sound.adapter`（AddonSoundBridge/AddonSoundPack）；
  Application 仅经 `bridge.client.sound.AddonSoundPort`（接口）访问（机制 E）。
- 音效资产暂存 `importer.addon.SoundAssetRegistry`（domain 侧）——bridge 不得依赖
  client.*（Application），实证 ArchUnit aclMustNotDependOnApplication 拦截过
  client.registry 方案。

## 6. 验证

- 单测：GraphValidatorTest MISSING_REFERENCE ×3（按节点报错/占位与存在跳过/域默认放行）。
- 实机（1.20.1）：
  - 验证器：含 bogus ref 的库打开，DiagnosticsCenter 报 MISSING_REFERENCE（日志实证）；
  - 缺失判定链：bogus sound/particle missing=true、valid particle missing=false、
    节点元素解析 el=true（逐项 eval 实证）；
  - 音效：334 事件注册、event→Sound→path→resource 全链解析、播放无 missing 告警；
  - 粒子：def/env/scope 全通、spawnEmitter 创建成功。
- 未覆盖（环境限制，非逻辑缺口）：
  - 红框像素级渲染：验证机画布 0 宽（fb_msihglejj7ru 预存），裁剪正确丢帧；
  - 发射器注册的 live 可见窗口：验证机渲染 5s+/帧（软件光栅 + fb_mslnvgddl7uy
    temp 清理风暴），submit 排水超时；管线各段已独立验证。
