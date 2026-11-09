### eyelib 21.1.28

三个版本同步更新：1.20.1 (Forge)、1.21.1 与 26.1.2 (NeoForge)。

#### 修复
- **26.1.2 开启光影后，部分实体整体不显示（只剩地面阴影）**：Iris 26.1 改为按 `RenderPipeline` 对象分发光影程序，经 `IrisApi.assignPipeline` 登记时内部走 `ShaderKey.findBestMatch`；该方法的「okay」分支按枚举声明顺序返回同一 program 的第一个键，而 `ENTITIES_ALPHA`（顶点 alpha 测试，永不通过）声明在 `ENTITIES_SOLID` 之前。任何不带 `ALPHA_CUTOUT` define 的实体管线因此拿到「永不通过」的 alpha 测试，片元被全部丢弃。现改为按 `ShaderKey` 精确登记（`ENTITIES_SOLID` / `ENTITIES_CUTOUT` / `ENTITIES_TRANSLUCENT` / `ENTITIES_EYES` / `ENTITIES_EYES_TRANS` / `GLINT`），与 Iris 给 vanilla 实体的映射一致。
  表现：材质名写 `entity`（无 defines/states 的默认实体材质）的模型在光影下完全不可见；doki 资源包里的鸡、猪、牛、哞菇即此形态，其余 cutout / 半透明材质原本正常。

#### 兼容性
- 1.20.1 与 1.21.1 渲染行为零变化：该光影桥接类只在 26.1 分支编译。
- 需要 Iris 的 26.1 分支；Iris 未安装或内部 API 不可用时自动退回原路径，不崩溃。
- 设计与验证细节见 ADR-0041。
