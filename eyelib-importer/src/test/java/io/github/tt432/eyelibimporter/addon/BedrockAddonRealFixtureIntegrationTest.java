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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class BedrockAddonRealFixtureIntegrationTest {
    private static final String FIXTURE_ROOT = "/io/github/tt432/eyelibimporter/addon/fixtures/microsoft-shapeshifter";

    @TempDir
    Path tempDir;

    @Test
    void loadsDownloadedOfficialAddonFolderFixture() throws Exception {
        BedrockAddon addon = BedrockAddonLoader.load(fixtureRootPath());

        assertOfficialFixtureLoaded(addon);
    }

    @Test
    void loadsDownloadedOfficialAddonAsMcaddon() throws Exception {
        Path addonArchive = tempDir.resolve("microsoft-shapeshifter.mcaddon");
        zipDirectory(fixtureRootPath(), addonArchive);

        BedrockAddon addon = BedrockAddonLoader.load(addonArchive);

        assertOfficialFixtureLoaded(addon);
    }

    private static void assertOfficialFixtureLoaded(BedrockAddon addon) {
        assertEquals(2, addon.packs().size());
        assertEquals(1, addon.resourcePacks().size());
        assertEquals(1, addon.dataPacks().size());

        assertTrue(addon.aggregate().resourcePack().clientEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.aggregate().behaviorPack().behaviorEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.aggregate().clientEntities().containsKey("sample:shapeshifter"));
        var shapeshifter = addon.aggregate().clientEntities().get("sample:shapeshifter");
        assertTrue(addon.aggregate().attachables().isEmpty());
        assertTrue(addon.aggregate().animationControllers().containsKey("controller.animation.shapeshifter.phase_change"));
        assertTrue(addon.aggregate().renderControllerFiles().containsKey("render_controllers/shapeshifter.render_controllers.json"));
        assertTrue(addon.aggregate().flattenedRenderControllers().containsKey("controller.render.shapeshifter"));
        assertTrue(addon.aggregate().animations().containsKey("animation.shapeshifter.phase_change"));
        assertTrue(addon.aggregate().models().containsKey("geometry.shapeshifter"));
        assertTrue(addon.aggregate().particleFiles().containsKey("particles/witchspell.json"));
        assertTrue(addon.aggregate().particlesByIdentifier().containsKey("sample:witchspell_emitter"));
        var witchspellFlipbook = addon.aggregate().particleFiles().get("particles/witchspell.json")
                .particleEffect().billboardFlipbook().orElseThrow();
        assertTrue(addon.aggregate().textures().containsKey("textures/entity/shapeshifter.png"));
        assertTrue(addon.aggregate().soundIndexFiles().containsKey("sounds.json"));
        assertTrue(addon.aggregate().soundDefinitionFiles().containsKey("sounds/sound_definitions.json"));
        assertTrue(addon.aggregate().languageFiles().containsKey("texts/en_US.lang"));
        assertTrue(addon.aggregate().behaviorEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.aggregate().soundFiles().containsKey("sounds/shapeshifter/phase_change.ogg"));
        assertTrue(addon.aggregate().textureMetadataFiles().containsKey("textures/render_controllers/shapeshifter.render_controllers.json"));
        assertEquals("1.8.0", shapeshifter.min_engine_version().orElseThrow().semanticString());
        assertEquals(7, shapeshifter.animation_controllers().size());
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
        assertEquals("#a8d89b", ((BedrockResourceValue.StringValue) shapeshifter.spawn_egg().orElseThrow().values().get("base_color")).value());
        assertEquals("#ddeb61", ((BedrockResourceValue.StringValue) shapeshifter.spawn_egg().orElseThrow().values().get("overlay_color")).value());
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