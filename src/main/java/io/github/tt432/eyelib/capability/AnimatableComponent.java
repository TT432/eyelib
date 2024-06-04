package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.network.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import io.github.tt432.eyelib.network.ModelComponentSyncPacket;
import io.github.tt432.eyelib.util.IdentifiableObject;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author TT432
 */
@Getter
public class AnimatableComponent<T> implements IdentifiableObject {
    public static final Codec<AnimatableComponent<Object>> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ModelComponent.SerializableInfo.CODEC.optionalFieldOf("model").forGetter(ac -> Optional.ofNullable(ac.modelComponent.getSerializableInfo())),
            AnimationComponent.SerializableInfo.CODEC.optionalFieldOf("animation").forGetter(ac -> Optional.ofNullable(ac.animationComponent.getSerializableInfo()))
    ).apply(ins, (mcsi, acsi) -> {
        AnimatableComponent<Object> result = new AnimatableComponent<>();
        mcsi.ifPresent(result.modelComponent::setInfo);
        acsi.ifPresent(i -> result.animationComponent.setup(i.animationControllers(), i.targetAnimations()));
        return result;
    }));

    @Nullable
    public static <T extends Entity> AnimatableComponent<T> getComponent(T entity) {
        return entity.getCapability(EyelibAttachableData.ANIMATABLE).<AnimatableComponent<T>>cast().resolve().orElse(null);
    }

    private T owner;
    private MolangScope scope;

    @NotNull
    private final ModelComponent modelComponent = new ModelComponent();

    @NotNull
    private final AnimationComponent animationComponent = new AnimationComponent();

    public void sync() {
        if (modelComponent.serializable()) {
            ownerAs(Entity.class).ifPresent(e -> EyelibNetworkManager.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e),
                    new ModelComponentSyncPacket(
                            e.getId(),
                            modelComponent.getSerializableInfo())));
        }

        if (animationComponent.serializable()) {
            ownerAs(Entity.class).ifPresent(e -> EyelibNetworkManager.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e),
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

    @Override
    public int id() {
        return owner.hashCode();
    }

    public void init(T owner) {
        this.owner = owner;
        scope = new MolangScope();
        scope.setOwner(this);
    }
}
