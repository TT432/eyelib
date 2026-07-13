# Repomix 模块打包 & Token 评估

将 eyelib 子模块打包为 AI 可用的单一 markdown 文件，并用 DeepSeek 分词器评估上下文占用。

## 工具链

```bash
# repomix — 源码打包为单文件
npm install -g repomix           # v1.14.1

# deepseek-tokenizer — DeepSeek 分词器
python3 -m venv /tmp/ds-tokenizer
/tmp/ds-tokenizer/bin/pip install deepseek-tokenizer
```

## DeepSeek 上下文窗口

**DeepSeek V3/V4 上下文窗口 = 1M tokens**。128K 是过时信息。

## 全模块 Token 估算

ADR-0014 前(14 子项目时代)全量约 523K DS tokens(1M 的 52%)。ADR-0014 flat-merge 后单 project 结构相近，可用 `scripts/repomix_main_only.py` 重新统计(默认输出 ~465K tokens)。

## 单模块打包

```bash
# main only (不含 test)
repomix --style markdown \
  --output /tmp/eyelib-module-<name>.md \
  --include "src/main/**,*.md,*.gradle" \
  --ignore "build/**,.gradle/**,src/test/**" \
  E:\_ideaProjects\qylEyelib\<module>
```

## 全项目一次性打包 (main-only)

使用 `scripts/repomix_main_only.py`：

```bash
python scripts/repomix_main_only.py [output_path]
# 默认输出 /tmp/eyelib-all-main.md (2.0 MB, ~465K tokens)
```

内部逻辑：自动发现所有含 `src/main/` 的模块目录，用单个 `--include` brace expansion 合并：

```bash
repomix --style markdown --output <out> \
  --include "{src/main,clientsmoke/src/main}/**,*.gradle,MODULES.md,AGENTS.md,gradle.properties" \
  --ignore "{build,.gradle,run,.idea}/**,**/src/test/**,*.{lock,log,mca,dat},*.log.gz" \
  .
```

> 注: ADR-0014 前用 `eyelib-*/src/main` 匹配各子项目,合并后单 project 已不需要。

### Pitfall: 多个 --include 是 AND 逻辑

repomix 的多个 `--include` 参数取**交集**（AND），不是并集（OR）：

```
❌ --include "src/main/**" --include "src/test/**"  → 交集为空,只命中 0 个文件
✅ --include "{src/main,src/test}/**"               → 并集,命中所有匹配文件
```

**永远用单个 brace expansion pattern 合并所有路径，不要拆成多个 `--include`。**

> 注: ADR-0014 前(子项目时代)示例曾用 `eyelib-molang/src/main`,现已无子项目结构。

## Token 计数

```bash
/tmp/ds-tokenizer/bin/python3 -c "
from deepseek_tokenizer import ds_token
with open('/tmp/eyelib-module-<name>.md', 'r') as f:
    text = f.read()
print(f'DeepSeek tokens: {len(ds_token.encode(text)):,}')
"
```

## 关键参数说明

- `--style markdown`: AI 最友好的输出格式
- `--include "src/main/**,*.md,*.gradle"`: 只打包主源码 + 文档 + 构建文件
- `--ignore "build/**,.gradle/**,src/test/**"`: 排除编译产物和测试
- `--compress`: (可选) Tree-sitter 提取代码结构，砍方法体实现细节

## 委托子代理时传递模块

不用把文件内容读进自己的上下文。把路径放进 `context` 字段即可:

```
delegate_task(
  goal="分析 molang 模块架构",
  context="模块打包文件在 /tmp/eyelib-module-molang.md，用 read_file 读取",
  toolsets=["terminal", "file"]
)
```

子代理自己读取文件，不占用父代理上下文。

### context 规则

```
✅ 目标 + 文件列表 + 约束
❌ 身份提示词（「你是 eyelib 项目的调试助手」）
❌ 文件摘要（「X 模块 — 包含 BedrockAddonLoader」）
❌ 引导性结论（「注意 SP2 覆盖了 root 的 RC」）
❌ 实现路径（「应该在 X 文件中改 Y」）
```

### 子代理超时诊断

子代理频繁超时，常见原因：
1. **context 太大** — 不需要全模块 repomix，只给涉及的文件即可。例如查 1 个 bug 给 2-3 个目标文件，不要给整个 5 万 token 的模块
2. **目标不聚焦** — 「分析 X」不如「用 MCP 工具查 entity 157 的 scope 中是否有 MolangEntityContext」
3. **gradle 构建卡住** — 构建用 `eyelib_debug_build` MCP tool,或 bash `gradlew`(AGENTS.md Tooling Restrictions)

## 相关工具

- `scripts/repomix_main_only.py` — 全项目 main-only 打包为单文件 (`--include` AND pitfall → brace expansion)
- `scripts/repomix_main_only.bat` — Windows 批处理包装
- `scripts/mcpack_extract_entity.py` — 从 .mcpack 提取单个 client_entity（参见 `minecraft-bedrock-resource-pack` skill）
