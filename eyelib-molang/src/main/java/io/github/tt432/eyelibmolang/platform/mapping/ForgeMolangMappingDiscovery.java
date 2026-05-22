package io.github.tt432.eyelibmolang.platform.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingDiscovery;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author TT432
 */
@NullMarked
public final class ForgeMolangMappingDiscovery implements MolangMappingDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeMolangMappingDiscovery.class);

    @Override
    public List<MolangMappingClassEntry> discover() {
        Type annotationType = Type.getType(MolangMapping.class);
        List<MolangMappingClassEntry> entries = new ArrayList<>();

        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotationData : scanData.getAnnotations()) {
                if (!Objects.equals(annotationData.annotationType(), annotationType)) {
                    continue;
                }

                String memberName = annotationData.memberName();
                try {
                    Map<String, Object> data = annotationData.annotationData();
                    Object value = data.get("value");
                    if (value == null) {
                        continue;
                    }

                    Object pureFunction = data.get("pureFunction");
                    entries.add(new MolangMappingClassEntry(
                            value.toString(),
                            Class.forName(memberName),
                            pureFunction == null || (boolean) pureFunction
                    ));
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOGGER.error("[MolangMappingTree] Failed to load: {}", memberName, e);
                }
            }
        }

        return entries;
    }
}