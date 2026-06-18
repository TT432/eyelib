package io.github.tt432.eyelib.importer.render.controller;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TT432
 */
class BrRenderControllerEntryCodecTest {
    @Test
    void parsesIgnoreLightingFlag() {
        BrRenderControllers controllers = TestCodecUtil.unwrap(BrRenderControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                                                                                                                            {
                                                                                                                              "render_controllers": {
                                                                                                                                "controller.render.test": {
                                                                                                                                 "geometry": "Geometry.default",
                                                                                                                                 "textures": ["Texture.default"],
                                                                                                                                 "ignore_lighting": true,
                                                                                                                                 "materials": [{"*": "Material.default"}]
                                                                                                                               }
                                                                                                                              }
                                                                                                                            }
                                                                                                                            """))
                                                                    );

        assertTrue(controllers.renderControllers().get("controller.render.test").ignoreLighting());
    }

    @Test
    void defaultsIgnoreLightingToFalse() {
        BrRenderControllers controllers = TestCodecUtil.unwrap(BrRenderControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                                                                                                                            {
                                                                                                                              "render_controllers": {
                                                                                                                                "controller.render.test": {}
                                                                                                                              }
                                                                                                                            }
                                                                                                                            """))
                                                                    );

        assertFalse(controllers.renderControllers().get("controller.render.test").ignoreLighting());
    }
}
