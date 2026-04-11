package io.github.tt432.eyelib.client.render.sync;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class RenderSyncApplyOps {
    private RenderSyncApplyOps() {
    }

    public static List<RenderModelSyncPayload> collectSerializableModelInfo(List<ModelComponent> modelComponents) {
        List<RenderModelSyncPayload> result = new ArrayList<>();
        for (ModelComponent component : modelComponents) {
            if (component.serializable()) {
                ModelComponent.SerializableInfo serializableInfo = component.getSerializableInfo();
                if (serializableInfo != null) {
                    result.add(RenderModelSyncPayload.from(serializableInfo));
                }
            }
        }
        return result;
    }

    public static void replaceModelComponents(List<ModelComponent> modelComponents,
                                              List<RenderModelSyncPayload> modelInfo,
                                              Function<RenderModelSyncPayload, ModelComponent.SerializableInfo> infoDecoder) {
        modelComponents.clear();
        for (RenderModelSyncPayload payload : modelInfo) {
            ModelComponent component = new ModelComponent();
            component.setInfo(infoDecoder.apply(payload));
            modelComponents.add(component);
        }
    }

    public static void applyAnimationInfo(Consumer<AnimationComponent.SerializableInfo> animationInfoApplier,
                                          AnimationComponent.SerializableInfo animationInfo) {
        animationInfoApplier.accept(animationInfo);
    }
}
