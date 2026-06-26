# Cross-Version 缓冲区与顶点系统对比 (1.20.1 / 1.21.1 / 26.1.2)

> 三个版本缓冲区与顶点系统的横向对比。详细单版分析见 `buffers-1.20.1.md` / `buffers-1.21.1.md` / `buffers-26.1.2.md`。

## 目录

1. [类/包变化表](#1-类包变化表)
2. [VertexConsumer API 演进](#2-vertexconsumer-api-演进)
3. [VertexFormatElement 演进](#3-vertexformatelement-演进)
4. [VertexFormat 演进](#4-vertexformat-演进)
5. [DefaultVertexFormat 格式常量演进](#5-defaultvertexformat-格式常量演进)
6. [内存模型演进](#6-内存模型演进)
7. [GPU Buffer 抽象演进](#7-gpu-buffer-抽象演进)
8. [Upload/Draw 路径演进](#8-uploaddraw-路径演进)
9. [BufferSource 批处理演进](#9-buffersource-批处理演进)
10. [关键不变量跨版本对比](#10-关键不变量跨版本对比)

---

## 1. 类/包变化表

| 职责 | 1.20.1 (Forge) | 1.21.1 (NeoForge) | 26.1.2 (NeoForge) |
|---|---|---|---|
| **顶点接口** | `VertexConsumer` (mutable chain,doubles) | `VertexConsumer` (renamed methods,floats) | `VertexConsumer` (adds setLineWidth,putBakedQuad) |
| **中间层** | `BufferVertexConsumer` + `DefaultedVertexConsumer` | **已删除** | **不存在** |
| **CPU 缓冲器** | `BufferBuilder`(直接持有 ByteBuffer) | `BufferBuilder`(使用 ByteBufferBuilder) | `BufferBuilder`(使用 ByteBufferBuilder,MAX_VERTEX_COUNT) |
| **内存构建器** | **不存在**(BufferBuilder 自管理) | `ByteBufferBuilder`(int 偏移,generation) | `ByteBufferBuilder`(long 偏移,maxCapacity,Tracy) |
| **渲染结果** | `BufferBuilder.RenderedBuffer` + `DrawState` | `MeshData` + `MeshData.DrawState` | `MeshData` + `MeshData.DrawState`(CompactVectorArray) |
| **排序数据** | `Vector3f[]` | `Vector3f[]` | `CompactVectorArray` |
| **GPU Buffer** | `VertexBuffer`(GL VAO+VBO+IBO) | `VertexBuffer`(GL VAO+VBO+IBO,简化 drawWithShader) | `GpuBuffer`(抽象) + `GpuBufferSlice` |
| **Upload 调度** | `BufferUploader` | `BufferUploader`(接受 MeshData) | **已删除**(合并到 RenderType.draw) |
| **顶点格式** | `VertexFormat`(ImmutableMap) | `VertexFormat`(Builder + mask) | `VertexFormat`(Builder + uploadImmediate*) |
| **顶点元素** | `VertexFormatElement`(class,Usage GL lambda) | `VertexFormatElement`(record,Usage NEextensible) | `VertexFormatElement`(record,no Usage,normalized) |
| **预定义格式** | `DefaultVertexFormat`(16 个格式+7 个 ELEMENT_*) | `DefaultVertexFormat`(使用 builder) | `DefaultVertexFormat`(NEW_ENTITY→ENTITY,block 减 Normal) |
| **批处理** | `MultiBufferSource.BufferSource`(BufferBuilder) | `BufferSource`(ByteBufferBuilder+BuffBuilder) | `BufferSource`(同,排序参数 change) |
| **RenderType** | `client.renderer.RenderType` | 同 | `client.renderer.rendertype.RenderType`(draw 内联上传) |

---

## 2. VertexConsumer API 演进

### 2.1 方法名对照

| 功能 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 开始顶点 | `vertex(double,double,double)` | `addVertex(float,float,float)` | `addVertex(float,float,float)` |
| 颜色(RGBA) | `color(int,int,int,int)` | `setColor(int,int,int,int)` | `setColor(int,int,int,int)` |
| 颜色(ARGB) | `color(int)` default | `setColor(int)` default | `setColor(int)` **abstract** |
| 纹理UV | `uv(float,float)` | `setUv(float,float)` | `setUv(float,float)` |
| Overlay | `overlayCoords(int,int)` | `setUv1(int,int)` | `setUv1(int,int)` |
| Lightmap | `uv2(int,int)` | `setUv2(int,int)` | `setUv2(int,int)` |
| 法线 | `normal(float,float,float)` | `setNormal(float,float,float)` | `setNormal(float,float,float)` |
| 结束顶点 | `endVertex()` void | (automatic) | (automatic) |
| 默认颜色 | `defaultColor`/`unsetDefaultColor` | **删除** | **不存在** |
| 白Alpha | — | `setWhiteAlpha(int)` | **删除** |
| 线宽 | — | — | `setLineWidth(float)` **新增** |
| BakedQuad | `putBulkData(Pose,BakedQuad,...)` | `putBulkData(Pose,BakedQuad,...)` | `putBakedQuad(Pose,BakedQuad,QuadInstance)` **新增** |

### 2.2 位置类型

| 版本 | 位置参数类型 | 原因 |
|---|---|---|
| 1.20.1 | `double` | 兼容旧代码 |
| 1.21.1 | `float` | 4→8 bytes 节省,精度足够 |
| 26.1.2 | `float` | 同 1.21.1 |

### 2.3 API "mutable vs immutable" 问题

**1.20.1**:接口完全 mutable — 所有写入方法改变 VertexConsumer 内部状态(BufferBuilder.bytes[]),返回 this 链式。
**1.21.1/26.1.2**:同样 mutable — BufferBuilder 仍是唯一实现,状态保存在 native heap(ByteBufferBuilder)。**没有引入 immutable 分支接口**(如某些 NeoForge 文档可能暗示的 `ImmutableVertexConsumer`)。

但 VertexConsumer 接口签名暗示了"mutable"设计:每个 setter 返回 `this`(允许链式 `consumer.addVertex(...).setColor(...).setUv(...)`)。

---

## 3. VertexFormatElement 演进

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| **类型** | `class` (4 fields) | `record` (5 fields) | `record` (5 fields) |
| **注册** | 无(即时构造为 static final) | `register(id,...)`→BY_ID 数组 | `register(id,...)`→BY_ID 数组 |
| **Usage 枚举** | POSITION/NORMAL/COLOR/UV/PADDING/GENERIC | POSITION/NORMAL/COLOR/UV/GENERIC | **删除**(normalized 替代) |
| **Usage GL** | 每个 Usage 内嵌 GL lambda | Usage 内嵌 SetupState lambda | 无(Pipeline 管理) |
| **PADDING** | Usage.PADDING(type=BYTE,count=1) | 删除(显式 Builder.padding()) | 不存在 |
| **LINE_WIDTH** | 不存在 | 不存在 | id=6, FLOAT, count=1, normalized=false |
| **normalized** | 隐式(在 GL lambda 中:POSITION false,COLOR true,NORMAL true) | 隐式(在 GL lambda 中) | **显式 boolean 字段** |
| **Type.glType** | 存在(5126/5121/5123 等) | 存在 | **删除** |
| **MAX_COUNT** | 无硬限制(ImmutableMap) | 32 | 32 |
| **mask()** | 不存在 | `1 << id` | `1 << id` |

### 3.1 元素跨版本对比

| 元素 | 1.20.1 构造 | 1.21.1 构造 | 26.1.2 构造 |
|---|---|---|---|
| POSITION | `new VFE(0,FLOAT,POSITION,3)` | `register(0,0,FLOAT,POSITION,3)` | `register(0,0,FLOAT,false,3)` |
| COLOR | `new VFE(0,UBYTE,COLOR,4)` | `register(1,0,UBYTE,COLOR,4)` | `register(1,0,UBYTE,**true**,4)` |
| UV0 | `new VFE(0,FLOAT,UV,2)` | `register(2,0,FLOAT,UV,2)` | `register(2,0,FLOAT,false,2)` |
| UV1 | `new VFE(1,SHORT,UV,2)` | `register(3,1,SHORT,UV,2)` | `register(3,1,SHORT,false,2)` |
| UV2 | `new VFE(2,SHORT,UV,2)` | `register(4,2,SHORT,UV,2)` | `register(4,2,SHORT,false,2)` |
| NORMAL | `new VFE(0,BYTE,NORMAL,3)` | `register(5,0,BYTE,NORMAL,3)` | `register(5,0,BYTE,**true**,3)` |
| PADDING | `new VFE(0,BYTE,PADDING,1)` | 不存在 | 不存在 |
| LINE_WIDTH | 不存在 | 不存在 | `register(6,0,FLOAT,false,1)` |

---

## 4. VertexFormat 演进

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| **构造** | `new VertexFormat(ImmutableMap)` | `VertexFormat.builder().add(...).build()` | 同 1.21.1 |
| **元素存储** | `ImmutableList<Element>` | `List<Element>` | `List<Element>` |
| **偏移访问** | `offsets[index]` 按序 | `offsetsByElement[element.id()]` | 同 |
| **GL 管理** | `setupBufferState`/`clearBufferState` | 同但自行调用 attrib 开关 | **删除** |
| **上传方法** | `getImmediateDrawVertexBuffer()`→VertexBuffer | `getImmediateDrawVertexBuffer()`→VertexBuffer | `uploadImmediateVertexBuffer(ByteBuffer)`→GpuBuffer + `uploadImmediateIndexBuffer(ByteBuffer)`→GpuBuffer |
| **Mode 枚举** | 8 种(含 LINES/LINE_STRIP/POINTS 否) | 8 种 | 8 种(减 LINE_STRIP,加 POINTS) |
| **Mode.asGLMode** | 存在(4,5,1,3,4,5,6,4) | 存在 | **删除** |
| **IndexType.asGLType** | 存在(5123,5125) | 存在 | **删除** |
| **elementsMask** | 无 | int(bitmask) | int(bitmask) |
| **contains()** | 无 | `(elementsMask & element.mask()) != 0` | 同 |
| **对齐验证** | 无 | 无 | **build() 验证 vertexSize % 4 == 0** |

---

## 5. DefaultVertexFormat 格式常量演进

### 5.1 一直存在的格式

| 格式 | 1.20.1 vertexSize | 1.21.1 | 26.1.2 | 元素变化 |
|---|---|---|---|---|
| `POSITION` | 12 | 12 | 12 | 无变化 |
| `POSITION_COLOR` | 16 | 16 | 16 | 无变化 |
| `POSITION_COLOR_NORMAL` | 20 | 20 | 20 | 无变化 |
| `POSITION_COLOR_LIGHTMAP` | 20 | 20 | 20 | 无变化 |
| `POSITION_TEX` | 20 | 20 | 20 | 无变化 |
| `POSITION_TEX_COLOR` | 24 | 24 | 24 | 无变化 |
| `POSITION_COLOR_TEX_LIGHTMAP` | 28 | 28 | 28 | 无变化 |
| `POSITION_TEX_LIGHTMAP_COLOR` | 28 | 28 | 28 | 无变化 |
| `POSITION_TEX_COLOR_NORMAL` | 28 | 28 | 28 | 无变化 |

### 5.2 有变化的格式

| 格式 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| `BLIT_SCREEN` | POSITION+UV+COLOR(24) | POSITION+UV+COLOR(24) | **删除** |
| `BLOCK` | POSITION+COLOR+UV0+UV2+NORMAL+PADDING(32) | 同(32) | POSITION+COLOR+UV0+UV2(**28**) |
| `NEW_ENTITY` | POSITION+COLOR+UV0+UV1+UV2+NORMAL+PADDING(36) | 同(36) | →`ENTITY` 同(36) |
| `PARTICLE` | 28 | 28 | 28(无变化) |
| `POSITION_COLOR_TEX` | POSITION+COLOR+UV0(24) | 同(24) | **删除**(与 POSITION_TEX_COLOR 重复) |
| `EMPTY` | 不存在 | 不存在 | **(空) 0 bytes** |
| `POSITION_COLOR_LINE_WIDTH` | 不存在 | 不存在 | POSITION+COLOR+LINE_WIDTH(**20**) |
| `POSITION_COLOR_NORMAL_LINE_WIDTH` | 不存在 | 不存在 | POSITION+COLOR+NORMAL+PADDING+LINE_WIDTH(**24**) |

### 5.3 元素定义方式

| 版本 | 方式 |
|---|---|
| 1.20.1 | `new VertexFormat(ImmutableMap.builder().put("Name", ELEMENT_XXX).build())` |
| 1.21.1 | `VertexFormat.builder().add("Name", VertexFormatElement.XXX).build()` |
| 26.1.2 | `VertexFormat.builder().add("Name", VertexFormatElement.XXX).build()` |

### 5.4 BLOCK 格式退化的影响

26.1.2 的 BLOCK vertexSize 28(vs 旧的 32),移除了 NORMAL+PADDING:
- 方块无法在 GPU 端做逐顶点法线插值(改为 shader 面法线推断)
- `BufferBuilder` 的 `fastFormat` 检测到 `format == DefaultVertexFormat.BLOCK` 时,不再写入 Normal 字节

### 5.5 ENTITY 格式(NEW_ENTITY 重命名)

26.1.2 保持 36 bytes:
```
Position(12) + Color(4) + UV0(8) + UV1(4) + UV2(4) + Normal(3) + Padding(1) = 36
```
实体渲染仍然需要 Normal 用于光照计算。

---

## 6. 内存模型演进

### 6.1 三层演进

| 版本 | 数据持有 | 中间对象 | 生命周期 |
|---|---|---|---|
| 1.20.1 | `BufferBuilder.ByteBuffer`(类字段) | `RenderedBuffer`(pointer+DrawState) | `release()` 递减计数器 |
| 1.21.1 | `ByteBufferBuilder`(外部传入) | `MeshData`(Result+DrawState) | `close()`→`freeResult()`→`discardResults()` |
| 26.1.2 | `ByteBufferBuilder`(外部传入,long 偏移) | `MeshData`(Result+DrawState) | `close()`→`freeResult()`→`discardResults()` |

### 6.2 字节流

**1.20.1**:
```
BufferBuilder.buffer (native ByteBuffer,容量按需 2MB 粒度增长)
  → Forge putBulkData 直接 put ByteBuffer
  → begin→vertex→color→...→endVertex 逐字节 put
  → end() → RenderedBuffer(pointer) → vertexBuffer()/indexBuffer() 返回 memSlice
```

**1.21.1**:
```
ByteBufferBuilder (native malloc pointer)
  → reserve(bytes) → 返回写入指针, ensureCapacity 按需 realloc
  → beginVertex→beginElement→memPut* 直接操作指针
  → build() → Result(offset,size,generation) → byteBuffer() 返回 memByteBuffer
  → storeMesh() → MeshData(Result+DrawState)
```

**26.1.2**:
```
ByteBufferBuilder (native malloc pointer, long 偏移, maxCapacity)
  → reserve(int) → Math.addExact + ensureCapacity(long)
  → 同 1.21.1 的 beginVertex/beginElement/memPut* 模式
  → build() → Result(long offset, int size)
  → storeMesh() → MeshData(Result+DrawState)
```

### 6.3 增长策略对比

| 版本 | 机制 | 步长 |
|---|---|---|
| 1.20.1 | `roundUp(x)` → `x + GROWTH_SIZE - (x % GROWTH_SIZE)` | `2097152`(2MB) |
| 1.21.1 | `maxCapacity(不检查)`, `newSize = capacity + min(capacity, 2MB)` | `min(capacity, 2MB)` |
| 26.1.2 | `clamp(capacity + min(capacity, 2MB), required, maxCapacity)` | `min(capacity, 2MB)`,上限受 `maxCapacity` 约束 |

---

## 7. GPU Buffer 抽象演进

### 7.1 三层进化

| 版本 | GPU Buffer 类型 | 索引方式 | 绑定方式 | Draw |
|---|---|---|---|---|
| 1.20.1 | `VertexBuffer`(VAO+VBO+IBO,GL int) | `vertexBufferId`, `indexBufferId`, `arrayObjectId` | `bind()`→`glBindVertexArray(arrayObjectId)` | `draw()`→`glDrawElements` |
| 1.21.1 | 同 `VertexBuffer` | 同上 | 同上 | 同上 |
| 26.1.2 | `GpuBuffer`(抽象,usage+size) | `GpuBuffer` 引用 | `RenderPass.setVertexBuffer(0, vertices)` | `RenderPass.drawIndexed(0,0,count,1)` |

### 7.2 Usage 对比

| 1.20.1/1.21.1 | 26.1.2 |
|---|---|
| `Usage.STATIC(35044)`=GL_STATIC_DRAW | `USAGE_VERTEX(32)` |
| `Usage.DYNAMIC(35048)`=GL_DYNAMIC_DRAW | `USAGE_INDEX(64)` |
| | `USAGE_COPY_DST(8)` + `USAGE_VERTEX(32)` = 40 (immediate VB) |
| | `USAGE_COPY_DST(8)` + `USAGE_INDEX(64)` = 72 (immediate IB) |

### 7.3 缓冲区复用策略

**1.20.1/1.21.1**:`VertexFormat.getImmediateDrawVertexBuffer()` 对每个 format 懒惰创建 1 个 `VertexBuffer(DYNAMIC)`,同一 format 的连续 draw 复用。

**26.1.2**:`VertexFormat.uploadImmediateVertexBuffer(ByteBuffer)` 内调用 `uploadToBuffer(target,buffer,usage,label)`:
- 如果 `GraphicsWorkarounds.alwaysCreateFreshImmediateBuffer()`:每次创建新 Buffer 并 close 旧 Buffer
- 否则:如果现有 target 够大,通过 `encoder.writeToBuffer(target.slice(), buffer)` 写入(可能触发 GPU 同步);如果不够大,close 旧 Buffer 并创建新 Buffer

---

## 8. Upload/Draw 路径演进

### 8.1 Upload

| 版本 | 入口 | 路径 |
|---|---|---|
| 1.20.1 | `BufferUploader.drawWithShader(RenderedBuffer)` | `upload(buffer)`→`bindImmediateBuffer`→`vb.upload(buffer)`→`glBindBuffer+glBufferData` |
| 1.21.1 | `BufferUploader.drawWithShader(MeshData)` | `upload(mesh)`→`bindImmediateBuffer`→`vb.upload(mesh)`→`glBindBuffer+glBufferData` |
| 26.1.2 | `RenderType.draw(MeshData)` | `format.uploadImmediateVertexBuffer(mesh.vertexBuffer())`→`uploadToBuffer`→`device.createBuffer` 或 `encoder.writeToBuffer` |

### 8.2 Draw

| 版本 | 入口 | 路径 |
|---|---|---|
| 1.20.1 | `VertexBuffer.drawWithShader(modelView,proj,shader)` | 内联设置 12+ uniform → `shader.apply()` → `draw()`→`glDrawElements` |
| 1.21.1 | `VertexBuffer.drawWithShader(modelView,proj,shader)` | `shader.setDefaultUniforms(...)` → `shader.apply()` → `draw()`→`glDrawElements` |
| 26.1.2 | `RenderType.draw(MeshData)` | 创建 `RenderPass` → `setPipeline` → `setVertexBuffer` → `setIndexBuffer` → `drawIndexed(0,0,count,1)` |

### 8.3 关键差异图示

```
1.20.1/1.21.1:                26.1.2:
BufferUploader (static)       RenderType.draw (instance method)
  → VertexBuffer.upload         → VertexFormat.uploadImmediate*
  → VertexBuffer.bind           → RenderPass.setVertexBuffer
  → VertexBuffer.draw           → RenderPass.drawIndexed
     └→ glDrawElements
```

---

## 9. BufferSource 批处理演进

| 特性 | 1.20.1 | 1.21.1 + 26.1.2 |
|---|---|---|
| **持有对象** | `BufferBuilder builder`(共享) | `ByteBufferBuilder sharedBuffer`(共享) |
| **固定缓冲** | `Map<RenderType, BufferBuilder> fixedBuffers` | `SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers` |
| **活跃 Builder** | `Set<BufferBuilder> startedBuffers` | `Map<RenderType, BufferBuilder> startedBuilders` |
| **状态追踪** | `Optional<RenderType> lastState` | `RenderType lastSharedType`(nullable) |
| **getBuffer** | 直接 begin 共享 builder 或取 fixed builder | 按需 `new BufferBuilder(byteBufferBuilder, mode, format)` |
| **endBatch** | `renderType.end(builder, vertexSorting)` | `builder.build()`→MeshData→sortQuads→`renderType.draw(mesh)` |

**核心架构差异**:
1.20.1 中 `BufferBuilder` 是长期持有、在同一 buffer 上反复 begin/end,通过 `startedBuffers` Set 追踪。
1.21.1+26.1.2 中 `BufferBuilder` 是临时对象(每次 getBuffer 创建新的或复用),底层 `ByteBufferBuilder` 才是长期持有的存储。

---

## 10. 关键不变量跨版本对比

| 不变量 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| **渲染线程约束** | ✅ upload/draw 必须在渲染线程 | ✅ 同 | ✅ 同 |
| **VertexFormat 不可变** | ✅ ImmutableMap 驱动 | ✅ Builder 一次性构造 | ✅ 同 |
| **元素完整性检查** | ✅ endVertex 检查 elementIndex==0 | ✅ endLastVertex 检查 elementsToFill mask | ✅ 同 |
| **double release 防护** | ✅ RenderedBuffer relased bool | ✅ Result closed bool | ✅ 同 |
| **Generation 防护** | ❌ 无(直接 ByteBuffer) | ✅ ByteBufferBuilder.generation | ✅ 同 |
| **LINES 扩展** | ✅ endVertex 内 copy | ✅ endLastVertex 内 copy | ✅ 同 |
| **QUADS 排序** | ✅ setQuadSorting | ✅ MeshData.sortQuads | ✅ 同 |
| **Max 顶点** | ❌ 无上限 | ❌ 无上限 | ✅ MAX_VERTEX_COUNT=16777215 |
| **缓冲区大小上限** | ❌ 无 | ❌ 无(仅 capacity 增长 min 约束) | ✅ maxCapacity 默认 4GB |
| **结果大小** | ❌ 无 | ❌ 无 | ✅ ≤ 2^31-1 bytes |
| **对齐验证** | ❌ 无 | ❌ 无 | ✅ vertexSize % 4 == 0 |
| **PADDING 管理** | Usage.PADDING 元素 | Builder.padding(int) | 同 |
| **COLOR normalized** | ✅ 隐式 GL lambda | ✅ 隐式 GL lambda | ✅ 显式 boolean |

---

## 10.1 顶点上传完整路径对比图

```
┌─────────────────── 1.20.1 ───────────────────┐
│ VertexConsumer API (vertex/color/uv/normal...) │
│   ↓                                            │
│ BufferBuilder.buffer (native ByteBuffer)        │
│   ↓ end()                                       │
│ RenderedBuffer (pointer + DrawState)            │
│   ↓ BufferUploader.drawWithShader              │
│ VertexBuffer.upload(buffer)                     │
│   ↓ glBindBuffer + glBufferData                 │
│ VertexBuffer.drawWithShader(...)                │
│   ↓ shader.apply() + glDrawElements             │
└────────────────────────────────────────────────┘

┌─────────────────── 1.21.1 ───────────────────┐
│ VertexConsumer API (addVertex/setColor/...)    │
│   ↓                                            │
│ ByteBufferBuilder (native malloc pointer)       │
│   ↓ reserve() → write via memPut*              │
│   ↓ build() → Result                           │
│ MeshData (Result + DrawState)                  │
│   ↓ BufferUploader.drawWithShader              │
│ VertexBuffer.upload(mesh)                      │
│   ↓ mesh.close()                               │
│   ↓ glBindBuffer + glBufferData                │
│ VertexBuffer.drawWithShader(...)                │
│   ↓ shader.setDefaultUniforms() + apply()      │
│   ↓ glDrawElements                             │
└────────────────────────────────────────────────┘

┌─────────────────── 26.1.2 ───────────────────┐
│ VertexConsumer API (addVertex/setColor/...)    │
│   ↓                                            │
│ ByteBufferBuilder (native malloc, long offset) │
│   ↓ reserve() → write via memPut*              │
│   ↓ build() → Result                           │
│ MeshData (Result + DrawState)                  │
│   ↓ RenderType.draw(mesh)                      │
│ VertexFormat.uploadImmediateVertexBuffer(buf)  │
│   ↓ uploadToBuffer(target,buf,usage,label)     │
│   ↓ device.createBuffer / encoder.writeToBuffer│
│ GpuBuffer (GPU VRAM)                           │
│   ↓ RenderPass.setVertexBuffer(0, gpuBuf)      │
│   ↓ RenderPass.setIndexBuffer(gpuBuf, idxType) │
│   ↓ RenderPass.drawIndexed(0,0,count,1)         │
└────────────────────────────────────────────────┘
```

