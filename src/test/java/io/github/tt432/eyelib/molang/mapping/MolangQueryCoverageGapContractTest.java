package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntryDefinition;
import io.github.tt432.eyelib.animation.bedrock.RootLocatorOffsetProvider;
import io.github.tt432.eyelib.bridge.molang.MolangBuiltInQuery;
import io.github.tt432.eyelib.client.molang.MolangQuery;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.compiler.MolangRuntimeSupport;
import io.github.tt432.eyelib.molang.mapping.api.MolangFunction;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.VisibleArgumentKind;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangString;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 5 个 Bedrock 合法调用形覆盖缺口的离线契约：名字必须在映射树可解析、且按 BE 的参数形
 * 选中对应重载（{@code warnMissing} 只在选不中变体时触发，所以这里检查的是「选得到」）。
 *
 * <p>缺口来历：Actions & Stuff 实机日志报
 * {@code math.ease_in_out_back / query.any / query.get_equipped_item_name /
 * query.is_item_name_any / query.get_root_locator_offset} 全部 unresolvable。
 *
 * @author TT432
 */
class MolangQueryCoverageGapContractTest {
    private static final float EPSILON = 0.0001F;

    @BeforeEach
    void setUp() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                new MolangMappingDiscovery.MolangMappingClassEntry("math", MolangMath.class, true),
                new MolangMappingDiscovery.MolangMappingClassEntry("query", MolangBuiltInQuery.class, false),
                new MolangMappingDiscovery.MolangMappingClassEntry("query", MolangQuery.class, false)));
    }

    @AfterEach
    void tearDown() {
        BrAnimationEntryDefinition.installRootLocatorOffsetProvider((scope, locatorName) -> null);
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void bedrockEaseInOutNamesResolveAndKeepLegacyAlias() {
        MolangScope scope = new MolangScope();

        assertEquals(15F, call(scope, "math.ease_in_out_back", num(10), num(20), num(0.5F)).asFloat(), EPSILON);
        // 旧名保留为 alias：已按错误拼写写死的包不破
        assertEquals(15F, call(scope, "math.ease_inout_back", num(10), num(20), num(0.5F)).asFloat(), EPSILON);

        assertNotNull(MolangMappingRegistries.mappingTree().findMethod("math.ease_in_out_elastic"));
        assertNotNull(MolangMappingRegistries.mappingTree().findMethod("math.ease_inout_elastic"));
    }

    @Test
    void anyAcceptsStringCallShape() {
        MolangScope scope = new MolangScope();

        assertEquals(1F, call(scope, "query.any", str("lead"), str("dirt"), str("lead")).asFloat(), EPSILON);
        assertEquals(0F, call(scope, "query.any", str("lead"), str("dirt"), str("gold")).asFloat(), EPSILON);
        // 数字形保持不变
        assertEquals(1F, call(scope, "query.any", num(3), num(1), num(3)).asFloat(), EPSILON);

        assertEquals("anyString", variant("query.any",
                VisibleArgumentKind.STRING, VisibleArgumentKind.STRING, VisibleArgumentKind.STRING)
                .method().getName());
        assertEquals("any", variant("query.any", VisibleArgumentKind.NUMBER, VisibleArgumentKind.NUMBER)
                .method().getName());
        assertEquals(2, MolangMappingRegistries.mappingTree().findMethod("query.any")
                .functionInfos().size(), "数字形与字符串形必须共存（不被出版签名去重合并）");
    }

    @Test
    void equippedItemNameResolvesForEveryBedrockArity() {
        MolangScope scope = new MolangScope();

        // 无 host、空槽：每个 arity 都必须解析到函数并返回 ''（不是 MolangNull，也不是 minecraft:air）
        assertEquals("", call(scope, "query.get_equipped_item_name").asString());
        assertEquals("", call(scope, "query.get_equipped_item_name", num(0)).asString());
        assertEquals("", call(scope, "query.get_equipped_item_name", num(1)).asString());
        assertEquals("", call(scope, "query.get_equipped_item_name", str("off_hand")).asString());
        assertEquals("", call(scope, "query.get_equipped_item_name", num(0), num(1)).asString());
        // BE 同样合法的混合形：槽名 + rendered 标志
        assertEquals("", call(scope, "query.get_equipped_item_name", str("main_hand"), num(1)).asString());

        // 各变体按参数类型/元数选中（字符串手槽与混合形都改变长形参避让出版签名去重）
        assertEquals(5, MolangMappingRegistries.mappingTree().findMethod("query.get_equipped_item_name")
                .functionInfos().size(), "五个变体都不能被出版签名去重合并掉");
        assertEquals(float.class,
                variant("query.get_equipped_item_name", VisibleArgumentKind.NUMBER).method().getParameterTypes()[1]);
        assertEquals(String.class,
                variant("query.get_equipped_item_name", VisibleArgumentKind.STRING).method().getParameterTypes()[1]);
        assertEquals(3,
                variant("query.get_equipped_item_name", VisibleArgumentKind.NUMBER, VisibleArgumentKind.NUMBER)
                        .method().getParameterCount());
        assertEquals(String.class,
                variant("query.get_equipped_item_name", VisibleArgumentKind.STRING, VisibleArgumentKind.NUMBER)
                        .method().getParameterTypes()[1]);
    }

    @Test
    void indexedItemNameAnyRegistersUnderSnakeCaseName() {
        MolangScope scope = new MolangScope();

        // BE 合法形 ('slot.hotbar', 3, 'ns:item')：曾因带 index 的重载注册成驼峰 isItemNameAny 而选不中
        MolangObject result = call(scope, "query.is_item_name_any",
                str("slot.hotbar"), num(3), str("minecraft:bow"));
        assertFalse(result instanceof MolangNull, "indexed is_item_name_any 必须是可解析变体");
        assertEquals(0F, result.asFloat(), EPSILON);

        assertEquals(4, variant("query.is_item_name_any",
                VisibleArgumentKind.STRING, VisibleArgumentKind.NUMBER, VisibleArgumentKind.STRING)
                .method().getParameterCount());
        assertEquals(3, variant("query.is_item_name_any",
                VisibleArgumentKind.STRING, VisibleArgumentKind.STRING)
                .method().getParameterCount());
        assertEquals(2, MolangMappingRegistries.mappingTree().findMethod("query.is_item_name_any")
                .functionInfos().size(), "带 index 与不带 index 的变体必须共存");

        assertNull(MolangMappingRegistries.mappingTree().findMethod("query.isItemNameAny"),
                "驼峰注册名必须消失，只保留 BE 蛇形名");
    }

    @Test
    void rootLocatorOffsetResolvesBothAxisKindsAndConvertsToBedrockUnits() {
        BrAnimationEntryDefinition.installRootLocatorOffsetProvider((scope, locatorName) ->
                "lead".equals(locatorName) ? new Vector3f(1F, 2F, 3F) : null);
        MolangScope scope = new MolangScope();

        // 单位决策：provider 返回块，查询侧 ×16 回到 BE 的 1/16 块单位
        assertEquals(16F, RootLocatorOffsetProvider.BLOCKS_TO_BEDROCK_UNITS, EPSILON);
        assertEquals(16F, call(scope, "query.get_root_locator_offset", str("lead"), num(0)).asFloat(), EPSILON);
        assertEquals(32F, call(scope, "query.get_root_locator_offset", str("lead"), num(1)).asFloat(), EPSILON);
        assertEquals(48F, call(scope, "query.get_root_locator_offset", str("lead"), num(2)).asFloat(), EPSILON);

        // 字符串轴等价（'y' 与 '1'）
        assertEquals(32F, call(scope, "query.get_root_locator_offset", str("lead"), str("y")).asFloat(), EPSILON);
        assertEquals(48F, call(scope, "query.get_root_locator_offset", str("lead"), str("2")).asFloat(), EPSILON);

        // 越界轴 / 不存在的 locator → 0
        assertEquals(0F, call(scope, "query.get_root_locator_offset", str("lead"), num(3)).asFloat(), EPSILON);
        assertEquals(0F, call(scope, "query.get_root_locator_offset", str("lead"), str("w")).asFloat(), EPSILON);
        assertEquals(0F, call(scope, "query.get_root_locator_offset", str("missing"), num(1)).asFloat(), EPSILON);

        // 数字轴 / 字符串轴两个变体必须共存（字符串轴用变长形参错开出版签名）
        assertEquals(2, MolangMappingRegistries.mappingTree().findMethod("query.get_root_locator_offset")
                .functionInfos().size());
        assertEquals(float.class, variant("query.get_root_locator_offset",
                VisibleArgumentKind.STRING, VisibleArgumentKind.NUMBER).method().getParameterTypes()[2]);
        assertEquals(String[].class, variant("query.get_root_locator_offset",
                VisibleArgumentKind.STRING, VisibleArgumentKind.STRING).method().getParameterTypes()[2]);
    }

    private static MolangObject call(MolangScope scope, String name, MolangObject... args) {
        return MolangRuntimeSupport.resolveCall(scope, name, args);
    }

    private static MolangObject num(float value) {
        return MolangFloat.valueOf(value);
    }

    private static MolangObject str(String value) {
        return MolangString.valueOf(value);
    }

    private static FunctionInfo variant(String name, VisibleArgumentKind... callShape) {
        FunctionInfo info = MolangMappingRegistries.mappingTree().selectQueryVariant(
                name,
                List.of(callShape),
                Set.of(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG));
        assertNotNull(info, "未选中变体: " + name + " " + List.of(callShape));
        return info;
    }
}
