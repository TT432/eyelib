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

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangQueryBindLinkContractTest {
    private final MolangBinder binder = new MolangBinder();
    private final MolangQueryBindLinker linker = new MolangQueryBindLinker();

    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void propertyQueryLinkPreservesSurfaceKindAndEmptyVisibleCallShape() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(PropertyMapping.class)));

        BindResult bindResult = bind("query.property_case");
        List<MolangQueryBindLinkContract.QueryLinkResult> links = linker.link(bindResult);

        assertEquals(1, links.size());
        MolangQueryBindLinkContract.QueryLinkResult linkResult = links.get(0);
        assertEquals("query.property_case", linkResult.symbolicQueryName());
        assertEquals(BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY, linkResult.querySurfaceKind());
        assertTrue(linkResult.visibleCallShape().isEmpty());
    }

    @Test
    void explicitCallQueryLinkPreservesSurfaceKindAndVisibleCallShapeOrder() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(ExplicitCallShapeMapping.class)));

        BindResult bindResult = bind("query.call_shape(1, 'toast', false)");
        List<MolangQueryBindLinkContract.QueryLinkResult> links = linker.link(bindResult);

        assertEquals(1, links.size());
        MolangQueryBindLinkContract.QueryLinkResult linkResult = links.get(0);
        assertEquals("query.call_shape", linkResult.symbolicQueryName());
        assertEquals(BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.EXPLICIT_CALL, linkResult.querySurfaceKind());
        assertEquals(
                List.of(
                        MolangMappingTree.VisibleArgumentKind.NUMBER,
                        MolangMappingTree.VisibleArgumentKind.STRING,
                        MolangMappingTree.VisibleArgumentKind.BOOLEAN
                ),
                linkResult.visibleCallShape()
        );
    }

    @Test
    void queryLinkIncludesStableNonNullCandidateAndRegistryRefs() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(PropertyMapping.class)));

        BindResult bindResult = bind("query.property_case");
        MolangQueryBindLinkContract.QueryBindLinkRequest request = bindResult.queryBindLinkRequests().get(0);

        MolangQueryBindLinkContract.QueryLinkResult first = linker.link(request);
        MolangQueryBindLinkContract.QueryLinkResult second = linker.link(request);

        assertNotNull(first.candidateSetRef());
        assertNotNull(first.registryVersionRef());
        assertEquals(first.candidateSetRef(), second.candidateSetRef());
        assertEquals(first.registryVersionRef(), second.registryVersionRef());
    }

    @Test
    void queryLinkCandidateDescriptorsExposeRequiredHostRoles() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(RequiredHostRoleMapping.class)));

        BindResult bindResult = bind("query.host_roles(1)");
        MolangQueryBindLinkContract.QueryLinkResult linkResult = linker.link(bindResult).get(0);

        assertEquals(1, linkResult.candidates().size());
        Set<MolangFunction.ParameterRole> requiredHostRoles = linkResult.candidates().get(0).requiredHostRoles();
        assertEquals(Set.of(MolangFunction.ParameterRole.RECEIVER, MolangFunction.ParameterRole.INJECTED_HOST), requiredHostRoles);
        assertFalse(requiredHostRoles.contains(MolangFunction.ParameterRole.VISIBLE_ARG));
    }

    @Test
    void unresolvedSymbolicQueryFailsLoudly() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(PropertyMapping.class)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> linker.link(new MolangQueryBindLinkContract.QueryBindLinkRequest(
                        "query.missing_case",
                        BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY,
                        List.of()
                ))
        );

        assertTrue(exception.getMessage().contains("query.missing_case"));
    }

    @Test
    void invalidVisibleCallShapeFailsLoudly() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(ExplicitCallShapeMapping.class)));

        List<MolangMappingTree.VisibleArgumentKind> invalidVisibleCallShape = new ArrayList<>(1);
        invalidVisibleCallShape.add(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> linker.link(new MolangQueryBindLinkContract.QueryBindLinkRequest(
                        "query.call_shape",
                        BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.EXPLICIT_CALL,
                        invalidVisibleCallShape
                ))
        );

        assertTrue(exception.getMessage().contains("Invalid/incomplete visible call-shape"));
    }

    @Test
    void multiCandidateQueryLinkRemainsNoWinnerContract() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(MultiCandidateLowerArityMapping.class),
                entry(MultiCandidateHigherArityMapping.class)
        ));

        BindResult bindResult = bind("query.multi_candidate(1)");
        MolangQueryBindLinkContract.QueryLinkResult linkResult = linker.link(bindResult).get(0);

        assertEquals(2, linkResult.candidates().size());
        List<String> recordComponents = Arrays.stream(MolangQueryBindLinkContract.QueryLinkResult.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertFalse(recordComponents.contains("selectedWinner"));
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
    public static final class PropertyMapping {
        @MolangFunction("property_case")
        public static float propertyCase() {
            return 1;
        }
    }

    @MolangMapping("query")
    public static final class ExplicitCallShapeMapping {
        @MolangFunction("call_shape")
        public static float callShape(float numberValue, String stringValue, boolean booleanValue) {
            return numberValue + stringValue.length() + (booleanValue ? 1 : 0);
        }
    }

    @MolangMapping("query")
    public static final class RequiredHostRoleMapping {
        @MolangFunction("host_roles")
        public static float hostRoles(
                @MolangFunction.Role(MolangFunction.ParameterRole.RECEIVER) ReceiverHost receiver,
                @MolangFunction.Role(MolangFunction.ParameterRole.INJECTED_HOST) InjectedHost injectedHost,
                float value
        ) {
            return receiver.offset() + injectedHost.offset() + value;
        }
    }

    @MolangMapping("query")
    public static final class MultiCandidateLowerArityMapping {
        @MolangFunction("multi_candidate")
        public static float multiCandidate(float value) {
            return value;
        }
    }

    @MolangMapping("query")
    public static final class MultiCandidateHigherArityMapping {
        @MolangFunction("multi_candidate")
        public static float multiCandidate(float left, float right) {
            return left + right;
        }
    }

    public record ReceiverHost(float offset) {
    }

    public record InjectedHost(float offset) {
    }
}
