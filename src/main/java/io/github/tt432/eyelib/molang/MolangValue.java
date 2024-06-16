package io.github.tt432.eyelib.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.Codecs;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.Function;

/**
 * @author TT432
 */
@Slf4j
public final class MolangValue {
    public static final float TRUE = 1;
    public static final float FALSE = 0;

    public static final MolangValue TRUE_VALUE = new MolangValue("1");
    public static final MolangValue FALSE_VALUE = new MolangValue("0");

    public static final Codec<MolangValue> CODEC = Codec.either(
            Codec.either(
                    Codec.either(Codec.STRING, Codec.FLOAT).listOf()
                            .xmap(el -> el.stream().map(e -> e.map(Function.identity(), Object::toString))
                                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                                    .toString(), s -> List.of(Either.left(s)))
                            .xmap(MolangValue::new, MolangValue::toString),
                    Codec.either(Codec.STRING, Codec.FLOAT)
                            .xmap(e -> e.map(Function.identity(), Object::toString), Either::left)
                            .xmap(MolangValue::new, MolangValue::toString)
            ).xmap(Codecs::unwrap, Either::right),
            RecordCodecBuilder.<MolangValue>create(ins -> ins.group(
                    Codec.STRING.fieldOf("context").forGetter(o -> o.context)
            ).apply(ins, MolangValue::new))
    ).xmap(Codecs::unwrap, Either::left);

    @Getter
    @NotNull
    private final String context;
    @NotNull
    private final MethodHandle method;

    public MolangValue(@NotNull String context) {
        this.context = context;
        method = MolangCompileHandler.compile(this);
    }

    public static MolangValue parse(String content) {
        return parse(new JsonPrimitive(content));
    }

    public static MolangValue parse(JsonElement json) {
        if (json==null) return FALSE_VALUE;
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
}
