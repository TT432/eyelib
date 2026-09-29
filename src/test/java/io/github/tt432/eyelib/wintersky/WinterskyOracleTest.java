package io.github.tt432.eyelib.wintersky;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;
import io.github.tt432.eyelib.wintersky.three.BufferAttribute;
import io.github.tt432.eyelib.wintersky.three.Object3D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * wintersky 根包（Scene/Config/Emitter/Particle）移植版与 Node golden oracle 的逐 tick 差分测试。
 *
 * <p>golden 由 {@code node scripts/wintersky-oracle/wintersky_cases.mjs} 生成并冻结于
 * src/test/resources/wintersky/wintersky_cases.json；夹具为 Mojang bedrock-samples
 * vanilla 粒子 + 手工分支补全（fixtures/）。随机源两侧均为 mulberry32(42)。
 */
class WinterskyOracleTest {

    private static final Gson GSON = new Gson();
    private static final double EPS = 1e-9;
    private static final double EPS_F32 = 1e-6;

    private static final String[] FIXTURES = {
            "basic_flame",
            "basic_smoke",
            "lava_particle",
            "evoker_spell",
            "ominous_spawning_particle",
            "mobflame",
            "white_smoke",
            "portal_north_south",
            "basic_crit",
            "enchanting_table_particle",
            "dripstone_lava_drip",
            "zz_branches",
    };
    private static final int TICKS = 45;

    @AfterEach
    void tearDown() {
        WinterskyRandom.reset();
    }

    @Test
    void matchesNodeGoldenOracle() {
        JsonArray goldenCases = readGolden();
        Map<String, JsonObject> goldenByName = new LinkedHashMap<>();
        for (JsonElement e : goldenCases) {
            JsonObject c = e.getAsJsonObject();
            goldenByName.put(c.get("name").getAsString(), c);
        }

        Map<String, Map<String, Object>> childEffects = new LinkedHashMap<>();
        childEffects.put("minecraft:basic_smoke_particle", loadFixture("basic_smoke"));
        childEffects.put("minecraft:basic_flame_particle", loadFixture("basic_flame"));

        for (String fixture : FIXTURES) {
            runCase(fixture, fixture, List.<String[]>of(new String[]{"tick", "45"}), goldenByName, childEffects);
        }
        runCase("jump_basic_smoke", "basic_smoke",
                List.<String[]>of(new String[]{"tick", "10"}, new String[]{"jumpTo", "1.0"}, new String[]{"tick", "5"}),
                goldenByName, childEffects);
        runCase("jump_back_basic_smoke", "basic_smoke",
                List.<String[]>of(new String[]{"tick", "40"}, new String[]{"jumpTo", "0.2"}, new String[]{"tick", "5"}),
                goldenByName, childEffects);

        assertEquals(FIXTURES.length + 2, goldenByName.size(), "golden 用例数");
    }

    @SuppressWarnings("unchecked")
    private void runCase(String name, String fixtureName, List<String[]> ops,
                         Map<String, JsonObject> goldenByName,
                         Map<String, Map<String, Object>> childEffects) {
        JsonObject golden = goldenByName.get(name);
        if (golden == null) fail("golden 缺少用例 " + name);

        WinterskyRandom.set(new Mulberry32(42));

        Scene.Options sceneOptions = new Scene.Options();
        sceneOptions.fetchParticleFile = (identifier, config) -> {
            Map<String, Object> json = childEffects.get(identifier);
            if (json == null) return null;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("json", json);
            return result;
        };
        Scene scene = new Scene(sceneOptions);
        Object3D camera = new Object3D();
        camera.position.set(3, 4, 5);
        camera.rotation.set(0.3, -0.7, 0.1);

        Emitter emitter = new Emitter(scene, loadFixture(fixtureName), new Emitter.Options());

        List<String> tickEvents = new ArrayList<>();
        emitter.on("event", d -> tickEvents.add("event:" + ((Map<String, Object>) d).get("event_id")));
        emitter.on("play_sound", d -> {
            Object se = ((Map<String, Object>) d).get("sound_effect");
            Object eventName = se instanceof Map<?, ?> m ? m.get("event_name") : null;
            tickEvents.add("sound:" + eventName);
        });
        emitter.on("play_child_particle", d -> {
            Object cfg = ((Map<String, Object>) d).get("config");
            tickEvents.add("child:" + (cfg instanceof Config c ? c.identifier : null));
        });
        emitter.on("start", d -> tickEvents.add("start"));
        emitter.on("end", d -> tickEvents.add("end"));
        emitter.on("stop", d -> tickEvents.add("stop"));

        emitter.start();

        JsonArray goldenTicks = golden.getAsJsonArray("ticks");
        int[] tickIndex = {0};
        for (String[] op : ops) {
            if ("tick".equals(op[0])) {
                int n = Integer.parseInt(op[1]);
                for (int i = 0; i < n; i++) {
                    emitter.tick(false);
                    scene.updateFacingRotation(camera);
                    JsonObject goldenTick = goldenTicks.get(tickIndex[0]).getAsJsonObject();
                    compareTick(name, tickIndex[0], emitter, tickEvents, goldenTick);
                    tickEvents.clear();
                    tickIndex[0]++;
                }
            } else if ("jumpTo".equals(op[0])) {
                emitter.jumpTo(Double.parseDouble(op[1]));
                scene.updateFacingRotation(camera);
                tickEvents.clear();
            }
        }
        assertEquals(goldenTicks.size(), tickIndex[0], name + " tick 数");
    }

    private void compareTick(String caseName, int tick, Emitter emitter, List<String> events, JsonObject g) {
        String where = caseName + " tick " + tick;
        num(where + ".age", emitter.age, g.get("age"), EPS);
        num(where + ".view_age", emitter.view_age, g.get("view_age"), EPS);
        assertEquals(g.get("enabled").getAsBoolean(), emitter.enabled, where + ".enabled");
        num(where + ".active_time", emitter.active_time, g.get("active_time"), EPS);
        num(where + ".sleep_time", emitter.sleep_time, g.get("sleep_time"), EPS);
        JsonArray grv = g.getAsJsonArray("random_vars");
        for (int i = 0; i < 4; i++) {
            num(where + ".random_vars[" + i + "]", emitter.random_vars[i], grv.get(i), EPS);
        }
        assertEquals(g.get("np").getAsInt(), emitter.particles.size(), where + ".np");
        assertEquals(g.get("dead").getAsInt(), emitter.dead_particles.size(), where + ".dead");
        assertEquals(g.get("nce").getAsInt(), emitter.child_emitters.size(), where + ".nce");
        int ncp = emitter.child_emitters.stream().mapToInt(e -> e.particles.size()).sum();
        assertEquals(g.get("ncp").getAsInt(), ncp, where + ".ncp");

        JsonArray gEvents = g.getAsJsonArray("events");
        assertEquals(gEvents.size(), events.size(), where + ".events.size " + events);
        for (int i = 0; i < events.size(); i++) {
            assertEquals(gEvents.get(i).getAsString(), events.get(i), where + ".events[" + i + "]");
        }

        JsonArray gParticles = g.getAsJsonArray("particles");
        assertEquals(gParticles.size(), emitter.particles.size(), where + ".particles.size");
        for (int i = 0; i < emitter.particles.size(); i++) {
            compareParticle(where + ".particles[" + i + "]", emitter.particles.get(i), gParticles.get(i).getAsJsonObject());
        }
    }

    private void compareParticle(String where, Particle p, JsonObject g) {
        vec(where + ".pos", p.position.x, p.position.y, p.position.z, g.getAsJsonArray("pos"), EPS);
        vec(where + ".speed", p.speed.x, p.speed.y, p.speed.z, g.getAsJsonArray("speed"), EPS);
        vec(where + ".facing", p.facing_direction.x, p.facing_direction.y, p.facing_direction.z,
                g.getAsJsonArray("facing"), EPS);
        num(where + ".rot", p.rotation, g.get("rot"), EPS);
        num(where + ".age", p.age, g.get("age"), EPS);
        num(where + ".lifetime", p.lifetime, g.get("lifetime"), EPS);
        JsonArray gScale = g.getAsJsonArray("scale");
        num(where + ".scale.x", p.mesh.scale.x, gScale.get(0), EPS);
        num(where + ".scale.y", p.mesh.scale.y, gScale.get(1), EPS);

        BufferAttribute clr = p.geometry.getAttribute("clr");
        JsonArray gClr = g.getAsJsonArray("clr");
        for (int i = 0; i < 4; i++) {
            num(where + ".clr[" + i + "]", clr.array.get(i), gClr.get(i), EPS_F32);
        }
        BufferAttribute uv = p.geometry.getAttribute("uv");
        JsonArray gUv = g.getAsJsonArray("uv");
        for (int i = 0; i < 8; i++) {
            num(where + ".uv[" + i + "]", uv.array.get(i), gUv.get(i), EPS_F32);
        }
        JsonArray gMeshRot = g.getAsJsonArray("meshRot");
        num(where + ".meshRot.x", p.mesh.rotation.getX(), gMeshRot.get(0), EPS);
        num(where + ".meshRot.y", p.mesh.rotation.getY(), gMeshRot.get(1), EPS);
        num(where + ".meshRot.z", p.mesh.rotation.getZ(), gMeshRot.get(2), EPS);
        assertEquals(g.get("order").getAsString(), p.mesh.rotation.getOrder(), where + ".order");
    }

    private void vec(String where, double x, double y, double z, JsonArray g, double eps) {
        num(where + ".x", x, g.get(0), eps);
        num(where + ".y", y, g.get(1), eps);
        num(where + ".z", z, g.get(2), eps);
    }

    /** golden 序列化：非有限值编码为字符串 "NaN"/"Infinity"/"-Infinity"。 */
    private void num(String where, double actual, JsonElement expectedEl, double eps) {
        if (expectedEl.isJsonPrimitive() && expectedEl.getAsJsonPrimitive().isString()) {
            String s = expectedEl.getAsString();
            switch (s) {
                case "NaN" -> {
                    if (!Double.isNaN(actual)) fail(where + ": 期望 NaN，实际 " + actual);
                    return;
                }
                case "Infinity" -> {
                    if (actual != Double.POSITIVE_INFINITY) fail(where + ": 期望 Infinity，实际 " + actual);
                    return;
                }
                case "-Infinity" -> {
                    if (actual != Double.NEGATIVE_INFINITY) fail(where + ": 期望 -Infinity，实际 " + actual);
                    return;
                }
                default -> fail(where + ": 未知 golden 标量 " + s);
            }
        }
        double expected = expectedEl.getAsDouble();
        if (Double.isNaN(actual) || Math.abs(actual - expected) > eps + eps * Math.abs(expected)) {
            fail(where + ": 期望 " + expected + "，实际 " + actual);
        }
    }

    private static JsonArray readGolden() {
        try (var reader = new InputStreamReader(
                Objects.requireNonNull(WinterskyOracleTest.class.getResourceAsStream("/wintersky/wintersky_cases.json")),
                StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonArray.class);
        } catch (Exception e) {
            throw new IllegalStateException("读取 wintersky_cases.json 失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> loadFixture(String name) {
        try (var reader = new InputStreamReader(
                Objects.requireNonNull(WinterskyOracleTest.class.getResourceAsStream("/wintersky/fixtures/" + name + ".json")),
                StandardCharsets.UTF_8)) {
            return (Map<String, Object>) JsonValues.toJava(GSON.fromJson(reader, JsonObject.class));
        } catch (Exception e) {
            throw new IllegalStateException("读取夹具 " + name + " 失败", e);
        }
    }
}
