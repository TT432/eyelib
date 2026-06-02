package io.github.tt432.eyelibimporter.addon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** @author TT432 */
class BedrockAddonRealFixtureIntegrationTest {
    private static final String FIXTURE_ROOT = "/io/github/tt432/eyelibimporter/addon/fixtures/microsoft-shapeshifter";

    @TempDir
    Path tempDir;

    @Test
    void loadsDownloadedOfficialAddonFolderFixture() throws Exception {
        BedrockAddon addon = BedrockAddonLoader.load(fixtureRootPath());

        assertPackCounts(addon);
        assertEntitiesLoaded(addon);
        assertAnimationsLoaded(addon);
        assertRenderControllersLoaded(addon);
        assertParticlesLoaded(addon);
        assertTexturesLoaded(addon);
    }

    @Test
    void loadsDownloadedOfficialAddonAsMcaddon() throws Exception {
        Path addonArchive = tempDir.resolve("microsoft-shapeshifter.mcaddon");
        zipDirectory(fixtureRootPath(), addonArchive);

        BedrockAddon addon = BedrockAddonLoader.load(addonArchive);

        assertPackCounts(addon);
        assertEntitiesLoaded(addon);
        assertAnimationsLoaded(addon);
        assertRenderControllersLoaded(addon);
        assertParticlesLoaded(addon);
        assertTexturesLoaded(addon);
    }

    private static void assertPackCounts(BedrockAddon addon) {
        assertEquals(2, addon.packs().size());
        assertEquals(1, addon.resourcePacks().size());
        assertEquals(1, addon.dataPacks().size());
    }

    private static void assertEntitiesLoaded(BedrockAddon addon) {
        assertTrue(addon.aggregate().resourcePack().clientEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.aggregate().behaviorPack().behaviorEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.aggregate().clientEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.aggregate().behaviorEntities().containsKey("sample:shapeshifter"));

        var shapeshifter = addon.aggregate().clientEntities().get("sample:shapeshifter");
        assertEquals("1.8.0", shapeshifter.min_engine_version().orElseThrow().semanticString());
        assertEquals(7, shapeshifter.animation_controllers().size());

        assertEquals("#a8d89b", ((BedrockResourceValue.StringValue) shapeshifter.spawn_egg().orElseThrow().values().get("base_color")).value());
        assertEquals("#ddeb61", ((BedrockResourceValue.StringValue) shapeshifter.spawn_egg().orElseThrow().values().get("overlay_color")).value());
    }

    private static void assertAnimationsLoaded(BedrockAddon addon) {
        assertTrue(addon.aggregate().animations().containsKey("animation.shapeshifter.phase_change"));
        assertTrue(addon.aggregate().animationControllers().containsKey("controller.animation.shapeshifter.phase_change"));
        assertTrue(addon.aggregate().models().containsKey("geometry.shapeshifter"));
    }

    private static void assertRenderControllersLoaded(BedrockAddon addon) {
        assertTrue(addon.aggregate().renderControllerFiles().containsKey("render_controllers/shapeshifter.render_controllers.json"));
        assertTrue(addon.aggregate().flattenedRenderControllers().containsKey("controller.render.shapeshifter"));
        assertTrue(addon.aggregate().textureMetadataFiles().containsKey("textures/render_controllers/shapeshifter.render_controllers.json"));
    }

    private static void assertParticlesLoaded(BedrockAddon addon) {
        assertTrue(addon.aggregate().particleFiles().containsKey("particles/witchspell.json"));
        assertTrue(addon.aggregate().particlesByIdentifier().containsKey("sample:witchspell_emitter"));

        var witchspellFlipbook = addon.aggregate().particleFiles().get("particles/witchspell.json")
                .particleEffect().billboardFlipbook().orElseThrow();
        assertEquals(128, witchspellFlipbook.textureWidth());
        assertEquals(128, witchspellFlipbook.textureHeight());
        assertEquals("64", witchspellFlipbook.baseUV().x());
        assertEquals("72", witchspellFlipbook.baseUV().y());
        assertEquals("8", witchspellFlipbook.sizeUV().x());
        assertEquals("8", witchspellFlipbook.sizeUV().y());
        assertEquals("-8", witchspellFlipbook.stepUV().x());
        assertEquals("0", witchspellFlipbook.stepUV().y());
        assertEquals("10", witchspellFlipbook.framesPerSecond());
        assertEquals("8", witchspellFlipbook.maxFrame());
        assertTrue(witchspellFlipbook.stretchToLifetime());
        assertFalse(witchspellFlipbook.loop());
    }

    private static void assertTexturesLoaded(BedrockAddon addon) {
        assertTrue(addon.aggregate().textures().containsKey("textures/entity/shapeshifter.png"));
        assertTrue(addon.aggregate().soundIndexFiles().containsKey("sounds.json"));
        assertTrue(addon.aggregate().soundDefinitionFiles().containsKey("sounds/sound_definitions.json"));
        assertTrue(addon.aggregate().languageFiles().containsKey("texts/en_US.lang"));
        assertTrue(addon.aggregate().soundFiles().containsKey("sounds/shapeshifter/phase_change.ogg"));
        assertTrue(addon.aggregate().attachables().isEmpty());
        assertTrue(addon.unmanagedResources().isEmpty());
        assertFalse(addon.warnings().stream().anyMatch(warning ->
                warning.code() == BedrockAddonWarningCode.UNMANAGED_RESOURCE));
        assertNotNull(addon.resourcePacks().get(0).packIcon());
    }

    private static Path fixtureRootPath() throws URISyntaxException {
        return Path.of(Objects.requireNonNull(
                BedrockAddonRealFixtureIntegrationTest.class.getResource(FIXTURE_ROOT)
        ).toURI());
    }

    private static void zipDirectory(Path sourceDir, Path archivePath) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(archivePath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
             var stream = Files.walk(sourceDir)) {
            stream.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> {
                        try {
                            String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                            zipOutputStream.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zipOutputStream);
                            zipOutputStream.closeEntry();
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }
    }
}
