package io.github.tt432.eyelibmolang.mapping.api;

import io.github.tt432.eyelibmolang.mapping.MolangBuiltInMappings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangMappingTree {
    public static final MolangMappingTree INSTANCE = new MolangMappingTree();

    public static void setupMolangMappingTree(MolangMappingDiscovery discovery) {
        INSTANCE.clear();
        for (MolangMappingDiscovery.MolangMappingClassEntry entry : MolangBuiltInMappings.discover()) {
            INSTANCE.addNode(entry.mappingName(), new MolangClass(entry.mappingClass(), entry.pureFunction()));
        }
        for (MolangMappingDiscovery.MolangMappingClassEntry entry : discovery.discover()) {
            INSTANCE.addNode(entry.mappingName(), new MolangClass(entry.mappingClass(), entry.pureFunction()));
        }
    }

    public record MolangClass(
            Class<?> classInstance,
            boolean pureFunction
    ) {
    }

    public record FunctionInfo(
            @Nullable MolangFunction molangFunction,
            MolangClass molangClass,
            Method method
    ) {
    }

    public final Node toplevelNode = new Node();

    public void clear() {
        toplevelNode.children.clear();
        toplevelNode.actualClasses.clear();
        toplevelNode.actualFunctions.clear();
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
        for (Method method : actualClass.classInstance().getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                processMethod(actualClass, method, last);
            }
        }
    }

    private static void processMethod(MolangClass actualClass, Method method, Node last) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof MolangFunction molangFunction) {
                last.actualFunctions.computeIfAbsent(molangFunction.value(), s -> new ArrayList<>()).add(new FunctionInfo(molangFunction, actualClass, method));

                for (var alias : molangFunction.alias()) {
                    last.actualFunctions.computeIfAbsent(alias, s -> new ArrayList<>()).add(new FunctionInfo(molangFunction, actualClass, method));
                }

                return;
            }
        }

        last.actualFunctions.computeIfAbsent(method.getName(), s -> new ArrayList<>()).add(new FunctionInfo(null, actualClass, method));
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
