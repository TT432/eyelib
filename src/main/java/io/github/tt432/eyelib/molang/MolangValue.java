package io.github.tt432.eyelib.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

    public static final Codec<MolangValue> CODEC = Codec.withAlternative(
            Codec.withAlternative(
                            Codec.withAlternative(
                                    Codec.STRING,
                                    Codec.FLOAT.xmap(Object::toString, Float::parseFloat)),
                            Codec.withAlternative(
                                    Codec.STRING,
                                    Codec.FLOAT.xmap(Object::toString, Float::parseFloat)
                            ).listOf().xmap(sl -> String.join("", sl), List::of))
                    .xmap(MolangValue::new, MolangValue::toString),
            RecordCodecBuilder.<MolangValue>create(ins -> ins.group(
                    Codec.STRING.fieldOf("context").forGetter(o -> o.context)
            ).apply(ins, MolangValue::new))
    );

    public static MolangValue parse(String content) {
        return parse(new JsonPrimitive(content));
    }

    public static MolangValue parse(JsonElement json) {
        if (json == null) return FALSE_VALUE;
        return parse(json, FALSE_VALUE);
    }

    public static MolangValue parse(JsonElement json, MolangValue defaultValue) {
        return CODEC.parse(JsonOps.INSTANCE, json).result().orElse(defaultValue);
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
