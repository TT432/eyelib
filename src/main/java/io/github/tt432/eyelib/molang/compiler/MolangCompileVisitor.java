package io.github.tt432.eyelib.molang.compiler;


import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.extras.constant.ConstantUtils;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.grammer.MolangBaseVisitor;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangString;
import io.github.tt432.eyelib.util.EyelibUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static io.github.tt432.eyelib.molang.compiler.MolangClassDescs.*;
import static java.lang.constant.ConstantDescs.*;

/**
 * @author TT432
 */
@Slf4j
public class MolangCompileVisitor extends MolangBaseVisitor<MolangCompileVisitor.CompileContext> {
    @Getter
    @Setter
    private CodeBuilder codeBuilder;

    public void startVisitor(CodeBuilder methodVisitor) {
        this.codeBuilder = methodVisitor;
    }

    public enum CompileContext {
        UNKNOWN,
        STRING,
        FLOAT;
    }

    private static String alias(String sourceName) {
        return switch (sourceName.toLowerCase(Locale.ROOT)) {
            case "c" -> "context";
            case "m" -> "math";
            case "t" -> "temp";
            case "q" -> "query";
            case "v" -> "variable";
            default -> sourceName;
        };
    }

    private static String rename(String name) {
        int i = name.indexOf(".");

        if (i != -1) {
            return (alias(name.substring(0, i)) + name.substring(i)).toLowerCase(Locale.ROOT);
        }

        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public CompileContext visitBase(MolangParser.BaseContext ctx) {
        int size = ctx.children.size();
        int last = size - 1;

        for (int i = 0; i < last; i++) {
            visit(ctx.children.get(i));
        }

        return visit(ctx.children.get(last));
    }

    @Override
    public CompileContext visitOneExpr(MolangParser.OneExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public CompileContext visitAccessArray(MolangParser.AccessArrayContext ctx) {
        visit(ctx.values());
        visit(ctx.expr());
        asFloat();
        codeBuilder.invokestatic(ClassDesc.of(EyelibUtils.class.getName()), "get",
                MethodTypeDesc.of(CD_MolangObject, CD_MolangObject, CD_float));
        return CompileContext.UNKNOWN;
    }

    @Override
    public CompileContext visitThis(MolangParser.ThisContext ctx) {
        // todo
//        return valueOf("0F");
        floatZero();
        return CompileContext.FLOAT;
    }

    @Override
    public CompileContext visitScopedExprSet(MolangParser.ScopedExprSetContext ctx) {
        return visit(ctx.exprSet());
    }

    @Override
    public CompileContext visitTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx) {
        visit(ctx.expr(0));
        asBoolean();
        Label label1 = codeBuilder.newLabel();
        Label label2 = codeBuilder.newLabel();
        codeBuilder.ifeq(label1);
        visit(ctx.expr(1));
        codeBuilder
                .goto_(label2)
                .labelBinding(label1);
        var result = visit(ctx.expr(2));
        codeBuilder.labelBinding(label2);

        return result;
    }

    @Override
    public CompileContext visitBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx) {
        visit(ctx.expr(0));
        asBoolean();
        Label label1 = codeBuilder.newLabel();
        Label label2 = codeBuilder.newLabel();
        codeBuilder.ifeq(label1);
        var result = visit(ctx.expr(1));
        codeBuilder
                .goto_(label2)
                .labelBinding(label1);
        floatZero();
        codeBuilder.labelBinding(label2);

        return result;
    }

    void asFloat() {
        codeBuilder.invokeinterface(CD_MolangObject, "asFloat", MethodTypeDesc.of(CD_float));
    }

    void asString() {
        codeBuilder.invokeinterface(CD_MolangObject, "asString", MethodTypeDesc.of(CD_String));
    }

    void asBoolean() {
        codeBuilder.invokeinterface(CD_MolangObject, "asBoolean", MethodTypeDesc.of(CD_boolean));
    }

    void floatZero() {
        codeBuilder.getstatic(ClassDesc.of(MolangFloat.class.getName()), "ZERO", ClassDesc.of(MolangFloat.class.getName()));
    }

    @Override
    public CompileContext visitSignedAtom(MolangParser.SignedAtomContext ctx) {
        if (ctx.op != null && ctx.op.getText().charAt(0) == '-') {
            visit(ctx.atom());
            asFloat();
            codeBuilder.fneg();
            return newMolangFloat();
        } else {
            return visit(ctx.atom());
        }
    }

    @Override
    public CompileContext visitComparisonOperator(MolangParser.ComparisonOperatorContext ctx) {
        visit(ctx.expr(0));
        asFloat();
        visit(ctx.expr(1));
        asFloat();

        Label label1 = codeBuilder.newLabel();
        Label label2 = codeBuilder.newLabel();

        (switch (ctx.op.getText()) {
            case ">=" -> codeBuilder.fcmpl().iflt(label1);
            case ">" -> codeBuilder.fcmpl().ifle(label1);
            case "<" -> codeBuilder.fcmpg().ifge(label1);
            case "<=" -> codeBuilder.fcmpg().ifgt(label1);
            default -> codeBuilder;
        }).fconst_1()
                .goto_(label2)
                .labelBinding(label1)
                .fconst_0()
                .labelBinding(label2);

        return newMolangFloat();
    }

    @Override
    public CompileContext visitAndOperator(MolangParser.AndOperatorContext ctx) {
        Label label1 = codeBuilder.newLabel();
        Label label2 = codeBuilder.newLabel();
        List<MolangParser.ExprContext> expr = ctx.expr();
        visit(expr.get(0));
        asBoolean();
        codeBuilder.ifeq(label1);
        visit(expr.get(1));
        asBoolean();
        codeBuilder.ifeq(label1)
                .fconst_1()
                .goto_(label2)
                .labelBinding(label1)
                .fconst_0()
                .labelBinding(label2);
        return newMolangFloat();
    }

    @Override
    public CompileContext visitOrOperator(MolangParser.OrOperatorContext ctx) {
        Label label1 = codeBuilder.newLabel();
        Label label2 = codeBuilder.newLabel();
        Label label3 = codeBuilder.newLabel();
        List<MolangParser.ExprContext> expr = ctx.expr();
        visit(expr.get(0));
        asBoolean();
        codeBuilder.ifne(label3);
        visit(expr.get(1));
        asBoolean();
        codeBuilder.ifeq(label1)
                .labelBinding(label3)
                .fconst_1()
                .goto_(label2)
                .labelBinding(label1)
                .fconst_0()
                .labelBinding(label2);
        return newMolangFloat();
    }

    @Override
    public CompileContext visitNullCoalescing(MolangParser.NullCoalescingContext ctx) {
        visit(ctx.values());
        codeBuilder.dup();
        Label label1 = codeBuilder.newLabel();
        codeBuilder.getstatic(ClassDesc.of(MolangNull.class.getName()), "INSTANCE", ClassDesc.of(MolangNull.class.getName()))
                .if_acmpne(label1)
                .pop();
        var result = visit(ctx.expr());
        codeBuilder.labelBinding(label1);

        return result;
    }

    @Override
    public CompileContext visitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx) {
        molangScope();
        codeBuilder.ldc(rename(ctx.ID().getText()));
        var result = visit(ctx.expr());
        codeBuilder.invokevirtual(CD_MolangScope, "set", MethodTypeDesc.of(CD_MolangObject, CD_String, CD_MolangObject));
        return result;
    }

    @Override
    public CompileContext visitMulOrDiv(MolangParser.MulOrDivContext ctx) {
        visit(ctx.expr(0));
        asFloat();
        visit(ctx.expr(1));
        asFloat();

        if (ctx.op.getText().charAt(0) == '*') {
            codeBuilder.fmul();
        } else {
            codeBuilder.fdiv();
        }

        return newMolangFloat();
    }

    @Override
    public CompileContext visitAddOrSub(MolangParser.AddOrSubContext ctx) {
        visit(ctx.expr(0));
        asFloat();
        visit(ctx.expr(1));
        asFloat();

        if (ctx.op.getText().charAt(0) == '-') {
            codeBuilder.fsub();
        } else {
            codeBuilder.fadd();
        }

        return newMolangFloat();
    }

    @Override
    public CompileContext visitNeExpr(MolangParser.NeExprContext ctx) {
        Label label1 = codeBuilder.newLabel();
        Label label2 = codeBuilder.newLabel();
        visit(ctx.expr());
        asBoolean();
        codeBuilder
                .ifeq(label1)
                .fconst_0()
                .goto_(label2)
                .labelBinding(label1)
                .fconst_1()
                .labelBinding(label2);
        return newMolangFloat();
    }

    @Override
    public CompileContext visitEqualsOperator(MolangParser.EqualsOperatorContext ctx) {
        visit(ctx.expr(0));
        visit(ctx.expr(1));

        codeBuilder.invokeinterface(CD_MolangObject, ctx.op.getText().equals("==") ? "equalsF" : "nEqualsF",
                MethodTypeDesc.of(CD_float, CD_MolangObject));

        return newMolangFloat();
    }

    void boxBoolean() {
        codeBuilder.invokestatic(CD_Boolean, "valueOf", MethodTypeDesc.of(CD_Boolean, CD_boolean));
    }

    void molangScope() {
        codeBuilder.loadLocal(TypeKind.REFERENCE, codeBuilder.parameterSlot(0));
    }

    public static final ClassDesc CD_MolangString = ConstantUtils.classDesc(MolangString.class);
    public static final ClassDesc CD_MolangFloat = ConstantUtils.classDesc(MolangFloat.class);

    CompileContext newMolangString() {
        codeBuilder.invokestatic(CD_MolangString, "valueOf", MethodTypeDesc.of(CD_MolangString, CD_String));
        return CompileContext.STRING;
    }

    CompileContext newMolangFloat() {
        codeBuilder.invokestatic(CD_MolangFloat, "valueOf", MethodTypeDesc.of(CD_MolangFloat, CD_float));
        return CompileContext.FLOAT;
    }

    @Override
    public CompileContext visitFunction(MolangParser.FunctionContext ctx) {
        String methodName = rename(ctx.ID().getText());
        MolangMappingTree.MethodData method = MolangMappingTree.INSTANCE.findMethod(methodName);

        if (method != null) {
            for (MolangMappingTree.FunctionInfo functionInfo : method.functionInfos()) {
                int start;
                if (!functionInfo.molangClass().pureFunction()) {
                    molangScope();
                    start = 1;
                } else {
                    start = 0;
                }
                Class<?>[] parameterTypes = functionInfo.method().getParameterTypes();
                int last = parameterTypes.length - (start + 1);
                boolean varArgs = functionInfo.method().isVarArgs();
                int exprSize = ctx.expr().size();
                for (int i = 0; i < exprSize; i++) {
                    if (varArgs) {
                        if (i == last) {
                            codeBuilder.bipush(exprSize - last).anewarray(CD_Object);
                        }

                        if (i >= last) {
                            codeBuilder.dup();
                            codeBuilder.bipush(i - last);
                        }
                    }
                    var type = visit(ctx.expr(i));

                    if (varArgs) {
                        if (i >= last) {
                            asString();
                            codeBuilder.aastore();
                        } else {
                            Class<?> parameterType = parameterTypes[i + start];
                            if (type != CompileContext.STRING == !parameterType.isPrimitive())
                                continue;
                            unboxMolangValue(parameterType);
                        }
                    } else {
                        if (i <= last) {
                            Class<?> parameterType = parameterTypes[i + start];
                            if (type != CompileContext.STRING == !parameterType.isPrimitive())
                                continue;
                            unboxMolangValue(parameterType);
                        }
                    }
                }

                return callMethod(functionInfo);
            }
        }

        floatZero();
        return CompileContext.FLOAT;
    }

    @Override
    public CompileContext visitStringValue(MolangParser.StringValueContext ctx) {
        String text = ctx.STRING().getText();
        codeBuilder.ldc(text.substring(1, text.length() - 1));
        return newMolangString();
    }

    CompileContext getField(String fieldName) {
        molangScope();
        codeBuilder
                .ldc(fieldName)
                .invokevirtual(CD_MolangScope, "get", MethodTypeDesc.of(CD_MolangObject, CD_String));
        return CompileContext.UNKNOWN;
    }

    private static final Set<String> loggedCache = new HashSet<>();

    private static void logOnce(String msg, BiConsumer<Logger, String> logAction) {
        if (loggedCache.contains(msg)) return;
        loggedCache.add(msg);
        logAction.accept(log, msg);
    }

    @Override
    public CompileContext visitVariable(MolangParser.VariableContext ctx) {
        String fieldName = rename(ctx.getText());

        if (fieldName.startsWith("variable") || fieldName.startsWith("array") || fieldName.startsWith("texture")
                || fieldName.startsWith("material") || fieldName.startsWith("geometry")) {
            return getField(fieldName);
        }

        var field = MolangMappingTree.INSTANCE.findField(fieldName);

        if (field != null) {
            codeBuilder.getstatic(ClassDesc.of(field.clazz().getName()), field.field().getName(), ConstantUtils.classDesc(field.field().getType()));
            return toMolangObject(field.field().getType());
        } else {
            var method = MolangMappingTree.INSTANCE.findMethod(fieldName);

            if (method != null) {
                for (MolangMappingTree.FunctionInfo functionInfo : method.functionInfos()) {
                    if (functionInfo.molangClass().pureFunction()) {
                        if (functionInfo.method().getParameterTypes().length == 0) {
                            return callMethod(functionInfo);
                        }
                    } else if (functionInfo.method().getParameterTypes().length == 1
                            && functionInfo.method().getParameterTypes()[0].equals(MolangScope.class)) {
                        molangScope();
                        return callMethod(functionInfo);
                    }
                }
            }

            logOnce("can't found field or method: " + fieldName, Logger::debug);
            return getField(fieldName);
        }
    }

    private CompileContext callMethod(MolangMappingTree.FunctionInfo method) {
        codeBuilder.invokestatic(ClassDesc.of(method.molangClass().classInstance().getName()), method.method().getName(),
                MethodTypeDesc.of(getCDFromClass(method.method().getReturnType()), Stream.of(method.method().getParameterTypes()).map(this::getCDFromClass).toArray(ClassDesc[]::new)));
        return toMolangObject(method.method().getReturnType());
    }

    ClassDesc getCDFromClass(Class<?> clazz) {
        return switch (clazz.getName()) {
            case "int" -> CD_int;
            case "long" -> CD_long;
            case "float" -> CD_float;
            case "double" -> CD_double;
            case "short" -> CD_short;
            case "byte" -> CD_byte;
            case "char" -> CD_char;
            case "boolean" -> CD_boolean;
            case "void" -> CD_void;
            default -> ConstantUtils.classDesc(clazz);
        };
    }

    void box(Class<?> clazz) {
        switch (clazz.getName()) {
            case "int" -> codeBuilder.invokestatic(CD_Integer, "valueOf", MethodTypeDesc.of(CD_Integer, CD_int));
            case "long" -> codeBuilder.invokestatic(CD_Long, "valueOf", MethodTypeDesc.of(CD_Long, CD_long));
            case "float" -> codeBuilder.invokestatic(CD_Float, "valueOf", MethodTypeDesc.of(CD_Float, CD_float));
            case "double" -> codeBuilder.invokestatic(CD_Double, "valueOf", MethodTypeDesc.of(CD_Double, CD_double));
            case "short" -> codeBuilder.invokestatic(CD_Short, "valueOf", MethodTypeDesc.of(CD_Short, CD_short));
            case "byte" -> codeBuilder.invokestatic(CD_Byte, "valueOf", MethodTypeDesc.of(CD_Byte, CD_byte));
            case "char" -> codeBuilder.invokestatic(CD_Character, "valueOf", MethodTypeDesc.of(CD_Character, CD_char));
            case "boolean" ->
                    codeBuilder.invokestatic(CD_Boolean, "valueOf", MethodTypeDesc.of(CD_Boolean, CD_boolean));
        }
    }

    CompileContext toMolangObject(Class<?> clazz) {
        return switch (clazz.getName()) {
            case "int", "short", "byte", "char", "boolean" -> {
                codeBuilder.i2f();
                yield newMolangFloat();
            }
            case "long" -> {
                codeBuilder.l2f();
                yield newMolangFloat();
            }
            case "float" -> newMolangFloat();
            case "double" -> {
                codeBuilder.d2f();
                yield newMolangFloat();
            }
            default -> newMolangString();
        };
    }

    void unboxMolangValue(Class<?> clazz) {
        switch (clazz.getName()) {
            case "int" -> {
                asFloat();
                codeBuilder.f2i();
            }
            case "long" -> {
                asFloat();
                codeBuilder.f2l();
            }
            case "float" -> asFloat();
            case "double" -> {
                asFloat();
                codeBuilder.f2d();
            }
            case "short" -> {
                asFloat();
                codeBuilder.f2i().i2s();
            }
            case "byte" -> {
                asFloat();
                codeBuilder.f2i().i2b();
            }
            case "char" -> {
                asFloat();
                codeBuilder.f2i().i2c();
            }
            case "boolean" -> asBoolean();
            default -> asString();
        }
    }

    @Override
    public CompileContext visitNumber(MolangParser.NumberContext ctx) {
        codeBuilder.ldc(Float.parseFloat(ctx.getText()));
        return newMolangFloat();
    }

    @Override
    public CompileContext visitParenthesesPrecedence(MolangParser.ParenthesesPrecedenceContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public CompileContext visitTerminal(TerminalNode node) {
//        return ";;";
        return CompileContext.UNKNOWN;
    }
}
