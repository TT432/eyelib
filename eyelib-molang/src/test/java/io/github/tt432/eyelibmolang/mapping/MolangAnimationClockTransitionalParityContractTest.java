package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.binding.link.MolangQueryBindLinkContract;
import io.github.tt432.eyelibmolang.compiler.binding.link.MolangQueryBindLinker;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontends;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangAnimationClockTransitionalParityContractTest {
    private final MolangBinder binder = new MolangBinder();
    private final MolangQueryBindLinker linker = new MolangQueryBindLinker();

    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void animationClockQueriesRemainPropertyProjectionWithEmptyVisibleCallShape() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(AnimationClockParityMapping.class)));

        assertPropertyProjection("query.anim_time", "query.anim_time");
        assertPropertyProjection("query.life_time", "query.life_time");
        assertPropertyProjection("query.delta_time", "query.delta_time");
    }

    @Test
    void lifeTimeAliasParityIsBehavioralAndCandidateMetadataIsValidatedPerName() {
        AnimationClockParityMapping.animTimeValue = 12.75F;
        AnimationClockParityMapping.deltaTimeValue = 0.2F;
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(AnimationClockParityMapping.class)));

        MolangQueryBindLinkContract.QueryLinkResult animTime = linkSingle("query.anim_time");
        MolangQueryBindLinkContract.QueryLinkResult lifeTime = linkSingle("query.life_time");
        MolangQueryBindLinkContract.QueryLinkResult deltaTime = linkSingle("query.delta_time");

        assertCandidateMetadata(animTime);
        assertCandidateMetadata(lifeTime);
        assertCandidateMetadata(deltaTime);

        float animTimeValue = invokeSingleCandidate(animTime);
        float lifeTimeValue = invokeSingleCandidate(lifeTime);
        float deltaTimeValue = invokeSingleCandidate(deltaTime);

        assertEquals(AnimationClockParityMapping.animTimeValue, animTimeValue, 0.0001F);
        assertEquals(animTimeValue, lifeTimeValue, 0.0001F);
        assertEquals(AnimationClockParityMapping.deltaTimeValue, deltaTimeValue, 0.0001F);
    }

    private void assertPropertyProjection(String source, String symbolicName) {
        MolangQueryBindLinkContract.QueryLinkResult result = linkSingle(source);

        assertEquals(symbolicName, result.symbolicQueryName());
        assertEquals(BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY, result.querySurfaceKind());
        assertTrue(result.visibleCallShape().isEmpty());
        assertEquals(1, result.candidates().size());
    }

    private static void assertCandidateMetadata(MolangQueryBindLinkContract.QueryLinkResult result) {
        assertNotNull(result.candidateSetRef());
        assertNotNull(result.registryVersionRef());
        assertEquals(1, result.candidates().size());
        assertNotNull(result.candidates().get(0).candidateRef());
        assertNotNull(result.candidates().get(0).callableDescriptor());
    }

    private static float invokeSingleCandidate(MolangQueryBindLinkContract.QueryLinkResult result) {
        try {
            Object value = result.candidates().get(0).callableDescriptor().method().invoke(null);
            return ((Number) value).floatValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to invoke linked query candidate", e);
        }
    }

    private MolangQueryBindLinkContract.QueryLinkResult linkSingle(String source) {
        BindResult bindResult = bind(source);
        List<MolangQueryBindLinkContract.QueryLinkResult> links = linker.link(bindResult);
        assertEquals(1, links.size());
        return links.get(0);
    }

    private BindResult bind(String source) {
        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);
        MolangAst.ExprSet ast = parseResult.ast().orElseThrow();
        return binder.bind(ast);
    }

    private static MolangMappingDiscovery.MolangMappingClassEntry entry(Class<?> mappingClass) {
        MolangMapping mapping = mappingClass.getAnnotation(MolangMapping.class);
        assertNotNull(mapping);
        return new MolangMappingDiscovery.MolangMappingClassEntry(mapping.value(), mappingClass, mapping.pureFunction());
    }

    @MolangMapping("query")
    public static final class AnimationClockParityMapping {
        private static float animTimeValue;
        private static float deltaTimeValue;

        @MolangFunction(value = "anim_time", alias = "life_time")
        public static float animTime() {
            return animTimeValue;
        }

        @MolangFunction("delta_time")
        public static float deltaTime() {
            return deltaTimeValue;
        }
    }
}
