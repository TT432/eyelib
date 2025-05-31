package io.github.tt432.eyelib.common.behavior.event.filter;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * 该枚举类定义了交互中涉及的不同主体类型。
 *
 * @author TT432
 */
public enum Subject implements StringRepresentable {
    /**
     * 交互所涉及的数据块。
     */
    block,
    /**
     * 参与交互的破坏性参与者。
     */
    damager,
    /**
     * 交互的另一个成员，而不是调用方。
     */
    other,
    /**
     * 调用方的当前父级。
     */
    parent,
    /**
     * 参与交互的玩家。
     */
    player,
    /**
     * 调用测试的实体或对象。
     */
    self,
    /**
     * 调用方的当前目标。
     */
    target;

    /**
     * 提供一个编解码器，用于将 {@link Subject} 枚举与字符串进行相互转换。
     */
    public static final Codec<Subject> CODEC = StringRepresentable.fromEnum(Subject::values);

    /**
     * 获取该枚举常量的序列化名称，这里直接返回枚举常量的名称。
     *
     * @return 枚举常量的名称
     */
    @Override
    public String getSerializedName() {
        return name();
    }
}
