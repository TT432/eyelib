package io.github.tt432.eyelib.nodegraph;

import io.github.tt432.eyelib.nodegraph.migration.V1ToV2Migration;
import io.github.tt432.eyelib.nodegraph.migration.V2ToV3Migration;
import io.github.tt432.eyelib.nodegraph.migration.V3ToV4Migration;
import io.github.tt432.eyelib.nodegraph.migration.V4ToV5Migration;
import io.github.tt432.eyelib.nodegraph.migration.V5ToV6Migration;
import io.github.tt432.eyelib.nodegraph.migration.V6ToV7Migration;
import io.github.tt432.eyelib.nodegraph.migration.V7ToV8Migration;
import io.github.tt432.eyelib.nodegraph.migration.V8ToV9Migration;
import io.github.tt432.eyelib.nodegraph.migration.V9ToV10Migration;
import io.github.tt432.eyelib.nodegraph.migration.V10ToV11Migration;
import io.github.tt432.eyelib.nodegraph.migration.V11ToV12Migration;
import io.github.tt432.eyelib.nodegraph.migration.V12ToV13Migration;
import io.github.tt432.eyelib.nodegraph.migration.V13ToV14Migration;
import io.github.tt432.eyelib.nodegraph.migration.VersionMigration;
import java.util.List;

/**
 * 图文档迁移：按 format_version 链式执行（规格 nodegraph-eproject-variables §3.4、
 * nodegraph-declaration-wiring §2.4）。
 *
 * <p>v1 → v2 变量节点化（无兼容双轨）：
 * <ul>
 *   <li>{@code var.get}（name=variable.foo）→ {@code variable}（name=foo，不带根）；</li>
 *   <li>{@code exec.set_var} root=variable → 新 {@code exec.set_var}（target 引脚）
 *       + 自动生成 variable 节点（置于原节点左上）连线 target；</li>
 *   <li>{@code exec.set_var} root=temp → {@code exec.set_temp}（name 保留）。</li>
 * </ul>
 *
 * <p>v2 → v3 声明连线化（CLIENT_ENTITY 库；声明 = 连线，扫描语义废止）：
 * <ul>
 *   <li>主图无声明连线的 ref.{geometry,texture,material,animation,ac} → 补线到 entity.root
 *       对应声明端口；</li>
 *   <li>主图未连任何 rc.condition_entry 的 ref.rc → 新建 rc.condition_entry（置于 ref 右侧，
 *       condition 用端口默认 1），ref→entry.rc、entry.entry→root.render_controllers；</li>
 *   <li>子图中完全无连线的声明类 ref → 移到主图（uid/选项保留，置于主图空闲区）并补线；
 *       有连线的不动（验证器 REF_NOT_CONNECTED 提示）；</li>
 *   <li>RENDER_CONTROLLER / ANIMATION_CONTROLLER 库不变。</li>
 * </ul>
 *
 * <p>v3 → v4 RC 内联（CLIENT_ENTITY 库）：rc.condition_entry 拆解、声明线重定向 ref.rc。
 *
 * <p>v4 → v5 颜色端口化（所有库）：rc.root 16 个 float 通道端口 → 4 个 COLOR 端口
 * （常量 → const.color，含表达式 → color.compose）。
 *
 * <p>v5 → v6 有序条目链 + decl_* 移除（规格 nodegraph-ordered-entries-and-rc-reference-set）：
 * <ul>
 *   <li>rc.root 的 textures/materials/part_visibility 多条目线 → 按 uid 序（= v5 发射序）
 *       排成链：首条目线保留，其余重定向为 e_i.entry → e_{i-1}.next；</li>
 *   <li>rc.root 的 decl_* 连线删除（ref 节点保留；协议短名由 DeclarationTables carve-out
 *       接管，非协议行自此不进表）；</li>
 *   <li>ref.rc 的 decl_* 不变。</li>
 * </ul>
 *
 * <p>v6 → v7 命名变量端口：剥除 decl_variables 桶接线与 declvar- 节点
 * （规格 nodegraph-animation-variable-refs §2.1；新接线需被引内容重建，重新导入自动恢复）。
 *
 * <p>v7 → v8 ref.ac 撤除命名变量端口（AC 图化）：剥除 ref.ac 的 var_refs 快照、
 * 指向其 read:/write: 端口的连线与失连 declvar- 节点；ref.animation 保留。
 *
 * <p>v8 → v9 左读右写：exec.set_var.target 与 ref.animation write:<名> 翻转为
 * 右侧输出，写入边接 variable 节点新增的 in 端口；写入边不参与环检测与布局分层。
 *
 * <p>v9 → v10 AC 图边化（规格 nodegraph-ac-graph-and-effects）：ac.root.initial_state
 * 与 ac.transition.target 字符串选项 → 按名解析的图边；新增 ref.particle/ref.sound
 * 与实体粒子/音效声明表（旧图无此数据，无迁移动作）。
 *
 * <p>v11 → v12 对象类型落地：变量声明与子图接口参数的 any（旧「object」选项的实际
 * 存储值）迁移为 object（struct）；any 自此回归纯通配语义。
 *
 * <p>v12 → v13 执行时机事件化（规格 nodegraph-event-nodes）：entity.root 的
 * initialize/pre_animation/parent_setup EXEC 槽 → event.* 源节点（exec_out 引链）；
 * ac.state 的 on_entry/on_exit 翻转为 EXEC OUT 源端口。链拓扑不变，导出产物等价。
 *
 * <p>v13 → v14 操作符节点拆分：op.binary / op.unary 的 op 选项固化为节点类型身份
 * （op.add … op.or / op.negate / op.not，一符一类型），op 选项移除；端口与连线不变。
 *
 * <p>纯函数：输入输出均为不可变文档；加载路径（资源包 loader / EprojectIo）统一调用。
 * 已是新格式的文档原样返回。
 */
public final class GraphMigrations {
    /** 迁移链：按版本升序，逐级应用到低于其目标版本的文档。 */
    private static final List<VersionMigration> CHAIN = List.of(
            new V1ToV2Migration(),
            new V2ToV3Migration(),
            new V3ToV4Migration(),
            new V4ToV5Migration(),
            new V5ToV6Migration(),
            new V6ToV7Migration(),
            new V7ToV8Migration(),
            new V8ToV9Migration(),
            new V9ToV10Migration(),
            new V10ToV11Migration(),
            new V11ToV12Migration(),
            new V12ToV13Migration(),
            new V13ToV14Migration());

    private GraphMigrations() {
    }

    /** 迁移整个图库；format_version 升到 {@link GraphLibrary#CURRENT_FORMAT_VERSION}。 */
    public static GraphLibrary migrate(GraphLibrary library) {
        GraphLibrary result = library;
        for (VersionMigration migration : CHAIN) {
            if (result.formatVersion() < migration.fromVersion() + 1) {
                result = migration.apply(result);
            }
        }
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, result.kind(), result.main(),
                result.graphs());
    }
}
