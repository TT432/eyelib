package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import lombok.extern.slf4j.Slf4j;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author TT432
 */
@Slf4j
public class GlobalMolangFunction {
    private static final Map<String, MolangFunction> MAP = new HashMap<>();

    static {
        Type annotationType = Type.getType(MolangFunctionHolder.class);
        List<ModFileScanData> allScanData = ModList.get().getAllScanData();

        for (ModFileScanData scanData : allScanData) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();

            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();

                    try {
                        MAP.put(a.annotationData().get("value").toString(),
                                Class.forName(memberName).asSubclass(MolangFunction.class).getConstructor().newInstance());
                    } catch (ReflectiveOperationException | LinkageError e) {
                        log.error("Failed to load: {}", memberName, e);
                    }
                }
            }
        }
    }

    public static MolangFunction get(String name) {
        return MAP.get(name);
    }

    public static boolean contains(String name) {
        return MAP.containsKey(name);
    }
}
