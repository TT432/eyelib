package io.github.tt432.eyelib.importer.model.importer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** 导入的图片数据，含宽高和 ARGB 像素数组。
 * @author TT432 */
@NullMarked
public final class ImportedImageData {

    public static final Codec<ImportedImageData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("width").forGetter(ImportedImageData::width),
            Codec.INT.fieldOf("height").forGetter(ImportedImageData::height),
            Codec.INT_STREAM.xmap(
                    intStream -> intStream.toArray(),
                    ints -> Arrays.stream(ints)
            ).fieldOf("argb_pixels").forGetter(ImportedImageData::copyPixels)
    ).apply(ins, ImportedImageData::new));
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

    @Nullable
    public static ImportedImageData decodeTga(@Nullable byte[] tgaBytes) throws IOException {
        if (tgaBytes == null) {
            return null;
        }
        if (tgaBytes.length < 18) {
            throw new IOException("TGA file too short");
        }

        if (hasPngSignature(tgaBytes)) {
            return decodePng(tgaBytes);
        }

        ByteBuffer buf = ByteBuffer.wrap(tgaBytes).order(ByteOrder.LITTLE_ENDIAN);

        int idLength = buf.get() & 0xFF;
        int colorMapType = buf.get() & 0xFF;
        int imageType = buf.get() & 0xFF;

        buf.position(buf.position() + 9);
        int width = buf.getShort() & 0xFFFF;
        int height = buf.getShort() & 0xFFFF;
        int bpp = buf.get() & 0xFF;
        int imageDescriptor = buf.get() & 0xFF;

        boolean bottomToTop = (imageDescriptor & 0x20) == 0;
        int pixelCount = width * height;
        if (width <= 0 || height <= 0 || width > 8192 || height > 8192 || pixelCount <= 0) {
            throw new IOException("TGA dimensions out of bounds: " + width + "x" + height);
        }

        int offset = 18 + idLength;
        if (colorMapType != 0) {
            buf.position(5);
            int colorMapLength = buf.getShort() & 0xFFFF;
            int colorMapEntrySize = buf.get() & 0xFF;
            offset += colorMapLength * (colorMapEntrySize / 8);
        }

        int bytesPerPixel = bpp / 8;
        int[] argbPixels = new int[pixelCount];
        int srcPos = offset;

        if (imageType == 2) {
            for (int i = 0; i < pixelCount; i++) {
                int b = tgaBytes[srcPos++] & 0xFF;
                int g = tgaBytes[srcPos++] & 0xFF;
                int r = tgaBytes[srcPos++] & 0xFF;
                int a = bytesPerPixel >= 4 ? (tgaBytes[srcPos++] & 0xFF) : 0xFF;
                argbPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        } else if (imageType == 10) {
            int i = 0;
            while (i < pixelCount && srcPos < tgaBytes.length) {
                int packetHeader = tgaBytes[srcPos++] & 0xFF;
                int count = (packetHeader & 0x7F) + 1;
                boolean rle = (packetHeader & 0x80) != 0;
                if (rle) {
                    int b = tgaBytes[srcPos++] & 0xFF;
                    int g = tgaBytes[srcPos++] & 0xFF;
                    int r = tgaBytes[srcPos++] & 0xFF;
                    int a = bytesPerPixel >= 4 ? (tgaBytes[srcPos++] & 0xFF) : 0xFF;
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    for (int j = 0; j < count && i < pixelCount; j++, i++) {
                        argbPixels[i] = argb;
                    }
                } else {
                    for (int j = 0; j < count && i < pixelCount; j++, i++) {
                        int b = tgaBytes[srcPos++] & 0xFF;
                        int g = tgaBytes[srcPos++] & 0xFF;
                        int r = tgaBytes[srcPos++] & 0xFF;
                        int a = bytesPerPixel >= 4 ? (tgaBytes[srcPos++] & 0xFF) : 0xFF;
                        argbPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            }
        } else {
            throw new IOException("Unsupported TGA image type: " + imageType);
        }

        if (bottomToTop) {
            int[] flipped = new int[pixelCount];
            for (int y = 0; y < height; y++) {
                int srcRow = (height - 1 - y) * width;
                int dstRow = y * width;
                System.arraycopy(argbPixels, srcRow, flipped, dstRow, width);
            }
            argbPixels = flipped;
        }

        return new ImportedImageData(width, height, argbPixels);
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

    private static boolean hasPngSignature(byte[] bytes) {
        return bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }
}