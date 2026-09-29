package io.github.tt432.eyelib.wintersky.molang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * MolangJS 1.6.6 {@code molang.js} 逐字移植（原作者 JannisX11，MIT）。
 *
 * <p>表达式树节点与求值语义与 JS 版一一对应，JS 动态类型行为由
 * {@link JsSemantics} 复刻。随机数经 {@link io.github.tt432.eyelib.wintersky.rng.WinterskyRandom}
 * （见 ADR-0034）。
 *
 * <p>公开 API 保留 JS 命名（global_variables / cache_enabled / use_radians /
 * variableHandler / parse / resetVariables），便于与上游源码并排审查。
 */
public class Molang {

    /** 对应 JS 中 context 里存放的 function 值（如 query.xxx 的上下文实现）。 */
    @FunctionalInterface
    public interface ContextFunction {
        @Nullable Object call(Object[] args);
    }

    /** 对应 JS 的 {@code Molang.variableHandler}：变量查找 (name, context) 或 query 调用 (name, context, args)。 */
    @FunctionalInterface
    public interface VariableHandler {
        @Nullable Object handle(String name, Map<String, Object> context, Object @Nullable [] args);
    }

    public final Map<String, Object> global_variables = new LinkedHashMap<>();
    public boolean cache_enabled = true;
    public boolean use_radians = false;
    public Map<String, Object> variables = new LinkedHashMap<>();
    public @Nullable VariableHandler variableHandler = null;

    private boolean found_unassigned_variable = false;
    private int loop_status = 0;

    private final Map<String, Object> cached = new LinkedHashMap<>();
    private int cache_size = 0;

    private void addToCache(String input, Object expression) {
        cached.put(input, expression);
        cache_size++;
        if (cache_size > 400) {
            // Free some cache
            int i = 0;
            for (var it = cached.keySet().iterator(); it.hasNext() && i < 10; i++) {
                it.next();
                it.remove();
            }
            cache_size -= 10;
        }
    }

    // Tree Types

    private record Scope(List<Object> lines) {
    }

    private record Loop(Object iterations, Object body) {
    }

    private record Comp(int operator, Object a, @Nullable Object b, @Nullable Object c) {
    }

    private record QueryFunction(String query, List<Object> args) {
    }

    private record Allocation(String name, Object value) {
    }

    private record ReturnStatement(Object value) {
    }

    private enum BreakStatement {
        VALUE
    }

    private enum ContinueStatement {
        VALUE
    }

    private double angleFactor() {
        return use_radians ? 1 : (Math.PI / 180);
    }

    private static final Pattern STRING_NUM_REGEX = Pattern.compile("^-?\\d+(\\.\\d+f?)?$");

    private static boolean isStringNumber(String string) {
        return STRING_NUM_REGEX.matcher(string).matches();
    }

    private static String toVariableName(String input) {
        if (input.length() >= 2 && input.charAt(1) == '.') {
            char ch = input.charAt(0);
            switch (ch) {
                case 'q':
                    return "query" + input.substring(1);
                case 'v':
                    return "variable" + input.substring(1);
                case 't':
                    return "temp" + input.substring(1);
                case 'c':
                    return "context" + input.substring(1);
                default:
                    return input;
            }
        } else {
            return input;
        }
    }

    private static final Pattern LOGIC_OPERATOR_REGEX = Pattern.compile("[&|<>=]");
    private static final Pattern ALLOCATION_REGEX = Pattern.compile("^(temp|variable|t|v)\\.\\w+=");
    private static final Pattern VARIABLE_NAME_REGEX = Pattern.compile("[a-z0-9._]{2,}");

    private static final String RETURN = "return";
    private static final String BREAK = "break";
    private static final String CONTINUE = "continue";

    /** Iterates through string, returns float, string or comp. */
    private Object iterateString(@Nullable String s) {
        if (s == null || s.isEmpty()) return 0.0;

        if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
        while (canTrimBrackets(s)) {
            s = s.substring(1, s.length() - 1);
        }

        if (isStringNumber(s)) return Double.parseDouble(s);

        List<String> lines = splitStringMultiple(s, ";");
        if (lines != null) {
            List<Object> scopeLines = new ArrayList<>();
            for (String line : lines) {
                if (line.isEmpty()) continue;
                Object result = iterateString(line);
                scopeLines.add(result);
                if (result instanceof ReturnStatement) break;
            }
            return new Scope(scopeLines);
        }

        // Return Statement
        if (s.startsWith(RETURN)) {
            return new ReturnStatement(iterateString(JsSemantics.jsSubstr(s, 6, s.length())));
        }

        // Bool
        switch (s) {
            case "true":
                return 1.0;
            case "false":
                return 0.0;
            case BREAK:
                return BreakStatement.VALUE;
            case CONTINUE:
                return ContinueStatement.VALUE;
            default:
                break;
        }

        boolean has_equal_sign = s.indexOf('=') != -1;

        // allocation
        Matcher match = (has_equal_sign && s.length() > 4) ? ALLOCATION_REGEX.matcher(s) : null;
        if (match != null && match.find() && (match.end() >= s.length() || s.charAt(match.end()) != '=')) {
            String name = match.group().substring(0, match.group().length() - 1);
            String value = s.substring(match.end());
            return new Allocation(toVariableName(name), iterateString(value));
        }

        boolean has_question_mark = s.indexOf('?') != -1;

        // Null Coalescing
        Object comp = has_question_mark ? testOp(s, "??", 19) : null;
        if (comp != null) return comp;

        // ternary
        List<String> split = has_question_mark ? splitString(s, "?") : null;
        if (split != null) {
            List<String> ab = splitString(split.get(1), ":");
            if (ab != null && !ab.isEmpty()) {
                return new Comp(10, iterateString(split.get(0)), iterateString(ab.get(0)), iterateString(ab.get(1)));
            } else {
                return new Comp(10, iterateString(split.get(0)), iterateString(split.get(1)), 0.0);
            }
        }

        // 2 part operators
        boolean has_logic_operators = LOGIC_OPERATOR_REGEX.matcher(s).find();
        comp = null;
        if (has_logic_operators) {
            comp = testOp(s, "&&", 11);
            if (comp == null) comp = testOp(s, "||", 12);
            if (comp == null && has_equal_sign) comp = testOp(s, "==", 17);
            if (comp == null && has_equal_sign) comp = testOp(s, "!=", 18);
            if (comp == null && has_equal_sign) comp = testOp(s, "<=", 14);
            if (comp == null) comp = testOp(s, "<", 13);
            if (comp == null && has_equal_sign) comp = testOp(s, ">=", 16);
            if (comp == null) comp = testOp(s, ">", 15);
        }
        if (comp == null) comp = testOp(s, "+", 1, true);
        if (comp == null) comp = testMinus(s);
        if (comp == null) comp = testOp(s, "*", 3);
        if (comp == null) comp = testOp(s, "/", 4, true);
        if (comp == null) comp = testNegator(s);
        if (comp != null) return comp;

        if (s.startsWith("math.")) {
            if (s.equals("math.pi")) {
                return Math.PI;
            }
            int begin = s.indexOf('(');
            String operator = JsSemantics.jsSubstr(s, 5, begin - 5);
            String inner = JsSemantics.jsSubstr(s, begin + 1, s.length() - begin - 2);
            List<String> params = splitStringMultiple(inner, ",");
            if (params == null) {
                params = new ArrayList<>();
                params.add(inner);
            }
            String p0 = !params.isEmpty() ? params.get(0) : null;
            String p1 = params.size() > 1 ? params.get(1) : null;
            String p2 = params.size() > 2 ? params.get(2) : null;

            switch (operator) {
                case "abs":
                    return new Comp(100, iterateString(p0), null, null);
                case "sin":
                    return new Comp(101, iterateString(p0), null, null);
                case "cos":
                    return new Comp(102, iterateString(p0), null, null);
                case "exp":
                    return new Comp(103, iterateString(p0), null, null);
                case "ln":
                    return new Comp(104, iterateString(p0), null, null);
                case "pow":
                    return new Comp(105, iterateString(p0), iterateString(p1), null);
                case "sqrt":
                    return new Comp(106, iterateString(p0), null, null);
                case "random":
                    return new Comp(107, iterateString(p0), iterateString(p1), null);
                case "ceil":
                    return new Comp(108, iterateString(p0), null, null);
                case "round":
                    return new Comp(109, iterateString(p0), null, null);
                case "trunc":
                    return new Comp(110, iterateString(p0), null, null);
                case "floor":
                    return new Comp(111, iterateString(p0), null, null);
                case "mod":
                    return new Comp(112, iterateString(p0), iterateString(p1), null);
                case "min":
                    return new Comp(113, iterateString(p0), iterateString(p1), null);
                case "max":
                    return new Comp(114, iterateString(p0), iterateString(p1), null);
                case "clamp":
                    return new Comp(115, iterateString(p0), iterateString(p1), iterateString(p2));
                case "lerp":
                    return new Comp(116, iterateString(p0), iterateString(p1), iterateString(p2));
                case "lerprotate":
                    return new Comp(117, iterateString(p0), iterateString(p1), iterateString(p2));
                case "asin":
                    return new Comp(118, iterateString(p0), null, null);
                case "acos":
                    return new Comp(119, iterateString(p0), null, null);
                case "atan":
                    return new Comp(120, iterateString(p0), null, null);
                case "atan2":
                    return new Comp(121, iterateString(p0), iterateString(p1), null);
                case "die_roll":
                    return new Comp(122, iterateString(p0), iterateString(p1), iterateString(p2));
                case "die_roll_integer":
                    return new Comp(123, iterateString(p0), iterateString(p1), iterateString(p2));
                case "hermite_blend":
                    return new Comp(124, iterateString(p0), null, null);
                case "random_integer":
                    return new Comp(125, iterateString(p0), iterateString(p1), null);
                case "min_angle":
                    return new Comp(126, iterateString(p0), null, null);
                default:
                    break;
            }
        }
        if (s.startsWith("loop(")) {
            String inner = s.substring(5, s.length() - 1);
            List<String> params = splitStringMultiple(inner, ",");
            if (params != null) {
                String iterations = params.get(0);
                String body = params.size() > 1 ? params.get(1) : null;
                return new Loop(iterateString(iterations), iterateString(body));
            }
        }

        Matcher varMatcher = VARIABLE_NAME_REGEX.matcher(s);
        List<String> varMatches = new ArrayList<>();
        while (varMatcher.find()) {
            varMatches.add(varMatcher.group());
        }
        if (varMatches.size() == 1 && varMatches.get(0).length() >= s.length() - 2) {
            return toVariableName(s);
        } else if (s.contains("(") && s.charAt(s.length() - 1) == ')') {
            int begin = s.indexOf('(');
            String query_name = toVariableName(s.substring(0, begin));
            String inner = s.substring(begin + 1, s.length() - 1);
            List<String> params = splitStringMultiple(inner, ",");
            if (params == null) {
                params = new ArrayList<>();
                params.add(inner);
            }
            List<Object> args = new ArrayList<>(params.size());
            for (String string : params) {
                if (string.startsWith("'") && string.endsWith("'")) {
                    args.add(string);
                } else {
                    args.add(iterateString(string));
                }
            }
            return new QueryFunction(query_name, args);
        }
        return 0.0;
    }

    private static boolean canTrimBrackets(String s) {
        boolean regular_brackets = s.startsWith("(") && s.endsWith(")");
        if (regular_brackets || (s.startsWith("{") && s.endsWith("}"))) {
            if (s.indexOf(regular_brackets ? ")" : "}") == s.length() - 1) return true;
            int level = 0;
            for (int i = 0; i < s.length() - 1; i++) {
                char c = s.charAt(i);
                if (c == '(' || c == '{') {
                    level++;
                } else if (c == ')' || c == '}') {
                    level--;
                }
                if (level == 0) return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private @Nullable Object testOp(String s, String ch, int operator) {
        return testOp(s, ch, operator, false);
    }

    private @Nullable Object testOp(String s, String ch, int operator, boolean inverse) {
        List<String> split = inverse ? splitStringReverse(s, ch) : splitString(s, ch);
        if (split != null) {
            return new Comp(operator, iterateString(split.get(0)), iterateString(split.get(1)), null);
        }
        return null;
    }

    private @Nullable Object testMinus(String s) {
        List<String> split = splitStringReverse(s, "-");
        if (split != null) {
            if (split.get(0).isEmpty()) {
                return new Comp(2, 0.0, iterateString(split.get(1)), null);
            } else {
                return new Comp(2, iterateString(split.get(0)), iterateString(split.get(1)), null);
            }
        }
        return null;
    }

    private @Nullable Object testNegator(String s) {
        if (s.startsWith("!") && s.length() > 1) {
            return new Comp(5, iterateString(s.substring(1)), 0.0, null);
        }
        return null;
    }

    private static @Nullable List<String> splitString(String s, String ch) {
        if (!s.contains(ch)) return null;
        int level = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '{') {
                level++;
            } else if (c == ')' || c == '}') {
                level--;
            } else {
                if (level == 0 && ch.charAt(0) == c && (ch.length() == 1 || s.startsWith(ch, i))) {
                    List<String> result = new ArrayList<>(2);
                    result.add(s.substring(0, i));
                    result.add(s.substring(i + ch.length()));
                    return result;
                }
            }
        }
        return null;
    }

    private static @Nullable List<String> splitStringMultiple(String s, String ch) {
        if (!s.contains(ch)) return null;
        int level = 0;
        List<String> pieces = null;
        int last_split = 0;
        loop:
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '{') {
                level++;
            } else if (c == ')' || c == '}') {
                level--;
            } else {
                if (level == 0 && ch.charAt(0) == c && (ch.length() == 1 || s.startsWith(ch, i))) {
                    String piece = s.substring(last_split, i);
                    if (pieces == null) pieces = new ArrayList<>();
                    pieces.add(piece);
                    last_split = i + ch.length();
                    if (!s.substring(last_split).contains(ch)) break loop;
                }
            }
        }
        if (pieces != null && !pieces.isEmpty()) {
            pieces.add(s.substring(last_split));
            return pieces;
        }
        return null;
    }

    private static @Nullable List<String> splitStringReverse(String s, String ch) {
        if (!s.contains(ch)) return null;
        int i = s.length() - 1;
        int level = 0;
        while (i >= 0) {
            char c = s.charAt(i);
            if (c == '(' || c == '{') {
                level++;
            } else if (c == ')' || c == '}') {
                level--;
            } else {
                if (level == 0 && ch.charAt(0) == c
                        && (ch.length() == 1 || s.startsWith(ch, i))
                        && (!ch.equals("-") || i == 0 || "+-*/<>=|&?:".indexOf(s.charAt(i - 1)) < 0)) {
                    List<String> result = new ArrayList<>(2);
                    result.add(s.substring(0, i));
                    result.add(s.substring(i + ch.length()));
                    return result;
                }
            }
            i--;
        }
        return null;
    }

    private boolean compareValues(Object a, @Nullable Object b, Map<String, Object> context) {
        Object av = (a instanceof String s && s.startsWith("'")) ? a : iterateExp(a, context, true);
        Object bv = (b instanceof String s && s.startsWith("'")) ? b : iterateExp(b, context, true);
        return JsSemantics.strictEquals(av, bv);
    }

    private static double num(@Nullable Object v) {
        return JsSemantics.toNumber(v);
    }

    private Object iterateExp(@Nullable Object T, Map<String, Object> context) {
        return iterateExp(T, context, false);
    }

    private Object iterateExp(@Nullable Object T, Map<String, Object> context, boolean allow_strings) {
        if (T instanceof Double d) {
            return d;
        } else if (T instanceof String name) {
            Object val = context.get(name);
            if (val == null && variableHandler != null) {
                val = variableHandler.handle(name, context, null);
            }
            if (val instanceof Double d) {
                return d;
            } else if (val instanceof String s && !allow_strings) {
                return parse(s, context);
            } else if (val == null) {
                found_unassigned_variable = true;
            } else if (val instanceof ContextFunction f) {
                return JsSemantics.orZero(f.call(new Object[0]));
            }
            return JsSemantics.orZero(val);
        } else if (T == null) {
            return 0.0;
        }

        if (T instanceof Comp comp) {
            switch (comp.operator()) {
                // Basic
                case 1: {
                    Object a = iterateExp(comp.a(), context);
                    Object b = iterateExp(comp.b(), context);
                    if (a instanceof String || b instanceof String) {
                        return JsSemantics.toJsString(a) + JsSemantics.toJsString(b);
                    }
                    return num(a) + num(b);
                }
                case 2:
                    return num(iterateExp(comp.a(), context)) - num(iterateExp(comp.b(), context));
                case 3:
                    return num(iterateExp(comp.a(), context)) * num(iterateExp(comp.b(), context));
                case 4:
                    return num(iterateExp(comp.a(), context)) / num(iterateExp(comp.b(), context));
                case 5:
                    return num(iterateExp(comp.a(), context)) == 0 ? 1.0 : 0.0;

                // Logical
                case 10:
                    return JsSemantics.truthy(iterateExp(comp.a(), context))
                            ? iterateExp(comp.b(), context)
                            : iterateExp(comp.c(), context);
                case 11: {
                    Object a = iterateExp(comp.a(), context);
                    if (!JsSemantics.truthy(a)) return 0.0;
                    return JsSemantics.truthy(iterateExp(comp.b(), context)) ? 1.0 : 0.0;
                }
                case 12: {
                    Object a = iterateExp(comp.a(), context);
                    if (JsSemantics.truthy(a)) return 1.0;
                    return JsSemantics.truthy(iterateExp(comp.b(), context)) ? 1.0 : 0.0;
                }
                case 13:
                    return num(iterateExp(comp.a(), context)) < num(iterateExp(comp.b(), context)) ? 1.0 : 0.0;
                case 14:
                    return num(iterateExp(comp.a(), context)) <= num(iterateExp(comp.b(), context)) ? 1.0 : 0.0;
                case 15:
                    return num(iterateExp(comp.a(), context)) > num(iterateExp(comp.b(), context)) ? 1.0 : 0.0;
                case 16:
                    return num(iterateExp(comp.a(), context)) >= num(iterateExp(comp.b(), context)) ? 1.0 : 0.0;
                case 17:
                    return compareValues(comp.a(), comp.b(), context) ? 1.0 : 0.0;
                case 18:
                    return compareValues(comp.a(), comp.b(), context) ? 0.0 : 1.0;
                case 19: {
                    found_unassigned_variable = false;
                    Object variable = iterateExp(comp.a(), context);
                    return found_unassigned_variable ? iterateExp(comp.b(), context) : variable;
                }

                // Math
                case 100:
                    return Math.abs(num(iterateExp(comp.a(), context)));
                case 101:
                    return Math.sin(num(iterateExp(comp.a(), context)) * angleFactor());
                case 102:
                    return Math.cos(num(iterateExp(comp.a(), context)) * angleFactor());
                case 103:
                    return Math.exp(num(iterateExp(comp.a(), context)));
                case 104:
                    return Math.log(num(iterateExp(comp.a(), context)));
                case 105:
                    return Math.pow(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)));
                case 106:
                    return Math.sqrt(num(iterateExp(comp.a(), context)));
                case 107:
                    return MathUtil.random(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)));
                case 108:
                    return Math.ceil(num(iterateExp(comp.a(), context)));
                case 109:
                    return JsSemantics.jsRound(num(iterateExp(comp.a(), context)));
                case 110: {
                    double v = num(iterateExp(comp.a(), context));
                    return v < 0 ? Math.ceil(v) : Math.floor(v);
                }
                case 111:
                    return Math.floor(num(iterateExp(comp.a(), context)));
                case 112:
                    return num(iterateExp(comp.a(), context)) % num(iterateExp(comp.b(), context));
                case 113:
                    return Math.min(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)));
                case 114:
                    return Math.max(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)));
                case 115:
                    return MathUtil.clamp(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)),
                            num(iterateExp(comp.c(), context)));
                // Lerp
                case 116:
                    return MathUtil.lerp(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)),
                            num(iterateExp(comp.c(), context)));
                case 117:
                    return MathUtil.lerpRotate(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)),
                            num(iterateExp(comp.c(), context)));
                // Inverse Trigonometry
                case 118:
                    return Math.asin(num(iterateExp(comp.a(), context))) / angleFactor();
                case 119:
                    return Math.acos(num(iterateExp(comp.a(), context))) / angleFactor();
                case 120:
                    return Math.atan(num(iterateExp(comp.a(), context))) / angleFactor();
                case 121:
                    return Math.atan2(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)))
                            / angleFactor();
                // Misc
                case 122:
                    return MathUtil.dieRoll(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)),
                            num(iterateExp(comp.c(), context)));
                case 123:
                    return MathUtil.dieRollInt(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)),
                            num(iterateExp(comp.c(), context)));
                case 124: {
                    double t = num(iterateExp(comp.a(), context));
                    return 3 * (t * t) - 2 * (t * t * t);
                }
                case 125:
                    return MathUtil.randomInt(num(iterateExp(comp.a(), context)), num(iterateExp(comp.b(), context)));
                case 126:
                    return MathUtil.minAngle(num(iterateExp(comp.a(), context)));
                default:
                    return 0.0;
            }
        } else if (T instanceof ReturnStatement ret) {
            loop_status = 1;
            return iterateExp(ret.value(), context);
        } else if (T instanceof Allocation alloc) {
            Object v = iterateExp(alloc.value(), context);
            context.put(alloc.name(), v);
            variables.put(alloc.name(), v);
            return 0.0;
        } else if (T instanceof QueryFunction query) {
            Object[] args = new Object[query.args().size()];
            for (int i = 0; i < args.length; i++) {
                Object arg = query.args().get(i);
                if (arg instanceof String s && s.startsWith("'") && s.endsWith("'")) {
                    args[i] = JsSemantics.jsSubstring(s, 1, s.length() - 1);
                } else {
                    args[i] = iterateExp(arg, context, true);
                }
            }

            switch (query.query()) {
                case "query.in_range":
                    return MathUtil.inRange(args);
                case "query.all":
                    return MathUtil.all(args);
                case "query.any":
                    return MathUtil.any(args);
                case "query.approx_eq":
                    return MathUtil.approxEq(args);
                default:
                    break;
            }
            Object ctxVal = context.get(query.query());
            if (ctxVal instanceof ContextFunction f) {
                return JsSemantics.orZero(f.call(args));
            }
            if (variableHandler != null) {
                return JsSemantics.orZero(variableHandler.handle(query.query(), context, args));
            }
            return 0.0;
        } else if (T instanceof Scope scope) {
            loop_status = 0;
            Object return_value = 0.0;
            for (Object line : scope.lines()) {
                return_value = iterateExp(line, context);
                if (loop_status > 0) {
                    break;
                }
            }
            return return_value;
        } else if (T instanceof Loop loop) {
            Object return_value2 = 0.0;
            double iterations = MathUtil.clamp(num(iterateExp(loop.iterations(), context)), 0, 1024);
            for (int i = 0; i < iterations; i++) {
                Object result = iterateExp(loop.body(), context);
                if (loop_status == 2) {
                    loop_status = 0;
                    break;
                }
                if (loop_status == 3) {
                    loop_status = 0;
                    continue;
                }
                return_value2 = result;
                if (loop_status == 1) break;
            }
            return return_value2;
        } else if (T instanceof BreakStatement) {
            loop_status = 2;
            return 0.0;
        } else if (T instanceof ContinueStatement) {
            loop_status = 3;
            return 0.0;
        }
        return 0.0;
    }

    private Object calculate(Object expression, @Nullable Map<String, Object> variables) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.putAll(global_variables);
        context.putAll(this.variables);
        if (variables != null) {
            context.putAll(variables);
        }
        Object end_result = iterateExp(expression, context);
        loop_status = 0;
        return end_result;
    }

    /** 对应 JS {@code parse(number)}：{@code input || 0}。 */
    public double parse(double input) {
        return (input == 0 || Double.isNaN(input)) ? 0 : input;
    }

    public double parse(@Nullable String input) {
        return parse(input, null);
    }

    public double parse(@Nullable String input, @Nullable Map<String, Object> variables) {
        if (input == null || input.isEmpty()) return 0;
        if (input.length() < 9 && isStringNumber(input)) {
            return Double.parseDouble(input);
        }

        Object expression = cache_enabled ? cached.get(input) : null;
        if (expression == null || (expression instanceof Double d && d == 0.0)) {
            expression = iterateString(input.toLowerCase(Locale.ROOT).replaceAll("\\s", ""));
            if (cache_enabled) {
                addToCache(input, expression);
            }
        }
        return JsSemantics.resultToNumber(calculate(expression, variables));
    }

    public void resetVariables() {
        variables = new LinkedHashMap<>();
    }
}
