package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 节点目录：全部节点类型的注册表（规格 §2.2）。
 *
 * <p>端口/选项/种类在这里定义，验证器与代码生成器以 {@link NodeType.Kind} 分派。
 * query/math 的函数目录不在此处——编辑器运行时从 MolangMappingTree 枚举（规格 D4），
 * 图文档只存函数字符串。
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
    public static final List<String> BINARY_OPS;

    static {
        List<String> ops = new ArrayList<>(ARITHMETIC_OPS);
        ops.addAll(LOGICAL_OPS);
        BINARY_OPS = List.copyOf(ops);
    }

    public static final List<String> UNARY_OPS = List.of("-", "!");

    // ---------- 执行流端口 ----------

    private static PortDef execIn() {
        return new PortDef("exec_in", PortDirection.IN, PortType.EXEC, Optional.empty(), false);
    }

    private static PortDef execOut() {
        return new PortDef("exec_out", PortDirection.OUT, PortType.EXEC, Optional.empty(), false);
    }

    private static PortDef slotOut(String id) {
        return PortDef.outSingle(id, PortType.SLOT);
    }

    private static PortDef slotIn(String id) {
        return PortDef.inMulti(id, PortType.SLOT);
    }

    // ---------- 注册 ----------

    private static NodeType register(NodeType type) {
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

    public static final NodeType CONST_NUMBER = register(NodeType.of(
            "const.number", NodeType.Kind.CONST_NUMBER, CAT_CONSTANT,
            List.of(NodeOptionDef.number("value", 0)),
            List.of(),
            List.of(PortDef.out("out", PortType.FLOAT))));

    public static final NodeType CONST_INT = register(NodeType.of(
            "const.int", NodeType.Kind.CONST_INT, CAT_CONSTANT,
            List.of(NodeOptionDef.integer("value", 0)),
            List.of(),
            List.of(PortDef.out("out", PortType.INT))));

    public static final NodeType CONST_BOOL = register(NodeType.of(
            "const.bool", NodeType.Kind.CONST_BOOL, CAT_CONSTANT,
            List.of(NodeOptionDef.bool("value", false)),
            List.of(),
            List.of(PortDef.out("out", PortType.BOOL))));

    public static final NodeType CONST_STRING = register(NodeType.of(
            "const.string", NodeType.Kind.CONST_STRING, CAT_CONSTANT,
            List.of(NodeOptionDef.string("value", "")),
            List.of(),
            List.of(PortDef.out("out", PortType.STRING))));

    /** const.color：取色器选择的 RGBA 常量（选项 #AARRGGBB，8bit/通道）。 */
    public static final NodeType CONST_COLOR = register(NodeType.of(
            "const.color", NodeType.Kind.CONST_COLOR, CAT_CONSTANT,
            List.of(NodeOptionDef.color("value", ColorValues.WHITE)),
            List.of(),
            List.of(PortDef.out("out", PortType.COLOR))));

    /** color.compose：四通道（各默认 1）合成颜色；通道可接任意 float 表达式（动态颜色）。 */
    public static final NodeType COLOR_COMPOSE = register(NodeType.of(
            "color.compose", NodeType.Kind.COLOR_COMPOSE, CAT_OPERATOR,
            List.of(),
            List.of(PortDef.in("r", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("g", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("b", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("a", PortType.FLOAT, new JsonPrimitive(1))),
            List.of(PortDef.out("out", PortType.COLOR))));

    // ---------- 变量 ----------

    /**
     * variable：黑板变量本身（变量面板拖拽产物）。name 不带根（与 {@link VariableDecl#name()} 一致）。
     * 左读右写（规格 §3.1，v9）：out（右）接任意值端口 = 隐式读；in（左，multi）接
     * exec.set_var.target / ref write: 输出 = 写入通道。写入边不参与环检测与布局分层。
     */
    public static final NodeType VARIABLE = register(NodeType.of(
            "variable", NodeType.Kind.VARIABLE, CAT_VARIABLE,
            List.of(NodeOptionDef.string("name", "foo")),
            List.of(PortDef.inMulti("in", PortType.VARIABLE)),
            List.of(PortDef.out("out", PortType.VARIABLE))));

    /** 写入通道判定：指向 variable 节点 in 端口的边（v9 左读右写）。 */
    public static boolean isVariableWriteInput(String nodeTypeId, String portId) {
        return VARIABLE.id().equals(nodeTypeId) && "in".equals(portId);
    }

    public static final NodeType CONTEXT_GET = register(NodeType.of(
            "context.get", NodeType.Kind.CONTEXT_GET, CAT_VARIABLE,
            List.of(NodeOptionDef.string("name", "context.other")),
            List.of(),
            List.of(PortDef.out("out", PortType.ANY))));

    public static final NodeType TEMP_GET = register(NodeType.of(
            "temp.get", NodeType.Kind.TEMP_GET, CAT_VARIABLE,
            List.of(NodeOptionDef.string("name", "temp.t")),
            List.of(),
            List.of(PortDef.out("out", PortType.ANY))));

    /** exec.set_var：黑板变量写入。target（右侧输出）接 variable 节点的 in（写身份）；value 为写入值。 */
    public static final NodeType EXEC_SET_VAR = register(NodeType.of(
            "exec.set_var", NodeType.Kind.EXEC_SET_VAR, CAT_EXEC,
            List.of(),
            List.of(execIn(),
                    PortDef.in("value", PortType.ANY, new JsonPrimitive(0))),
            List.of(execOut(), PortDef.outSingle("target", PortType.VARIABLE))));

    /** exec.set_temp：瞬态 temp 赋值（不进变量面板；name 可带不带 temp. 前缀）。 */
    public static final NodeType EXEC_SET_TEMP = register(NodeType.of(
            "exec.set_temp", NodeType.Kind.EXEC_SET_TEMP, CAT_EXEC,
            List.of(NodeOptionDef.string("name", "temp.t")),
            List.of(execIn(), PortDef.in("value", PortType.ANY, new JsonPrimitive(0))),
            List.of(execOut())));

    // ---------- 查询与数学 ----------

    /**
     * call 类节点的 arg 端口：函数在 {@link MolangFunctionSignatures} 有签名时按签名生成
     * （定长：端口数 = 参数数，arg_count 选项失效；变长：max(arg_count, 固定前缀 + 1)），
     * 端口 id 恒为 argN（连线稳定），参数名/类型进显示 label；端口物理类型保持 ANY
     * （ANY 输入才有行内字面值编辑器）。未知函数退回 arg_count × ANY 无标注。
     */
    private static List<PortDef> callArgPorts(NodeInstance instance, int defaultArgCount) {
        MolangFunctionSignatures.Signature sig =
                MolangFunctionSignatures.find(instance.optionString("function", ""));
        if (sig != null) {
            List<MolangFunctionSignatures.Arg> fixed = sig.fixed();
            MolangFunctionSignatures.Arg varArg = sig.varArg();
            int total = varArg == null
                    ? fixed.size()
                    : Math.max(instance.optionInt("arg_count", defaultArgCount), fixed.size() + 1);
            List<PortDef> ports = new ArrayList<>(total);
            for (int i = 0; i < total; i++) {
                MolangFunctionSignatures.Arg arg;
                if (i < fixed.size()) {
                    arg = fixed.get(i);
                } else if (varArg != null) {
                    arg = new MolangFunctionSignatures.Arg(
                            varArg.name() + "[" + (i - fixed.size()) + "]",
                            varArg.kind());
                } else {
                    continue; // 定长签名不会走到（total == fixed.size()）
                }
                String label = arg.kind() == MolangFunctionSignatures.ArgKind.STRING
                        ? arg.name() + ": str"
                        : arg.name();
                ports.add(PortDef.inLabeled("arg" + (i + 1), PortType.ANY, label));
            }
            return ports;
        }
        int argCount = Math.max(0, Math.min(instance.optionInt("arg_count", defaultArgCount), 16));
        List<PortDef> ports = new ArrayList<>();
        for (int i = 1; i <= argCount; i++) {
            ports.add(PortDef.in("arg" + i, PortType.ANY));
        }
        return ports;
    }

    public static final NodeType QUERY_CALL = register(NodeType.dynamic(
            "query.call", NodeType.Kind.QUERY_CALL, CAT_QUERY,
            List.of(
                    NodeOptionDef.asset("function", "query.anim_time", "molang.query"),
                    NodeOptionDef.integer("arg_count", 0)),
            (instance, resolver) -> callArgPorts(instance, 0),
            NodeType.PortProvider.fixed(List.of(PortDef.out("out", PortType.ANY)))));

    public static final NodeType MATH_CALL = register(NodeType.dynamic(
            "math.call", NodeType.Kind.MATH_CALL, CAT_QUERY,
            List.of(
                    NodeOptionDef.asset("function", "math.sin", "molang.math"),
                    NodeOptionDef.integer("arg_count", 1)),
            (instance, resolver) -> callArgPorts(instance, 1),
            NodeType.PortProvider.fixed(List.of(PortDef.out("out", PortType.FLOAT)))));

    public static final NodeType EXEC_CALL = register(NodeType.dynamic(
            "exec.call", NodeType.Kind.EXEC_CALL, CAT_EXEC,
            List.of(
                    NodeOptionDef.asset("function", "query.foo", "molang.call"),
                    NodeOptionDef.integer("arg_count", 0)),
            (instance, resolver) -> {
                List<PortDef> ports = new ArrayList<>();
                ports.add(execIn());
                ports.addAll(callArgPorts(instance, 0));
                return ports;
            },
            NodeType.PortProvider.fixed(List.of(execOut()))));

    // ---------- 运算 ----------

    public static final NodeType OP_BINARY = register(NodeType.dynamic(
            "op.binary", NodeType.Kind.OP_BINARY, CAT_OPERATOR,
            List.of(NodeOptionDef.enumeration("op", "+", BINARY_OPS)),
            NodeType.PortProvider.fixed(List.of(
                    PortDef.in("a", PortType.ANY, new JsonPrimitive(0)),
                    PortDef.in("b", PortType.ANY, new JsonPrimitive(0)))),
            (instance, resolver) -> {
                String op = instance.optionString("op", "+");
                PortType out = LOGICAL_OPS.contains(op) ? PortType.BOOL : PortType.FLOAT;
                return List.of(PortDef.out("out", out));
            }));

    public static final NodeType OP_UNARY = register(NodeType.dynamic(
            "op.unary", NodeType.Kind.OP_UNARY, CAT_OPERATOR,
            List.of(NodeOptionDef.enumeration("op", "-", UNARY_OPS)),
            NodeType.PortProvider.fixed(List.of(
                    PortDef.in("a", PortType.ANY, new JsonPrimitive(0)))),
            (instance, resolver) -> {
                String op = instance.optionString("op", "-");
                PortType out = "!".equals(op) ? PortType.BOOL : PortType.FLOAT;
                return List.of(PortDef.out("out", out));
            }));

    public static final NodeType OP_TERNARY = register(NodeType.of(
            "op.ternary", NodeType.Kind.OP_TERNARY, CAT_OPERATOR,
            List.of(),
            List.of(
                    PortDef.in("cond", PortType.BOOL, new JsonPrimitive(1)),
                    PortDef.in("a", PortType.ANY, new JsonPrimitive(0)),
                    PortDef.in("b", PortType.ANY, new JsonPrimitive(0))),
            List.of(PortDef.out("out", PortType.ANY))));

    public static final NodeType OP_NULLCOALESCE = register(NodeType.of(
            "op.null_coalesce", NodeType.Kind.OP_NULLCOALESCE, CAT_OPERATOR,
            List.of(),
            List.of(
                    PortDef.in("a", PortType.ANY, new JsonPrimitive(0)),
                    PortDef.in("b", PortType.ANY, new JsonPrimitive(0))),
            List.of(PortDef.out("out", PortType.ANY))));

    // ---------- 执行流 ----------

    public static final NodeType EXEC_LOOP = register(NodeType.of(
            "exec.loop", NodeType.Kind.EXEC_LOOP, CAT_EXEC,
            List.of(),
            List.of(
                    execIn(),
                    PortDef.in("count", PortType.INT, new JsonPrimitive(10)),
                    new PortDef("body", PortDirection.IN, PortType.EXEC, Optional.empty(), false)),
            List.of(execOut())));

    public static final NodeType EXEC_FOREACH = register(NodeType.of(
            "exec.for_each", NodeType.Kind.EXEC_FOREACH, CAT_EXEC,
            List.of(NodeOptionDef.string("var_name", "temp.item")),
            List.of(
                    execIn(),
                    PortDef.in("array", PortType.ARRAY),
                    new PortDef("body", PortDirection.IN, PortType.EXEC, Optional.empty(), false)),
            List.of(execOut())));

    // 终端节点（break/continue/return）也带 exec_out：我们的链是「反向汇入槽」模型，
    // 没有 exec_out 的节点无法接入 loop.body 等 EXEC 槽；其后的语句为死代码（语义上正确）。
    public static final NodeType EXEC_BREAK = register(NodeType.of(
            "exec.break", NodeType.Kind.EXEC_BREAK, CAT_EXEC,
            List.of(),
            List.of(execIn()),
            List.of(execOut())));

    public static final NodeType EXEC_CONTINUE = register(NodeType.of(
            "exec.continue", NodeType.Kind.EXEC_CONTINUE, CAT_EXEC,
            List.of(),
            List.of(execIn()),
            List.of(execOut())));

    public static final NodeType EXEC_RETURN = register(NodeType.of(
            "exec.return", NodeType.Kind.EXEC_RETURN, CAT_EXEC,
            List.of(),
            List.of(
                    execIn(),
                    PortDef.in("value", PortType.ANY, new JsonPrimitive(0))),
            List.of(execOut())));

    // ---------- 资源引用 ----------

    public static final NodeType REF_GEOMETRY = register(NodeType.of(
            "ref.geometry", NodeType.Kind.REF_GEOMETRY, CAT_REF,
            List.of(
                    // short_name 空 = 派生（ShortNames.effective）；非空 = 显式覆盖（外部契约逃生舱）
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "geometry.example.model", "geometry")),
            List.of(),
            List.of(PortDef.out("ref", PortType.GEOMETRY_REF))));

    public static final NodeType REF_TEXTURE = register(NodeType.of(
            "ref.texture", NodeType.Kind.REF_TEXTURE, CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("path", "textures/entity/example", "texture")),
            List.of(),
            List.of(PortDef.out("ref", PortType.TEXTURE_REF))));

    public static final NodeType REF_MATERIAL = register(NodeType.of(
            "ref.material", NodeType.Kind.REF_MATERIAL, CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("material", "entity_alphatest", "material")),
            List.of(),
            List.of(PortDef.out("ref", PortType.MATERIAL_REF))));

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
        return portId.startsWith(VAR_READ_PREFIX) || portId.startsWith(VAR_WRITE_PREFIX);
    }

    /** 从选项快照派生命名变量端口（无快照 = 无端口）。 */
    public static List<PortDef> varRefPorts(NodeInstance instance) {
        List<PortDef> ports = new ArrayList<>(varRefReadPorts(instance));
        ports.addAll(varRefWritePorts(instance));
        return ports;
    }

    /** 读端口（左侧输入）：快照 reads 集。 */
    public static List<PortDef> varRefReadPorts(NodeInstance instance) {
        com.google.gson.JsonElement raw = instance.options().get(VAR_REFS_OPTION);
        if (raw == null || !raw.isJsonObject()) {
            return List.of();
        }
        com.google.gson.JsonArray reads = raw.getAsJsonObject().getAsJsonArray("reads");
        if (reads == null) {
            return List.of();
        }
        List<PortDef> ports = new ArrayList<>();
        for (com.google.gson.JsonElement e : reads) {
            String name = e.getAsString();
            // 左读右写（v9）：读 = 左侧输入，写 = 右侧输出；位置即语义，不加文字标记
            ports.add(PortDef.inLabeled(VAR_READ_PREFIX + name, PortType.VARIABLE, "v." + name));
        }
        return ports;
    }

    /** 写端口（右侧输出）：快照 writes 集。 */
    public static List<PortDef> varRefWritePorts(NodeInstance instance) {
        com.google.gson.JsonElement raw = instance.options().get(VAR_REFS_OPTION);
        if (raw == null || !raw.isJsonObject()) {
            return List.of();
        }
        com.google.gson.JsonArray writes = raw.getAsJsonObject().getAsJsonArray("writes");
        if (writes == null) {
            return List.of();
        }
        List<PortDef> ports = new ArrayList<>();
        for (com.google.gson.JsonElement e : writes) {
            String name = e.getAsString();
            ports.add(PortDef.outLabeled(VAR_WRITE_PREFIX + name, PortType.VARIABLE, "v." + name));
        }
        return ports;
    }

    public static final NodeType REF_ANIMATION = register(NodeType.dynamic(
            "ref.animation", NodeType.Kind.REF_ANIMATION, CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "animation.example.walk", "animation")),
            (instance, resolver) -> varRefReadPorts(instance),
            (instance, resolver) -> {
                List<PortDef> outs = new ArrayList<>();
                outs.add(PortDef.out("ref", PortType.ANIMATION_REF));
                outs.addAll(varRefWritePorts(instance));
                return outs;
            }));

    public static final NodeType REF_AC = register(NodeType.of(
            "ref.ac", NodeType.Kind.REF_AC, CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "controller.animation.example.main", "ac")),
            // v8：ref.ac 不建变量命名端口——AC 的变量引用由 AC 图自身承担（ac.state 的
            // on_entry/on_exit exec 链内 variable 节点即真实引用）；动画无图形态，
            // ref.animation 保留 read:/write: 命名端口（规格 §2.1，用户决策 2026-08-08）
            List.of(),
            List.of(PortDef.out("ref", PortType.AC_REF))));

    public static final NodeType REF_RC = register(NodeType.of(
            "ref.rc", NodeType.Kind.REF_RC, CAT_REF,
            List.of(NodeOptionDef.asset("identifier", "controller.render.example", "rc")),
            List.of(
                    // v4：外部 RC 的挂载条件 + 为它准备的 geo/tex/mat 表行（规格 §2.2）
                    PortDef.in("condition", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.inMulti("decl_geometries", PortType.GEOMETRY_REF),
                    PortDef.inMulti("decl_textures", PortType.TEXTURE_REF),
                    PortDef.inMulti("decl_materials", PortType.MATERIAL_REF)),
            List.of(PortDef.out("ref", PortType.RC_REF))));

    /** ref.particle：粒子效果引用（v10）。实体级声明接 entity.root.particles；
     * AC state 侧经 particle.entry.ref 引用。 */
    public static final NodeType REF_PARTICLE = register(NodeType.of(
            "ref.particle", NodeType.Kind.REF_PARTICLE, CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "example:my_particle", "particle")),
            List.of(),
            List.of(PortDef.out("ref", PortType.PARTICLE_REF))));

    /** ref.sound：音效引用（v10）。实体级声明接 entity.root.sounds；AC state 直接接 sounds 槽。 */
    public static final NodeType REF_SOUND = register(NodeType.of(
            "ref.sound", NodeType.Kind.REF_SOUND, CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "example:my_sound", "sound")),
            List.of(),
            List.of(PortDef.out("ref", PortType.SOUND_REF))));

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

    public static final NodeType ENTITY_ROOT = register(NodeType.of(
            "entity.root", NodeType.Kind.ENTITY_ROOT, CAT_ENTITY,
            List.of(NodeOptionDef.string("identifier", "example:my_entity")),
            List.of(
                    new PortDef("initialize", PortDirection.IN, PortType.EXEC, Optional.empty(), false),
                    new PortDef("pre_animation", PortDirection.IN, PortType.EXEC, Optional.empty(), false),
                    new PortDef("parent_setup", PortDirection.IN, PortType.EXEC, Optional.empty(), false),
                    PortDef.in("scale", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("scale_x", PortType.FLOAT),
                    PortDef.in("scale_y", PortType.FLOAT),
                    PortDef.in("scale_z", PortType.FLOAT),
                    // 声明端口（v4：动画/AC 在实体级；geo/tex/mat 锚点在 RC，见 RC_DECLARATION_PORTS）
                    PortDef.inMulti("animations", PortType.ANIMATION_REF),
                    PortDef.inMulti("animation_controllers", PortType.AC_REF),
                    // v10：粒子/音效声明表（spec nodegraph-ac-graph-and-effects §2.2）
                    PortDef.inMulti("particles", PortType.PARTICLE_REF),
                    PortDef.inMulti("sounds", PortType.SOUND_REF),
                    slotIn("animate"),
                    // v4：直连 rc.root.controller / ref.rc.ref（条件在源节点的 condition 端口）
                    PortDef.inMulti("render_controllers", PortType.RC_REF)),
            List.of()));

    public static final NodeType ANIMATE_ENTRY = register(NodeType.of(
            "animate.entry", NodeType.Kind.ANIMATE_ENTRY, CAT_ENTITY,
            List.of(),
            List.of(
                    PortDef.in("ref", PortType.ANY),
                    PortDef.in("weight", PortType.FLOAT, new JsonPrimitive(1))),
            List.of(slotOut("entry"))));

    // ---------- RenderController ----------

    public static final NodeType RC_ROOT = register(NodeType.of(
            "rc.root", NodeType.Kind.RC_ROOT, CAT_RC,
            List.of(
                    NodeOptionDef.string("identifier", "controller.render.example"),
                    NodeOptionDef.bool("ignore_lighting", false),
                    NodeOptionDef.of("arrays", NodeOptionDef.OptionType.TEXT, new JsonPrimitive(""))),
            List.of(
                    // v4：内联进实体画布时的挂载条件（规格 §2.1）；独立 RC 库中闲置
                    // v6：decl_* 已删——geo/tex/mat 引用集由下方 geometry/textures/materials
                    // 三端口的连线决定（规格 nodegraph-ordered-entries-and-rc-reference-set §3.1）
                    PortDef.in("condition", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("geometry", PortType.STRING, new JsonPrimitive("geometry.default")),
                    slotIn("textures"),
                    slotIn("materials"),
                    slotIn("part_visibility"),
                    PortDef.in("color", PortType.COLOR),
                    PortDef.in("is_hurt_color", PortType.COLOR),
                    PortDef.in("on_fire_color", PortType.COLOR),
                    PortDef.in("overlay_color", PortType.COLOR)),
            // v4：接 entity.root.render_controllers = 本实体定义并挂载该 RC
            List.of(PortDef.out("controller", PortType.RC_REF))));

    public static final NodeType LIST_ENTRY = register(NodeType.of(
            "list.entry", NodeType.Kind.LIST_ENTRY, CAT_RC,
            List.of(),
            // v6：next 链（单连接 SLOT）——数组顺序 = 链序（规格 §2.1）
            List.of(PortDef.in("value", PortType.ANY),
                    PortDef.in("next", PortType.SLOT)),
            List.of(slotOut("entry"))));

    public static final NodeType MATERIAL_ENTRY = register(NodeType.of(
            "material.entry", NodeType.Kind.MATERIAL_ENTRY, CAT_RC,
            List.of(NodeOptionDef.string("pattern", "*")),
            List.of(PortDef.in("value", PortType.ANY),
                    PortDef.in("next", PortType.SLOT)),
            List.of(slotOut("entry"))));

    public static final NodeType PART_VISIBILITY_ENTRY = register(NodeType.of(
            "part_visibility.entry", NodeType.Kind.PART_VISIBILITY_ENTRY, CAT_RC,
            List.of(NodeOptionDef.string("bone_pattern", "*")),
            List.of(PortDef.in("condition", PortType.BOOL, new JsonPrimitive(1)),
                    PortDef.in("next", PortType.SLOT)),
            List.of(slotOut("entry"))));

    // ---------- AnimationController ----------

    public static final NodeType AC_ROOT = register(NodeType.of(
            "ac.root", NodeType.Kind.AC_ROOT, CAT_AC,
            List.of(
                    NodeOptionDef.asset("identifier", "controller.animation.example.main", "ac")),
            // v10：initial = 初始 state 的图边（替代 initial_state 字符串选项，规格 §2.1）
            List.of(slotIn("states"),
                    new PortDef("initial", PortDirection.IN, PortType.SLOT, Optional.empty(), false)),
            List.of()));

    public static final NodeType AC_STATE = register(NodeType.of(
            "ac.state", NodeType.Kind.AC_STATE, CAT_AC,
            List.of(
                    // name 仅内部使用（导出 states 键；编辑器 UI 行隐藏，同 short_name 处理）
                    NodeOptionDef.string("name", "default"),
                    NodeOptionDef.number("blend_transition", 0.2),
                    NodeOptionDef.bool("blend_via_shortest_path", false)),
            List.of(
                    new PortDef("on_entry", PortDirection.IN, PortType.EXEC, Optional.empty(), false),
                    new PortDef("on_exit", PortDirection.IN, PortType.EXEC, Optional.empty(), false),
                    slotIn("animations"),
                    slotIn("transitions"),
                    // v10：incoming = 指向本状态的 transition 图边；粒子/音效（规格 §2.1/2.3）
                    slotIn("incoming"),
                    slotIn("particles"),
                    PortDef.inMulti("sounds", PortType.SOUND_REF)),
            List.of(slotOut("state"))));

    public static final NodeType AC_TRANSITION = register(NodeType.of(
            "ac.transition", NodeType.Kind.AC_TRANSITION, CAT_AC,
            // v10：target 从字符串选项改为指向目标 state 的 SLOT 图边
            List.of(),
            List.of(PortDef.in("condition", PortType.FLOAT, new JsonPrimitive(1))),
            List.of(slotOut("transition"),
                    new PortDef("target", PortDirection.OUT, PortType.SLOT, Optional.empty(), false))));

    /** particle.entry：AC state 的粒子效果条目（v10）。ref 槽接 ref.particle；
     * script = pre_effect_script（molang 值槽，未连线 = 不输出该字段）。 */
    public static final NodeType PARTICLE_ENTRY = register(NodeType.of(
            "particle.entry", NodeType.Kind.PARTICLE_ENTRY, CAT_AC,
            List.of(
                    NodeOptionDef.string("locator", ""),
                    NodeOptionDef.bool("bind_to_actor", true)),
            List.of(
                    PortDef.in("ref", PortType.ANY),
                    PortDef.in("script", PortType.ANY)),
            List.of(slotOut("entry"))));

    // ---------- 子图 ----------

    public static final NodeType SUBGRAPH_CALL = register(NodeType.dynamic(
            "subgraph.call", NodeType.Kind.SUBGRAPH_CALL, CAT_SUBGRAPH,
            List.of(NodeOptionDef.string("subgraph", "")),
            (instance, resolver) -> {
                String name = instance.optionString("subgraph", "");
                return resolver.resolve(name)
                        .map(iface -> iface.inputs().stream()
                                .<PortDef>map(p -> p.defaultValue()
                                        .map(d -> new PortDef(p.name(), PortDirection.IN, p.type(), Optional.of(d), false))
                                        .orElse(PortDef.in(p.name(), p.type())))
                                .toList())
                        .orElse(List.of());
            },
            (instance, resolver) -> {
                String name = instance.optionString("subgraph", "");
                return resolver.resolve(name)
                        .map(iface -> List.of(PortDef.out("result", iface.output().type())))
                        .orElse(List.of(PortDef.out("result", PortType.ANY)));
            }));

    /** 子图输入锚点：动态输出 = 所在图接口的输入参数。 */
    public static final NodeType SUBGRAPH_INPUT = register(NodeType.dynamic(
            "subgraph.input", NodeType.Kind.SUBGRAPH_INPUT, CAT_SUBGRAPH,
            List.of(),
            NodeType.PortProvider.fixed(List.of()),
            (instance, resolver) -> resolver.self()
                    .map(iface -> iface.inputs().stream()
                            .<PortDef>map(p -> PortDef.out(p.name(), p.type()))
                            .toList())
                    .orElse(List.of())));

    /** 子图输出锚点：exec_in（可空）+ result 输入（类型随所在图接口输出）。 */
    public static final NodeType SUBGRAPH_OUTPUT = register(NodeType.dynamic(
            "subgraph.output", NodeType.Kind.SUBGRAPH_OUTPUT, CAT_SUBGRAPH,
            List.of(),
            (instance, resolver) -> {
                PortType resultType = resolver.self()
                        .map(iface -> iface.output().type())
                        .orElse(PortType.ANY);
                return List.of(
                        new PortDef("exec_in", PortDirection.IN, PortType.EXEC, Optional.empty(), false),
                        PortDef.in("result", resultType));
            },
            NodeType.PortProvider.fixed(List.of())));
}
