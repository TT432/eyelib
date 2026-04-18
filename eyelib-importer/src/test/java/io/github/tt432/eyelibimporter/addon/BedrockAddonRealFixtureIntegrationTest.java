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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(addon.clientEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.attachables().isEmpty());
        assertTrue(addon.animationControllers().containsKey("controller.animation.shapeshifter.phase_change"));
        assertTrue(addon.renderControllerFiles().containsKey("render_controllers/shapeshifter.render_controllers.json"));
        assertTrue(addon.flattenedRenderControllers().containsKey("controller.render.shapeshifter"));
        assertTrue(addon.animations().containsKey("animation.shapeshifter.phase_change"));
        assertTrue(addon.models().containsKey("geometry.shapeshifter"));
        assertTrue(addon.particleFiles().containsKey("particles/witchspell.json"));
        assertTrue(addon.particlesByIdentifier().containsKey("sample:witchspell_emitter"));
        assertTrue(addon.textures().containsKey("textures/entity/shapeshifter.png"));
        assertTrue(addon.soundIndexFiles().containsKey("sounds.json"));
        assertTrue(addon.soundDefinitionFiles().containsKey("sounds/sound_definitions.json"));
        assertTrue(addon.languageFiles().containsKey("texts/en_US.lang"));
        assertTrue(addon.behaviorEntities().containsKey("sample:shapeshifter"));
        assertTrue(addon.soundFiles().containsKey("sounds/shapeshifter/phase_change.ogg"));
        assertEquals(1, addon.unmanagedResources().size());
        assertTrue(addon.unmanagedResources().containsKey("shapeshifter:textures/render_controllers/shapeshifter.render_controllers.json"));
        assertEquals(BedrockUnmanagedReason.TEXTURE_SIDE_METADATA,
                addon.unmanagedResources().get("shapeshifter:textures/render_controllers/shapeshifter.render_controllers.json").reason());
        assertTrue(addon.warnings().stream().anyMatch(warning ->
                warning.code() == BedrockAddonWarningCode.UNMANAGED_RESOURCE
                        && "textures/render_controllers/shapeshifter.render_controllers.json".equals(warning.relativePath())));
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
