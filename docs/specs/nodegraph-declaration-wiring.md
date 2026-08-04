# 节点图：声明连线语义 + 闭包导入 + 诊断面板

状态：已实施并验证（2026-08-04，ADR-0025）
前置：nodegraph-visual-molang.md、nodegraph-shortname-elimination.md、nodegraph-eproject-variables.md

## 1. 背景与问题

1. 声明表 ref.*（geometry/texture/material/animation/ac）历史上**不连线**：组装器从主图可达全部图
   全局扫描收集。画布上看不出 ref 属于哪个实体，用户无法判断"接没接上"。
2. UI 导入 ClientEntity 只导入实体本身；它引用的 RenderController / AnimationController
   不会跟随导入，用户"看不到 RenderController"。
3. 诊断（导入/构建/验证的 error/warning）刷聊天栏，噪声大且不可回溯。

## 2. D1 声明连线语义

**声明 = 连线。** ref.* 节点只有接到 entity.root 的对应声明端口才进入声明表；扫描语义废止。

### 2.1 entity.root 新输入端口（均 multi）

| 端口 id | 类型 | 接受的源 |
|---|---|---|
| geometries | GEOMETRY_REF | ref.geometry.ref |
| textures | TEXTURE_REF | ref.texture.ref |
| materials | MATERIAL_REF | ref.material.ref |
| animations | ANIMATION_REF | ref.animation.ref |
| animation_controllers | AC_REF | ref.ac.ref |

位置：scale_z 之后、animate 之前。render_controllers / animate 槽语义不变
（rc.condition_entry、animate.entry 原有连线不变；ref.rc → rc.condition_entry 不变）。

### 2.2 组装器（ClientEntityAssembler）

- 声明表 = root 五个声明端口的连线源节点，按 uid 字典序、同有效短名去重（putIfAbsent）。
- 端口源节点类型不匹配 → 组装诊断 error（INVALID_DECLARATION_REF）。
- 保留：default 别名（D2 旧）、ref.ac 合入 animations JSON 字段（D6 旧）。
- `AssemblySupport.reachableGraphs` 不再服务于声明表（codegen 子图内联仍可用）。

### 2.3 验证器（GraphValidator）

- REF_CONFLICT：范围改为「连到声明端口的 ref」（同有效短名不同标识符）。
- 新 REF_NOT_CONNECTED（WARNING，仅 CLIENT_ENTITY 库）：
  - 主图 ref.geometry/texture/material/animation/ac 未连到对应声明端口，
    且其标识符选项非空（标识符为空的占位 ref 不报——UNKNOWN_REFERENCE 已覆盖）；
    消息需点明「仅接 animate.entry 不进声明表」情形；
  - 主图 ref.rc 未连到任何 rc.condition_entry；
  - 子图中的声明类 ref.*（无法声明）→ 提示移至主图接线。
- RC/AC 库中的 ref.*（接表达式槽的 VALUE_REFS 用法）不受影响、不报。

### 2.4 迁移（GraphMigrations v2→v3，CURRENT_FORMAT_VERSION=3）

加载路径统一调用（资源包 loader / EprojectIo），与 v1→v2 链式执行：
- CLIENT_ENTITY 库主图：无声明连线的 ref.{geometry,texture,material,animation,ac} → 补线到对应端口。
- 主图 ref.rc 未连任何 rc.condition_entry → 新建 rc.condition_entry（置于 ref 右侧，
  condition 用端口默认 1），ref→entry.rc、entry.entry→root.render_controllers。
- 子图中**完全无连线**的声明类 ref → 移动到主图（uid/选项保留，位置置于主图空闲区）并补线；
  子图中有连线的 ref 不动（REF_NOT_CONNECTED 会提示）。
- RENDER_CONTROLLER / ANIMATION_CONTROLLER 库不变。

### 2.5 导入器（JsonGraphImporters）

- importRefTable 每个 ref 建节点后立即 wire 到对应 root 声明端口。
- resolveAnimationRef 的未声明兜底 ref.animation 保持不连线（UNKNOWN_REFERENCE 已报）。

### 2.6 KnownRefTables（client）

collectGraphLibraries 改为只收集「主图连到声明端口」的 ref（与组装语义一致）。

## 3. D2 闭包导入（RC/AC 跟随实体）

UI 导入 ClientEntity（注册表 id 与 JSON 文件两种来源均生效）时：

1. 实体导入并注册库（命名沿用现有 finish 逻辑）；
2. 收集主图 ref.rc 标识符（去重、uid 序）→ 在 `render_controllers/` 资源目录全部
   `.json` 中找含该键的文件 → importRenderController（KnownRefTables.collectForRc）→
   注册为独立库；找不到 → 诊断 warning（CLOSURE_MISS），不阻断；
3. 收集 ref.ac 标识符 → `animation_controllers/` 目录同法 → importAnimationControllers；
4. 诊断按文档分节汇总进 DiagnosticsCenter（§4）；编辑器打开实体库。

共享实现：`client.nodegraph.ImportClosure`（ResourceManager 扫描，单次导入内缓存目录列表；
跨版本资源读取差异用 //? 消化）。两版编辑器 ImportDialog(s) 均改走闭包；
直接导入 RC/AC 文件的路径不变。

## 4. D3 诊断中心 + 浮动面板

### 4.1 DiagnosticsCenter（client.nodegraph，进程内单例）

```java
public record Section(String label, List<Diagnostic> diagnostics) {}
public record Batch(long epochMillis, String source, List<Section> sections) {
    long errors(); long warnings();
}
report(String source, String label, List<Diagnostic>)   // 单节便捷
report(String source, List<Section>)                    // 多节（闭包导入）
latest(); addListener(Consumer<Batch>); removeListener(...)
```

- 空诊断不上报（面板保留上一批）。
- 聊天栏不再出现任何 error/warning 诊断；成功类一句确认（已保存/已新建项目）保留。
- 日志（slf4j）保持全量。

### 4.2 上报点

- 导入（含闭包每文档一节）、构建（NodegraphBuildService 结果）、编辑器打开库时的翻译诊断、
  规范化等批量诊断 → DiagnosticsCenter。
- 「无法识别的 JSON 形态」等错误 → DiagnosticsCenter（source=导入）。
- ldlib2 EvmDiagnostics.report 的聊天路径删除；info() 保留。

### 4.3 浮动面板（两版编辑器各一，IDEA 工具窗口风格）

- 左下角（节点面板下方）常驻开关按钮：「问题」+ 红 error 数 / 黄 warning 数徽标（0/0 置灰）。
- 点击开关浮动面板（约 360×220，位于按钮上方）：标题栏（来源 + 时间 + E/W 统计 + 关闭 ✕），
  可滚动列表，行 = 严重级色点 + code + message（节 label 作分组头）。
- 行带 nodeUid 时点击 → 画布选中并居中该节点（best effort，两版均做）。
- 面板监听 DiagnosticsCenter 自动刷新；不自动弹出。

## 5. 非目标

- 编辑器内「放下 ref 节点自动接线到 root」（后续 UX 增强，另立任务）。
- 诊断面板的历史批次的持久化 / 跨会话保留。
- 26.1 网格 LOD（既有遗留）。

## 6. 验证标准

1. domain 单测全绿 + 新增：声明连线组装、REF_NOT_CONNECTED、迁移 v3（旧扫描图 → 补线后产物等价）。
2. 悦灵全闭包实机：导入实体 → RC/AC 库自动出现；ref.* 全部带线；构建注入后渲染与 pack 原版一致。
3. 聊天栏零诊断；面板开关、徽标计数、行点击定位节点生效（两版编辑器）。

## 7. 验证结果（2026-08-04）

- 单测：1.20.1 / 1.21.1 全量绿（domain 201 测试含新增 15+，架构门禁过）；26.1.2 编译绿。
- 实机 1.20.1（DiagWiring2）：悦灵闭包导入 6 RC 0 miss；61 声明 ref 全部接线、
  ref.rc 6/6 接 rc.condition_entry；验证器 0 诊断；7 库构建注入 0 诊断；
  渲染组件解析 = pack 原版（geometry.oreville_ans.* / czm.png）；面板开关/徽标（E0 W4）/
  分节列表/行点击聚焦端到端（REF_NOT_CONNECTED 断线场景 → 点击行 → selected=[geo0] 且视口居中）。
- 实机 1.21.1（DiagWiring21）：闭包 6 RC 0 miss、geometries=13 连线、构建成功；
  面板打开后标题/分节/空节跳过/✕ 齐全（合成点击受虚拟显示光标回中限制，
  开关走 Button/setOnClick 与工具栏按钮同机制）。
- 闭包文档源修正：RC 注册表优先（EntityJsonService 编码），资源扫描兜底——
  初版纯资源扫描 vanilla RC 全 miss（vanilla 不走 BrResourcesLoader 管线）。
