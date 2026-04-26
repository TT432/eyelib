package io.github.tt432.eyelibmolang.mapping.api;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.mapping.MolangBuiltInMappings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangMappingTree {
    public static final MolangMappingTree INSTANCE = new MolangMappingTree();

    public record RegistryVersionRef(String value) {
    }

    private static final Comparator<MolangMappingDiscovery.MolangMappingClassEntry> MAPPING_ENTRY_ORDER = Comparator
            .comparing((MolangMappingDiscovery.MolangMappingClassEntry entry) -> entry.mappingName().toLowerCase(Locale.ROOT))
            .thenComparing(entry -> entry.mappingClass().getName())
            .thenComparing(MolangMappingDiscovery.MolangMappingClassEntry::pureFunction);

    private static final Comparator<MolangClass> CLASS_PUBLICATION_ORDER = Comparator
            .comparing((MolangClass molangClass) -> molangClass.classInstance().getName())
            .thenComparing(MolangClass::pureFunction);

    private static final Comparator<Method> METHOD_DISCOVERY_ORDER = Comparator
            .comparing(Method::getName)
            .thenComparing(Method::isVarArgs)
            .thenComparingInt(Method::getParameterCount)
            .thenComparing(method -> Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")));

    private static final Comparator<FunctionInfo> FUNCTION_PUBLICATION_ORDER = Comparator
            .comparing((FunctionInfo functionInfo) -> publicationSignature(functionInfo).varArgs())
            .thenComparing((FunctionInfo functionInfo) -> publicationSignature(functionInfo).visibleArity(), Comparator.reverseOrder())
            .thenComparing(functionInfo -> functionInfo.molangClass().classInstance().getName())
            .thenComparing(functionInfo -> functionInfo.method().getName())
            .thenComparing(functionInfo -> Arrays.stream(functionInfo.method().getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")));

    public static void setupMolangMappingTree(MolangMappingDiscovery discovery) {
        INSTANCE.clear();
        for (MolangMappingDiscovery.MolangMappingClassEntry entry : sortEntries(MolangBuiltInMappings.discover())) {
            INSTANCE.addNode(entry.mappingName(), new MolangClass(entry.mappingClass(), entry.pureFunction()));
        }
        for (MolangMappingDiscovery.MolangMappingClassEntry entry : sortEntries(discovery.discover())) {
            INSTANCE.addNode(entry.mappingName(), new MolangClass(entry.mappingClass(), entry.pureFunction()));
        }
        INSTANCE.normalizeAndValidatePublicationOrder();
    }

    private static List<MolangMappingDiscovery.MolangMappingClassEntry> sortEntries(List<MolangMappingDiscovery.MolangMappingClassEntry> entries) {
        return entries.stream().sorted(MAPPING_ENTRY_ORDER).toList();
    }

    public record MolangClass(
            Class<?> classInstance,
            boolean pureFunction
    ) {
    }

    public record FunctionInfo(
            @Nullable MolangFunction molangFunction,
            MolangClass molangClass,
            Method method,
            List<FunctionParameterRole> parameterRoles
    ) {
    }

    public record FunctionParameterRole(
            int index,
            Class<?> parameterType,
            MolangFunction.ParameterRole role,
            boolean explicit
    ) {
    }

    public enum VisibleArgumentKind {
        NUMBER,
        BOOLEAN,
        STRING
    }

    public final Node toplevelNode = new Node();
    private RegistryVersionRef registryVersionRef = new RegistryVersionRef("0");

    public void clear() {
        toplevelNode.children.clear();
        toplevelNode.actualClasses.clear();
        toplevelNode.actualFunctions.clear();
        registryVersionRef = buildRegistryVersionRef();
    }

    public RegistryVersionRef registryVersionRef() {
        return registryVersionRef;
    }

    public static class Node {
        public final Map<String, Node> children = new HashMap<>();
        public final List<MolangClass> actualClasses = new ArrayList<>();
        public final Map<String, List<FunctionInfo>> actualFunctions = new HashMap<>();
    }

    public void addNode(String name, MolangClass actualClass) {
        String[] split = name.split("\\.");

        Node last = toplevelNode;

        for (String s : split) {
            last = last.children.computeIfAbsent(s, $ -> new Node());
        }

        last.actualClasses.add(actualClass);
        List<Method> methods = Arrays.stream(actualClass.classInstance().getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .sorted(METHOD_DISCOVERY_ORDER)
                .toList();
        for (Method method : methods) {
            processMethod(actualClass, method, last);
        }
    }

    private record PublicationSignature(boolean varArgs, int visibleArity) {
    }

    private static PublicationSignature publicationSignature(FunctionInfo functionInfo) {
        long visibleArgCount = functionInfo.parameterRoles().stream()
                .filter(parameterRole -> parameterRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG)
                .count();

        boolean varArgs = functionInfo.method().isVarArgs();
        if (varArgs && !functionInfo.parameterRoles().isEmpty()) {
            FunctionParameterRole lastRole = functionInfo.parameterRoles().get(functionInfo.parameterRoles().size() - 1);
            if (lastRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG) {
                return new PublicationSignature(true, (int) Math.max(0, visibleArgCount - 1));
            }
        }

        return new PublicationSignature(varArgs, (int) visibleArgCount);
    }

    private void normalizeAndValidatePublicationOrder() {
        normalizeAndValidateNode(toplevelNode, "");
        registryVersionRef = buildRegistryVersionRef();
    }

    private RegistryVersionRef buildRegistryVersionRef() {
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
                        .append(variantSpecificity(functionInfo))
                        .append(':')
                        .append(variantPriority(functionInfo))
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

    private void normalizeAndValidateNode(Node node, String scopeName) {
        node.actualClasses.sort(CLASS_PUBLICATION_ORDER);

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
                functionInfos.sort(FUNCTION_PUBLICATION_ORDER);
                validateEqualTieConflicts(scopeName, functionName, functionInfos);
                orderedFunctions.put(functionName, functionInfos);
            }
        }
        node.actualFunctions.clear();
        node.actualFunctions.putAll(orderedFunctions);

        for (Map.Entry<String, Node> child : node.children.entrySet()) {
            String childScopeName = scopeName.isEmpty() ? child.getKey() : scopeName + "." + child.getKey();
            normalizeAndValidateNode(child.getValue(), childScopeName);
        }
    }

    private static void validateEqualTieConflicts(String scopeName, String functionName, List<FunctionInfo> functionInfos) {
        if (functionName.isBlank() || functionInfos.size() <= 1) {
            return;
        }

        Map<PublicationSignature, FunctionInfo> signatures = new HashMap<>();
        for (FunctionInfo functionInfo : functionInfos) {
            PublicationSignature signature = publicationSignature(functionInfo);
            FunctionInfo existing = signatures.putIfAbsent(signature, functionInfo);
            if (existing != null && !existing.method().equals(functionInfo.method())) {
                String qualifiedName = scopeName.isEmpty() ? functionName : scopeName + "." + functionName;
                throw new IllegalStateException(
                        "Unresolved callable publication conflict for '" + qualifiedName
                                + "' with signature " + signature
                                + ": " + describeFunction(existing)
                                + " vs " + describeFunction(functionInfo)
                );
            }
        }
    }

    private static String describeFunction(FunctionInfo functionInfo) {
        return functionInfo.molangClass().classInstance().getName() + "#" + functionInfo.method().getName()
                + Arrays.stream(functionInfo.method().getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    private static void processMethod(MolangClass actualClass, Method method, Node last) {
        List<FunctionParameterRole> parameterRoles = resolveParameterRoles(method);

        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof MolangFunction molangFunction) {
                last.actualFunctions.computeIfAbsent(molangFunction.value(), s -> new ArrayList<>())
                        .add(new FunctionInfo(molangFunction, actualClass, method, parameterRoles));

                for (var alias : molangFunction.alias()) {
                    last.actualFunctions.computeIfAbsent(alias, s -> new ArrayList<>())
                            .add(new FunctionInfo(molangFunction, actualClass, method, parameterRoles));
                }

                return;
            }
        }

        last.actualFunctions.computeIfAbsent(method.getName(), s -> new ArrayList<>())
                .add(new FunctionInfo(null, actualClass, method, parameterRoles));
    }

    private static List<FunctionParameterRole> resolveParameterRoles(Method method) {
        Parameter[] parameters = method.getParameters();
        MolangFunction.ParameterRole[] resolved = new MolangFunction.ParameterRole[parameters.length];
        boolean[] explicit = new boolean[parameters.length];
        List<Integer> unresolvedHostCandidates = new ArrayList<>();
        List<Integer> nonSpecialHostIndices = new ArrayList<>();
        int explicitReceiverIndex = -1;

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            MolangFunction.ParameterRole explicitRole = explicitRole(parameter);
            if (explicitRole != null) {
                resolved[i] = explicitRole;
                explicit[i] = true;
                if (explicitRole == MolangFunction.ParameterRole.RECEIVER) {
                    if (explicitReceiverIndex != -1) {
                        throw roleDiscoveryFailure(method, "multiple explicit receiver parameters");
                    }
                    explicitReceiverIndex = i;
                }
                if (isNonSpecialHostRole(explicitRole)) {
                    nonSpecialHostIndices.add(i);
                }
                continue;
            }

            Class<?> parameterType = parameter.getType();
            if (isSpecialEngineArgumentType(parameterType)) {
                throw roleDiscoveryFailure(
                        method,
                        "special engine parameter '" + parameterType.getName() + "' requires explicit @MolangFunction.Role metadata"
                );
            }

            if (isVisibleArgumentType(parameterType)) {
                resolved[i] = MolangFunction.ParameterRole.VISIBLE_ARG;
                continue;
            }

            unresolvedHostCandidates.add(i);
            nonSpecialHostIndices.add(i);
        }

        if (explicitReceiverIndex != -1 && !unresolvedHostCandidates.isEmpty()) {
            throw roleDiscoveryFailure(
                    method,
                    "host parameter role ambiguity: non-receiver host parameters require explicit @MolangFunction.Role metadata"
            );
        }

        if (explicitReceiverIndex == -1 && !unresolvedHostCandidates.isEmpty()) {
            if (unresolvedHostCandidates.size() > 1) {
                throw roleDiscoveryFailure(
                        method,
                        "host parameter role ambiguity: cannot infer receiver from multiple non-special host parameters"
                );
            }

            int inferredReceiverIndex = unresolvedHostCandidates.get(0);
            int firstNonSpecialHostIndex = nonSpecialHostIndices.stream().min(Integer::compareTo).orElse(inferredReceiverIndex);
            if (firstNonSpecialHostIndex != inferredReceiverIndex) {
                throw roleDiscoveryFailure(
                        method,
                        "bounded receiver inference failure: only the first non-special host parameter may infer RECEIVER"
                );
            }

            resolved[inferredReceiverIndex] = MolangFunction.ParameterRole.RECEIVER;
        }

        List<FunctionParameterRole> parameterRoles = new ArrayList<>(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            MolangFunction.ParameterRole role = resolved[i];
            if (role == null) {
                throw roleDiscoveryFailure(method, "parameter role could not be resolved deterministically");
            }
            parameterRoles.add(new FunctionParameterRole(i, parameters[i].getType(), role, explicit[i]));
        }

        return List.copyOf(parameterRoles);
    }

    private static MolangFunction.@Nullable ParameterRole explicitRole(Parameter parameter) {
        MolangFunction.Role roleAnnotation = parameter.getAnnotation(MolangFunction.Role.class);
        return roleAnnotation != null ? roleAnnotation.value() : null;
    }

    private static boolean isNonSpecialHostRole(MolangFunction.ParameterRole role) {
        return role == MolangFunction.ParameterRole.RECEIVER || role == MolangFunction.ParameterRole.INJECTED_HOST;
    }

    private static boolean isSpecialEngineArgumentType(Class<?> type) {
        return type == MolangScope.class;
    }

    private static boolean isVisibleArgumentType(Class<?> type) {
        if (type.isPrimitive()) {
            return type != void.class;
        }

        if (type.isArray()) {
            return isVisibleArgumentType(type.getComponentType());
        }

        return Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == String.class;
    }

    private static IllegalStateException roleDiscoveryFailure(Method method, String reason) {
        String signature = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
        return new IllegalStateException(
                "Callable discovery role resolution failed for '"
                        + method.getDeclaringClass().getName()
                        + "#"
                        + method.getName()
                        + signature
                        + "': "
                        + reason
        );
    }

    public record FieldData(
            Class<?> clazz,
            Field field
    ) {
    }

    @Nullable
    public FieldData findField(String name) {
        int i = name.indexOf(".");

        String fieldName;
        String scopeName;

        if (i != -1) {
            scopeName = name.substring(0, i).toLowerCase(Locale.ROOT);
            fieldName = name.substring(i + 1);
        } else {
            scopeName = "";
            fieldName = name;
        }

        Node node = findNode(scopeName);

        List<MolangClass> classes;

        if (node == null) {
            classes = List.of();
        } else {
            classes = node.actualClasses;
        }

        for (var classData : classes) {
            var aClass = classData.classInstance;

            try {
                return new FieldData(aClass, aClass.getField(fieldName));
            } catch (NoSuchFieldException ignored) {
            }
        }

        return null;
    }

    public record MethodData(
            List<FunctionInfo> functionInfos
    ) {
    }

    @Nullable
    public FunctionInfo selectQueryVariant(
            String name,
            List<VisibleArgumentKind> visibleArgumentCallShape,
            Set<MolangFunction.ParameterRole> availableHostRoles
    ) {
        MethodData methodData = findMethod(name);
        if (methodData == null) {
            return null;
        }

        List<FunctionInfo> arityCandidates = methodData.functionInfos().stream()
                .filter(functionInfo -> visibleArityMatches(functionInfo, visibleArgumentCallShape.size()))
                .toList();
        if (arityCandidates.isEmpty()) {
            return null;
        }

        List<FunctionInfo> compatibilityCandidates = arityCandidates.stream()
                .filter(functionInfo -> visibleArgumentsCompatible(functionInfo, visibleArgumentCallShape))
                .toList();
        if (compatibilityCandidates.isEmpty()) {
            return null;
        }

        Set<MolangFunction.ParameterRole> hostRoles = availableHostRoles;
        List<FunctionInfo> hostRoleCandidates = compatibilityCandidates.stream()
                .filter(functionInfo -> requiredHostRolesAvailable(functionInfo, hostRoles))
                .toList();
        if (hostRoleCandidates.isEmpty()) {
            return null;
        }

        int highestSpecificity = hostRoleCandidates.stream()
                .mapToInt(MolangMappingTree::variantSpecificity)
                .max()
                .orElse(Integer.MIN_VALUE);
        List<FunctionInfo> specificityCandidates = hostRoleCandidates.stream()
                .filter(functionInfo -> variantSpecificity(functionInfo) == highestSpecificity)
                .toList();

        int highestPriority = specificityCandidates.stream()
                .mapToInt(MolangMappingTree::variantPriority)
                .max()
                .orElse(Integer.MIN_VALUE);
        List<FunctionInfo> priorityCandidates = specificityCandidates.stream()
                .filter(functionInfo -> variantPriority(functionInfo) == highestPriority)
                .toList();

        if (priorityCandidates.size() > 1) {
            throw variantSelectionAmbiguityFailure(
                    name,
                    priorityCandidates,
                    highestSpecificity,
                    highestPriority,
                    visibleArgumentCallShape,
                    hostRoles
            );
        }

        return priorityCandidates.get(0);
    }

    private static boolean visibleArityMatches(FunctionInfo functionInfo, int visibleArity) {
        PublicationSignature signature = publicationSignature(functionInfo);
        if (signature.varArgs()) {
            return visibleArity >= signature.visibleArity();
        }
        return visibleArity == signature.visibleArity();
    }

    private static boolean visibleArgumentsCompatible(FunctionInfo functionInfo, List<VisibleArgumentKind> visibleArgumentCallShape) {
        List<? extends Class<?>> visibleParameterTypes = visibleParameterTypes(functionInfo);
        if (!functionInfo.method().isVarArgs() || visibleParameterTypes.isEmpty()) {
            if (visibleParameterTypes.size() != visibleArgumentCallShape.size()) {
                return false;
            }

            for (int i = 0; i < visibleArgumentCallShape.size(); i++) {
                if (!visibleArgumentCompatible(visibleParameterTypes.get(i), visibleArgumentCallShape.get(i))) {
                    return false;
                }
            }
            return true;
        }

        FunctionParameterRole lastRole = functionInfo.parameterRoles().get(functionInfo.parameterRoles().size() - 1);
        if (lastRole.role() != MolangFunction.ParameterRole.VISIBLE_ARG) {
            if (visibleParameterTypes.size() != visibleArgumentCallShape.size()) {
                return false;
            }

            for (int i = 0; i < visibleArgumentCallShape.size(); i++) {
                if (!visibleArgumentCompatible(visibleParameterTypes.get(i), visibleArgumentCallShape.get(i))) {
                    return false;
                }
            }
            return true;
        }

        int fixedVisibleCount = visibleParameterTypes.size() - 1;
        if (visibleArgumentCallShape.size() < fixedVisibleCount) {
            return false;
        }

        for (int i = 0; i < fixedVisibleCount; i++) {
            if (!visibleArgumentCompatible(visibleParameterTypes.get(i), visibleArgumentCallShape.get(i))) {
                return false;
            }
        }

        Class<?> varArgComponentType = visibleParameterTypes.get(visibleParameterTypes.size() - 1).getComponentType();
        if (varArgComponentType == null) {
            return false;
        }

        for (int i = fixedVisibleCount; i < visibleArgumentCallShape.size(); i++) {
            if (!visibleArgumentCompatible(varArgComponentType, visibleArgumentCallShape.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean requiredHostRolesAvailable(FunctionInfo functionInfo, Set<MolangFunction.ParameterRole> availableHostRoles) {
        for (FunctionParameterRole parameterRole : functionInfo.parameterRoles()) {
            MolangFunction.ParameterRole role = parameterRole.role();
            if (role == MolangFunction.ParameterRole.VISIBLE_ARG) {
                continue;
            }
            if (!availableHostRoles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    private static int variantSpecificity(FunctionInfo functionInfo) {
        return functionInfo.molangFunction() == null ? 0 : functionInfo.molangFunction().specificity();
    }

    private static int variantPriority(FunctionInfo functionInfo) {
        return functionInfo.molangFunction() == null ? 0 : functionInfo.molangFunction().priority();
    }

    private static List<? extends Class<?>> visibleParameterTypes(FunctionInfo functionInfo) {
        return functionInfo.parameterRoles().stream()
                .filter(parameterRole -> parameterRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG)
                .map(FunctionParameterRole::parameterType)
                .toList();
    }

    private static boolean visibleArgumentCompatible(Class<?> parameterType, VisibleArgumentKind callShapeKind) {
        VisibleArgumentKind parameterKind = visibleArgumentKind(parameterType);
        return parameterKind == callShapeKind;
    }

    private static @Nullable VisibleArgumentKind visibleArgumentKind(Class<?> parameterType) {
        if (parameterType.isArray()) {
            return visibleArgumentKind(parameterType.getComponentType());
        }

        if (parameterType == boolean.class || parameterType == Boolean.class) {
            return VisibleArgumentKind.BOOLEAN;
        }

        if (parameterType == String.class || parameterType == char.class || parameterType == Character.class) {
            return VisibleArgumentKind.STRING;
        }

        if (parameterType.isPrimitive() || Number.class.isAssignableFrom(parameterType)) {
            return VisibleArgumentKind.NUMBER;
        }

        return null;
    }

    private static IllegalStateException variantSelectionAmbiguityFailure(
            String qualifiedName,
            List<FunctionInfo> ambiguousCandidates,
            int specificity,
            int priority,
            List<VisibleArgumentKind> visibleArgumentCallShape,
            Set<MolangFunction.ParameterRole> availableHostRoles
    ) {
        String candidates = ambiguousCandidates.stream()
                .map(MolangMappingTree::describeFunction)
                .collect(Collectors.joining(" vs "));
        return new IllegalStateException(
                "Ambiguous query variant selection for '" + qualifiedName
                        + "' with call shape " + visibleArgumentCallShape
                        + " and host roles " + availableHostRoles
                        + ": equal specificity=" + specificity
                        + " and priority=" + priority
                        + " across " + candidates
        );
    }

    @Nullable
    public MethodData findMethod(String name) {
        int i = name.indexOf(".");

        String methodName;
        String scopeName;

        if (i != -1) {
            scopeName = name.substring(0, i).toLowerCase(Locale.ROOT);
            methodName = name.substring(i + 1);
        } else {
            scopeName = "";
            methodName = name;
        }

        Node node = findNode(scopeName);

        if (node == null) {
            return null;
        }

        var functionInfo = node.actualFunctions.get(methodName);

        if (functionInfo != null) {
            return new MethodData(functionInfo);
        }

        return null;
    }

    private @Nullable Node findNode(String name) {
        String[] split = name.split("\\.");

        Node last = toplevelNode;

        for (String s : split) {
            if (last != null) {
                last = last.children.get(s);
            }
        }

        return last;
    }

    public List<MolangClass> findClasses(String name) {
        Node node = findNode(name);
        return node != null ? node.actualClasses : List.of();
    }
}
