package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public record BrAnimation(
        Map<String, BrAnimationEntry> animations
) {
    public static final Codec<BrAnimation> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.dispatchedMap(Codec.STRING, BrAnimationEntry::codec).fieldOf("animations").forGetter(o -> o.animations)
    ).apply(ins, BrAnimation::new));
}
