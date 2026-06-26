# 缓冲区与顶点系统 — 26.1.2 (NeoForge)

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码,所有路径相对于该目录。
> 全量 `com.mojang.blaze3d.vertex.*` 包 + `com.mojang.blaze3d.buffers.*` + `net.minecraft.client.renderer.rendertype.RenderType`。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [VertexConsumer 链式 API](#2-vertexconsumer-链式-api)
3. [VertexFormat 与元素布局](#3-vertexformat-与元素布局)
4. [ByteBufferBuilder 升级](#4-bytebufferbuilder-升级)
5. [BufferBuilder / MeshData / CompactVectorArray](#5-bufferbuilder--meshdata--compactvectorarray)
6. [GpuBuffer 抽象(替代 VertexBuffer)](#6-gpubuffer-抽象替代-vertexbuffer)
7. [BufferSource 批处理](#7-buffersource-批处理)
8. [GPU 上传与绘制(RenderType.draw)](#8-gpu-上传与绘制rendertypedraw)
9. [关键不变量与约束](#9-关键不变量与约束)

---

## 1. 类位置与职责

| 类 | 路径 | 职责 | vs 1.21.1 |
|---|---|---|---|
| `VertexConsumer` | `com/mojang/blaze3d/vertex/VertexConsumer.java` | 顶点接口 | 新增 `setLineWidth`, `putBlockBakedQuad`, `putBakedQuad` |
| `BufferBuilder` | `com/mojang/blaze3d/vertex/BufferBuilder.java` | 顶点构建 | 新增 `setLineWidth`, `MAX_VERTEX_COUNT` |
| `ByteBufferBuilder` | `com/mojang/blaze3d/vertex/ByteBufferBuilder.java` | 原生缓冲 | **升级**:long 偏移, maxCapacity, TracyClient |
| `MeshData` | `com/mojang/blaze3d/vertex/MeshData.java` | 网格数据 | `CompactVectorArray` 替代 `Vector3f[]` |
| `CompactVectorArray` | `com/mojang/blaze3d/vertex/CompactVectorArray.java` | **新** float[] 阵列 | 紧凑存储排序点 |
| `VertexFormat` | `com/mojang/blaze3d/vertex/VertexFormat.java` | 顶点格式 | **新增** `uploadImmediateVertexBuffer`/`uploadImmediateIndexBuffer` |
| `VertexFormatElement` | `com/mojang/blaze3d/vertex/VertexFormatElement.java` | 顶点元素 | **简化**:删除 `Usage` enum+GL lambda, 新增 `normalized`, `LINE_WIDTH` |
| `DefaultVertexFormat` | `com/mojang/blaze3d/vertex/DefaultVertexFormat.java` | 预定义格式 | `NEW_ENTITY`→`ENTITY`, `EMPTY`, LINE_WIDTH 格式 |
| `GpuBuffer` | `com/mojang/blaze3d/buffers/GpuBuffer.java` | **新** GPU 缓冲区抽象 | 替代 `VertexBuffer` |
| `GpuBufferSlice` | `com/mojang/blaze3d/buffers/GpuBufferSlice.java` | GpuBuffer 切片 | Buffer 子范围引用 |
| `BufferUploader` | `com/mojang/blaze3d/vertex/BufferUploader.java` | **已删除** | 上传统一移至 `RenderType.draw` |
| `VertexBuffer` | `com/mojang/blaze3d/vertex/VertexBuffer.java` | **已删除** | 由 `GpuBuffer` 替代 |
| `MultiBufferSource.BufferSource` | `net/minecraft/client/renderer/MultiBufferSource.java` | 缓冲区管理 | `sortOnUpload` 参数变化(`vertexSorting`) |

### 关键架构变化

| 概念 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| GPU Buffer 抽象 | `VertexBuffer`(GL VAO+VBO,含 usage enum) | `GpuBuffer`(抽象类,含 `@Usage int`) |
| Upload 触发者 | `BufferUploader` 静态方法 | `RenderType.draw(MeshData)` 内联 |
| Upload API | `VertexBuffer.upload(RenderedBuffer/MeshData)` | `VertexFormat.uploadImmediateVertexBuffer(ByteBuffer)` → `GpuBuffer` |
| Index Buffer | `id` (GL int) | `GpuBuffer` 引用 |
| Draw call | `VertexBuffer.draw()` → `glDrawElements` | `RenderPass.drawIndexed(0, 0, indexCount, 1)` |
| State binding | `VertexFormat.setupBufferState()` → GL VAO | `RenderPass.setVertexBuffer()` + Pipeline |
| Immediate VB cache | `VertexBuffer(DYNAMIC)` per format | `GpuBuffer` per format |

---

## 2. VertexConsumer 链式 API

### 2.1 核心方法(同 1.21.1 命名)

```java
VertexConsumer addVertex(float x, float y, float z);
VertexConsumer setColor(int r, int g, int b, int a);
VertexConsumer setColor(int color);                      // 新增 packed int 重载
VertexConsumer setUv(float u, float v);
VertexConsumer setUv1(int u, int v);
VertexConsumer setUv2(int u, int v);
VertexConsumer setNormal(float x, float y, float z);
VertexConsumer setLineWidth(float width);                // 新增
```

### 2.2 新增方法

```java
default void putBlockBakedQuad(float x,float y,float z, BakedQuad quad, QuadInstance instance)
default void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance)
```

**设计**:用 `QuadInstance` 替代旧 `putBulkData` 的多参数,封装 `getColor(vertex)`, `overlayCoords`, `getLightCoordsWithEmission` 等。

`addVertexWith2DPose(Matrix3x2fc pose, float x, float y)`:为 GUI 2D 渲染添加的辅助方法,`z=0`。

### 2.3 删除 vs 1.21.1

- `putBulkData(PoseStack.Pose, BakedQuad, ...)` 删除(由 `putBakedQuad` 替代)
- `setWhiteAlpha` 删除
- `addVertex(Vector3f)` → `addVertex(Vector3fc)` (interface 不可变)

---

## 3. VertexFormat 与元素布局

### 3.1 VertexFormatElement(record) 重大简化

```java
public record VertexFormatElement(int id, int index, Type type, boolean normalized, int count)
```

**删除**:`Usage` 枚举 → GL 绑定逻辑移至 Pipeline 层。
**删除**:`supportsUsage` 校验 → 注册时不再检查重复(由 Pipeline 保证正确性)。

**新增**:`normalized` boolean 字段(原隐含在 Usage 的 GL lambda 中,现显式声明)。

| 元素 | id | index | Type | normalized | count | byteSize |
|---|---|---|---|---|---|---|
| `POSITION` | 0 | 0 | FLOAT | false | 3 | 12 |
| `COLOR` | 1 | 0 | UBYTE | **true** | 4 | 4 |
| `UV0` | 2 | 0 | FLOAT | false | 2 | 8 |
| `UV1` | 3 | 1 | SHORT | false | 2 | 4 |
| `UV2` | 4 | 2 | SHORT | false | 2 | 4 |
| `NORMAL` | 5 | 0 | BYTE | **true** | 3 | 3 |
| `LINE_WIDTH` | **6** | 0 | FLOAT | false | **1** | **4** |

**Type enum 简化**:删除 `glType` 字段(不再需要直接 GL 类型枚举),仅保留 `size` + `name`。

### 3.2 VertexFormat

```java
public class VertexFormat {
    private static final int VERTEX_ALIGNMENT = 4;
    List<VertexFormatElement> elements;
    List<String> names;
    int vertexSize;
    int elementsMask;
    int[] offsetsByElement[32];
    @Nullable GpuBuffer immediateDrawVertexBuffer;      // 替代 VertexBuffer
    @Nullable GpuBuffer immediateDrawIndexBuffer;        // 新增
}
```

**删除**:`setupBufferState()` / `clearBufferState()` → Pipeline 管理。
**新增**:

```java
// CPU ByteBuffer → GPU
public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer buffer);
public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer buffer);
```

内部 `uploadToBuffer(target, buffer, usage, label)`:

```java
private static GpuBuffer uploadToBuffer(target, buffer, usage, label):
  GpuDevice device = RenderSystem.getDevice()
  if GraphicsWorkarounds.alwaysCreateFreshImmediateBuffer():
    if target != null: target.close()
    return device.createBuffer(label, usage, buffer)
  else:
    if target == null:
      target = device.createBuffer(label, usage, buffer)
    else:
      CommandEncoder encoder = device.createCommandEncoder()
      if target.size() < buffer.remaining():
        target.close()
        target = device.createBuffer(label, usage, buffer)
      else:
        encoder.writeToBuffer(target.slice(), buffer)
    return target
```

**Usage 值**:vertex=`40`(USAGE_VERTEX=32 | USAGE_COPY_DST=8), index=`72`(USAGE_INDEX=64 | USAGE_COPY_DST=8)。

### 3.3 Builder 模式

```java
VertexFormat.Builder b = VertexFormat.builder()
    .add("Position", POSITION)
    .add("Color", COLOR)
    .padding(1)    // 显式 padding
    .build()

// build() 验证: vertexSize 必须是 4 的倍数,否则 IllegalStateException
if (!Mth.isMultipleOf(vertexSize, 4))
    throw new IllegalStateException(...)
```

### 3.4 Mode 简化

| Mode | primitiveLength | primitiveStride | connectedPrimitives |
|---|---|---|---|
| `LINES` | 2 | 2 | false |
| `DEBUG_LINES` | 2 | 2 | false |
| `DEBUG_LINE_STRIP` | 2 | 1 | true |
| `POINTS` | **1** | 1 | false |
| `TRIANGLES` | 3 | 3 | false |
| `TRIANGLE_STRIP` | 3 | 1 | true |
| `TRIANGLE_FAN` | 3 | 1 | true |
| `QUADS` | 4 | 4 | false |

**删除**:`asGLMode` 字段(GLMODE 映射移至 Pipeline)。
**删除**:`LINE_STRIP` 模式。
**新增**:`POINTS` 模式。

### 3.5 IndexType 简化

```java
enum IndexType {
    SHORT(2), INT(4);  // 仅保留 bytes,删除 asGLType
}
```

### 3.6 DefaultVertexFormat 格式常量

| 格式 | 元素 | vertexSize | 备注 |
|---|---|---|---|
| `EMPTY` | (空) | 0 | **新增** |
| `BLOCK` | POSITION + COLOR + UV0 + UV2 | 28 | **去掉** NORMAL+PADDING |
| `ENTITY` | POSITION + COLOR + UV0 + UV1 + UV2 + NORMAL + padding(1) | 36 | **重命名** NEW_ENTITY → ENTITY |
| `PARTICLE` | POSITION + UV0 + COLOR + UV2 | 28 | 不变 |
| `POSITION` | POSITION | 12 | 不变 |
| `POSITION_COLOR` | POSITION + COLOR | 16 | 不变 |
| `POSITION_COLOR_NORMAL` | POSITION + COLOR + NORMAL + padding(1) | 20 | 不变 |
| `POSITION_COLOR_LIGHTMAP` | POSITION + COLOR + UV2 | 20 | 不变 |
| `POSITION_TEX` | POSITION + UV0 | 20 | 不变 |
| `POSITION_TEX_COLOR` | POSITION + UV0 + COLOR | 24 | 不变 |
| `POSITION_COLOR_TEX_LIGHTMAP` | POSITION + COLOR + UV0 + UV2 | 28 | 不变 |
| `POSITION_TEX_LIGHTMAP_COLOR` | POSITION + UV0 + UV2 + COLOR | 28 | 不变 |
| `POSITION_TEX_COLOR_NORMAL` | POSITION + UV0 + COLOR + NORMAL + padding(1) | 28 | 不变 |
| `POSITION_COLOR_LINE_WIDTH` | POSITION + COLOR + LINE_WIDTH | **20** | **新增** |
| `POSITION_COLOR_NORMAL_LINE_WIDTH` | POSITION + COLOR + NORMAL + padding(1) + LINE_WIDTH | **24** | **新增** |

**关键差异 vs 1.21.1**:
- `BLOCK`: vertexSize 从 32→28(去掉 Normal + Padding)
- `NEW_ENTITY` → `ENTITY`
- `BLIT_SCREEN` 删除,`POSITION_COLOR_TEX` 删除
- 新增 `EMPTY` + 2 个 LINE_WIDTH 格式
- LINE_WIDTH 元素: `register(6, 0, FLOAT, false, 1)` 4 bytes

---

## 4. ByteBufferBuilder 升级

### 4.1 核心变化

```java
public class ByteBufferBuilder implements AutoCloseable {
    public static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("ByteBufferBuilder");
    public static final long DEFAULT_MAX_CAPACITY = 4294967295L;  // 4GB
    public long pointer;               // int→long (支持 >2GB)
    public long capacity;              // int→long
    public final long maxCapacity;     // 新增上限
    public long writeOffset;           // int→long
    public long nextResultOffset;      // int→long
    public int resultCount;
    public int generation;
}
```

**升级点**:
- 所有偏移/容量从 `int` → `long`(支持 >2GB 缓冲区)
- 新增 `maxCapacity` 上限检查
- 新增 `TracyClient` 内存追踪
- 新增 `exactlySized(int)` 工厂方法(固定大小,禁止增长)

```java
static ByteBufferBuilder exactlySized(int capacity):
  new ByteBufferBuilder(capacity, capacity)  // initialCapacity == maxCapacity
```

### 4.2 reserve 变化

```java
long reserve(int size):
  long offset = writeOffset
  long next = Math.addExact(offset, (long)size)  // 溢出检查
  ensureCapacity(next)
  writeOffset = next
  return Math.addExact(pointer, offset)
```

### 4.3 Result

```java
class Result implements AutoCloseable {
    public final long offset;    // int→long
    public final int capacity;   // int(Result 内部仍然是 int,限制 2^31-1)
    public final int generation;
    boolean closed;
}
```

`build()` 检查 `size > 2147483647`(2^31-1)抛异常。

---

## 5. BufferBuilder / MeshData / CompactVectorArray

### 5.1 BufferBuilder 变化

```java
public class BufferBuilder implements VertexConsumer {
    public static final int MAX_VERTEX_COUNT = 16777215;  // 新增上限
    // ... 与 1.21.1 基本一致
}
```

**新增**:`setLineWidth(float)` 实现:

```java
public VertexConsumer setLineWidth(float width):
  long pointer = beginElement(LineWidth)
  if pointer != -1L:
    memPutFloat(pointer, width)
```

**fastFormat 检测**:使用 `DefaultVertexFormat.ENTITY` 而非 `NEW_ENTITY`。

**addVertex 快速路径**:在 26.1.2 中,BLOCK 格式不再有 NORMAL 元素,快速路径在 BLOCK 模式(fullFormat=false)下只写 Position+Color+UV0+UV2,不写 normal。

### 5.2 MeshData

与 1.21.1 结构一致,但 `SortState` 使用 `CompactVectorArray`:

```java
public record SortState(CompactVectorArray centroids, IndexType indexType) {
    // buildSortedIndexBuffer: 使用 CompactVectorArray.getX/Y/Z 提取质心
}
```

### 5.3 CompactVectorArray(新增)

```java
public class CompactVectorArray {
    private final float[] contents;  // 长度 = 3 * count

    void set(int index, float x, float y, float z);
    Vector3f get(int index, Vector3f output);
    float getX(int index), getY(int index), getZ(int index);
}
```

替代 1.21.1 的 `Vector3f[]`,使用原生 `float[]` 避免 JOML 对象分配。

---

## 6. GpuBuffer 抽象(替代 VertexBuffer)

### 6.1 GpuBuffer 定义

```java
public abstract class GpuBuffer implements AutoCloseable {
    // Usage 位掩码
    USAGE_MAP_READ         = 1
    USAGE_MAP_WRITE        = 2
    USAGE_HINT_CLIENT_STORAGE = 4
    USAGE_COPY_DST         = 8
    USAGE_COPY_SRC         = 16
    USAGE_VERTEX           = 32
    USAGE_INDEX            = 64
    USAGE_UNIFORM          = 128
    USAGE_UNIFORM_TEXEL_BUFFER = 256

    @Usage int usage;
    long size;

    abstract boolean isClosed();
    abstract void close();

    GpuBufferSlice slice(long offset, long length);   // 切片引用
    GpuBufferSlice slice();                           // 全量切片

    interface MappedView extends AutoCloseable {      // 映射视图
        ByteBuffer data();
    }
};
```

### 6.2 GpuBufferSlice

```java
GpuBufferSlice(GpuBuffer parent, long offset, long length)
// 封装 (parent, offset, length), 用于 sub-range 操作
```

### 6.3 上传路径(CPU → GPU)

```
MeshData.vertexBuffer() → ByteBuffer (CPU)
  → VertexFormat.uploadImmediateVertexBuffer(byteBuffer) → GpuBuffer (GPU)
    → device.createBuffer(label, usage=USAGE_VERTEX|COPY_DST, buffer)
      或 encoder.writeToBuffer(target.slice(), buffer)
```

### 6.4 绘制路径

在 `RenderType.draw` 内:
```
RenderPass draw:
  renderPass.setPipeline(pipeline)
  renderPass.setVertexBuffer(0, vertices)         // GpuBuffer
  renderPass.setIndexBuffer(indices, indexType)   // GpuBuffer
  renderPass.drawIndexed(0, 0, indexCount, 1)
```

**对比旧 VertexBuffer**:
- 旧:`glBindVertexArray(vaoId)` → `glDrawElements`
- 新:`RenderPass.setVertexBuffer` + `setIndexBuffer` + `drawIndexed`
- GpuBuffer 是 RenderPass/CommandEncoder 的抽象,不再直接操作 GL ID

---

## 7. BufferSource 批处理

与 1.21.1 结构基本一致,差异:

```java
// 排序时的 vertexSorting 来源不同
mesh.sortQuads(buffer, RenderSystem.getProjectionType().vertexSorting());
// 1.21.1: RenderSystem.getVertexSorting()
```

---

## 8. GPU 上传与绘制(RenderType.draw)

### 8.1 核心流程

```java
RenderType.draw(MeshData mesh):
  1. 处理 modelViewStack(layeringTransform)
  2. DynamicTransforms uniform 写入
  3. 上传顶点:
     vertices = format.uploadImmediateVertexBuffer(mesh.vertexBuffer())
  4. 索引处理:
     if mesh.indexBuffer() == null:
       indices = AutoStorageIndexBuffer.getBuffer(indexCount)
       indexType = autoIndex.type()
     else:
       indices = format.uploadImmediateIndexBuffer(mesh.indexBuffer())
       indexType = mesh.drawState().indexType()
  5. 创建 RenderPass:
     encoder.createRenderPass(label, colorTexture, depthTexture)
  6. 设置 Pipeline + VertexBuffer + Textures + IndexBuffer
  7. drawIndexed(0, 0, indexCount, 1)
  8. mesh.close()
```

### 8.2 整体架构图

```
应用层              CPU 缓冲区层           GPU 抽象层             Pipeline 层
──────              ──────────             ─────────              ──────────
VertexConsumer  →  BufferBuilder  →  MeshData  →  GpuBuffer  →  RenderPass
  .addVertex()      .beginVertex()     .vertexBuf()   (GPU Buffer)  .setVertexBuf()
  .setColor()       .reserve()         .indexBuf()                   .setIndexBuf()
  .setUv()          .build()           .close()                      .drawIndexed()
  ...
```

### 8.3 BufferUploader / VertexBuffer 删除

`BufferUploader` 类完全不存在于 26.1.2 源码中。`VertexBuffer` 类也不存在。相关功能分解到:
- **Upload**:`VertexFormat.uploadImmediateVertexBuffer/IndexBuffer`(管理 GpuBuffer 复用)
- **Draw**:`RenderType.draw`(内联渲染流程)
- **Buffer 生命周期**:`GpuBuffer` + `GpuBufferSlice`(抽象层)

---

## 9. 关键不变量与约束

| 不变量 | 说明 |
|---|---|
| **VertexSize 对齐** | Builder.build() 验证 vertexSize 必须是 4 的倍数 |
| **MAX_VERTEX_COUNT** | BufferBuilder 最多 16777215 个顶点 |
| **ByteBufferBuilder max capacity** | 默认 4GB, reserve 时 Math.addExact 溢出检查 |
| **Result capacity** | 最大 2^31-1 字节(>此值 build 抛异常) |
| **GpuBuffer Usage 标记** | 不可变的 usage int,构造时设定 |
| **VertexFormatElement id 范围** | 0~31,全局注册,重复注册抛异常 |
| **COLOR normalized=true** | COLOR 元素标记为 normalized(0-255 → 0.0-1.0) |
| **NORMAL normalized=true** | NORMAL 元素标记为 normalized(-128~127 → -1.0~1.0) |
| **LINE_WIDTH element** | id=6 新增元素,1 float(4 bytes), not normalized |
| **BLOCK 格式无 Normal** | 26.1.2 的 BLOCK vertexSize=28,去掉了 Normal + Padding |
| **ENTITY 格式保留 Normal** | ENTITY(原 NEW_ENTITY) vertexSize=36,保留完整布局 |
| **GraphicsWorkarounds** | uploadToBuffer 考虑 GPU 兼容性(fresh buffer 策略) |

### 9.1 CPU→GPU 完整路径(26.1.2)

```
VertexConsumer.addVertex/setColor/...
    ↓ (通过 beginVertex → reserve → memPutFloat/memPutByte...)
ByteBufferBuilder (native malloc heap)
    ↓ (build → Result, 标记边界)
MeshData (Result + DrawState)
    ↓ (vertexBuffer() 返回 ByteBuffer)
VertexFormat.uploadImmediateVertexBuffer(ByteBuffer)
    ↓ (GpuDevice.createBuffer 或 encoder.writeToBuffer)
GpuBuffer (GPU VRAM)
    ↓ (RenderPass.setVertexBuffer)
RenderPass.drawIndexed()
    ↓
GPU 执行 draw call
```

