package io.github.tt432.eyelib.capability.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

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

        public static final StreamCodec<SerializableInfo> STREAM_CODEC = new StreamCodec<>() {
            private final StreamCodec<Map<String, String>> animationsCodec = EyelibStreamCodecs.map(EyelibStreamCodecs.STRING, EyelibStreamCodecs.STRING);
            private final StreamCodec<Map<String, MolangValue>> animateCodec = EyelibStreamCodecs.map(EyelibStreamCodecs.STRING, MolangValue.STREAM_CODEC);

            @Override
            public void encode(SerializableInfo obj, FriendlyByteBuf buf) {
                animationsCodec.encode(obj.animations, buf);
                animateCodec.encode(obj.animate, buf);
            }

            @Override
            public SerializableInfo decode(FriendlyByteBuf buf) {
                var animations = animationsCodec.decode(buf);
                var animate = animateCodec.decode(buf);
                return new SerializableInfo(animations, animate);
            }
        };
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

    public void setInfo(SerializableInfo info) {
        setup(info.animations, info.animate);
    }

    {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ManagerEntryChangedEvent.class, e -> {
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
