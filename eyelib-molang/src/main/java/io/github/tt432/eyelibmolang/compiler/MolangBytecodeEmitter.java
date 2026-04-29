package io.github.tt432.eyelibmolang.compiler;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import io.github.tt432.eyelibmolang.type.MolangObject;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

/**
 * Generates JVM bytecode from a bound Molang AST using jdk-classfile-backport.
 *
 * <p>The generated .class file implements {@link CompiledMolangExpression} and
 * supports number literals and binary arithmetic ({@code + - * /}).
 * Each expression tree node is emitted as real JVM instructions so that
 * runtime scope values participate in evaluation.
 */
public final class MolangBytecodeEmitter {

    private static final int ACC_PUBLIC = 0x0001;

    private static final ClassDesc CD_MOLANG_OBJECT =
            ClassDesc.of("io.github.tt432.eyelibmolang.type.MolangObject");
    private static final ClassDesc CD_MOLANG_FLOAT =
            ClassDesc.of("io.github.tt432.eyelibmolang.type.MolangFloat");
    private static final ClassDesc CD_FLOAT = ClassDesc.ofDescriptor("F");
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

        String internalName = "io/github/tt432/eyelibmolang/compiler/generated/Molang$Expr$"
                + Integer.toHexString(sourceExpr.hashCode());
        ClassDesc thisClass = ClassDesc.ofDescriptor("L" + internalName + ";");
        ClassDesc cdString = ClassDesc.of("java.lang.String");
        ClassDesc cdSet = ClassDesc.of("java.util.Set");
        ClassDesc cdScope = ClassDesc.of("io.github.tt432.eyelibmolang.MolangScope");
        ClassDesc cdCompiledExpr = ClassDesc.of(
                "io.github.tt432.eyelibmolang.compiler.CompiledMolangExpression"
        );
        ClassDesc cdObject = ClassDesc.of("java.lang.Object");

        return ClassFile.of().build(thisClass, classBuilder -> {
            // Pin to Java 17 (major version 61) for Forge compatibility
            classBuilder.withVersion(61, 0);
            classBuilder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
            classBuilder.withSuperclass(cdObject);
            classBuilder.withInterfaceSymbols(cdCompiledExpr);

            // ---- no-arg constructor ----
            classBuilder.withMethod("<init>",
                    MethodTypeDesc.of(CD_VOID),
                    ACC_PUBLIC,
                    mb -> mb.withCode(code -> {
                        code.aload(0);
                        code.invokespecial(cdObject, "<init>",
                                MethodTypeDesc.of(CD_VOID));
                        code.return_();
                    }));

            // ---- evaluate(MolangScope) : MolangObject ----
            classBuilder.withMethod("evaluate",
                    MethodTypeDesc.of(CD_MOLANG_OBJECT, cdScope),
                    ACC_PUBLIC,
                    mb -> mb.withCode(code -> {
                        emitExpr(code, rootExpr);
                        code.areturn();
                    }));

            // ---- sourceExpression() : String ----
            classBuilder.withMethod("sourceExpression",
                    MethodTypeDesc.of(cdString),
                    ACC_PUBLIC,
                    mb -> mb.withCode(code -> {
                        code.ldc(sourceExpr);
                        code.areturn();
                    }));

            // ---- requiredHostRoles() : Set<String> ----
            classBuilder.withMethod("requiredHostRoles",
                    MethodTypeDesc.of(cdSet),
                    ACC_PUBLIC,
                    mb -> mb.withCode(code -> {
                        code.invokestatic(cdSet, "of",
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
        } else if (expr instanceof BoundMolang.BoundBinaryExpr binary) {
            emitBinaryExpr(code, binary);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported expression type: " + expr.getClass().getSimpleName());
        }
    }

    private static void emitNumberLiteral(CodeBuilder code, float value) {
        code.ldc(value);
        code.invokestatic(CD_MOLANG_FLOAT, "valueOf",
                MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
    }

    private static void emitBinaryExpr(CodeBuilder code, BoundMolang.BoundBinaryExpr binary) {
        // Evaluate left operand → MolangObject on stack
        emitExpr(code, binary.left());
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat",
                MethodTypeDesc.of(CD_FLOAT));
        // Evaluate right operand → MolangObject on stack
        emitExpr(code, binary.right());
        code.invokeinterface(CD_MOLANG_OBJECT, "asFloat",
                MethodTypeDesc.of(CD_FLOAT));

        // Apply float operator (both operands are float on stack)
        switch (binary.operator()) {
            case "+" -> code.fadd();
            case "-" -> code.fsub();
            case "*" -> code.fmul();
            case "/" -> code.fdiv();
            default -> throw new IllegalArgumentException(
                    "Unsupported binary operator: " + binary.operator());
        }

        // Wrap result in MolangFloat
        code.invokestatic(CD_MOLANG_FLOAT, "valueOf",
                MethodTypeDesc.of(CD_MOLANG_FLOAT, CD_FLOAT));
    }
}
