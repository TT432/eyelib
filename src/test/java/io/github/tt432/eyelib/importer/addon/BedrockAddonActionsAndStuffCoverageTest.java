package io.github.tt432.eyelib.importer.addon;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用真实 Actions-and-Stuff 1.10 v2 样本包验证加载覆盖率，包括子包层叠覆盖。
 * 此测试依赖开发者本地的 {@code Actions-and-Stuff-1.10-v2.mcpack} addon 文件，
 * 该文件版权归属于 Actions-and-Stuff 团队，不能提交到仓库中。
 * 如需运行，将样本包放置到 {@code run/resourcepacks/} 目录下，并移除 {@code @Disabled} 注解。
 *
 * @author TT432
 */
@Disabled("需要本地 Actions-and-Stuff addon 文件 (run/resourcepacks/Actions-and-Stuff-1.10-v2.mcpack)")
class BedrockAddonActionsAndStuffCoverageTest {

    private static final Path SAMPLE_PATH = Path.of("run/resourcepacks/Actions-and-Stuff-1.10-v2.mcpack");

    @Test
    void loadsSampleAndReportsCoverageStatistics() throws Exception {
        BedrockAddon addon = BedrockAddonLoader.load(SAMPLE_PATH);
        System.out.println("loaded packs: " + addon.packs().size());
        assertEquals(1, addon.packs().size());

        var rp = addon.resourcePacks().get(0);
        System.out.println("textures: " + rp.textures().size());
        assertTrue(rp.textures().size() > 1000, "应至少有1000张纹理");

        int tga = 0;
        for (String path : rp.textures().keySet()) {
            if (path.toLowerCase().endsWith(".tga")) tga++;
        }
        System.out.println("TGA count: " + tga);
        assertTrue(tga > 50, "应有至少50张TGA被解码");

        int unmanaged = addon.unmanagedResources().size();
        System.out.println("unmanaged: " + unmanaged);
        Map<String, Long> breakdown = addon.unmanagedResources().values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.reason() + " / " + r.family(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.counting()));
        breakdown.forEach((k, v) -> System.out.println("  " + k + ": " + v));

        addon.warnings().stream()
                .filter(w -> w.relativePath() != null)
                .filter(w -> w.code() == BedrockAddonWarningCode.SCHEMA_PARSE_FAILED)
                .forEach(w -> System.out.println("  PARSE_FAILED: " + w.relativePath() + " — " + w.message()));

        addon.unmanagedResources().values().stream()
                .filter(r -> r.family() == BedrockResourceFamily.UNKNOWN_JSON)
                .forEach(r -> System.out.println("  UNKNOWN_JSON: " + r.relativePath()));
        assertTrue(unmanaged > 0, "应有未托管资源（brarchive、异常文件等）");

        System.out.println("manifest subpacks: " + rp.manifest().subpacks().size());
        assertEquals(3, rp.manifest().subpacks().size());
        assertNotNull(rp.manifest().header().packOptimizationVersion());
        assertNotNull(rp.packIcon());

        var creeper = addon.aggregate().clientEntities().get("minecraft:creeper");
        assertNotNull(creeper, "应加载 creeper client entity");
        assertEquals(16, creeper.render_controllers().size(), "creeper 应保留16个 render controller 引用");
        var renderControllers = addon.aggregate().flattenedRenderControllers();
        for (String renderController : creeper.render_controllers()) {
            assertTrue(renderControllers.containsKey(renderController),
                    "creeper 引用的 render controller 应存在: " + renderController);
        }
    }
}
