# Repomix 模块→Token 对照表

eyelib 各模块 main-only repomix 输出（DeepSeek tokenizer）。用于预估子代理上下文消耗。

生成命令：
```bash
repomix --style markdown --output /tmp/eyelib-module-xxx.md \
  --include "src/main/**" --ignore "build/**,.gradle/**" \
  /mnt/e/_ideaProjects/qylEyelib/<name>
```

| 模块 | 文件数 | o200k_base tokens | DeepSeek tokens | 大小 |
|---|---|---|---|---|
| root (src/main) | 180 | 123,454 | 131,883 | 550 KB |
| eyelib-importer | 104 | 74,369 | 80,088 | 339 KB |
| eyelib-behavior | 211 | 55,367 | 62,156 | 238 KB |
| eyelib-molang | 81 | 52,411 | 56,123 | 244 KB |
| eyelib-particle | 87 | 35,312 | 38,461 | 160 KB |
| eyelib-util | 57 | 31,532 | 34,194 | 118 KB |
| eyelib-material | 45 | 26,095 | 28,336 | 119 KB |
| eyelib-animation | 49 | 22,947 | 24,603 | 107 KB |
| clientsmoke | 15 | 17,810 | 19,072 | 80 KB |
| eyelib-bridge | 14 | 16,740 | 17,507 | 72 KB |
| eyelib-attachment | 45 | 14,624 | 15,920 | 67 KB |
| eyelib-model | 24 | 7,088 | 7,702 | 29 KB |
| eyelib-track | 18 | 4,896 | 5,208 | 19 KB |
| eyelib-network | 6 | 1,985 | 2,161 | 9 KB |
| **合计** | **936** | **484,630** | **523,414** | **2.1 MB** |

DeepSeek 1M 上下文窗口，所有模块合计仅占 ~52%。