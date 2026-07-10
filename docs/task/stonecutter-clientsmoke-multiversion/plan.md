# Stonecutter 检查 + Clientsmoke 多版本化

## 调研结论

### Stonecutter 版本
- 当前 `settings.gradle`: `dev.kikugie.stonecutter` version **0.9.6**
- Gradle Plugin Portal 最新: **0.9.6** (2026-06-15)
- **结论: 已是最新，无需更新**

### 过期用法 (主项目 build.gradle)
build.gradle 有明确的 "0.7.x" 时代注释和写法。0.8+ 引入了现代 API:

| 当前写法 (0.7 风格) | 0.9 现代写法 |
|---|---|
| `stonecutter.current.version` | `sc.current.version` (别名) |
| `stonecutter.eval(mcVersion, '<1.20.6')` | `sc.current.parsed.matches '<1.20.6'` |
| 注释 "0.7.x 无 sc 别名" | 应更新 |

sourceSet 手动替换 (line 24-31, 51-54) 是否冗余 → **待实测验证** (0.9 centralScript 是否自动替换)

注: `stonecutter.eval` 仍是文档化 API, 非硬过期。清理是代码质量改进。

### Clientsmoke 多版本化

#### 决策记录
- **基线**: 以 forge 完整版为基线, 三版本都保留完整功能
- **项目结构**: 替换全部三个项目 (clientsmoke + clientsmoke-neoforge + clientsmokeannotation), 合并为单一 stonecutter 项目
- **依赖**: annotation + runtime 改为编译期和运行时都可见

#### 现有两版本功能差距
| 维度 | forge (1.20.1) | neoforge (26.1.2) |
|---|---|---|
| 状态机 | 13 状态 ~1018 行 | 7 状态 ~207 行 |
| 世界创建 | ✓ createFreshLevel + WorldPresets.FLAT | ✗ |
| 截图 | ✓ RenderLevelStageEvent + NativeImage + FBO | ✗ |
| HUD隐藏 | ✓ | ✗ |
| REPOSITION | ✓ delayTicks | ✗ |
| JSON报告 | ✓ | ✓ + JUnit XML |
| 两阶段退出 | ✓ mc.stop() + halt() | ✓ |

#### 跨版本 API 适配点 (//? 注释)
1. **事件**: Forge TickEvent/RenderLevelStageEvent ↔ NeoForge ClientTickEvent/RenderLevelStage
2. **世界创建**: createFreshLevel API 在 1.20.1/1.21.1/26.1.2 差异 (最难)
3. **Config**: ForgeConfigSpec ↔ ModConfigSpec
4. **Mod构造**: FMLJavaModLoadingContext ↔ 构造器注入(IEventBus, ModContainer)
5. **Window**: getWindow().getWindow() ↔ handle()
6. **注册表**: RegistryAccess/WorldPresets 跨版本变化
7. **Mixin**: Forge Mixin AP ↔ NeoForge (1.21+ 可能不需 mixin)

#### 风险
- 26.1.2 世界创建 API 未知 (新版本体系), 可能需要重新调研
- 1.21.1 不存在, 需新建

## 子任务分解

### Phase 1: 过期用法清理
- ST-1: 清理主项目 build.gradle 的 0.7 时代过期用法 + 验证 sourceSet

### Phase 2: clientsmoke 多版本化
- ST-2: 创建新 stonecutter 项目结构 (替换三项目)
- ST-3: 以 forge 基线合并源码 + //? 适配 1.20.1/1.21.1/26.1.2
- ST-4: 整合 annotation + 依赖配置 (编译期+运行时)
- ST-5: 验证三版本编译

## 验收标准
1. 主项目 build.gradle 过期注释/API 清理, 三版本编译通过
2. 新 clientsmoke 单一项目支持 1.20.1/1.21.1/26.1.2 三版本
3. 三版本保留完整功能 (世界创建/截图/报告)
4. annotation 编译期+运行时可见
5. 主项目 clientsmoke 运行配置正常工作
