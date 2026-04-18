package io.github.tt432.eyelibmolang;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmolang.compiler.MolangCompileHandler;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author TT432
 */
public record MolangValue(
        String context,
        MolangFunction method
) {
    private static final Logger log = LoggerFactory.getLogger(MolangValue.class);

    @FunctionalInterface
    public interface MolangFunction {
        MolangFunction NULL = s -> MolangNull.INSTANCE;

        MolangObject apply(MolangScope scope);
    }

    @AllArgsConstructor
    public static class ConstMolangFunction implements MolangFunction {
        MolangObject molangObject;

        @Override
        public MolangObject apply(MolangScope scope) {
            return molangObject;
        }
    }

    public MolangValue(String context, MolangCompiledFunction method) {
        this(context, wrap(method));
    }

    public MolangValue(String context) {
        this(context, wrap(MolangCompileHandler.compile(context)));
    }

    private static MolangFunction wrap(MolangCompiledFunction method) {
        return method::apply;
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

    public static final Codec<MolangValue> CODEC = MolangCodecs.singleOrListStrings()
            .xmap(parts -> new MolangValue(String.join("", parts)), value -> List.of(value.toString()));

    public MolangObject getObject(MolangScope scope) {
        try {
            return Objects.requireNonNullElse(method.apply(scope), MolangNull.INSTANCE);
        } catch (Throwable e) {
            log.error("molang: {}", context, e);
            String name = method.getClass().getSimpleName();
            var classInfo = MolangCompileHandler.cache.getClassInfoByClassName(name);
            if (classInfo != null && classInfo.bytecode() != null) {
                MolangCompileHandler.exportClass(name, classInfo.bytecode());
            }

            return MolangNull.INSTANCE;
        }
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

