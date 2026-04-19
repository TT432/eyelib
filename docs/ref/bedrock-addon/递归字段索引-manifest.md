# Bedrock Add-On 递归字段索引：manifest / 容器 / pack

## 1. 说明

本页把 Add-On 的**容器层、pack 根层、`manifest.json` 字段层**写成递归树。

- `NON_TERMINAL`：当前节点还应继续拆。
- `TERMINAL`：当前节点已经到原子字段或原子取值形态。
- 权威级别：`S` = 官方机器可读 schema；`O` = 官方文档；`R` = 官方样例；`C` = 社区回退。

> 这一页优先回答：`.mcaddon/.mcpack` 是什么，`manifest.json` 能递归拆到哪些字段，哪些字段是 stable，哪些只是 preview / 回退来源。

## 2. 容器层

- `NON_TERMINAL` `com.mojang/`
  - 来源：O1
  - 子节点：
    - `NON_TERMINAL` `development_behavior_packs/<pack>/`
    - `NON_TERMINAL` `development_resource_packs/<pack>/`
    - `NON_TERMINAL` `behavior_packs/<pack>/`
    - `NON_TERMINAL` `resource_packs/<pack>/`
- `NON_TERMINAL` `.mcpack`
  - 含义：单 pack 导入容器
  - 来源：O1
  - 子节点：`NON_TERMINAL` `<pack root>/manifest.json`
- `NON_TERMINAL` `.mcaddon`
  - 含义：把多个 pack 根一起打包的捆绑容器
  - 来源：C1
  - 子节点：
    - `NON_TERMINAL` `<behavior pack root>/manifest.json`
    - `NON_TERMINAL` `<resource pack root>/manifest.json`

## 3. Pack 根层

- `NON_TERMINAL` `<pack root>/manifest.json`
  - 来源：O5-MAIN
- `NON_TERMINAL` `<pack root>/content folders`
  - 例：`scripts/`、`structures/`、`textures/`
  - 来源：O2、O3、O4、R3、R4

## 4. `manifest.json` 递归树

- `TERMINAL` `format_version`
  - 含义：manifest 语法版本
  - 来源：O5-MAIN、O5-VERSION、O5-SCHEMA、C3
  - 备注：`2` 是当前稳定主线；`3` 是 preview 语境

- `NON_TERMINAL` `header`
  - 来源：O5-HEADER、O5-SCHEMA
  - 子节点：
    - `TERMINAL` `header.description`
    - `TERMINAL` `header.name`
    - `TERMINAL` `header.uuid`
    - `NON_TERMINAL` `header.version`
      - `TERMINAL` 三元数组 `[major, minor, revision]`
      - `TERMINAL` semver 字符串（preview / v3）
    - `NON_TERMINAL` `header.min_engine_version`
      - `TERMINAL` 三元数组
      - `TERMINAL` semver 字符串
    - `TERMINAL` `header.pack_scope`
      - 备注：RP-only，官方文档与官方样例都出现
    - `TERMINAL` `header.allow_random_seed`
      - 备注：world template only
    - `NON_TERMINAL` `header.base_game_version`
      - `TERMINAL` 三元数组
      - `TERMINAL` semver 字符串
    - `TERMINAL` `header.lock_template_options`

- `NON_TERMINAL` `modules`
  - 来源：O5-MODULE、O5-SCHEMA、R3、R4
  - 子节点：
    - `NON_TERMINAL` `modules[*]`
      - `TERMINAL` `description`
      - `TERMINAL` `uuid`
      - `NON_TERMINAL` `type`
        - `TERMINAL` `resources`
        - `TERMINAL` `data`
        - `TERMINAL` `world_template`
        - `TERMINAL` `script`
        - `TERMINAL` `client_data`（样例/兼容分支，不作为稳定主表默认项）
      - `NON_TERMINAL` `version`
        - `TERMINAL` 三元数组
        - `TERMINAL` semver 字符串（preview / v3）
      - `TERMINAL` `language`
        - 备注：仅 `type=script` 时有意义；官方样例常见 `javascript`
      - `TERMINAL` `entry`
        - 备注：脚本入口文件

- `NON_TERMINAL` `dependencies`
  - 来源：O5-DEP、R3、R4
  - 子节点：
    - `NON_TERMINAL` pack dependency object
      - `TERMINAL` `uuid`
      - `NON_TERMINAL` `version`
        - `TERMINAL` 三元数组（stable / v2）
        - `TERMINAL` semver 字符串（preview / v3）
    - `NON_TERMINAL` module dependency object
      - `TERMINAL` `module_name`
      - `NON_TERMINAL` `version`
        - `TERMINAL` 三元数组（stable / v2）
        - `TERMINAL` semver 字符串（preview / v3）

- `NON_TERMINAL` `capabilities`
  - 来源：O5-CAP、R5、R6、C3
  - 子节点：
    - `TERMINAL` `chemistry`
    - `TERMINAL` `editorExtension`
    - `TERMINAL` `experimental_custom_ui`
    - `TERMINAL` `raytraced`
    - `TERMINAL` `pbr`（样例/legacy 能力值；不应写成当前官方主表能力）

- `NON_TERMINAL` `metadata`
  - 来源：O5-META、O5-SCHEMA、R3、R4
  - 子节点：
    - `TERMINAL` `authors`
    - `TERMINAL` `license`
    - `TERMINAL` `url`
    - `TERMINAL` `product_type`
      - 备注：当前官方主线写法收敛到 `addon`
    - `NON_TERMINAL` `generated_with`
      - `TERMINAL` `generated_with.<tool_name>`
      - `TERMINAL` `generated_with.<tool_name>[*]`

- `NON_TERMINAL` `settings`
  - 来源：O5-SETTINGS、O5-SETTINGS-DROPDOWN、R3
  - 稳定性：preview / v3
  - 子节点：
    - `NON_TERMINAL` `label`
      - `TERMINAL` `type`
      - `TERMINAL` `text`
    - `NON_TERMINAL` `toggle`
      - `TERMINAL` `type`
      - `TERMINAL` `text`
      - `TERMINAL` `name`
      - `TERMINAL` `default`
    - `NON_TERMINAL` `slider`
      - `TERMINAL` `type`
      - `TERMINAL` `text`
      - `TERMINAL` `name`
      - `TERMINAL` `min`
      - `TERMINAL` `max`
      - `TERMINAL` `step`
      - `TERMINAL` `default`
    - `NON_TERMINAL` `dropdown`
      - `TERMINAL` `type`
      - `TERMINAL` `text`
      - `TERMINAL` `name`
      - `TERMINAL` `default`
      - `TERMINAL` `options[*].name`
      - `TERMINAL` `options[*].text`

- `NON_TERMINAL` `subpacks`
  - 来源：O5-SCHEMA、O5-SUBPACK
  - 子节点：
    - `NON_TERMINAL` `subpacks[*]`
      - `TERMINAL` `folder_name`
      - `TERMINAL` `name`
      - `TERMINAL` `memory_tier`（legacy/stable 教程语义）
      - `TERMINAL` `memory_performance_tier`（preview / v3 experimental）

## 5. 与本仓库 importer 的关系

- 本仓库 importer 已显式解析 `format_version`、`header`、`modules`、`dependencies`、`metadata`、`capabilities`，并保留 `settings` 列表与其他 `extraFields`。
- 但 importer **没有**把本页所有字段都做成显式语义节点；例如 world-template 字段、`pack_scope`、`language` / `entry`、`subpacks` 仍不应写成“仓内已 typed 建模”。
- 这只能说明 **Eyelib 当前支持面**，不能代替官方来源判断字段合法性。

## 6. 主要来源编号

- O1
- O2
- O3
- O4
- O5-MAIN
- O5-HEADER
- O5-MODULE
- O5-DEP
- O5-META
- O5-CAP
- O5-SETTINGS
- O5-SETTINGS-DROPDOWN
- O5-VERSION
- O5-SCHEMA
- C3
