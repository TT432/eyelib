# 多 Pack/Addon .material 加载行为

完整的加载流程与分析，包括与 Bedrock 官方合并规则的差异。

## 加载流程（两阶段）

### 阶段1：资源重载 → BrMaterialLoader

```
MC ResourceManager 扫描所有资源包
  → 收集 eyelib/materials/*.material（所有 pack 的所有文件）
  → BrMaterialLoader.apply()
      → 逐文件解析 BrMaterial.CODEC
      → 全部 flatten 进一个 LinkedHashMap
      → MaterialManager.INSTANCE.replaceAll(flattened)
```

关键行为：

- **同路径文件**（如两个 pack 都有 `eyelib/materials/entity.material`）→ MC 层按 pack 优先级只保留高优先级版本。低优先级文件内容不会进入 `apply()`。
- **不同文件名**（`pack_a.material` / `pack_b.material`）→ 两者都被收集，flatten 到同一 map。
- **同 key 冲突** → `LinkedHashMap.forEach(flattened::put)`，后迭代到的覆盖前。迭代顺序取决于 `LoaderParsingOps.parseBySourceKey` 中 `Map.forEach` 遍历的 `Map<ResourceLocation, JsonElement>` 顺序（MC 内部是 pack 优先级排序的 LinkedHashMap）。
- **`replaceAll` = 全量替换**：清空 MaterialManager，写入扁平化所有条目。此时 MaterialManager 里只有 `.material` 文件的内容（包括 eyelib 自己的 `vanilla.material`）。

### 阶段2：Addon 加载 → BedrockAddonRuntimeBridge

**2026-08-16 起：.mcpack/.mcaddon 走原版包管理。**
`BedrockAddonPackFinder`（AddPackFindersEvent）把 resourcepacks/ 下每个 .mcpack/.mcaddon
注册为可选 vanilla 客户端资源包（`file/<文件名>`，required=false，Position.TOP，
选择持久化于 options.txt）；`BedrockAddonAutoLoader` 在资源重载时枚举
`ResourceManager.listPacks()` 中选中的 `BedrockPackResources`，按包优先级（底→顶）
`BedrockAddon.merge` 合并为单一视图后一次性桥接：

```
PackRepository（选中、排序）
  → BedrockAddonAutoLoader（listPacks 过滤 BedrockPackResources）
  → BedrockAddonLoader.load() 解析每个选中 addon
  → BedrockAddon.merge（后加载=高优先级覆盖，复用 fromPacks 合并规则）
  → BedrockAddonRuntimeBridge.replaceFromResourcePack()
      → ClientEntityManager replaceAll（合并视图全量替换）
      → Model/Attachable/Material/RenderControllerManager 阴影叠加
        （记录每键原值；包禁用/移除时恢复基线或删除）
      → MaterialManager.INSTANCE::put 语义由阴影叠加实现
```

关键行为：

- **未选中不加载**：默认未启用，需在资源包界面启用一次（原版语义）。
- **禁用即卸载**：阴影叠加恢复基线；纹理先 clear 再传；动画/音效 staging 单槽替换。
- **多包冲突**：按 PackRepository 选中顺序（界面上下拖动），高优先级包覆盖同 key 条目。

### 包图标与设置界面（2026-08-16 第二批）

- **图标**：`BedrockPackResources.getRootResource("pack.png")` 桥接到包内
  `pack_icon.png`（.mcaddon 回落 `resource_pack/pack_icon.png`），vanilla
  `PackSelectionScreen.loadPackIcon` 自动注册为动态纹理；无图标回落 unknown_pack.png。
- **设置按钮**：`PackEntryMixin`（client mixin，仅 <26.1）在资源包列表条目右侧
  绘制齿轮（`assets/eyelib/textures/gui/pack_settings_gear.png`），点击打开
  `BedrockPackSettingsScreen`（bridge/client/gui/adapter/）。仅当包声明了
  subpacks 或非 label 的 settings 时显示。
- **设置模型**（domain，importer/addon/）：
  - `BedrockPackSetting`：manifest format_version 3 settings 的类型化解析
    （label/toggle/slider/dropdown，官方文档《Create a Pack With Custom Settings》）。
  - `BedrockPackSettingsStore`：用户选择持久化于
    `config/eyelib/bedrock-pack-settings.json`，键 = 资源包文件名
    （与 vanilla pack id `file/<文件名>` 同源）。
  - `BedrockPackSettingsService`：重载时注册启用包的设置目录（底→顶），
    molang 查询按顶→底解析第一个声明该设置名的包，用户选择优先于默认值。
    已知近似：Bedrock 设置按包隔离，这里按设置名跨包解析（名字带命名空间，冲突概率低）。
  - `BedrockPackSettingsCatalog` / `BedrockLangFile`：UI 用的目录解析
    （manifest + subpacks + texts/en_US.lang 本地化表，行内 `\t#` 注释）。
- **subpack 选择**：设置界面的 Subpack 循环按钮写入 store；
  `BedrockAddonLoader.load(Path, subpackOverride)` 优先用户选择，未知文件夹
  回落自动规则（最高 memoryPerformanceTier、同 tier 取最后）并报
  `SUBPACK_OVERRIDE_UNKNOWN` 警告。subpack 改变加载内容，关闭界面时触发资源重载；
  toggle/slider/dropdown 由 molang 即时读取，无需重载。
- **设置控件形态**（2026-08-16 第三批）：slider → `AbstractSliderButton`
  （step 网格）；dropdown → 自绘 `DropdownWidget`（收起态显示当前值 + ▼，
  点击展开屏幕级弹出层——列表剪刀域画不下，渲染/命中都在
  `BedrockPackSettingsScreen` 层做：底部放不下向上展开、超 7 条滚轮滚动、
  ESC/点外收层）；toggle → CycleButton 开关（vanilla 惯例）。
- **molang 查询**（client/molang/MolangQuery，官方语义）：
  `query.is_pack_setting_enabled(name)`（toggle）、
  `query.is_pack_setting_selected(name, selection)`（dropdown 字符串比较）、
  `query.get_pack_setting(name)`（slider 数值）。
- **26.1 缺口**：PackEntryMixin 与 BedrockPackSettingsScreen 以 `//? if <26.1`
  整体排除（26.1 输入/渲染体系重写，MouseButtonEvent/无经典 Screen.mouseClicked），
  molang 查询与加载侧全版本生效。

## 与 Bedrock 官方合并规则的差异

### Bedrock 多文件合并规则

来源：`E:\_____基岩版文档\bedrock-wiki\docs\documentation\material-config-description.md`

Bedrock 通过 `fancy.json`/`sad.json`/`common.json` 等质量配置文件显式列出要加载的 .material 文件列表。当同一材质名称出现在多个文件中时：

1. **普通字段**：后加载覆盖前。
2. `defines`、`states`、`samplerStates` 支持 `+`（添加）和 `-`（删除）前缀。
3. **合并顺序**：全部覆盖操作 → 全部添加操作 → 全部删除操作。
4. **删除终极优先级**：任何文件声明了 `-defines: [MACRO_3]`，合成后的材质必定没有 MACRO_3，无论其他文件怎么 add。

### eyelib 对照

| 方面 | Bedrock | Eyelib |
|------|---------|--------|
| 文件列表 | 质量配置 JSON 显式列出 | MC 资源重载 flat scan |
| 同 key 普通字段 | 后加载覆盖前 | LinkedHashMap 迭代最后写入胜 |
| `+defines`/`-defines` | 跨文件合并操作符：覆盖→添加→删除，删除最终优先 | CODEC 解析为 `defines.add()`/`defines.sub()`，仅在单条**继承链**内由 `ModifyAble.toList()` 解析，**无跨文件合并逻辑** |
| `+states`/`-states` | 同上 | 同上 |
| 同路径文件的处理 | 质量配置决定加载顺序 | MC pack 优先级决定，低优先级文件直接被丢弃 |

**核心差距**：Bedrock 的 `-defines` 具有跨文件终极优先级。eyelib 做不到——eyelib 的 `+`/`-` 只在 `ModifyAble` 继承链（parent→child）内生效，多条目同名冲突简单用 `LinkedHashMap.put()` 覆盖。

## 场景速查

| 场景 | 行为 |
|------|------|
| Pack A 和 Pack B 都有 `entity.material`（同名路径） | MC 按优先级只取一个，另一个被丢弃 |
| Pack A 有 `a.material`，Pack B 有 `b.material`，都定义了 `"foo:bar"` | 两者都被加载，`foo:bar` = 后迭代到的那个 |
| .mcpack addon 的材质 | 在 `.material` 文件之后加载，叠加覆盖同名 key（阴影语义，禁用即恢复） |
| 两个 .mcpack 都有同名材质条目 | 资源包界面排序靠上（高优先级）的覆盖 |
| `+defines` 跨两个 pack 文件 | eyelib 不合并——每个 entry 独立 CODEC 解析，后迭代到的完全覆盖前一个 |
