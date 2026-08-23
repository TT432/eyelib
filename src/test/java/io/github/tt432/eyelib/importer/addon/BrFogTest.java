package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BrFog#parse} 的全字段解析、缺省回落与非法 identifier 拒绝。
 *
 * @author TT432
 */
class BrFogTest {

    private static BrFog parse(String json) {
        return BrFog.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void parsesFullDocument() {
        BrFog fog = parse("""
                {
                  "format_version": "1.16.100",
                  "minecraft:fog_settings": {
                    "description": { "identifier": "custom_pack:example" },
                    "distance": {
                      "air": {
                        "fog_start": 0.92,
                        "fog_end": 1.0,
                        "fog_color": "#ABD2FF",
                        "render_distance_type": "render"
                      },
                      "water": {
                        "fog_start": 0,
                        "fog_end": 60.0,
                        "fog_color": "#44AFF5",
                        "render_distance_type": "fixed",
                        "transition_fog": { "mid_seconds": 5 }
                      },
                      "lava": {
                        "fog_start": 0.0,
                        "fog_end": 0.64,
                        "fog_color": "#991A00",
                        "render_distance_type": "fixed"
                      }
                    },
                    "volumetric": { "density": { "air": { "max_density": 0.5 } } }
                  }
                }
                """);

        assertEquals("custom_pack:example", fog.identifier());
        assertEquals(3, fog.distances().size());

        BrFog.DistanceSetting air = fog.distance(BrFog.MEDIUM_AIR);
        assertEquals(0.92f, air.fogStart(), 1e-6);
        assertEquals(1.0f, air.fogEnd(), 1e-6);
        assertEquals("render", air.renderDistanceType());
        assertTrue(air.renderRelative());
        assertEquals("#ABD2FF", air.fogColor());

        // transition_fog / volumetric 忽略不解析，但不影响已知字段
        BrFog.DistanceSetting water = fog.distance("water");
        assertFalse(water.renderRelative());
        assertEquals(60.0f, water.fogEnd(), 1e-6);
        assertNull(fog.distance("lava_resistance"));
    }

    @Test
    void missingOptionalFieldsFallBackToNeutralDefaults() {
        BrFog fog = parse("""
                {
                  "format_version": "1.16.100",
                  "minecraft:fog_settings": {
                    "description": { "identifier": "pack:minimal" },
                    "distance": { "air": {} }
                  }
                }
                """);

        BrFog.DistanceSetting air = fog.distance(BrFog.MEDIUM_AIR);
        assertEquals(0.0f, air.fogStart(), 1e-6);
        assertEquals(1.0f, air.fogEnd(), 1e-6);
        assertTrue(air.renderRelative());
        assertEquals("#FFFFFF", air.fogColor());
    }

    @Test
    void missingDistanceObjectYieldsEmptyDistances() {
        BrFog fog = parse("""
                {
                  "format_version": "1.16.100",
                  "minecraft:fog_settings": {
                    "description": { "identifier": "pack:nodistance" }
                  }
                }
                """);

        assertTrue(fog.distances().isEmpty());
        assertNull(fog.distance(BrFog.MEDIUM_AIR));
    }

    @Test
    void missingIdentifierThrows() {
        assertThrows(IllegalArgumentException.class, () -> parse("""
                { "format_version": "1.16.100",
                  "minecraft:fog_settings": { "description": {} } }
                """));
    }

    @Test
    void identifierWithoutNamespaceThrows() {
        assertThrows(IllegalArgumentException.class, () -> parse("""
                { "format_version": "1.16.100",
                  "minecraft:fog_settings": { "description": { "identifier": "no_namespace" } } }
                """));
    }

    @Test
    void missingSettingsObjectThrows() {
        assertThrows(IllegalArgumentException.class, () -> parse("""
                { "format_version": "1.16.100" }
                """));
    }

    @Test
    void renderTypeValuesScaleByRenderDistanceBlocks() {
        BrFog.DistanceSetting render = new BrFog.DistanceSetting(0.5f, 0.9f, "render", "#000000");
        assertEquals(80.0f, render.fogStartBlocks(160.0f), 1e-6);
        assertEquals(144.0f, render.fogEndBlocks(160.0f), 1e-6);

        BrFog.DistanceSetting fixed = new BrFog.DistanceSetting(2.0f, 4.0f, "fixed", "#000000");
        assertEquals(2.0f, fixed.fogStartBlocks(160.0f), 1e-6);
        assertEquals(4.0f, fixed.fogEndBlocks(160.0f), 1e-6);
    }

    @Test
    void fogColorParsesToRgbFloats() {
        BrFog.DistanceSetting setting = new BrFog.DistanceSetting(0, 1, "fixed", "#FF8080");
        float[] rgb = setting.rgbFloats();
        assertEquals(1.0f, rgb[0], 1e-6);
        assertEquals(128 / 255.0f, rgb[1], 1e-3);
        assertEquals(128 / 255.0f, rgb[2], 1e-3);

        // 非法颜色回落白色
        BrFog.DistanceSetting bad = new BrFog.DistanceSetting(0, 1, "fixed", "not-a-color");
        assertEquals(1.0f, bad.rgbFloats()[0], 1e-6);
    }
}
