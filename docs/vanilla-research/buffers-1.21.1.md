# 缓冲区与顶点系统 — 1.21.1 (NeoForge)

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码,所有路径相对于该目录。 源码树由 `scripts/extract-mc-source.py` 重建。
> 全量 `com.mojang.blaze3d.vertex.*` 包 + `net.minecraft.client.renderer.MultiBufferSource`/`BufferUploader`。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [VertexConsumer 链式 API](#2-vertexconsumer-链式-api)
3. [VertexFormat 与元素布局](#3-vertexformat-与元素布局)
4. [ByteBufferBuilder 原生缓冲构建器](#4-bytebufferbuilder-原生缓冲构建器)
5. [BufferBuilder / MeshData 内存模型](#5-bufferbuilder--meshdata-内存模型)
6. [BufferSource 批处理](#6-buffersource-批处理)
7. [BufferUploader GPU 上传与绘制](#7-bufferuploader-gpu-上传与绘制)
8. [VertexBuffer GL 抽象](#8-vertexbuffer-gl-抽象)
9. [关键不变量与约束](#9-关键不变量与约束)

---

## 1. 类位置与职责

| 类 | 路径 | 职责 |
|---|---|---|
| `VertexConsumer` | `com/mojang/blaze3d/vertex/VertexConsumer.java` | 顶点数据写入接口(链式 API,重命名) |
| `BufferBuilder` | `com/mojang/blaze3d/vertex/BufferBuilder.java` | 实现 `VertexConsumer`,使用 `ByteBufferBuilder` 管理内存 |
| `ByteBufferBuilder` | `com/mojang/blaze3d/vertex/ByteBufferBuilder.java` | **新增** native malloc 缓冲区构建器 |
| `MeshData` | `com/mojang/blaze3d/vertex/MeshData.java` | **新增** 替代旧 `BufferBuilder.RenderedBuffer` |
| `VertexFormat` | `com/mojang/blaze3d/vertex/VertexFormat.java` | Builder 模式顶点格式 |
| `VertexFormatElement` | `com/mojang/blaze3d/vertex/VertexFormatElement.java` | record,全局注册的顶点元素 |
| `DefaultVertexFormat` | `com/mojang/blaze3d/vertex/DefaultVertexFormat.java` | 预定义格式常量 |
| `VertexBuffer` | `com/mojang/blaze3d/vertex/VertexBuffer.java` | 保留 GL VAO/VBO/IBO |
| `BufferUploader` | `com/mojang/blaze3d/vertex/BufferUploader.java` | 接受 `MeshData` 替代 `RenderedBuffer` |
| `Tesselator` | `com/mojang/blaze3d/vertex/Tesselator.java` | 单例,持有 `BufferBuilder` |
| `MultiBufferSource.BufferSource` | `net/minecraft/client/renderer/MultiBufferSource.java` | 重构:使用 `ByteBufferBuilder` + `BufferBuilder` 对 |
| `VertexSorting` | `com/mojang/blaze3d/vertex/VertexSorting.java` | 排序接口(elementsFromMask 流) |

### 关键变化 vs 1.20.1

| 删除 | 新增/替换 |
|---|---|
| `BufferVertexConsumer` / `DefaultedVertexConsumer` | `ByteBufferBuilder` |
| `BufferBuilder.RenderedBuffer` + `DrawState` | `MeshData` (持有 `ByteBufferBuilder.Result`) |
| Forge `OnlyIn` | NeoForge `OnlyIn` |
| `vertex(double,...)`, `color(...)`, `uv(...)` 方法名 | `addVertex(float,...)`, `setColor(...)`, `setUv(...)` 重命名 |
| `overlayCoords` / `uv2` | `setOverlay` / `setLight` 语义化重命名 |
| `defaultColor` / `unsetDefaultColor` | `setWhiteAlpha` |

---

## 2. VertexConsumer 链式 API

### 2.1 核心抽象方法(全部 `float` 位置)

```java
VertexConsumer addVertex(float x, float y, float z);           // 开始顶点(必须)
VertexConsumer setColor(int red, int green, int blue, int alpha);
VertexConsumer setUv(float u, float v);
VertexConsumer setUv1(int u, int v);                           // OVERLAY(short,short)
VertexConsumer setUv2(int u, int v);                           // LIGHTMAP(short,short)
VertexConsumer setNormal(float normalX, float normalY, float normalZ);
```

**重要变化**:
- `vertex(double,...)` → `addVertex(float,...)`:位置从 double 变 float,且**必须显式调用**来开始每个顶点
- 不再有 `endVertex()`(自动在 `addVertex` 内处理前一个)
- 不再有 `defaultColor/unsetDefaultColor`

### 2.2 语义化包装方法

```java
default VertexConsumer setLight(int packedLight)     // packed → setUv2
default VertexConsumer setOverlay(int packedOverlay) // packed → setUv1
default VertexConsumer setColor(int color)           // packed ARGB → setColor(r,g,b,a)
default VertexConsumer setColor(float r,float g,float b,float a) // float→int
default VertexConsumer setWhiteAlpha(int alpha)      // setColor(ARGB.color(alpha, -1))
```

### 2.3 批量方法

```java
default void addVertex(float x,float y,float z, int color, float u,float v,
    int packedOverlay, int packedLight, float normalX,float normalY,float normalZ)
// 一次性提交完整顶点

default void putBulkData(PoseStack.Pose pose, BakedQuad quad, ...)
// 批量提交 BakedQuad(与 1.20.1 类似但签名变化)
```

**差异**:`putBulkData` 不再有 `MemoryStack` 内分配 ByteBuffer 读 quad 数据,改为直接从 `BakedQuad.getVertices()` int[] 解析。

### 2.4 矩阵辅助

```java
default VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z)
default VertexConsumer addVertex(Matrix4f pose, float x, float y, float z)
default VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z)
```

---

## 3. VertexFormat 与元素布局

### 3.1 VertexFormatElement(record)

```java
public record VertexFormatElement(int id, int index, Type type, Usage usage, int count)
```

**关键变化**:元素全局注册到 `BY_ID[32]` 数组,通过 `mask()` 位掩码识别。

| 元素 | id | index | Type | Usage | count | byteSize | mask |
|---|---|---|---|---|---|---|---|
| `POSITION` | 0 | 0 | FLOAT | POSITION | 3 | 12 | 1 |
| `COLOR` | 1 | 0 | UBYTE | COLOR | 4 | 4 | 2 |
| `UV0` | 2 | 0 | FLOAT | UV | 2 | 8 | 4 |
| `UV1` | 3 | 1 | SHORT | UV | 2 | 4 | 8 |
| `UV2` | 4 | 2 | SHORT | UV | 2 | 4 | 16 |
| `NORMAL` | 5 | 0 | BYTE | NORMAL | 3 | 3 | 32 |

**删除**:`Usage.PADDING` → 格式构造时用 `VertexFormat.Builder.padding(int)` 显式声明,不在 elements 列表中。

**Usage enum 简化**:不再有 `ClearState` 接口,`clearBufferState` 移至 `VertexFormat`(直接 `GlStateManager._disableVertexAttribArray(i)`)。

`Usage` 为 NeoForge `IExtensibleEnum`(允许模组扩展)。

### 3.2 VertexFormat

```java
public class VertexFormat {
    List<VertexFormatElement> elements;
    List<String> names;
    int vertexSize;
    int elementsMask;                         // 所有元素 mask 的 OR
    int[] offsetsByElement[32];              // 按 element.id 索引的偏移
    VertexBuffer immediateDrawVertexBuffer;
}
```

**Builder 模式**:
```java
VertexFormat.Builder b = VertexFormat.builder()
    .add("Position", POSITION)
    .add("Color", COLOR)
    .padding(1)          // 显式 padding(不在元素列表中)
    .build();
```

**GL 管理**:`setupBufferState()` → 对每个元素先 `_enableVertexAttribArray(j)` 再调用 `element.setupBufferState(j, offset, vertexSize)`; `clearBufferState()` → `_disableVertexAttribArray(i)` 全部。

**IndexType**:去掉 `asGLType` 字段(5123/5125 → 仅 `bytes`=2/4)。

### 3.3 DefaultVertexFormat 格式常量

所有格式通过 `VertexFormat.builder()` 构造,不再有 `ELEMENT_*` 静态常量(直接用 `VertexFormatElement.POSITION` 等)。

| 格式 | 元素(按顺序) | vertexSize | 备注 |
|---|---|---|---|
| `BLOCK` | POSITION + COLOR + UV0 + UV2 + NORMAL + padding(1) | 32 | 同 1.20.1 |
| `NEW_ENTITY` | POSITION + COLOR + UV0 + UV1 + UV2 + NORMAL + padding(1) | 36 | 同 1.20.1 |
| `PARTICLE` | POSITION + UV0 + COLOR + UV2 | 28 | 同 1.20.1 |
| `POSITION` | POSITION | 12 | 同 |
| `POSITION_COLOR` | POSITION + COLOR | 16 | 同 |
| `POSITION_COLOR_NORMAL` | POSITION + COLOR + NORMAL + padding(1) | 20 | 同 |
| `POSITION_COLOR_LIGHTMAP` | POSITION + COLOR + UV2 | 20 | 同 |
| `POSITION_TEX` | POSITION + UV0 | 20 | 同 |
| `POSITION_TEX_COLOR` | POSITION + UV0 + COLOR | 24 | 同 |
| `POSITION_COLOR_TEX_LIGHTMAP` | POSITION + COLOR + UV0 + UV2 | 28 | 同 |
| `POSITION_TEX_LIGHTMAP_COLOR` | POSITION + UV0 + UV2 + COLOR | 28 | 同 |
| `POSITION_TEX_COLOR_NORMAL` | POSITION + UV0 + COLOR + NORMAL + padding(1) | 28 | 同 |
| `BLIT_SCREEN` | POSITION + UV + COLOR | 24 | 保留但 UV=ELEMENT_UV(即 UV0) |

**删除** vs 1.20.1:
- `POSITION_COLOR_TEX`(与 POSITION_TEX_COLOR 重复)
- `ELEMENT_*` 静态常量(改用 `VertexFormatElement.*`)

---

## 4. ByteBufferBuilder 原生缓冲构建器

### 4.1 核心设计

`ByteBufferBuilder` 替代了 `BufferBuilder` 的直接 `ByteBuffer` 字段,提供 **generation-based** 内存管理:

```java
public class ByteBufferBuilder implements AutoCloseable {
    public static final int MAX_GROWTH_SIZE = 2097152;  // 2MB
    public long pointer;           // native malloc 指针
    public int capacity;
    public int writeOffset;        // 当前写入偏移
    public int nextResultOffset;   // 下一个 Result 起始偏移
    public int resultCount;        // 活跃 Result 数量
    public int generation;         // 代数(每次 discard 递增)
}
```

### 4.2 核心操作

```java
// 预留空间,返回 native 指针用于写入
long reserve(int bytes):
  ensureCapacity(writeOffset + bytes)
  offset = writeOffset; writeOffset += bytes
  return pointer + offset

// 创建 Result(标记从 nextResultOffset 到 writeOffset 的区域)
Result build():
  size = writeOffset - nextResultOffset
  if size == 0: return null
  nextResultOffset = writeOffset
  resultCount++
  return new Result(offset, size, generation)

// 清理
discard():        // 压缩内存(discardResults),重置偏移
discardResults(): // memCopy 尾部数据到开头,generation++
close():          // free(pointer)

// 增长(in-place realloc)
ensureCapacity(size):
  if size > capacity:
    newSize = capacity + min(capacity, MAX_GROWTH_SIZE)
    resize(newSize)
```

### 4.3 Result 类

```java
class Result implements AutoCloseable {
    int offset, capacity, generation;
    boolean closed;

    ByteBuffer byteBuffer() → MemoryUtil.memByteBuffer(pointer + offset, capacity)
    close() → freeResult() (resultCount--, 可能触发 discardResults)
}
```

**约束**:`byteBuffer()` 返回的是**指针视图**(不是拷贝),必须在 `Result` 未关闭时使用;调用 `close()` 后指针悬空。

---

## 5. BufferBuilder / MeshData 内存模型

### 5.1 BufferBuilder 结构

```java
public class BufferBuilder implements VertexConsumer {
    public final ByteBufferBuilder buffer;           // 外部传入
    public long vertexPointer = -1L;                 // 当前顶点起始指针
    public int vertices;
    public final VertexFormat format;
    public final VertexFormat.Mode mode;
    public final boolean fastFormat;                  // BLOCK 或 NEW_ENTITY
    public final boolean fullFormat;                  // NEW_ENTITY(含 UV1)
    public final int vertexSize;
    public final int initialElementsToFill;           // format.elementsMask & ~POSITION.mask
    public final int[] offsetsByElement;
    public int elementsToFill;                        // 当前顶点待填充元素 mask
    public boolean building = true;
}
```

构造时要求 `format.contains(POSITION)`。

### 5.2 写入路径

#### beginVertex

```java
long beginVertex():
  endLastVertex()           // 检查前一个顶点并可能复制(LINES 模式)
  vertices++
  return buffer.reserve(vertexSize)  // 在 ByteBufferBuilder 预留空间
```

#### beginElement

```java
long beginElement(VertexFormatElement element):
  elementsToFill &= ~element.mask()   // 清除该元素的 mask 位
  if elementsToFill 未变: return -1L  // 元素不在 format 中
  return vertexPointer + offsetsByElement[element.id()]
```

**检查**:`endLastVertex()` 确保 `elementsToFill == 0`(所有必需元素已填充),否则抛异常列出缺失元素。

#### 快速路径(fastFormat=true)

```java
addVertex(x,y,z, color, u,v, overlay, light, nx,ny,nz):
  if fastFormat:
    long i = beginVertex()
    memPutFloat(i+0,x), memPutFloat(i+4,y), memPutFloat(i+8,z)
    putRgba(i+12,color)         // ARGB→ABGR 字节序转换
    memPutFloat(i+16,u), memPutFloat(i+20,v)
    if fullFormat:
      putPackedUv(i+24,overlay) // UV1
      putPackedUv(i+28,light)   // UV2 (+ normal)
    else:
      putPackedUv(i+24,light)   // UV2 only
  else:
    VertexConsumer.super.addVertex(...)  // 通用路径
```

**putRgba**:
```java
static void putRgba(long pointer, int argb):
  int abgr = FastColor.ABGR32.fromArgb32(argb)  // 字节序转换
  memPutInt(pointer, IS_LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr))
```

**putPackedUv**:
```java
static void putPackedUv(long pointer, int packedUv):
  if IS_LITTLE_ENDIAN: memPutInt(pointer, packedUv)
  else: memPutShort(pointer, lo), memPutShort(pointer+2, hi)
```

### 5.3 build / storeMesh

```java
MeshData build():
  ensureBuilding()
  endLastVertex()
  MeshData mesh = storeMesh()
  building = false; vertexPointer = -1L
  return mesh

MeshData storeMesh():
  if vertices == 0: return null
  ByteBufferBuilder.Result result = buffer.build()   // 创建 Result
  if result == null: return null
  int indexCount = mode.indexCount(vertices)
  IndexType indexType = IndexType.least(vertices)    // 基于顶点数选 SHORT/INT
  return new MeshData(result, new MeshData.DrawState(format,vertices,indexCount,mode,indexType))
```

### 5.4 MeshData

```java
public class MeshData implements AutoCloseable {
    ByteBufferBuilder.Result vertexBuffer;      // 顶点数据
    ByteBufferBuilder.Result indexBuffer;       // 排序索引(可选)
    DrawState drawState;                        // (format, vertexCount, indexCount, mode, indexType)
}
```

**排序**:
```java
MeshData.SortState sortQuads(ByteBufferBuilder bufferBuilder, VertexSorting sorting):
  if mode != QUADS: return null
  unpackQuadCentroids(...)  // 从顶点缓冲提取每 quad 质心
  buildSortedIndexBuffer(...)  // 排序 → 写入索引到 bufferBuilder
  indexBuffer = result
```

**close**:关闭 vertexBuffer + indexBuffer(级联到 ByteBufferBuilder.Result.close → freeResult)。

---

## 6. BufferSource 批处理

### 6.1 结构

```java
class BufferSource implements MultiBufferSource {
    protected final ByteBufferBuilder sharedBuffer;                         // 共享缓冲区
    protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers; // 固定缓冲区池
    protected final Map<RenderType, BufferBuilder> startedBuilders;         // 活跃 builder
    protected RenderType lastSharedType;                                     // 上个共享 RenderType
}
```

**关键变化 vs 1.20.1**:
- 不再直接持有 `BufferBuilder` → 持有 `ByteBufferBuilder`,按需创建 `BufferBuilder` 对
- 用 `SequencedMap`(LinkedHashMap)代替 `Map`,保证 endBatch 遍历顺序
- `lastState Optional<RenderType>` → `lastSharedType RenderType`(非 Optional)

### 6.2 getBuffer 流程

```
getBuffer(renderType):
  1. 从 startedBuilders 取已有 builder
  2. 如果已有且不能合并(nextBuilder 不允许):endBatch,清空 builder
  3. 如果已有:返回 builder(复用)
  4. 创建新 builder:
     - 如果在 fixedBuffers 中有专用 buffer → new BufferBuilder(fixedBuffer, mode, format)
     - 否则:如果 lastSharedType 非空 → endBatch(lastSharedType)
       然后 new BufferBuilder(sharedBuffer, mode, format)
       设置 lastSharedType = renderType
  5. 注册到 startedBuilders[renderType] = builder
  6. 返回 builder
```

### 6.3 endBatch

```java
endBatch():
  endLastBatch()              // 结束共享 builder
  for fixedBuffers.keyset:    // 依次结束固定 buffer
    endBatch(renderType)
      → remove builder from startedBuilders
      → builder.build() → MeshData
      → 如果 sortOnUpload: mesh.sortQuads(buffer, vertexSorting)
      → renderType.draw(mesh)  // 触发 upload + draw
```

---

## 7. BufferUploader GPU 上传与绘制

### 7.1 核心流程(接受 MeshData)

```java
drawWithShader(MeshData mesh):
  → RenderSystem 线程检查
  → _drawWithShader:
    - VertexBuffer vb = upload(mesh)
    - vb.drawWithShader(modelView, projection, shader)

draw(MeshData mesh):
  → VertexBuffer vb = upload(mesh)
  → vb.draw()
```

### 7.2 upload(MeshData)

```java
VertexBuffer upload(MeshData mesh):
  RenderSystem.assertOnRenderThread()
  VertexBuffer vb = format.getImmediateDrawVertexBuffer()
  bindImmediateBuffer(vb)   // 避免重复 bind
  vb.upload(mesh)
  return vb
```

与 1.20.1 的区别:接受 `MeshData` 而非 `RenderedBuffer`,内部自动 close MeshData。

---

## 8. VertexBuffer GL 抽象

### 8.1 结构(与 1.20.1 基本一致)

```java
public class VertexBuffer implements AutoCloseable {
    public final Usage usage;
    public int vertexBufferId, indexBufferId, arrayObjectId;
    public VertexFormat format;
    public AutoStorageIndexBuffer sequentialIndices;
    public IndexType indexType;
    public int indexCount;
    public Mode mode;
}
```

### 8.2 upload(MeshData)

```java
upload(MeshData meshData):
  DrawState ds = meshData.drawState()
  format = uploadVertexBuffer(ds, meshData.vertexBuffer())
    - 如果 format 变化: clearBufferState + setupBufferState
    - glBindBuffer + glBufferData
  sequentialIndices = uploadIndexBuffer(ds, meshData.indexBuffer())
    - 有显式索引: glBindBuffer + glBufferData
    - null: 使用 AutoStorageIndexBuffer
  indexCount = ds.indexCount(); indexType = ds.indexType(); mode = ds.mode()
  meshData.close()  // 自动释放 ByteBufferBuilder.Result
```

### 8.3 drawWithShader 简化

`_drawWithShader` 在 1.21.1 中大幅简化:调用 `shader.setDefaultUniforms(mode, modelView, projection, window)` 统一设置,不再逐个 uniform 内联。

```java
void _drawWithShader(...):
  shader.setDefaultUniforms(mode, modelViewMatrix, projectionMatrix, window)
  shader.apply()
  this.draw()
  shader.clear()
```

---

## 9. 关键不变量与约束

| 不变量 | 说明 |
|---|---|
| **渲染线程** | upload / draw / bind / unbind 必须在渲染线程 |
| **addVertex 必须首调** | 每次 beginElement 前必须先 addVertex |
| **元素完整性** | endLastVertex 检查 `elementsToFill == 0`,否则抛异常列出缺失元素 |
| **format 含 POSITION** | BufferBuilder 构造强制要求 |
| **ByteBufferBuilder generation** | Result.byteBuffer() 检查 generation 有效性,防止 use-after-free |
| **Result 生命周期** | close() 只允许一次,二次调用 no-op |
| **MAX_COUNT=32** | VertexFormatElement 最多 32 个(element mask 是 int) |
| **little-endian 处理** | BufferBuilder 内静态检查 `IS_LITTLE_ENDIAN`,putRgba/putPackedUv 适配 |
| **LINES 模式扩展** | endLastVertex 自动复制前顶点(memCopy) |
| **MAX_GROWTH_SIZE=2MB** | ByteBufferBuilder 最大单次增长 2MB |

### 9.1 内存流转全路径(vs 1.20.1)

```
1.20.1:  BufferBuilder.ByteBuffer → RenderedBuffer → VertexBuffer.upload → GL
1.21.1:  ByteBufferBuilder.Result → MeshData → VertexBuffer.upload → GL
```

**核心差异**:1.21.1 中顶点数据不再由 `BufferBuilder` 直接拥有,而是通过 `ByteBufferBuilder` 的 reserve/build 模式分离管理,`MeshData` 作为中间传递对象,自动管理 `Result` 的生命周期。

