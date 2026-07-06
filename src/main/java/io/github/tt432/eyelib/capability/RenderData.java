package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.component.ClientEntityComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.render.sync.ClientRenderSyncService;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationComponentInfo;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.molang.MolangScope;
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
                ModelComponentInfo.CODEC.listOf()
                                        .optionalFieldOf("model")
                                        .forGetter(ac -> Optional.of(ac.modelComponents.stream()
                                                                                       .map(ModelComponent::getSerializableInfo)
                                                                                       .toList())),
                AnimationComponentInfo.CODEC.optionalFieldOf("animation")
                                            .forGetter(ac -> Optional.ofNullable(ac.animationComponent.getSerializableInfo()))
        ).apply(ins, (mcsi, acsi) -> {
            RenderData<T> result = new RenderData<>();
            mcsi.ifPresent(l -> {
                for (ModelComponentInfo serializableInfo : l) {
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
    public static <T> RenderData<T> getComponent(Entity entity) {
        return (RenderData<T>) DataAttachmentHelper.getOrCreate(AttachableDataTypes.RENDER_DATA.get(), entity);
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

    /**
     * 确保 owner 已绑定到当前对象；未绑定或绑定了别的对象时重新初始化。
     * 这是 lazy-init 守卫的唯一合法位置（IQF 判据 Q-4），调用方不应再自行做 getOwner 比较。
     */
    @SuppressWarnings("unchecked")
    public void ensureOwner(Object owner) {
        if (getOwner() != owner) {
            init((T) owner);
        }
    }

    @SuppressWarnings("unchecked")
    public void init(T owner) {
        this.owner = owner;
        scope = new MolangScope();
        scope.getHostContext().put(RenderData.class, this);
        if (owner != null) {
            scope.getHostContext().put((Class<T>) owner.getClass(), owner);
        }

        scope.set("variable.scale", 1);
    }

    /**
     * 返回已初始化的 scope；未 init 时抛 IllegalStateException。
     * 用于必须保证 scope 可用的调用点（如 AttachableResolver 评估 item molang 条件）。
     */
    public MolangScope requireScope() {
        MolangScope s = scope;
        if (s == null) {
            throw new IllegalStateException("RenderData scope not initialized; call ensureOwner first");
        }
        return s;
    }
}