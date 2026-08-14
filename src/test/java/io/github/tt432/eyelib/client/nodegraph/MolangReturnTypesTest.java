package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.nodegraph.PortType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MolangReturnTypes} 返回类型推导：反射映射、变体冲突回落、字段形态、未知回落。
 *
 * @author TT432
 */
class MolangReturnTypesTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    private static void setup(Class<?>... classes) {
        List<MolangMappingDiscovery.MolangMappingClassEntry> entries = java.util.Arrays.stream(classes)
                .<MolangMappingDiscovery.MolangMappingClassEntry>map(c ->
                        new MolangMappingDiscovery.MolangMappingClassEntry("query", c, false))
                .toList();
        MolangMappingTree.setupMolangMappingTree(() -> entries);
    }

    @Test
    void primitiveReturnTypesMapToPortTypes() {
        setup(TypedMapping.class);
        assertEquals(PortType.FLOAT, MolangReturnTypes.returnTypeOf("query.f_float"));
        assertEquals(PortType.INT, MolangReturnTypes.returnTypeOf("query.f_int"));
        assertEquals(PortType.BOOL, MolangReturnTypes.returnTypeOf("query.f_bool"));
        assertEquals(PortType.STRING, MolangReturnTypes.returnTypeOf("query.f_string"));
        assertEquals(PortType.ARRAY, MolangReturnTypes.returnTypeOf("query.f_list"));
        assertEquals(PortType.OBJECT, MolangReturnTypes.returnTypeOf("query.f_map"));
        assertEquals(PortType.ANY, MolangReturnTypes.returnTypeOf("query.f_void"));
    }

    @Test
    void staticFieldReturnTypeResolves() {
        setup(TypedMapping.class);
        assertEquals(PortType.STRING, MolangReturnTypes.returnTypeOf("query.LABEL"));
    }

    @Test
    void conflictingVariantReturnTypesFallBackToAny() {
        setup(ConflictLeft.class, ConflictRight.class);
        assertEquals(PortType.ANY, MolangReturnTypes.returnTypeOf("query.conflict"));
    }

    @Test
    void unknownFunctionIsAny() {
        setup(TypedMapping.class);
        assertEquals(PortType.ANY, MolangReturnTypes.returnTypeOf("query.nonexistent"));
        assertEquals(PortType.ANY, MolangReturnTypes.returnTypeOf(""));
        assertEquals(PortType.ANY, MolangReturnTypes.returnTypeOf("my_custom_fn"));
    }

    @MolangMapping(value = "query", pureFunction = false)
    public static final class TypedMapping {
        public static final String LABEL = "label";

        @MolangFunction("f_float")
        public static float fFloat(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return 0;
        }

        @MolangFunction("f_int")
        public static int fInt(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return 0;
        }

        @MolangFunction("f_bool")
        public static boolean fBool(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return false;
        }

        @MolangFunction("f_string")
        public static String fString(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return "";
        }

        @MolangFunction("f_list")
        public static List<Float> fList(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return List.of();
        }

        @MolangFunction("f_map")
        public static Map<String, Object> fMap(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return Map.of();
        }

        @MolangFunction("f_void")
        public static void fVoid(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
        }
    }

    @MolangMapping(value = "query", pureFunction = false)
    public static final class ConflictLeft {
        @MolangFunction("conflict")
        public static float conflict(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return 0;
        }
    }

    @MolangMapping(value = "query", pureFunction = false)
    public static final class ConflictRight {
        @MolangFunction("conflict")
        public static String conflict(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope,
                                      float extra) {
            return "";
        }
    }
}
