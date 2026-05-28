package io.github.tt432.eyelibmolang.compiler;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.jspecify.annotations.NullMarked;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

/**
 * 从绑定后的 Molang AST 生成 JVM 字节码，生成的 .class 实现 {@link CompiledMolangExpression}。
 *
 * @author TT432
 */
@NullMarked
public final class MolangBytecodeEmitter {

    private static final int ACC_PUBLIC = 0x0001;

    private static final ClassDesc CD_MOLANG_OBJECT =
            ClassDesc.of("io.github.tt432.eyelibmolang.type.MolangObject");
    private static final ClassDesc CD_MOLANG_FLOAT =
            ClassDesc.of("io.github.tt432.eyelibmolang.type.MolangFloat");
    private static final ClassDesc CD_MOLANG_STRING =
            ClassDesc.of("io.github.tt432.eyelibmolang.type.MolangString");
    private static final ClassDesc CD_MOLANG_NULL =
            ClassDesc.of("io.github.tt432.eyelibmolang.type.MolangNull");
    private static final ClassDesc CD_MOLANG_SCOPE =
            ClassDesc.of("io.github.tt432.eyelibmolang.MolangScope");
    private static final ClassDesc CD_RUNTIME_SUPPORT =
            ClassDesc.of("io.github.tt432.eyelibmolang.compiler.MolangRuntimeSupport");
    private static final ClassDesc CD_STRING = ClassDesc.of("java.lang.String");
    private static final ClassDesc CD_FLOAT = ClassDesc.ofDescriptor("F");
    private static final ClassDesc CD_BOOL = ClassDesc.ofDescriptor("Z");
    private static final ClassDesc CD_MOLANG_OBJECT_ARRAY =
            ClassDesc.ofDescriptor("[Lio/github/tt432/eyelibmolang/type/MolangObject;");
    private static final ClassDesc CD_INT = ClassDesc.ofDescriptor("I");
    private static final ClassDesc CD_VOID = ClassDesc.ofDescriptor("V");

    private MolangBytecodeEmitter() {
    }

    /**
     * Emits a complete .class file implementing {@link CompiledMolangExpression}
     * for the given bound AST.
     *
     * @param input the bound compiler input (must not be {@code null})
     * @return the class file bytes
     */
    public static byte[] emit(BoundMolangCompilerInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        String sourceExpr = input.sourceExpression();
        BoundMolang.BoundExpr rootExpr = input.root().root();

        // Hidden class must be in same runtime package as the lookup class (MolangCompilerImpl)
        String internalName = "io/github/tt432/eyelibmolang/compiler/Molang$Expr$"
                + Integer.toHexString(sourceExpr.hashCode());
        ClassDesc thisClass = ClassDesc.ofDescriptor("L" + internalName + ";");
        ClassDesc cdString = ClassDesc.of("java.lang.String");
        ClassDesc cdSet = ClassDesc.of("java.util.Set");
        ClassDesc cdScope = ClassDesc.of("io.github.tt432.eyelibmolang.MolangScope");
        ClassDesc cdCompiledExpr = ClassDesc.of(
                "io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression"
        );
        ClassDesc cdObject = ClassDesc.of("java.lang.Object");

        ClassDesc cdCollections = ClassDesc.of("java.util.Collections");

        return ClassFile.of().build(thisClass, classBuilder -> {
            // Pin to Java 17 (major version 61) for Forge compatibility
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
                        emitExpr(code, rootExpr);
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
    private static void emitExpr(CodeBuilder code, BoundMolang.BoundExpr expr) {
        if (expr instanceof BoundMolang.BoundNumberLiteralExpr num) {
            emitNumberLiteral(code, (float) num.value());
        } else if (expr instanceof BoundMolang.BoundStringLiteralExpr str) {
            code.ldc(unquote(str.rawText()));
            code.invokestatic(CD_MOLANG_STRING, "valueOf", MethodTypeDesc.of(CD_MOLANG_STRING, CD_STRING));
        } else if (expr instanceof BoundMolang.BoundUnaryExpr unary) {
            emitUnaryExpr(code, unary);
        } else if (expr instanceof BoundMolang.BoundBinaryExpr binary) {
            emitBinaryExpr(code, binary);
        } else if (expr instanceof BoundMolang.BoundGroupingExpr grouping) {
            emitExpr(code, grouping.expression());
        } else if (expr instanceof BoundMolang.BoundThisExpr) {
            code.getstatic(CD_MOLANG_FLOAT, "ZERO", CD_MOLANG_FLOAT);
        } else if (expr instanceof BoundMolang.BoundIdentifierExpr identifierExpr) {
            code.aload(1);
            code.ldc(identifierExpr.name());
            code.invokevirtual(CD_MOLANG_SCOPE, "get", MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_STRING));
        } else if (expr instanceof BoundMolang.BoundAssignmentExpr assignmentExpr) {
            emitAssignmentExpr(code, assignmentExpr);
        } else if (expr instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            code.aload(1);
            code.ldc(memberAccessName(memberAccessExpr));
            code.invokestatic(CD_RUNTIME_SUPPORT, "resolveMemberAccess",
                    MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_MOLANG_SCOPE, CD_STRING));
        } else if (expr instanceof BoundMolang.BoundCallExpr callExpr) {
            code.aload(1);
            code.ldc(resolveCallName(callExpr.callee()));
            emitCallArgsArray(code, callExpr.arguments());
            code.invokestatic(CD_RUNTIME_SUPPORT, "resolveCall",
                    MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_MOLANG_SCOPE, CD_STRING, CD_MOLANG_OBJECT_ARRAY));
        } else if (expr instanceof BoundMolang.BoundArrowAccessExpr arrowAccessExpr) {
            // Arrow access (->) is a documented Molang construct for cross-entity access.
            // Design intent: left side evaluates to a host entity reference,
            // right side evaluates in that host's context.
            // Current implementation: left side is evaluated then discarded;
            // only the right side value is returned. 
            // TODO: 实现箭头访问语义的 HostContext 切换。
            emitExpr(code, arrowAccessExpr.left());
            code.pop();
            emitExpr(code, arrowAccessExpr.right());
        } else if (expr instanceof BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
            emitExpr(code, queryAccessExpr.access());
        } else if (expr instanceof BoundMolang.BoundIndexExpr indexExpr) {
            code.aload(1);
            emitExpr(code, indexExpr.owner());
            emitExpr(code, indexExpr.index());
            code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
            code.f2i();
            code.invokestatic(CD_RUNTIME_SUPPORT, "resolveIndex",
                    MethodTypeDesc.of(CD_MOLANG_OBJECT, CD_MOLANG_SCOPE, CD_MOLANG_OBJECT, CD_INT));
        } else if (expr instanceof BoundMolang.BoundBlockExpr blockExpr) {
            emitBlockExpr(code, blockExpr);
        } else if (expr instanceof BoundMolang.BoundLoopExpr loopExpr) {
            emitLoopExpr(code, loopExpr);
        } else if (expr instanceof BoundMolang.BoundForEachExpr forEachExpr) {
            if (forEachExpr.deferredReason() != null) {
                code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
            } else {
                emitExpr(code, forEachExpr.body());
            }
        } else if (expr instanceof BoundMolang.BoundNullCoalesceExpr nullCoalesceExpr) {
            emitNullCoalesceExpr(code, nullCoalesceExpr);
        } else if (expr instanceof BoundMolang.BoundTernaryConditionalExpr ternary) {
            emitTernaryExpr(code, ternary);
        } else if (expr instanceof BoundMolang.BoundUnknownExpr || expr instanceof BoundMolang.BoundDeferredExpr) {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported expression type: " + expr.getClass().getSimpleName());
        }
    }

    private static void emitStmt(CodeBuilder code, BoundMolang.BoundStmt stmt) {
        if (stmt instanceof BoundMolang.BoundExprStmt exprStmt) {
            emitExpr(code, exprStmt.expression());
        } else if (stmt instanceof BoundMolang.BoundReturnStmt returnStmt) {
            emitExpr(code, returnStmt.expression());
            code.areturn();
        } else if (stmt instanceof BoundMolang.BoundBreakStmt breakStmt) {
            if (breakStmt.deferredReason() != null) {
                code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
            }
        } else if (stmt instanceof BoundMolang.BoundContinueStmt continueStmt) {
            if (continueStmt.deferredReason() != null) {
                code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
            }
        } else {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
        }
    }

    private static void emitNumberLiteral(CodeBuilder code, float value) {
        code.ldc(value);
        code.invokestatic(CD_MOLANG_FLOAT, "valueOf",
                MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
    }

    private static void emitBinaryExpr(CodeBuilder code, BoundMolang.BoundBinaryExpr binary) {
        String operator = binary.operator();
        if ("&&".equals(operator)) {
            emitExpr(code, binary.left());
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            Label falseLabel = code.newLabel();
            Label endLabel = code.newLabel();
            code.ifeq(falseLabel);
            emitExpr(code, binary.right());
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_BOOL));
            code.goto_(endLabel);
            code.labelBinding(falseLabel);
            code.getstatic(CD_MOLANG_FLOAT, "ZERO", CD_MOLANG_FLOAT);
            code.labelBinding(endLabel);
            return;
        }
        if ("||".equals(operator)) {
            emitExpr(code, binary.left());
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            Label falseLabel = code.newLabel();
            Label endLabel = code.newLabel();
            code.ifeq(falseLabel);
            code.getstatic(CD_MOLANG_FLOAT, "ONE", CD_MOLANG_FLOAT);
            code.goto_(endLabel);
            code.labelBinding(falseLabel);
            emitExpr(code, binary.right());
            code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
            code.invokestatic(CD_MOLANG_FLOAT, "valueOf", MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_BOOL));
            code.labelBinding(endLabel);
            return;
        }

        emitExpr(code, binary.left());
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
        emitExpr(code, binary.right());
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));

        switch (operator) {
            case "+" -> code.fadd();
            case "-" -> code.fsub();
            case "*" -> code.fmul();
            case "/" -> emitSafeDiv(code);
            case "==", "!=", "<", "<=", ">", ">=" -> emitComparison(code, operator);
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

    private static void emitUnaryExpr(CodeBuilder code, BoundMolang.BoundUnaryExpr unary) {
        emitExpr(code, unary.expression());
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

    private static void emitAssignmentExpr(CodeBuilder code, BoundMolang.BoundAssignmentExpr assignmentExpr) {
        emitExpr(code, assignmentExpr.value());
        if (assignmentExpr.writableTarget()) {
            String targetName = resolveAssignmentTargetName(assignmentExpr.target());
            if (targetName != null) {
                // Stack: { ..., value }
                // We need: { ..., value, scope, name, value(dup) } for invokevirtual scope.set(name, value)
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

    private static void emitNullCoalesceExpr(CodeBuilder code, BoundMolang.BoundNullCoalesceExpr expr) {
        emitExpr(code, expr.left());
        code.dup();
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
        code.fconst_0();
        code.fcmpg();
        Label returnLeft = code.newLabel();
        Label end = code.newLabel();
        code.ifne(returnLeft);
        code.pop();
        emitExpr(code, expr.right());
        code.goto_(end);
        code.labelBinding(returnLeft);
        code.labelBinding(end);
    }

    private static void emitTernaryExpr(CodeBuilder code, BoundMolang.BoundTernaryConditionalExpr ternary) {
        emitExpr(code, ternary.condition());
        code.invokeinterface(CD_MOLANG_OBJECT, "asBoolean", MethodTypeDesc.of(CD_BOOL));
        Label elseLabel = code.newLabel();
        Label endLabel = code.newLabel();
        code.ifeq(elseLabel);
        emitExpr(code, ternary.whenTrue());
        code.goto_(endLabel);
        code.labelBinding(elseLabel);
        emitExpr(code, ternary.whenFalse());
        code.labelBinding(endLabel);
    }

    private static void emitBlockExpr(CodeBuilder code, BoundMolang.BoundBlockExpr blockExpr) {
        if (blockExpr.statements().isEmpty()) {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
            return;
        }
        for (int i = 0; i < blockExpr.statements().size(); i++) {
            BoundMolang.BoundStmt stmt = blockExpr.statements().get(i);
            emitStmt(code, stmt);
            if (i < blockExpr.statements().size() - 1) {
                code.pop();
            }
        }
    }

    private static void emitLoopExpr(CodeBuilder code, BoundMolang.BoundLoopExpr loopExpr) {
        if (loopExpr.deferredReason() != null) {
            code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
            return;
        }
        emitExpr(code, new BoundMolang.BoundNumberLiteralExpr(loopExpr.span(), loopExpr.iterationCountRawText(),
                parseIterationCount(loopExpr.iterationCountRawText())));
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat", MethodTypeDesc.of(CD_FLOAT));
        code.f2i();
        code.istore(2);
        code.iconst_0();
        code.istore(3);

        Label loopStart = code.newLabel();
        Label loopEnd = code.newLabel();
        code.labelBinding(loopStart);
        code.iload(3);
        code.iload(2);
        code.if_icmpge(loopEnd);
        emitExpr(code, loopExpr.body());
        code.pop();
        code.iinc(3, 1);
        code.goto_(loopStart);
        code.labelBinding(loopEnd);
        code.getstatic(CD_MOLANG_NULL, "INSTANCE", CD_MOLANG_NULL);
    }

    private static void emitCallArgsArray(CodeBuilder code, List<BoundMolang.BoundExpr> args) {
        code.ldc(args.size());
        code.anewarray(CD_MOLANG_OBJECT);
        for (int i = 0; i < args.size(); i++) {
            code.dup();
            code.ldc(i);
            emitExpr(code, args.get(i));
            code.aastore();
        }
    }

    private static String resolveAssignmentTargetName(BoundMolang.BoundExpr target) {
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

    private static double parseIterationCount(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignored) {
            return 0;
        }
    }
}