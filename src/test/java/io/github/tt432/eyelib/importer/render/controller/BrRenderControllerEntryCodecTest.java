package io.github.tt432.eyelib.importer.render.controller;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TT432
 */
@NullMarked
class BrRenderControllerEntryCodecTest {
    @Test
    void parsesIgnoreLightingFlag() {
        BrRenderControllers controllers = BrRenderControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
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
                                                                   .getOrThrow(false, AssertionError::new);

        assertTrue(controllers.renderControllers().get("controller.render.test").ignoreLighting());
    }

    @Test
    void defaultsIgnoreLightingToFalse() {
        BrRenderControllers controllers = BrRenderControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                                                                                                                           {
                                                                                                                             "render_controllers": {
                                                                                                                               "controller.render.test": {}
                                                                                                                             }
                                                                                                                           }
                                                                                                                           """))
                                                                   .getOrThrow(false, AssertionError::new);

        assertFalse(controllers.renderControllers().get("controller.render.test").ignoreLighting());
    }
}
