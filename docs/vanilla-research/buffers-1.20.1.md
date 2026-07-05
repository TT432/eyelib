# 缓冲区与顶点系统 — 1.20.1 (Forge)

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码,所有路径相对于该目录。 源码树由 `scripts/extract-mc-source.py` 重建。
> 全量 `com.mojang.blaze3d.vertex.*` 包 + `net.minecraft.client.renderer.MultiBufferSource`/`BufferUploader`。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [VertexConsumer 链式 API](#2-vertexconsumer-链式-api)
3. [VertexFormat 与元素布局](#3-vertexformat-与元素布局)
4. [BufferBuilder 内存模型](#4-bufferbuilder-内存模型)
5. [BufferSource 批处理](#5-buffersource-批处理)
6. [BufferUploader GPU 上传与绘制](#6-bufferuploader-gpu-上传与绘制)
7. [VertexBuffer GL 抽象](#7-vertexbuffer-gl-抽象)
8. [关键不变量与约束](#8-关键不变量与约束)

---

## 1. 类位置与职责

| 类 | 路径 | 职责 |
|---|---|---|
| `VertexConsumer` | `com/mojang/blaze3d/vertex/VertexConsumer.java` | 顶点数据写入接口(链式 API) |
| `BufferVertexConsumer` | `com/mojang/blaze3d/vertex/BufferVertexConsumer.java` | 中间接口,添加 `putByte/putShort/putFloat` |
| `DefaultedVertexConsumer` | `com/mojang/blaze3d/vertex/DefaultedVertexConsumer.java` | 默认颜色支持(defaultColor/unsetDefaultColor) |
| `BufferBuilder` | `com/mojang/blaze3d/vertex/BufferBuilder.java` | 实现类,管理原生 ByteBuffer |
| `VertexFormat` | `com/mojang/blaze3d/vertex/VertexFormat.java` | 顶点格式元数据(Element 列表 + offset + stride) |
| `VertexFormatElement` | `com/mojang/blaze3d/vertex/VertexFormatElement.java` | 单个顶点元素(type + usage + count + index) |
| `DefaultVertexFormat` | `com/mojang/blaze3d/vertex/DefaultVertexFormat.java` | 预定义格式常量 16 个 |
| `VertexBuffer` | `com/mojang/blaze3d/vertex/VertexBuffer.java` | GL VAO + VBO + IBO 封装 |
| `BufferUploader` | `com/mojang/blaze3d/vertex/BufferUploader.java` | CPU→GPU 上传调度 |
| `Tesselator` | `com/mojang/blaze3d/vertex/Tesselator.java` | 单例,持有 `BufferBuilder`(256KB) |
| `MultiBufferSource.BufferSource` | `net/minecraft/client/renderer/MultiBufferSource.java` | 多 RenderType 缓冲区管理 |
| `VertexSorting` | `com/mojang/blaze3d/vertex/VertexSorting.java` | QUADS 排序接口 |

### 继承链

```
VertexConsumer (interface)
  └→ BufferVertexConsumer (interface, adds putByte/putShort/putFloat)
      └→ DefaultedVertexConsumer (abstract, adds defaultColor support)
          └→ BufferBuilder (concrete, owns ByteBuffer)
```

---

## 2. VertexConsumer 链式 API

### 2.1 核心抽象方法(mutable chain)

```java
VertexConsumer vertex(double x, double y, double z);
VertexConsumer color(int red, int green, int blue, int alpha);
VertexConsumer uv(float u, float v);
VertexConsumer overlayCoords(int u, int v);
VertexConsumer uv2(int u, int v);
VertexConsumer normal(float x, float y, float z);
void endVertex();
```

**特点**:
- **位置用 `double`**(x,y,z),其余用 `float`/`int`
- 所有 setter 返回 `this` 以便链式调用
- `endVertex()` 是 `void` 返回(终止符),不返回 this
- `color` 有 float 重载 `color(float r, float g, float b, float a)` 和 packed int 重载 `color(int argb)`
- `overlayCoords(int packed)` 和 `uv2(int packed)` 接受打包坐标,内部拆包为 `(packed & 0xFFFF, packed >> 16 & 0xFFFF)`

### 2.2 辅助 default 方法

```java
default void vertex(float x,float y,float z, float r,float g,float b,float a,
    float texU,float texV, int overlayUV,int lightmapUV, float normalX,float normalY,float normalZ)
```
一次性提交完整顶点(位置+颜色+UV+overlay+光照+法线)。

`putBulkData(PoseStack.Pose, BakedQuad, ...)`:批量提交 BakedQuad 的 4 个顶点,内部解析 BakedQuad 的 `int[] vertices` 数组,每个顶点 8 个 int(32 字节),提取位置(float0-8)、颜色(byte12-15)、UV(float16-20)等。

### 2.3 defaultColor 机制

`DefaultedVertexConsumer` 提供 `defaultColor(r,g,b,a)` / `unsetDefaultColor()`:
- 设置默认颜色后,后续顶点若 format 包含 COLOR element,自动填充默认颜色
- `BufferBuilder.nextElement()` 中,当 `defaultColorSet && currentElement.getUsage() == COLOR` 时自动调用 `color(defaultR,defaultG,defaultB,defaultA)`
- 显式调用 `color()` 会抛异常(冲突)

### 2.4 矩阵变换辅助

```java
default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z);
default VertexConsumer normal(Matrix3f matrix, float x, float y, float z);
```

---

## 3. VertexFormat 与元素布局

### 3.1 VertexFormatElement 定义

| Field | 类型 | 说明 |
|---|---|---|
| `type` | `Type` enum | FLOAT/UBYTE/BYTE/USHORT/SHORT/UINT/INT,含 size/glType |
| `usage` | `Usage` enum | POSITION/NORMAL/COLOR/UV/PADDING/GENERIC |
| `index` | int | Usage 内的槽索引(仅 UV 允许多个同类型) |
| `count` | int | 元素数量(如 POSITION=3, COLOR=4) |
| `byteSize` | int | `type.size * count` |

**Usage.setupBufferState**:各 Usage 枚举值内嵌 GL lambda,直接调用 `GlStateManager._vertexAttribPointer` 等。
**Usage.clearBufferState**:调用 `GlStateManager._disableVertexAttribArray`。PADDING 和 GENERIC 的 setup/clear 为空。

**约束**:`supportsUsage()` 禁止同一 Usage 出现多次(UV 例外,通过 index 区分)。

### 3.2 VertexFormat

构造时从 `ImmutableMap<String, VertexFormatElement>` 计算:
- `elements`:元素列表(按插入顺序)
- `offsets`:每个元素在顶点内的字节偏移(int 列表,累计 byteSize)
- `vertexSize`:总字节数

```java
public void setupBufferState();  // 遍历 elements,启用 VAO attrib,设置指针
public void clearBufferState();  // 遍历 elements,禁用 VAO attrib
public VertexBuffer getImmediateDrawVertexBuffer();  // 懒惰创建 DYNAMIC VertexBuffer
```

**IndexType**:`SHORT(5123,2)` / `INT(5125,4)`, `least(indexCount)` 根据 indexCount >= 65536 选择 INT。
**Mode**:`LINES(4,2,2)`, `LINE_STRIP(5,2,1,true)`, `DEBUG_LINES(1,2,2)`, `DEBUG_LINE_STRIP(3,2,1,true)`, `TRIANGLES(4,3,3)`, `TRIANGLE_STRIP(5,3,1,true)`, `TRIANGLE_FAN(6,3,1,true)`, `QUADS(4,4,4)`。每项 4 参数:`(asGLMode, primitiveLength, primitiveStride, connectedPrimitives)`。

### 3.3 DefaultVertexFormat 元素常量

| 常量 | Type | Usage | Count | Index | byteSize |
|---|---|---|---|---|---|
| `ELEMENT_POSITION` | FLOAT | POSITION | 3 | 0 | 12 |
| `ELEMENT_COLOR` | UBYTE | COLOR | 4 | 0 | 4 |
| `ELEMENT_UV0` | FLOAT | UV | 2 | 0 | 8 |
| `ELEMENT_UV1` | SHORT | UV | 2 | 1 | 4 |
| `ELEMENT_UV2` | SHORT | UV | 2 | 2 | 4 |
| `ELEMENT_NORMAL` | BYTE | NORMAL | 3 | 0 | 3 |
| `ELEMENT_PADDING` | BYTE | PADDING | 1 | 0 | 1 |

**ELEMENT_UV = ELEMENT_UV0**(别名)。

### 3.4 DefaultVertexFormat 格式常量(element 组成表)

| 格式 | 元素(按顺序) | vertexSize | 用途 |
|---|---|---|---|
| `BLIT_SCREEN` | POSITION + UV + COLOR | 24 | 屏幕 Blit |
| `BLOCK` | POSITION + COLOR + UV0 + UV2 + NORMAL + PADDING | 32 | 方块渲染 |
| `NEW_ENTITY` | POSITION + COLOR + UV0 + UV1 + UV2 + NORMAL + PADDING | 36 | 实体渲染 |
| `PARTICLE` | POSITION + UV0 + COLOR + UV2 | 28 | 粒子 |
| `POSITION` | POSITION | 12 | 纯位置 |
| `POSITION_COLOR` | POSITION + COLOR | 16 | 位置+颜色 |
| `POSITION_COLOR_NORMAL` | POSITION + COLOR + NORMAL + PADDING | 20 | 位置+颜色+法线 |
| `POSITION_COLOR_LIGHTMAP` | POSITION + COLOR + UV2 | 20 | 位置+颜色+光照 |
| `POSITION_TEX` | POSITION + UV0 | 20 | 位置+纹理 |
| `POSITION_COLOR_TEX` | POSITION + COLOR + UV0 | 24 | 位置+颜色+纹理 |
| `POSITION_TEX_COLOR` | POSITION + UV0 + COLOR | 24 | 位置+纹理+颜色 |
| `POSITION_COLOR_TEX_LIGHTMAP` | POSITION + COLOR + UV0 + UV2 | 28 | 位置+颜色+纹理+光照 |
| `POSITION_TEX_LIGHTMAP_COLOR` | POSITION + UV0 + UV2 + COLOR | 28 | 位置+纹理+光照+颜色 |
| `POSITION_TEX_COLOR_NORMAL` | POSITION + UV0 + COLOR + NORMAL + PADDING | 28 | 位置+纹理+颜色+法线 |

**注**:POSITION_COLOR_TEX 和 POSITION_TEX_COLOR 元素相同但顺序不同(Color 和 UV0 交换位置)。

### 3.5 BLOCK 格式详细布局(32 bytes/vertex)

| 偏移 | 元素 | Type |
|---|---|---|
| 0 | Position.x | FLOAT |
| 4 | Position.y | FLOAT |
| 8 | Position.z | FLOAT |
| 12 | Color.r | UBYTE |
| 13 | Color.g | UBYTE |
| 14 | Color.b | UBYTE |
| 15 | Color.a | UBYTE |
| 16 | UV0.u | FLOAT |
| 20 | UV0.v | FLOAT |
| 24 | UV2.u | SHORT |
| 26 | UV2.v | SHORT |
| 28 | Normal.x | BYTE |
| 29 | Normal.y | BYTE |
| 30 | Normal.z | BYTE |
| 31 | Padding | BYTE |

### 3.6 NEW_ENTITY 格式详细布局(36 bytes/vertex)

| 偏移 | 元素 | Type |
|---|---|---|
| 0-11 | Position | FLOAT×3 |
| 12-15 | Color | UBYTE×4 |
| 16-23 | UV0 | FLOAT×2 |
| 24-25 | UV1(OVERLAY) | SHORT×2 |
| 26-27 | UV2(LIGHTMAP) | SHORT×2 |
| 28 | Normal.x | BYTE |
| 29 | Normal.y | BYTE |
| 30 | Normal.z | BYTE |
| 31 | Padding | BYTE |

---

## 4. BufferBuilder 内存模型

### 4.1 数据结构

```java
public ByteBuffer buffer;              // 原生直接缓冲区(native heap)
public int renderedBufferCount;         // 已渲染批次数
public int renderedBufferPointer;       // 当前批次起始偏移
public int nextElementByte;             // 当前写入位置
public int vertices;                    // 当前批次顶点数
public VertexFormatElement currentElement;  // 当前填充的元素
public int elementIndex;                // 当前元素索引
public VertexFormat format;
public VertexFormat.Mode mode;
public boolean fastFormat;              // BLOCK 或 NEW_ENTITY 时为 true
public boolean fullFormat;              // NEW_ENTITY 时为 true(有 UV1)
public boolean building;                // 是否在 begin/end 之间
public Vector3f[] sortingPoints;        // QUADS 排序质心
public VertexSorting sorting;
public boolean indexOnly;               // 只有索引没有顶点数据
```

### 4.2 生命周期

```
new BufferBuilder(capacity)
  → MemoryTracker.create(capacity * 6)  [分配 6 倍容量的 ByteBuffer]
  → begin(mode, format)                  [设置 building=true, 重置指针]
    → vertex() / color() / uv() ...     [写入字节]
    → endVertex()                       [vertices++, 自动增长]
  → end() 或 endOrDiscardIfEmpty()      [创建 RenderedBuffer]
    → storeRenderedBuffer()              [计算 indexCount, 处理排序索引]
      → reset()                          [building=false, vertices=0]
  → releaseRenderedBuffer()             [计数递减, 清空 buffer]
```

### 4.3 写入路径

#### 快速路径(fastFormat=true,即 BLOCK/NEW_ENTITY)

`BufferBuilder.vertex(...)` 方法直接写入固定偏移:
```
putFloat(0,x), putFloat(4,y), putFloat(8,z)
putByte(12,r), putByte(13,g), putByte(14,b), putByte(15,a)
putFloat(16,u), putFloat(20,v)
if fullFormat: putShort(24,overlayLo), putShort(26,overlayHi)
putShort(i+0,lightmapLo), putShort(i+2,lightmapHi)
putByte(i+4,nx), putByte(i+5,ny), putByte(i+6,nz)
nextElementByte += i+8
endVertex()
```

#### 通用路径

通过 `VertexConsumer.super.vertex(...)` 逐元素调用 `color()`→`uv()`→`overlayCoords()`→...→`endVertex()`,由 `nextElement()` 管理元素切换和偏移累加。

### 4.4 增长策略

```java
static int roundUp(int x) {
    int growth = 2097152;  // GROWTH_SIZE = 2MB
    return x + growth - (x % growth);
}
```

当 `nextElementByte + increaseAmount > buffer.capacity()` 时:
```java
int newSize = capacity + roundUp(increaseAmount);
buffer = MemoryTracker.resize(buffer, newSize);
// MemoryTracker.resize → MemoryUtil.nmemRealloc (je_native_realloc)
```

### 4.5 RenderedBuffer / DrawState

**DrawState**(record):`format, vertexCount, indexCount, mode, indexType, indexOnly, sequentialIndex`。
**RenderedBuffer**:包含 `pointer`(起始偏移) + `DrawState`,提供:
```java
vertexBuffer() → bufferSlice(pointer+start, pointer+end)  // memSlice
indexBuffer()  → bufferSlice(pointer+indexStart, pointer+indexEnd)
release()      → BufferBuilder.releaseRenderedBuffer()
```

索引数据在顶点数据之后**紧邻存储**:`indexBufferStart = indexOnly ? 0 : vertexBufferEnd()`。

### 4.6 Forge 扩展

```java
public void putBulkData(ByteBuffer buffer)  // 直接写入外部 ByteBuffer
```

---

## 5. BufferSource 批处理

### 5.1 结构

```java
class BufferSource implements MultiBufferSource {
    protected final BufferBuilder builder;                          // 共享 BufferBuilder
    protected final Map<RenderType, BufferBuilder> fixedBuffers;    // 固定缓冲区(如 translucent)
    protected Optional<RenderType> lastState;                       // 上一个使用的 RenderType
    protected final Set<BufferBuilder> startedBuffers;              // 已 begin 的 builder
}
```

### 5.2 getBuffer 流程

```
getBuffer(renderType)
  → 检查 lastState:
    - 如果 RenderType 变化或不能合并:endBatch(lastState)
    - 如果 buffer 未 start:buffer.begin(renderType.mode(), renderType.format())
  → 从 fixedBuffers 取专属 Builder,不存在则用共享 builder
  → 更新 lastState
  → 返回 BufferBuilder(也是 VertexConsumer)
```

**关键不变量**:同一时刻只有一个共享 buffer 在 building 状态,切换 RenderType 时自动 end 前一个 Batch。

### 5.3 endBatch / endLastBatch

```java
endBatch(renderType):
  → getBuilderRaw(renderType)
  → 如果 build != 共享 builder 或 lastState 匹配:
    - renderType.end(builder, vertexSorting)  // 触发 RenderedBuffer + upload
    - 从 startedBuffers 移除
```

```java
endLastBatch():
  → 如果 lastState 存在且不在 fixedBuffers 中:
    - endBatch(lastState)
    - lastState = empty
```

---

## 6. BufferUploader GPU 上传与绘制

### 6.1 核心流程

```
drawWithShader(RenderedBuffer)
  → RenderSystem 线程检查(recordRenderCall 延迟)
  → _drawWithShader:
    - upload(buffer) → VertexBuffer
    - vertexbuffer.drawWithShader(modelView, projection, shader)
```

```
draw(RenderedBuffer)
  → upload(buffer) → VertexBuffer
  → vertexbuffer.draw() → glDrawElements
```

### 6.2 upload 详细流程

```java
private static VertexBuffer upload(BufferBuilder.RenderedBuffer buffer):
  1. RenderSystem.assertOnRenderThread()
  2. if buffer.isEmpty(): buffer.release(); return null
  3. VertexBuffer vb = format.getImmediateDrawVertexBuffer()  // 懒惰分配 DYNAMIC VB
  4. bindImmediateBuffer(vb): if (vb != lastImmediateBuffer) vb.bind(); lastImmediateBuffer = vb
  5. vb.upload(buffer)
  6. return vb
```

### 6.3 Immediate Buffer 策略

- `VertexFormat.getImmediateDrawVertexBuffer()` 对每个 format 懒惰创建一个 `VertexBuffer(DYNAMIC)`
- 同一 format 的连续 draw 重用同一个 VertexBuffer,避免重复 bind/unbind
- `lastImmediateBuffer` 静态变量记录当前绑定的 buffer

### 6.4 reset / invalidate

```java
reset():  // GL context 丢失时调用
  invalidate() → lastImmediateBuffer = null
  VertexBuffer.unbind() → glBindVertexArray(0)
```

---

## 7. VertexBuffer GL 抽象

### 7.1 结构

```java
public class VertexBuffer implements AutoCloseable {
    public final Usage usage;           // STATIC(35044=GL_STATIC_DRAW) / DYNAMIC(35048=GL_DYNAMIC_DRAW)
    public int vertexBufferId;          // GL VBO
    public int indexBufferId;           // GL IBO
    public int arrayObjectId;           // GL VAO
    public VertexFormat format;
    public AutoStorageIndexBuffer sequentialIndices;  // 顺序索引缓存
    public IndexType indexType;
    public int indexCount;
    public Mode mode;
}
```

构造函数在渲染线程执行:
```java
this.vertexBufferId = GlStateManager._glGenBuffers();
this.indexBufferId  = GlStateManager._glGenBuffers();
this.arrayObjectId  = GlStateManager._glGenVertexArrays();
```

### 7.2 upload(RenderedBuffer)

```java
upload(buffer):
  1. DrawState ds = buffer.drawState()
  2. uploadVertexBuffer(ds, buffer.vertexBuffer()):
     - 如果 format 变化: clearBufferState + setupBufferState
     - glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId)
     - glBufferData(GL_ARRAY_BUFFER, vertexBuffer, usage.id)
  3. uploadIndexBuffer(ds, buffer.indexBuffer()):
     - 如果有显式索引: glBindBuffer + glBufferData → sequentialIndices=null
     - 如果顺序索引: 使用 AutoStorageIndexBuffer(全局共享)
  4. buffer.release()
```

### 7.3 draw / drawWithShader

```java
draw():
  → RenderSystem.drawElements(mode.asGLMode, indexCount, indexType.asGLType)

drawWithShader(modelView, projection, shader):
  → 设置 shader 全部 uniform(ModelView/Projection/Color/Fog/等)
  → shader.apply()
  → draw()
  → shader.clear()
```

`_drawWithShader` 方法直接内联设置 12 个 Sampler + MODEL_VIEW_MATRIX + PROJECTION_MATRIX + COLOR_MODULATOR + GLINT_ALPHA + FOG_START/END/COLOR/SHAPE + TEXTURE_MATRIX + GAME_TIME + SCREEN_SIZE + LINE_WIDTH。

### 7.4 AutoStorageIndexBuffer

顺序渲染时(如 TRIANGLES:0,1,2,3,4,5...),顶点不需要显式索引。RenderSystem 维护一个全局的顺序索引缓冲区,按需扩展。

---

## 8. 关键不变量与约束

| 不变量 | 说明 |
|---|---|
| **渲染线程** | BufferUploader.upload / VertexBuffer.upload / draw 必须在渲染线程 |
| **VertexFormat 不可变** | `ImmutableMap` 驱动,一旦构造不可修改 |
| **begin/end 配对** | BufferBuilder 必须先 `begin()` 才能写入,`building` 互斥检查 |
| **元素完整性** | `endVertex()` 要求 `elementIndex == 0`(所有元素已填) |
| **defaultColor 冲突** | 设置了 defaultColor 后不能再显式调用 `color()` |
| **RenderedBuffer 单次释放** | `release()` 检查 `released` 标记,二次调用抛异常 |
| **BufferBuilder 批清理** | `clear()` 时若 `renderedBufferCount > 0` 发出警告 |
| **LINES/LINE_STRIP 扩展** | `endVertex()` 自动复制前一个顶点(为直线生成两个端点) |
| **QUADS 排序** | 通过 `setQuadSorting(VertexSorting)` → `putSortedQuadIndices` 生成索引重排 |
| **MemoryTracker** | 通过 `MemoryUtil.nmemAlloc/nmemRealloc` 管理 native 内存,非 GC 堆 |

### 8.1 内存流转全路径

```
应用代码
  → VertexConsumer.vertex()/color()/... [CPU 写 ByteBuffer]
  → BufferBuilder.end()  [创建 RenderedBuffer(pointer + DrawState)]
  → BufferSource.endBatch()  [RenderType.end(builder)]
  → BufferUploader.drawWithShader()  [upload 到 GPU]
    → VertexBuffer.upload()  [glBufferData + glVertexAttribPointer]
    → VertexBuffer.drawWithShader()  [glDrawElements]
  → BufferBuilder.releaseRenderedBuffer()  [清理 native 缓冲区]
```

