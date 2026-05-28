package io.github.tt432.eyelibmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImporterCodecs {
    public static final Codec<Vector3f> VECTOR3F = Codec.FLOAT.listOf().comapFlatMap(
            values -> values.size() == 3
                    ? DataResult.success(new Vector3f(values.get(0), values.get(1), values.get(2)))
                    : DataResult.error(() -> "expected 3 values, got " + values.size()),
            vector -> List.of(vector.x(), vector.y(), vector.z())
    );

    public static final Codec<Vector3fc> VECTOR3FC = Codec.FLOAT.listOf().comapFlatMap(
            values -> values.size() == 3
                    ? DataResult.success(new Vector3f(values.get(0), values.get(1), values.get(2)))
                    : DataResult.error(() -> "expected 3 values, got " + values.size()),
            vector -> List.of(vector.x(), vector.y(), vector.z())
    );

    public static final Codec<Vector2fc> VECTOR2FC = Codec.FLOAT.listOf().comapFlatMap(
            values -> values.size() == 2
                    ? DataResult.success(new Vector2f(values.get(0), values.get(1)))
                    : DataResult.error(() -> "expected 2 values, got " + values.size()),
            vector -> List.of(vector.x(), vector.y())
    );
}