package io.github.tt432.eyelib.nodegraph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * 节点目录：全部节点类型的注册表（规格 §2.2）。
 *
 * <p>端口/选项/种类在这里定义，验证器与代码生成器以 {@link NodeType.Kind} 分派。
 * query/math 的函数目录不在此处——编辑器运行时从 MolangMappingTree 枚举（规格 D4），
 * 图文档只存函数字符串。
 *
 * <p>各分类区块的注册体在同包的 {@code NodeTypes*} 注册类中；本类的静态字段按拆分前的
 * 原文顺序逐一接线，因此登记顺序（= 节点库目录顺序）与拆分前逐字一致。
 */
public final class NodeTypes {
    private NodeTypes() {
    }

    private static final Map<String, NodeType> REGISTRY = new LinkedHashMap<>();

    public static final String CAT_CONSTANT = "constant";
    public static final String CAT_VARIABLE = "variable";
    public static final String CAT_QUERY = "query";
    public static final String CAT_OPERATOR = "operator";
    public static final String CAT_EXEC = "exec";
    public static final String CAT_EVENT = "event";
    public static final String CAT_REF = "reference";
    public static final String CAT_ENTITY = "entity";
    public static final String CAT_RC = "render_controller";
    public static final String CAT_AC = "animation_controller";
    public static final String CAT_SUBGRAPH = "subgraph";

    // ---------- 二元/一元运算符（molang 无 % 运算符） ----------

    /** 算术二元运算符（输出 FLOAT）。 */
    public static final List<String> ARITHMETIC_OPS = List.of("+", "-", "*", "/");
    /** 比较/逻辑二元运算符（输出 BOOL）。 */
    public static final List<String> LOGICAL_OPS = List.of("==", "!=", "<", "<=", ">", ">=", "&&", "||");

    // ---------- 注册 ----------

    /** 登记节点类型（按调用顺序入表；调用顺序 = 节点库目录顺序）。 */
    static NodeType register(NodeType type) {
        REGISTRY.put(type.id(), type);
        return type;
    }

    public static Optional<NodeType> get(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static NodeType require(String id) {
        NodeType type = REGISTRY.get(id);
        if (type == null) {
            throw new IllegalArgumentException("unknown node type: " + id);
        }
        return type;
    }

    public static List<NodeType> all() {
        return List.copyOf(REGISTRY.values());
    }

    // ---------- 常量 ----------

    public static final NodeType CONST_NUMBER = NodeTypesConstants.CONST_NUMBER;
    public static final NodeType CONST_INT = NodeTypesConstants.CONST_INT;
    public static final NodeType CONST_BOOL = NodeTypesConstants.CONST_BOOL;
    public static final NodeType CONST_STRING = NodeTypesConstants.CONST_STRING;
    /** const.color：取色器选择的 RGBA 常量（选项 #AARRGGBB，8bit/通道）。 */
    public static final NodeType CONST_COLOR = NodeTypesConstants.CONST_COLOR;
    /** color.compose：四通道（各默认 1）合成颜色；通道可接任意 float 表达式（动态颜色）。 */
    public static final NodeType COLOR_COMPOSE = NodeTypesConstants.COLOR_COMPOSE;

    // ---------- 变量 ----------

    /**
     * variable：黑板变量本身（变量面板拖拽产物）。name 不带根（与 {@link VariableDecl#name()} 一致）。
     * 左读右写（规格 §3.1，v9）：out（右）接任意值端口 = 隐式读；in（左，multi）接
     * exec.set_var.target / ref write: 输出 = 写入通道。写入边不参与环检测与布局分层。
     */
    public static final NodeType VARIABLE = NodeTypesVariables.VARIABLE;

    /** 写入通道判定：指向 variable 节点 in 端口的边（v9 左读右写）。 */
    public static boolean isVariableWriteInput(String nodeTypeId, String portId) {
        return NodeTypesVariables.isVariableWriteInput(nodeTypeId, portId);
    }

    public static final NodeType CONTEXT_GET = NodeTypesVariables.CONTEXT_GET;
    public static final NodeType TEMP_GET = NodeTypesVariables.TEMP_GET;
    /** exec.set_var：黑板变量写入。target（右侧输出）接 variable 节点的 in（写身份）；value 为写入值。 */
    public static final NodeType EXEC_SET_VAR = NodeTypesVariables.EXEC_SET_VAR;
    /** exec.set_temp：瞬态 temp 赋值（不进变量面板；name 可带不带 temp. 前缀）。 */
    public static final NodeType EXEC_SET_TEMP = NodeTypesVariables.EXEC_SET_TEMP;

    // ---------- 查询与数学 ----------

    public static final NodeType QUERY_CALL = NodeTypesCalls.QUERY_CALL;
    public static final NodeType MATH_CALL = NodeTypesCalls.MATH_CALL;
    public static final NodeType EXEC_CALL = NodeTypesCalls.EXEC_CALL;

    // ---------- 运算 ----------
    // v14 起一符一节点类型（规格 nodegraph-workbench §2 拆分决策）：操作符是类型身份，
    // 不再有 op 实例选项；输出端口类型在注册时固化（逻辑/比较 → BOOL，其余 → FLOAT）。

    /** 二元操作符：符号 → 节点类型（符号表顺序）。 */
    public static final Map<String, NodeType> BINARY_OP_TYPES = NodeTypesOperators.BINARY_OP_TYPES;
    /** 一元操作符：符号 → 节点类型。 */
    public static final Map<String, NodeType> UNARY_OP_TYPES = NodeTypesOperators.UNARY_OP_TYPES;

    /** 二元操作符符号 → 节点类型；未知符号返回 null。 */
    public static @Nullable NodeType binaryOp(String symbol) {
        return NodeTypesOperators.binaryOp(symbol);
    }

    /** 一元操作符符号 → 节点类型；未知符号返回 null。 */
    public static @Nullable NodeType unaryOp(String symbol) {
        return NodeTypesOperators.unaryOp(symbol);
    }

    /** 操作符节点类型 id → 符号；非操作符类型抛 {@link IllegalArgumentException}。 */
    public static String opSymbolOf(String typeId) {
        return NodeTypesOperators.opSymbolOf(typeId);
    }

    public static final NodeType OP_TERNARY = NodeTypesOperators.OP_TERNARY;
    public static final NodeType OP_NULLCOALESCE = NodeTypesOperators.OP_NULLCOALESCE;

    // ---------- 执行流 ----------

    public static final NodeType EXEC_LOOP = NodeTypesExecFlow.EXEC_LOOP;
    public static final NodeType EXEC_FOREACH = NodeTypesExecFlow.EXEC_FOREACH;
    // 终端节点（break/continue/return）也带 exec_out：我们的链是「反向汇入槽」模型，
    // 没有 exec_out 的节点无法接入 loop.body 等 EXEC 槽；其后的语句为死代码（语义上正确）。
    public static final NodeType EXEC_BREAK = NodeTypesExecFlow.EXEC_BREAK;
    public static final NodeType EXEC_CONTINUE = NodeTypesExecFlow.EXEC_CONTINUE;
    public static final NodeType EXEC_RETURN = NodeTypesExecFlow.EXEC_RETURN;

    // ---------- 资源引用 ----------

    public static final NodeType REF_GEOMETRY = NodeTypesRefs.REF_GEOMETRY;
    public static final NodeType REF_TEXTURE = NodeTypesRefs.REF_TEXTURE;
    public static final NodeType REF_MATERIAL = NodeTypesRefs.REF_MATERIAL;

    /**
     * ref 节点的变量引用端口（规格 nodegraph-animation-variable-refs §2.1）：
     * 每个被引用的实体级 molang 变量一个命名端口——`read:<name>`（该动画/AC 读取）
     * / `write:<name>`（该动画/AC 写入），VARIABLE 类型单连接，label 标注 v.<name> 读/写。
     * 端口集由节点选项快照 {@link #VAR_REFS_OPTION}（导入时按被引内容提取写入）驱动；
     * 声明通道——不参与值发射计数与未连接检查。
     */
    public static final String VAR_REFS_OPTION = "var_refs";
    public static final String VAR_READ_PREFIX = "read:";
    public static final String VAR_WRITE_PREFIX = "write:";

    /** 变量引用声明端口判定（EmitSession/GraphValidator 的豁免口径）。 */
    public static boolean isVarRefPort(String portId) {
        return NodeTypesRefs.isVarRefPort(portId);
    }

    /**
     * 变量引用端口 id：名字中的 '.' 编码为 ':'（LDLib2 端口 id 禁用 '.'——保留给子端口；
     * molang 变量名是 IDENT(.IDENT)* 不含 ':'，编码在可达名字集上无碰撞）。
     * 连线两端与 {@link #varRefReadPorts}/{@link #varRefWritePorts} 必须同走本函数。
     */
    public static String varRefPortId(String prefix, String name) {
        return NodeTypesRefs.varRefPortId(prefix, name);
    }

    /** 从选项快照派生命名变量端口（无快照 = 无端口）。 */
    public static List<PortDef> varRefPorts(NodeInstance instance) {
        return NodeTypesRefs.varRefPorts(instance);
    }

    /** 读端口（左侧输入）：快照 reads 集。 */
    public static List<PortDef> varRefReadPorts(NodeInstance instance) {
        return NodeTypesRefs.varRefReadPorts(instance);
    }

    /** 写端口（右侧输出）：快照 writes 集。 */
    public static List<PortDef> varRefWritePorts(NodeInstance instance) {
        return NodeTypesRefs.varRefWritePorts(instance);
    }

    public static final NodeType REF_ANIMATION = NodeTypesRefs.REF_ANIMATION;
    public static final NodeType REF_AC = NodeTypesRefs.REF_AC;
    public static final NodeType REF_RC = NodeTypesRefs.REF_RC;
    /** ref.particle：粒子效果引用（v10）。实体级声明接 entity.root.particles；
     * AC state 侧经 particle.entry.ref 引用。 */
    public static final NodeType REF_PARTICLE = NodeTypesRefs.REF_PARTICLE;
    /** ref.sound：音效引用（v10）。实体级声明接 entity.root.sounds；AC state 直接接 sounds 槽。 */
    public static final NodeType REF_SOUND = NodeTypesRefs.REF_SOUND;

    // ---------- 实体装配 ----------

    /** entity.root 的声明端口（v4：动画/AC 声明在实体级（双消费端：animate 脚本 + AC 状态机）；
     * v10：粒子/音效同机制）。 */
    public static final Map<String, String> ENTITY_DECLARATION_PORTS = Map.of(
            "ref.animation", "animations",
            "ref.ac", "animation_controllers",
            "ref.particle", "particles",
            "ref.sound", "sounds");

    /** ref.rc 的声明端口（v6 起仅 ref.rc 持有）：外部 RC 没有值端口，
     * decl_* 是它的表行唯一声明通道；rc.root 的引用集由三值端口决定（规格 §3.1）。
     * decl_ 前缀避免与 rc.root 的 textures/materials SLOT 端口撞名。
     * GraphMigrations v5→v6 与导入器剩余行处理也使用本表。 */
    public static final Map<String, String> RC_DECLARATION_PORTS = Map.of(
            "ref.geometry", "decl_geometries",
            "ref.texture", "decl_textures",
            "ref.material", "decl_materials");

    /** v3 历史映射（仅 GraphMigrations v2→v3 使用）：五类 ref → entity.root 声明端口。 */
    public static final Map<String, String> DECLARATION_PORTS = Map.of(
            "ref.geometry", "geometries",
            "ref.texture", "textures",
            "ref.material", "materials",
            "ref.animation", "animations",
            "ref.ac", "animation_controllers");

    /** extra_fields 选项 id：未建模 description 字段的原文直通（JSON 文本，无 NodeOptionDef——
     *  编辑器侧走 LibraryContext.extraOptions 侧表往返，无 UI 行，构建回写）。 */
    public static final String EXTRA_FIELDS_OPTION = "extra_fields";
    /** extra_scripts 选项 id：scripts 对象内未建模键的原文直通（同 extra_fields，合并目标是 scripts 对象）。 */
    public static final String EXTRA_SCRIPTS_OPTION = "extra_scripts";

    public static final NodeType ENTITY_ROOT = NodeTypesEntity.ENTITY_ROOT;
    public static final NodeType ANIMATE_ENTRY = NodeTypesEntity.ANIMATE_ENTRY;

    // ---------- Event（执行时机锚点，v13） ----------

    /** event.initialize：ClientEntity scripts.initialize 的执行时机源节点。 */
    public static final NodeType EVENT_INITIALIZE = NodeTypesEvents.EVENT_INITIALIZE;
    /** event.pre_animation：scripts.pre_animation 的执行时机源节点。 */
    public static final NodeType EVENT_PRE_ANIMATION = NodeTypesEvents.EVENT_PRE_ANIMATION;
    /** event.parent_setup：scripts.parent_setup 的执行时机源节点。 */
    public static final NodeType EVENT_PARENT_SETUP = NodeTypesEvents.EVENT_PARENT_SETUP;

    /** event 节点类型 id → scripts 槽名（装配/迁移/验证共用；同类事件每张主图至多 1 个）。 */
    public static final Map<String, String> EVENT_SCRIPT_SLOTS = Map.of(
            "event.initialize", "initialize",
            "event.pre_animation", "pre_animation",
            "event.parent_setup", "parent_setup");

    // ---------- RenderController ----------

    public static final NodeType RC_ROOT = NodeTypesRenderController.RC_ROOT;
    public static final NodeType LIST_ENTRY = NodeTypesRenderController.LIST_ENTRY;
    public static final NodeType MATERIAL_ENTRY = NodeTypesRenderController.MATERIAL_ENTRY;
    public static final NodeType PART_VISIBILITY_ENTRY = NodeTypesRenderController.PART_VISIBILITY_ENTRY;

    // ---------- AnimationController ----------

    public static final NodeType AC_ROOT = NodeTypesAnimationController.AC_ROOT;
    public static final NodeType AC_STATE = NodeTypesAnimationController.AC_STATE;
    public static final NodeType AC_TRANSITION = NodeTypesAnimationController.AC_TRANSITION;
    /** particle.entry：AC state 的粒子效果条目（v10）。ref 槽接 ref.particle；
     * script = pre_effect_script（molang 值槽，未连线 = 不输出该字段）。 */
    public static final NodeType PARTICLE_ENTRY = NodeTypesAnimationController.PARTICLE_ENTRY;

    // ---------- 子图 ----------

    public static final NodeType SUBGRAPH_CALL = NodeTypesSubgraph.SUBGRAPH_CALL;
    /** 子图输入锚点：动态输出 = 所在图接口的输入参数。 */
    public static final NodeType SUBGRAPH_INPUT = NodeTypesSubgraph.SUBGRAPH_INPUT;
    /** 子图输出锚点：exec_in（可空）+ result 输入（类型随所在图接口输出）。 */
    public static final NodeType SUBGRAPH_OUTPUT = NodeTypesSubgraph.SUBGRAPH_OUTPUT;
}
