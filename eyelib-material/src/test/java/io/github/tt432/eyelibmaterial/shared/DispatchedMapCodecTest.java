package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import io.github.tt432.eyelibutil.codec.DispatchedMapCodec;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
class DispatchedMapCodecTest {

    @Test
    @DisplayName("DispatchedMapCodec roundtrip with string entries")
    void roundtripStringMap() {
        var codec = new DispatchedMapCodec<String, String>(
                PrimitiveCodec.STRING,
                key -> PrimitiveCodec.STRING
        );

        Map<String, String> input = Map.of("a", "hello", "b", "world");

        var encoded = codec.encodeStart(JsonOps.INSTANCE, input)
                .getOrThrow(false, msg -> new AssertionError("Encode failed: " + msg));
        var decoded = codec.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, msg -> new AssertionError("Decode failed: " + msg));

        assertEquals(2, decoded.size(), "Map must have 2 entries");
        assertEquals("hello", decoded.get("a"), "Value for key 'a' should be 'hello'");
        assertEquals("world", decoded.get("b"), "Value for key 'b' should be 'world'");
    }
}