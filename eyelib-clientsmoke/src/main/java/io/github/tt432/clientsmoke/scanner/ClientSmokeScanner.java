package io.github.tt432.clientsmoke.scanner;

import io.github.tt432.clientsmoke.config.ClientSmokeConfig;
import io.github.tt432.clientsmokeannotation.ClientSmoke;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bytecode-level annotation scanner that discovers {@code @ClientSmoke}-annotated
 * classes without triggering JVM class initialization.
 *
 * <p>Uses Forge 1.20.1 {@link ModFileScanData} infrastructure — the same mechanism
 * that discovers {@code @Mod} and {@code @EventBusSubscriber} annotations. The scan
 * reads {@code .class} file bytecodes via ASM; annotated classes are NOT loaded by
 * the JVM during discovery.</p>
 *
 * <p><strong>Class loading safety:</strong> This scanner NEVER calls
 * {@code Class.forName()}, {@code ClassLoader.loadClass()}, or any reflective API
 * that would trigger {@code <clinit>} on discovered classes. Class loading is
 * deferred to Phase 4 (test execution), after the Minecraft world is fully loaded
 * and stabilized.</p>
 *
 * <p>Per D-09: Called from the {@code @Mod} constructor after config registration.
 * Per D-10: Scans ALL JARs on the classpath via {@code ModList.get().getAllScanData()}.
 * Per D-11: Safety is validated by design — no class loading APIs present in this class.</p>
 *
 * @see ForgeMolangMappingDiscovery for the proven ModFileScanData iteration pattern
 */
public final class ClientSmokeScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmokeScanner.class);

    /**
     * Result of a successful annotation scan.
     * Contains all metadata needed for deferred test execution in Phase 4.
     */
    public record DiscoveredTest(
            String className,
            String description,
            int priority,
            String modId
    ) {}

    /**
     * Scan all loaded mod JARs for {@code @ClientSmoke}-annotated classes.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Checks {@link ClientSmokeConfig#ENABLED} — returns empty list if disabled</li>
     *   <li>Iterates ALL {@link ModFileScanData} from {@link ModList#getAllScanData()}</li>
     *   <li>Filters for annotation type matching {@code @ClientSmoke}</li>
     *   <li>Extracts class name, description, priority, and modId from bytecode data</li>
     *   <li>Logs discovered test count and names at INFO level</li>
     * </ol>
     *
     * <p><strong>Zero class loading guarantee:</strong> Uses only
     * {@link ModFileScanData.AnnotationData} which is populated by Forge's ASM
     * bytecode scanner. No {@code Class.forName()} or reflective access is performed.</p>
     *
     * @return unmodifiable list of discovered tests, empty if disabled or none found
     */
    public static List<DiscoveredTest> scan() {
        Type annotationType = Type.getType(ClientSmoke.class);
        List<DiscoveredTest> discovered = new ArrayList<>();

        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotationData : scanData.getAnnotations()) {
                if (!Objects.equals(annotationData.annotationType(), annotationType)) {
                    continue;
                }

                String className = annotationData.memberName();
                Map<String, Object> data = annotationData.annotationData();

                String description = extractString(data, "description", "");
                int priority = extractInt(data, "priority", 0);
                String modId = extractString(data, "modId", "");

                discovered.add(new DiscoveredTest(className, description, priority, modId));
            }
        }

        // Log results
        if (discovered.isEmpty()) {
            LOGGER.info("[ClientSmoke] Scan complete — no @ClientSmoke tests found");
        } else {
            LOGGER.info("[ClientSmoke] Scan complete — found {} @ClientSmoke test(s)", discovered.size());
            for (DiscoveredTest test : discovered) {
                LOGGER.info("[ClientSmoke]   → {} (priority={}, description=\"{}\")",
                        test.className(), test.priority(), test.description());
            }
        }

        return Collections.unmodifiableList(discovered);
    }

    /**
     * Extract a string value from annotation data with a default fallback.
     * Annotation attribute values in ModFileScanData are stored as their runtime types;
     * String attributes are stored as {@link String}, int attributes as {@link Number}.
     */
    private static String extractString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return (value instanceof String s) ? s : defaultValue;
    }

    private static int extractInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    private ClientSmokeScanner() {
        // Utility class — no instantiation
    }
}
