package io.github.tt432.eyelib.molang.compiler;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.tt432.eyelib.molang.compiler.binding.BoundMolang;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.Nullable;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 从绑定后的 Molang AST 生成 JVM 字节码，生成的 .class 实现 {@link CompiledMolangExpression}。
 *
 * @author TT432
 */
public final class MolangBytecodeEmitter {

    private static final int ACC_PUBLIC = 0x0001;

    private static final ClassDesc CD_MOLANG_OBJECT =
            ClassDesc.of("io.github.tt432.eyelib.molang.type.MolangObject");
    private static final ClassDesc CD_MOLANG_FLOAT =
            ClassDesc.of("io.github.tt432.eyelib.molang.type.MolangFloat");
    private static final ClassDesc CD_MOLANG_STRING =
            ClassDesc.of("io.github.tt432.eyelib.molang.type.MolangString");
    private static final ClassDesc CD_MOLANG_NULL =
            ClassDesc.of("io.github.tt432.eyelib.molang.type.MolangNull");
    private static final ClassDesc CD_MOLANG_ARRAY =
            ClassDesc.of("io.github.tt432.eyelib.molang.type.MolangArray");
    private static final ClassDesc CD_MOLANG_SCOPE =
            ClassDesc.of("io.github.tt432.eyelib.molang.MolangScope");
    private static final ClassDesc CD_RUNTIME_SUPPORT =
            ClassDesc.of("io.github.tt432.eyelib.molang.compiler.MolangRuntimeSupport");
    private static final ClassDesc CD_STRING = ClassDesc.of("java.lang.String");
    private static final ClassDesc CD_ITERATOR = ClassDesc.of("java.util.Iterator");
    private static final ClassDesc CD_LIST = ClassDesc.of("java.util.List");
    private static final ClassDesc CD_FLOAT = ClassDesc.ofDescriptor("F");
    private static final ClassDesc CD_BOOL = ClassDesc.ofDescriptor("Z");
    private static final ClassDesc CD_MOLANG_OBJECT_ARRAY =
            ClassDesc.ofDescriptor("[Lio/github/tt432/eyelib/molang/type/MolangObject;");
    private static final ClassDesc CD_INT = ClassDesc.ofDescriptor("I");
    private static final ClassDesc CD_VOID = ClassDesc.ofDescriptor("V");

    private MolangBytecodeEmitter() {
    }

    /**
     * Emits a complete .class file implementing {@link CompiledMolangExpression}
     * for the given bound AST.
     *
     * @param input the bound compiler input (must not be {@code null})
     *
     * @return the class file bytes
     */
    public static byte[] emit(BoundMolangCompilerInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        String sourceExpr = input.sourceExpression();
        BoundMolang.BoundExpr rootExpr = input.root().root();

        // 隐藏类必须与查找类（MolangCompilerImpl）在同一个运行时包中
        String internalName = "io/github/tt432/eyelib/molang/compiler/Molang$Expr$"
                + Integer.toHexString(sourceExpr.hashCode());
        ClassDesc thisClass = ClassDesc.ofDescriptor("L" + internalName + ";");
        ClassDesc cdString = ClassDesc.of("java.lang.String");
        ClassDesc cdSet = ClassDesc.of("java.util.Set");
        ClassDesc cdScope = ClassDesc.of("io.github.tt432.eyelib.molang.MolangScope");
        ClassDesc cdCompiledExpr = ClassDesc.of(
                "io.github.tt432.eyelib.molang.compiler.CompiledMolangExpression"
        );
        ClassDesc cdObject = ClassDesc.of("java.lang.Object");

        ClassDesc cdCollections = ClassDesc.of("java.util.Collections");

        return ClassFile.of().build(thisClass, classBuilder -> {
            // 固定为 Java 17（主版本号 61）以保证 Forge 兼容性
            classBuilder.withVersion(61, 0);
            classBuilder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
            classBuilder.withSuperclass(cdObject);
            classBuilder.withInterfaceSymbols(cdCompiledExpr);

            // no-arg constructor
            classBuilder.withMethod("<init>",
                                    MethodTypeDesc.of(CD_VOID),
                                    ACC_PUBLIC,
                                    mb -> mb.withCode(code -> {
                                        code.aload(0);
                                        code.invokespecial(cdObject, "<init>",
                                                           MethodTypeDesc.of(CD_VOID));
                                        code.return_();
                                    }));

            // evaluate(MolangScope) : MolangObject
            classBuilder.withMethod("evaluate",
                                     MethodTypeDesc.of(CD_MOLANG_OBJECT, cdScope),
                                     ACC_PUBLIC,
                                     mb -> mb.withCode(code -> {
                                         EmitState state = new EmitState();
                                         emitExpr(code, rootExpr, state);
                                         code.areturn();
                                     }));

            // sourceExpression() : String
            classBuilder.withMethod("sourceExpression",
                                    MethodTypeDesc.of(cdString),
                                    ACC_PUBLIC,
                                    mb -> mb.withCode(code -> {
                                        code.ldc(sourceExpr);
                                        code.areturn();
                                    }));

            // requiredHostRoles() : Set<String>
            classBuilder.withMethod("requiredHostRoles",
                                    MethodTypeDesc.of(cdSet),
                                    ACC_PUBLIC,
                                    mb -> mb.withCode(code -> {
                                        code.invokestatic(cdCollections, "emptySet",
                                                          MethodTypeDesc.of(cdSet));
                                        code.areturn();
                                    }));
        });
    }

    /**
     * Recursively emits bytecode for a bound expression, leaving a
     * {@link MolangObject} on the stack.
     */
    private static void emitExpr(CodeBuilder code, BoundMolang.BoundExpr expr, EmitState state) {
        if (expr instanceof BoundMolang.BoundNumberLiteralExpr num) {
            emitNumberLiteral(code, (float) num.value());
        } else if (expr instanceof BoundMolang.BoundStringLiteralExpr str) {
            code.ldc(unquote(str.rawText()));
            code.invokestatic(CD_MOLANG_STRING, "valueOf", MethodTypeDesc.of(CD_MOLANG_STRING, CD_STRING));
        } else if (expr instanceof BoundMolang.BoundUnaryExpr unary) {
            emitUnaryExpr(code, unary, state);
        } else if (expr instanceof BoundMolang.BoundBinaryExpr binary) {
            emitBinaryExpr(code, binary, state);
        } else if (expr instanceof BoundMolang.BoundGroupingExpr grouping) {
            emitExpr(code, grouping.expression(), state);
        } else if (expr instanceof BoundMolang.BoundThisExpr) {
            code.aload(1);
            code.ldc("this");
            code.invokevirtual(CD_MOLANG_SCOPE, "get", MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_STRING));
        } else if (expr instanceof BoundMolang.BoundBreakExpr) {
            code.goto_(currentLoopContext(state).breakLabel());
            code.aconst_null();
        } else if (expr instanceof BoundMolang.BoundContinueExpr) {
            code.goto_(currentLoopContext(state).continueLabel());
            code.aconst_null();
        } else if (expr instanceof BoundMolang.BoundIdentifierExpr identifierExpr) {
            code.aload(1);
            code.ldc(identifierExpr.name());
            code.invokevirtual(CD_MOLANG_SCOPE, "get", MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_STRING));
        } else if (expr instanceof BoundMolang.BoundAssignmentExpr assignmentExpr) {
            emitAssignmentExpr(code, assignmentExpr, state);
        } else if (expr instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            code.aload(1);
            code.ldc(memberAccessName(memberAccessExpr));
            code.invokestatic(CD_RUNTIME_SUPPORT, "resolveMemberAccess",
                              MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_MOLANG_SCOPE, CD_STRING));
        } else if (expr instanceof BoundMolang.BoundCallExpr callExpr) {
            code.aload(1);
            code.ldc(resolveCallName(callExpr.callee()));
            emitCallArgsArray(code, callExpr.arguments(), state);
            code.invokestatic(CD_RUNTIME_SUPPORT, "resolveCall",
                              MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_MOLANG_SCOPE, CD_STRING, CD_MOLANG_OBJECT_ARRAY));
        } else if (expr instanceof BoundMolang.BoundArrowAccessExpr arrowAccessExpr) {
            // 箭头访问（->）是文档化的 Molang 跨实体访问构造。
            // 设计意图：左侧求值为宿主实体引用，右侧在该宿主上下文中求值。
            // 当前实现：左侧求值后丢弃，仅返回右侧值。
            // TODO: 实现箭头访问语义的 HostContext 切换。
            emitExpr(code, arrowAccessExpr.left(), state);
            code.pop();
            emitExpr(code, arrowAccessExpr.right(), state);
        } else if (expr instanceof BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
            emitExpr(code, queryAccessExpr.access(), state);
        } else if (expr instanceof BoundMolang.BoundIndexExpr indexExpr) {
            code.aload(1);
            emitExpr(code, indexExpr.owner(), state);
            emitExpr(code, indexExpr.index(), state);
            code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
            code.f2i();
            code.invokestatic(CD_RUNTIME_SUPPORT, "resolveIndex",
                              MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_MOLANG_SCOPE, CD_MOLANG_OBJECT, CD_INT));
        } else if (expr instanceof BoundMolang.BoundBlockExpr blockExpr) {
            emitBlockExpr(code, blockExpr, state);
        } else if (expr instanceof BoundMolang.BoundLoopExpr loopExpr) {
            emitLoopExpr(code, loopExpr, state);
        } else if (expr instanceof BoundMolang.BoundForEachExpr forEachExpr) {
            emitForEachExpr(code, forEachExpr, state);
        } else if (expr instanceof BoundMolang.BoundNullCoalesceExpr nullCoalesceExpr) {
            emitNullCoalesceExpr(code, nullCoalesceExpr, state);
        } else if (expr instanceof BoundMolang.BoundTernaryConditionalExpr ternary) {
            emitTernaryExpr(code, ternary, state);
        } else if (expr instanceof BoundMolang.BoundBinaryConditionalExpr binaryCond) {
            emitBinaryConditionalExpr(code, binaryCond, state);
        } else if (expr instanceof BoundMolang.BoundUnknownExpr || expr instanceof BoundMolang.BoundDeferredExpr) {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported expression type: " + expr.getClass().getSimpleName());
        }
    }

    private static void emitStmt(CodeBuilder code, BoundMolang.BoundStmt stmt, EmitState state) {
        if (stmt instanceof BoundMolang.BoundExprStmt exprStmt) {
            emitExpr(code, exprStmt.expression(), state);
        } else if (stmt instanceof BoundMolang.BoundReturnStmt returnStmt) {
            emitExpr(code, returnStmt.expression(), state);
            code.areturn();
        } else if (stmt instanceof BoundMolang.BoundBreakStmt breakStmt) {
            LoopContext loopContext = currentLoopContext(state);
            if (breakStmt.valueExpr() != null) {
                emitExpr(code, breakStmt.valueExpr(), state);
                code.astore(loopContext.resultLocalSlot());
            }
            code.goto_(loopContext.breakLabel());
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        } else if (stmt instanceof BoundMolang.BoundContinueStmt continueStmt) {
            LoopContext loopContext = currentLoopContext(state);
            if (continueStmt.valueExpr() != null) {
                emitExpr(code, continueStmt.valueExpr(), state);
                code.pop();
            }
            code.goto_(loopContext.continueLabel());
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        } else {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        }
    }

    private static void emitNumberLiteral(CodeBuilder code, float value) {
        code.ldc(value);
        code.invokestatic(CD_MOLANG_FLOAT, "valueOf",
                          MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
    }

    private static void emitBinaryExpr(CodeBuilder code, BoundMolang.BoundBinaryExpr binary, EmitState state) {
        String operator = binary.operator();
        if ("&&".equals(operator)) {
            emitExpr(code, binary.left(), state);
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            Label falseLabel = code.newLabel();
            Label endLabel = code.newLabel();
            code.ifeq(falseLabel);
            emitExpr(code, binary.right(), state);
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_BOOL));
            code.goto_(endLabel);
            code.labelBinding(falseLabel);
            code.getstatic(CD_MOLANG_FLOAT, "ZERO", CD_MOLANG_FLOAT);
            code.labelBinding(endLabel);
            return;
        }
        if ("||".equals(operator)) {
            emitExpr(code, binary.left(), state);
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            Label falseLabel = code.newLabel();
            Label endLabel = code.newLabel();
            code.ifeq(falseLabel);
            code.getstatic(CD_MOLANG_FLOAT, "ONE", CD_MOLANG_FLOAT);
            code.goto_(endLabel);
            code.labelBinding(falseLabel);
            emitExpr(code, binary.right(), state);
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_BOOL));
            code.labelBinding(endLabel);
            return;
        }

        // Handle == and != with type-aware comparison via equalsF/nEqualsF
        if ("==".equals(operator)) {
            emitExpr(code, binary.left(), state);
            emitExpr(code, binary.right(), state);
            code.invokeinterface(CD_MOLANG_OBJECT, "equalsF",
                                 MethodTypeDesc.of(CD_FLOAT, CD_MOLANG_OBJECT));
            code.invokestatic(CD_MOLANG_FLOAT, "valueOf",
                              MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
            return;
        }
        if ("!=".equals(operator)) {
            emitExpr(code, binary.left(), state);
            emitExpr(code, binary.right(), state);
            code.invokeinterface(CD_MOLANG_OBJECT, "nEqualsF",
                                 MethodTypeDesc.of(CD_FLOAT, CD_MOLANG_OBJECT));
            code.invokestatic(CD_MOLANG_FLOAT, "valueOf",
                              MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
            return;
        }

        emitExpr(code, binary.left(), state);
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
        emitExpr(code, binary.right(), state);
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));

        switch (operator) {
            case "+" -> code.fadd();
            case "-" -> code.fsub();
            case "*" -> code.fmul();
            case "/" -> emitSafeDiv(code);
            case "<", "<=", ">", ">=" -> emitComparison(code, operator);
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + operator);
        }

        if (!"==".equals(operator) && !"!=".equals(operator)
                && !"<".equals(operator) && !"<=".equals(operator)
                && !">".equals(operator) && !">=".equals(operator)) {
            code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
        }
    }

    /**
     * 发出安全除法字节码，除数为零时返回 0.0f，与 Bedrock Molang 语义一致。
     * 前置：栈上为 [leftFloat, rightFloat]，rightFloat 在栈顶。
     * 后置：栈上为 [resultFloat]。
     */
    private static void emitSafeDiv(CodeBuilder code) {
        Label notZero = code.newLabel();
        Label end = code.newLabel();
        code.dup();          // [left, right, right]
        code.fconst_0();     // [left, right, right, 0.0f]
        code.fcmpg();        // [left, right, cmp]  — pops right_copy and 0.0f
        code.ifne(notZero);  // [left, right]        — pops cmp, branches if right != 0
        code.pop();          // [left]               — discard right
        code.pop();          // []                   — discard left
        code.fconst_0();     // [0.0f]
        code.goto_(end);
        code.labelBinding(notZero);
        code.fdiv();         // [left / right]
        code.labelBinding(end);
    }

    private static void emitComparison(CodeBuilder code, String operator) {
        Label trueLabel = code.newLabel();
        Label endLabel = code.newLabel();
        code.fcmpg();
        switch (operator) {
            case "==" -> code.ifeq(trueLabel);
            case "!=" -> code.ifne(trueLabel);
            case "<" -> code.iflt(trueLabel);
            case "<=" -> code.ifle(trueLabel);
            case ">" -> code.ifgt(trueLabel);
            case ">=" -> code.ifge(trueLabel);
            default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
        }
        code.getstatic(CD_MOLANG_FLOAT, "ZERO", CD_MOLANG_FLOAT);
        code.goto_(endLabel);
        code.labelBinding(trueLabel);
        code.getstatic(CD_MOLANG_FLOAT, "ONE", CD_MOLANG_FLOAT);
        code.labelBinding(endLabel);
    }

    private static void emitUnaryExpr(CodeBuilder code, BoundMolang.BoundUnaryExpr unary, EmitState state) {
        emitExpr(code, unary.expression(), state);
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
        switch (unary.operator()) {
            case "+" -> code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
            case "-" -> {
                code.fneg();
                code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
            }
            case "!" -> {
                Label zeroLabel = code.newLabel();
                Label endLabel = code.newLabel();
                code.fconst_0();
                code.fcmpg();
                code.ifeq(zeroLabel);
                code.getstatic(CD_MOLANG_FLOAT, "ZERO", CD_MOLANG_FLOAT);
                code.goto_(endLabel);
                code.labelBinding(zeroLabel);
                code.getstatic(CD_MOLANG_FLOAT, "ONE", CD_MOLANG_FLOAT);
                code.labelBinding(endLabel);
            }
            default -> code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        }
    }

    private static void emitAssignmentExpr(CodeBuilder code, BoundMolang.BoundAssignmentExpr assignmentExpr, EmitState state) {
        emitExpr(code, assignmentExpr.value(), state);
        if (assignmentExpr.writableTarget()) {
            String targetName = resolveAssignmentTargetName(assignmentExpr.target());
            if (targetName != null) {
                // 栈：{ ..., value }
                // 我们需要：{ ..., value, scope, name, value(dup) } 用于 invokevirtual scope.set(name, value)
                code.aload(1);                                // → {value, scope}
                code.swap();                                  // → {scope, value}
                code.dup_x1();                                // → {value, scope, value}
                code.ldc(targetName);                         // → {value, scope, value, name}
                code.swap();                                  // → {value, scope, name, value}
                code.invokevirtual(CD_MOLANG_SCOPE, "set",
                                   MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_STRING, CD_MOLANG_OBJECT));
                code.pop();                                   // discard set return, leaving original value
            }
        }
    }

    private static void emitNullCoalesceExpr(CodeBuilder code, BoundMolang.BoundNullCoalesceExpr expr, EmitState state) {
        emitExpr(code, expr.left(), state);
        code.dup();
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
        code.fconst_0();
        code.fcmpg();
        Label returnLeft = code.newLabel();
        Label end = code.newLabel();
        code.ifne(returnLeft);
        code.pop();
        emitExpr(code, expr.right(), state);
        code.goto_(end);
        code.labelBinding(returnLeft);
        code.labelBinding(end);
    }

    private static void emitTernaryExpr(CodeBuilder code, BoundMolang.BoundTernaryConditionalExpr ternary, EmitState state) {
        emitExpr(code, ternary.condition(), state);
        code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
        Label elseLabel = code.newLabel();
        Label endLabel = code.newLabel();
        code.ifeq(elseLabel);
        emitExpr(code, ternary.whenTrue(), state);
        code.goto_(endLabel);
        code.labelBinding(elseLabel);
        emitExpr(code, ternary.whenFalse(), state);
        code.labelBinding(endLabel);
    }

    /**
     * 发出二元条件表达式字节码：condition 为真则返回 whenTrue，否则返回 MolangNull.INSTANCE。
     * 对应 Bedrock 语法 {@code <test> ? <if true>}。
     */
    private static void emitBinaryConditionalExpr(CodeBuilder code, BoundMolang.BoundBinaryConditionalExpr expr, EmitState state) {
        emitExpr(code, expr.condition(), state);
        code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
        Label elseLabel = code.newLabel();
        Label endLabel = code.newLabel();
        code.ifeq(elseLabel);
        emitExpr(code, expr.whenTrue(), state);
        code.goto_(endLabel);
        code.labelBinding(elseLabel);
        code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        code.labelBinding(endLabel);
    }

    private static void emitBlockExpr(CodeBuilder code, BoundMolang.BoundBlockExpr blockExpr, EmitState state) {
        if (blockExpr.statements().isEmpty()) {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
            return;
        }
        for (int i = 0; i < blockExpr.statements().size(); i++) {
            BoundMolang.BoundStmt stmt = blockExpr.statements().get(i);
            emitStmt(code, stmt, state);
            if (i < blockExpr.statements().size() - 1) {
                code.pop();
            }
        }
        BoundMolang.BoundStmt lastStmt = blockExpr.statements().get(blockExpr.statements().size() - 1);
        if (lastStmt instanceof BoundMolang.BoundReturnStmt) {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        } else if (!blockExpr.returnsLastValue()) {
            code.pop();
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        }
    }

    private static void emitLoopExpr(CodeBuilder code, BoundMolang.BoundLoopExpr loopExpr, EmitState state) {
        int counterSlot = state.allocateLocal();
        int resultSlot = state.allocateLocal();
        emitExpr(code, loopExpr.countExpr(), state);
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
        code.f2i();
        code.istore(counterSlot);

        // 基岩版安全限制：loop 最大迭代 1024 次
        code.iload(counterSlot);
        code.ldc(1024);
        Label clampEnd = code.newLabel();
        code.if_icmple(clampEnd);
        code.ldc(1024);
        code.istore(counterSlot);
        code.labelBinding(clampEnd);

        code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        code.astore(resultSlot);

        Label loopStart = code.newLabel();
        Label loopEnd = code.newLabel();
        Label continueTarget = code.newLabel();
        state.loopContexts.push(new LoopContext(continueTarget, loopEnd, resultSlot));

        code.labelBinding(loopStart);
        code.iload(counterSlot);
        code.ifle(loopEnd);
        emitExpr(code, loopExpr.body(), state);
        code.pop();
        code.labelBinding(continueTarget);
        code.iinc(counterSlot, -1);
        code.goto_(loopStart);
        code.labelBinding(loopEnd);
        state.loopContexts.pop();
        code.aload(resultSlot);
    }

    private static void emitForEachExpr(CodeBuilder code, BoundMolang.BoundForEachExpr forEachExpr, EmitState state) {
        int arraySlot = state.allocateLocal();
        int iteratorSlot = state.allocateLocal();
        int resultSlot = state.allocateLocal();
        Label loopStart = code.newLabel();
        Label continueTarget = code.newLabel();
        Label loopEnd = code.newLabel();

        emitExpr(code, forEachExpr.collection(), state);
        code.astore(arraySlot);

        code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        code.astore(resultSlot);

        code.aload(arraySlot);
        code.instanceOf(CD_MOLANG_ARRAY);
        code.ifeq(loopEnd);

        code.aload(arraySlot);
        code.checkcast(CD_MOLANG_ARRAY);
        code.invokevirtual(CD_MOLANG_ARRAY, "value", MethodTypeDesc.of(CD_LIST));
        code.invokeinterface(CD_LIST, "iterator", MethodTypeDesc.of(CD_ITERATOR));
        code.astore(iteratorSlot);

        state.loopContexts.push(new LoopContext(continueTarget, loopEnd, resultSlot));
        code.labelBinding(loopStart);
        code.aload(iteratorSlot);
        code.invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(CD_BOOL));
        code.ifeq(loopEnd);

        code.aload(1);
        code.ldc(resolveForEachVariableName(forEachExpr.variable()));
        code.aload(iteratorSlot);
        code.invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(ClassDesc.of("java.lang.Object")));
        code.checkcast(CD_MOLANG_OBJECT);
        code.invokevirtual(CD_MOLANG_SCOPE, "set", MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_STRING, CD_MOLANG_OBJECT));
        code.pop();

        emitExpr(code, forEachExpr.body(), state);
        code.pop();
        code.labelBinding(continueTarget);
        code.goto_(loopStart);
        code.labelBinding(loopEnd);
        state.loopContexts.pop();
        code.aload(resultSlot);
    }

    private static void emitCallArgsArray(CodeBuilder code, List<BoundMolang.BoundExpr> args, EmitState state) {
        code.ldc(args.size());
        code.anewarray(CD_MOLANG_OBJECT);
        for (int i = 0; i < args.size(); i++) {
            code.dup();
            code.ldc(i);
            emitExpr(code, args.get(i), state);
            code.aastore();
        }
    }

    private static @Nullable String resolveAssignmentTargetName(BoundMolang.BoundExpr target) {
        if (target instanceof BoundMolang.BoundIdentifierExpr id) {
            return id.name();
        }
        if (target instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            return memberAccessName(memberAccessExpr);
        }
        if (target instanceof BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
            return resolveAssignmentTargetName(queryAccessExpr.access());
        }
        return null;
    }

    private static String resolveCallName(BoundMolang.BoundExpr callee) {
        if (callee instanceof BoundMolang.BoundIdentifierExpr id) {
            return id.name();
        }
        if (callee instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            return memberAccessName(memberAccessExpr);
        }
        return "";
    }

    private static String resolveForEachVariableName(BoundMolang.BoundExpr variable) {
        String targetName = resolveAssignmentTargetName(variable);
        if (targetName == null) {
            return "variable.";
        }
        if (targetName.startsWith("variable.")) {
            return targetName;
        }
        int lastDot = targetName.lastIndexOf('.');
        return "variable." + (lastDot >= 0 ? targetName.substring(lastDot + 1) : targetName);
    }

    private static LoopContext currentLoopContext(EmitState state) {
        LoopContext loopContext = state.loopContexts.peek();
        if (loopContext == null) {
            throw new IllegalStateException("break/continue outside of loop");
        }
        return loopContext;
    }

    private static String memberAccessName(BoundMolang.BoundMemberAccessExpr expr) {
        List<String> segments = new ArrayList<>();
        collectMemberSegments(expr, segments);
        return String.join(".", segments);
    }

    private static void collectMemberSegments(BoundMolang.BoundMemberAccessExpr expr, List<String> segments) {
        if (expr.owner() instanceof BoundMolang.BoundMemberAccessExpr owner) {
            collectMemberSegments(owner, segments);
        } else if (expr.owner() instanceof BoundMolang.BoundIdentifierExpr id) {
            segments.add(id.name());
        }
        segments.add(expr.memberName());
    }

    private static String unquote(String rawText) {
        if (rawText == null || rawText.length() < 2) {
            return rawText == null ? "" : rawText;
        }
        char first = rawText.charAt(0);
        char last = rawText.charAt(rawText.length() - 1);
        if ((first == '\'' || first == '"') && first == last) {
            return rawText.substring(1, rawText.length() - 1);
        }
        return rawText;
    }

    private static final class EmitState {
        private final Deque<LoopContext> loopContexts = new ArrayDeque<>();
        private int nextLocalSlot = 2;

        private int allocateLocal() {
            return nextLocalSlot++;
        }
    }

    private record LoopContext(Label continueLabel, Label breakLabel, int resultLocalSlot) {
    }
}
