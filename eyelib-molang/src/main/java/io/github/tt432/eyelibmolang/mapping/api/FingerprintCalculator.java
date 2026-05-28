package io.github.tt432.eyelibmolang.mapping.api;

import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionParameterRole;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.MolangClass;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.Node;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.RegistryVersionRef;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.PublicationSignature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FingerprintCalculator {

    static RegistryVersionRef buildRegistryVersionRef(Node toplevelNode) {
        StringBuilder fingerprint = new StringBuilder();
        appendNodeFingerprint(toplevelNode, "", fingerprint);
        return new RegistryVersionRef(Integer.toUnsignedString(fingerprint.toString().hashCode(), 16));
    }

    private static void appendNodeFingerprint(Node node, String scopeName, StringBuilder fingerprint) {
        fingerprint.append("scope:").append(scopeName).append(';');

        for (MolangClass molangClass : node.actualClasses) {
            fingerprint
                    .append("class:")
                    .append(molangClass.classInstance().getName())
                    .append(':')
                    .append(molangClass.pureFunction())
                    .append(';');
        }

        for (Map.Entry<String, List<FunctionInfo>> functionEntry : node.actualFunctions.entrySet()) {
            fingerprint.append("function:").append(functionEntry.getKey()).append('=');
            for (FunctionInfo functionInfo : functionEntry.getValue()) {
                fingerprint
                        .append(describeFunction(functionInfo))
                        .append(':')
                        .append(functionInfo.molangFunction() != null ? functionInfo.molangFunction().specificity() : 0)
                        .append(':')
                        .append(functionInfo.molangFunction() != null ? functionInfo.molangFunction().priority() : 0)
                        .append(':')
                        .append(functionInfo.method().isVarArgs())
                        .append('[');
                for (FunctionParameterRole parameterRole : functionInfo.parameterRoles()) {
                    fingerprint
                            .append(parameterRole.index())
                            .append('@')
                            .append(parameterRole.parameterType().getName())
                            .append('=')
                            .append(parameterRole.role())
                            .append('=')
                            .append(parameterRole.explicit())
                            .append(',');
                }
                fingerprint.append(']');
            }
            fingerprint.append(';');
        }

        for (Map.Entry<String, Node> childEntry : node.children.entrySet()) {
            String childScope = scopeName.isEmpty() ? childEntry.getKey() : scopeName + "." + childEntry.getKey();
            appendNodeFingerprint(childEntry.getValue(), childScope, fingerprint);
        }
    }

    static String describeFunction(FunctionInfo functionInfo) {
        return functionInfo.molangClass().classInstance().getName() + "#" + functionInfo.method().getName()
                + Arrays.stream(functionInfo.method().getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    static void normalizeAndValidateNode(
            Node node,
            String scopeName,
            Comparator<MolangClass> classPublicationOrder,
            Comparator<FunctionInfo> functionPublicationOrder
    ) {
        node.actualClasses.sort(classPublicationOrder);

        List<String> childNames = new ArrayList<>(node.children.keySet());
        childNames.sort(String::compareTo);
        Map<String, Node> orderedChildren = new LinkedHashMap<>();
        for (String childName : childNames) {
            orderedChildren.put(childName, node.children.get(childName));
        }
        node.children.clear();
        node.children.putAll(orderedChildren);

        List<String> functionNames = new ArrayList<>(node.actualFunctions.keySet());
        functionNames.sort(String::compareTo);
        Map<String, List<FunctionInfo>> orderedFunctions = new LinkedHashMap<>();
        for (String functionName : functionNames) {
            List<FunctionInfo> functionInfos = node.actualFunctions.get(functionName);
            if (functionInfos != null) {
                functionInfos.sort(functionPublicationOrder);
                deduplicateEqualTies(functionInfos);
                orderedFunctions.put(functionName, functionInfos);
            }
        }
        node.actualFunctions.clear();
        node.actualFunctions.putAll(orderedFunctions);

        for (Map.Entry<String, Node> child : node.children.entrySet()) {
            String childScopeName = scopeName.isEmpty() ? child.getKey() : scopeName + "." + child.getKey();
            normalizeAndValidateNode(child.getValue(), childScopeName, classPublicationOrder, functionPublicationOrder);
        }
    }

    private static void deduplicateEqualTies(List<FunctionInfo> functionInfos) {
        if (functionInfos.size() <= 1) {
            return;
        }

        Map<PublicationSignature, FunctionInfo> lastWin = new LinkedHashMap<>();
        for (FunctionInfo functionInfo : functionInfos) {
            PublicationSignature signature = MolangMappingTree.publicationSignature(functionInfo);
            lastWin.put(signature, functionInfo);
        }
        functionInfos.clear();
        functionInfos.addAll(lastWin.values());
    }
}