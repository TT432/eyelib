package io.github.tt432.eyelib.molang.scratch;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.SourceSpan;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 积木树 → molang AST 导出。
 *
 * <p>口径：
 * <ul>
 *   <li>必选空插槽 → 数字 0（Scratch 空输入语义）。</li>
 *   <li>可空插槽（break/continue 值）留空 → 语法省略。</li>
 *   <li>字段非法（数字不可解析/标识符不合法/运算符未知/字符串含单引号/RAW 为空）
 *       → 抛 {@link ExportException}，由 UI 呈红，不做静默兜底。</li>
 * </ul>
 *
 * @author TT432
 */
public final class ScratchExport {
    private ScratchExport() {
    }

    /** 导出异常：携带问题积木的定位描述。 */
    public static final class ExportException extends RuntimeException {
        public ExportException(String message) {
            super(message);
        }
    }

    private static SourceSpan span() {
        return SourceSpan.unknown();
    }

    /** 语句积木 → AST 语句。 */
    public static MolangAst.Stmt stmtToAst(ScratchBlock block) {
        return switch (block.kind()) {
            case STMT_EXPR -> new MolangAst.ExprStmt(span(), exprToAst(block.socket(0)));
            case STMT_ASSIGN -> new MolangAst.ExprStmt(span(), new MolangAst.AssignmentExpr(
                    span(), exprToAst(block.requireSocket("target")), exprToAst(block.requireSocket("value"))));
            case STMT_RETURN -> new MolangAst.ReturnStmt(span(), exprToAst(block.requireSocket("value")));
            case STMT_BREAK -> new MolangAst.BreakStmt(span(), optionalExpr(block.requireSocket("value")));
            case STMT_CONTINUE -> new MolangAst.ContinueStmt(span(), optionalExpr(block.requireSocket("value")));
            case CTRL_LOOP -> new MolangAst.ExprStmt(span(), new MolangAst.LoopExpr(
                    span(), exprToAst(block.requireSocket("count")), bodyToAst(block.body())));
            case CTRL_FOREACH -> new MolangAst.ExprStmt(span(), new MolangAst.ForEachExpr(
                    span(), exprToAst(block.requireSocket("variable")),
                    exprToAst(block.requireSocket("collection")), bodyToAst(block.body())));
            case CTRL_BLOCK -> new MolangAst.ExprStmt(span(), bodyToAst(block.body()));
            default -> throw new ExportException("非语句积木不能出现在语句位置: " + block.kind());
        };
    }

    /** 语句列表 → BlockExpr。 */
    public static MolangAst.BlockExpr bodyToAst(List<ScratchBlock> body) {
        List<MolangAst.Stmt> stmts = new ArrayList<>();
        for (ScratchBlock block : body) {
            stmts.add(stmtToAst(block));
        }
        return new MolangAst.BlockExpr(span(), stmts);
    }

    /** 可选插槽：空 → null。 */
    private static MolangAst.@Nullable Expr optionalExpr(ScratchBlock.Socket socket) {
        return socket.child() == null ? null : exprToAst(socket);
    }

    /** 必选插槽：空 → 0（Scratch 空输入语义）。 */
    static MolangAst.Expr exprToAst(ScratchBlock.Socket socket) {
        if (socket.child() == null) {
            return new MolangAst.NumberLiteralExpr(span(), "0", 0.0);
        }
        return exprToAst(socket.child());
    }

    /** 表达式积木 → AST 表达式。 */
    public static MolangAst.Expr exprToAst(ScratchBlock block) {
        return switch (block.kind()) {
            case NUM -> number(block);
            case STR -> string(block);
            case THIS -> new MolangAst.ThisExpr(span());
            case VAR -> foldPath(block.field("path"), block);
            case CALL -> new MolangAst.CallExpr(span(), foldPath(block.field("name"), block),
                    callArgs(block));
            case BINARY -> binary(block);
            case UNARY -> unary(block);
            case NULLCO -> new MolangAst.NullCoalesceExpr(span(),
                    exprToAst(block.requireSocket("left")), exprToAst(block.requireSocket("right")));
            case TERNARY -> new MolangAst.TernaryConditionalExpr(span(),
                    exprToAst(block.requireSocket("cond")), exprToAst(block.requireSocket("whenTrue")),
                    exprToAst(block.requireSocket("whenFalse")));
            case COND_BINARY -> new MolangAst.BinaryConditionalExpr(span(),
                    exprToAst(block.requireSocket("cond")), exprToAst(block.requireSocket("whenFalse")));
            case MEMBER -> new MolangAst.MemberAccessExpr(span(),
                    exprToAst(block.requireSocket("owner")), ident(block.field("member"), block));
            case INDEX -> new MolangAst.IndexExpr(span(),
                    exprToAst(block.requireSocket("owner")), exprToAst(block.requireSocket("index")));
            case ARROW -> new MolangAst.ArrowAccessExpr(span(),
                    exprToAst(block.requireSocket("target")),
                    new MolangAst.MemberAccessExpr(span(),
                            new MolangAst.IdentifierExpr(span(), ident(block.field("owner"), block)),
                            ident(block.field("member"), block)));
            case RAW -> raw(block);
            default -> throw new ExportException("非表达式积木不能出现在表达式位置: " + block.kind());
        };
    }

    private static MolangAst.Expr number(ScratchBlock block) {
        String text = block.field("text").trim();
        if (text.isEmpty()) {
            return new MolangAst.NumberLiteralExpr(span(), "0", 0.0);
        }
        try {
            return new MolangAst.NumberLiteralExpr(span(), text, Double.parseDouble(text));
        } catch (NumberFormatException e) {
            throw new ExportException("数字字段不可解析: '" + text + "'");
        }
    }

    private static MolangAst.Expr string(ScratchBlock block) {
        String text = block.field("text");
        if (text.indexOf('\'') >= 0) {
            throw new ExportException("字符串字段含单引号（molang 字符串无转义）: '" + text + "'");
        }
        return new MolangAst.StringLiteralExpr(span(), "'" + text + "'");
    }

    private static MolangAst.Expr raw(ScratchBlock block) {
        String text = block.field("text").trim();
        if (text.isEmpty()) {
            throw new ExportException("原文积木内容为空");
        }
        return new MolangAst.UnknownExpr(span(), text);
    }

    private static MolangAst.Expr binary(ScratchBlock block) {
        String op = block.field("op");
        if (!ScratchKind.BINARY_OPS.contains(op)) {
            throw new ExportException("未知二元运算符: '" + op + "'");
        }
        return new MolangAst.BinaryExpr(span(), op,
                exprToAst(block.requireSocket("left")), exprToAst(block.requireSocket("right")));
    }

    private static MolangAst.Expr unary(ScratchBlock block) {
        String op = block.field("op");
        if (!ScratchKind.UNARY_OPS.contains(op)) {
            throw new ExportException("未知一元运算符: '" + op + "'");
        }
        return new MolangAst.UnaryExpr(span(), op, exprToAst(block.requireSocket("expr")));
    }

    private static List<MolangAst.Expr> callArgs(ScratchBlock block) {
        List<MolangAst.Expr> args = new ArrayList<>();
        for (ScratchBlock.Socket socket : block.sockets()) {
            args.add(exprToAst(socket));
        }
        return args;
    }

    /** 点链折叠：a.b.c → MemberAccess(MemberAccess(Identifier a, b), c)；首段 this → ThisExpr。 */
    static MolangAst.Expr foldPath(String path, ScratchBlock owner) {
        String trimmed = path.trim();
        String[] segments = trimmed.split("\\.");
        List<String> valid = new ArrayList<>();
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                valid.add(ident(segment, owner));
            }
        }
        if (valid.isEmpty()) {
            throw new ExportException("点链路径为空: '" + path + "'");
        }
        MolangAst.Expr expr = "this".equalsIgnoreCase(valid.get(0))
                ? new MolangAst.ThisExpr(span())
                : new MolangAst.IdentifierExpr(span(), valid.get(0));
        for (int i = 1; i < valid.size(); i++) {
            expr = new MolangAst.MemberAccessExpr(span(), expr, valid.get(i));
        }
        return expr;
    }

    private static String ident(String text, ScratchBlock owner) {
        String trimmed = text.trim();
        if (!trimmed.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new ExportException("非法标识符: '" + text + "'（积木 " + owner.kind() + "）");
        }
        return trimmed;
    }
}
