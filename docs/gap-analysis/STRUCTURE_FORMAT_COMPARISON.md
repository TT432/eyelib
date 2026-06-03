# 结构文件格式对比：Bedrock .mcstructure vs Java Edition .nbt

> 调查日期：2026-06-02
> 范围：Bedrock `.mcstructure` NBT 格式 vs Java Edition `.nbt` 结构模板格式
> 目的：判断实现 Bedrock `.mcstructure` 导入的价值、复杂度与转化可行性

---

## 1. 概览：两种格式一句话

| 维度 | Bedrock `.mcstructure` | Java Edition `.nbt` |
|------|------------------------|---------------------|
| 文件扩展名 | `.mcstructure` | `.nbt` |
| 压缩 | **无压缩** | GZip 压缩 |
| 字节序 | **Little-Endian** | **Big-Endian** |
| 根标签名 | 有命名顶层字段（`format_version`, `size`, `structure`, `structure_world_origin`） | 无名称根 Compound |
| 内部编码 | NBT（Little-Endian 变种） | 标准 NBT（Big-Endian） |
| 存储位置 | 行为包 `BP/structures/` | 数据包 `data/<namespace>/structures/` |
| Minecraft 原生读取 | 原生支持 | 原生支持 |

---

## 2. 格式详细对比

### 2.1 顶层结构

```
Bedrock .mcstructure                          Java Edition .nbt
─────────────────────────                     ─────────────────────────
Root Compound:                                 Root Compound (no name):
  format_version: int (1)                        DataVersion: int (optional)
  size: [int, int, int]                          author: string (optional)
  structure: Compound {                          size: [int, int, int]
    block_indices: [ [int...], [int...] ]        blocks: [Compound...]
    entities: [Compound...]                      entities: [Compound...]
    palette: Compound {                          palette: [Compound...]
      default: Compound {
        block_palette: [Compound...]
        block_position_data: Compound {...}
      }
    }
  }
  structure_world_origin: [int, int, int]
```

**关键差异：**

1. **深度不同** — Bedrock 的 `structure.palette.default.block_palette` 有 3 层嵌套；JE 的 `palette` 直接是列表
2. **root tag name** — Bedrock 的每个顶层字段有命名标签；JE 整个文件是一个未命名的 Compound
3. **origin** — Bedrock 有 `structure_world_origin` 追踪保存位置（用于实体放置偏移）；JE 无此概念

### 2.2 Block 编码方式 — 核心差异

这是两者**最大的结构性差异**：

| 维度 | Bedrock `.mcstructure` | Java Edition `.nbt` |
|------|------------------------|---------------------|
| 存储方式 | **ZYX 展平索引数组** | **逐 block 列表** |
| 所有 block | 是（包括空气，用 `-1` 索引表示无 block） | 否（只存储非空气 block） |
| 两层 | 是（`block_indices[0]` 主层 + `block_indices[1]` 水浸层） | 否（单层） |
| 顺序 | ZYX 顺序，固定位置推导 | 每个 block 显式记录 `pos: [x, y, z]` |
| 数据密度 | 紧凑（索引固定长度 = size_x × size_y × size_z） | 稀疏（仅存储实际占位的 block） |
| 空位表示 | 索引 `-1` | 不在列表中 |
| Palette 索引 | 单层 ZYX 展平数组中的值 | 每个 block 的 `state` 字段 |

**示例对比：**

**Bedrock**（结构 size = `[2, 3, 4]`）：
```
block_indices[0] = [0, 0, 1, 1, 2, -1, ...]   // 24 个值 = 2×3×4
block_indices[1] = [-1, -1, -1, ..., 250, ...] // 水浸 layer 大部分为 -1
```

**JE**（同样结构）：
```
blocks = [
  { pos: [0,0,0], state: 0 },
  { pos: [0,0,1], state: 0 },
  { pos: [0,0,2], state: 1 },
  { pos: [0,0,3], state: 1 },
  { pos: [0,1,0], state: 2 },
  // ... 只包含非空气 block
]
```

### 2.3 Palette 格式对比

| 字段 | Bedrock (block_palette 条目) | JE (palette 条目) |
|------|------------------------------|-------------------|
| Block 标识符 | `name: string`（如 `minecraft:planks`） | `Name: string`（如 `minecraft:planks`） |
| Block States | `states: Compound`（值类型：string / int / byte） | `Properties: Compound`（值类型：全部 string） |
| 版本 | `version: int`（如 `17959425`） | 无（使用全局 DataVersion） |
| 命名空间 | 内置命名空间（支持多个 palette 名称，如 `default`） | 无命名（单个列表） |

**关键差异 — Block States 值类型：**

- **Bedrock**：`states` 中的值可以是 `string`（枚举）、`int`（标量）、`byte`（布尔值）— 区分类型
- **JE**：`Properties` 中的所有值都是 `string` — 布尔值如 `"true"`/`"false"`，枚举如 `"oak"`，数值如 `"3"`
- **影响**：转化时需要做 Bedrock 的 typed block states → JE string-only block states 的映射

### 2.4 Block Entity 存储

| 维度 | Bedrock `.mcstructure` | Java Edition `.nbt` |
|------|------------------------|---------------------|
| 存储位置 | `palette.default.block_position_data` 映射 | `blocks[].nbt` 嵌套字段 |
| 索引方式 | 展平 ZYX 索引（integer key） | 每个 block 条目自带 |
| 层归属 | `layer is unspecified`（不区分主/次层） | 单层，直接关联 |

**Bedrock 的 block_position_data 结构：**
```
block_position_data: Compound {
  "0": Compound {     // 展平索引 = 0
    block_entity_data: Compound { ... NBT ... }
    tick_queue_data: [Compound...]   // 计划刻（珊瑚死亡、水流等）
  }
  "27": Compound {    // 展平索引 = 27
    block_entity_data: Compound { ... NBT ... }
  }
}
```

**JE 的 blocks[].nbt 结构：**
```
blocks[3]: {
  pos: [1, 2, 0],
  state: 5,
  nbt: { ... block entity NBT ... }   // 直接内嵌
}
```

### 2.5 Entity 存储

两者基本一致 — 都是标准的 entity NBT 格式，但在处理上有细微差异：

- **Bedrock**：存储 `Pos` 和 `UniqueID`，加载时根据 `structure_world_origin` 重新计算绝对位置
- **JE**：存储相对位置（`pos` 是 [x, y, z] 偏移），加载时根据放置位置偏移

### 2.6 其他差异

| 维度 | Bedrock `.mcstructure` | Java Edition `.nbt` |
|------|------------------------|---------------------|
| Block state 版本号 | `version: int`（如 hex 01 10 D2 03 = 1.16.210.03） | 全局 `DataVersion: int` |
| 可选元数据 | `tick_queue_data`（计划刻数据） | 无 |
| 作者 | 无 | `author: string`（可选） |
| Air block 保留 | 是（-1 表示 void） | 否（不存储即为 air） |
| Waterlog 支持 | 第二层 `block_indices[1]` | 通过 block state 属性 |
| 最大尺寸 | 无硬限制（但 vanilla UI 限制 64×256×64） | 无限制 |
| Multi-palette | 支持命名 palette（仅 `default` 使用） | 无命名（单个列表） |

---

## 3. 转化可行性分析：Bedrock → JE

### 3.1 可以自动转化的部分（中低难度）

| 转化项 | 难度 | 备注 |
|--------|------|------|
| `size` → `size` | ✅ 直接 | 值相同，结构相同 |
| `block_palette` → `palette` | 🟡 中 | `name` → `Name`（直接）；`states` → `Properties`（需要类型映射） |
| block state 值的类型映射 | 🟡 中 | byte→"true"/"false"；int→str(int)；string→string |
| `block_indices[0]` 展平 → `blocks[]` 列表 | 🟡 中 | 需要遍历 ZYX，跳过 -1 索引，重建 pos 字段 |
| `entities` → `entities` | ✅ 低 | 基本可直接复制 NBT |
| `structure_world_origin` 处理 | ✅ 低 | 用于实体 Pos 偏移计算 |

### 3.2 需要额外处理的（中高难度）

| 转化项 | 难度 | 备注 |
|--------|------|------|
| **第二层（waterlog）** | 🟡 中 | `block_indices[1]` 的非 -1 索引需要合并到主层 block state 中，添加 waterlogged 属性 |
| **`tick_queue_data`** | 🔴 高 | JE 的 StructureTemplate 不支持计划刻数据；需要丢弃或自定义存储 |
| **`block_entity_data` 索引对应** | 🟡 中 | 需要将 Bedrock 的展平索引（Integer key）转换为 JE 的 pos 关联 |
| **`version` 兼容性** | 🟠 低 | Bedrock 的 block version（如 17959425）JE 不使用；需要提取 block 版本的语义含义或忽略 |
| **Block mapping（ID 差异）** | 🔴 高 | Bedrock 和 JE 的 block ID 不完全一致；某些 block 在对面版本不存在 |

### 3.3 无法转化或需丢弃的

1. **`tick_queue_data`** — JE 无对应概念
2. **`format_version`** — 元数据，无意义
3. **`structure_world_origin`** — 不需要（JE 使用相对放置位置）
4. **`author`** — Bedrock 无此字段
5. **`DataVersion`** — JE 特有，转化时可设置当前版本

### 3.4 转化架构建议

```
.mcstructure (Little-Endian NBT)
        │
        ▼
  NBT 反序列化（需要自定义 Little-Endian NBT 读取）
        │
        ▼
  McstructureData (Java record)
        │
        ▼
  Converter.mcstructureToJeNbt()
        │
        ▼
  CompoundTag (标准 JE NBT) → GZip 压缩 → .nbt 文件
        │
        ▼
  Minecraft StructureTemplate.readFromNBT(CompoundTag)
```

**注意**：Minecraft Forge 的 `StructureTemplate` 类（`net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate`）有 `load(CompoundTag)` 方法可以直接加载标准 NBT 格式。转化后的 `.nbt` 文件可以被原生使用。

---

## 4. 项目代码现状（eyelib）

### 4.1 已有实现

| 组件 | 状态 | 路径 |
|------|------|------|
| `BedrockResourceFamily.classify` | ✅ `structures/` → `STRUCTURE` | `eyelib-importer/.../BedrockResourceFamily.java` |
| `unmanagedReasonFor(STRUCTURE)` | ✅ `OUTSIDE_IMPORTER_SCOPE` | `eyelib-importer/.../BedrockAddonLoader.java:645` |
| 二进制资源捕获 | ✅ 落入 `captureUnmanaged` 分支 | `BedrockAddonLoader.java:822` |
| `BedrockBinaryAsset` | ✅ 记录二进制文件 | `eyelib-importer/.../BedrockBinaryAsset.java` |
| NBT 工具 | ✅ `EyelibStreamCodecs`（CompoundTag/Tag 编解码） | `eyelib-util/.../EyelibStreamCodecs.java` |

### 4.2 缺失

| 缺失项 | 说明 |
|--------|------|
| **`.mcstructure` 解析器（Little-Endian NBT）** | 无 — Minecraft 标准 `NbtIo` 用 Big-Endian，不能直接读 `.mcstructure` |
| **McstructureData 数据结构** | 无 — 需要定义 `record McstructureData(formatVersion, size, structure, origin)` |
| **Bedrock→JE 转化器** | 无 — 需要 `McstructureToJeConverter` |
| **runtime 放置** | 无 — 需要 `StructurePlacer` 在 JE 世界中放置结构 |
| **aggregate 字段** | 无 — `PackAccumulator`/`BedrockAddonPack`/`BedrockAddonSideAggregate` 均无 structure 字段 |
| **processEntry case** | 无 — 需要 `case STRUCTURE` 处理二进制 NBT 文件 |

### 4.3 JE 侧已有的基础设施

- **Minecraft `NbtIo`**：Big-Endian NBT 读写（可以直接读 JE `.nbt` 文件）
- **Minecraft `StructureTemplate`**：JE 结构模板类，有 `load(CompoundTag)` 方法
- **Minecraft `StructurePlaceSettings`**：放置配置（旋转、镜像、实体继承等）
- **`FriendlyByteBuf.readNbt()/writeNbt()`**：标准 NBT 流编解码（Big-Endian）

> **关键点**：JE 的 `NbtIo` 不能直接读 `.mcstructure` 文件，因为它是 Little-Endian NBT。
> 需要实现一个 **Little-Endian NBT 读取器** 或使用第三方库。
> 但 **不依赖 Bedrock 解析** — 对于纯 JE `.nbt` 结构文件导入，项目可以直接复用现有 Minecraft 的 `NbtIo` 和 `StructureTemplate`。

---

## 5. 实现价值判断

### 5.1 场景分析

| 用户场景 | 需要 Bedrock 解析？ | 需要 JE 解析？ | 优先级 |
|----------|---------------------|----------------|--------|
| 从 JE 数据包加载结构文件 | ❌ 不需要 | ✅ 需要（直接使用 NbtIo） | **🔴 P0** |
| 从 Bedrock 行为包导入结构 | ✅ 需要 | ❌ 不需要（需要转化） | 🟡 P2 |
| elylib 提供通用结构管理 API | 🟡 可有可无 | ✅ 需要 | 🔴 P1 |
| 将 Bedrock 结构转化为 JE 格式（跨平台移植） | ✅ 需要 | ✅ 需要（作为输出目标） | 🟠 P3 |

### 5.2 工作量估计

| 功能模块 | 预估工作量 | 难度 | 
|----------|-----------|------|
| **JE `.nbt` 直接读取 + runtime 放置** | 小（1-2 天） | 🟢 低（复用 Minecraft 内置 API） |
| **结构文件代码管理**（aggregate 字段 + processEntry + clasify） | 极小（半天） | 🟢 低 |
| **`.mcstructure` 解析器**（Little-Endian NBT 读取） | 中（2-3 天） | 🟡 中 |
| **Bedrock→JE 转化器** | 中（2-3 天） | 🟡 中 |
| **Block state 映射**（BE↔JE block ID/state 差异） | 大（持续） | 🔴 高（需维护映射表） |

### 5.3 价值评估

| 场景 | 价值 | 理由 |
|------|------|------|
| **JE 结构文件直接管理** | 🔴 **高** | 用户在 JE 服务器/单人游戏中最常用结构文件；eyelib 作为 Forge 模组，直接复用 Minecraft 原生 API |
| **.mcstructure 导入** | 🟡 **中** | Bedrock 行为包可能在跨平台生态中使用；但大部分 eyelib 用户以 JE 为核心 |
| **BE→JE 自动转化** | 🟠 **低-中** | 需要维护 block ID 映射表；Bedrock 和 JE 的 block 差异随时间增大 |

### 5.4 推荐路线

```
Phase 1 (P0-P1) — JE 结构文件支持（purely Forge-native）:
  ├─ 在 importer 中添加 structure 字段到 aggregate
  ├─ 用 Minecraft NbtIo + StructureTemplate 直接解析 .nbt 文件
  ├─ runtime 放置结构（StructurePlaceUtils.placeStructure）
  └─ 添加命名空间解析（data/<ns>/structures/<path>.nbt → ns:path）

Phase 2 (P2) — .mcstructure 解析器:
  ├─ 实现 Little-Endian NBT 读取（或引入第三方 lib）
  ├─ 定义 McstructureData record
  ├─ 解析 .mcstructure 并存储为 BedrockBinaryAsset
  └─ 结构命名空间解析（structures/ns/path.mcstructure → ns:path）

Phase 3 (P3) — 转化管道:
  ├─ McstructureToJeConverter
  │   ├─ palette 转化（name→Name, states→Properties）
  │   ├─ block_indices[0] → blocks[] 展平重建
  │   └─ block_indices[1] → waterlogged 属性合并
  ├─ block_entity_data 索引重映射
  ├─ block ID/state 映射表（手动维护）
  └─ 输出标准 JE .nbt 文件
```

---

## 6. 结论

### 6.1 立即行动（Phase 1）

**JE `.nbt` 结构文件导入价值高、需要实现**：

1. eyelib 是 Forge 模组，JE 结构文件是最自然的资源格式
2. Minecraft 提供了完整的 NBT 读写（`NbtIo`）+ 结构模板解析（`StructureTemplate`）+ 放置 API（`StructurePlaceUtils`）
3. 结构文件在数据包生态中广泛使用（预制结构、地形生成、地图制作者）
4. 对用户来说，最常见的使用场景就是 "把 JE 结构文件放到数据包中然后用"

**工作量小**：只需要在 importer 中添加:
- `BedrockAddonSideAggregate` 中加 `structureFiles` 字段
- `processEntry` 中加 `case STRUCTURE`（但实际上是读取 `<namespace>/structures/` 目录的 .nbt 文件）
- 用 `NbtIo.readCompressed(Path)` 读取
- 用 `StructureTemplate.load(CompoundTag)` 解析

### 6.2 谨慎评估（Phase 2-3）

**`.mcstructure` 解析 + BE→JE 转化**：

- **价值有限** — 主要面向跨平台内容创作者，但 eyelib 核心用户群体是 JE Forge 模组开发者
- **复杂度高** — 需要实现 Little-Endian NBT 读取 + block 映射表维护
- **后期可以补充** — 如果社区有活跃需求，可以作为独立模块添加

### 6.3 最终建议

| 功能 | 建议 | 优先级 |
|------|------|--------|
| JE `.nbt` 结构文件导入 | ✅ **必须实现** | **P0** |
| `.mcstructure` 二进制管理 | 🟡 可以管理（作为二进制资源存储） | P2 |
| `.mcstructure` → JE `.nbt` 转化 | 🟠 延后评估（需要时再实现） | P3 |

> **核心判断**：eyelib 应该优先支持 JE 自身的 `.nbt` 结构文件，因为它是 Forge 模组，可以使用 Minecraft 原生 API，且用户场景最明确。`.mcstructure` 的解析可以晚些决定，或者只保留为二进制资源管理（不解析内部结构）。

---

## 附录 A：Bedrock .mcstructure NBT 结构（完整）

```
Root Compound (no root name):
  format_version: TAG_Int           # 当前始终为 1
  size: TAG_List of 3 TAG_Int       # [width, height, depth]
  structure: TAG_Compound {
    block_indices: TAG_List of 2 TAG_List of TAG_Int
      # [0] = primary layer (ZYX order, -1 = void)
      # [1] = secondary layer (waterlogged, etc.)
    entities: TAG_List of TAG_Compound
      # Standard entity NBT; Pos & UniqueID stored but replaced on load
    palette: TAG_Compound {
      default: TAG_Compound {
        block_palette: TAG_List of TAG_Compound {
          name: TAG_String           # e.g. "minecraft:planks"
          states: TAG_Compound       # typed values: string/int/byte
          version: TAG_Int           # e.g. 17959425
        }
        block_position_data: TAG_Compound {
          <index (TAG_String)>: TAG_Compound {
            block_entity_data: TAG_Compound
            tick_queue_data: TAG_List of TAG_Compound {
              tick_delay: TAG_Int
            }
          }
        }
      }
    }
  }
  structure_world_origin: TAG_List of 3 TAG_Int
    # [x, y, z] position of structure block when saved
```

## 附录 B：JE .nbt 结构模板（完整）

```
Root Compound (unnamed):
  DataVersion: TAG_Int (optional)    # global data version
  author: TAG_String (optional)      # player who saved
  size: TAG_List of 3 TAG_Int        # [width, height, depth]
  blocks: TAG_List of TAG_Compound {
    pos: TAG_List of 3 TAG_Int       # [x, y, z] relative to origin
    state: TAG_Int                   # index into palette
    nbt: TAG_Compound (optional)     # block entity data
  }
  entities: TAG_List of TAG_Compound
    # Standard entity NBT
  palette: TAG_List of TAG_Compound {
    Name: TAG_String                # e.g. "minecraft:planks"
    Properties: TAG_Compound (optional)  # all values are TAG_String
  }
```
