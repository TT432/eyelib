package io.github.tt432.eyelib.wintersky.three;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * three.js r134 Texture 移植（构造默认字段 + needsUpdate 语义）。
 */
public class Texture extends EventDispatcher {

    private static int textureId = 0;
    public static final @Nullable TextureImage DEFAULT_IMAGE = null;
    public static final int DEFAULT_MAPPING = Constants.UVMapping;

    public final int id = textureId++;

    public String name = "";

    public @Nullable TextureImage image;
    public final List<Object> mipmaps = new ArrayList<>();

    public int mapping;

    public int wrapS;
    public int wrapT;

    public int magFilter;
    public int minFilter;

    public int anisotropy;

    public int format;
    public String internalFormat = null;
    public int type;

    public final Vector2 offset = new Vector2(0, 0);
    public final Vector2 repeat = new Vector2(1, 1);
    public final Vector2 center = new Vector2(0, 0);
    public double rotation = 0;

    public boolean matrixAutoUpdate = true;

    public boolean generateMipmaps = true;
    public boolean premultiplyAlpha = false;
    public boolean flipY = true;
    public int unpackAlignment = 4;

    public int encoding;

    public final Map<String, Object> userData = new HashMap<>();

    public int version = 0;
    public @Nullable Runnable onUpdate = null;

    public Texture() {
        this(DEFAULT_IMAGE);
    }

    public Texture(@Nullable TextureImage image) {
        this(image, DEFAULT_MAPPING, Constants.ClampToEdgeWrapping, Constants.ClampToEdgeWrapping,
                Constants.LinearFilter, Constants.LinearMipmapLinearFilter,
                Constants.RGBAFormat, Constants.UnsignedByteType, 1, Constants.LinearEncoding);
    }

    public Texture(@Nullable TextureImage image, int mapping, int wrapS, int wrapT,
                   int magFilter, int minFilter, int format, int type, int anisotropy, int encoding) {
        this.image = image;
        this.mapping = mapping;
        this.wrapS = wrapS;
        this.wrapT = wrapT;
        this.magFilter = magFilter;
        this.minFilter = minFilter;
        this.anisotropy = anisotropy;
        this.format = format;
        this.type = type;
        this.encoding = encoding;
    }

    /** JS {@code set needsUpdate(value)}：value === true 时 version++。 */
    public void setNeedsUpdate(boolean value) {
        if (value) this.version++;
    }

    /** three dispose()：仅派发 dispose 事件。 */
    public void dispose() {
        this.dispatchEvent(new Event("dispose"));
    }
}
