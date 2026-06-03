package io.github.tt432.eyelib.client.particle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleApiDelegationBoundaryTest {
    @Test
    void retainedRootSpawnFacadeDelegatesToParticleModuleApiAndLegacyRegistryFacadesAreDeleted() throws IOException {
        // ParticleSpawnService 已被删除 —— 验证文件不存在
        assertTrue(Files.notExists(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java"
        )));

        SourceCheck adapter = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleSpawnRuntimeAdapter.java");
        SourceCheck loader = source("src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java");
        SourceCheck particleReadme = source("src/main/java/io/github/tt432/eyelib/client/particle/package-info.java");
        SourceCheck registryReadme = source("src/main/java/io/github/tt432/eyelib/client/registry/package-info.java");
        SourceCheck loaderReadme = source("src/main/java/io/github/tt432/eyelib/client/loader/package-info.java");
        SourceCheck moduleReadme = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java");
        SourceCheck moduleBoundaries = source("docs/decisions/0002-module-boundaries.md");
        SourceCheck sideBoundaries = source("docs/decisions/0003-side-boundaries.md");
        Path obsoleteRootRequest = Path.of("src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java");
        List<Path> deletedLegacyFacades = List.of(
                Path.of("src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java"),
                Path.of("src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java"),
                Path.of("src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java"),
                Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java")
        );

        deletedLegacyFacades.forEach(path -> assertTrue(Files.notExists(path), () -> path + " should remain deleted"));

        // ParticleSpawnRuntimeAdapter 现在接管了之前的 ParticleSpawnService 职责
        adapter.assertContains("implements ParticleSpawnApi");
        adapter.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;");
        adapter.assertContains("definitions.get(request.particleId())");
        adapter.assertContains("public void spawn(ParticleSpawnRequest request)");
        adapter.assertContains("renderManager.removeEmitter(spawnId);");

        loader.assertContains("ParticleResourcePublication.replaceFromJsonResources");
        loader.assertContains("entry.getKey().toString()");

        particleReadme.assertContains("粒子运行时");
        particleReadme.assertContains("渲染管理器");
        particleReadme.assertContains("查找/生成边界");
        assertTrue(Files.notExists(obsoleteRootRequest), () -> obsoleteRootRequest + " should not be reintroduced");
        registryReadme.assertContains("已解析客户端资产的发布与查询边界");
        loaderReadme.assertContains("资源重载监听器");
        loaderReadme.assertContains("解析入口");
        moduleReadme.assertContains("粒子定义");
        moduleReadme.assertContains("运行时");
        moduleReadme.assertContains("加载管线");
        moduleReadme.assertContains("客户端渲染");
        moduleBoundaries.assertContains("ParticleDefinitionRegistry");
        moduleBoundaries.assertContains("ParticleResourcePublication");
        moduleBoundaries.assertContains("ParticleDefinition.identifier()");
        moduleBoundaries.assertContains("Root legacy `client/particle/bedrock/**` schema/runtime tree has been deleted");
        sideBoundaries.assertContains("ResourceLocation adaptation for particle loading remains at root Forge/resource integration boundaries");
    }

    @Test
    void particleModuleMainSourcesRemainFreeOfRootMinecraftAndForgeImports() throws IOException {
        Path sourceRoot = Path.of("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle");
        List<String> forbiddenFragments = List.of(
                "import io.github.tt432.eyelib.client.",
                "import io.github.tt432.eyelib.network.",
                "import io.github.tt432.eyelib.capability.",
                "import io.github.tt432.eyelib.mc.impl.",
                "import net.minecraft.",
                "import net.minecraftforge."
        );

        try (var paths = Files.walk(sourceRoot)) {
            List<Path> violatingFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("\\client\\"))
                    .filter(path -> !path.toString().contains("/client/"))
                    .filter(path -> !path.toString().contains("\\network\\"))
                    .filter(path -> !path.toString().contains("/network/"))
                    .filter(path -> !path.getFileName().toString().equals("EyelibParticleMod.java"))
                    .filter(path -> containsAnyForbiddenImport(path, forbiddenFragments))
                    .toList();

            assertTrue(violatingFiles.isEmpty(), () -> "Forbidden particle module imports: " + violatingFiles);
        }
    }

    private static boolean containsAnyForbiddenImport(Path path, List<String> forbiddenFragments) {
        try {
            String source = Files.readString(path);
            return forbiddenFragments.stream().anyMatch(source::contains);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static SourceCheck source(String path) throws IOException {
        return new SourceCheck(path, Files.readString(Path.of(path)));
    }

    private record SourceCheck(String path, String content) {
        void assertContains(String expected) {
            assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
        }

        void assertNotContains(String unexpected) {
            assertFalse(content.contains(unexpected), () -> path + " should not contain: " + unexpected);
        }
    }
}
