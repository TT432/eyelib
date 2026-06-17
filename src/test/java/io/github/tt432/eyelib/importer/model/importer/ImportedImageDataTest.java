package io.github.tt432.eyelibimporter.model.importer;

import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/** @author TT432 */
class ImportedImageDataTest {

    @Test
    void decodeUncompressed24bppBottomToTop() throws Exception {
        int w = 2, h = 2;
        // 手工构造两个像素: 红(FF0000) 和 绿(00FF00)
        // TGA 格式是 BGR 字节序
        byte[] red = {0, 0, (byte) 0xFF};   // B=0, G=0, R=255
        byte[] green = {0, (byte) 0xFF, 0};  // B=0, G=255, R=0
        byte[] blue = {(byte) 0xFF, 0, 0};   // B=255, G=0, R=0
        byte[] white = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        byte[] tga = buildUncompressedTga(w, h, 24, false,
                red, green, blue, white);

        ImportedImageData img = ImportedImageData.decodeTga(tga);
        assertNotNull(img);
        assertEquals(w, img.width());
        assertEquals(h, img.height());
        // 源数据 bottom-to-top，解码后应翻转为 top-to-bottom
        // 原始: 第0行(r,g), 第1行(b,w) → 翻转后: 第0行(b,w), 第1行(r,g)
        assertEquals(0xFFFF0000, img.getPixelArgb(0, 1)); // red at (0,1)
        assertEquals(0xFF00FF00, img.getPixelArgb(1, 1)); // green at (1,1)
        assertEquals(0xFF0000FF, img.getPixelArgb(0, 0)); // blue at (0,0)
        assertEquals(0xFFFFFFFF, img.getPixelArgb(1, 0)); // white at (1,0)
    }

    @Test
    void decodeUncompressed32bppWithAlpha() throws Exception {
        int w = 1, h = 2;
        // 半透明红 ARGB=0x80FF0000 → BGRA=0,0,0xFF,0x80
        byte[] semiRed = {0, 0, (byte) 0xFF, (byte) 0x80};
        // 全不透明蓝 ARGB=0xFF0000FF → BGRA=0xFF,0,0,0xFF
        byte[] opaqueBlue = {(byte) 0xFF, 0, 0, (byte) 0xFF};

        byte[] tga = buildUncompressedTga(w, h, 32, false,
                semiRed, opaqueBlue);

        ImportedImageData img = ImportedImageData.decodeTga(tga);
        assertNotNull(img);
        assertEquals(w, img.width());
        assertEquals(h, img.height());
        assertEquals(0x80FF0000, img.getPixelArgb(0, 1)); // semi-red
        assertEquals(0xFF0000FF, img.getPixelArgb(0, 0)); // opaque blue
    }

    @Test
    void decodeRle24bpp() throws Exception {
        int w = 3, h = 1;
        byte[] tga = buildRleTga(w, h, 24, false,
                new byte[]{0, 0, (byte) 0xFF},  // 1 raw red
                new byte[]{0, (byte) 0xFF, 0},  // 1 raw green
                new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}  // 1 raw white
        );

        ImportedImageData img = ImportedImageData.decodeTga(tga);
        assertNotNull(img);
        assertEquals(w, img.width());
        assertEquals(h, img.height());
        assertEquals(0xFFFF0000, img.getPixelArgb(0, 0));
        assertEquals(0xFF00FF00, img.getPixelArgb(1, 0));
        assertEquals(0xFFFFFFFF, img.getPixelArgb(2, 0));
    }

    @Test
    void decodeRleWithRun() throws Exception {
        int w = 4, h = 1;
        // 两个 raw 像素 + 一个 RLE run(length=2)
        byte[] tga = buildMixedRleTga(w, h, 24,
                new byte[]{0, 0, (byte) 0xFF},           // 1 raw red
                new byte[]{0, 0, (byte) 0xFF},           // 1 raw red
                2, new byte[]{0, (byte) 0xFF, 0}         // RLE run of 2 green
        );

        ImportedImageData img = ImportedImageData.decodeTga(tga);
        assertNotNull(img);
        assertEquals(0xFFFF0000, img.getPixelArgb(0, 0)); // red
        assertEquals(0xFFFF0000, img.getPixelArgb(1, 0)); // red
        assertEquals(0xFF00FF00, img.getPixelArgb(2, 0)); // green
        assertEquals(0xFF00FF00, img.getPixelArgb(3, 0)); // green
    }

    @Test
    void decodeTopToBottomNoFlip() throws Exception {
        int w = 1, h = 2;
        byte[] top = {(byte) 0xFF, 0, 0};    // B=255 (blue)
        byte[] bottom = {0, (byte) 0xFF, 0};  // G=255 (green)

        byte[] tga = buildUncompressedTga(w, h, 24, true,
                top, bottom);

        ImportedImageData img = ImportedImageData.decodeTga(tga);
        assertNotNull(img);
        assertEquals(0xFF0000FF, img.getPixelArgb(0, 0)); // blue stays at row 0
        assertEquals(0xFF00FF00, img.getPixelArgb(0, 1)); // green stays at row 1
    }

    @Test
    void nullInputReturnsNull() throws Exception {
        assertNull(ImportedImageData.decodeTga(null));
    }

    @Test
    void tooShortThrows() {
        assertThrows(IOException.class, () -> ImportedImageData.decodeTga(new byte[10]));
    }

    @Test
    void unsupportedTypeThrows() {
        // type=1 (color-mapped) — unsupported
        byte[] buf = new byte[18];
        buf[2] = 1;
        assertThrows(IOException.class, () -> ImportedImageData.decodeTga(buf));
    }

    // helpers

    private static byte[] buildUncompressedTga(int w, int h, int bpp, boolean topToBottom,
                                                byte[]... pixels) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(0);  // idLength
        bos.write(0);  // colorMapType
        bos.write(2);  // imageType = uncompressed true-color
        bos.write(new byte[]{0, 0});  // colorMapOrigin
        bos.write(new byte[]{0, 0});  // colorMapLength
        bos.write(0);  // colorMapDepth
        bos.write(new byte[]{0, 0});  // xOrigin
        bos.write(new byte[]{0, 0});  // yOrigin
        bos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) w).array());
        bos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) h).array());
        bos.write(bpp);
        int desc = topToBottom ? 0x20 : 0;
        if (bpp == 32) desc |= 8; // alpha bits
        bos.write(desc);
        for (byte[] pixel : pixels) {
            bos.write(pixel);
        }
        return bos.toByteArray();
    }

    private static byte[] buildRleTga(int w, int h, int bpp, boolean topToBottom,
                                       byte[]... pixels) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(0);
        bos.write(0);
        bos.write(10); // imageType = RLE true-color
        bos.write(new byte[]{0, 0, 0, 0, 0});
        bos.write(new byte[]{0, 0, 0, 0});
        bos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) w).array());
        bos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) h).array());
        bos.write(bpp);
        int desc = topToBottom ? 0x20 : 0;
        if (bpp == 32) desc |= 8;
        bos.write(desc);
        for (byte[] pixel : pixels) {
            bos.write(0); // raw packet header: count=1, rle=0
            bos.write(pixel);
        }
        return bos.toByteArray();
    }

    private static byte[] buildMixedRleTga(int w, int h, int bpp,
                                            Object... segments) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(0);
        bos.write(0);
        bos.write(10);
        bos.write(new byte[]{0, 0, 0, 0, 0});
        bos.write(new byte[]{0, 0, 0, 0});
        bos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) w).array());
        bos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) h).array());
        bos.write(bpp);
        bos.write(0x20); // topToBottom
        for (int i = 0; i < segments.length; i++) {
            Object seg = segments[i];
            if (seg instanceof byte[] rawPixels) {
                bos.write(0); // raw packet, count=1
                bos.write(rawPixels);
            } else if (seg instanceof Integer count) {
                i++;
                byte[] rlePixel = (byte[]) segments[i];
                bos.write(0x80 | (count - 1)); // RLE packet header
                bos.write(rlePixel);
            }
        }
        return bos.toByteArray();
    }
}
