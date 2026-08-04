# ADR-0026：RenderController 内联进实体画布

## 状态

已接受（2026-08-04）。替代 ADR-0025 中「RC 闭包导入为独立库」的默认路径（独立 RC 库仍保留为高级用法）。

## 背景

ADR-0025 建立「声明=连线」后，实体画布出现冗余：同一 ref.geometry 要在实体库
（接 entity.root.geometries，声明表行）和 RC 库（接 rc.root.geometry，字段引用）
各放一份，靠短名隐式对齐。用户指出：geo ref 单独接 ClientEntity 没有消费端，
RC 编辑应在同一画布内。

## 决策

1. **rc.root 可放 CLIENT_ENTITY 主图**：新增 condition（条件）、controller（RC_REF 输出）、
   geometries/textures/materials（仅声明）端口；entity.root.render_controllers 改为
   RC_REF 多接端口，rc.condition_entry 删除。
2. **ref 接 RC = 声明+引用；裸字符串 = 仅引用**。geo/tex/mat 声明表由组装器从
   RC 锚点（rc.root/ref.rc）派生，entity.root 三声明端口删除；动画/AC 保留实体级
   （双消费端：animate 脚本 + AC 状态机）。
3. **ref.rc 获 condition + 三声明端口**：外部 RC（vanilla/共享）的表行准备仍有锚点。
4. **实体构建可产 RC 文档**：AssemblyResult.extraDocs，构建服务注册。
5. **导入产单库**：RC 文档经 resolver 内联反编译进实体主图；AC 闭包保持独立库。
6. format_version 3→4，链式迁移（condition_entry 拆解、声明线重定向 ref.rc）。

## 理由

- 语义归位：声明表的唯一消费端是 RC，ref 的连线目标因此是 RC 锚点；
  「声明+引用」合并为一根线，消除双份 ref 的样板与短名漂移风险。
- 单画布编辑符合蓝图编辑器心智（UE 蓝图组件/资产引用同画布可见）。
- 派生表是纯函数，构建产物与 v3 多库方案逐字段等价（悦灵实测对照）。

## 后果

- 好处：导入产物从 7 库变 1 库；ref 只存在一份；声明表永远与实际引用一致；
  外部 RC 的表行准备显式化（ref.rc 声明端口）。
- 代价：format v4 迁移；entity 构建职责变重（产 RC 文档）；arrays 仍是 TEXT 原文
  （未被字段引用的表条目需挂声明端口——导入自动处理，手工编辑需知情）。
- 独立 RC 库保留：跨实体共享 RC 的编辑仍走独立库 + ref.rc 引用。
