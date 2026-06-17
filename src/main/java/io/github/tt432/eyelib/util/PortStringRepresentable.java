package io.github.tt432.eyelib.util;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 替代 Minecraft StringRepresentable 的枚举序列化接口。
 *
 * @author TT432
 */
public interface PortStringRepresentable {

    String getSerializedName();

    /**
     * 为实现了此接口的枚举创建一个基于名称的 Codec。
     */
    static <T extends Enum<T> & PortStringRepresentable> Codec<T> fromEnum(Supplier<T[]> values) {
        return Codec.STRING.xmap(
                name -> Arrays.stream(values.get())
                        .filter(e -> e.getSerializedName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown enum name: " + name)),
                PortStringRepresentable::getSerializedName
        );
    }
}
