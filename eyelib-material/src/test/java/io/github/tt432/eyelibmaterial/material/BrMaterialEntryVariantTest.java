package io.github.tt432.eyelibmaterial.material;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for variant lookup methods on {@link BrMaterialEntry}.
 */
class BrMaterialEntryVariantTest {

    private static BrMaterialEntry createMinimalEntry(String name) {
        return new BrMaterialEntry(
                "", name,
                Optional.empty(), Optional.empty(),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.empty(), Optional.empty(), Optional.empty()),
                Optional.empty(),
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                List.of()
        );
    }

    @Test
    @DisplayName("getVariant finds a variant by name")
    void getVariantFound() {
        var skinningEntry = createMinimalEntry("skinning");
        var variantMap = Map.of("skinning", skinningEntry);
        var parent = createMinimalEntry("parent", List.of(variantMap));

        var result = parent.getVariant("skinning");
        assertTrue(result.isPresent(), "Variant 'skinning' should be found");
        assertSame(skinningEntry, result.get(), "Should return the exact variant entry");
    }

    @Test
    @DisplayName("getVariant returns empty for nonexistent variant name")
    void getVariantNotFound() {
        var parent = createMinimalEntry("parent");
        var result = parent.getVariant("nonexistent");
        assertTrue(result.isEmpty(), "Nonexistent variant should not be found");
    }

    @Test
    @DisplayName("hasVariants returns true when variants list is non-empty")
    void hasVariantsTrue() {
        var skinningEntry = createMinimalEntry("skinning");
        var variantMap = Map.of("skinning", skinningEntry);
        var parent = createMinimalEntry("parent", List.of(variantMap));

        assertTrue(parent.hasVariants(), "Material with variants should return true");
    }

    @Test
    @DisplayName("hasVariants returns false when variants list is empty")
    void hasVariantsFalse() {
        var parent = createMinimalEntry("parent");
        assertFalse(parent.hasVariants(), "Material without variants should return false");
    }

    @Test
    @DisplayName("getVariant finds variant among multiple variant maps")
    void getVariantMultipleMaps() {
        var diffuseEntry = createMinimalEntry("diffuse");
        var skinningEntry = createMinimalEntry("skinning");
        var map1 = Map.of("diffuse", diffuseEntry);
        var map2 = Map.of("skinning", skinningEntry);
        var parent = createMinimalEntry("parent", List.of(map1, map2));

        var found = parent.getVariant("diffuse");
        assertTrue(found.isPresent(), "Variant 'diffuse' should be found");
        assertSame(diffuseEntry, found.get());

        found = parent.getVariant("skinning");
        assertTrue(found.isPresent(), "Variant 'skinning' should be found");
        assertSame(skinningEntry, found.get());
    }

    private static BrMaterialEntry createMinimalEntry(String name, List<Map<String, BrMaterialEntry>> variants) {
        return new BrMaterialEntry(
                "", name,
                Optional.empty(), Optional.empty(),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.empty(), Optional.empty(), Optional.empty()),
                Optional.empty(),
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                variants
        );
    }
}
