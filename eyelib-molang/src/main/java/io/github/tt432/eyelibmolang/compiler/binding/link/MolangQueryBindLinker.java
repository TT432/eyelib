package io.github.tt432.eyelibmolang.compiler.binding.link;

import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MolangQueryBindLinker {
    private final MolangMappingTree mappingTree;

    public MolangQueryBindLinker() {
        this(MolangMappingTree.INSTANCE);
    }

    public MolangQueryBindLinker(MolangMappingTree mappingTree) {
        this.mappingTree = Objects.requireNonNull(mappingTree, "mappingTree");
    }

    public List<MolangQueryBindLinkContract.QueryLinkResult> link(BindResult bindResult) {
        Objects.requireNonNull(bindResult, "bindResult");

        List<MolangQueryBindLinkContract.QueryLinkResult> linkResults = new ArrayList<>(bindResult.queryBindLinkRequests().size());
        for (MolangQueryBindLinkContract.QueryBindLinkRequest request : bindResult.queryBindLinkRequests()) {
            linkResults.add(link(request));
        }

        return List.copyOf(linkResults);
    }

    public MolangQueryBindLinkContract.QueryLinkResult link(MolangQueryBindLinkContract.QueryBindLinkRequest request) {
        validateRequest(request);

        MolangMappingTree.MethodData methodData = mappingTree.findMethod(request.symbolicQueryName());
        if (methodData == null) {
            throw new IllegalStateException(
                    "Unresolved canonical query symbolic name '" + request.symbolicQueryName()
                            + "' under registry version '" + mappingTree.registryVersionRef().value() + "'."
            );
        }

        List<MolangQueryBindLinkContract.QueryCandidateDescriptor> candidates = buildCandidateDescriptors(methodData);

        return new MolangQueryBindLinkContract.QueryLinkResult(
                new MolangQueryBindLinkContract.CandidateSetRef(candidateSetRef(request.symbolicQueryName(), candidates)),
                new MolangQueryBindLinkContract.RegistryVersionRef(mappingTree.registryVersionRef().value()),
                request.symbolicQueryName(),
                request.querySurfaceKind(),
                request.visibleCallShape(),
                candidates
        );
    }

    private static void validateRequest(MolangQueryBindLinkContract.QueryBindLinkRequest request) {
        Objects.requireNonNull(request, "request");

        if (!isCanonicalQuerySymbolicName(request.symbolicQueryName())) {
            throw new IllegalArgumentException(
                    "Invalid canonical query symbolic name '" + request.symbolicQueryName()
                            + "'; expected 'query.<name>' form."
            );
        }

        if (request.querySurfaceKind() == BoundMolang.BoundQueryAccessExpr.QueryProjectionKind.PROPERTY
            && !request.visibleCallShape().isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid visible call-shape for PROPERTY query surface; expected empty call-shape but got "
                            + request.visibleCallShape()
            );
        }

        if (request.visibleCallShape().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Invalid/incomplete visible call-shape for query '" + request.symbolicQueryName() + "'."
            );
        }
    }

    private static boolean isCanonicalQuerySymbolicName(String symbolicQueryName) {
        if (symbolicQueryName == null || symbolicQueryName.isBlank()) {
            return false;
        }

        if (!symbolicQueryName.startsWith("query.")) {
            return false;
        }

        return symbolicQueryName.length() > "query.".length();
    }

    private static List<MolangQueryBindLinkContract.QueryCandidateDescriptor> buildCandidateDescriptors(MolangMappingTree.MethodData methodData) {
        List<MolangQueryBindLinkContract.QueryCandidateDescriptor> candidates = new ArrayList<>(methodData.functionInfos().size());

        for (int index = 0; index < methodData.functionInfos().size(); index++) {
            MolangMappingTree.FunctionInfo functionInfo = methodData.functionInfos().get(index);
            candidates.add(new MolangQueryBindLinkContract.QueryCandidateDescriptor(
                    new MolangQueryBindLinkContract.CandidateRef(candidateRef(index, functionInfo)),
                    requiredHostRoles(functionInfo),
                    functionInfo
            ));
        }

        return List.copyOf(candidates);
    }

    private static Set<MolangFunction.ParameterRole> requiredHostRoles(MolangMappingTree.FunctionInfo functionInfo) {
        LinkedHashSet<MolangFunction.ParameterRole> requiredHostRoles = new LinkedHashSet<>();
        for (MolangMappingTree.FunctionParameterRole parameterRole : functionInfo.parameterRoles()) {
            if (parameterRole.role() != MolangFunction.ParameterRole.VISIBLE_ARG) {
                requiredHostRoles.add(parameterRole.role());
            }
        }
        return requiredHostRoles;
    }

    private static String candidateRef(int index, MolangMappingTree.FunctionInfo functionInfo) {
        return index + "|"
                + functionInfo.molangClass().classInstance().getName() + "#"
                + functionInfo.method().getName()
                + Arrays.stream(functionInfo.method().getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    private static String candidateSetRef(
            String symbolicQueryName,
            List<MolangQueryBindLinkContract.QueryCandidateDescriptor> candidates
    ) {
        String candidateSetFingerprint = candidates.stream()
                .map(candidate -> candidate.candidateRef().value())
                .collect(Collectors.joining("|"));

        return symbolicQueryName + "::" + Integer.toUnsignedString(candidateSetFingerprint.hashCode(), 16);
    }
}
