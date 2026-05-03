package io.github.tt432.eyelibmolang;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmolang.compiler.*;
import io.github.tt432.eyelibmolang.compiler.cache.MolangCompileCache;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
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

    private static final MolangCompileCache compileCache = new MolangCompileCache(
            MolangMappingTree.INSTANCE,
            resolveCacheDirectory()
    );

    private static Path resolveCacheDirectory() {
        String prop = System.getProperty("eyelib.molang.cache.dir");
        if (prop != null && !prop.isEmpty()) {
            return Path.of(prop);
        }
        return Path.of(".cache", "eyelib", "compile");
    }

    @FunctionalInterface
    public interface MolangFunction {
        MolangFunction NULL = s -> MolangNull.INSTANCE;

        MolangObject apply(MolangScope scope);
    }

    public static class ConstMolangFunction implements MolangFunction {
        private final MolangObject molangObject;

        public ConstMolangFunction(MolangObject molangObject) {
            this.molangObject = molangObject;
        }

        @Override
        public MolangObject apply(MolangScope scope) {
            return molangObject;
        }
    }

    public MolangValue(String context, MolangCompiledFunction method) {
        this(context, wrap(method));
    }

    public MolangValue(String context) {
        this(context, resolveFunction(context));
    }

    public static MolangValue constant(String context, MolangObject molangObject) {
        return new MolangValue(context, new ConstMolangFunction(molangObject));
    }

    private static MolangFunction wrap(MolangCompiledFunction method) {
        return method::apply;
    }

    /**
     * Resolves a Molang expression string to an evaluable function.
     * <p>
     * Pipeline:
     * 1. Try constant folding via {@link MolangConstantExpressionEvaluator#tryEvaluate}
     * 2. Compile via {@link MolangCompilerImpl}
     * 3. Cache the compiled result via {@link MolangCompileCache}
     */
    private static MolangFunction resolveFunction(String context) {
        return MolangConstantExpressionEvaluator.tryEvaluate(context)
                                                .<MolangFunction>map(ConstMolangFunction::new)
                                                .orElseGet(() -> {
                                                    CompiledMolangExpression compiled = compileCache.getOrCompile(
                                                            context,
                                                            () -> new MolangCompilerImpl().compile(context, CompileContext.defaults())
                                                    );
                                                    return wrap(compiled);
                                                });
    }

    public static final float TRUE = 1;
    public static final float FALSE = 0;

    private static final Float2ObjectOpenHashMap<MolangValue> MOLANG_VALUE_CONSTANT_POOL = new Float2ObjectOpenHashMap<>();

    public static final MolangValue ONE = getConstant(TRUE);
    public static final MolangValue ZERO = getConstant(FALSE);
    public static final MolangValue TRUE_VALUE = ONE;
    public static final MolangValue FALSE_VALUE = ZERO;

    public static MolangValue getConstant(float value) {
        return MOLANG_VALUE_CONSTANT_POOL.computeIfAbsent(value, k -> constant(String.valueOf(k), MolangFloat.valueOf(k)));
    }

    /**
     * Returns the size of the constant pool for telemetry.
     */
    public static int getConstantPoolSize() {
        return MOLANG_VALUE_CONSTANT_POOL.size();
    }

    public static final Codec<MolangValue> CODEC = MolangCodecs.singleOrListStrings()
                                                               .xmap(parts -> new MolangValue(String.join("", parts)), value -> List.of(value.toString()));

    public MolangObject getObject(MolangScope scope) {
        try {
            return Objects.requireNonNullElse(method.apply(scope), MolangNull.INSTANCE);
        } catch (Throwable e) {
            log.error("molang: {}", context, e);
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

