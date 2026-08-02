package io.github.tt432.eyelib.client.nodegraph.workbench;

import io.github.tt432.eyelib.client.molangdebug.MolangDebugService;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangType;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.codegen.CodegenResult;
import io.github.tt432.eyelib.nodegraph.codegen.MolangGenerator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * 画布节点值徽标模型（规格 nodegraph-workbench §W3）：编辑器无关的共享核心，
 * 两版 LDLib 编辑器只负责把 {@link Badge} 画到节点坐标处。
 *
 * <p>工作流：{@link #setTarget(Entity)} 选定调试目标 →
 * 图内容变化时 {@link #updateGraph(GraphLibrary, String)}（引用比较，COW 文档变了才重发射）→
 * 每帧 {@link #tick()} 对目标 scope 求值 → {@link #badges()} 取结果绘制。
 *
 * <p>发射走 {@link MolangGenerator#emitNodeOutput}（与构建同一 codegen，语义一致）；
 * 求值走 {@link MolangDebugService#eval}（含编译缓存与异常收敛）。跳过：
 * 无值输出端口的节点（装配根/exec-only）、subgraph.* 节点（temp.sg 隔离语义，
 * v1 不在画布求值）、超过 {@value #MAX_BADGES} 个之后的节点。
 */
public final class NodeDebugOverlayModel {
    /** 徽标数量上限（超出部分忽略并在 {@link #truncated()} 暴露）。 */
    public static final int MAX_BADGES = 64;

    /**
     * 一个节点的实时值徽标。
     *
     * @param text  已格式化文本（错误时为错误摘要，无目标/无 scope 时为「—」）
     * @param type  显示类型（推断或声明）
     * @param error 是否为错误（绘制为红字）
     */
    public record Badge(String text, MolangType type, boolean error) {}

    /** 待求值节点：表达式 + 显示用端口类型。 */
    private record Entry(String expression, MolangType displayType, boolean emitError) {}

    private @Nullable Entity target;
    private @Nullable GraphLibrary library;
    private String graphName = "";
    private Map<String, Entry> entries = Map.of();
    private Map<String, Badge> badges = Map.of();
    private boolean truncated;

    /** 选定调试目标（null = 清除，徽标全部显示「—」）。 */
    public void setTarget(@Nullable Entity entity) {
        if (target != entity) {
            target = entity;
            tick();
        }
    }

    public @Nullable Entity target() {
        return target;
    }

    /**
     * 图内容更新（编辑器在任何画布变更后调用）。引用相等则零开销返回；
     * 文档是 COW 不可变 record，任何编辑都会产生新实例。
     */
    public void updateGraph(GraphLibrary newLibrary, String newGraphName) {
        if (library == newLibrary && graphName.equals(newGraphName)) {
            return;
        }
        library = newLibrary;
        graphName = newGraphName;
        reemit();
    }

    /** 当前徽标（nodeUid → Badge）；tick() 后更新。 */
    public Map<String, Badge> badges() {
        return badges;
    }

    /** 节点数超限被截断（编辑器可据此显示提示）。 */
    public boolean truncated() {
        return truncated;
    }

    /** 每帧调用：对当前目标 scope 求值全部表达式。 */
    public void tick() {
        if (entries.isEmpty()) {
            badges = Map.of();
            return;
        }
        Entity t = target;
        MolangScope scope = t != null ? MolangDebugService.resolveScope(t) : null;
        Map<String, Badge> next = new LinkedHashMap<>();
        for (var e : entries.entrySet()) {
            Entry entry = e.getValue();
            if (entry.emitError()) {
                next.put(e.getKey(), new Badge("发射错误", MolangType.DYNAMIC, true));
                continue;
            }
            if (scope == null) {
                next.put(e.getKey(), new Badge("—", entry.displayType(), false));
                continue;
            }
            MolangDebugService.EvalResult result = MolangDebugService.eval(scope, entry.expression());
            if (result.success()) {
                MolangObject value = result.value();
                MolangType type = MolangType.infer(value);
                next.put(e.getKey(), new Badge(type.format(value), type, false));
            } else {
                next.put(e.getKey(), new Badge(shorten(result.error()), MolangType.DYNAMIC, true));
            }
        }
        badges = next;
    }

    // ---------- 内部 ----------

    private void reemit() {
        GraphLibrary lib = library;
        if (lib == null) {
            entries = Map.of();
            truncated = false;
            badges = Map.of();
            return;
        }
        Optional<GraphData> graph = lib.graph(graphName);
        if (graph.isEmpty()) {
            entries = Map.of();
            truncated = false;
            badges = Map.of();
            return;
        }
        MolangGenerator generator = new MolangGenerator(lib);
        Map<String, Entry> next = new LinkedHashMap<>();
        boolean overflow = false;
        for (NodeInstance node : graph.get().nodes()) {
            if (next.size() >= MAX_BADGES) {
                overflow = true;
                break;
            }
            Optional<NodeType> type = NodeTypes.get(node.type());
            if (type.isEmpty() || isSubgraphKind(type.get().kind()) || isAssemblyOnlyRef(type.get().kind())) {
                continue;
            }
            Optional<PortDef> out = type.get().outputsOf(node, lib.subgraphResolver()).stream()
                    .filter(p -> p.type().isValue())
                    .findFirst();
            if (out.isEmpty()) {
                continue;
            }
            CodegenResult result = generator.emitNodeOutput(graphName, node.uid(), out.get().id());
            MolangType displayType = toMolangType(out.get().type());
            next.put(node.uid(), new Entry(result.code(), displayType, result.hasErrors()));
        }
        entries = next;
        truncated = overflow;
        tick();
    }

    /** 仅装配上下文引用（无 molang 表达式语义，codegen 对其报错）——不做徽标。 */
    private static boolean isAssemblyOnlyRef(NodeType.Kind kind) {
        return kind == NodeType.Kind.REF_ANIMATION
                || kind == NodeType.Kind.REF_AC
                || kind == NodeType.Kind.REF_RC;
    }

    private static boolean isSubgraphKind(NodeType.Kind kind) {
        return kind == NodeType.Kind.SUBGRAPH_CALL
                || kind == NodeType.Kind.SUBGRAPH_INPUT
                || kind == NodeType.Kind.SUBGRAPH_OUTPUT;
    }

    private static MolangType toMolangType(io.github.tt432.eyelib.nodegraph.PortType type) {
        return switch (type) {
            case FLOAT -> MolangType.FLOAT;
            case INT -> MolangType.INT;
            case BOOL -> MolangType.BOOL;
            case STRING -> MolangType.STRING;
            case ARRAY -> MolangType.ARRAY;
            default -> MolangType.DYNAMIC;
        };
    }

    private static String shorten(@Nullable String error) {
        if (error == null) {
            return "错误";
        }
        int nl = error.indexOf('\n');
        String first = nl >= 0 ? error.substring(0, nl) : error;
        return first.length() > 48 ? first.substring(0, 48) + "…" : first;
    }
}
