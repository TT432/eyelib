package io.github.tt432.eyelib.molang;

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
                        INSTANCE.addNode(a.annotationData().get("value").toString(), Class.forName(memberName));
                    } catch (ReflectiveOperationException | LinkageError e) {
                        log.error("[MolangMappingTree] Failed to load: {}", memberName, e);
                    }
                }
            }
        }
    }

    private final Map<String, Node> children = new HashMap<>();

    @RequiredArgsConstructor
    private static class Node {
        final Map<String, Node> children = new HashMap<>();
        final List<Class<?>> actualClasses = new ArrayList<>();
    }

    public void addNode(String name, Class<?> actualClass) {
        String[] split = name.split("\\.");

        Node last = null;

        for (String s : split) {
            if (last == null) {
                last = children.computeIfAbsent(s, $ -> new Node());
            } else {
                last = last.children.computeIfAbsent(s, $ -> new Node());
            }
        }

        if (last != null) {
            last.actualClasses.add(actualClass);
        }
    }

    public String findField(String name) {
        int i = name.indexOf(".");

        if (i != -1) {
            List<Class<?>> classes = findClasses(name.substring(0, i));

            String foundField = null;

            for (Class<?> aClass : classes) {
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

    public String findMethod(String name) {
        int i = name.indexOf(".");

        if (i != -1) {
            List<Class<?>> classes = findClasses(name.substring(0, i));

            String foundMethod = null;

            for (Class<?> aClass : classes) {
                String methodName = name.substring(i + 1);

                for (Method method : aClass.getDeclaredMethods()) {
                    if (Modifier.isPublic(method.getModifiers())
                            && Modifier.isStatic(method.getModifiers())
                            && method.getReturnType().equals(float.class)
                            && method.getName().equals(methodName)) {
                        foundMethod = "${aClass.getName()}.${methodName}";
                        break;
                    }
                }
            }

            if (foundMethod == null) {
                foundMethod = "0F";
            }

            return foundMethod;
        }

        return "0F";
    }

    public List<Class<?>> findClasses(String name) {
        String[] split = name.split("\\.");

        Node last = null;

        for (String s : split) {
            last = children.get(s);
        }

        if (last != null) {
            return last.actualClasses;
        }

        return List.of();
    }
}
