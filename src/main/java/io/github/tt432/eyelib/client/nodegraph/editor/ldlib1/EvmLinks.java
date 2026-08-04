//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.TriggerLink;
import com.lowdragmc.lowdraglib.utils.TypeAdapter;
import io.github.tt432.eyelib.nodegraph.PortType;

/**
 * EVM 端口类型（{@link PortType}）→ LDLib 1.x graphprocessor 端口 Java 类型映射。
 *
 * <p>graphprocessor 以 {@code Class} 判型（{@code BaseGraph.areTypesConnectable}：
 * {@code to.isAssignableFrom(from)} + {@code to == Object} + {@code from == Object}
 * + {@link TypeAdapter} 注册转换器），因此：
 * <ul>
 *   <li>FLOAT/INT/BOOL → {@link Float}/{@link Integer}/{@link Boolean}，并注册三向互通转换器
 *       （域规则 number 子类型隐式互通，规格 §2.1）；</li>
 *   <li>STRING → {@link String}；ANY → {@link Object}（与任何类型互连，与域规则一致）；</li>
 *   <li>EXEC → {@link TriggerLink}：graphprocessor 内建特判「TriggerLink 仅连 TriggerLink」，
 *       恰好等于 exec↔exec；TriggerLink 本身是无语义空标记类（执行语义在
 *       TriggerProcessor/ITriggerableNode，本编辑器禁用 processor，不触及）；</li>
 *   <li>SLOT/ARRAY/各 *_REF → 专属空标记类：同类才连，与域规则一致；</li>
 *   <li>每个 *_REF 标记类注册到 String 的单向转换器（域规则：ref 产出可接 STRING 输入）。</li>
 * </ul>
 *
 * <p>转换器函数永远不会被执行（编辑器不运行图，{@code GraphViewWidget.setProcessor(null)}），
 * 注册它们仅为让 {@code areTypesConnectable} 放行域内合法的连接。
 */
public final class EvmLinks {
    private EvmLinks() {
    }

    /** SLOT（装配槽）标记类。 */
    public static final class SlotLink {
        private SlotLink() {
        }
    }

    /** ARRAY（molang 数组）标记类。 */
    public static final class ArrayLink {
        private ArrayLink() {
        }
    }

    /** GEOMETRY_REF 标记类。 */
    public static final class GeometryRefLink {
        private GeometryRefLink() {
        }
    }

    /** TEXTURE_REF 标记类。 */
    public static final class TextureRefLink {
        private TextureRefLink() {
        }
    }

    /** MATERIAL_REF 标记类。 */
    public static final class MaterialRefLink {
        private MaterialRefLink() {
        }
    }

    /** ANIMATION_REF 标记类。 */
    public static final class AnimationRefLink {
        private AnimationRefLink() {
        }
    }

    /** AC_REF 标记类。 */
    public static final class AcRefLink {
        private AcRefLink() {
        }
    }

    /** RC_REF 标记类。 */
    public static final class RcRefLink {
        private RcRefLink() {
        }
    }

    /** VARIABLE 标记类。 */
    public static final class VariableLink {
        private VariableLink() {
        }
    }

    /** 域端口类型 → graphprocessor 判型用 Java 类型。 */
    public static Class<?> toClass(PortType type) {
        return switch (type) {
            case EXEC -> TriggerLink.class;
            case FLOAT -> Float.class;
            case INT -> Integer.class;
            case BOOL -> Boolean.class;
            case STRING -> String.class;
            case ANY -> Object.class;
            case ARRAY -> ArrayLink.class;
            case SLOT -> SlotLink.class;
            case GEOMETRY_REF -> GeometryRefLink.class;
            case TEXTURE_REF -> TextureRefLink.class;
            case MATERIAL_REF -> MaterialRefLink.class;
            case ANIMATION_REF -> AnimationRefLink.class;
            case AC_REF -> AcRefLink.class;
            case RC_REF -> RcRefLink.class;
            case VARIABLE -> VariableLink.class;
        };
    }

    /** graphprocessor 端口 Java 类型 → 域端口类型（黑板变量类型反推用；未知 → ANY）。 */
    public static PortType toPortType(Class<?> clazz) {
        if (clazz == TriggerLink.class) return PortType.EXEC;
        if (clazz == Float.class || clazz == float.class) return PortType.FLOAT;
        if (clazz == Integer.class || clazz == int.class) return PortType.INT;
        if (clazz == Boolean.class || clazz == boolean.class) return PortType.BOOL;
        if (clazz == String.class) return PortType.STRING;
        if (clazz == ArrayLink.class) return PortType.ARRAY;
        if (clazz == SlotLink.class) return PortType.SLOT;
        if (clazz == GeometryRefLink.class) return PortType.GEOMETRY_REF;
        if (clazz == TextureRefLink.class) return PortType.TEXTURE_REF;
        if (clazz == MaterialRefLink.class) return PortType.MATERIAL_REF;
        if (clazz == AnimationRefLink.class) return PortType.ANIMATION_REF;
        if (clazz == AcRefLink.class) return PortType.AC_REF;
        if (clazz == RcRefLink.class) return PortType.RC_REF;
        if (clazz == VariableLink.class) return PortType.VARIABLE;
        return PortType.ANY;
    }

    private static boolean adaptersRegistered = false;

    /** 注册标记类的显示名/颜色与隐式转换器（幂等）。 */
    public static synchronized void ensureTypeAdapters() {
        if (adaptersRegistered) return;
        adaptersRegistered = true;

        // number 三子类型互通（规格 §2.1；函数永不执行，仅供 areTypesConnectable 判型）
        TypeAdapter.registerAdapter(Boolean.class, Float.class, b -> b ? 1f : 0f);
        TypeAdapter.registerAdapter(Float.class, Boolean.class, f -> f != 0f);
        TypeAdapter.registerAdapter(Integer.class, Float.class, Integer::floatValue);
        TypeAdapter.registerAdapter(Float.class, Integer.class, Float::intValue);
        TypeAdapter.registerAdapter(Integer.class, Boolean.class, i -> i != 0);
        TypeAdapter.registerAdapter(Boolean.class, Integer.class, b -> b ? 1 : 0);

        // 用户面向类型名：number:{float/int/bool}/string（覆盖 graphprocessor 内建名）
        TypeAdapter.registerTypeDisplayName(Float.class, "number<float>");
        TypeAdapter.registerTypeDisplayName(Integer.class, "number<int>");
        TypeAdapter.registerTypeDisplayName(Boolean.class, "number<bool>");
        TypeAdapter.registerTypeDisplayName(String.class, "string");

        // 资源引用产出 → STRING 输入（单向；string → ref 不注册，与域规则一致）
        TypeAdapter.registerAdapter(GeometryRefLink.class, String.class, Object::toString);
        TypeAdapter.registerAdapter(TextureRefLink.class, String.class, Object::toString);
        TypeAdapter.registerAdapter(MaterialRefLink.class, String.class, Object::toString);
        TypeAdapter.registerAdapter(AnimationRefLink.class, String.class, Object::toString);
        TypeAdapter.registerAdapter(AcRefLink.class, String.class, Object::toString);
        TypeAdapter.registerAdapter(RcRefLink.class, String.class, Object::toString);

        TypeAdapter.registerTypeDisplayName(SlotLink.class, "slot");
        TypeAdapter.registerTypeDisplayName(ArrayLink.class, "array");
        TypeAdapter.registerTypeDisplayName(GeometryRefLink.class, "geometry_ref");
        TypeAdapter.registerTypeDisplayName(TextureRefLink.class, "texture_ref");
        TypeAdapter.registerTypeDisplayName(MaterialRefLink.class, "material_ref");
        TypeAdapter.registerTypeDisplayName(AnimationRefLink.class, "animation_ref");
        TypeAdapter.registerTypeDisplayName(AcRefLink.class, "ac_ref");
        TypeAdapter.registerTypeDisplayName(RcRefLink.class, "rc_ref");

        TypeAdapter.registerTypeColor(SlotLink.class, ColorPattern.ORANGE.color);
        TypeAdapter.registerTypeColor(ArrayLink.class, ColorPattern.LIME.color);
        TypeAdapter.registerTypeColor(GeometryRefLink.class, ColorPattern.CYAN.color);
        TypeAdapter.registerTypeColor(TextureRefLink.class, ColorPattern.GREEN.color);
        TypeAdapter.registerTypeColor(MaterialRefLink.class, ColorPattern.PURPLE.color);
        TypeAdapter.registerTypeColor(AnimationRefLink.class, ColorPattern.LIGHT_BLUE.color);
        TypeAdapter.registerTypeColor(AcRefLink.class, ColorPattern.MAGENTA.color);
        TypeAdapter.registerTypeColor(RcRefLink.class, ColorPattern.BROWN.color);

        // variable 身份 → 任意值端口 = 隐式读（单向；不注册反向，set_var.target 只接受
        // variable 节点，拖错由验证器 SET_TARGET_NOT_VARIABLE 报错）
        TypeAdapter.registerAdapter(VariableLink.class, Float.class, v -> 0f);
        TypeAdapter.registerAdapter(VariableLink.class, Integer.class, v -> 0);
        TypeAdapter.registerAdapter(VariableLink.class, Boolean.class, v -> false);
        TypeAdapter.registerAdapter(VariableLink.class, String.class, Object::toString);
        TypeAdapter.registerAdapter(VariableLink.class, ArrayLink.class, v -> null);
        TypeAdapter.registerAdapter(VariableLink.class, Object.class, v -> v);
        TypeAdapter.registerTypeDisplayName(VariableLink.class, "variable");
        TypeAdapter.registerTypeColor(VariableLink.class, ColorPattern.YELLOW.color);
    }
}
//?}
