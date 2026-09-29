package io.github.tt432.eyelib.wintersky.molang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Molang 移植版 vs MolangJS 1.6.6（Node 运行）差分测试。
 *
 * <p>用例由 scripts/wintersky-oracle/molang_cases.mjs 生成并冻结在
 * src/test/resources/wintersky/molang_cases.json；随机用例通过 mulberry32
 * 注入与 Node 端相同的确定性随机序列。数值容差 1e-9（吸收 V8 与 JVM
 * 三角函数的末位 ulp 差）。
 */
class MolangOracleTest {

    private static final double EPS = 1e-9;

    @AfterEach
    void tearDown() {
        WinterskyRandom.reset();
    }

    /** mulberry32（与 oracle 生成脚本中的 JS 实现逐位一致）。 */
    private static final class Mulberry32 implements RandomGenerator {
        private int a;

        Mulberry32(int seed) {
            this.a = seed;
        }

        private int draw() {
            a = a + 0x6D2B79F5;
            int t = (a ^ (a >>> 15)) * (1 | a);
            t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
            return t ^ (t >>> 14);
        }

        @Override
        public double nextDouble() {
            return (draw() & 0xFFFFFFFFL) / 4294967296.0;
        }

        @Override
        public long nextLong() {
            long hi = draw() & 0xFFFFFFFFL;
            long lo = draw() & 0xFFFFFFFFL;
            return (hi << 32) | lo;
        }
    }

    @Test
    void molangMatchesNodeOracle() throws Exception {
        JsonArray cases;
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/wintersky/molang_cases.json"),
                        "molang_cases.json missing"),
                StandardCharsets.UTF_8)) {
            cases = JsonParser.parseReader(reader).getAsJsonArray();
        }
        assertTrue(cases.size() > 50, "oracle cases should be loaded");

        for (JsonElement caseElement : cases) {
            JsonObject c = caseElement.getAsJsonObject();
            String name = c.get("name").getAsString();
            Molang molang = new Molang();
            molang.use_radians = c.get("useRadians").getAsBoolean();
            switch (c.get("handler").getAsString()) {
                case "values" -> molang.variableHandler = (key, context, args) -> {
                    if (key.equals("query.add")) {
                        return ((Number) args[0]).doubleValue() + ((Number) args[1]).doubleValue();
                    }
                    if (key.equals("variable.custom")) {
                        return 4.0;
                    }
                    return null;
                };
                case "ctxfn" -> molang.global_variables.put("query.double",
                        (Molang.ContextFunction) args -> ((Number) args[0]).doubleValue() * 2);
                default -> {
                }
            }
            if (!c.get("seed").isJsonNull()) {
                WinterskyRandom.set(new Mulberry32(c.get("seed").getAsInt()));
            } else {
                WinterskyRandom.reset();
            }

            JsonArray exprs = c.getAsJsonArray("exprs");
            JsonArray expected = c.getAsJsonArray("expected");
            for (int i = 0; i < exprs.size(); i++) {
                String expr = exprs.get(i).getAsString();
                double actual = molang.parse(expr, Map.of());
                JsonElement e = expected.get(i);
                if (e.isJsonObject()) {
                    String special = e.getAsJsonObject().get("special").getAsString();
                    switch (special) {
                        case "Infinity" -> assertEquals(Double.POSITIVE_INFINITY, actual, name + " / " + expr);
                        case "-Infinity" -> assertEquals(Double.NEGATIVE_INFINITY, actual, name + " / " + expr);
                        case "NaN" -> assertTrue(Double.isNaN(actual), name + " / " + expr);
                        case "-0" -> assertEquals(-0.0, actual, name + " / " + expr);
                        default -> fail("unknown special: " + special);
                    }
                } else {
                    double exp = e.getAsDouble();
                    double tolerance = EPS * Math.max(1, Math.abs(exp));
                    if (!(Math.abs(actual - exp) <= tolerance)) {
                        fail(name + " / " + expr + ": expected " + exp + " but was " + actual);
                    }
                }
            }
        }
    }
}
