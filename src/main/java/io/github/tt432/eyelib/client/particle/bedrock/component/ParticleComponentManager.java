package io.github.tt432.eyelib.client.particle.bedrock.component;

import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * @author TT432
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ParticleComponentManager {
    public record ParticleComponentInfo(
            ResourceLocation name,
            String type,
            ComponentTarget target,
            Class<?> clazz,
            Codec<Object> codec
    ) {
    }

    public static class ComponentSet {
        public final Map<ResourceLocation, ParticleComponentInfo> byName = new HashMap<>();
        public final Map<String, List<ParticleComponentInfo>> byType = new HashMap<>();
    }

    public static final ComponentSet all = new ComponentSet();
    public static final ComponentSet emitter = new ComponentSet();
    public static final ComponentSet particle = new ComponentSet();

    @SubscribeEvent
    public static void onEvent(FMLCommonSetupEvent event) {
        loadParticleComponents();
    }

    private static void loadParticleComponents() {
        Type annotationType = Type.getType(ParticleComponent.class);
        List<ModFileScanData> allScanData = ModList.get().getAllScanData();

        for (ModFileScanData scanData : allScanData) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();

            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();

                    try {
                        var clazz = Class.forName(memberName, false,
                                ParticleComponentManager.class.getClassLoader());
                        Codec codec = (Codec) clazz.getField("CODEC").get(null);
                        ComponentTarget target = ComponentTarget.valueOf(((ModAnnotation.EnumHolder) a.annotationData().get("target")).value());
                        var name = ResourceLocation.parse((String) a.annotationData().get("value"));
                        var type = (String) a.annotationData().get("type");
                        var info = new ParticleComponentInfo(name, type, target, clazz, codec);
                        var set = switch (target) {
                            case EMITTER -> emitter;
                            case PARTICLE -> particle;
                        };
                        set.byName.put(name, info);
                        set.byType.computeIfAbsent(type, k -> new ArrayList<>()).add(info);
                        all.byName.put(name, info);
                        all.byType.computeIfAbsent(type, k -> new ArrayList<>()).add(info);
                    } catch (ReflectiveOperationException | LinkageError e) {
                        log.error("[ParticleComponentManager] Failed to load: {}", memberName, e);
                    }
                }
            }
        }
    }
}
