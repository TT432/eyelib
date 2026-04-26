# Draft: Animation Playback Issue

## Requirements (confirmed)
- [issue]: 现在动画播放有问题，尝试分析哪里有问题
- [mode]: 先做分析与上下文收集，再进入深入定位
- [symptom-update]: 动画计算出的 PoseStack 有误，导致模型视觉错误

## Technical Decisions
- [analysis-first]: 优先做只读排查，不做代码修改
- [scope]: 先定位播放链路与高概率逻辑缺陷，再补充可验证路径

## Research Findings
- [path]: `src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java` 负责每帧调用 `BrAnimator.tickAnimation`
- [path]: `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/BrAnimationController.java` 控制器状态切换与混合逻辑
- [path]: `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` 动画时间推进与骨骼插值
- [path]: `src/main/java/io/github/tt432/eyelib/capability/component/AnimationComponent.java` 动画映射初始化与运行时数据缓存
- [path]: `src/main/java/io/github/tt432/eyelib/client/render/visitor/ModelVisitor.java` 的 `applyBoneTranslate` 使用 `context["bones"]` 缓存 Pose
- [path]: `src/main/java/io/github/tt432/eyelib/util/client/PoseHelper.java` 的 `copy` 可能是浅拷贝（直接传递 pose/normal）
- [path]: `src/main/java/io/github/tt432/eyelib/client/render/RenderHelper.java` 复用同一个 `ModelVisitContext` 且未发现 clear 调用

## Open Questions
- [scope]: PoseStack 错误是否同时出现在实体渲染与管理器预览（AnimationView）两条链路？
- [repro]: 是否是“同一实体首帧正常，后续帧逐步偏移/扭曲”这种时间累积型错误？

## Scope Boundaries
- INCLUDE: 动画播放主链路逻辑排查、关键可疑点定位、最小验证步骤
- EXCLUDE: 直接修复代码与提交
