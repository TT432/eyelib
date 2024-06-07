package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.network.ModelComponentSyncPacket;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author TT432
 */
@Getter
public class RenderData<T> {
    public static final Codec<RenderData<Object>> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ModelComponent.SerializableInfo.CODEC.optionalFieldOf("model").forGetter(ac -> Optional.ofNullable(ac.modelComponent.getSerializableInfo())),
            AnimationComponent.SerializableInfo.CODEC.optionalFieldOf("animation").forGetter(ac -> Optional.ofNullable(ac.animationComponent.getSerializableInfo()))
    ).apply(ins, (mcsi, acsi) -> {
        RenderData<Object> result = new RenderData<>();
        mcsi.ifPresent(result.modelComponent::setInfo);
        acsi.ifPresent(i -> result.animationComponent.setup(i.animationControllers(), i.targetAnimations()));
        return result;
    }));

    public static RenderData<Object> getComponent(Entity entity) {
        return entity.getData(EyelibAttachableData.RENDER_DATA);
    }

    private T owner;
    private MolangScope scope;

    @NotNull
    private final ModelComponent modelComponent = new ModelComponent();

    @NotNull
    private final AnimationComponent animationComponent = new AnimationComponent();

    public void sync() {
        if (modelComponent.serializable()) {
            ownerAs(Entity.class).ifPresent(e -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(e,
                    new ModelComponentSyncPacket(
                            e.getId(),
                            modelComponent.getSerializableInfo())));
        }

        if (animationComponent.serializable()) {
            ownerAs(Entity.class).ifPresent(e -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(e,
                    new AnimationComponentSyncPacket(
                            e.getId(),
                            animationComponent.getSerializableInfo())));
        }
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
    }
}
