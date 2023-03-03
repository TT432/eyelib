package io.github.tt432.eyelib.common.bedrock;

import com.google.gson.annotations.SerializedName;

public enum FormatVersion {
    /**
     * 模型
     */
    @SerializedName("1.12.0")
    VERSION_1_12_0,
    @SerializedName("1.14.0")
    VERSION_1_14_0,
    /**
     * 动画
     */
    @SerializedName("1.8.0")
    VERSION_1_8_0,
    /**
     * 粒子
     */
    @SerializedName("1.10.0")
    VERSION_1_10_0
}
