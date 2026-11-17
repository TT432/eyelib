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

    // 名称解析缓存：findField/findMethod/selectQueryVariant 的结果只取决于注册表内容，
    // 注册表变更（addNode/clear）时整体失效。Optional.empty 表示「已解析为 null」。
    private final Map<String, Optional<FieldData>> fieldResolutionCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Optional<MethodData>> methodResolutionCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<QueryVariantKey, Optional<FunctionInfo>> queryVariantCache = new java.util.concurrent.ConcurrentHashMap<>();

    private record QueryVariantKey(String name,
                                   List<VisibleArgumentKind> visibleArgumentCallShape,
                                   Set<MolangFunction.ParameterRole> availableHostRoles) {
    }

    private void invalidateResolutionCaches() {
        fieldResolutionCache.clear();
        methodResolutionCache.clear();
        queryVariantCache.clear();
        // 单调递增的注册表纪元：MolangRuntimeSupport 零参绑定缓存以此检测失效，
        // 免回调节（每次求值一次 volatile 读 + 长整型比较）。
        epoch++;
    }
    private volatile long epoch;

    /**
     * 注册表纪元：addNode/clear/normalizeAndValidatePublicationOrder 时自增。
     * 供外层按注册表内容缓存解析结果的组件做廉价失效检测。
     */
    public long epoch() {
        return epoch;
    }

    MolangMappingTree() {
    }

    public void clear() {
        toplevelNode.children.clear();
        toplevelNode.actualClasses.clear();
        toplevelNode.actualFunctions.clear();
        toplevelNode.cachedFields.clear();
        invalidateResolutionCaches();
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
        invalidateResolutionCaches();
        // 必须与 findNode 一致按点分段：否则 addNode("query.owner", X) 会建成单段节点，
        // findNode("query") 永远找不到它，点分命名空间无法被树形态表达。
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
        invalidateResolutionCaches();
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
        Optional<FieldData> cached = fieldResolutionCache.get(name);
        if (cached != null) return cached.orElse(null);
        FieldData resolved = findFieldUncached(name);
        fieldResolutionCache.put(name, Optional.ofNullable(resolved));
        return resolved;
    }

    private @Nullable FieldData findFieldUncached(String name) {
        return probeMember(name, (node, member) -> node.cachedFields.get(member));
    }

    /**
     * 节点成员探测：按点分名逐段加深根路径，探测「当前节点 + 剩余成员名」。
     *
     * <p>顺序保证向后兼容：先把首段当根、其余整段（可再含点）当成员名探测一次
     * （历史语义），未命中才逐段加深根路径。这样 {@code addNode("query.owner", X)}
     * 注册的成员可按 {@code query.owner.<fn>} 访问，而旧式「成员名自带点」的注册仍优先。
     */
    private <T> @Nullable T probeMember(String name, NodeMemberProbe<T> probe) {
        int dot = name.indexOf('.');
        if (dot == -1) {
            // 无点分：成员挂在 "" 根下（addNode("") 的约定）
            Node root = toplevelNode.children.get("");
            return root == null ? null : probe.probe(root, name);
        }

        Node current = toplevelNode.children.get(name.substring(0, dot).toLowerCase(Locale.ROOT));
        if (current == null) {
            return null;
        }

        @Nullable T found = probe.probe(current, name.substring(dot + 1));
        int nextDot = name.indexOf('.', dot + 1);
        while (found == null && nextDot != -1) {
            current = current.children.get(name.substring(dot + 1, nextDot).toLowerCase(Locale.ROOT));
            if (current == null) {
                return null;
            }
            found = probe.probe(current, name.substring(nextDot + 1));
            dot = nextDot;
            nextDot = name.indexOf('.', dot + 1);
        }
        return found;
    }

    private interface NodeMemberProbe<T> {
        @Nullable T probe(Node node, String member);
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
        // 键直接复用调用方集合：callShape/hostRoles 均为调用方新造或共享不可变常量，无突变风险。
        var key = new QueryVariantKey(name, visibleArgumentCallShape, availableHostRoles);
        Optional<FunctionInfo> cached = queryVariantCache.get(key);
        if (cached != null) return cached.orElse(null);
        FunctionInfo resolved = VariantSelector.selectQueryVariant(this, name, visibleArgumentCallShape, availableHostRoles);
        queryVariantCache.put(key, Optional.ofNullable(resolved));
        return resolved;
    }

    @Nullable
    public MethodData findMethod(String name) {
        Optional<MethodData> cached = methodResolutionCache.get(name);
        if (cached != null) return cached.orElse(null);
        MethodData resolved = findMethodUncached(name);
        methodResolutionCache.put(name, Optional.ofNullable(resolved));
        return resolved;
    }

    private @Nullable MethodData findMethodUncached(String name) {
        List<FunctionInfo> functionInfos = probeMember(name, (node, member) -> node.actualFunctions.get(member));
        return functionInfos == null ? null : new MethodData(functionInfos);
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