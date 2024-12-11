package io.github.tt432.eyelib.molang;

import com.mojang.serialization.Codec;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * @author TT432
 */
@Slf4j
public record MolangValue(
        @NotNull String context,
        @NotNull MolangFunction method
) {
    @FunctionalInterface
    public interface MolangFunction {
        MolangFunction NULL = s -> MolangNull.INSTANCE;

        MolangObject apply(@NotNull MolangScope scope);
    }

    public MolangValue(@NotNull String context) {
        this(context, MolangCompileHandler.compile(context));
    }

    public static final float TRUE = 1;
    public static final float FALSE = 0;

    private static final Float2ObjectOpenHashMap<MolangValue> MOLANG_VALUE_CONSTANT_POOL = new Float2ObjectOpenHashMap<>();

    public static final MolangValue ONE = getConstant(TRUE);
    public static final MolangValue ZERO = getConstant(FALSE);
    public static final MolangValue TRUE_VALUE = ONE;
    public static final MolangValue FALSE_VALUE = ZERO;

    public static MolangValue getConstant(float value) {
        return MOLANG_VALUE_CONSTANT_POOL.computeIfAbsent(value, k -> new MolangValue(String.valueOf(k)));
    }

    public static final Codec<MolangValue> CODEC;

    static {
        Codec<String> codec = Codec.withAlternative(
                Codec.withAlternative(Codec.STRING, Codec.FLOAT.xmap(Object::toString, Float::parseFloat)),
                Codec.BOOL.xmap(b -> b ? "1" : "0", s -> s.equals("1"))
        );
        CODEC = ChinExtraCodecs.singleOrList(codec)
                .xmap(sl -> String.join("", sl), List::of)
                .xmap(MolangValue::new, MolangValue::toString);
    }

    public static final StreamCodec<ByteBuf, MolangValue> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public MolangObject getObject(MolangScope scope) {
        return Objects.requireNonNullElse(method.apply(scope), MolangNull.INSTANCE);
    }

    public float eval(MolangScope scope) {
        return getObject(scope).asFloat();
    }

    public boolean evalAsBool(MolangScope scope) {
        return getObject(scope).asBoolean();
    }

    @Override
    public String toString() {
        return context;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof MolangValue mv && mv.context.equals(context));
    }
}
