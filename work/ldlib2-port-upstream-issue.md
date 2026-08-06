# 上游 Issue 草稿（待用户发布到 weiliangyan/LDLib2-1.20.1-Forge）

标题：release r8 在 1.20.1 无法启动：34 处调用 1.21 才有的 ResourceLocation 静态方法

正文：

r8 发布产物（ldlib2-forge-1.20.1-2.2.27+forge.1.20.1-all.jar，sha256 dc5465c5…）在
Minecraft 1.20.1 + Forge 47.1.3 启动即崩溃：

```
java.lang.NoSuchMethodError: 'net.minecraft.resources.ResourceLocation
    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(java.lang.String, java.lang.String)'
	at com.lowdragmc.lowdraglib2.LDLib2.id(LDLib2.java:86)
```

`ResourceLocation.fromNamespaceAndPath(String,String)` / `parse(String)` /
`withDefaultNamespace(String)` 三个静态方法均为 MC 1.21 新增，1.20.1 不存在。
README 中「客户端可以启动并进入游戏」与 r8 产物行为矛盾（main 分支 4bced05 同问题）。

全量常量池扫描（含继承解析对照 1.20.1 成员表），泄漏面仅这 3 个方法，共 34 个调用点：

- `fromNamespaceAndPath` ×8（LDLib2、ClientProxy、LDLibShaders、LDShaderInstance、
  ModelBakeryMixin、ShaderInstanceMixin、FilePath、ResourceHelper）
- `parse` ×23（IModelRenderer、LDShaderHolder、各 configurator accessors、
  StringConfigurator、IResourcePath、AnimationTexture、IGuiTexture、ShaderTexture、
  SpriteTexture、VanillaSpriteTexture、TextureValue、ILDLRegister(Client)、
  LDLRegistry$RL、XmlUtils）
- `withDefaultNamespace` ×3（IModelRenderer、VanillaSpriteTexture($1)）

1.20.1 对应 API：`tryBuild(ns, path)` / `tryParse(str)`（tryParse 同样默认 minecraft
命名空间）。语义差异：1.21 的 parse 对非法输入抛异常，tryParse 返回 null——若依赖
抛异常语义需自行包一层。

建议：将这三处替换为 1.20.1 等价 API 重新发布。我们已用字节码修补验证过：
全部重定向后客户端可启动进世界，UI 编辑器与 nodegraphtookit 图编辑器可打开、
可渲染、可保存，无其他 1.21 API 泄漏。
