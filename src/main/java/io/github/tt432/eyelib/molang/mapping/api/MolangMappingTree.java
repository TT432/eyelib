package io.github.tt432.eyelib.molang.mapping.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author TT432
 */
@Slf4j
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MolangMappingTree {
    public static final MolangMappingTree INSTANCE = new MolangMappingTree();

    @SubscribeEvent
    public static void onEvent(FMLCommonSetupEvent event) {
        onModStart();
    }

    public static void onModStart() {
        Type annotationType = Type.getType(MolangMapping.class);
        List<ModFileScanData> allScanData = ModList.get().getAllScanData();

        for (ModFileScanData scanData : allScanData) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();

            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();

                    try {
                        Map<String, Object> annotationData = a.annotationData();
                        INSTANCE.addNode(annotationData.get("value").toString(),
                                new MolangClass(Class.forName(memberName), (Boolean) annotationData.get("pureFunction")));
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

    private final Node toplevelNode = new Node();

    @RequiredArgsConstructor
    private static class Node {
        final Map<String, Node> children = new HashMap<>();
        final List<MolangClass> actualClasses = new ArrayList<>();
    }

    public void addNode(String name, MolangClass actualClass) {
        String[] split = name.split("\\.");

        Node last = toplevelNode;

        for (String s : split) {
            last = last.children.computeIfAbsent(s, $ -> new Node());
        }

        last.actualClasses.add(actualClass);
    }

    public String findField(String name) {
        int i = name.indexOf(".");

        if (i != -1) {
            var classes = findClasses(name.substring(0, i));

            String foundField = null;

            for (var classData : classes) {
                var aClass = classData.classInstance;

                try {
                    String fieldName = name.substring(i);
                    aClass.getField(fieldName);

                    foundField = "${aClass.getName()}.${fieldName}";
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }

            if (foundField == null) {
                foundField = "0F";
            }

            return foundField;
        }

        return "0F";
    }

    public String findMethod(String name, String args) {
        int i = name.indexOf(".");

        if (i != -1) {
            var classes = findClasses(name.substring(0, i));

            String foundMethod = null;

            for (var classData : classes) {
                var aClass = classData.classInstance;
                String methodName = name.substring(i + 1);

                for (Method method : aClass.getMethods()) {
                    if (Modifier.isStatic(method.getModifiers())
                            && method.getReturnType().equals(float.class)
                            && method.getName().equals(methodName)) {
                        if (classData.pureFunction) {
                            foundMethod = "${aClass.getName()}.${methodName}($1, ${args})";
                        } else {
                            foundMethod = "${aClass.getName()}.${methodName}(${args})";
                        }

                        break;
                    }
                }
            }

            return Objects.requireNonNullElse(foundMethod, "0F");
        }

        return "0F";
    }

    public List<MolangClass> findClasses(String name) {
        String[] split = name.split("\\.");

        Node last = toplevelNode;

        for (String s : split) {
            last = toplevelNode.children.get(s);
        }

        return last.actualClasses;
    }
}
