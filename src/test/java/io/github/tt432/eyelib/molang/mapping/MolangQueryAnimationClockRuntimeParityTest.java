package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelibanimation.AnimationEffects;
import io.github.tt432.eyelibanimation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** @author TT432 */
class MolangQueryAnimationClockRuntimeParityTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void animTimeLifeTimeAndDeltaTimeFollowRuntimeDataFieldsWhenOwnerIsPresent() {
        setupAnimationClockQueryMapping();

        BrAnimationEntry entry = animationEntryWithConstantAnimTimeUpdate(6.25F);
        BrAnimationEntry.Data data = entry.createData();
        MolangScope scope = new MolangScope();

        entry.tickAnimation(data, Map.of(), scope, 4F, 1F, ModelRuntimeData.EMPTY, new AnimationEffects(), () -> {
        });
        entry.tickAnimation(data, Map.of(), scope, 9F, 1F, ModelRuntimeData.EMPTY, new AnimationEffects(), () -> {
        });
        scope.getHostContext().put(BrAnimationEntry.Data.class, data);

        float animTime = evaluateRuntimeQuery("query.anim_time", scope);
        float lifeTime = evaluateRuntimeQuery("query.life_time", scope);
        float deltaTime = evaluateRuntimeQuery("query.delta_time", scope);

        assertEquals(data.animTime(), animTime, 0.0001F);
        assertEquals(data.animTime(), lifeTime, 0.0001F);
        assertEquals(data.deltaTime(), deltaTime, 0.0001F);
    }

    @Test
    void animationClockQueriesFallbackToZeroWithoutAnimationEntryDataOwner() {
        setupAnimationClockQueryMapping();

        MolangScope scope = new MolangScope();

        assertEquals(0F, evaluateRuntimeQuery("query.anim_time", scope), 0.0001F);
        assertEquals(0F, evaluateRuntimeQuery("query.life_time", scope), 0.0001F);
        assertEquals(0F, evaluateRuntimeQuery("query.delta_time", scope), 0.0001F);
    }

    private static void setupAnimationClockQueryMapping() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(AnimationClockRuntimeParityMapping.class)));
    }

    private static BrAnimationEntry animationEntryWithConstantAnimTimeUpdate(float animTime) {
        return new BrAnimationEntry(
                "animation.test.animation_clock",
                BrLoopType.LOOP,
                2F,
                false,
                MolangValue.getConstant(animTime),
                MolangValue.ONE,
                MolangValue.ZERO,
                MolangValue.ZERO,
                io.github.tt432.eyelibanimation.AnimationEffect.empty(),
                io.github.tt432.eyelibanimation.AnimationEffect.empty(),
                io.github.tt432.eyelibanimation.AnimationEffect.empty(),
                new Int2ObjectOpenHashMap<>()
        );
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    private static float evaluateRuntimeQuery(String symbolicQueryName, MolangScope scope) {
        MolangMappingTree.MethodData methodData = MolangMappingTree.INSTANCE.findMethod(symbolicQueryName);
        assertNotNull(methodData);

        Method method = methodData.functionInfos().stream()
                .filter(functionInfo -> functionInfo.molangClass().classInstance() == AnimationClockRuntimeParityMapping.class)
                .map(MolangMappingTree.FunctionInfo::method)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing runtime parity mapping candidate for " + symbolicQueryName));

        try {
            Object result = method.invoke(null, scope);
            return ((Number) result).floatValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to invoke runtime parity query " + symbolicQueryName, e);
        }
    }

    @MolangMapping(value = "query", pureFunction = false)
    public static final class AnimationClockRuntimeParityMapping {
        @MolangFunction(value = "anim_time", alias = "life_time")
        public static float animTime(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return MolangQuery.animTime(scope);
        }

        @MolangFunction("delta_time")
        public static float deltaTime(@MolangFunction.Role(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG) MolangScope scope) {
            return MolangQuery.deltaTime(scope);
        }
    }
}
