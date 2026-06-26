package io.github.tt432.clientsmoke.scanner;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通过 NeoForge 扫描数据发现客户端烟雾测试类。
 *
 * @author TT432
 */
public final class ClientSmokeScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmokeScanner.class);

    public record DiscoveredTest(String className, String description, int priority, String modId) {}

    public static List<DiscoveredTest> scan() {
        Type annotationType = Type.getType("Lio/github/tt432/clientsmokeannotation/ClientSmoke;");
        List<DiscoveredTest> discovered = new ArrayList<>();

        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotationData : scanData.getAnnotations()) {
                if (!Objects.equals(annotationData.annotationType(), annotationType)) {
                    continue;
                }

                String className = annotationData.clazz().getClassName();
                Map<String, Object> data = annotationData.annotationData();
                discovered.add(new DiscoveredTest(
                        className,
                        extractString(data, "description", ""),
                        extractInt(data, "priority", 0),
                        extractString(data, "modId", "")
                ));
            }
        }

        LOGGER.info("[ClientSmoke] Scan complete - found {} @ClientSmoke test(s)", discovered.size());
        return Collections.unmodifiableList(discovered);
    }

    private static String extractString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value instanceof String string ? string : defaultValue;
    }

    private static int extractInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private ClientSmokeScanner() {}
}
