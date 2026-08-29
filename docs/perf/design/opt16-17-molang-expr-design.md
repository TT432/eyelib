# Opt16 设计：molang 求值链 + 渲染解析链残余热点

JFR 基线：opt16-before.jfr（1.20.1 world n384，Render thread 373 样本，60s profile；原始 .jfr 为工作现场文件，已清理）。

## 归因（叶帧 / 归属）

| 热点 | 占比 | 归属 |
|---|---|---|
| BrBoneAnimationSampler.lerp 叶 | 9.4% | 采样循环本体（已融合查找，残余为真实工作） |
| CompiledMolangExpression.apply 叶 | 8.6% | 生成字节码本体（resolveMemberAccess/resolveCall/asFloat/valueOf） |
| MolangValue3.getX 叶 | 7.5% | evalWithThis 逐轴求值（内联归属帧） |
| BrBoneAnimationSampler.sample 叶 | 5.6% | 同上链 |
| MolangValue.getObject 叶 | 4.0% | clearTemp 短路+try+null 检查（已薄） |
| Int2ObjectOpenHashMap.find | 4.0% | ModelRuntimeData.getData(12) + collectBindBones(3) |
| HashMap.getNode | 4.0% | UnmodifiableMap.get←RegistrySnapshot.get←Registry.get←BrControllerExecutor.blend(6+2+1) + MolangScope.get←resolveMemberAccess(5) |
| MolangRuntimeSupport.resolveCall 叶 | 2.1% | 已缓存（shape/zeroArg），残余小 |
| MolangFloat.valueOf 叶 | 1.6% | wrapJavaResult 逐次分配(5) |
| MolangScope.get/rootKeyOf | ~1.9% | resolveMemberAccess 的前置遮蔽检查 |
| 含 getObject 的栈 | 20.1% | molang 求值链总量 |
| molang 总链 | 38.6% | |
| BatchSkinning | 0.0% | 合批后已从 profile 消失 |

## 关键结构事实（源码实证）

1. `query.*`/`math.*` 从不写入 scope cache（grep `.set("query|math|context.` 仅命中 context.item_slot/context.is_first_person 两处附挂路径）。`context.*` 有写入点。
2. scope.setParent 仅粒子实例使用；实体渲染 scope 无 parent。
3. resolveMemberAccess 逐次求值做 scope.get(dottedName) 遮蔽检查（rootKeyOf+map get+parent 递归），对 query/math 恒 miss。
4. EntityRenderOrchestrator.collectBindBones 每实体每帧新建 Int2ObjectOpenHashMap + 逐骨骼 putIfAbsent（TickStage 热路径，EntityRenderOrchestrator.java:211/279）。modelComponents 列表的变更点全集：setupClientEntity(clear+addAll/add, :581/:603/:621/:632) 与 RenderSyncApplyOps.replaceModelComponents(:35)。
5. BrControllerExecutor.updateAnimations/blend 每实体每动画每帧：animations.get(animName)（HashMap）+ AnimationLookup.get（Registry→RegistrySnapshot→UnmodifiableMap→LinkedHashMap）。Registry 已有 generation() 失效机制（Registry.java:60）。
6. MolangFloat.valueOf 仅缓存 0/1；wrapJavaResult 每次 Number→新实例（JFR 1.3%+GC 压力）。
7. BrClipExecutor 骨骼循环：getData(boneId) 在空通道短路之前执行——无通道骨骼白做一次 computeIfAbsent（还创建零值 Entry）。Entry 缺失与零值 Entry 在读侧完全等价（position/rotation/scale/getOrDefault 均回退 bind/IDENTITY）；entries() 无渲染合成消费端。

## Opt16 五项

### 1. resolveMemberAccess query/math 快路径（~2-3%）
- MolangScope 增 `volatile int rootOverrideBits`（QUERY=1, MATH=2）；putTracked 对首段为 query/math 的键（含整名与 struct 根）置位；remove 不清（粘滞保守——置位只牺牲性能不影响正确性）。
- `hasRootOverrideChain(int bit)`：沿 parent 链逐层检查（渲染 scope parent==null，一级即停）。
- resolveMemberAccess：名首段 query/math 且链上无对应覆盖位 → 跳过 scope.get，直接 zeroArgBinding。其余名（context/variable/temp/裸名）走原路径。
- 正确性：scope.get 非 null 当且仅当本层/祖先 cache 含根键为 query/math 根的条目；这些条目只能经 set 系列→putTracked 写入，全部置位。整名 "query" 置位属保守超集（不影响 query.x 查询但触发慢路径）。

### 2. MolangFloat 小整数缓存（~1.3%+GC）
- valueOf(float)：value==(int)value 且 -16≤v≤256 → 共享数组。record equals 按值，缓存透明。

### 3. BrClipExecutor 短路前移（小）
- has* 三判据移到 getData 之前；无通道骨骼不再建 Entry（读侧等价，见事实 7）。

### 4. collectBindBones 缓存（~1.5%+每帧分配消除）
- RenderData 增 `bindBonesCache` + `bindBones()` 懒构建 + `invalidateBindBones()`。
- 失效点（变更点全集，事实 4）：setupClientEntity 的 components.clear() 两处（:581/:632）+ RenderSyncApplyOps.replaceModelComponents（经 ClientRenderSyncService.apply 调用处加失效，因 replaceModelComponents 签名无 RenderData）。
- collectBindBones 保留为构建函数（RenderData.bindBones 委托）；4 个调用点（EntityRenderOrchestrator×2、AttachableItemRenderSetup、ItemRendererMixin、ItemInHandRendererMixin）改走缓存。

### 5. BrControllerExecutor 动画解析缓存（~2.2%）
- BrAnimationController.Data 增解析缓存：键 animName，值 Optional<Animation>；守卫 = (animations map 实例 identity, AnimationRegistries.animation().generation())，错位即清空重建。
- updateAnimations 与 blend 的 lastState 分支统一走 data 缓存解析（animations.get+AnimationLookup.get 合并进缓存条目）。
- 正确性：animations map 为实体定义数据（构建后不变，identity 守卫防御替换）；registry 变更经 generation 失效。

## 非目标
- lerp/sample/evalWithThis 的真实求值工作（动态表达式每帧必须重求值，`this` 逐轴不同，无缓存空间）。
- getObject 薄壳（clearTemp 已短路、try/catch 零成本直到抛出）。
- TickStage 并行化（独立大项）。


## Opt17 追加设计（after-JFR 驱动，opt16-after.jfr，316 render 样本；原始 .jfr 为工作现场文件，已清理）

after-profile 确认 Opt16 生效：Int2Object find 4.0%→消失、RegistrySnapshot 链消失、HashMap.getNode 4.0%→2.2%。
新浮现：
1. **checkCustomized 2.5% ← invokeZeroArgMethod ← resolveMemberAccess ← apply**：invokeExact 调用点被全部表达式共享（megamorphic），JIT 无法按站点特化。
2. **Arrays.equals←MethodType.equals 3.8% + asSpreaderChecks 1.3% + checkPtypes 1.0%**：invokeMethod 的 invokeWithArguments 泛型分派（非零参调用），逐次 MethodType 比较。
3. **StringLatin1.newString←substring←rootKeyOf 2.9%**：rootKeyOf 对 3+ 段名/非根点分名每次 substring 分配。

### Opt17-A：MemberSite 站点级单态分发
- 发射器为每个可扁平化成员访问生成实例字段 `ms$N`（MemberSite，构造期经 newMemberSite 初始化）；访问点改发 `aload(0); getfield; aload(1); invokevirtual MemberSite.resolve`。
- resolve 内联 resolveMemberAccess 全语义：null 检查 → 覆盖位链（快路径位）/scope.get → 站点本地 (tree,epoch) 守卫的 minimal/full 绑定缓存 → invokeZeroArgField/Method。
- 收益机制：evaluate 属生成类专有，resolve 内联后 invoker.invokeExact 的 LambdaForm 按表达式类单态化，消除 checkCustomized 与 ZERO_ARG_CACHE CHM 查找。
- 表达式实例全局共享（compile cache），站点状态仅 (tree,epoch,binding)——scope 相关状态（override/host）逐次现查，正确性保持。

### Opt17-B'：invokeMethod 缓存 spreader invokeExact
- SPREAD_INVOKERS：Method → asFixedArity().asSpreader(Object[],n).asType((Object[])Object)+WRAP_RESULT 组合句柄（解析期一次）。
- 调用点 invokeExact(args) 消除逐次 asSpreader/MethodType 检查；诊断开关复用 -Deyelib.molang.exactInvoker=false 回退 invokeWithArguments。

### Opt17-C：rootKeyOf CHM 备忘
- 纯函数备忘（ConcurrentHashMap<String,String> get+putIfAbsent），仅对会分配 substring 的形态（3+ 段 molang 根名/非根点分名）走 memo；命中 = String 缓存哈希 + 一次 probe。

### 验证
- 契约测试：MemberSite 端到端（编译→求值→运行时注册→遮蔽→struct 遮蔽）；既有 251 类全量回归覆盖 B'/C。
- 运行时截图 + JFR 复测（目标：checkCustomized/MethodType.equals/substring 消失）+ world n384 基准。
