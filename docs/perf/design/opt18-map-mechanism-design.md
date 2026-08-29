# Opt18 设计：map 机制消除（render 线程 43.4% 热点簇）

证据：opt18-tick-before.jfr（1.20.1 world n384 slime，1467 render 样本；原始 .jfr 为工作现场文件，已清理）

## 子树份额
- SetupStage 30.8%（setupClientEntity 29.5% / setupModel 23.1% / AnimationComponent.setup 5.0%）
- TickStage 25.1%（BrAnimator.tickAnimation 24.4%）
- renderComponents 32.6%（HighSpeedRenderModelVisitor 14.1%）
- BatchSkinning 1.3%（GPU 蒙皮已廉价）

## setupModel 内部构成（339 样本）
- self 85（25%）
- molang 结构导航 42%：MolangStruct.get 54 + MolangScope.get 49 + rootKeyOf 10 + MolangStruct.set/remove 17 + scope$1.get 5
- 动态 query：livingFloat 22
- evalPartVisibility 15 + evalAsBool 12
- HostRole.hashCode 9

## 方案（5 项独立增量，逐项契约测试）
1. **B AnimationComponent.setup 引用短路**：记录上次应用的 (animations, animate) 引用，`==` 相同即跳过 map equals（每帧 RegularImmutableMap.get 5%）。
2. **C ModelRuntimeData 数组化**：GlobalBoneIdHandler 全局稠密 id → Entry[] 按需扩容；消除 rotation/position/scale/getData 的 Int2ObjectOpenHashMap 探测（6.7%+tick 写入侧）。
3. **D HostRole 驻留 + HostContext 数组**：of(name,type) 全局驻留分配序号 id；hostRoleStore/hostRoleMemo 改 Object[]/Entry[] 按 id 索引（HostRole.hashCode 2%+memo get）。
4. **E scope 结构导航 memo**：full-name → 叶 MolangObject memo，写纪元失效（putTracked/set/remove 自增）；覆盖 texture./material./geometry. 根的 LinkedHashMap.get+getPath 链（~7%）。
5. **A setupModel 常量短路**：Slot 首次解析时判定全部动态输入（geometry/texture/material/colorMask/rcColor/part_visibility 表达式）是否 isConstant（ConstMolangFunction；query.* 编译为 MemberSite/resolveCall，非常量，自动排除动态路径）；全常量→解析一次永久缓存（失效=clientEntity/modelVersion 变化）；任一动态→回退 Opt15 逐帧键求值。目标 23.1% 的大部分。

## 顺序与验证
B→C→D→E 一批，基准一次；A 单独一批+基准。标准验证链：编译+单测+截图+JFR+benchmark（1.20.1/1.21.1 world n384 串行协议）。
