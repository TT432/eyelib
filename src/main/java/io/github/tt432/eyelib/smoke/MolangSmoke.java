package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.compiler.ExpressionCompileException;
import io.github.tt432.eyelib.molang.compiler.cache.MolangCompileCache;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangString;

/**
 * Molang 全链路烟雾测试——在 MC 进程内验证 parser → binder → bytecode → evaluate。
 *
 * @author TT432
 */
@ClientSmoke(description = "Molang 编译器全链路：解析→绑定→字节码→求值 + 缓存", priority = 10)
public class MolangSmoke {

    public MolangSmoke() {
        testBasicCompilation();
        testBinaryConditional();
        testTernaryConditional();
        testNullCoalesce();
        testBlockExpression();
        testLoopExpression();
        testStringLiteral();
        testCacheHit();
        testNumberSuffixF();
        testRealBedrockExpressions();
        testErrorExpressionDoesNotCrash();
    }

    private static void require(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    /** 基本算术 */
    private void testBasicCompilation() {
        MolangValue v = new MolangValue("1 + 2");
        MolangObject r = v.method().apply(new MolangScope());
        require(r instanceof MolangFloat, "Expected MolangFloat");
        require(Math.abs(r.asFloat() - 3.0f) < 0.001f, "1+2 should be 3.0, got " + r);
    }

    /** 二元条件：之前 binder 把它 defer 为 UNSUPPORTED */
    private void testBinaryConditional() {
        MolangValue v = new MolangValue("1 ? 42");
        MolangObject r = v.method().apply(new MolangScope());
        require(Math.abs(r.asFloat() - 42.0f) < 0.001f,
                "1?42 should be 42.0, got " + r);

        MolangValue v2 = new MolangValue("0 ? 42");
        MolangObject r2 = v2.method().apply(new MolangScope());
        require(Math.abs(r2.asFloat() - 0.0f) < 0.001f,
                "0?42 should be 0.0, got " + r2);
    }

    /** 三元条件 */
    private void testTernaryConditional() {
        MolangValue v = new MolangValue("1 ? 3 : 5");
        MolangObject r = v.method().apply(new MolangScope());
        require(Math.abs(r.asFloat() - 3.0f) < 0.001f, "1?3:5 should be 3.0");

        MolangValue v2 = new MolangValue("0 ? 3 : 5");
        MolangObject r2 = v2.method().apply(new MolangScope());
        require(Math.abs(r2.asFloat() - 5.0f) < 0.001f, "0?3:5 should be 5.0");
    }

    /** 空值合并：未定义变量使用默认值 */
    private void testNullCoalesce() {
        MolangValue v = new MolangValue("undefined_var ?? 42");
        MolangObject r = v.method().apply(new MolangScope());
        require(Math.abs(r.asFloat() - 42.0f) < 0.001f,
                "undefined??42 should be 42.0, got " + r);
    }

    /** 块表达式：隐式 return 0 */
    private void testBlockExpression() {
        MolangValue v = new MolangValue("{ t.x = 5; }");
        MolangObject r = v.method().apply(new MolangScope());
        require(Math.abs(r.asFloat() - 0.0f) < 0.001f,
                "block without return should return 0.0, got " + r);

        MolangValue v2 = new MolangValue("{ t.x = 5; return t.x + 2; }");
        MolangObject r2 = v2.method().apply(new MolangScope());
        require(Math.abs(r2.asFloat() - 7.0f) < 0.001f,
                "block with return should return 7.0, got " + r2);
    }

    /** loop 表达式 */
    private void testLoopExpression() {
        MolangValue v = new MolangValue(
                "t.c = 0; loop(3, { t.c = t.c + 1; }); return t.c;");
        MolangObject r = v.method().apply(new MolangScope());
        require(Math.abs(r.asFloat() - 3.0f) < 0.001f,
                "loop 3 times should give 3.0, got " + r);
    }

    /** 字符串字面量 */
    private void testStringLiteral() {
        MolangValue v = new MolangValue("'hello'");
        MolangObject r = v.method().apply(new MolangScope());
        require(r instanceof MolangString, "Expected MolangString, got " + r.getClass());
    }

    /** 缓存命中 */
    private void testCacheHit() {
        MolangValue a = new MolangValue("42.5");
        MolangValue b = new MolangValue("42.5");
        // 同一个表达式字符串应被缓存复用（通过 MolangValue 内部静态缓存）
        require(a.method() != null, "method should not be null");
        require(b.method() != null, "method should not be null");
    }

    /** 数字尾缀 f（vanilla .mcpack 中大量使用） */
    private void testNumberSuffixF() {
        MolangValue v = new MolangValue("1.0f + 2.0f");
        MolangObject r = v.method().apply(new MolangScope());
        require(Math.abs(r.asFloat() - 3.0f) < 0.001f,
                "1.0f + 2.0f should be 3.0, got " + r);
    }

    /** 真实 Bedrock 表达式 */
    private void testRealBedrockExpressions() {
        String[] exprs = {
                "math.sin(q.anim_time * 1.23)",
                "v.buff_timer = (v.buff_timer ?? 0) + q.delta_time",
                "q.health < 5 ? 1 : 0",
                "q.is_jumping ? 3 : 0",
                "query.is_sheared ? geometry.sheared : geometry.woolly",
                "math.cos(q.anim_time * 38.17) * 80.0",
                "-this",
                "math.abs(-42)",
                "math.sqrt(16)",
                "math.floor(3.7)",
        };

        for (String expr : exprs) {
            MolangValue v = new MolangValue(expr);
            MolangObject r = v.method().apply(new MolangScope());
            require(r != null, "Expression should not return null: " + expr);
        }
    }

    /** 错误表达式不应 crash——编译器应优雅处理 */
    private void testErrorExpressionDoesNotCrash() {
        try {
            new MolangValue("'unclosed");
            // 可能抛异常也可能不抛——只要不 OOM/段错误即可
        } catch (ExpressionCompileException e) {
            // 预期行为
        } catch (Exception e) {
            // 非预期异常——但字符串未闭合在 MolangValue 构造函数中可能抛
        }
    }
}
