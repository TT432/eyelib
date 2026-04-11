package io.github.tt432.eyelibimporter.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;

/**
 * Platform-free model visible bounds definition.
 */
public record VisibleBox(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
) {
    public static final VisibleBox EMPTY = new VisibleBox(0, 0, 0, 0, 0, 0);

    public static final Codec<VisibleBox> CODEC = Codec.DOUBLE.listOf().comapFlatMap(
            values -> values.size() == 6
                    ? DataResult.success(new VisibleBox(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5)))
                    : DataResult.error(() -> "expected 6 values for VisibleBox, got " + values.size()),
            visibleBox -> List.of(
                    visibleBox.minX,
                    visibleBox.minY,
                    visibleBox.minZ,
                    visibleBox.maxX,
                    visibleBox.maxY,
                    visibleBox.maxZ
            )
    );

    public static VisibleBox fromBlockbenchDimensions(List<Double> dimensions) {
        if (dimensions.size() < 3) {
            return EMPTY;
        }

        double width = dimensions.get(0);
        double height = dimensions.get(1);
        double depth = dimensions.get(2);
        return new VisibleBox(-width / 2, 0, -depth / 2, width / 2, height, depth / 2);
    }

    public static VisibleBox fromBedrockDescription(double visibleBoundsWidth, double visibleBoundsHeight, double visibleBoundsOffsetY) {
        if (visibleBoundsWidth <= 0 || visibleBoundsHeight <= 0) {
            return EMPTY;
        }

        double halfWidth = visibleBoundsWidth / 2D;
        double minY = visibleBoundsOffsetY - visibleBoundsHeight / 2D;
        double maxY = visibleBoundsOffsetY + visibleBoundsHeight / 2D;
        return new VisibleBox(-halfWidth, minY, -halfWidth, halfWidth, maxY, halfWidth);
    }
}
