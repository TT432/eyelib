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
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author TT432
 */
@Getter
public class RenderData<T> {
    @SuppressWarnings("unchecked")
    private static final HostRole<RenderData<?>> RENDER_DATA =
            HostRole.of("render_data", (Class<RenderData<?>>) (Class<?>) RenderData.class);
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
    // ------------------------------------------------------------------
    // bind 骨骼缓存（Opt16）：原 EntityRenderOrchestrator.collectBindBones 每次调用
    // 全量重建 Int2ObjectOpenHashMap（TickStage 每实体每帧一次，JFR ~1.5%+分配）。
    // 内容仅随 modelComponents 变化，故按失效点缓存。
    // 失效契约：modelComponents 的全部变更点（setupClientEntity 的 components.clear()
    // 两处、RenderSyncApplyOps.replaceModelComponents 经 ClientRenderSyncService.apply）
    // 必须调用 invalidateBindBones()。
    // ------------------------------------------------------------------
    private @Nullable Int2ObjectMap<Model.Bone> bindBonesCache;

    /**
     * 实体全部模型组件的 bind 骨骼（按骨骼 id，先组件优先），懒构建缓存。
     * 供 molang `this` 求值与 attachable 骨骼定位使用。
     */
    public Int2ObjectMap<Model.Bone> bindBones() {
        Int2ObjectMap<Model.Bone> result = bindBonesCache;
        if (result == null) {
            result = new Int2ObjectOpenHashMap<>();
            for (ModelComponent mc : modelComponents) {
                var model = mc.getModel();
                if (model == null) continue;
                for (var entry : model.allBones().int2ObjectEntrySet()) {
                    result.putIfAbsent(entry.getIntKey(), entry.getValue());
                }
            }
            bindBonesCache = result;
        }
        return result;
    }

    /** modelComponents 变更后必须调用（见字段注释的失效契约）。 */
    public void invalidateBindBones() {
        bindBonesCache = null;
    }
    /**
     * 实体模型是否需要动画翻转补偿（基岩版 geo 约定）。
     * 返回第一个可用模型组件的约定；无模型组件时默认 true（向后兼容）。
     */
    public boolean flipAnimation() {
        for (ModelComponent mc : modelComponents) {
            var model = mc.getModel();
            if (model != null) return model.flipAnimation();
        }
        return true;
    }

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
     * 只读获取 scope：仅当 owner 匹配且 scope 已初始化时返回，否则 empty。
     * 供调试器等只读消费端使用——owner 守卫收敛在本类（IQF Q-4），调用方不得再自行
     * 比较 {@code getOwner()}；本方法不触发 lazy-init。
     */
    public Optional<MolangScope> scopeIfOwnedBy(Object expectedOwner) {
        MolangScope s = scope;
        return s != null && owner == expectedOwner ? Optional.of(s) : Optional.empty();
    }

    /**
     * 确保 owner 已绑定到当前对象；未绑定或绑定了别的对象时重新初始化。
     * 这是 lazy-init 守卫的唯一合法位置（IQF 判据 Q-4），调用方不应再自行做 getOwner 比较。
     */
    @SuppressWarnings("unchecked")
    public void ensureOwner(Object owner) {
        T currentOwner = getOwner();
        if (currentOwner != owner) {
            init((T) owner);
        }
    }

    @SuppressWarnings("unchecked")
    public void init(T owner) {
        this.owner = owner;
        // 渲染链单线程访问（render 线程 lazy-init 与求值，见 MolangDebugService 文档）：
        // 用单线程 scope 消除 4 个并发结构的每求值开销（JFR：CHM 遍历/清理占渲染线程显著份额）
        scope = MolangScope.singleThreaded();
        scope.getHostContext().put(RENDER_DATA, this);
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