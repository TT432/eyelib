package io.github.tt432.eyelib.molang.scratch;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * molang AST → 积木树导入。
 *
 * <p>映射口径：
 * <ul>
 *   <li>点链（标识符/this + 成员访问链）→ VAR；调用 callee 为点链 → CALL。</li>
 *   <li>括号（GroupingExpr）丢弃——发射器按优先级重建，语义保持。</li>
 *   <li>表达式位置的 loop/for_each/块/break/continue 及复杂 callee/箭头右端
 *       → RAW 原文直通（不丢信息）。</li>
 *   <li>语句位置的赋值/loop/for_each/裸块 → 对应专用语句积木。</li>
 * </ul>
 *
 * @author TT432
 */
public final class ScratchImport {
    private ScratchImport() {
    }

    /** 脚本顶层：ExprSet → 语句积木列表。 */
    public static List<ScratchBlock> importScript(MolangAst.ExprSet exprSet) {
        MolangAst.Expr root = exprSet.root();
        if (root instanceof MolangAst.BlockExpr block && block.returnsLastValue()) {
            return block.statements().stream().map(ScratchImport::importStmt).toList();
        }
        return List.of(importStmt(new MolangAst.ExprStmt(root.span(), root)));
    }

    /** 语句体（BlockExpr）→ 语句积木列表。 */
    public static List<ScratchBlock> importBody(MolangAst.BlockExpr body) {
        return body.statements().stream().map(ScratchImport::importStmt).toList();
    }

    /** 语句。 */
    public static ScratchBlock importStmt(MolangAst.Stmt stmt) {
        if (stmt instanceof MolangAst.ExprStmt exprStmt) {
            return importExprStmt(exprStmt.expression());
        }
        if (stmt instanceof MolangAst.ReturnStmt returnStmt) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.STMT_RETURN);
            block.requireSocket("value").setChild(importExpr(returnStmt.expression()));
            return block;
        }
        if (stmt instanceof MolangAst.BreakStmt breakStmt) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.STMT_BREAK);
            if (breakStmt.valueExpr() != null) {
                block.requireSocket("value").setChild(importExpr(breakStmt.valueExpr()));
            }
            return block;
        }
        if (stmt instanceof MolangAst.ContinueStmt continueStmt) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.STMT_CONTINUE);
            if (continueStmt.valueExpr() != null) {
                block.requireSocket("value").setChild(importExpr(continueStmt.valueExpr()));
            }
            return block;
        }
        return rawStmt(stmt);
    }

    private static ScratchBlock importExprStmt(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.AssignmentExpr assignment) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.STMT_ASSIGN);
            block.requireSocket("target").setChild(importExpr(assignment.target()));
            block.requireSocket("value").setChild(importExpr(assignment.value()));
            return block;
        }
        if (expr instanceof MolangAst.LoopExpr loop) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.CTRL_LOOP);
            block.requireSocket("count").setChild(importExpr(loop.count()));
            block.body().addAll(importBody(loop.body()));
            return block;
        }
        if (expr instanceof MolangAst.ForEachExpr forEach) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.CTRL_FOREACH);
            block.requireSocket("variable").setChild(importExpr(forEach.variable()));
            block.requireSocket("collection").setChild(importExpr(forEach.collection()));
            block.body().addAll(importBody(forEach.body()));
            return block;
        }
        if (expr instanceof MolangAst.BlockExpr blockExpr) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.CTRL_BLOCK);
            block.body().addAll(importBody(blockExpr));
            return block;
        }
        ScratchBlock block = ScratchBlock.of(ScratchKind.STMT_EXPR);
        block.requireSocket("expr").setChild(importExpr(expr));
        return block;
    }

    /** 表达式（null 返回不可能——一切节点都有映射，RAW 兜底）。 */
    public static ScratchBlock importExpr(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.NumberLiteralExpr number) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.NUM);
            block.setField("text", number.rawText());
            return block;
        }
        if (expr instanceof MolangAst.StringLiteralExpr string) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.STR);
            block.setField("text", stripQuotes(string.rawText()));
            return block;
        }
        if (expr instanceof MolangAst.ThisExpr) {
            return ScratchBlock.of(ScratchKind.THIS);
        }
        if (expr instanceof MolangAst.IdentifierExpr || expr instanceof MolangAst.MemberAccessExpr) {
            String path = memberPath(expr);
            if (path != null) {
                ScratchBlock block = ScratchBlock.of(ScratchKind.VAR);
                block.setField("path", path);
                return block;
            }
            MolangAst.MemberAccessExpr memberAccess = (MolangAst.MemberAccessExpr) expr;
            ScratchBlock block = ScratchBlock.of(ScratchKind.MEMBER);
            block.requireSocket("owner").setChild(importExpr(memberAccess.owner()));
            block.setField("member", memberAccess.memberName());
            return block;
        }
        if (expr instanceof MolangAst.CallExpr call) {
            String calleePath = memberPath(call.callee());
            if (calleePath == null) {
                return rawExpr(expr);
            }
            ScratchBlock block = ScratchBlock.of(ScratchKind.CALL);
            block.setField("name", calleePath);
            for (MolangAst.Expr arg : call.arguments()) {
                block.addSocket("arg").setChild(importExpr(arg));
            }
            return block;
        }
        if (expr instanceof MolangAst.UnaryExpr unary) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.UNARY);
            block.setField("op", unary.operator());
            block.requireSocket("expr").setChild(importExpr(unary.expression()));
            return block;
        }
        if (expr instanceof MolangAst.BinaryExpr binary) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.BINARY);
            block.setField("op", binary.operator());
            block.requireSocket("left").setChild(importExpr(binary.left()));
            block.requireSocket("right").setChild(importExpr(binary.right()));
            return block;
        }
        if (expr instanceof MolangAst.NullCoalesceExpr nullCoalesce) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.NULLCO);
            block.requireSocket("left").setChild(importExpr(nullCoalesce.left()));
            block.requireSocket("right").setChild(importExpr(nullCoalesce.right()));
            return block;
        }
        if (expr instanceof MolangAst.TernaryConditionalExpr ternary) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.TERNARY);
            block.requireSocket("cond").setChild(importExpr(ternary.condition()));
            block.requireSocket("whenTrue").setChild(importExpr(ternary.whenTrue()));
            block.requireSocket("whenFalse").setChild(importExpr(ternary.whenFalse()));
            return block;
        }
        if (expr instanceof MolangAst.BinaryConditionalExpr binaryCond) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.COND_BINARY);
            block.requireSocket("cond").setChild(importExpr(binaryCond.condition()));
            block.requireSocket("whenFalse").setChild(importExpr(binaryCond.whenFalse()));
            return block;
        }
        if (expr instanceof MolangAst.IndexExpr index) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.INDEX);
            block.requireSocket("owner").setChild(importExpr(index.owner()));
            block.requireSocket("index").setChild(importExpr(index.index()));
            return block;
        }
        if (expr instanceof MolangAst.ArrowAccessExpr arrowAccess) {
            if (arrowAccess.right() instanceof MolangAst.MemberAccessExpr right
                    && right.owner() instanceof MolangAst.IdentifierExpr owner) {
                ScratchBlock block = ScratchBlock.of(ScratchKind.ARROW);
                block.requireSocket("target").setChild(importExpr(arrowAccess.left()));
                block.setField("owner", owner.name());
                block.setField("member", right.memberName());
                return block;
            }
            return rawExpr(expr);
        }
        if (expr instanceof MolangAst.GroupingExpr grouping) {
            // 括号丢弃：发射器按优先级重建，语义保持
            return importExpr(grouping.expression());
        }
        if (expr instanceof MolangAst.UnknownExpr unknown) {
            ScratchBlock block = ScratchBlock.of(ScratchKind.RAW);
            block.setField("text", unknown.text());
            return block;
        }
        // 表达式位置的 loop / for_each / 块 / break / continue / 赋值
        return rawExpr(expr);
    }

    /** 点链展开：Identifier/This + MemberAccess 链 → "a.b.c"；其他形态 → null。 */
    private static @Nullable String memberPath(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.IdentifierExpr identifier) {
            return identifier.name();
        }
        if (expr instanceof MolangAst.ThisExpr) {
            return "this";
        }
        if (expr instanceof MolangAst.MemberAccessExpr memberAccess) {
            String ownerPath = memberPath(memberAccess.owner());
            return ownerPath == null ? null : ownerPath + "." + memberAccess.memberName();
        }
        return null;
    }

    private static String stripQuotes(String rawText) {
        if (rawText.length() >= 2 && rawText.startsWith("'") && rawText.endsWith("'")) {
            return rawText.substring(1, rawText.length() - 1);
        }
        return rawText;
    }

    private static ScratchBlock rawExpr(MolangAst.Expr expr) {
        ScratchBlock block = ScratchBlock.of(ScratchKind.RAW);
        block.setField("text", MolangTextEmitter.emitExpr(expr, 0));
        return block;
    }

    private static ScratchBlock rawStmt(MolangAst.Stmt stmt) {
        ScratchBlock raw = ScratchBlock.of(ScratchKind.RAW);
        raw.setField("text", MolangTextEmitter.emitStmt(stmt));
        ScratchBlock block = ScratchBlock.of(ScratchKind.STMT_EXPR);
        block.requireSocket("expr").setChild(raw);
        return block;
    }
}
