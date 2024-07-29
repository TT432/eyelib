package io.github.tt432.eyelib.molang;

import com.mojang.serialization.Codec;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.List;

/**
 * @author TT432
 */
@Slf4j
public record MolangValue(
        @NotNull String context,
        @NotNull MethodHandle method
) {
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
        Codec<String> codec = Codec.withAlternative(Codec.STRING, Codec.FLOAT.xmap(Object::toString, Float::parseFloat));
        CODEC = ChinExtraCodecs.singleOrList(codec)
                .xmap(sl -> String.join("", sl), List::of)
                .xmap(MolangValue::new, MolangValue::toString);
    }

    public float eval(MolangScope scope) {
        try {
            return (float) method.invoke(scope);
        } catch (Throwable e) {
            log.error("Error occurred", e);
        }

        return 0F;
    }

    public boolean evalAsBool(MolangScope scope) {
        return eval(scope) != FALSE;
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
