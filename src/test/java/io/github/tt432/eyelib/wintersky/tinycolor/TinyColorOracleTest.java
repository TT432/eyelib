package io.github.tt432.eyelib.wintersky.tinycolor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TinyColor 移植版 vs tinycolor2 1.4.2（Node 运行）差分测试。
 *
 * <p>用例由 scripts/wintersky-oracle/tinycolor_cases.mjs 生成并冻结在
 * src/test/resources/wintersky/tinycolor_cases.json。
 */
class TinyColorOracleTest {

    @Test
    void tinycolorMatchesNodeOracle() throws Exception {
        JsonArray cases;
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/wintersky/tinycolor_cases.json"),
                        "tinycolor_cases.json missing"),
                StandardCharsets.UTF_8)) {
            cases = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("cases");
        }
        assertTrue(cases.size() >= 20, "oracle cases should be loaded, got " + cases.size());

        for (JsonElement caseElement : cases) {
            JsonObject c = caseElement.getAsJsonObject();
            String kind = c.get("kind").getAsString();
            JsonObject input = c.getAsJsonObject("input");
            JsonObject expected = c.getAsJsonObject("expected");

            TinyColor color = switch (kind) {
                case "parse_hex" -> new TinyColor(input.get("hex").getAsString());
                case "parse_object" -> {
                    JsonObject obj = input.getAsJsonObject("obj");
                    yield new TinyColor(obj.get("r").getAsDouble(), obj.get("g").getAsDouble(),
                            obj.get("b").getAsDouble(), obj.get("a").getAsDouble());
                }
                case "mix" -> TinyColor.mix(
                        new TinyColor(input.get("c1").getAsString()),
                        new TinyColor(input.get("c2").getAsString()),
                        input.get("amount").getAsDouble());
                default -> {
                    fail("unknown case kind: " + kind);
                    throw new AssertionError();
                }
            };

            JsonObject expRgb = expected.getAsJsonObject("rgb");
            TinyColor.Rgba rgb = color.toRgb();
            try {
                assertEquals(expRgb.get("r").getAsDouble(), rgb.r, 1e-9, "r");
                assertEquals(expRgb.get("g").getAsDouble(), rgb.g, 1e-9, "g");
                assertEquals(expRgb.get("b").getAsDouble(), rgb.b, 1e-9, "b");
                assertEquals(expRgb.get("a").getAsDouble(), rgb.a, 1e-9, "a");
                assertEquals(expected.get("hex8").getAsString(), color.toHex8String(), "hex8");
            } catch (AssertionError e) {
                fail("case " + kind + " input=" + input + " failed: " + e.getMessage());
            }
        }
    }
}
