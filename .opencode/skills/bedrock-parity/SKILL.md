---
name: bedrock-parity
description: 用真实 Bedrock addon 对照验证 eyelib 实体、Molang 与材质渲染行为。Use when comparing JE and BE output, collecting runtime evidence, or diagnosing parity differences.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432/eyelib
  version: "1.0.0"
  tags: eyelib, bedrock, parity, rendering, molang
  related-skills: eyelib, eyelib-debug, eyelib-renderdoc, progressive-exploration, mcmcp
---

# bedrock-parity

用真实 Bedrock addon 对照验证 eyelib 实体、Molang 与材质渲染行为（JE/BE 输出对比、运行时证据收集、parity 差异诊断）。

## When to use
- 需要以真实 Bedrock addon（优先 .mcpack 数据）校对 eyelib 的实体外观、动画、Molang、材质或 attachable 行为
- Do NOT use when: 不要把某次对比的截图或提交历史当作规范

## Rules
- When 高风险陷阱:
  - NEVER 旧客户端占用调试端口时探针可能读到旧代码——不可信任未确认归属的调试端口探针结果。
  - player.teleportTo 可能被服务端回弹；需要服务端命令传送。
  - 宽体实体贴墙会 suffocation；判断实体“消失”前先查死亡来源。
  - 未实现 Molang query 返回 MolangNull 而不是 0；沿表达式检查 null 传播。
  - NEVER texture_mesh 坐标和 UV 不要凭 JE 直觉猜；必须用 Blockbench Bedrock codec 读取内部坐标。
- When 固定流程:
  - 建立 oracle 的权威顺序：先查 Mojang Creator 文档，再查实际 .mcpack/.brarchive，最后查 Bedrock Wiki；项目文档只作二次解释。
  - 对齐场景：JE/BE 使用同坐标、朝向、光照、时间、FOV=60；对齐群系、装备和实体类型。
  - 排除随机性：变体用一排实体看分布；状态动画用单个受控实体，避免把随机相位当 bug。
  - 按层归因：模型加载 → 部件可见 → 顶点提交 → 几何/UV/颜色 → 片元 → alpha discard → 混合/光照/overlay。
  - 修复后复测：先定向单测或运行时 probe，再做跨版本构建；渲染接线或材质变化必须补 clientsmoke/RenderDoc 证据。
- When 关键语义速查:
  - RC 的纹理数组只有在多采样/掩码材质中才会合成多层；单采样材质只取第 0 层。
  - overlay_color 是状态覆盖色，不是常驻 tint。
  - emissive 低 alpha 需要独立 clamp 路径；additive 保留 (SourceAlpha, One)。
  - 收集的骨骼姿态不含 pivot 平移；attachable/locator 必须自行补 pivot。
  - PREFER 完整判定规则参考 docs/concepts/bedrock-parity-investigation.md。

## Workflow
1. 建立 oracle：Mojang Creator 文档 → .mcpack/.brarchive → Bedrock Wiki
2. 对齐场景：同坐标、朝向、光照、时间、FOV=60、群系、装备、实体类型
3. 排除随机性：变体看分布、状态动画用受控单实体
4. 收集证据：mcmcp_execute 查询 RenderData/scope/model components/动画状态；RenderDoc 确认顶点、像素历史和深度遮挡
5. 按层归因：模型加载 → 部件可见 → 顶点提交 → 几何/UV/颜色 → 片元 → alpha discard → 混合/光照/overlay [decision]
6. 修复后复测：先定向单测/运行时 probe，再跨版本构建；渲染接线或材质变化补 clientsmoke/RenderDoc 证据 [stop]
