package io.github.tt432.eyelib.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderMixinAccessorOwnershipTest {
    @Test
    void livingEntityRendererAccessorIsRenderOwnedTechnicalMixinWiring() throws IOException {
        SourceCheck accessor = source("src/main/java/io/github/tt432/eyelib/mc/impl/mixin/LivingEntityRendererAccessor.java");
        SourceCheck renderAction = source("src/main/java/io/github/tt432/eyelib/client/render/SimpleRenderAction.java");
        SourceCheck mixinConfig = source("src/main/resources/eyelib.mixins.json");
        String docs = readDocs(
                "MODULES.md",
                "docs/architecture/01-module-boundaries.md",
                "docs/architecture/04-mc-debt-ledger.md",
                "src/main/java/io/github/tt432/eyelib/client/render/README.md",
                "src/main/java/io/github/tt432/eyelib/mc/impl/mixin/README.md"
        );

        assertAll(
                () -> accessor.assertContains("package io.github.tt432.eyelib.mc.impl.mixin;"),
                () -> accessor.assertContains("@Mixin(LivingEntityRenderer.class)"),
                () -> accessor.assertContains("float callGetWhiteOverlayProgress"),
                () -> renderAction.assertContains("import io.github.tt432.eyelib.mc.impl.mixin.LivingEntityRendererAccessor;"),
                () -> renderAction.assertContains("((LivingEntityRendererAccessor) (event.getRenderer())).callGetWhiteOverlayProgress"),
                () -> mixinConfig.assertContains("\"package\": \"io.github.tt432.eyelib.mc.impl.mixin\""),
                () -> mixinConfig.assertContains("\"LivingEntityRendererAccessor\""),
                () -> assertTrue(docs.contains("FM-015")),
                () -> assertTrue(docs.contains("client-render-owned")),
                () -> assertTrue(docs.contains("technical mixin wiring")),
                () -> assertTrue(docs.contains("one shared package root"))
        );
    }

    private static SourceCheck source(String path) throws IOException {
        return new SourceCheck(path, Files.readString(Path.of(path)));
    }

    private static String readDocs(String... paths) throws IOException {
        StringBuilder docs = new StringBuilder();
        for (String path : paths) {
            docs.append(Files.readString(Path.of(path))).append('\n');
        }
        return docs.toString();
    }

    private record SourceCheck(String path, String content) {
        void assertContains(String expected) {
            assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
        }
    }
}
