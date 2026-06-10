package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基于语法的差分模糊测试。
 * 从 MoLang 产生式随机生成表达式，同时走常量求值器和 bytecode 编译器两条路径。
 * 若常量求值器给出结果，必须与 bytecode 一致。
 * 所有表达式不应产生非预期异常。
 *
 * @author TT432
 */
class MolangFuzzTest {
    private static final int ITERATIONS = 5000;
    private static final MolangCompilerImpl compiler = new MolangCompilerImpl();
    private final Random rng = new Random(0x5EED);

    @RepeatedTest(100)
    void constantExpressionsProduceIdenticalResultsOnBothPaths() {
        for (int i = 0; i < ITERATIONS / 100; i++) {
            String expr = genConstantExpr(rng, 0);
            var constResult = MolangConstantExpressionEvaluator.tryEvaluate(expr);
            float fromBytecode = compiler.compile(expr, CompileContext.defaults())
                    .evaluate(new MolangScope()).asFloat();

            if (constResult.isEmpty()) {
                assertFalse(Float.isNaN(fromBytecode),
                        () -> "Bytecode also failed on: " + expr);
                continue;
            }

            assertEquals(constResult.get().asFloat(), fromBytecode, 0.0001f,
                    () -> "Mismatch for: " + expr);
        }
    }

    @RepeatedTest(50)
    void randomExpressionsDoNotCrash() {
        for (int i = 0; i < ITERATIONS / 50; i++) {
            String expr = genExpr(rng, 0);
            try {
                MolangObject result = compiler.compile(expr, CompileContext.defaults())
                        .evaluate(new MolangScope());
                assertNotNull(result, () -> "null result for: " + expr);
            } catch (ExpressionCompileException e) {
                // 合法：编译器正确拒绝了语义无效的表达式
            } catch (Exception e) {
                fail("Unexpected crash: " + expr + " — "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    // ============================================================
    // 表达式生成器
    // ============================================================

    private static final String[] IDENTIFIERS = {
            "x", "y", "z", "a", "b", "counter", "health",
            "q.anim_time", "q.life_time", "q.is_baby", "q.ground_speed",
            "v.x", "v.y", "v.counter", "t.temp", "t.val",
            "query.health", "variable.counter", "temp.foo"
    };

    private static final String[] BINARY_OPS = {"+", "-", "*", "/", "<", "<=", ">", ">=", "==", "!=", "&&", "||"};

    private static String genExpr(Random rng, int depth) {
        if (depth > 5) return genAtomic(rng);
        return switch (rng.nextInt(12)) {
            case 0 -> genAtomic(rng);
            case 1 -> pick(rng, "-", "!", "") + " " + genExpr(rng, depth + 1);
            case 2 -> genExpr(rng, depth + 1) + " " + pick(rng, BINARY_OPS) + " " + genExpr(rng, depth + 1);
            case 3 -> genExpr(rng, depth + 1) + " ? " + genExpr(rng, depth + 1) + " : " + genExpr(rng, depth + 1);
            case 4 -> genExpr(rng, depth + 1) + " ? " + genExpr(rng, depth + 1);
            case 5 -> genExpr(rng, depth + 1) + " ?? " + genExpr(rng, depth + 1);
            case 6 -> "(" + genExpr(rng, depth + 1) + ")";
            case 7 -> pick(rng, "v.x", "v.y", "t.val") + " = " + genExpr(rng, depth + 1);
            case 8 -> genCall(rng, depth);
            case 9 -> genExpr(rng, depth + 1) + "." + pick(rng, "x", "y", "z", "foo");
            case 10 -> genBlock(rng, depth);
            case 11 -> genReturn(rng, depth);
            default -> genAtomic(rng);
        };
    }

    private static String genConstantExpr(Random rng, int depth) {
        if (depth > 6) return genNumber(rng);
        return switch (rng.nextInt(6)) {
            case 0 -> genNumber(rng);
            case 1 -> "(" + genConstantExpr(rng, depth + 1) + ")";
            case 2 -> "-" + genConstantExpr(rng, depth + 1);
            case 3 -> genConstantExpr(rng, depth + 1) + " " + pick(rng, "+", "-", "*", "/")
                    + " " + genConstantExpr(rng, depth + 1);
            case 4 -> genConstantExpr(rng, depth + 1) + " ? " + genConstantExpr(rng, depth + 1)
                    + " : " + genConstantExpr(rng, depth + 1);
            case 5 -> genConstantExpr(rng, depth + 1) + " ? " + genConstantExpr(rng, depth + 1);
            default -> genNumber(rng);
        };
    }

    private static String genAtomic(Random rng) {
        return switch (rng.nextInt(5)) {
            case 0 -> genNumber(rng);
            case 1 -> "'" + randomString(rng, 1, 5) + "'";
            case 2 -> pick(rng, IDENTIFIERS);
            case 3 -> "this";
            case 4 -> pick(rng, "true", "false");
            default -> genNumber(rng);
        };
    }

    private static String genNumber(Random rng) {
        return switch (rng.nextInt(4)) {
            case 0 -> String.valueOf(rng.nextInt(100));
            case 1 -> String.format("%.1f", rng.nextDouble() * 100);
            case 2 -> String.valueOf(rng.nextInt(100)) + ".0f";
            case 3 -> String.format("%d.%de%d", rng.nextInt(10), rng.nextInt(100), rng.nextInt(3) - 1);
            default -> String.valueOf(rng.nextInt(100));
        };
    }

    private static String genCall(Random rng, int depth) {
        String fn = pick(rng, "math.abs", "math.sin", "math.cos", "math.floor", "math.ceil", "math.sqrt", "math.clamp");
        int argCount = fn.equals("math.clamp") ? 3 : 1;
        List<String> args = new ArrayList<>();
        for (int j = 0; j < argCount; j++) {
            args.add(genExpr(rng, depth + 1));
        }
        return fn + "(" + String.join(", ", args) + ")";
    }

    private static String genBlock(Random rng, int depth) {
        int stmtCount = rng.nextInt(3) + 1;
        StringBuilder sb = new StringBuilder("{");
        for (int j = 0; j < stmtCount; j++) {
            sb.append(pick(rng, "v.x", "t.val")).append(" = ").append(genExpr(rng, depth + 1)).append(";");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String genReturn(Random rng, int depth) {
        return "{ " + pick(rng, "v.x", "t.val") + " = " + genExpr(rng, depth + 1)
                + "; return " + genExpr(rng, depth + 1) + "; }";
    }

    @SafeVarargs
    private static <T> T pick(Random rng, T... items) {
        return items[rng.nextInt(items.length)];
    }

    private static String randomString(Random rng, int minLen, int maxLen) {
        int len = minLen + rng.nextInt(maxLen - minLen + 1);
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < len; j++) {
            sb.append((char) ('a' + rng.nextInt(26)));
        }
        return sb.toString();
    }
}
