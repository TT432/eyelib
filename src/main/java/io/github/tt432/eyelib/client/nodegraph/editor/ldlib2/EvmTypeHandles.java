package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import io.github.tt432.eyelib.nodegraph.PortType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 端口类型 ↔ LDLib2 {@link TypeHandle} 双向映射（规格 §2.1 / 适配层 §2）。
 *
 * <p>映射规则：FLOAT/BOOL/STRING 用内置 handle；EXEC → {@link TypeHandles#EXECUTION_FLOW}；
 * ANY/ARRAY → {@link TypeHandles#OBJECT}；VARIABLE、SLOT 与 6 种 *_REF 为自定义 handle
 * （{@link TypeHandleHelpers#customType}，identification 形如 {@code eyelib:slot}）。
 *
 * <p>自定义 handle 经 {@link Holder} 懒加载注册恰好一次（customType 重复注册会报错日志）。
 * REF 类型解析为 {@link String}.class，使编辑器连线检查放行 ref→string（domain 允许的隐式规则），
 * 精度约束由保存时的 domain 验证器兜底；SLOT/VARIABLE 解析为独立 marker 类，仅同 handle 可连。
 *
 * <p>VARIABLE（variable 节点 out / exec.set_var target）的「可接一切值端口」不映射为 ANY 兜底：
 * LDLib2 提供 {@code GraphModel#canAssignTo} 覆盖点（{@link EvmGraph.EvmGraphModel} 已实现
 * domain 兼容矩阵，VARIABLE→任意值端口放行、其它类型→VARIABLE 拒绝），独立 handle 让
 * 连线规则与 domain 严格对齐，编辑器不会放出 domain 不允许的连接。
 */
public final class EvmTypeHandles {
    private EvmTypeHandles() {
    }

    /** SLOT 结构端口的 marker 类型（仅占位，使 slot 不与任何值类型互通）。 */
    public static final class SlotValue {
        private SlotValue() {
        }
    }

    /** VARIABLE 变量身份端口的 marker 类型（仅占位；身份不承载值，兼容规则见类注释）。 */
    public static final class VariableValue {
        private VariableValue() {
        }
    }

    /** COLOR 复合颜色端口的 marker 类型（仅占位；颜色不承载标量值，仅同 handle 可连）。 */
    public static final class ColorValue {
        private ColorValue() {
        }
    }

    /** 自定义 handle 注册（类加载恰好一次；颜色随字段初始化一并设置，不用 static 块）。 */
    private static final class Holder {
        static final TypeHandle SLOT = colored(SlotValue.class, "eyelib:slot", "Slot", 0xFF9C27B0);
        static final TypeHandle VARIABLE = colored(VariableValue.class, "eyelib:variable", "Variable", 0xFFE91E63);
        static final TypeHandle GEOMETRY_REF = colored(String.class, "eyelib:geometry_ref", "Geometry Ref", 0xFF4CAF50);
        static final TypeHandle TEXTURE_REF = colored(String.class, "eyelib:texture_ref", "Texture Ref", 0xFF8BC34A);
        static final TypeHandle MATERIAL_REF = colored(String.class, "eyelib:material_ref", "Material Ref", 0xFF009688);
        static final TypeHandle ANIMATION_REF = colored(String.class, "eyelib:animation_ref", "Animation Ref", 0xFFFF9800);
        static final TypeHandle AC_REF = colored(String.class, "eyelib:ac_ref", "AC Ref", 0xFFFF5722);
        static final TypeHandle RC_REF = colored(String.class, "eyelib:rc_ref", "RC Ref", 0xFF3F51B5);
        static final TypeHandle PARTICLE_REF = colored(String.class, "eyelib:particle_ref", "Particle Ref", 0xFF00BCD4);
        static final TypeHandle SOUND_REF = colored(String.class, "eyelib:sound_ref", "Sound Ref", 0xFF795548);
        static final TypeHandle COLOR = colored(ColorValue.class, "eyelib:color", "Color", 0xFFD81B60);

        static final List<TypeHandle> ALL = List.of(
                TypeHandles.EXECUTION_FLOW,
                TypeHandles.FLOAT,
                TypeHandles.INT,
                TypeHandles.BOOL,
                TypeHandles.STRING,
                TypeHandles.OBJECT,
                SLOT,
                VARIABLE,
                GEOMETRY_REF,
                TEXTURE_REF,
                MATERIAL_REF,
                ANIMATION_REF,
                AC_REF,
                RC_REF,
                PARTICLE_REF,
                SOUND_REF,
                COLOR);
        /**
         * 黑板变量声明可选类型：变量存的是 molang 值，只收值类型（FLOAT/INT/BOOL/STRING/
         * OBJECT=ANY）。EXECUTION_FLOW/SLOT/VARIABLE 是结构/身份标注，*_REF 是引用语义
         * 子类型，COLOR 是复合值（无 molang 标量），均不可声明。
         */
        static final List<TypeHandle> VARIABLE_DECL_TYPES = List.of(
                TypeHandles.FLOAT,
                TypeHandles.INT,
                TypeHandles.BOOL,
                TypeHandles.STRING,
                TypeHandles.OBJECT);

        private static TypeHandle colored(Class<?> type, String id, String friendlyName, int color) {
            TypeHandle handle = TypeHandleHelpers.customType(type, id, friendlyName);
            TypeHandleHelpers.setCustomColor(handle, color);
            return handle;
        }
    }

    /** getSupportTypes 清单：内置值类型 + EXECUTION_FLOW + 全部自定义 handle。 */
    public static List<TypeHandle> allSupported() {
        return Holder.ALL;
    }

    /** 黑板变量声明可选类型（仅 molang 值类型：FLOAT/INT/BOOL/STRING/OBJECT=ANY）。 */
    public static List<TypeHandle> allVariableDeclTypes() {
        return Holder.VARIABLE_DECL_TYPES;
    }

    /** domain 端口类型 → LDLib2 TypeHandle。 */
    public static TypeHandle toHandle(PortType type) {
        return switch (type) {
            case EXEC -> TypeHandles.EXECUTION_FLOW;
            case FLOAT -> TypeHandles.FLOAT;
            case INT -> TypeHandles.INT;
            case BOOL -> TypeHandles.BOOL;
            case STRING -> TypeHandles.STRING;
            case ARRAY, ANY -> TypeHandles.OBJECT;
            case SLOT -> Holder.SLOT;
            case VARIABLE -> Holder.VARIABLE;
            case GEOMETRY_REF -> Holder.GEOMETRY_REF;
            case TEXTURE_REF -> Holder.TEXTURE_REF;
            case MATERIAL_REF -> Holder.MATERIAL_REF;
            case ANIMATION_REF -> Holder.ANIMATION_REF;
            case AC_REF -> Holder.AC_REF;
            case RC_REF -> Holder.RC_REF;
            case PARTICLE_REF -> Holder.PARTICLE_REF;
            case SOUND_REF -> Holder.SOUND_REF;
            case COLOR -> Holder.COLOR;
            case UNKNOWN -> TypeHandles.UNKNOWN;
        };
    }

    /** LDLib2 TypeHandle → domain 端口类型；不认识的 handle 返回 null。 */
    public static @Nullable PortType toPortType(@Nullable TypeHandle handle) {
        if (handle == null) return null;
        if (handle.equals(TypeHandles.EXECUTION_FLOW)) return PortType.EXEC;
        if (handle.equals(TypeHandles.FLOAT)) return PortType.FLOAT;
        if (handle.equals(TypeHandles.INT)) return PortType.INT;
        if (handle.equals(TypeHandles.BOOL)) return PortType.BOOL;
        if (handle.equals(TypeHandles.STRING)) return PortType.STRING;
        if (handle.equals(TypeHandles.OBJECT)) return PortType.ANY;
        if (handle.equals(TypeHandles.UNKNOWN)) return PortType.UNKNOWN;
        if (handle.equals(Holder.SLOT)) return PortType.SLOT;
        if (handle.equals(Holder.VARIABLE)) return PortType.VARIABLE;
        if (handle.equals(Holder.GEOMETRY_REF)) return PortType.GEOMETRY_REF;
        if (handle.equals(Holder.TEXTURE_REF)) return PortType.TEXTURE_REF;
        if (handle.equals(Holder.MATERIAL_REF)) return PortType.MATERIAL_REF;
        if (handle.equals(Holder.ANIMATION_REF)) return PortType.ANIMATION_REF;
        if (handle.equals(Holder.AC_REF)) return PortType.AC_REF;
        if (handle.equals(Holder.RC_REF)) return PortType.RC_REF;
        if (handle.equals(Holder.PARTICLE_REF)) return PortType.PARTICLE_REF;
        if (handle.equals(Holder.SOUND_REF)) return PortType.SOUND_REF;
        if (handle.equals(Holder.COLOR)) return PortType.COLOR;
        return null;
    }
}
//?}
