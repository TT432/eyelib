package io.github.tt432.eyelib.capability.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author TT432
 */
@Getter
public class AnimationComponent {
    @ParametersAreNonnullByDefault
    public record SerializableInfo(
            Map<String, String> animations,
            Map<String, MolangValue> animate
    ) {
        public static final Codec<SerializableInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("animations").forGetter(SerializableInfo::animations),
                Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).fieldOf("animate").forGetter(SerializableInfo::animate)
        ).apply(ins, SerializableInfo::new));

        public static final StreamCodec<ByteBuf, SerializableInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
                SerializableInfo::animations,
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, MolangValue.STREAM_CODEC),
                SerializableInfo::animate,
                SerializableInfo::new
        );
    }

    public Object getAnimationData(String controllerName) {
        return animationData.computeIfAbsent(controllerName,
                s -> Eyelib.getAnimationManager().get(s).createData());
    }

    public boolean serializable() {
        return serializableInfo != null;
    }

    @Nullable
    SerializableInfo serializableInfo;
    private final Map<Animation<?>, MolangValue> animate = new HashMap<>();
    private final Map<String, Object> animationData = new HashMap<>();

    public BoneRenderInfos tickedInfos;
    public AnimationEffects effects;

    public void setInfo(SerializableInfo info) {
        setup(info.animations, info.animate);
    }

    {
        NeoForge.EVENT_BUS.addListener(ManagerEntryChangedEvent.class, e -> {
            if (e.getManagerName().equals(Eyelib.getAnimationManager().getManagerName())) {
                AtomicBoolean changed = new AtomicBoolean(false);
                animate.forEach((k, v) -> {
                    if (k.name().equals(e.getEntryName())) {
                        changed.set(true);
                    }
                });
                if (changed.get()) {
                    serializableInfo = null;
                }
            }
        });
    }

    public void setup(Map<String, String> animations, Map<String, MolangValue> animate) {
        if (serializableInfo != null
                && serializableInfo.animate.equals(animate)
                && serializableInfo.animations.equals(animations)) return;

        serializableInfo = new SerializableInfo(animations, animate);

        this.animate.clear();
        animationData.clear();

        animate.forEach((s, v) ->
                this.animate.put(Eyelib.getAnimationManager().get(animations.get(s)), v));

        new HashMap<>();
        for (var s : this.animate.keySet()) {
            if (s == null) continue;
            var data = s.createData();
            animationData.put(s.name(), data);
        }
    }
}
