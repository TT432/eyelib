package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.type.MolangArray;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangString;
import io.github.tt432.eyelib.molang.type.MolangStruct;
import io.github.tt432.eyelib.nodegraph.PortType;

import java.util.List;
import java.util.Map;

/**
 * query/math 函数返回类型推导（编辑器节点端口显示类型来源，v13）。
 *
 * <p>来源优先级：手工覆盖表 → 映射树反射。反射取 {@code findMethod} 全部注册变体的
 * Java 返回类型，逐变体映射为 {@link PortType}——全变体同型才采纳，冲突/未知回落 ANY；
 * 无方法条目时试 {@code findField}（静态字段形态的零参 query）。.emolang 自定义函数
 * 无返回类型标注，恒 ANY。
 *
 * <p>bool 语义 query 以 float 实现（has_main_hand 等）——FLOAT/BOOL 在兼容矩阵互通，
 * 不做逐条 bool  curated；需要精确语义时在 {@link #OVERRIDES} 覆盖。
 */
public final class MolangReturnTypes {
    private MolangReturnTypes() {
    }

    /** 手工覆盖表：反射推不出/推错时在此钉死（键 = 全名，如 "query.get_name"）。 */
    private static final Map<String, PortType> OVERRIDES = Map.of();

    /** 函数全名 → 返回端口类型；未知/变体冲突/无参数字面 → ANY。 */
    public static PortType returnTypeOf(String functionName) {
        if (functionName == null || functionName.isEmpty()) {
            return PortType.ANY;
        }
        PortType override = OVERRIDES.get(functionName);
        if (override != null) {
            return override;
        }
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        MolangMappingTree.MethodData method = tree.findMethod(functionName);
        if (method != null && !method.functionInfos().isEmpty()) {
            PortType merged = null;
            for (MolangMappingTree.FunctionInfo info : method.functionInfos()) {
                PortType t = javaTypeToPortType(info.method().getReturnType());
                if (merged == null) {
                    merged = t;
                } else if (merged != t) {
                    return PortType.ANY;
                }
            }
            return merged != null ? merged : PortType.ANY;
        }
        MolangMappingTree.FieldData field = tree.findField(functionName);
        if (field != null) {
            return javaTypeToPortType(field.field().getType());
        }
        return PortType.ANY;
    }

    /** Java 返回/字段类型 → domain 端口类型。 */
    static PortType javaTypeToPortType(Class<?> type) {
        if (type == float.class || type == double.class
                || type == Float.class || type == Double.class || type == MolangFloat.class) {
            return PortType.FLOAT;
        }
        if (type == int.class || type == long.class || type == short.class || type == byte.class
                || type == Integer.class || type == Long.class || type == Short.class || type == Byte.class) {
            return PortType.INT;
        }
        if (type == boolean.class || type == Boolean.class) {
            return PortType.BOOL;
        }
        if (type == String.class || type == CharSequence.class || type == MolangString.class) {
            return PortType.STRING;
        }
        if (type.isArray() || List.class.isAssignableFrom(type) || MolangArray.class.isAssignableFrom(type)) {
            return PortType.ARRAY;
        }
        if (Map.class.isAssignableFrom(type) || type == MolangStruct.class) {
            return PortType.OBJECT;
        }
        return PortType.ANY;
    }
}
