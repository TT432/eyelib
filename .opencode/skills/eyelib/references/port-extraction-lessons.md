# Port 提取验证运行：经验教训

> 2026-06-08，批次 1（eyelib-material）完成后的总结。

## 成功的模式

### PortStringRepresentable 替换（21 个枚举）

机械替换模式：改 import → 改 implements → 改 CODEC 创建。子代理可以并行批量处理。零运行时退化。

关键实现细节：
- `fromEnum()` 用 `Codec.STRING.xmap` 替代 MC 的 `StringRepresentable.fromEnum()`
- 泛型 `T extends Enum<T> & PortStringRepresentable` + `Supplier<T[]>`（因 Java `T::values` 每次返回新数组）

### PortResourceLocation

纯数据 record，`of()` / `parse()` / `toString()`。只替代 domain 内部的使用，不改变公共 API 签名（以免影响大量下游调用方）。

### PortRenderPass 接口

`transparency()` 返回 `SOLID|ALPHA_TEST|TRANSLUCENT|ADDITIVE`，`disableCulling()` 返回 bool。`BrMaterialEntry.getRenderType()` 改为返回 PortRenderPass（而非 MC RenderType），使 domain 层不再依赖 MC 渲染类型。

## 踩过的坑

### 1. Forge dev 新子项目陷阱

新建 `eyelib-bridge` 后启动游戏，Forge 报 `Missing mods list` 或 `constructed 0 mods but had 1 mods specified`。

根因：Forge dev 的 classpath scanner 扫描所有 JAR，要求每个 JAR 有合法的 `[[mods]]` 声明 + `@Mod` 注解类。

修复：
- `mods.toml` 中保留 `[[mods]]` 和 `modId`
- 添加 `@Mod("eyelibbridge")` 的空类
- 根 `build.gradle` 加 `api`/`modImplementation`/`jarJar`

### 2. 循环依赖：domain → bridge ❌

eyelib-material 的 smoke test（MaterialPipelineSmoke）和 particle 的 BedrockParticleRenderer 引用了老位置的 RenderTypeResolver。子代理错误地在 BrMaterialEntry 中导入了 bridge 的代码。编译报 `Circular dependency between tasks`。

根因：bridge 依赖所有 domain，如果 domain 也依赖 bridge（即使 compileOnly），Gradle 检测到循环。

修复：
- 在 domain 侧保留 Port 版本的 RenderTypeResolver（返回 Port 类型，不依赖 MC）
- MaterialPipelineSmoke 整体移至 bridge 模块
- Particle 侧的 MC 桥接在调用处内联（switch PortRenderPass → RenderType），不引入 bridge 依赖

### 3. Pin 行为的测试被正确破坏

`BrMaterialEntryRenderTypeTest` 有 3 个测试预期 `LinkageError`——它们断言"在 JUnit 里调 MC 会崩"，而非验证 Bedrock 规范行为。修改为 Port 类型后这些测试自然失败——因为代码不再调 MC 了。

修复：改为验证 Port 行为的测试（透明度、剔除状态），而非文档化环境缺陷。

### 4. 子代理导入错误的模块

子代理在 BrMaterialEntry 中添加了 `import io.github.tt432.eyelibbridge.material.RenderTypeResolver`——违反了架构规则。domain 模块不能引用 bridge。

教训：复杂任务中，关键约束（domain 不能依赖 bridge）需要在 task 描述中明确重复。

### 5. 编译命令：WSL vs Windows

WSL 下的 `./gradlew` 和 `java -cp ... GradleWrapperMain` 对 `/mnt/e/` 路径超时/挂死。Windows 侧的 `cmd.exe /c "cd /d E:\... && gradlew.bat ..."` 快 5-20×。
