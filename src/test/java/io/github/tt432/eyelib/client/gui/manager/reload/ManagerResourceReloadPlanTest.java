package io.github.tt432.eyelib.client.gui.manager.reload;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagerResourceReloadPlanTest {
    @Test
    void classifySingleFileRoutesKnownJsonFolders() {
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.ANIMATION_JSON,
                ManagerResourceReloadPlan.classifySingleFile("animations/a.json"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.ANIMATION_CONTROLLER_JSON,
                ManagerResourceReloadPlan.classifySingleFile("animation_controllers/a.json"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.RENDER_CONTROLLER_JSON,
                ManagerResourceReloadPlan.classifySingleFile("render_controllers/a.json"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.ENTITY_JSON,
                ManagerResourceReloadPlan.classifySingleFile("entity/a.json"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.PARTICLE_JSON,
                ManagerResourceReloadPlan.classifySingleFile("particles/a.json"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.MODEL_JSON,
                ManagerResourceReloadPlan.classifySingleFile("models/a.json"));
    }

    @Test
    void classifySingleFileHandlesModelAndTextureFiles() {
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.MODEL_BBMODEL,
                ManagerResourceReloadPlan.classifySingleFile("models/a.bbmodel"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.TEXTURE_PNG,
                ManagerResourceReloadPlan.classifySingleFile("textures/a.png"));
    }

    @Test
    void classifySingleFileReturnsUnsupportedForOtherPaths() {
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.UNSUPPORTED,
                ManagerResourceReloadPlan.classifySingleFile("textures/a.jpg"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.UNSUPPORTED,
                ManagerResourceReloadPlan.classifySingleFile("notes/readme.json"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.UNSUPPORTED,
                ManagerResourceReloadPlan.classifySingleFile("models/a.PNG"));
    }

    @Test
    void classifySingleFileNormalizesBackslashes() {
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.ANIMATION_JSON,
                ManagerResourceReloadPlan.classifySingleFile("animations\\a.json"));
        assertEquals(ManagerResourceReloadPlan.ReloadTarget.TEXTURE_PNG,
                ManagerResourceReloadPlan.classifySingleFile("textures\\a.png"));
    }

    @Test
    void classifySingleFileFromPathsUsesRelativeRoute() {
        Path basePath = Path.of("assets");

        assertEquals(ManagerResourceReloadPlan.ReloadTarget.MODEL_JSON,
                ManagerResourceReloadPlan.classifySingleFile(basePath, basePath.resolve("models/example.json")));
    }

    @Test
    void toTextureKeyBuildsLowercaseForwardSlashKey() {
        Path basePath = Path.of("assets");

        assertEquals("textures/item/layer.png",
                ManagerResourceReloadPlan.toTextureKey(basePath, basePath.resolve("textures/Item/Layer.PNG")));
    }
}
