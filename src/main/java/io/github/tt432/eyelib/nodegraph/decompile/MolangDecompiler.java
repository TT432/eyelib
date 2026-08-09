package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.compiler.common.MolangRootAliasCanonicalizer;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangParserFrontends;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.SourceSpan;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Molang 反编译器（规格 nodegraph-workbench §W2）：molang 文本 → MolangAst → 图 IR 片段。
 * 纯函数：同输入同输出，坐标一律 0（布局见 {@link GraphLayout}）。
 *
 * <p>映射表（规格 §W2）：
 * <ul>
 *   <li>数字字面量 → const.number（整数值 → const.int）；字符串 → const.string；true/false → const.bool；
 *       例外：赋值语句的值是常量字面量时不建 const 节点，直接内联为 set 节点 value 端口的行内值；</li>
 *   <li>{@code a op b} / {@code op a} / {@code c ? a : b} / {@code a ?? b}
 *       → op.binary / op.unary / op.ternary / op.null_coalesce；</li>
 *   <li>{@code variable.x} → variable 节点（name 不带根）；{@code temp.x} / {@code context.x}
 *       → temp.get / context.get
 *       （q./v./t./c. 别名经 {@link MolangRootAliasCanonicalizer} 归一，temp/context 的 name 存带根全名）；</li>
 *   <li>{@code query.f(...)} / {@code math.f(...)} → query.call / math.call
 *       （function + arg_count + argN 连线；无参成员访问形 {@code query.f} 同样归为 arg_count=0 的调用）；</li>
 *   <li>{@code geometry.x} / {@code texture.x} / {@code material.x} → ref.geometry / ref.texture / ref.material
 *       （short_name；assembler 在表达式槽正是这样发射的，见 VALUE_REFS）；</li>
 *   <li>{@code variable.x = e;} → exec.set_var + variable 节点连线 target；{@code temp.x = e;} → exec.set_temp；
 *       {@code loop(n, {…})} / {@code for_each(v, arr, {…})} → exec.loop / exec.for_each；
 *       break/continue/return → exec.break / exec.continue / exec.return；
 *       query/math 调用语句 → exec.call；裸语句块拍平进当前链；</li>
 *   <li>语句序列（分号）→ exec 链：前驱 exec_out → 后继 exec_in，<b>槽口连链尾</b>
 *       （反向汇入槽模型，与 {@code EmitSession.resolveChain} 的遍历方向互为逆）。</li>
 * </ul>
 *
 * <p><b>不支持</b>（arrow {@code a->b}、下标 {@code a[i]}、属主非根标识符的 member access、
 * this、二元条件简写 {@code c ?: b}、值上下文中的赋值/loop/块等）：
 * UNSUPPORTED_IMPORT 诊断 + StickyNote 保留原文 + const.number 0 占位连线（语句上下文不产生占位节点），
 * 不中断其余部分。
 */
public final class MolangDecompiler {
    private MolangDecompiler() {
    }

    /** call 节点 arg_count 上限（与 {@code NodeTypes.callArgPorts} 的 16 一致）。 */
    private static final int MAX_CALL_ARGS = 16;

    /**
     * 表达式反编译产物。
     *
     * @param output 产物输出端口（连到消费值槽的源；解析失败/不支持时为 const.number 0 占位）
     */
    public record ExprFragment(
            List<NodeInstance> nodes,
            List<Wire> wires,
            List<StickyNote> stickyNotes,
            List<Diagnostic> diagnostics,
            Optional<PortRef> output) {
    }

    /**
     * 语句序列反编译产物。
     *
     * @param chain exec 链节点 uid（链首→链尾；空 = 无有效语句）
     */
    public record ExecFragment(
            List<NodeInstance> nodes,
            List<Wire> wires,
            List<StickyNote> stickyNotes,
            List<Diagnostic> diagnostics,
            List<String> chain) {
        /** 链尾 exec_out（连到消费 EXEC 槽的源）。 */
        public Optional<PortRef> tail() {
            return chain.isEmpty()
                    ? Optional.empty()
                    : Optional.of(new PortRef(chain.get(chain.size() - 1), "exec_out"));
        }
    }

    // ---------- 入口 ----------

    /** molang 表达式 → 图片段。语句序列输入（多块/带分号）→ UNSUPPORTED_IMPORT + 占位。 */
    public static ExprFragment decompileExpression(String source) {
        Builder b = new Builder(source);
        Optional<MolangAst.ExprSet> ast = MolangParserFrontends.active().parseExprSet(source).ast();
        if (ast.isEmpty()) {
            b.sticky(source);
            b.warn(DecompileDiagnostics.PARSE_FAILURE, "molang 解析失败：" + abbreviate(source));
            return new ExprFragment(b.nodes, b.wires, b.stickies, b.diagnostics,
                    Optional.of(b.placeholder()));
        }
        MolangAst.Expr root = ast.get().root();
        if (root instanceof MolangAst.BlockExpr) {
            b.unsupportedNote(root, "语句序列不能作为表达式导入");
            return new ExprFragment(b.nodes, b.wires, b.stickies, b.diagnostics,
                    Optional.of(b.placeholder()));
        }
        PortRef out = expr(b, root);
        return new ExprFragment(b.nodes, b.wires, b.stickies, b.diagnostics, Optional.of(out));
    }

    /** molang 语句序列 → exec 链片段（单表达式输入按单语句处理）。 */
    public static ExecFragment decompileStatements(String source) {
        Builder b = new Builder(source);
        Optional<MolangAst.ExprSet> ast = MolangParserFrontends.active().parseExprSet(source).ast();
        if (ast.isEmpty()) {
            b.sticky(source);
            b.warn(DecompileDiagnostics.PARSE_FAILURE, "molang 解析失败：" + abbreviate(source));
            return new ExecFragment(b.nodes, b.wires, b.stickies, b.diagnostics, List.of());
        }
        MolangAst.Expr root = ast.get().root();
        List<MolangAst.Stmt> statements = root instanceof MolangAst.BlockExpr block
                ? block.statements()
                : List.of(new MolangAst.ExprStmt(root.span(), root));
        List<String> chain = new ArrayList<>();
        for (MolangAst.Stmt stmt : statements) {
            statement(b, stmt, chain);
        }
        linkChain(b, chain);
        return new ExecFragment(b.nodes, b.wires, b.stickies, b.diagnostics, List.copyOf(chain));
    }

    // ---------- 语句 ----------

    private static void statement(Builder b, MolangAst.Stmt stmt, List<String> chain) {
        if (stmt instanceof MolangAst.ExprStmt exprStmt) {
            exprStatement(b, exprStmt.expression(), chain);
        } else if (stmt instanceof MolangAst.ReturnStmt ret) {
            String uid = b.addNode("exec.return", Map.of());
            b.wireFrom(expr(b, ret.expression()), uid, "value");
            chain.add(uid);
        } else if (stmt instanceof MolangAst.BreakStmt brk) {
            if (brk.valueExpr() != null) {
                b.unsupportedNote(brk, "break 带值（图无法表达返回值）");
            }
            chain.add(b.addNode("exec.break", Map.of()));
        } else if (stmt instanceof MolangAst.ContinueStmt cont) {
            if (cont.valueExpr() != null) {
                b.unsupportedNote(cont, "continue 带值（图无法表达返回值）");
            }
            chain.add(b.addNode("exec.continue", Map.of()));
        }
    }

    private static void exprStatement(Builder b, MolangAst.Expr expr, List<String> chain) {
        if (expr instanceof MolangAst.AssignmentExpr assignment) {
            assignment(b, assignment, chain);
        } else if (expr instanceof MolangAst.LoopExpr loop) {
            String uid = b.addNode("exec.loop", Map.of());
            b.wireFrom(expr(b, loop.count()), uid, "count");
            body(b, loop.body(), uid);
            chain.add(uid);
        } else if (expr instanceof MolangAst.ForEachExpr forEach) {
            forEach(b, forEach, chain);
        } else if (expr instanceof MolangAst.BlockExpr block) {
            // 裸语句块：语义 = 顺序执行，拍平进当前链
            for (MolangAst.Stmt s : block.statements()) {
                statement(b, s, chain);
            }
        } else if (expr instanceof MolangAst.CallExpr call && callRoot(call) != null) {
            chain.add(callNode(b, "exec.call", callRoot(call), call.arguments()));
        } else if (expr instanceof MolangAst.TernaryConditionalExpr ternary
                && conditionalAssignments(b, ternary.condition(), ternary.whenTrue(), ternary.whenFalse(), chain)) {
            // c ? {赋值块} : {赋值块} → 条件赋值脱糖（见 conditionalAssignments）
        } else if (expr instanceof MolangAst.BinaryConditionalExpr binary
                && conditionalAssignments(b, binary.condition(), binary.whenFalse(), null, chain)) {
            // c ? {赋值块}（record 字段名 whenFalse 实为 then 分支）→ 同上，else 侧缺省
        } else {
            b.unsupportedNote(expr, "无法作为语句导入的表达式");
        }
    }

    /**
     * 条件赋值脱糖（molang 无 if 语句，`c?{...}` 是惯用法）：语句序列符号执行——
     * 顺序赋值直接覆盖，条件分支按变量折叠为嵌套三元（缺省侧取自引用，`v=v` 为无操作）。
     * 例：`c1?{v=1; c2?{v=2;}}` → `v = c1 ? (c2 ? 2 : 1) : v`。
     * 仅当全部语句都是 variable./temp. 赋值或（嵌套的）条件赋值块时适用；否则返回 false 走 UNSUPPORTED。
     */
    private static boolean conditionalAssignments(Builder b, MolangAst.Expr condition,
                                                  MolangAst.@Nullable Expr whenTrue,
                                                  MolangAst.@Nullable Expr whenFalse,
                                                  List<String> chain) {
        Map<String, MolangAst.Expr> thenValues = execAssigns(statementsOf(whenTrue));
        Map<String, MolangAst.Expr> elseValues = execAssigns(statementsOf(whenFalse));
        if (thenValues == null || elseValues == null
                || (thenValues.isEmpty() && elseValues.isEmpty())) {
            return false;
        }
        Set<String> vars = new LinkedHashSet<>(thenValues.keySet());
        vars.addAll(elseValues.keySet());
        for (String varKey : vars) {
            MolangAst.Expr thenValue = thenValues.getOrDefault(varKey, selfRefAst(varKey));
            MolangAst.Expr elseValue = elseValues.getOrDefault(varKey, selfRefAst(varKey));
            String ternary = b.addNode("op.ternary", Map.of());
            b.wireFrom(expr(b, condition), ternary, "cond");
            b.wireFrom(expr(b, thenValue), ternary, "a");
            b.wireFrom(expr(b, elseValue), ternary, "b");
            String root = varKey.substring(0, varKey.indexOf('.'));
            // name 与 assignment() 一致：variable 根存不带根名，temp 根存带根全名
            String setVar = addSetNode(b, root, varKey);
            b.wire(ternary, "out", setVar, "value");
            chain.add(setVar);
        }
        return true;
    }

    /** 分支 → 语句列表（null=空；BlockExpr=其语句；单赋值=单语句）。 */
    private static List<MolangAst.Stmt> statementsOf(MolangAst.@Nullable Expr branch) {
        if (branch == null) {
            return List.of();
        }
        if (branch instanceof MolangAst.BlockExpr block) {
            return block.statements();
        }
        if (branch instanceof MolangAst.AssignmentExpr) {
            return List.of(new MolangAst.ExprStmt(branch.span(), branch));
        }
        return List.of(new MolangAst.ExprStmt(SourceSpan.unknown(), branch));
    }

    /**
     * 语句序列符号执行：赋值覆盖；`c?{T}`/`c?{T}:{F}` 分支递归后对每个变量折叠
     * `v = c ? 分支T值 : 分支F值`（未触及侧回落执行前状态）；任何其它语句 → null。
     */
    private static @Nullable Map<String, MolangAst.Expr> execAssigns(List<MolangAst.Stmt> statements) {
        Map<String, MolangAst.Expr> values = new LinkedHashMap<>();
        for (MolangAst.Stmt stmt : statements) {
            if (!(stmt instanceof MolangAst.ExprStmt exprStmt)) {
                return null;
            }
            MolangAst.Expr e = exprStmt.expression();
            if (e instanceof MolangAst.AssignmentExpr assignment) {
                String key = assignmentKey(assignment);
                if (key == null) {
                    return null;
                }
                values.put(key, assignment.value());
            } else if (e instanceof MolangAst.TernaryConditionalExpr ternary) {
                Map<String, MolangAst.Expr> thenV = execAssigns(statementsOf(ternary.whenTrue()));
                Map<String, MolangAst.Expr> elseV = execAssigns(statementsOf(ternary.whenFalse()));
                if (thenV == null || elseV == null) {
                    return null;
                }
                Set<String> branchVars = new LinkedHashSet<>(thenV.keySet());
                branchVars.addAll(elseV.keySet());
                for (String v : branchVars) {
                    MolangAst.Expr a = thenV.getOrDefault(v, values.getOrDefault(v, selfRefAst(v)));
                    MolangAst.Expr bv = elseV.getOrDefault(v, values.getOrDefault(v, selfRefAst(v)));
                    values.put(v, new MolangAst.TernaryConditionalExpr(ternary.span(), ternary.condition(), a, bv));
                }
            } else if (e instanceof MolangAst.BinaryConditionalExpr binary) {
                // record 字段名 whenFalse 实为 then 分支（parser 简写 c?{...} 无 else）
                Map<String, MolangAst.Expr> thenV = execAssigns(statementsOf(binary.whenFalse()));
                if (thenV == null) {
                    return null;
                }
                for (var entry : thenV.entrySet()) {
                    String v = entry.getKey();
                    MolangAst.Expr base = values.getOrDefault(v, selfRefAst(v));
                    values.put(v, new MolangAst.TernaryConditionalExpr(binary.span(), binary.condition(), entry.getValue(), base));
                }
            } else {
                return null;
            }
        }
        return values;
    }

    /** 赋值目标 key（root.path）；非 variable./temp. 或不可解析 → null。 */
    private static @Nullable String assignmentKey(MolangAst.AssignmentExpr assignment) {
        QualifiedName target = resolve(assignment.target());
        if (target == null || !(target.root().equals("variable") || target.root().equals("temp"))) {
            return null;
        }
        return target.root() + "." + target.path();
    }

    /** 自引用 AST（variable.x → member access 链；供三元缺省侧与表达式内嵌）。 */
    private static MolangAst.Expr selfRefAst(String varKey) {
        int dot = varKey.indexOf('.');
        MolangAst.Expr current = new MolangAst.IdentifierExpr(SourceSpan.unknown(), varKey.substring(0, dot));
        for (String segment : varKey.substring(dot + 1).split("\\.")) {
            current = new MolangAst.MemberAccessExpr(SourceSpan.unknown(), current, segment);
        }
        return current;
    }

    private static void assignment(Builder b, MolangAst.AssignmentExpr assignment, List<String> chain) {
        QualifiedName target = resolve(assignment.target());
        if (target == null || !(target.root().equals("variable") || target.root().equals("temp"))) {
            b.unsupportedNote(assignment, "赋值目标不是 variable./temp. 成员");
            return;
        }
        String uid = addSetNode(b, target.root(), target.qualified());
        JsonElement inline = inlineConstant(assignment.value());
        if (inline != null) {
            // 常量赋值不建 const 节点：内联为 value 端口行内值（编辑器可直接改，codegen 经 literal 发射）
            b.putConstant(uid, "value", inline);
        } else {
            b.wireFrom(expr(b, assignment.value()), uid, "value");
        }
        chain.add(uid);
    }

    /**
     * 赋值节点发射：variable 根 → exec.set_var + variable 节点连线 target（写身份走引脚）；
     * temp 根 → exec.set_temp（name 存带根全名）。返回 set 节点 uid。
     */
    private static String addSetNode(Builder b, String root, String qualified) {
        if (root.equals("variable")) {
            String name = qualified.substring("variable.".length());
            String varNode = b.addNode("variable", ImportGraphBuilder.opts("name", name));
            String set = b.addNode("exec.set_var", Map.of());
            // v9 左读右写：set.target（右侧输出）→ variable 节点 in（写入通道）
            b.wire(set, "target", varNode, "in");
            return set;
        }
        return b.addNode("exec.set_temp", ImportGraphBuilder.opts("name", qualified));
    }

    /** 赋值值是常量字面量 → 行内 JSON（分组解包；单目 +/- 折叠进数字）；非常量 → null。 */
    private static @Nullable JsonElement inlineConstant(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.GroupingExpr grouping) {
            return inlineConstant(grouping.expression());
        }
        if (expr instanceof MolangAst.NumberLiteralExpr number) {
            return numberJson(number.value(), number.rawText());
        }
        if (expr instanceof MolangAst.UnaryExpr unary
                && (unary.operator().equals("-") || unary.operator().equals("+"))) {
            MolangAst.Expr operand = unary.expression();
            if (operand instanceof MolangAst.GroupingExpr grouping) {
                operand = grouping.expression();
            }
            if (operand instanceof MolangAst.NumberLiteralExpr number) {
                double signed = unary.operator().equals("-") ? -number.value() : number.value();
                return numberJson(signed, number.rawText());
            }
            return null;
        }
        if (expr instanceof MolangAst.StringLiteralExpr string) {
            return new JsonPrimitive(unquote(string.rawText()));
        }
        if (expr instanceof MolangAst.IdentifierExpr identifier) {
            String name = identifier.name().toLowerCase(Locale.ROOT);
            if (name.equals("true") || name.equals("false")) {
                return new JsonPrimitive(name.equals("true"));
            }
        }
        return null;
    }

    private static void forEach(Builder b, MolangAst.ForEachExpr forEach, List<String> chain) {
        String varName;
        QualifiedName var = resolve(forEach.variable());
        if (var != null && (var.root().equals("temp") || var.root().equals("variable"))) {
            varName = var.qualified();
        } else if (forEach.variable() instanceof MolangAst.IdentifierExpr id) {
            // 裸标识符：codegen 回落 temp. 前缀，与源语义一致
            varName = id.name();
        } else {
            b.unsupportedNote(forEach, "for_each 变量不是 temp./variable. 成员");
            return;
        }
        String uid = b.addNode("exec.for_each", ImportGraphBuilder.opts("var_name", varName));
        b.wireFrom(expr(b, forEach.collection()), uid, "array");
        body(b, forEach.body(), uid);
        chain.add(uid);
    }

    /** loop/for_each 的 body：子语句链，链尾 exec_out → 属主 body 槽。 */
    private static void body(Builder b, MolangAst.BlockExpr bodyBlock, String ownerUid) {
        List<String> subChain = new ArrayList<>();
        for (MolangAst.Stmt s : bodyBlock.statements()) {
            statement(b, s, subChain);
        }
        linkChain(b, subChain);
        if (!subChain.isEmpty()) {
            b.wire(subChain.get(subChain.size() - 1), "exec_out", ownerUid, "body");
        }
    }

    /** exec 链连线：前驱 exec_out → 后继 exec_in（槽口连链尾，同 EmitSession.resolveChain 语义）。 */
    private static void linkChain(Builder b, List<String> chain) {
        for (int i = 1; i < chain.size(); i++) {
            b.wire(chain.get(i - 1), "exec_out", chain.get(i), "exec_in");
        }
    }

    // ---------- 表达式 ----------

    private static PortRef expr(Builder b, MolangAst.Expr expr) {
        if (expr instanceof MolangAst.NumberLiteralExpr number) {
            return numberLiteral(b, number);
        }
        if (expr instanceof MolangAst.StringLiteralExpr string) {
            return b.valueNode("const.string",
                    ImportGraphBuilder.opts("value", unquote(string.rawText())), "out");
        }
        if (expr instanceof MolangAst.IdentifierExpr identifier) {
            String name = identifier.name().toLowerCase(Locale.ROOT);
            if (name.equals("true") || name.equals("false")) {
                return b.valueNode("const.bool",
                        ImportGraphBuilder.opts("value", name.equals("true")), "out");
            }
            return b.unsupported(identifier, "裸标识符 '" + identifier.name() + "'");
        }
        if (expr instanceof MolangAst.GroupingExpr grouping) {
            // 图 IR 无括号节点；codegen 全括号化，括号在往返中自然消解
            return expr(b, grouping.expression());
        }
        if (expr instanceof MolangAst.UnaryExpr unary) {
            String uid = b.addNode("op.unary", ImportGraphBuilder.opts("op", unary.operator()));
            b.wireFrom(expr(b, unary.expression()), uid, "a");
            return new PortRef(uid, "out");
        }
        if (expr instanceof MolangAst.BinaryExpr binary) {
            String uid = b.addNode("op.binary", ImportGraphBuilder.opts("op", binary.operator()));
            b.wireFrom(expr(b, binary.left()), uid, "a");
            b.wireFrom(expr(b, binary.right()), uid, "b");
            return new PortRef(uid, "out");
        }
        if (expr instanceof MolangAst.TernaryConditionalExpr ternary) {
            String uid = b.addNode("op.ternary", Map.of());
            b.wireFrom(expr(b, ternary.condition()), uid, "cond");
            b.wireFrom(expr(b, ternary.whenTrue()), uid, "a");
            b.wireFrom(expr(b, ternary.whenFalse()), uid, "b");
            return new PortRef(uid, "out");
        }
        if (expr instanceof MolangAst.NullCoalesceExpr nullCoalesce) {
            String uid = b.addNode("op.null_coalesce", Map.of());
            b.wireFrom(expr(b, nullCoalesce.left()), uid, "a");
            b.wireFrom(expr(b, nullCoalesce.right()), uid, "b");
            return new PortRef(uid, "out");
        }
        if (expr instanceof MolangAst.MemberAccessExpr member) {
            return memberAccess(b, member);
        }
        if (expr instanceof MolangAst.CallExpr call) {
            QualifiedName qn = callRoot(call);
            if (qn == null || call.arguments().size() > MAX_CALL_ARGS) {
                return b.unsupported(call, qn == null
                        ? "调用目标不是 query./math. 成员"
                        : "调用参数超过 " + MAX_CALL_ARGS + " 个");
            }
            String type = qn.root().equals("math") ? "math.call" : "query.call";
            return new PortRef(callNode(b, type, qn, call.arguments()), "out");
        }
        return b.unsupported(expr, describe(expr));
    }

    private static PortRef memberAccess(Builder b, MolangAst.MemberAccessExpr member) {
        QualifiedName qn = resolve(member);
        if (qn == null) {
            return b.unsupported(member, "member access 属主不是根标识符");
        }
        return switch (qn.root()) {
            case "variable" -> b.valueNode("variable",
                    ImportGraphBuilder.opts("name", qn.path()), "out");
            case "temp" -> b.valueNode("temp.get",
                    ImportGraphBuilder.opts("name", qn.qualified()), "out");
            case "context" -> b.valueNode("context.get",
                    ImportGraphBuilder.opts("name", qn.qualified()), "out");
            case "query" -> b.valueNode("query.call", ImportGraphBuilder.opts(
                    "function", qn.qualified(), "arg_count", 0), "out");
            case "math" -> b.valueNode("math.call", ImportGraphBuilder.opts(
                    "function", qn.qualified(), "arg_count", 0), "out");
            // RC 表达式槽的资源引用（assembler VALUE_REFS 的逆）：仅有 short_name 信息
            case "geometry" -> b.valueNode("ref.geometry",
                    ImportGraphBuilder.opts("short_name", qn.path()), "ref");
            case "texture" -> b.valueNode("ref.texture",
                    ImportGraphBuilder.opts("short_name", qn.path()), "ref");
            case "material" -> b.valueNode("ref.material",
                    ImportGraphBuilder.opts("short_name", qn.path()), "ref");
            default -> b.unsupported(member, "未知根 '" + qn.root() + "'");
        };
    }

    /** query.call / math.call / exec.call 公共构造：function + arg_count + argN 连线。 */
    private static String callNode(Builder b, String type, @Nullable QualifiedName qn,
                                   List<MolangAst.Expr> args) {
        String uid = b.addNode(type, ImportGraphBuilder.opts(
                "function", qn == null ? "" : qn.qualified(),
                "arg_count", args.size()));
        for (int i = 0; i < args.size(); i++) {
            b.wireFrom(expr(b, args.get(i)), uid, "arg" + (i + 1));
        }
        return uid;
    }

    /** 调用形（callee 为 query/math 根成员）→ 归一限定名；否则 null。 */
    private static @Nullable QualifiedName callRoot(MolangAst.CallExpr call) {
        QualifiedName qn = resolve(call.callee());
        return qn != null && (qn.root().equals("query") || qn.root().equals("math")) ? qn : null;
    }

    private static PortRef numberLiteral(Builder b, MolangAst.NumberLiteralExpr number) {
        return isIntegral(number.value(), number.rawText())
                ? b.valueNode("const.int", ImportGraphBuilder.opts("value", (long) number.value()), "out")
                : b.valueNode("const.number", ImportGraphBuilder.opts("value", number.value()), "out");
    }

    /** 数字 → JSON 字面值：整数值保持 long（与 const.int 同口径），否则 double。 */
    private static JsonElement numberJson(double value, String rawText) {
        return new JsonPrimitive(isIntegral(value, rawText) ? (Number) (long) value : value);
    }

    /** 整数判据：raw 无小数点/指数记号且值可精确表示为 long。 */
    private static boolean isIntegral(double value, String rawText) {
        String raw = rawText.toLowerCase(Locale.ROOT);
        return !raw.contains(".") && !raw.contains("e")
                && value == Math.rint(value)
                && Math.abs(value) < 9.0e15;
    }

    /** 字符串字面量：rawText 含单引号；反转义 \\ 与 \'（codegen quote 的逆）。 */
    static String unquote(String rawText) {
        String body = rawText.length() >= 2 && rawText.startsWith("'") && rawText.endsWith("'")
                ? rawText.substring(1, rawText.length() - 1)
                : rawText;
        StringBuilder out = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char next = body.charAt(i + 1);
                if (next == '\\' || next == '\'') {
                    out.append(next);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    // ---------- 限定名解析 ----------

    /**
     * member access 链 → 归一限定名（根经别名规范化；{@code variable.foo.bar} → root=variable, path=foo.bar）。
     * 属主不是根标识符（调用结果/下标等）或非 member access → null。
     */
    private static @Nullable QualifiedName resolve(MolangAst.Expr expr) {
        List<String> segments = new ArrayList<>();
        MolangAst.Expr current = expr;
        while (current instanceof MolangAst.MemberAccessExpr member) {
            segments.add(0, member.memberName());
            current = member.owner();
        }
        if (segments.isEmpty() || !(current instanceof MolangAst.IdentifierExpr identifier)) {
            return null;
        }
        String root = MolangRootAliasCanonicalizer.canonicalizeRoot(identifier.name());
        return new QualifiedName(root, String.join(".", segments));
    }

    /** @param root 归一根名；@param path 点分成员路径（不含根） */
    private record QualifiedName(String root, String path) {
        /** 带根全名（var.get/temp.get/context.get 的 name、query/math.call 的 function 选项用）。 */
        String qualified() {
            return root + "." + path;
        }
    }

    private static String describe(MolangAst.Expr expr) {
        if (expr instanceof MolangAst.ArrowAccessExpr) return "arrow 访问（a->b）";
        if (expr instanceof MolangAst.IndexExpr) return "数组下标（a[i]）";
        if (expr instanceof MolangAst.ThisExpr) return "this";
        if (expr instanceof MolangAst.BinaryConditionalExpr) return "二元条件简写（c ?: b）";
        if (expr instanceof MolangAst.AssignmentExpr) return "值上下文中的赋值";
        if (expr instanceof MolangAst.LoopExpr || expr instanceof MolangAst.ForEachExpr) {
            return "值上下文中的 loop/for_each";
        }
        if (expr instanceof MolangAst.BlockExpr) return "值上下文中的语句块";
        if (expr instanceof MolangAst.BreakExpr || expr instanceof MolangAst.ContinueExpr) {
            return "值上下文中的 break/continue";
        }
        if (expr instanceof MolangAst.UnknownExpr) return "未识别表达式";
        return expr.getClass().getSimpleName();
    }

    private static String abbreviate(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 80 ? oneLine : oneLine.substring(0, 77) + "...";
    }

    // ---------- 片段构造 ----------

    /** 单次反编译的累积器：确定性 uid（n0/n1… 节点、s0/s1… 便签）、坐标一律 0。 */
    private static final class Builder {
        final String source;
        final List<NodeInstance> nodes = new ArrayList<>();
        final List<Wire> wires = new ArrayList<>();
        final List<StickyNote> stickies = new ArrayList<>();
        final List<Diagnostic> diagnostics = new ArrayList<>();
        int nodeSeq;
        int stickySeq;

        Builder(String source) {
            this.source = source;
        }

        String addNode(String type, Map<String, JsonElement> options) {
            String uid = "n" + nodeSeq++;
            nodes.add(new NodeInstance(uid, type, 0, 0, options, Map.of()));
            return uid;
        }

        PortRef valueNode(String type, Map<String, JsonElement> options, String outPort) {
            return new PortRef(addNode(type, options), outPort);
        }

        /** 未连线端口行内值（codegen 经 literal 发射；与 ImportGraphBuilder.putConstant 同语义）。 */
        void putConstant(String nodeUid, String portId, JsonElement value) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                NodeInstance n = nodes.get(i);
                if (n.uid().equals(nodeUid)) {
                    Map<String, JsonElement> constants = new LinkedHashMap<>(n.constants());
                    constants.put(portId, value);
                    nodes.set(i, new NodeInstance(n.uid(), n.type(), n.x(), n.y(), n.options(), constants));
                    return;
                }
            }
        }

        void wire(String fromNode, String fromPort, String toNode, String toPort) {
            wires.add(new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort)));
        }

        void wireFrom(PortRef from, String toNode, String toPort) {
            wire(from.node(), from.port(), toNode, toPort);
        }

        /** 不支持的结构（值上下文）：诊断 + 便签留原文 + const.number 0 占位连线。 */
        PortRef unsupported(MolangAst.Node node, String description) {
            String text = sourceText(node.span());
            PortRef placeholder = placeholder();
            warn(DecompileDiagnostics.UNSUPPORTED_IMPORT,
                    "不支持的 molang 结构（" + description + "），原文已保留在便签：" + abbreviate(text),
                    placeholder.node());
            sticky(text);
            return placeholder;
        }

        /** 不支持的结构（语句上下文）：诊断 + 便签留原文，不产生占位节点。 */
        void unsupportedNote(MolangAst.Node node, String description) {
            String text = sourceText(node.span());
            warn(DecompileDiagnostics.UNSUPPORTED_IMPORT,
                    "不支持的 molang 结构（" + description + "），原文已保留在便签：" + abbreviate(text));
            sticky(text);
        }

        /** const.number 0 占位节点（语义丢失处的显式连线目标）。 */
        PortRef placeholder() {
            return valueNode("const.number", ImportGraphBuilder.opts("value", 0), "out");
        }

        void sticky(String text) {
            stickies.add(new StickyNote("s" + stickySeq++, text, 0, 0, 200, 100, "#FFFF88"));
        }

        void warn(String code, String message) {
            diagnostics.add(Diagnostic.warning(code, message));
        }

        void warn(String code, String message, String nodeUid) {
            diagnostics.add(Diagnostic.warning(code, message, nodeUid));
        }

        String sourceText(SourceSpan span) {
            if (span.startIndex() < 0 || span.stopIndexInclusive() < span.startIndex()
                    || span.stopIndexInclusive() >= source.length()) {
                return source;
            }
            return source.substring(span.startIndex(), span.stopIndexInclusive() + 1);
        }
    }
}
