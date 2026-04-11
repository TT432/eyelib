package io.github.tt432.eyelibimporter.model.importer;

import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public final class ImportedImageData {
    private final int width;
    private final int height;
    private final int[] argbPixels;

    public ImportedImageData(int width, int height, int[] argbPixels) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Image dimensions must be non-negative");
        }
        if (argbPixels.length != width * height) {
            throw new IllegalArgumentException("Pixel array length does not match image dimensions");
        }
        this.width = width;
        this.height = height;
        this.argbPixels = argbPixels;
    }

    public static ImportedImageData empty(int width, int height) {
        return new ImportedImageData(width, height, new int[width * height]);
    }

    @Nullable
    public static ImportedImageData decodePng(@Nullable byte[] pngBytes) throws IOException {
        if (pngBytes == null) {
            return null;
        }

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (bufferedImage == null) {
            throw new IOException("Invalid PNG data");
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] pixels = new int[width * height];
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
        return new ImportedImageData(width, height, pixels);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int getPixelArgb(int x, int y) {
        return argbPixels[index(x, y)];
    }

    public void setPixelArgb(int x, int y, int argb) {
        argbPixels[index(x, y)] = argb;
    }

    public void blitTo(ImportedImageData target, int destX, int destY) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                target.setPixelArgb(destX + x, destY + y, getPixelArgb(x, y));
            }
        }
    }

    public int[] copyPixels() {
        return Arrays.copyOf(argbPixels, argbPixels.length);
    }

    private int index(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Pixel coordinate out of bounds: (" + x + ", " + y + ")");
        }
        return y * width + x;
    }
}
