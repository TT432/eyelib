### eyelib 21.1.26

1.20.1 (Forge)、1.21.1 与 26.1.2 (NeoForge) 同步更新。

#### 修复
- **26.1.2 + Iris/Oculus 光影下模型错位、铺满屏幕的巨面片、天空与云被污染**：光影激活时 GPU 蒙皮自动回退到经典 CPU 路径；自定义 `eyelib:core/entity_skinned` 渲染管线同时登记到光影包的 ENTITIES / ARMOR_GLINT / EMISSIVE_ENTITIES / ENTITIES_TRANSLUCENT / SHADOW_ENTITIES 程序，未登记的管线不再用原版程序去解释自有顶点布局。
- **睡眠姿态平移顺序**：平移先于实体缩放，复刻 vanilla `LivingEntityRenderer` 的顺序。

#### 兼容性
- 关闭光影、或未安装光影的客户端路径不变。
- 坐标：`io.github.tt432:eyelib:21.1.26+<mc>-<loader>`，同版本三节点需要按 MC 版本取对应文件。
