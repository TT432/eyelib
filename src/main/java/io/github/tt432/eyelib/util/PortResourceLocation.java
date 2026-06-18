package io.github.tt432.eyelib.util;

import com.mojang.serialization.Codec;

/**
 * 替代 Minecraft ResourceLocation 的纯数据资源标识。
 *
 * @author TT432
 */
public record PortResourceLocation(String namespace, String path) implements Comparable<PortResourceLocation> {

    public static final Codec<PortResourceLocation> CODEC = Codec.STRING.xmap(
            PortResourceLocation::parse, PortResourceLocation::toString);

    public static PortResourceLocation of(String namespace, String path) {
        return new PortResourceLocation(namespace, path);
    }

    /** 解析 "namespace:path" 格式的字符串 */
    public static PortResourceLocation parse(String location) {
        int i = location.indexOf(':');
        if (i >= 0) {
            return new PortResourceLocation(location.substring(0, i), location.substring(i + 1));
        }
        return new PortResourceLocation("minecraft", location);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public int compareTo(PortResourceLocation o) {
        int i = namespace.compareTo(o.namespace);
        return i != 0 ? i : path.compareTo(o.path);
    }
}
