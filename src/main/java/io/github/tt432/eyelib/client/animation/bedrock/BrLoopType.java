package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.util.codec.Codecs;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TT432
 */
@Slf4j
public enum BrLoopType {
    HOLD_ON_LAST_FRAME,
    LOOP,
    ONCE;

    public static final Codec<BrLoopType> CODEC = Codec.either(
                    Codec.STRING.xmap(
                            s -> switch (s) {
                                case "hold_on_last_frame" -> HOLD_ON_LAST_FRAME;
                                case "true" -> LOOP;
                                default -> ONCE;
                            },
                            t -> switch (t) {
                                case LOOP -> "true";
                                case HOLD_ON_LAST_FRAME -> "hold_on_last_frame";
                                default -> "false";
                            }),
                    Codec.BOOL.xmap(b -> b ? LOOP : ONCE, t -> t == LOOP))
            .xmap(Codecs::unwrap, Either::left)
            .orElse(ONCE);

    public static BrLoopType parse(JsonElement json) {
        return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(true, log::error);
    }
}
