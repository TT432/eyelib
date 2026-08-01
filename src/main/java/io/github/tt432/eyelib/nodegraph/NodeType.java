package io.github.tt432.eyelib.nodegraph;

import java.util.List;
import java.util.Optional;

/**
 * 节点类型（目录条目）。实例无关信息：id、类别、选项定义、端口提供策略。
 *
 * <p>端口分两类：静态端口（大多数节点）与动态端口（由实例选项或所引用子图接口决定，
 * 如 {@code subgraph_call} 的实参端口）。端口 id 在重算间必须稳定。
 */
public final class NodeType {
    private final String id;
    private final Kind kind;
    private final String category;
    private final List<NodeOptionDef> options;
    private final PortProvider inputProvider;
    private final PortProvider outputProvider;

    public NodeType(String id, Kind kind, String category, List<NodeOptionDef> options,
                    PortProvider inputProvider, PortProvider outputProvider) {
        this.id = id;
        this.kind = kind;
        this.category = category;
        this.options = List.copyOf(options);
        this.inputProvider = inputProvider;
        this.outputProvider = outputProvider;
    }

    public static NodeType of(String id, Kind kind, String category,
                              List<NodeOptionDef> options, List<PortDef> inputs, List<PortDef> outputs) {
        return new NodeType(id, kind, category, options, PortProvider.fixed(inputs), PortProvider.fixed(outputs));
    }

    public static NodeType dynamic(String id, Kind kind, String category, List<NodeOptionDef> options,
                                   PortProvider inputProvider, PortProvider outputProvider) {
        return new NodeType(id, kind, category, options, inputProvider, outputProvider);
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public String category() {
        return category;
    }

    public List<NodeOptionDef> options() {
        return options;
    }

    public Optional<NodeOptionDef> option(String id) {
        return options.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    /** 计算实例的输入端口（含动态端口）。 */
    public List<PortDef> inputsOf(NodeInstance instance, SubgraphResolver resolver) {
        return inputProvider.portsOf(instance, resolver);
    }

    /** 计算实例的输出端口（含动态端口）。 */
    public List<PortDef> outputsOf(NodeInstance instance, SubgraphResolver resolver) {
        return outputProvider.portsOf(instance, resolver);
    }

    /**
     * 子图接口解析器：按子图名取接口（供 subgraph_call 动态端口推导）。
     */
    public interface SubgraphResolver {
        Optional<GraphInterface> resolve(String subgraphName);

        /** 当前正在编辑/编译的图自身的接口（subgraph.input/output 锚点用）。 */
        default Optional<GraphInterface> self() {
            return Optional.empty();
        }

        /** 构造一个解析器：name 查库内子图，self 为当前图接口。 */
        static SubgraphResolver of(GraphLibrary library, Optional<GraphInterface> self) {
            return new SubgraphResolver() {
                @Override
                public Optional<GraphInterface> resolve(String subgraphName) {
                    return library.subgraphResolver().resolve(subgraphName);
                }

                @Override
                public Optional<GraphInterface> self() {
                    return self;
                }
            };
        }
    }

    /** 端口提供策略。 */
    @FunctionalInterface
    public interface PortProvider {
        List<PortDef> portsOf(NodeInstance instance, SubgraphResolver resolver);

        static PortProvider fixed(List<PortDef> ports) {
            List<PortDef> copy = List.copyOf(ports);
            return (instance, resolver) -> copy;
        }
    }

    /**
     * 节点种类（代码生成/验证的语义分派键）。
     */
    public enum Kind {
        // 常量
        CONST_NUMBER, CONST_BOOL, CONST_STRING,
        // 变量
        VAR_GET, VAR_SET, TEMP_GET, TEMP_SET,
        // 查询与数学
        QUERY_CALL, MATH_CALL,
        // 运算
        OP_BINARY, OP_UNARY, OP_TERNARY, OP_NULLCOALESCE,
        // 执行流
        EXEC_SET_VAR, EXEC_CALL, EXEC_LOOP, EXEC_FOREACH, EXEC_BREAK, EXEC_CONTINUE, EXEC_RETURN,
        // 资源引用
        REF_GEOMETRY, REF_TEXTURE, REF_MATERIAL, REF_ANIMATION, REF_AC, REF_RC,
        // 实体装配
        ENTITY_ROOT, ANIMATE_ENTRY, RC_CONDITION_ENTRY,
        RC_ROOT, LIST_ENTRY, MATERIAL_ENTRY, PART_VISIBILITY_ENTRY,
        AC_ROOT, AC_STATE, AC_TRANSITION,
        // 子图
        SUBGRAPH_CALL, SUBGRAPH_INPUT, SUBGRAPH_OUTPUT
    }
}
