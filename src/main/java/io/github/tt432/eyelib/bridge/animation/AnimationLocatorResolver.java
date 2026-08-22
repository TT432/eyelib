package io.github.tt432.eyelib.bridge.animation;

import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntryDefinition;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import io.github.tt432.eyelib.molang.mapping.api.HostRoles;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * locator 世界坐标解析的 bridge 实现，通过反射访问 RenderData 的 model components。
 * 反射只为绕开 bridge → capability 的模块依赖；Class/Method 全部启动期缓存，
 * 热路径无 Class.forName / getMethod 查找。
 *
 * @author TT432
 */
public interface AnimationLocatorResolver {
    HostRole<Entity> ENTITY = HostRole.of("entity", Entity.class);

    static void install() {
        BrAnimationEntryDefinition.installLocatorProvider(AnimationLocatorResolver::resolve);
    }

    private static Matrix4f resolve(MolangScope scope, @Nullable String locatorName) {
        Entity entity = scope.getHostContext().get(ENTITY).orElse(null);
        if (entity == null) {
            return new Matrix4f();
        }

        float partialTick = scope.contains("variable.partial_tick")
                ? scope.get("variable.partial_tick").asFloat()
                : 0F;
        Matrix4f entityPose = new Matrix4f().translation(
                (float) Mth.lerp(partialTick, entity.xOld, entity.getX()),
                (float) Mth.lerp(partialTick, entity.yOld, entity.getY()),
                (float) Mth.lerp(partialTick, entity.zOld, entity.getZ())
        );
        if (locatorName == null || locatorName.isEmpty()) {
            return entityPose;
        }

        scope.getHostContext().get(HostRoles.CLIENT_ENTITY).flatMap(io.github.tt432.eyelib.importer.entity.BrClientEntity::scripts)
                .ifPresent(scripts -> entityPose.scale(
                        scripts.getScaleX(scope), scripts.getScaleY(scope), scripts.getScaleZ(scope)));
        if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.isBaby()) {
                entityPose.scale(0.5F);
            }
            float yBodyRot = Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
            entityPose.rotateY((float) Math.toRadians(-yBodyRot));
        }

        Object renderData = renderData(scope, entity);
        if (renderData == null) {
            return entityPose;
        }

        ModelRuntimeData data = scope.getHostContext().get(HostRoles.MODEL_RUNTIME_DATA)
                .orElse(ModelRuntimeData.EMPTY);
        try {
            List<?> components = (List<?>) Refs.GET_MODEL_COMPONENTS.invoke(renderData);
            for (Object component : components) {
                Method getModel = GET_MODEL.get(component.getClass());
                if (getModel == null) {
                    continue;
                }
                Object value = getModel.invoke(component);
                if (value instanceof Model model) {
                    var resolved = ModelPoseTransforms.resolveLocatorPose(model, data, locatorName, entityPose);
                    if (resolved.isPresent()) {
                        return resolved.get();
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            return entityPose;
        }
        return entityPose;
    }

    private static @Nullable Object renderData(MolangScope scope, Entity entity) {
        Object scoped = scope.getHostContext().get(Refs.RENDER_DATA_CLASS).orElse(null);
        if (scoped != null) {
            return scoped;
        }
        try {
            return Refs.GET_COMPONENT.invoke(null, entity);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /** 启动期一次性解析的反射引用。 */
    final class Refs {
        static final Class<?> RENDER_DATA_CLASS;
        static final Method GET_COMPONENT;
        static final Method GET_MODEL_COMPONENTS;

        static {
            try {
                RENDER_DATA_CLASS = Class.forName("io.github.tt432.eyelib.capability.RenderData");
                GET_COMPONENT = RENDER_DATA_CLASS.getMethod("getComponent", Entity.class);
                GET_MODEL_COMPONENTS = RENDER_DATA_CLASS.getMethod("getModelComponents");
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Refs() {
        }
    }

    /** 各组件实现类的 getModel 方法缓存（组件类集合封闭且少量）。 */
    ClassValue<@Nullable Method> GET_MODEL = new ClassValue<>() {
        @Override
        protected @Nullable Method computeValue(Class<?> type) {
            try {
                return type.getMethod("getModel");
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    };
}
