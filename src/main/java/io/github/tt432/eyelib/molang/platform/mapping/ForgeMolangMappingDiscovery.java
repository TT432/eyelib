package io.github.tt432.eyelib.molang.platform.mapping;

import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
//? if <1.20.6 {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
//?} else {
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
//?}
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Forge Mod 文件扫描实现的映射发现。
 *
 * @author TT432
 */
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
