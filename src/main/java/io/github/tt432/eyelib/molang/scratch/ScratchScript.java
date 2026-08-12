package io.github.tt432.eyelib.molang.scratch;

import io.github.tt432.eyelib.molang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.SourceSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Scratch 脚本（积木工作区的领域门面）：顶层语句积木列表 + molang 文本双向转换。
 *
 * <p>顶层装配与解析器行为镜像：单条可表达式化语句（表达式/赋值/loop/for_each）
 * 导出为裸表达式（无分号无大括号），单条裸块导出为 {@code { ... }}，
 * 其余导出为 {@code s1;\ns2;} 语句序列。导入走
 * {@link HandwrittenMolangAstParserFrontend}（与编译器同一前端）。
 *
 * @author TT432
 */
public final class ScratchScript {
    private final List<ScratchBlock> statements = new ArrayList<>();

    /** 顶层语句积木列表（可变；UI 直接操作）。 */
    public List<ScratchBlock> statements() {
        return statements;
    }

    public static ScratchScript empty() {
        return new ScratchScript();
    }

    /** molang 文本 → 脚本；解析失败 → empty。 */
    public static Optional<ScratchScript> fromMolang(String text) {
        return new HandwrittenMolangAstParserFrontend().parseExprSetAst(text)
                .map(exprSet -> {
                    ScratchScript script = new ScratchScript();
                    script.statements.addAll(ScratchImport.importScript(exprSet));
                    return script;
                });
    }

    /** 脚本 → molang 文本；字段非法时抛 {@link ScratchExport.ExportException}。 */
    public String toMolang() {
        return MolangTextEmitter.emitScript(toExprSet());
    }

    /** 脚本 → ExprSet（顶层装配规则见类文档）。 */
    public MolangAst.ExprSet toExprSet() {
        if (statements.size() == 1) {
            ScratchBlock only = statements.get(0);
            MolangAst.Expr bare = switch (only.kind()) {
                case STMT_EXPR -> ScratchExport.exprToAst(only.requireSocket("expr"));
                case STMT_ASSIGN -> new MolangAst.AssignmentExpr(SourceSpan.unknown(),
                        ScratchExport.exprToAst(only.requireSocket("target")),
                        ScratchExport.exprToAst(only.requireSocket("value")));
                case CTRL_LOOP -> new MolangAst.LoopExpr(SourceSpan.unknown(),
                        ScratchExport.exprToAst(only.requireSocket("count")),
                        ScratchExport.bodyToAst(only.body()));
                case CTRL_FOREACH -> new MolangAst.ForEachExpr(SourceSpan.unknown(),
                        ScratchExport.exprToAst(only.requireSocket("variable")),
                        ScratchExport.exprToAst(only.requireSocket("collection")),
                        ScratchExport.bodyToAst(only.body()));
                case CTRL_BLOCK -> ScratchExport.bodyToAst(only.body());
                default -> null;
            };
            if (bare != null) {
                return new MolangAst.ExprSet(bare.span(), bare);
            }
        }
        List<MolangAst.Stmt> stmts = new ArrayList<>();
        for (ScratchBlock block : statements) {
            stmts.add(ScratchExport.stmtToAst(block));
        }
        // 顶层多语句：returnsLastValue=true（对齐解析器顶层 BlockExpr 形态）
        return new MolangAst.ExprSet(SourceSpan.unknown(),
                new MolangAst.BlockExpr(SourceSpan.unknown(), stmts, true));
    }
}
