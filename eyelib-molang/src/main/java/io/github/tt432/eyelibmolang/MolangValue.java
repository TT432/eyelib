package io.github.tt432.eyelibmolang;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmolang.compiler.*;
import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.cache.MolangCompileCache;
import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
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
import java.util.Set;

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
    private static final MolangBinder binder = new MolangBinder();

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
     * 2. Parse → Bind → Compile via {@link MolangCompilerImpl}, falling back to
     * {@link BoundMolangEvaluator} for unsupported expression types
     * 3. Cache the compiled result via {@link MolangCompileCache}
     */
    private static MolangFunction resolveFunction(String context) {
        return MolangConstantExpressionEvaluator.tryEvaluate(context)
                                                .<MolangFunction>map(ConstMolangFunction::new)
                                                .orElseGet(() -> {
                                                    CompiledMolangExpression compiled = compileCache.getOrCompile(context, () -> {
                                                        try {
                                                            return new MolangCompilerImpl().compile(context, CompileContext.defaults());
                                                        } catch (ExpressionCompileException compileEx) {
                                                            // Fall back to AST evaluator for expressions the bytecode
                                                            // emitter doesn't handle yet (variables, queries, calls, etc.)
                                                            return createEvaluator(context);
                                                        } catch (Throwable t) {
                                                            log.error("Unexpected molang compile error for: {}", context, t);
                                                            return createEvaluatorFallback(context);
                                                        }
                                                    });
                                                    return wrap(compiled);
                                                });
    }

    /**
     * Creates a {@link BoundMolangEvaluator} by parsing and binding the expression.
     */
    private static CompiledMolangExpression createEvaluator(String context) {
        var astOpt = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(context);
        if (astOpt.isEmpty()) {
            return NULL_COMPILED;
        }
        BindResult bindResult = binder.bind(astOpt.get());
        return new BoundMolangEvaluator(
                context,
                bindResult.root(),
                CompileContext.defaults().mappingTree()
        );
    }

    /**
     * Last-resort fallback when even parser/binder fail.
     */
    private static CompiledMolangExpression createEvaluatorFallback(String context) {
        try {
            return createEvaluator(context);
        } catch (Throwable t) {
            log.error("Failed to create evaluator for: {}", context, t);
            return NULL_COMPILED;
        }
    }

    /** A no-op CompiledMolangExpression that always returns MolangNull. */
    private static final CompiledMolangExpression NULL_COMPILED = new CompiledMolangExpression() {
        @Override
        public MolangObject evaluate(MolangScope scope) {
            return MolangNull.INSTANCE;
        }

        @Override
        public String sourceExpression() {
            return "";
        }

        @Override
        public Set<String> requiredHostRoles() {
            return Set.of();
        }
    };

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

