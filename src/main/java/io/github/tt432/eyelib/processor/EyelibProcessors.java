package io.github.tt432.eyelib.processor;

import io.github.tt432.eyelib.processor.anno.AnnoProcessorHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author DustW
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibProcessors {
    public static void process() {
        getInstances(AnnoProcessorHolder.class, AnnoProcessor.class)
                .values()
                .forEach(AnnoProcessor::process);
    }

    public static <T> Map<ModFileScanData.AnnotationData, Class<? extends T>> getClasses(Class<?> annotationClass, Class<T> superClass) {
        HashMap<ModFileScanData.AnnotationData, Class<? extends T>> result = new HashMap<>();

        Type annotationType = Type.getType(annotationClass);
        List<ModFileScanData> allScanData = ModList.get().getAllScanData();

        for (ModFileScanData scanData : allScanData) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();

            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();

                    try {
                        Class<?> asmClass = Class.forName(memberName);
                        Class<? extends T> asmInstanceClass = asmClass.asSubclass(superClass);
                        result.put(a, asmInstanceClass);
                    } catch (ReflectiveOperationException | LinkageError e) {
                        log.error("Failed to load: {}", memberName, e);
                    }
                }
            }
        }

        return result;
    }

    public static <T> Map<ModFileScanData.AnnotationData, T> getInstances(Class<?> annotationClass, Class<T> instanceClass) {
        return getClasses(annotationClass, instanceClass).entrySet().stream()
                .map(e -> {
                    Class<? extends T> value = e.getValue();

                    try {
                        Constructor<? extends T> constructor = value.getDeclaredConstructor();
                        T instance = constructor.newInstance();
                        return Map.entry(e.getKey(), instance);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException ex) {
                        throw new RuntimeException(ex);
                    }
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
