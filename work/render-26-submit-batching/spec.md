# 26.1.2 submit 批量优化（§7.2）— 规格

> **工作单元类型**：执行（规格）
> **输入**：`docs/concepts/cross-version-render-architecture.md` §7.2、§11；vanilla 源码（26.1.2.78 sources jar）

## 问题

`DeferredRenderSink.submit` 每段几何注册一个 `submitCustomGeometry` 节点，回调内自建
`ByteBufferBuilder(768KB)` + immediate `BufferSource` + `endBatch` —— 每实体每 pass 一次 draw call，
且每次分配 768KB native buffer。vanilla `CustomFeatureRenderer` 本身按 RenderType 分组、
每组共享一个 `getBuffer()` 返回的 BufferBuilder —— 写进**回调提供的 buffer** 才是真正的批量路径。

该路径曾崩溃（"Not building!"，commit b4a67086 改为独立 buffer 规避），根因未定位。
§11 声称"由 vanilla 按 RenderType 批量绘制"与实际代码不符——当前是延迟绘制但不批量。

## 已核实的 vanilla 行为（26.1.2.78 源码）

- `CustomFeatureRenderer.renderSolid/Translucent`：按 RenderType 分组，每组 `getBuffer(rt)` 一次，
  顺序调用各 submit 的 callback 写入同一 buffer → `LevelRenderer` 在 phase 末 `bufferSource.endBatch()` 统一绘制。
- `BufferSource.getBuffer`：QUADS 类型复用 startedBuilders 中的 builder（building=true），
  否则新建。stale（building=false）builder 不可能从 getBuffer 返回（build() 只发生在 endBatch，且随即 remove）。
- 结论：b4a67086 时的崩溃根因不明，可能已被后续修复（Sampler1 6f2fa95b、纹理注入 53045fe5）掩盖，
  也可能依然存在。**必须实验验证。**

## 方案

1. **实验**：`DeferredRenderSink` 回调恢复为直接写提供的 buffer
   （`collector.submitCustomGeometry(pose, renderType, (p, consumer) -> writer.write(p, consumer))`）。
2. **验证**：26.1.2 clientsmoke 全测试通过（实体渲染路径覆盖）。
3. **分支**：
   - 通过 → 保留 provided-buffer 路径，批量自然达成（vanilla 按 RenderType 共享 builder）。
   - 崩溃复现 → 取堆栈定位根因并修；修不了则回退独立 buffer 方案，把根因记录进 §7.2，关闭本任务。

## 不变量

- 1.20.1 / 1.21.1 零改动（ImmediateRenderSink 不动）
- 26.1.2 实体渲染像素一致（clientsmoke 捕获对比）

## 验证

1. `eyelib_debug_build` 三版本编译
2. `eyelib_debug_clientsmoke` 26.1.2 全测试通过
3. 若通过：更新架构文档 §7.2 为真实状态；若回退：§7.2 记录根因与放弃原因
