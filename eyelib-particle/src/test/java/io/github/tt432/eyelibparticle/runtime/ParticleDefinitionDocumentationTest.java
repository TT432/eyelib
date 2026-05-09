package io.github.tt432.eyelibparticle.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleDefinitionDocumentationTest {
    @Test
    void ownershipDocumentsNameCanonicalOwnersLegacyRootStatusAndMappedFields() throws IOException {
        SourceCheck modules = source("MODULES.md");
        SourceCheck moduleBoundaries = source("docs/architecture/01-module-boundaries.md");
        SourceCheck sideBoundaries = source("docs/architecture/02-side-boundaries.md");
        SourceCheck particleReadme = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md");
        SourceCheck runtimePackageInfo = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java");
        SourceCheck rootParticleReadme = source("src/main/java/io/github/tt432/eyelib/client/particle/README.md");

        modules.assertContains("canonical raw Bedrock particle schema/codec owner");
        modules.assertContains("io.github.tt432.eyelibimporter.particle.BrParticle");
        modules.assertContains("canonical module runtime definition owner");
        modules.assertContains("io.github.tt432.eyelibparticle.runtime.ParticleDefinition");
        modules.assertContains("root `client/particle/bedrock/BrParticle`");
        modules.assertContains("transitional compatibility adapters only");

        moduleBoundaries.assertContains("canonical raw Bedrock particle schema/codec owner");
        moduleBoundaries.assertContains("canonical module runtime definition owner");
        moduleBoundaries.assertContains("ParticleDefinitionAdapter");
        moduleBoundaries.assertContains("client/particle/bedrock/BrParticle");
        moduleBoundaries.assertContains("legacy/non-canonical");

        sideBoundaries.assertContains("allowed particle -> importer dependency for ParticleDefinitionAdapter");
        sideBoundaries.assertContains("legacy/non-canonical runtime adapter target");

        particleReadme.assertContains("canonical raw Bedrock particle schema/codec owner");
        particleReadme.assertContains("canonical module runtime definition owner");
        particleReadme.assertContains("allowed particle -> importer dependency for ParticleDefinitionAdapter");

        runtimePackageInfo.assertContains("legacy/non-canonical runtime adapter target");
        runtimePackageInfo.assertContains("allowed particle -> importer dependency for ParticleDefinitionAdapter");

        rootParticleReadme.assertContains("canonical raw Bedrock particle schema/codec owner");
        rootParticleReadme.assertContains("canonical module runtime definition owner");
        rootParticleReadme.assertContains("legacy/non-canonical runtime adapter target");

        assertMappedFieldsDocumented(moduleBoundaries);
        assertMappedFieldsDocumented(sideBoundaries);
        assertMappedFieldsDocumented(particleReadme);
        assertMappedFieldsDocumented(runtimePackageInfo);
        assertMappedFieldsDocumented(rootParticleReadme);

        assertPhaseDeferralsDocumented(modules);
        assertPhaseDeferralsDocumented(particleReadme);
        assertPhaseDeferralsDocumented(rootParticleReadme);
    }

    private static void assertMappedFieldsDocumented(SourceCheck source) {
        source.assertContains("mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation");
    }

    private static void assertPhaseDeferralsDocumented(SourceCheck source) {
        source.assertContains("Phase 12");
        source.assertContains("Phase 13 rewires command/network integration");
        source.assertContains("Phase 14 owns");
    }

    private static SourceCheck source(String path) throws IOException {
        Path root = projectRoot();
        Path resolved = root.resolve(path);
        return new SourceCheck(path, Files.readString(resolved));
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

    private record SourceCheck(String path, String content) {
        void assertContains(String expected) {
            assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
        }
    }
}
