package io.github.tt432.eyelib.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;

import java.lang.invoke.MethodHandle;

/**
 * @author TT432
 */
public final class MolangValue {
    public static final float TRUE = 1;
    public static final float FALSE = 0;

    public static final MolangValue TRUE_VALUE = new MolangValue("1");
    public static final MolangValue FALSE_VALUE = new MolangValue("0");

    public static final Codec<MolangValue> CODEC = Codec.either(
                    Codec.STRING,
                    RecordCodecBuilder.<MolangValue>create(ins -> ins.group(
                            Codec.STRING.fieldOf("context").forGetter(o -> o.context)
                    ).apply(ins, MolangValue::new)))
            .xmap(e -> e.left().map(MolangValue::new).orElseGet(() -> e.right().get()),
                    m -> Either.left(m.context));

    @Getter
    private final String context;
    @Setter
    private MethodHandle method;

    public MolangValue(String context) {
        this.context = context;
        MolangCompileHandler.register(this);
    }

    public static MolangValue parse(String content) {
        return parse(new JsonPrimitive(content));
    }

    public static MolangValue parse(JsonElement json) {
        return parse(json, FALSE_VALUE);
    }

    public static MolangValue parse(JsonElement json, MolangValue defaultValue) {
        return CODEC.parse(JsonOps.INSTANCE, json)
                .map(m -> m == null ? defaultValue : m)
                .getOrThrow(true, RuntimeException::new);
    }

    public float eval(MolangScope scope) {
        if (method != null) {
            try {
                return (float) method.invoke(scope);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return 0F;
    }

    public boolean evalAsBool(MolangScope scope) {
        return eval(scope) != FALSE;
    }
}
