package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.binding.link.MolangCallableBindLinkContract;
import io.github.tt432.eyelibmolang.compiler.binding.link.MolangCallableBindLinker;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontends;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.SourceSpan;
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

class MolangCallableBindLinkContractTest {
    private final MolangBinder binder = new MolangBinder();
    private final MolangCallableBindLinker linker = new MolangCallableBindLinker();

    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void callableLinkPreservesSymbolicNameAndVisibleCallShapeForInitialEngineSubset() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        assertCallableBindAndLink(
                "math.sin(30)",
                "math.sin",
                List.of(MolangMappingTree.VisibleArgumentKind.NUMBER)
        );
        assertCallableBindAndLink(
                "math.clamp(1, 0, 1)",
                "math.clamp",
                List.of(
                        MolangMappingTree.VisibleArgumentKind.NUMBER,
                        MolangMappingTree.VisibleArgumentKind.NUMBER,
                        MolangMappingTree.VisibleArgumentKind.NUMBER
                )
        );
        assertCallableBindAndLink(
                "math.random(0, 1)",
                "math.random",
                List.of(
                        MolangMappingTree.VisibleArgumentKind.NUMBER,
                        MolangMappingTree.VisibleArgumentKind.NUMBER
                )
        );

        MolangCallableBindLinkContract.CallableLinkResult loopLink = linker.link(
                new MolangCallableBindLinkContract.CallableBindLinkRequest(
                        "loop",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER)
                )
        );
        assertEquals("loop", loopLink.symbolicCallableName());
        assertEquals(List.of(MolangMappingTree.VisibleArgumentKind.NUMBER), loopLink.visibleCallShape());
    }

    @Test
    void callableLinkIncludesStableNonNullCandidateAndRegistryRefs() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        BindResult bindResult = bind("math.sin(30)");
        MolangCallableBindLinkContract.CallableBindLinkRequest request = bindResult.callableBindLinkRequests().get(0);

        MolangCallableBindLinkContract.CallableLinkResult first = linker.link(request);
        MolangCallableBindLinkContract.CallableLinkResult second = linker.link(request);

        assertNotNull(first.candidateSetRef());
        assertNotNull(first.registryVersionRef());
        assertEquals(first.candidateSetRef(), second.candidateSetRef());
        assertEquals(first.registryVersionRef(), second.registryVersionRef());
    }

    @Test
    void callableLinkCandidateDescriptorsExposeRequiredHostRoles() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(entry(RequiredHostRoleCallableMapping.class)));

        MolangCallableBindLinkContract.CallableLinkResult linkResult = linker.link(
                new MolangCallableBindLinkContract.CallableBindLinkRequest(
                        "math.callable_host_roles",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER)
                )
        );

        assertEquals(1, linkResult.candidates().size());
        Set<MolangFunction.ParameterRole> requiredHostRoles = linkResult.candidates().get(0).requiredHostRoles();
        assertEquals(Set.of(MolangFunction.ParameterRole.RECEIVER, MolangFunction.ParameterRole.INJECTED_HOST), requiredHostRoles);
        assertFalse(requiredHostRoles.contains(MolangFunction.ParameterRole.VISIBLE_ARG));
    }

    @Test
    void unresolvedSymbolicCallableFailsLoudly() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> linker.link(new MolangCallableBindLinkContract.CallableBindLinkRequest(
                        "math.missing_case",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER)
                ))
        );

        assertTrue(exception.getMessage().contains("math.missing_case"));
    }

    @Test
    void invalidCallableSymbolicNameFailsLoudly() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> linker.link(new MolangCallableBindLinkContract.CallableBindLinkRequest(
                        " ",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER)
                ))
        );

        assertTrue(exception.getMessage().contains("Invalid canonical callable symbolic name"));
    }

    @Test
    void invalidVisibleCallShapeFailsLoudly() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        List<MolangMappingTree.VisibleArgumentKind> invalidVisibleCallShape = new ArrayList<>(1);
        invalidVisibleCallShape.add(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> linker.link(new MolangCallableBindLinkContract.CallableBindLinkRequest(
                        "math.sin",
                        invalidVisibleCallShape
                ))
        );

        assertTrue(exception.getMessage().contains("Invalid/incomplete visible call-shape"));
    }

    @Test
    void multiCandidateCallableLinkRemainsNoWinnerContract() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                entry(MultiCandidateLowerArityCallableMapping.class),
                entry(MultiCandidateHigherArityCallableMapping.class)
        ));

        MolangCallableBindLinkContract.CallableLinkResult linkResult = linker.link(
                new MolangCallableBindLinkContract.CallableBindLinkRequest(
                        "math.multi_candidate",
                        List.of(MolangMappingTree.VisibleArgumentKind.NUMBER)
                )
        );

        assertEquals(2, linkResult.candidates().size());
        List<String> recordComponents = Arrays.stream(MolangCallableBindLinkContract.CallableLinkResult.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertFalse(recordComponents.contains("selectedWinner"));
    }

    @Test
    void binderSkipsCallableEmissionForQueryRootCalls() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        BindResult bindResult = bind("query.life_time(1)");

        assertTrue(bindResult.callableBindLinkRequests().isEmpty());
        assertEquals(1, bindResult.queryBindLinkRequests().size());
    }

    @Test
    void binderSkipsCallableEmissionWhenSymbolicCallableNameExtractionIsBlank() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        SourceSpan span = SourceSpan.unknown();
        MolangAst.Expr owner = new MolangAst.IdentifierExpr(span, "math");
        MolangAst.Expr memberAccess = new MolangAst.MemberAccessExpr(span, owner, "sin");
        MolangAst.Expr groupedCallee = new MolangAst.GroupingExpr(span, memberAccess);
        MolangAst.CallExpr callExpr = new MolangAst.CallExpr(
                span,
                groupedCallee,
                List.of(new MolangAst.NumberLiteralExpr(span, "30", 30))
        );
        MolangAst.ExprSet ast = new MolangAst.ExprSet(span, callExpr);

        BindResult bindResult = binder.bind(ast);

        assertTrue(bindResult.callableBindLinkRequests().isEmpty());
        assertTrue(bindResult.queryBindLinkRequests().isEmpty());
    }

    @Test
    void binderKeepsLoopAsDedicatedControlFormWithoutCallableRequestEmission() {
        MolangMappingTree.setupMolangMappingTree(List::of);

        BindResult bindResult = bind("loop(3, {variable.counter = variable.counter + 1;})");

        assertTrue(bindResult.callableBindLinkRequests().isEmpty());
        assertTrue(bindResult.queryBindLinkRequests().isEmpty());
    }

    private void assertCallableBindAndLink(
            String source,
            String expectedSymbolicName,
            List<MolangMappingTree.VisibleArgumentKind> expectedVisibleCallShape
    ) {
        BindResult bindResult = bind(source);
        assertEquals(1, bindResult.callableBindLinkRequests().size());
        assertTrue(bindResult.queryBindLinkRequests().isEmpty());

        MolangCallableBindLinkContract.CallableBindLinkRequest request = bindResult.callableBindLinkRequests().get(0);
        assertEquals(expectedSymbolicName, request.symbolicCallableName());
        assertEquals(expectedVisibleCallShape, request.visibleCallShape());

        MolangCallableBindLinkContract.CallableLinkResult linkResult = linker.link(request);
        assertEquals(expectedSymbolicName, linkResult.symbolicCallableName());
        assertEquals(expectedVisibleCallShape, linkResult.visibleCallShape());
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

    @MolangMapping("math")
    public static final class RequiredHostRoleCallableMapping {
        @MolangFunction("callable_host_roles")
        public static float callableHostRoles(
                @MolangFunction.Role(MolangFunction.ParameterRole.RECEIVER) ReceiverHost receiver,
                @MolangFunction.Role(MolangFunction.ParameterRole.INJECTED_HOST) InjectedHost injectedHost,
                float value
        ) {
            return receiver.offset() + injectedHost.offset() + value;
        }
    }

    @MolangMapping("math")
    public static final class MultiCandidateLowerArityCallableMapping {
        @MolangFunction("multi_candidate")
        public static float multiCandidate(float value) {
            return value;
        }
    }

    @MolangMapping("math")
    public static final class MultiCandidateHigherArityCallableMapping {
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
