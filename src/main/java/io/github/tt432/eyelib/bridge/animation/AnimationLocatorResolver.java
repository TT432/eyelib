package io.github.tt432.eyelib.bridge.animation;

import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntryDefinition;
import io.github.tt432.eyelib.molang.MolangScope;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * locator 世界坐标解析的 bridge 实现，通过反射访问 RenderData 的 model components。
 *
 * @author TT432
 */
public final class AnimationLocatorResolver {

    private AnimationLocatorResolver() {
    }

    public static void install() {
        BrAnimationEntryDefinition.installLocatorProvider(AnimationLocatorResolver::resolve);
    }

    @SuppressWarnings("unchecked")
    private static Vector3f resolve(MolangScope scope, @Nullable String locatorName) {
        Entity entity = scope.getHostContext().get(Entity.class).orElse(null);
        if (entity == null) return new Vector3f();

        Vector3f fallback = entity.position().toVector3f();
        if (locatorName == null || locatorName.isEmpty()) {
            return fallback;
        }
        try {
            Object cap = Class.forName("io.github.tt432.eyelib.capability.RenderData")
                    .getMethod("getComponent", Entity.class)
                    .invoke(null, entity);
            if (cap == null) return fallback;
            List<?> comps = (List<?>) cap.getClass()
                    .getMethod("getModelComponents").invoke(cap);
            if (comps.isEmpty()) return fallback;
            Object model = comps.get(0).getClass().getMethod("getModel").invoke(comps.get(0));
            if (model == null) return fallback;
            Map<?, ?> bones = (Map<?, ?>) model.getClass()
                    .getMethod("allBones").invoke(model);
            for (Object bone : bones.values()) {
                Object locator = bone.getClass().getMethod("locator").invoke(bone);
                Map<?, ?> offsets = (Map<?, ?>) locator.getClass()
                        .getMethod("offsets").invoke(locator);
                if (offsets.containsKey(locatorName)) {
                    Vector3fc offset = (Vector3fc) offsets.get(locatorName);
                    return fallback.add(offset.x(), offset.y(), offset.z(), new Vector3f());
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
