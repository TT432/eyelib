package io.github.tt432.eyelib.molang.mapping.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author TT432
 */
@Slf4j
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangMappingTree {
    public static final MolangMappingTree INSTANCE = new MolangMappingTree();

    @SubscribeEvent
    public static void onEvent(FMLCommonSetupEvent event) {
        setupMolangMappingTree();
    }

    public static void setupMolangMappingTree() {
        Type annotationType = Type.getType(MolangMapping.class);
        List<ModFileScanData> allScanData = ModList.get().getAllScanData();

        for (ModFileScanData scanData : allScanData) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();

            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();

                    try {
                        Map<String, Object> annotationData = a.annotationData();
                        Object isPureFunction = annotationData.get("pureFunction");
                        INSTANCE.addNode(annotationData.get("value").toString(),
                                new MolangClass(Class.forName(memberName),
                                        isPureFunction == null || (boolean) isPureFunction));
                    } catch (ReflectiveOperationException | LinkageError e) {
                        log.error("[MolangMappingTree] Failed to load: {}", memberName, e);
                    }
                }
            }
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

    public static class Node {
        public final Map<String, Node> children = new HashMap<>();
        public final List<MolangClass> actualClasses = new ArrayList<>();
        public final Map<String, FunctionInfo> actualFunctions = new HashMap<>();
    }

    public void addNode(String name, MolangClass actualClass) {
        String[] split = name.split("\\.");

        Node last = toplevelNode;

        for (String s : split) {
            last = last.children.computeIfAbsent(s, $ -> new Node());
        }

        last.actualClasses.add(actualClass);
        for (Method method : actualClass.classInstance().getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType().equals(float.class)) {
                processMethod(actualClass, method, last);
            }
        }
    }

    private static void processMethod(MolangClass actualClass, Method method, Node last) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof MolangFunction molangFunction) {
                last.actualFunctions.put(molangFunction.value(), new FunctionInfo(molangFunction, actualClass, method));

                for (var alias : molangFunction.alias()) {
                    last.actualFunctions.put(alias, new FunctionInfo(molangFunction, actualClass, method));
                }

                return;
            }
        }

        last.actualFunctions.put(method.getName(), new FunctionInfo(null, actualClass, method));
    }

    @Nullable
    public String findField(String name) {
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

        String foundField = null;

        for (var classData : classes) {
            var aClass = classData.classInstance;

            try {
                aClass.getField(fieldName);

                foundField = aClass.getName() + "." + fieldName;
                break;
            } catch (NoSuchFieldException ignored) {
            }
        }

        return foundField;
    }

    @Nullable
    public String findMethod(String name, String args) {
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

        FunctionInfo functionInfo = node.actualFunctions.get(methodName);

        String foundMethod = null;

        if (functionInfo != null) {
            MolangClass molangClass = functionInfo.molangClass;
            Class<?> aClass = molangClass.classInstance();

            methodName = functionInfo.method.getName();

            if (!molangClass.pureFunction) {
                if (args.isEmpty()) {
                    foundMethod = aClass.getName() + "." + methodName + "($1)";
                } else {
                    foundMethod = aClass.getName() + "." + methodName + "($1, " + args + ")";
                }
            } else {
                foundMethod = aClass.getName() + "." + methodName + "(" + args + ")";
            }
        }

        return foundMethod;
    }

    private Node findNode(String name) {
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
