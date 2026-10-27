package io.github.tt432.eyelib.importer;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.animation.bedrock.BrBoneKeyFrame;
import io.github.tt432.eyelib.animation.bedrock.baked.BakedBoneKeyFrame;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelib.importer.particle.BrParticle;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bedrock 序列化名必须与 JVM 默认 Locale 无关：tr_TR 下 String#toLowerCase() 把 I 映射成 ı，
 * "LINEAR" 会写成 "lınear"，属性/曲线文件往返断裂。
 */
class BedrockSerializedNameLocaleTest {
    @Test
    void lerpModeSerializedNamesStayAsciiUnderTurkishLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertEquals("linear", BrBoneKeyFrame.LerpMode.LINEAR.getSerializedName());
            assertEquals("linear", TestCodecUtil.unwrap(BrBoneKeyFrameSchema.LerpMode.CODEC
                    .encodeStart(JsonOps.INSTANCE, BrBoneKeyFrameSchema.LerpMode.LINEAR)).getAsString());
            assertEquals(BrBoneKeyFrameSchema.LerpMode.LINEAR, TestCodecUtil.unwrap(BrBoneKeyFrameSchema.LerpMode.CODEC
                    .parse(JsonOps.INSTANCE, new JsonPrimitive("linear"))));
            assertEquals(BakedBoneKeyFrame.BakedLerpMode.CATMULLROM,
                    BakedBoneKeyFrame.BakedLerpMode.fromSchemaName("catmullrom"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void particleCurveSerializedNamesStayAsciiUnderTurkishLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertEquals("linear", BrParticle.Curve.Type.LINEAR.getSerializedName());
            assertEquals(BrParticle.Curve.Type.LINEAR, TestCodecUtil.unwrap(BrParticle.Curve.Type.CODEC
                    .parse(JsonOps.INSTANCE, new JsonPrimitive("linear"))));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
