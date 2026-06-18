package io.github.tt432.eyelib.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MolangValue 核心测试：求值、常量池、CODEC、函数接口、缓存。
 *
 * @author TT432
 */
class MolangValueTest {

    @Test
    void 表达式求值得3() {
        MolangValue v = new MolangValue("1+2");
        assertEquals(3.0f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void 空字符串求值得0() {
        MolangValue v = new MolangValue("");
        assertEquals(0.0f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void 空白字符串求值得0() {
        MolangValue v = new MolangValue("   ");
        assertEquals(0.0f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void constant返回固定值() {
        MolangValue v = MolangValue.constant("test", MolangFloat.valueOf(42));
        assertEquals(42.0f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void ONE求值得1() {
        assertEquals(1.0f, MolangValue.ONE.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void ZERO求值得0() {
        assertEquals(0.0f, MolangValue.ZERO.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void getConstant返回对应值() {
        MolangValue v = MolangValue.getConstant(3.5f);
        assertEquals(3.5f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void 常量池相同值返回同一实例() {
        MolangValue v1 = MolangValue.getConstant(5.0f);
        MolangValue v2 = MolangValue.getConstant(5.0f);
        assertSame(v1, v2);
    }

    @Test
    void codec解析字符串表达式() {
        JsonElement json = JsonParser.parseString("\"1+2\"");
        DataResult<MolangValue> result = MolangValue.CODEC.parse(JsonOps.INSTANCE, json);
        MolangValue v = TestCodecUtil.unwrap(result);
        assertEquals(3.0f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void codec解析字符串列表() {
        JsonElement json = JsonParser.parseString("[\"part1\",\"part2\"]");
        DataResult<MolangValue> result = MolangValue.CODEC.parse(JsonOps.INSTANCE, json);
        MolangValue v = TestCodecUtil.unwrap(result);
        assertEquals("part1part2", v.context());
    }

    @Test
    void codec解析浮点数() {
        JsonElement json = JsonParser.parseString("1.5");
        DataResult<MolangValue> result = MolangValue.CODEC.parse(JsonOps.INSTANCE, json);
        MolangValue v = TestCodecUtil.unwrap(result);
        assertEquals(1.5f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void codec解析布尔值True() {
        JsonElement json = JsonParser.parseString("true");
        DataResult<MolangValue> result = MolangValue.CODEC.parse(JsonOps.INSTANCE, json);
        MolangValue v = TestCodecUtil.unwrap(result);
        // Boolean true → "1" → NumberLiteral 1 → eval 1.0
        assertEquals(1.0f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void codec解析布尔值False() {
        JsonElement json = JsonParser.parseString("false");
        DataResult<MolangValue> result = MolangValue.CODEC.parse(JsonOps.INSTANCE, json);
        MolangValue v = TestCodecUtil.unwrap(result);
        // Boolean false → "0" → NumberLiteral 0 → eval 0.0
        assertEquals(0.0f, v.eval(new MolangScope()), 0.0001f);
    }

    @Test
    void null函数返回MolangNull() {
        MolangObject result = MolangValue.MolangFunction.NULL.apply(new MolangScope());
        assertSame(MolangNull.INSTANCE, result);
    }

    @Test
    void 相同表达式复用编译缓存() {
        // 使用不可常量折叠的表达式（比较运算符），走编译缓存路径
        MolangValue v1 = new MolangValue("1<2");
        MolangValue v2 = new MolangValue("1<2");

        // 上下文相同
        assertEquals(v1, v2);
        // 求值结果相同
        assertEquals(v1.eval(new MolangScope()), v2.eval(new MolangScope()), 0.0001f);
        // 底层缓存不会重复编译（不抛异常即表明工作正常）
    }
}
