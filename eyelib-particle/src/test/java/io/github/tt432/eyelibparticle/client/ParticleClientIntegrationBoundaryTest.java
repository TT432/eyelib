package io.github.tt432.eyelibparticle.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleClientIntegrationBoundaryTest {
    @Test
    void forgeHooksAreClientSideOnlyAndDelegateLifecycleWork() throws IOException {
        SourceCheck hooks = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderHooks.java");
        SourceCheck manager = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderManager.java");

        hooks.assertContains("@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)");
        hooks.assertContains("TickEvent.Phase.START");
        hooks.assertContains("RenderLevelStageEvent.Stage.AFTER_ENTITIES");
        hooks.assertContains("ClientPlayerNetworkEvent.LoggingOut");
        hooks.assertContains("ParticleRenderManager.INSTANCE.onRenderTickStart()");
        hooks.assertContains("ParticleRenderManager.INSTANCE.onClientTickStart()");
        hooks.assertContains("ParticleRenderManager.INSTANCE.clear()");

        manager.assertNotContains("@SubscribeEvent");
        manager.assertNotContains("RenderLevelStageEvent");
        manager.assertNotContains("TickEvent.");
        manager.assertNotContains("ClientPlayerNetworkEvent");
    }

    @Test
    void rendererOwnsMinecraftMaterialTextureAndBufferAdaptation() throws IOException {
        SourceCheck renderer = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/BedrockParticleRenderer.java");

        renderer.assertContains("RenderTypeResolver.resolve(new ResourceLocation(material))");
        renderer.assertContains("withSuffix(\".png\")");
        renderer.assertContains("Minecraft.getInstance().renderBuffers().bufferSource().getBuffer");
        renderer.assertContains("PoseStack");
        renderer.assertContains("VertexConsumer");
        renderer.assertContains("LightTexture.FULL_BRIGHT");
    }

    @Test
    void clientPackageDocumentsExplicitIntegrationException() throws IOException {
        SourceCheck packageInfo = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/package-info.java");

        packageInfo.assertContains("explicit client integration layer");
        packageInfo.assertContains("Dist.CLIENT");
        packageInfo.assertContains("runtime/** remains root/MC/Forge-clean");
    }

    private static SourceCheck source(String path) throws IOException {
        Path root = projectRoot();
        Path resolved = root.resolve(path);
        return new SourceCheck(path, stripCommentsAndStringLiterals(Files.readString(resolved)));
    }

    private static Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("MODULES.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate project root from user.dir");
    }

    private static String stripCommentsAndStringLiterals(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean lineComment = false;
        boolean blockComment = false;
        boolean stringLiteral = false;
        boolean charLiteral = false;
        boolean textBlock = false;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            char nextNext = i + 2 < source.length() ? source.charAt(i + 2) : '\0';

            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    result.append(current);
                } else {
                    result.append(' ');
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    result.append("  ");
                    i++;
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }
            if (textBlock) {
                if (current == '"' && next == '"' && nextNext == '"') {
                    textBlock = false;
                    result.append("   ");
                    i += 2;
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }
            if (stringLiteral) {
                if (current == '\\' && next != '\0') {
                    result.append("  ");
                    i++;
                } else if (current == '"') {
                    stringLiteral = false;
                    result.append(' ');
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }
            if (charLiteral) {
                if (current == '\\' && next != '\0') {
                    result.append("  ");
                    i++;
                } else if (current == '\'') {
                    charLiteral = false;
                    result.append(' ');
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }

            if (current == '/' && next == '/') {
                lineComment = true;
                result.append("  ");
                i++;
            } else if (current == '/' && next == '*') {
                blockComment = true;
                result.append("  ");
                i++;
            } else if (current == '"' && next == '"' && nextNext == '"') {
                textBlock = true;
                result.append("   ");
                i += 2;
            } else if (current == '"') {
                stringLiteral = true;
                result.append(' ');
            } else if (current == '\'') {
                charLiteral = true;
                result.append(' ');
            } else {
                result.append(current);
            }
        }

        return result.toString();
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
