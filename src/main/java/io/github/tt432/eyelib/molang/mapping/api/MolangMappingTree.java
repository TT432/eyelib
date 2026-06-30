package io.github.tt432.eyelib.molang.mapping.api;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Molang 映射注册表，管理所有已注册的函数/字段映射。
 *
 * @author TT432
 */
public class MolangMappingTree {
    public record RegistryVersionRef(String value) {
    }

    static final Comparator<MolangClass> CLASS_PUBLICATION_ORDER = Comparator
            .comparing((MolangClass molangClass) -> molangClass.classInstance().getName())
            .thenComparing(MolangClass::pureFunction);

    private static final Comparator<Method> METHOD_DISCOVERY_ORDER = Comparator
            .comparing(Method::getName)
            .thenComparing(Method::isVarArgs)
            .thenComparingInt(Method::getParameterCount)
            .thenComparing(method -> Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")));

    static final Comparator<FunctionInfo> FUNCTION_PUBLICATION_ORDER = Comparator
            .comparing((FunctionInfo functionInfo) -> publicationSignature(functionInfo).varArgs())
            .thenComparing((FunctionInfo functionInfo) -> publicationSignature(functionInfo).visibleArity(), Comparator.reverseOrder())
            .thenComparing(functionInfo -> functionInfo.molangClass().classInstance().getName())
            .thenComparing(functionInfo -> functionInfo.method().getName())
            .thenComparing(functionInfo -> Arrays.stream(functionInfo.method().getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")));

    public static void setupMolangMappingTree(MolangMappingDiscovery discovery) {
        MolangMappingRegistries.setupMappingTree(discovery);
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

    MolangMappingTree() {
    }

    public void clear() {
        toplevelNode.children.clear();
        toplevelNode.actualClasses.clear();
        toplevelNode.actualFunctions.clear();
        toplevelNode.cachedFields.clear();
        registryVersionRef = FingerprintCalculator.buildRegistryVersionRef(toplevelNode);
    }

    public RegistryVersionRef registryVersionRef() {
        return registryVersionRef;
    }

    public static class Node {
        public final Map<String, Node> children = new HashMap<>();
        public final List<MolangClass> actualClasses = new ArrayList<>();
        public final Map<String, List<FunctionInfo>> actualFunctions = new HashMap<>();
        public final Map<String, FieldData> cachedFields = new HashMap<>();
    }

    public void addNode(String name, MolangClass actualClass) {
        String[] split = name.split("\\.");

        Node last = toplevelNode;

        for (String s : split) {
            last = last.children.computeIfAbsent(s, $ -> new Node());
        }

        for (MolangClass existing : last.actualClasses) {
            if (existing.classInstance().equals(actualClass.classInstance())) {
                return;
            }
        }

        last.actualClasses.add(actualClass);
        // 预建字段缓存：putIfAbsent 保证先注册类的同名字段优先，与原 findField 顺序遍历语义一致。
        for (Field field : actualClass.classInstance().getFields()) {
            last.cachedFields.putIfAbsent(field.getName(), new FieldData(actualClass.classInstance(), field));
        }
        List<Method> methods = Arrays.stream(actualClass.classInstance().getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .sorted(METHOD_DISCOVERY_ORDER)
                .toList();
        for (Method method : methods) {
            ParameterRoleResolver.processMethod(actualClass, method, last);
        }
    }

    record PublicationSignature(boolean varArgs, int visibleArity) {
    }

    static PublicationSignature publicationSignature(FunctionInfo functionInfo) {
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

    void normalizeAndValidatePublicationOrder() {
        FingerprintCalculator.normalizeAndValidateNode(toplevelNode, "", CLASS_PUBLICATION_ORDER, FUNCTION_PUBLICATION_ORDER);
        registryVersionRef = FingerprintCalculator.buildRegistryVersionRef(toplevelNode);
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
        return node == null ? null : node.cachedFields.get(fieldName);
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
        return VariantSelector.selectQueryVariant(this, name, visibleArgumentCallShape, availableHostRoles);
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