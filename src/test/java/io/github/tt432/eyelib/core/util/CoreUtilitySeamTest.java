package io.github.tt432.eyelib.core.util;

import com.mojang.datafixers.util.Either;
import io.github.tt432.eyelib.core.util.codec.Eithers;
import io.github.tt432.eyelib.core.util.collection.ListAccessors;
import io.github.tt432.eyelib.core.util.color.ColorEncodings;
import io.github.tt432.eyelib.core.util.texture.TexturePaths;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreUtilitySeamTest {
    @Test
    void emissiveTexturePathSuffixIsDeterministic() {
        assertEquals("foo/bar.emissive.png", TexturePaths.emissivePath("foo/bar.png"));
        assertEquals("foo/bar", TexturePaths.emissivePath("foo/bar"));
    }

    @Test
    void argbToAbgrReordersChannelsWithoutMinecraftRuntime() {
        assertEquals(0x11443322, ColorEncodings.argbToAbgr(0x11223344));
        assertEquals(0xFFCCBBAA, ColorEncodings.argbToAbgr(0xFFAABBCC));
    }

    @Test
    void listAccessorsMirrorLegacyListHelperBehavior() {
        List<String> values = List.of("first", "middle", "last");
        assertEquals("first", ListAccessors.first(values));
        assertEquals("last", ListAccessors.last(values));
    }

    @Test
    void unwrapEitherWorksForBothSides() {
        assertEquals("left", Eithers.unwrap(Either.left("left")));
        assertEquals("right", Eithers.unwrap(Either.right("right")));
    }
}
