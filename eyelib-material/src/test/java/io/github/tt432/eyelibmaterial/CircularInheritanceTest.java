package io.github.tt432.eyelibmaterial;

import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
class CircularInheritanceTest {

    @Test
    @DisplayName("A→B→A circular inheritance throws IllegalStateException")
    void circularInheritanceThrows() {
        var a = createEntry("A", "B");
        var b = createEntry("B", "A");
        var materials = Map.of("A", a, "B", b);

        assertThrows(IllegalStateException.class,
                () -> a.defines().toList(a, materials),
                "A→B→A cycle should be detected");
    }

    @Test
    @DisplayName("No base material — no exception")
    void noBaseSucceeds() {
        var c = createEntry("C", "");
        var materials = Map.of("C", c);

        assertDoesNotThrow(() -> c.defines().toList(c, materials));
    }

    @Test
    @DisplayName("Normal 3-level chain E→F→G succeeds without exception")
    void normalChainSucceeds() {
        var e = createEntry("E", "F");
        var f = createEntry("F", "G");
        var g = createEntry("G", "");
        var materials = Map.of("E", e, "F", f, "G", g);

        assertDoesNotThrow(() -> e.defines().toList(e, materials));
    }

    @Test
    @DisplayName("Self-referencing material H→H throws")
    void selfReferenceThrows() {
        var h = createEntry("H", "H");
        var materials = Map.of("H", h);

        assertThrows(IllegalStateException.class,
                () -> h.defines().toList(h, materials),
                "Self-reference H→H should be detected");
    }

    private static BrMaterialEntry createEntry(String name, String base) {
        return new BrMaterialEntry(
                base,
                name,
                Optional.empty(),
                Optional.empty(),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.empty(), Optional.empty(), Optional.empty()),
                Optional.empty(),
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of()
        );
    }
}