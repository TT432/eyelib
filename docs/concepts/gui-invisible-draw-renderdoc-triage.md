# MC GUI 自绘元素"不可见"排查：先 RenderDoc 定生死，再猜 Java 层

> 2026-08-12 实证（scratch 积木编辑器调试，约 4 小时空转后 30 分钟定位）。

## 症状

自绘 GUI 元素（`BufferBuilder` + `RenderType.gui()` 顶点流）在 Screen 里完全不可见，
但同 Screen 的文字（drawString）、`GuiGraphics.fill` 背景正常可见。

## 错误路径（本次实测浪费数小时的路）

在 Java 层逐个猜 GL 状态并做对照探针：背面剔除环绕方向、深度测试 LEQUAL、
scissor 裁剪、`BufferBuilder` begin/end 状态机、`color(int)` vs float 分量、
攒批 flush 时机、RenderType 选择（gui/guiOverlay）——**全部被实验证明无关**。
原因：这些症状（"Screen render 内不可见、Post 事件探针可见"）与**被后续绘制覆盖**
完全同构，Java 层无法区分。

## 正确路径（30 分钟定位）

1. RenderDoc 截帧（`RenderDocCapturer.startCapture/endCapture`，见 eyelib-renderdoc skill）。
2. Python replay：对疑似 draw（按 indices 数量识别）`SetFrameEvent` 后 dump 输出纹理成图。
3. 两种结论分流：
   - **帧缓冲里有** → 画成功了，是被后续 draw 覆盖 → 沿 draw 序列扫描像素计数，
     找到覆盖发生的 eventId，读该 draw 的顶点定位凶手。
   - **帧缓冲里没有** → 顶点/管线状态问题，再回去查 Java/GL。
4. 本例凶手：一个 `fill(0, contentBottom, w, PREVIEW_H, BAR_BG)` 调用——
   `GuiGraphics.fill` 是 `(x1,y1,x2,y2)` 语义，第 4 参误传高度，
   `GG.fill` 内部 swap 后画出**全屏不透明矩形**，在积木之后绘制，盖掉一切非文字元素。

## 1.20.1 实证 API 事实

- `GuiGraphics.fill/drawString` 等末尾 `flushIfUnmanaged()`：unmanaged GG（Screen render 用的）
  每个操作**立即 flush**，几乎不留攒批。自绘原语可以安全地 `endBatch(type)` 立即 flush。
- `RenderType.gui()`：QUADS + LEQUAL 深度 + **CULL（剔背面）**；
  `guiOverlay()`：QUADS + **NO_DEPTH_TEST + COLOR_WRITE（不写深度）**——
  GUI 覆盖层语义与文字一致，自绘 GUI 元素首选。
- `text(...)`：NO_DEPTH_TEST——**文字永远无视深度可见**，"文字可见但填充不可见"
  不能推出深度问题。
- GUI ortho 投影下**屏幕逆时针 = GL 正面**；顺时针顶点序被 gui() 的 CULL 剔除。
- 截图坐标 = 物理像素，Screen/GG 坐标 = 逻辑像素（×guiScale）。
  eval 探针放坐标时先确认逻辑屏尺寸（`window.getGuiScaledWidth/Height`），
  放在逻辑屏外的探针"不可见"是正常的。

## 自绘三角形原语的可用配方（scratch 编辑器 fillTriangles 实证）

```java
VertexConsumer buffer = bufferSource.getBuffer(RenderType.guiOverlay());
// 每三角形写退化 quad (v0, v2, v1, v1)：对齐 QUADS mode + 逆时针正面
buffer.vertex(pose, x0, y0, 0).color(r, g, b, a).endVertex(); // float 分量，与 GG.fill 一致
...
bufferSource.endBatch(RenderType.guiOverlay());
```
