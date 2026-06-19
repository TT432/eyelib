# 子代理委派模式

## 原则

| 做 | 不做 |
|---|---|
| 写「目标」+「文件列表」+「约束」 | 身份提示词（「你是 eyelib 项目的调试助手」） |
| 给最少必要的文件（不塞整模块） | 文件摘要（「X 模块 — 包含 BedrockAddonLoader」） |
| 任务聚焦一个可验证答案 | 引导性结论（「注意 SP2 覆盖了 root 的 RC」） |
| 用 jsonpath 形式规则 | 实现路径（「应该在 X 文件中改 Y」） |
| 告知可用资源位置 | 推测性上下文 |

## 文件选择

- **诊断问题**：只给问题相关的少数文件（如 `Add.java` + `EntityBehaviorData.java`），不是整个模块的 repomix
- **实现功能**：给涉及模块的 repomix + 相关的 .mcpack 提取文件
- **环境操作**（端口、超时、构建）：不委派，自己处理

## 格式

```markdown
当前问题：<一句话>

全部所需文件（不允许查看其他文件）：
/path/to/a.md
/path/to/b.md

<关键约束或特性>

## 可用资源
- 项目路径 E:\_ideaProjects\qylEyelib
- MC 源码 E:\_ideaProjects\qylEyelib\.mc-source\
- Bedrock 文档 E:\_____基岩版文档\
```

## 短名映射规则示例

```jsonpath
shortNameLocations:
  - $.minecraft:client_entity.description.scripts.animate[*]
  - $.animation_controllers.<cid>.states.<sid>.animations[*].<key>

resolve(shortName) = $.animations[shortName] ?? shortName
```