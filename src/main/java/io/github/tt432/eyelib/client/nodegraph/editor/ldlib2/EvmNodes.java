package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 每种 domain 节点类型（{@link NodeTypes} 目录）对应的 LDLib2 Node 子类。
 * 用嵌套静态类集中声明，避免 37 个单类文件。
 *
 * <p>发现机制说明：LDLib2 的自动发现（{@code GraphNodeRegistry.create} + FML ModFileScanData
 * 全 mod 扫描，{@code ReflectionUtils.findAnnotationClasses}，无包限制）本可覆盖本包类；
 * 但该机制要求构造期访问 {@code ModList}，在零 MC 单测中不可用。因此 {@link EvmGraph#getSupportNodes()}
 * 直接返回 {@link #ALL} 显式清单，不创建 GraphNodeRegistry；@NodeAttribute 仅作
 * 物品库分组元数据（group 按 '/' 建树）使用。
 */
public final class EvmNodes {
    private EvmNodes() {
    }

    @NodeAttribute(name = "const.number", group = NodeTypes.CAT_CONSTANT, graphTypes = {EvmGraph.class})
    public static final class ConstNumber extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.CONST_NUMBER;
        }
    }

    @NodeAttribute(name = "const.int", group = NodeTypes.CAT_CONSTANT, graphTypes = {EvmGraph.class})
    public static final class ConstInt extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.CONST_INT;
        }
    }

    @NodeAttribute(name = "const.bool", group = NodeTypes.CAT_CONSTANT, graphTypes = {EvmGraph.class})
    public static final class ConstBool extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.CONST_BOOL;
        }
    }

    @NodeAttribute(name = "const.string", group = NodeTypes.CAT_CONSTANT, graphTypes = {EvmGraph.class})
    public static final class ConstString extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.CONST_STRING;
        }
    }

    @NodeAttribute(name = "const.color", group = NodeTypes.CAT_CONSTANT, graphTypes = {EvmGraph.class})
    public static final class ConstColor extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.CONST_COLOR;
        }
    }

    @NodeAttribute(name = "color.compose", group = NodeTypes.CAT_OPERATOR, graphTypes = {EvmGraph.class})
    public static final class ColorCompose extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.COLOR_COMPOSE;
        }
    }

    @NodeAttribute(name = "variable", group = NodeTypes.CAT_VARIABLE, graphTypes = {EvmGraph.class})
    public static final class Variable extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.VARIABLE;
        }
    }

    @NodeAttribute(name = "context.get", group = NodeTypes.CAT_VARIABLE, graphTypes = {EvmGraph.class})
    public static final class ContextGet extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.CONTEXT_GET;
        }
    }

    @NodeAttribute(name = "temp.get", group = NodeTypes.CAT_VARIABLE, graphTypes = {EvmGraph.class})
    public static final class TempGet extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.TEMP_GET;
        }
    }

    @NodeAttribute(name = "exec.set_var", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecSetVar extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_SET_VAR;
        }
    }

    @NodeAttribute(name = "exec.set_temp", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecSetTemp extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_SET_TEMP;
        }
    }

    @NodeAttribute(name = "query.call", group = NodeTypes.CAT_QUERY, graphTypes = {EvmGraph.class})
    public static final class QueryCall extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.QUERY_CALL;
        }
    }

    @NodeAttribute(name = "math.call", group = NodeTypes.CAT_QUERY, graphTypes = {EvmGraph.class})
    public static final class MathCall extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.MATH_CALL;
        }
    }

    @NodeAttribute(name = "exec.call", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecCall extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_CALL;
        }
    }

    @NodeAttribute(name = "op.binary", group = NodeTypes.CAT_OPERATOR, graphTypes = {EvmGraph.class})
    public static final class OpBinary extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.OP_BINARY;
        }
    }

    @NodeAttribute(name = "op.unary", group = NodeTypes.CAT_OPERATOR, graphTypes = {EvmGraph.class})
    public static final class OpUnary extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.OP_UNARY;
        }
    }

    @NodeAttribute(name = "op.ternary", group = NodeTypes.CAT_OPERATOR, graphTypes = {EvmGraph.class})
    public static final class OpTernary extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.OP_TERNARY;
        }
    }

    @NodeAttribute(name = "op.null_coalesce", group = NodeTypes.CAT_OPERATOR, graphTypes = {EvmGraph.class})
    public static final class OpNullCoalesce extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.OP_NULLCOALESCE;
        }
    }

    @NodeAttribute(name = "exec.loop", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecLoop extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_LOOP;
        }
    }

    @NodeAttribute(name = "exec.for_each", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecForEach extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_FOREACH;
        }
    }

    @NodeAttribute(name = "exec.break", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecBreak extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_BREAK;
        }
    }

    @NodeAttribute(name = "exec.continue", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecContinue extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_CONTINUE;
        }
    }

    @NodeAttribute(name = "exec.return", group = NodeTypes.CAT_EXEC, graphTypes = {EvmGraph.class})
    public static final class ExecReturn extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.EXEC_RETURN;
        }
    }

    @NodeAttribute(name = "ref.geometry", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefGeometry extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_GEOMETRY;
        }
    }

    @NodeAttribute(name = "ref.texture", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefTexture extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_TEXTURE;
        }
    }

    @NodeAttribute(name = "ref.material", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefMaterial extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_MATERIAL;
        }
    }

    @NodeAttribute(name = "ref.animation", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefAnimation extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_ANIMATION;
        }
    }

    @NodeAttribute(name = "ref.ac", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefAc extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_AC;
        }
    }

    @NodeAttribute(name = "ref.rc", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefRc extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_RC;
        }
    }

    @NodeAttribute(name = "entity.root", group = NodeTypes.CAT_ENTITY, graphTypes = {EvmGraph.class})
    public static final class EntityRoot extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.ENTITY_ROOT;
        }
    }

    @NodeAttribute(name = "animate.entry", group = NodeTypes.CAT_ENTITY, graphTypes = {EvmGraph.class})
    public static final class AnimateEntry extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.ANIMATE_ENTRY;
        }
    }

    @NodeAttribute(name = "rc.root", group = NodeTypes.CAT_RC, graphTypes = {EvmGraph.class})
    public static final class RcRoot extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.RC_ROOT;
        }
    }

    @NodeAttribute(name = "list.entry", group = NodeTypes.CAT_RC, graphTypes = {EvmGraph.class})
    public static final class ListEntry extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.LIST_ENTRY;
        }
    }

    @NodeAttribute(name = "material.entry", group = NodeTypes.CAT_RC, graphTypes = {EvmGraph.class})
    public static final class MaterialEntry extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.MATERIAL_ENTRY;
        }
    }

    @NodeAttribute(name = "part_visibility.entry", group = NodeTypes.CAT_RC, graphTypes = {EvmGraph.class})
    public static final class PartVisibilityEntry extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.PART_VISIBILITY_ENTRY;
        }
    }

    @NodeAttribute(name = "ac.root", group = NodeTypes.CAT_AC, graphTypes = {EvmGraph.class})
    public static final class AcRoot extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.AC_ROOT;
        }
    }

    @NodeAttribute(name = "ac.state", group = NodeTypes.CAT_AC, graphTypes = {EvmGraph.class})
    public static final class AcState extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.AC_STATE;
        }
    }

    @NodeAttribute(name = "ac.transition", group = NodeTypes.CAT_AC, graphTypes = {EvmGraph.class})
    public static final class AcTransition extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.AC_TRANSITION;
        }
    }

    @NodeAttribute(name = "particle.entry", group = NodeTypes.CAT_AC, graphTypes = {EvmGraph.class})
    public static final class ParticleEntry extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.PARTICLE_ENTRY;
        }
    }

    @NodeAttribute(name = "ref.particle", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefParticle extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_PARTICLE;
        }
    }

    @NodeAttribute(name = "ref.sound", group = NodeTypes.CAT_REF, graphTypes = {EvmGraph.class})
    public static final class RefSound extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.REF_SOUND;
        }
    }

    @NodeAttribute(name = "subgraph.call", group = NodeTypes.CAT_SUBGRAPH, graphTypes = {EvmGraph.class})
    public static final class SubgraphCall extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.SUBGRAPH_CALL;
        }
    }

    @NodeAttribute(name = "subgraph.input", group = NodeTypes.CAT_SUBGRAPH, graphTypes = {EvmGraph.class})
    public static final class SubgraphInput extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.SUBGRAPH_INPUT;
        }
    }

    @NodeAttribute(name = "subgraph.output", group = NodeTypes.CAT_SUBGRAPH, graphTypes = {EvmGraph.class})
    public static final class SubgraphOutput extends EvmNodeBase {
        @Override
        public NodeType type() {
            return NodeTypes.SUBGRAPH_OUTPUT;
        }
    }

    /** getSupportNodes 清单（创建顺序 = 目录顺序）。 */
    public static final List<Class<? extends Node>> ALL = List.of(
            ConstNumber.class, ConstInt.class, ConstBool.class, ConstString.class,
            ConstColor.class, ColorCompose.class,
            Variable.class, ContextGet.class, TempGet.class, ExecSetVar.class, ExecSetTemp.class,
            QueryCall.class, MathCall.class, ExecCall.class,
            OpBinary.class, OpUnary.class, OpTernary.class, OpNullCoalesce.class,
            ExecLoop.class, ExecForEach.class, ExecBreak.class, ExecContinue.class, ExecReturn.class,
            RefGeometry.class, RefTexture.class, RefMaterial.class, RefAnimation.class, RefAc.class, RefRc.class,
            EntityRoot.class, AnimateEntry.class,
            RcRoot.class, ListEntry.class, MaterialEntry.class, PartVisibilityEntry.class,
            AcRoot.class, AcState.class, AcTransition.class, ParticleEntry.class,
            RefParticle.class, RefSound.class,
            SubgraphCall.class, SubgraphInput.class, SubgraphOutput.class);

    private static final Map<String, Class<? extends EvmNodeBase>> BY_TYPE_ID = new LinkedHashMap<>();
    /** 显示名缓存：双重检查锁懒初始化（buildDisplayNames 需读取全部节点类，启动期不付成本）。 */
    private static volatile @Nullable Map<String, String> displayNames;

    private static Map<String, String> displayNames() {
        Map<String, String> map = displayNames;
        if (map == null) {
            synchronized (EvmNodes.class) {
                map = displayNames;
                if (map == null) {
                    map = buildDisplayNames();
                    displayNames = map;
                }
            }
        }
        return map;
    }

    private static Map<String, String> buildDisplayNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("const.number", "Number");
        names.put("const.bool", "Bool");
        names.put("const.string", "String");
        names.put("const.color", "Color");
        names.put("color.compose", "Color Compose");
        names.put("variable", "Variable");
        names.put("temp.get", "Get Temp");
        names.put("exec.set_var", "Set Variable");
        names.put("exec.set_temp", "Set Temp");
        names.put("query.call", "Query Call");
        names.put("math.call", "Math Call");
        names.put("exec.call", "Exec Call");
        names.put("op.binary", "Binary Op");
        names.put("op.unary", "Unary Op");
        names.put("op.ternary", "Ternary");
        names.put("op.null_coalesce", "Null Coalesce");
        names.put("exec.loop", "Loop");
        names.put("exec.for_each", "For Each");
        names.put("exec.break", "Break");
        names.put("exec.continue", "Continue");
        names.put("exec.return", "Return");
        names.put("ref.geometry", "Geometry Ref");
        names.put("ref.texture", "Texture Ref");
        names.put("ref.material", "Material Ref");
        names.put("ref.animation", "Animation Ref");
        names.put("ref.ac", "AC Ref");
        names.put("ref.rc", "RC Ref");
        names.put("entity.root", "Entity Root");
        names.put("animate.entry", "Animate Entry");
        names.put("rc.root", "RC Root");
        names.put("list.entry", "List Entry");
        names.put("material.entry", "Material Entry");
        names.put("part_visibility.entry", "Part Visibility Entry");
        names.put("ac.root", "AC Root");
        names.put("ac.state", "AC State");
        names.put("ac.transition", "AC Transition");
        names.put("subgraph.call", "Subgraph Call");
        names.put("subgraph.input", "Subgraph Input");
        names.put("subgraph.output", "Subgraph Output");
        return names;
    }

    /** domain 节点类型 id → LDLib2 节点类；未知类型返回 null。 */
    @SuppressWarnings("unchecked")
    public static @Nullable Class<? extends EvmNodeBase> classOf(String typeId) {
        if (BY_TYPE_ID.isEmpty()) {
            for (Class<? extends Node> cls : ALL) {
                NodeAttribute attr = cls.getAnnotation(NodeAttribute.class);
                if (attr != null) {
                    BY_TYPE_ID.put(attr.name(), (Class<? extends EvmNodeBase>) cls);
                }
            }
        }
        return BY_TYPE_ID.get(typeId);
    }

    /** 节点显示名（静态映射表；未知 id 原样返回）。 */
    public static String displayName(String typeId) {
        return displayNames().getOrDefault(typeId, typeId);
    }
}
//?}
