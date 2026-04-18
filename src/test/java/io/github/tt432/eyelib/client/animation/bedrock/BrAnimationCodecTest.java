package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationClipDefinition;
import io.github.tt432.eyelib.client.animation.NamedTrackContainerDefinition;
import io.github.tt432.eyelib.client.animation.TrackAnimationDefinition;
import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrAnimationCodecTest {
    @Test
    void parsesRuntimeAnimationFromImporterOwnedSchemaCodec() {
        String json = """
                {
                  "animations": {
                    "animation.test.idle": {
                      "loop": "true",
                      "animation_length": 1.5,
                      "timeline": {
                        "0.0": ["query.life_time"]
                      },
                      "bones": {
                        "body": {
                          "rotation": {
                            "0.0": [0.0, 0.0, 0.0]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        BrAnimation animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        BrAnimationEntry entry = animation.animations().get("animation.test.idle");
        assertNotNull(entry);
        assertEquals("animation.test.idle", entry.name());
        assertEquals(1.5F, entry.animationLength());
        assertEquals(BrLoopType.LOOP, entry.loop());
        assertEquals(1, entry.timeline().data().size());
        assertTrue(!entry.bones().isEmpty());
        BrBoneAnimation boneAnimation = entry.bones().values().iterator().next();
        assertTrue(boneAnimation.channels().containsKey(BrBoneAnimation.ROTATION));
        assertNotNull(boneAnimation.rotation().floorEntry(0F));

        TrackAnimationDefinition<Integer, BrBoneAnimation> definition = entry.definition();
        Integer firstBoneId = entry.bones().keySet().intStream().findFirst().orElseThrow();
        assertEquals(entry.bones().get(firstBoneId), definition.track(firstBoneId));

        AnimationClipDefinition<Integer, BrBoneAnimation, BrLoopType, io.github.tt432.eyelibmolang.MolangValue> clipDefinition = entry.definition();
        assertEquals("animation.test.idle", clipDefinition.name());
        assertEquals(BrLoopType.LOOP, clipDefinition.loop());
        assertEquals(1.5F, clipDefinition.animationLength());

        assertEquals(4, entry.definition().namedTracks().byName().size());
        assertEquals(entry.soundEffects(), ((BrAnimationEntryEffectTrackDefinition<?>) entry.definition().namedTracks().byName().get(BrAnimationEntryTrackName.SOUND_EFFECTS)).effect());
        assertEquals(entry.particleEffects(), ((BrAnimationEntryEffectTrackDefinition<?>) entry.definition().namedTracks().byName().get(BrAnimationEntryTrackName.PARTICLE_EFFECTS)).effect());
        assertEquals(entry.timeline(), ((BrAnimationEntryEffectTrackDefinition<?>) entry.definition().namedTracks().byName().get(BrAnimationEntryTrackName.TIMELINE)).effect());
        assertEquals(entry.bones(), ((BrAnimationEntryBoneTrackDefinition) entry.definition().namedTracks().byName().get(BrAnimationEntryTrackName.BONES)).bones());

        NamedTrackContainerDefinition<BrAnimationEntryTrackName, BrAnimationEntryTrackDefinition> namedTracks = entry.definition().namedTracks();
        assertEquals(entry.definition().namedTracks().tracksByName(), entry.definition().namedTracks().byName());
        assertEquals(entry.bones(), ((BrAnimationEntryBoneTrackDefinition) namedTracks.trackOrNull(BrAnimationEntryTrackName.BONES)).bones());
    }
}
