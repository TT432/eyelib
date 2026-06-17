package io.github.tt432.eyelibutil;

import org.jspecify.annotations.NullMarked;

/**
 * 替代 Minecraft ResourceLocation 的纯数据资源标识。
 *
 * @author TT432
 */
@NullMarked
public record PortResourceLocation(String namespace, String path) {

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
}
