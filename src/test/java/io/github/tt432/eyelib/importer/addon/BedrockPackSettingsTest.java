package io.github.tt432.eyelib.importer.addon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BedrockPackSetting} 解析、{@link BedrockPackSettingsCatalog}、
 * {@link BedrockPackSettingsStore} 持久化、{@link BedrockPackSettingsService} 解析、
 * 以及 {@link BedrockAddonLoader#load(Path, String)} 的 subpack override。
 */
class BedrockPackSettingsTest {
    @TempDir
    Path tempDir;

    // ---------- BedrockPackSetting 解析 ----------

    @Test
    void parsesAllSettingTypes() {
        BedrockPackManifest manifest = BedrockPackManifest.parse(
                com.google.gson.JsonParser.parseString(manifestWithSettings("""
                        ,
                        "settings": [
                          {"type": "label", "text": "text.pack.intro"},
                          {"type": "toggle", "text": "Explosion Hands", "name": "mypack:hands", "default": true},
                          {"type": "slider", "text": "Power", "name": "mypack:power", "min": 1, "max": 4, "step": 1, "default": 3},
                          {"type": "dropdown", "text": "Level", "name": "mypack:level", "default": "high",
                           "options": [{"name": "high", "text": "High"}, {"name": "low", "text": "text.pack.low"}]},
                          {"text": "no type — skipped"},
                          {"type": "unknown_widget", "name": "mypack:x"}
                        ]
                        """)).getAsJsonObject());

        List<BedrockPackSetting> settings = BedrockPackSetting.parseList(manifest.settings());

        assertEquals(4, settings.size());
        assertEquals(BedrockPackSetting.Type.LABEL, settings.get(0).type());
        assertNull(settings.get(0).name());
        assertEquals(BedrockPackSetting.Type.TOGGLE, settings.get(1).type());
        assertTrue(settings.get(1).defaultBoolean());
        assertEquals(BedrockPackSetting.Type.SLIDER, settings.get(2).type());
        assertEquals(3.0, settings.get(2).defaultNumber());
        assertEquals(1.0, settings.get(2).min());
        assertEquals(4.0, settings.get(2).max());
        assertEquals(BedrockPackSetting.Type.DROPDOWN, settings.get(3).type());
        assertEquals("high", settings.get(3).defaultOption());
        assertEquals(2, settings.get(3).options().size());
        assertEquals("text.pack.low", settings.get(3).options().get(1).text());
    }

    @Test
    void sliderSnapAndClamp() {
        BedrockPackSetting slider = BedrockPackSetting.parseList(BedrockPackManifest.parse(
                com.google.gson.JsonParser.parseString(manifestWithSettings("""
                        ,
                        "settings": [{"type": "slider", "name": "mypack:s", "min": 1, "max": 4, "step": 1, "default": 99}]
                        """)).getAsJsonObject()).settings()).get(0);
        assertEquals(4.0, slider.clampedDefault());
        assertEquals(3.0, slider.snapToStep(2.6));
        assertEquals(1.0, slider.snapToStep(-5));
    }

    // ---------- Catalog ----------

    @Test
    void catalogLoadsManifestAndLangFromZip() throws Exception {
        Path packDir = tempDir.resolve("catalog-pack");
        writeString(packDir.resolve("manifest.json"), manifestWithSettings("""
                ,
                "subpacks": [{"folder_name": "spa", "name": "Sub A", "memory_performance_tier": 0}],
                "settings": [{"type": "toggle", "text": "text.pack.toggle", "name": "mypack:t", "default": false}]
                """));
        writeString(packDir.resolve("texts/en_US.lang"),
                "pack.name=Pretty Pack\ntext.pack.toggle=Fancy Toggle\t# trailing comment\n");
        Path zip = zipDirectoryTo(packDir, tempDir.resolve("catalog.mcpack"));

        BedrockPackSettingsCatalog catalog = BedrockPackSettingsCatalog.load(zip);

        assertNotNull(catalog);
        assertEquals("catalog.mcpack", catalog.packKey());
        assertEquals(1, catalog.subpacks().size());
        assertEquals(1, catalog.settings().size());
        assertTrue(catalog.hasSettings());
        assertEquals("Pretty Pack", catalog.displayText("pack.name"));
        assertEquals("Fancy Toggle", catalog.displayText("text.pack.toggle"));
        assertEquals("unresolved.key", catalog.displayText("unresolved.key"));
    }

    @Test
    void catalogReturnsNullForNonPack() throws Exception {
        Path junk = tempDir.resolve("junk.mcpack");
        Files.writeString(junk, "not a zip");
        assertNull(BedrockPackSettingsCatalog.load(junk));
    }

    // ---------- Store 持久化 ----------

    @Test
    void storePersistsChoicesAcrossInit() throws Exception {
        BedrockPackSettingsStore.ensureInitialized(tempDir);
        BedrockPackSettingsStore.setSubpack("pack-a.mcpack", "SP1");
        BedrockPackSettingsStore.setValue("pack-a.mcpack", "mypack:t", true);
        BedrockPackSettingsStore.setValue("pack-a.mcpack", "mypack:s", 2.5);
        BedrockPackSettingsStore.setValue("pack-a.mcpack", "mypack:d", "gold");

        Path saved = tempDir.resolve("config/eyelib/bedrock-pack-settings.json");
        assertTrue(Files.isRegularFile(saved));

        // 拷到另一个 game dir 再 init，验证从磁盘读回
        Path other = tempDir.resolve("other-root");
        Path copied = other.resolve("config/eyelib/bedrock-pack-settings.json");
        Files.createDirectories(copied.getParent());
        Files.copy(saved, copied);
        BedrockPackSettingsStore.ensureInitialized(other);

        assertEquals("SP1", BedrockPackSettingsStore.subpackOverride("pack-a.mcpack").orElseThrow());
        assertTrue(BedrockPackSettingsStore.toggleValue("pack-a.mcpack", "mypack:t").orElseThrow());
        assertEquals(2.5, BedrockPackSettingsStore.sliderValue("pack-a.mcpack", "mypack:s").orElseThrow());
        assertEquals("gold", BedrockPackSettingsStore.dropdownValue("pack-a.mcpack", "mypack:d").orElseThrow());
        assertTrue(BedrockPackSettingsStore.subpackOverride("pack-b.mcpack").isEmpty());
    }

    // ---------- Service 解析 ----------

    @Test
    void serviceResolvesDefaultThenUserOverride() {
        BedrockPackSettingsStore.ensureInitialized(tempDir.resolve("svc"));
        BedrockPackSetting toggle = toggleSetting("svc:flag", true);
        BedrockPackSetting dropdown = dropdownSetting("svc:color", "pink", "pink", "gold");
        BedrockPackSetting slider = sliderSetting("svc:power", 3.0);
        BedrockPackSettingsService.updateActivePacks(List.of(
                new BedrockPackSettingsService.ActivePack("svc.mcpack", List.of(toggle, dropdown, slider))));

        // 默认值
        assertTrue(BedrockPackSettingsService.isPackSettingEnabled("svc:flag"));
        assertTrue(BedrockPackSettingsService.isPackSettingSelected("svc:color", "pink"));
        assertFalse(BedrockPackSettingsService.isPackSettingSelected("svc:color", "gold"));
        assertEquals(3.0, BedrockPackSettingsService.packSettingValue("svc:power"));

        // 用户覆盖
        BedrockPackSettingsStore.setValue("svc.mcpack", "svc:flag", false);
        BedrockPackSettingsStore.setValue("svc.mcpack", "svc:color", "gold");
        BedrockPackSettingsStore.setValue("svc.mcpack", "svc:power", 4.0);
        assertFalse(BedrockPackSettingsService.isPackSettingEnabled("svc:flag"));
        assertTrue(BedrockPackSettingsService.isPackSettingSelected("svc:color", "gold"));
        assertEquals(4.0, BedrockPackSettingsService.packSettingValue("svc:power"));

        // 类型不匹配 / 未知名 → 中性值
        assertFalse(BedrockPackSettingsService.isPackSettingEnabled("svc:color"));
        assertEquals(0.0, BedrockPackSettingsService.packSettingValue("svc:flag"));
        assertFalse(BedrockPackSettingsService.isPackSettingSelected("svc:missing", "x"));

        BedrockPackSettingsService.updateActivePacks(List.of());
        assertFalse(BedrockPackSettingsService.isPackSettingEnabled("svc:flag"));
    }

    @Test
    void servicePrefersHigherPriorityPack() {
        BedrockPackSetting bottom = toggleSetting("prio:flag", false);
        BedrockPackSetting top = toggleSetting("prio:flag", true);
        // listPacks 顺序 = 底→顶；同名设置顶包胜出
        BedrockPackSettingsService.updateActivePacks(List.of(
                new BedrockPackSettingsService.ActivePack("bottom.mcpack", List.of(bottom)),
                new BedrockPackSettingsService.ActivePack("top.mcpack", List.of(top))));
        assertTrue(BedrockPackSettingsService.isPackSettingEnabled("prio:flag"));
        BedrockPackSettingsService.updateActivePacks(List.of());
    }

    // ---------- Loader subpack override ----------

    @Test
    void loaderAppliesSubpackOverride() throws Exception {
        Path zip = subpackFixture();

        BedrockAddon auto = BedrockAddonLoader.load(zip);
        assertEquals("spb", auto.packs().get(0).selectedSubpack());
        assertTrue(auto.aggregate().textures().containsKey("textures/b.png"));
        assertFalse(auto.aggregate().textures().containsKey("textures/a.png"));

        BedrockAddon overridden = BedrockAddonLoader.load(zip, "spa");
        assertEquals("spa", overridden.packs().get(0).selectedSubpack());
        assertTrue(overridden.aggregate().textures().containsKey("textures/a.png"));
        assertFalse(overridden.aggregate().textures().containsKey("textures/b.png"));
    }

    @Test
    void loaderFallsBackWithWarningOnUnknownOverride() throws Exception {
        Path zip = subpackFixture();

        BedrockAddon addon = BedrockAddonLoader.load(zip, "no_such_subpack");

        assertEquals("spb", addon.packs().get(0).selectedSubpack());
        assertTrue(addon.warnings().stream().anyMatch(w ->
                w.code() == BedrockAddonWarningCode.SUBPACK_OVERRIDE_UNKNOWN));
    }

    // ---------- fixtures ----------

    private Path subpackFixture() throws Exception {
        Path packDir = tempDir.resolve("subpack-pack-" + UUID.randomUUID());
        writeString(packDir.resolve("manifest.json"), """
                {
                  "format_version": 3,
                  "header": {"name": "SP", "description": "", "uuid": "%s", "version": "1.0.0"},
                  "modules": [{"type": "resources", "uuid": "%s", "version": "1.0.0"}],
                  "metadata": {"authors": ["test"]},
                  "subpacks": [
                    {"folder_name": "spa", "name": "A", "memory_performance_tier": 0},
                    {"folder_name": "spb", "name": "B", "memory_performance_tier": 1}
                  ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        writePng(packDir.resolve("textures/root.png"), 0xFF000000);
        writePng(packDir.resolve("subpacks/spa/textures/a.png"), 0xFF111111);
        writePng(packDir.resolve("subpacks/spb/textures/b.png"), 0xFF222222);
        return zipDirectoryTo(packDir, tempDir.resolve("subpack-" + UUID.randomUUID() + ".mcpack"));
    }

    private static String manifestWithSettings(String extra) {
        return """
                {
                  "format_version": 3,
                  "header": {"name": "S", "description": "", "uuid": "%s", "version": "1.0.0"},
                  "modules": [{"type": "resources", "uuid": "%s", "version": "1.0.0"}],
                  "metadata": {"authors": ["test"]}%s
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), extra);
    }

    private static BedrockPackSetting toggleSetting(String name, boolean def) {
        return new BedrockPackSetting(BedrockPackSetting.Type.TOGGLE, name, name,
                def, 0, 0, 1, 0, null, List.of());
    }

    private static BedrockPackSetting dropdownSetting(String name, String def, String... options) {
        return new BedrockPackSetting(BedrockPackSetting.Type.DROPDOWN, name, name,
                false, 0, 0, 1, 0, def,
                java.util.Arrays.stream(options).map(o -> new BedrockPackSetting.Option(o, o)).toList());
    }

    private static BedrockPackSetting sliderSetting(String name, double def) {
        return new BedrockPackSetting(BedrockPackSetting.Type.SLIDER, name, name,
                false, def, 0, 4, 1, null, List.of());
    }

    private static void writeString(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.UTF_8);
    }

    private static void writePng(Path path, int argb) throws IOException {
        Files.createDirectories(path.getParent());
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, argb);
        ImageIO.write(image, "png", path.toFile());
    }

    private static Path zipDirectoryTo(Path sourceDir, Path archivePath) throws IOException {
        try (OutputStream out = Files.newOutputStream(archivePath);
             ZipOutputStream zip = new ZipOutputStream(out);
             var stream = Files.walk(sourceDir)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                zip.putNextEntry(new ZipEntry(sourceDir.relativize(path).toString().replace('\\', '/')));
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
        return archivePath;
    }
}
