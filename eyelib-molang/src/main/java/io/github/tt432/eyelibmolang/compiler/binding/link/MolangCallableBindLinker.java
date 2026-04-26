package io.github.tt432.eyelibmolang.compiler.binding.link;

import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MolangCallableBindLinker {
    private final MolangMappingTree mappingTree;

    public MolangCallableBindLinker() {
        this(MolangMappingTree.INSTANCE);
    }

    public MolangCallableBindLinker(MolangMappingTree mappingTree) {
        this.mappingTree = Objects.requireNonNull(mappingTree, "mappingTree");
    }

    public List<MolangCallableBindLinkContract.CallableLinkResult> link(BindResult bindResult) {
        Objects.requireNonNull(bindResult, "bindResult");

        List<MolangCallableBindLinkContract.CallableLinkResult> linkResults = new ArrayList<>(bindResult.callableBindLinkRequests().size());
        for (MolangCallableBindLinkContract.CallableBindLinkRequest request : bindResult.callableBindLinkRequests()) {
            linkResults.add(link(request));
        }

        return List.copyOf(linkResults);
    }

    public MolangCallableBindLinkContract.CallableLinkResult link(MolangCallableBindLinkContract.CallableBindLinkRequest request) {
        validateRequest(request);

        MolangMappingTree.MethodData methodData = mappingTree.findMethod(request.symbolicCallableName());
        if (methodData == null) {
            throw new IllegalStateException(
                    "Unresolved canonical callable symbolic name '" + request.symbolicCallableName()
                            + "' under registry version '" + mappingTree.registryVersionRef().value() + "'."
            );
        }

        List<MolangCallableBindLinkContract.CallableCandidateDescriptor> candidates = buildCandidateDescriptors(methodData);

        return new MolangCallableBindLinkContract.CallableLinkResult(
                new MolangCallableBindLinkContract.CandidateSetRef(candidateSetRef(request.symbolicCallableName(), candidates)),
                new MolangCallableBindLinkContract.RegistryVersionRef(mappingTree.registryVersionRef().value()),
                request.symbolicCallableName(),
                request.visibleCallShape(),
                candidates
        );
    }

    private static void validateRequest(MolangCallableBindLinkContract.CallableBindLinkRequest request) {
        Objects.requireNonNull(request, "request");

        if (!isCanonicalCallableSymbolicName(request.symbolicCallableName())) {
            throw new IllegalArgumentException(
                    "Invalid canonical callable symbolic name '" + request.symbolicCallableName() + "'."
            );
        }

        if (request.visibleCallShape().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Invalid/incomplete visible call-shape for callable '" + request.symbolicCallableName() + "'."
            );
        }
    }

    private static boolean isCanonicalCallableSymbolicName(String symbolicCallableName) {
        if (symbolicCallableName == null || symbolicCallableName.isBlank()) {
            return false;
        }

        if (symbolicCallableName.startsWith(".") || symbolicCallableName.endsWith(".")) {
            return false;
        }

        if (symbolicCallableName.contains("..")) {
            return false;
        }

        return Arrays.stream(symbolicCallableName.split("\\."))
                .noneMatch(String::isBlank);
    }

    private static List<MolangCallableBindLinkContract.CallableCandidateDescriptor> buildCandidateDescriptors(MolangMappingTree.MethodData methodData) {
        List<MolangCallableBindLinkContract.CallableCandidateDescriptor> candidates = new ArrayList<>(methodData.functionInfos().size());

        for (int index = 0; index < methodData.functionInfos().size(); index++) {
            MolangMappingTree.FunctionInfo functionInfo = methodData.functionInfos().get(index);
            candidates.add(new MolangCallableBindLinkContract.CallableCandidateDescriptor(
                    new MolangCallableBindLinkContract.CandidateRef(candidateRef(index, functionInfo)),
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
            String symbolicCallableName,
            List<MolangCallableBindLinkContract.CallableCandidateDescriptor> candidates
    ) {
        String candidateSetFingerprint = candidates.stream()
                .map(candidate -> candidate.candidateRef().value())
                .collect(Collectors.joining("|"));

        return symbolicCallableName + "::" + Integer.toUnsignedString(candidateSetFingerprint.hashCode(), 16);
    }
}
