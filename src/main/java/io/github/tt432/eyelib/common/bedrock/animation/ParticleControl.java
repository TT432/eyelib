package io.github.tt432.eyelib.common.bedrock.animation;

import io.github.tt432.eyelib.common.bedrock.BedrockResourceManager;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.ParticleEffect;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Timestamp;
import io.github.tt432.eyelib.common.bedrock.particle.BedrockParticleManager;
import io.github.tt432.eyelib.common.bedrock.particle.ParticleEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * @author DustW
 */
public class ParticleControl {
    private final Queue<Map.Entry<Timestamp, ParticleEffect>> particleQueue = new LinkedList<>();

    public void init(SingleAnimation animation) {
        if (animation != null) {
            var particleEffects = animation.getParticleEffects();

            if (particleEffects != null) {
                particleQueue.addAll(particleEffects.entrySet());
            }
        }
    }

    public void stop() {
        particleQueue.clear();
    }

    public void process(Entity entity, double tick) {
        var curr = particleQueue.peek();

        if (curr != null && tick >= curr.getKey().getTick()) {
            ParticleEffect particleEffect = curr.getValue();

            ParticleEmitter emitter = ParticleEmitter.from(
                    BedrockResourceManager.getInstance().getParticle(new ResourceLocation(particleEffect.getId())),
                    Minecraft.getInstance().level, entity.position()
            );

            emitter.setLocator(particleEffect.getLocator());
            emitter.setBindingEntity(entity);

            BedrockParticleManager.addParticle(emitter);

            particleQueue.poll();
        }
    }
}
