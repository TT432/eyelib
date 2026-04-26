package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.common.MolangRootAliasCanonicalizer;
import io.github.tt432.eyelibmolang.generated.MolangBaseVisitor;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MolangExpressionAnalysisVisitor extends MolangBaseVisitor<MolangExpressionAnalysis> {
    private final MolangExpressionEnvironment environment;

    MolangExpressionAnalysisVisitor(MolangExpressionEnvironment environment) {
        this.environment = environment;
    }

    @Override
    protected MolangExpressionAnalysis defaultResult() {
        return MolangExpressionAnalysis.dynamic("unsupported_node");
    }

    @Override
    public MolangExpressionAnalysis visitBase(MolangParser.BaseContext ctx) {
        MolangExpressionAnalysis result = MolangExpressionAnalysis.constant();
        for (int i = 0; i < ctx.children.size(); i++) {
            result = combineSequential(result, visit(ctx.children.get(i)));
        }
        return result;
    }

    @Override
    public MolangExpressionAnalysis visitOneExpr(MolangParser.OneExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public MolangExpressionAnalysis visitAccessArray(MolangParser.AccessArrayContext ctx) {
        MolangExpressionAnalysis values = visit(ctx.values());
        MolangExpressionAnalysis index = visit(ctx.expr());
        return combine(values, index).withCompileTimeEvaluable(false).addBlocker("array_access_not_folded");
    }

    @Override
    public MolangExpressionAnalysis visitThis(MolangParser.ThisContext ctx) {
        return MolangExpressionAnalysis.dynamic("this_not_foldable");
    }

    @Override
    public MolangExpressionAnalysis visitScopedExprSet(MolangParser.ScopedExprSetContext ctx) {
        return visit(ctx.exprSet());
    }

    @Override
    public MolangExpressionAnalysis visitTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)), visit(ctx.expr(2)));
    }

    @Override
    public MolangExpressionAnalysis visitBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public MolangExpressionAnalysis visitSignedAtom(MolangParser.SignedAtomContext ctx) {
        return visit(ctx.atom());
    }

    @Override
    public MolangExpressionAnalysis visitComparisonOperator(MolangParser.ComparisonOperatorContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public MolangExpressionAnalysis visitAndOperator(MolangParser.AndOperatorContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public MolangExpressionAnalysis visitOrOperator(MolangParser.OrOperatorContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public MolangExpressionAnalysis visitNullCoalescing(MolangParser.NullCoalescingContext ctx) {
        return combine(visit(ctx.values()), visit(ctx.expr()));
    }

    @Override
    public MolangExpressionAnalysis visitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx) {
        return combine(visit(ctx.expr()), MolangExpressionAnalysis.impure("assignment"));
    }

    @Override
    public MolangExpressionAnalysis visitMulOrDiv(MolangParser.MulOrDivContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public MolangExpressionAnalysis visitAddOrSub(MolangParser.AddOrSubContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public MolangExpressionAnalysis visitNeExpr(MolangParser.NeExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public MolangExpressionAnalysis visitEqualsOperator(MolangParser.EqualsOperatorContext ctx) {
        return combine(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public MolangExpressionAnalysis visitFunction(MolangParser.FunctionContext ctx) {
        String methodName = rename(ctx.ID().getText());
        MolangMappingTree.MethodData methodData = MolangMappingTree.INSTANCE.findMethod(methodName);
        if (methodData == null) {
            return MolangExpressionAnalysis.dynamic("unknown_function:" + methodName);
        }

        MolangMappingTree.FunctionInfo functionInfo = pickFunction(methodData.functionInfos(), ctx.expr().size());
        if (functionInfo == null) {
            return MolangExpressionAnalysis.dynamic("unmatched_function_overload:" + methodName);
        }

        MolangExpressionAnalysis combined = foldExpressions(ctx.expr());
        FunctionTraits traits = functionTraits(functionInfo);

        boolean compileTimeEvaluable = combined.compileTimeEvaluable()
                && functionInfo.molangClass().pureFunction()
                && traits.deterministic();
        boolean runtimeEnumerable = combined.runtimeEnumerable() && traits.runtimeEnumerable();
        boolean sideEffectFree = combined.sideEffectFree() && traits.sideEffectFree();

        ArrayList<String> blockers = new ArrayList<>(combined.blockers());
        if (!traits.deterministic()) {
            blockers.add("non_deterministic_function:" + methodName);
        }
        if (!traits.runtimeEnumerable()) {
            blockers.add("non_enumerable_function:" + methodName);
        }
        if (!traits.sideEffectFree()) {
            blockers.add("impure_function:" + methodName);
        }
        if (!functionInfo.molangClass().pureFunction()) {
            blockers.add("scope_function:" + methodName);
        }

        return new MolangExpressionAnalysis(compileTimeEvaluable, runtimeEnumerable, sideEffectFree, blockers);
    }

    @Override
    public MolangExpressionAnalysis visitStringValue(MolangParser.StringValueContext ctx) {
        return MolangExpressionAnalysis.constant();
    }

    @Override
    public MolangExpressionAnalysis visitVariable(MolangParser.VariableContext ctx) {
        String fieldName = rename(ctx.getText());
        if (isScopeVariable(fieldName)) {
            return new MolangExpressionAnalysis(
                    false,
                    environment.isRuntimeEnumerableVariable(fieldName),
                    true,
                    List.of("scope_variable:" + fieldName)
            );
        }

        MolangMappingTree.FieldData field = MolangMappingTree.INSTANCE.findField(fieldName);
        if (field != null) {
            boolean pureField = isPureField(fieldName, field.clazz());
            boolean compileTimeEvaluable = pureField;
            boolean runtimeEnumerable = pureField;
            return new MolangExpressionAnalysis(
                    compileTimeEvaluable,
                    runtimeEnumerable,
                    true,
                    compileTimeEvaluable ? List.of() : List.of("runtime_field:" + fieldName)
            );
        }

        MolangMappingTree.MethodData method = MolangMappingTree.INSTANCE.findMethod(fieldName);
        if (method != null) {
            for (MolangMappingTree.FunctionInfo functionInfo : method.functionInfos()) {
                if (functionInfo.molangClass().pureFunction() && functionInfo.method().getParameterCount() == 0) {
                    return fromFunctionTraits(fieldName, functionInfo);
                }
                if (!functionInfo.molangClass().pureFunction()
                        && functionInfo.method().getParameterCount() == 1
                        && functionInfo.method().getParameterTypes()[0].getName().equals("io.github.tt432.eyelibmolang.MolangScope")) {
                    return fromFunctionTraits(fieldName, functionInfo);
                }
            }
        }

        return MolangExpressionAnalysis.dynamic("unknown_variable:" + fieldName);
    }

    @Override
    public MolangExpressionAnalysis visitNumber(MolangParser.NumberContext ctx) {
        return MolangExpressionAnalysis.constant();
    }

    @Override
    public MolangExpressionAnalysis visitParenthesesPrecedence(MolangParser.ParenthesesPrecedenceContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public MolangExpressionAnalysis visitTerminal(TerminalNode node) {
        return MolangExpressionAnalysis.constant();
    }

    private MolangExpressionAnalysis fromFunctionTraits(String name, MolangMappingTree.FunctionInfo functionInfo) {
        FunctionTraits traits = functionTraits(functionInfo);
        boolean compileTimeEvaluable = functionInfo.molangClass().pureFunction() && traits.deterministic();
        boolean runtimeEnumerable = traits.runtimeEnumerable();
        boolean sideEffectFree = traits.sideEffectFree();

        ArrayList<String> blockers = new ArrayList<>();
        if (!traits.deterministic()) {
            blockers.add("non_deterministic_function:" + name);
        }
        if (!traits.runtimeEnumerable()) {
            blockers.add("non_enumerable_function:" + name);
        }
        if (!traits.sideEffectFree()) {
            blockers.add("impure_function:" + name);
        }
        if (!functionInfo.molangClass().pureFunction()) {
            blockers.add("scope_function:" + name);
        }

        return new MolangExpressionAnalysis(compileTimeEvaluable, runtimeEnumerable, sideEffectFree, blockers);
    }

    private static FunctionTraits functionTraits(MolangMappingTree.FunctionInfo functionInfo) {
        if (!functionInfo.molangClass().pureFunction()) {
            return new FunctionTraits(false, false, false);
        }

        String ownerClassName = functionInfo.molangClass().classInstance().getName();
        String methodName = functionInfo.method().getName();

        if (ownerClassName.equals("io.github.tt432.eyelibmolang.mapping.MolangMath")
                && (methodName.equals("random")
                || methodName.equals("random_integer")
                || methodName.equals("die_roll")
                || methodName.equals("die_roll_integer"))) {
            return new FunctionTraits(false, false, true);
        }

        if (ownerClassName.equals("io.github.tt432.eyelibmolang.mapping.MolangToplevel")
                && methodName.equals("loop")) {
            return new FunctionTraits(false, false, false);
        }

        return new FunctionTraits(true, true, true);
    }

    private static boolean isPureField(String fieldName, Class<?> fieldOwnerClass) {
        int separator = fieldName.indexOf('.');
        String scopeName = separator == -1 ? "" : fieldName.substring(0, separator).toLowerCase(Locale.ROOT);

        for (MolangMappingTree.MolangClass molangClass : MolangMappingTree.INSTANCE.findClasses(scopeName)) {
            if (molangClass.classInstance().equals(fieldOwnerClass)) {
                return molangClass.pureFunction();
            }
        }

        return false;
    }

    private record FunctionTraits(boolean deterministic, boolean runtimeEnumerable, boolean sideEffectFree) {
    }

    private MolangExpressionAnalysis foldExpressions(List<? extends org.antlr.v4.runtime.tree.ParseTree> expressions) {
        MolangExpressionAnalysis result = MolangExpressionAnalysis.constant();
        for (org.antlr.v4.runtime.tree.ParseTree expression : expressions) {
            result = combine(result, visit(expression));
        }
        return result;
    }

    private MolangExpressionAnalysis combine(MolangExpressionAnalysis... analyses) {
        MolangExpressionAnalysis result = MolangExpressionAnalysis.constant();
        for (MolangExpressionAnalysis analysis : analyses) {
            result = combine(result, analysis);
        }
        return result;
    }

    private MolangExpressionAnalysis combineSequential(MolangExpressionAnalysis left, MolangExpressionAnalysis right) {
        return combine(left, right);
    }

    private MolangExpressionAnalysis combine(MolangExpressionAnalysis left, MolangExpressionAnalysis right) {
        ArrayList<String> blockers = new ArrayList<>(left.blockers());
        blockers.addAll(right.blockers());
        return new MolangExpressionAnalysis(
                left.compileTimeEvaluable() && right.compileTimeEvaluable(),
                left.runtimeEnumerable() && right.runtimeEnumerable(),
                left.sideEffectFree() && right.sideEffectFree(),
                blockers
        );
    }

    private static boolean isScopeVariable(String fieldName) {
        return fieldName.startsWith("variable")
                || fieldName.startsWith("array")
                || fieldName.startsWith("texture")
                || fieldName.startsWith("material")
                || fieldName.startsWith("geometry")
                || fieldName.startsWith("temp")
                || fieldName.startsWith("context");
    }

    private static MolangMappingTree.FunctionInfo pickFunction(List<MolangMappingTree.FunctionInfo> functionInfos, int argumentCount) {
        for (MolangMappingTree.FunctionInfo functionInfo : functionInfos) {
            int start = functionInfo.molangClass().pureFunction() ? 0 : 1;
            int parameterCount = functionInfo.method().getParameterCount() - start;
            if (functionInfo.method().isVarArgs()) {
                if (argumentCount >= parameterCount - 1) {
                    return functionInfo;
                }
            } else if (parameterCount == argumentCount) {
                return functionInfo;
            }
        }
        return null;
    }

    private static String rename(String name) {
        int separator = name.indexOf('.');
        if (separator != -1) {
            String canonicalized = MolangRootAliasCanonicalizer.canonicalizeQualifiedNameRoot(name);
            int canonicalSeparator = canonicalized.indexOf('.');
            return (alias(canonicalized.substring(0, canonicalSeparator))
                    + canonicalized.substring(canonicalSeparator)).toLowerCase(Locale.ROOT);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private static String alias(String sourceName) {
        String normalized = sourceName.toLowerCase(Locale.ROOT);
        if ("m".equals(normalized)) {
            return "math";
        }
        return MolangRootAliasCanonicalizer.canonicalizeRoot(sourceName);
    }
}
