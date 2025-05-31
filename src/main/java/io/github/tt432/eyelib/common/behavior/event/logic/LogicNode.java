package io.github.tt432.eyelib.common.behavior.event.logic;

import com.mojang.serialization.MapCodec;
import io.github.tt432.eyelib.capability.EntityBehaviorData;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

import java.util.Map;

import static io.github.tt432.eyelib.util.codec.EyelibCodec.list;

/**
 * @author TT432
 */
public interface LogicNode {
    MapCodec<LogicNode> CODEC = list(() -> Map.of(
            "add", new EyelibCodec.CodecInfo<>(Add.class, Add.CODEC),
            "randomize", new EyelibCodec.CodecInfo<>(Randomize.class, Randomize.CODEC),
            "sequence", new EyelibCodec.CodecInfo<>(Sequence.class, Sequence.CODEC),
            "remove", new EyelibCodec.CodecInfo<>(Remove.class, Remove.CODEC)
    ));

    void eval(EntityBehaviorData data);
}
