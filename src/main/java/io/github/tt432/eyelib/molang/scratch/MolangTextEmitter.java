package io.github.tt432.eyelib.molang.scratch;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * molang AST → 文本发射器（优先级感知，最小括号）。
 *
 * <p>优先级表与 {@code HandwrittenMolangAstParserFrontend} 的递归下降层级一一对应
 * （高数值 = 绑定更紧）：
 * <pre>
 *   1  赋值 =（右结合）
 *   2  三元 ?: / 二元条件 ?
 *   3  空合并 ??
 *   4  ||   5  &&   6  == !=   7  &lt; &lt;= &gt; &gt;=   8  + -   9  * /
 *   10 一元 - !
 *   11 后缀 . -> () []
 *   12 主表达式（字面量/标识符/括号/块/loop/for_each）
 * </pre>
 *
 * <p>规则：子表达式优先级 &lt; 上下文优先级时补括号；左结合运算的右子树上下文 +1；
 * 右结合（赋值/三元分支）不 +1。保证 {@code parse(emit(ast))} 结构等价。
 *
 * @author TT432
 */
public final class MolangTextEmitter {
    private MolangTextEmitter() {
    }

    private static final int PREC_ASSIGN = 1;
    private static final int PREC_TERNARY = 2;
    private static final int PREC_NULLCO = 3;
    private static final int PREC_UNARY = 10;
    private static final int PREC_POSTFIX = 11;
    private static final int PREC_PRIMARY = 12;

    private static final Map<String, Integer> BINARY_PREC = Map.ofEntries(
            Map.entry("||", 4),
            Map.entry("&&", 5),
            Map.entry("==", 6), Map.entry("!=", 6),
            Map.entry("<", 7), Map.entry("<=", 7), Map.entry(">", 7), Map.entry(">=", 7),
            Map.entry("+", 8), Map.entry("-", 8),
            Map.entry("*", 9), Map.entry("/", 9)
    );

    /** 脚本顶层：单表达式裸出；多语句分号+换行分隔、末尾补分号。 */
    public static String emitScript(MolangAst.ExprSet exprSet) {
        MolangAst.Expr root = exprSet.root();
        if (root instanceof MolangAst.BlockExpr block) {
            if (block.returnsLastValue()) {
                // 单表达式语句（带分号解析形态）镜像解析器无分号形态：裸出
                if (block.statements().size() == 1
                        && block.statements().get(0) instanceof MolangAst.ExprStmt exprStmt) {
                    return emitExpr(exprStmt.expression(), 0);
                }
                return block.statements().stream()
                        .map(MolangTextEmitter::emitStmt)
                        .collect(Collectors.joining(";\n", "", block.statements().isEmpty() ? "" : ";"));
            }
            // 顶层裸块（{ ... } 单语句）
            return emitBlock(block);
        }
        return emitExpr(root, 0);
    }

    /** 语句。 */
    public static String emitStmt(MolangAst.Stmt stmt) {
        if (stmt instanceof MolangAst.ExprStmt exprStmt) {
            return emitExpr(exprStmt.expression(), 0);
        }
        if (stmt instanceof MolangAst.ReturnStmt returnStmt) {
            return "return " + emitExpr(returnStmt.expression(), 0);
        }
        if (stmt instanceof MolangAst.BreakStmt breakStmt) {
            return breakStmt.valueExpr() == null
                    ? "break"
                    : "break " + emitExpr(breakStmt.valueExpr(), 0);
        }
        if (stmt instanceof MolangAst.ContinueStmt continueStmt) {
            return continueStmt.valueExpr() == null
                    ? "continue"
                    : "continue " + emitExpr(continueStmt.valueExpr(), 0);
        }
        throw new IllegalArgumentException("未知语句节点: " + stmt.getClass());
    }

    /** 表达式（ctxPrec = 上下文优先级；子表达式优先级更低时补括号）。 */
    public static String emitExpr(MolangAst.Expr expr, int ctxPrec) {
        String text = emitOwn(expr, ctxPrec);
        return precOf(expr) < ctxPrec ? "(" + text + ")" : text;
    }

    private static String emitOwn(MolangAst.Expr expr, int ctxPrec) {
        if (expr instanceof MolangAst.NumberLiteralExpr number) {
            return number.rawText();
        }
        if (expr instanceof MolangAst.StringLiteralExpr string) {
            return string.rawText();
        }
        if (expr instanceof MolangAst.ThisExpr) {
            return "this";
        }
        if (expr instanceof MolangAst.IdentifierExpr identifier) {
            return identifier.name();
        }
        if (expr instanceof MolangAst.UnknownExpr unknown) {
            return unknown.text();
        }
        if (expr instanceof MolangAst.BreakExpr) {
            return "break";
        }
        if (expr instanceof MolangAst.ContinueExpr) {
            return "continue";
        }
        if (expr instanceof MolangAst.UnaryExpr unary) {
            return unary.operator() + emitExpr(unary.expression(), PREC_UNARY);
        }
        if (expr instanceof MolangAst.BinaryExpr binary) {
            int prec = binaryPrec(binary.operator());
            return emitExpr(binary.left(), prec)
                    + " " + binary.operator() + " "
                    + emitExpr(binary.right(), prec + 1);
        }
        if (expr instanceof MolangAst.NullCoalesceExpr nullCoalesce) {
            return emitExpr(nullCoalesce.left(), PREC_NULLCO)
                    + " ?? "
                    + emitExpr(nullCoalesce.right(), PREC_NULLCO + 1);
        }
        if (expr instanceof MolangAst.TernaryConditionalExpr ternary) {
            return emitExpr(ternary.condition(), PREC_NULLCO)
                    + " ? " + emitExpr(ternary.whenTrue(), 0)
                    + " : " + emitExpr(ternary.whenFalse(), 0);
        }
        if (expr instanceof MolangAst.BinaryConditionalExpr binaryCond) {
            return emitExpr(binaryCond.condition(), PREC_NULLCO)
                    + " ? " + emitExpr(binaryCond.whenFalse(), 0);
        }
        if (expr instanceof MolangAst.AssignmentExpr assignment) {
            return emitExpr(assignment.target(), PREC_TERNARY)
                    + " = "
                    + emitExpr(assignment.value(), PREC_ASSIGN);
        }
        if (expr instanceof MolangAst.MemberAccessExpr memberAccess) {
            return emitExpr(memberAccess.owner(), PREC_POSTFIX) + "." + memberAccess.memberName();
        }
        if (expr instanceof MolangAst.ArrowAccessExpr arrowAccess) {
            return emitExpr(arrowAccess.left(), PREC_POSTFIX)
                    + "->" + emitExpr(arrowAccess.right(), PREC_POSTFIX);
        }
        if (expr instanceof MolangAst.CallExpr call) {
            String args = call.arguments().stream()
                    .map(arg -> emitExpr(arg, 0))
                    .collect(Collectors.joining(", "));
            return emitExpr(call.callee(), PREC_POSTFIX) + "(" + args + ")";
        }
        if (expr instanceof MolangAst.IndexExpr index) {
            return emitExpr(index.owner(), PREC_POSTFIX)
                    + "[" + emitExpr(index.index(), 0) + "]";
        }
        if (expr instanceof MolangAst.GroupingExpr grouping) {
            return "(" + emitExpr(grouping.expression(), 0) + ")";
        }
        if (expr instanceof MolangAst.BlockExpr block) {
            return emitBlock(block);
        }
        if (expr instanceof MolangAst.LoopExpr loop) {
            return "loop(" + emitExpr(loop.count(), 0) + ", " + emitBlock(loop.body()) + ")";
        }
        if (expr instanceof MolangAst.ForEachExpr forEach) {
            return "for_each(" + emitExpr(forEach.variable(), 0)
                    + ", " + emitExpr(forEach.collection(), 0)
                    + ", " + emitBlock(forEach.body()) + ")";
        }
        throw new IllegalArgumentException("未知表达式节点: " + expr.getClass());
    }

    private static String emitBlock(MolangAst.BlockExpr block) {
        if (block.statements().isEmpty()) {
            return "{}";
        }
        return block.statements().stream()
                .map(MolangTextEmitter::emitStmt)
                .collect(Collectors.joining("; ", "{ ", "; }"));
    }

    private static int precOf(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.AssignmentExpr) {
            return PREC_ASSIGN;
        }
        if (expr instanceof MolangAst.TernaryConditionalExpr
                || expr instanceof MolangAst.BinaryConditionalExpr) {
            return PREC_TERNARY;
        }
        if (expr instanceof MolangAst.NullCoalesceExpr) {
            return PREC_NULLCO;
        }
        if (expr instanceof MolangAst.BinaryExpr binary) {
            return binaryPrec(binary.operator());
        }
        if (expr instanceof MolangAst.UnaryExpr) {
            return PREC_UNARY;
        }
        if (expr instanceof MolangAst.MemberAccessExpr || expr instanceof MolangAst.ArrowAccessExpr
                || expr instanceof MolangAst.CallExpr || expr instanceof MolangAst.IndexExpr) {
            return PREC_POSTFIX;
        }
        return PREC_PRIMARY;
    }

    private static int binaryPrec(String operator) {
        Integer prec = BINARY_PREC.get(operator);
        if (prec == null) {
            throw new IllegalArgumentException("未知二元运算符: " + operator);
        }
        return prec;
    }
}
