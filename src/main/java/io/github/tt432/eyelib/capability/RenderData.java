package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.render.sync.ClientRenderSyncService;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelib.mc.impl.data_attach.DataAttachmentHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author TT432
 */
@Getter
public class RenderData<T> {
    public static <T> Codec<RenderData<T>> codec() {
        return RecordCodecBuilder.create(ins -> ins.group(
                ModelComponent.SerializableInfo.CODEC.listOf().optionalFieldOf("model").forGetter(ac -> Optional.of(ac.modelComponents.stream().map(ModelComponent::getSerializableInfo).toList())),
                AnimationComponent.SerializableInfo.CODEC.optionalFieldOf("animation").forGetter(ac -> Optional.ofNullable(ac.animationComponent.getSerializableInfo()))
        ).apply(ins, (mcsi, acsi) -> {
            RenderData<T> result = new RenderData<>();
            mcsi.ifPresent(l -> {
                for (ModelComponent.SerializableInfo serializableInfo : l) {
                    ModelComponent e = new ModelComponent();
                    e.setInfo(serializableInfo);
                    result.modelComponents.add(e);
                }
            });
            acsi.ifPresent(result.animationComponent::setInfo);
            return result;
        }));
    }

    @SuppressWarnings("unchecked")
    public static <T>RenderData<T> getComponent(Entity entity) {
        return (RenderData<T>) DataAttachmentHelper.getOrCreate(EyelibAttachableData.RENDER_DATA.get(), entity);
    }

    @Nullable
    private T owner;
    @Nullable
    private MolangScope scope;
    @Setter
    private boolean useBuiltInRenderSystem = true;

    private final List<ModelComponent> modelComponents = new ArrayList<>();

    private final AnimationComponent animationComponent = new AnimationComponent();

    private final ClientEntityComponent clientEntityComponent = new ClientEntityComponent();

    private final RenderControllerComponent renderControllerComponent = new RenderControllerComponent();

    public void sync() {
        ClientRenderSyncService.sync(this);
    }

    @SuppressWarnings("unchecked")
    public <N> Optional<N> ownerAs(Class<N> tClass) {
        if (tClass.isInstance(owner)) {
            return Optional.of((N) owner);
        }

        return Optional.empty();
    }

    public void init(T owner) {
        this.owner = owner;
        scope = new MolangScope();
        scope.setOwner(this);
        scope.setOwner(this.owner);

        scope.set("variable.scale", 1);
    }
}

