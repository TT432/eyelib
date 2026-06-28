package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.util.event.api.OnRenderStage;
import io.github.tt432.eyelib.util.event.api.RenderStageSubscriberDiscovery;
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Forge Mod 文件扫描实现的渲染阶段订阅者发现，对齐 {@code ForgeMolangMappingDiscovery} 范式。
 * 借助 (Neo)Forge 反射系统的 ASM 预扫描数据（{@code ModFileScanData.AnnotationData}）发现 {@code @OnRenderStage} 方法。
 *
 * @author TT432
 */
public final class ForgeRenderStageSubscriberDiscovery implements RenderStageSubscriberDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeRenderStageSubscriberDiscovery.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType SIGNATURE =
            MethodType.methodType(void.class, float.class, double.class, double.class, double.class);

    @Override
    public List<RenderStageSubscriber> discover() {
        Type annotationType = Type.getType(OnRenderStage.class);
        List<RenderStageSubscriber> entries = new ArrayList<>();

        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotationData : scanData.getAnnotations()) {
                if (!Objects.equals(annotationData.annotationType(), annotationType)) {
                    continue;
                }

                String memberName = annotationData.memberName();
                try {
                    String className = extractClassName(memberName);
                    String methodName = extractMethodName(memberName);

                    Class<?> clazz = Class.forName(className);
                    MethodHandle handle = LOOKUP.findStatic(clazz, methodName, SIGNATURE);
                    entries.add(new RenderStageSubscriber(clazz, methodName, handle));
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOGGER.error("[RenderStage] Failed to load: {}", memberName, e);
                }
            }
        }

        return entries;
    }

    private static String extractClassName(String memberName) {
        String nameWithoutDesc = stripDescriptor(memberName);
        int lastDot = nameWithoutDesc.lastIndexOf('.');
        return nameWithoutDesc.substring(0, lastDot);
    }

    private static String extractMethodName(String memberName) {
        String nameWithoutDesc = stripDescriptor(memberName);
        int lastDot = nameWithoutDesc.lastIndexOf('.');
        return nameWithoutDesc.substring(lastDot + 1);
    }

    private static String stripDescriptor(String memberName) {
        int parenIdx = memberName.indexOf('(');
        return parenIdx >= 0 ? memberName.substring(0, parenIdx) : memberName;
    }
}
