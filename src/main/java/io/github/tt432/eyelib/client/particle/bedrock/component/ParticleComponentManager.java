package io.github.tt432.eyelib.client.particle.bedrock.component;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.ResourceLocations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * @author TT432
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber
public class ParticleComponentManager {
    public record ParticleComponentInfo(
            ResourceLocation name,
            String type,
            ComponentTarget target,
            Class<ParticleComponent> clazz,
            Codec<ParticleComponent> codec
    ) {
    }

    public static final Map<ResourceLocation, ParticleComponentInfo> byName = new HashMap<>();
    public static final Map<String, List<ParticleComponentInfo>> byType = new HashMap<>();

    public static Codec<ParticleComponent> codec(ResourceLocation particleComponentName) {
        ParticleComponentInfo particleComponentInfo = byName.get(particleComponentName);
        if (particleComponentInfo != null) return particleComponentInfo.codec();
        else return Codec.unit(ParticleComponent.EMPTY);
    }

    @SubscribeEvent
    public static void onEvent(FMLCommonSetupEvent event) {
        loadParticleComponents();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void loadParticleComponents() {
        Type annotationType = Type.getType(RegisterParticleComponent.class);
        List<ModFileScanData> allScanData = ModList.get().getAllScanData();

        for (ModFileScanData scanData : allScanData) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();

            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();

                    try {
                        Class clazz = Class.forName(memberName, false,
                                ParticleComponentManager.class.getClassLoader());
                        Codec codec = (Codec) clazz.getField("CODEC").get(null);
                        ComponentTarget target = ComponentTarget.valueOf(((ModAnnotation.EnumHolder) a.annotationData().get("target")).getValue());
                        var name = ResourceLocations.of((String) a.annotationData().get("value"));
                        var type = (String) a.annotationData().get("type");
                        var info = new ParticleComponentInfo(name, type, target, clazz, codec);
                        byName.put(name, info);
                        byType.computeIfAbsent(type, k -> new ArrayList<>()).add(info);
                    } catch (ReflectiveOperationException | LinkageError e) {
                        log.error("[ParticleComponentManager] Failed to load: {}", memberName, e);
                    }
                }
            }
        }
    }
}
