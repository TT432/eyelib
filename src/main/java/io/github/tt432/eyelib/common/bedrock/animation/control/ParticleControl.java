package io.github.tt432.eyelib.common.bedrock.animation.control;

import io.github.tt432.eyelib.common.bedrock.BedrockResourceManager;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.ParticleEffect;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Timestamp;
import io.github.tt432.eyelib.common.bedrock.particle.BedrockParticleManager;
import io.github.tt432.eyelib.common.bedrock.particle.ParticleEmitter;
import io.github.tt432.eyelib.common.bedrock.particle.pojo.ParticleFile;
import io.github.tt432.eyelib.molang.MolangParser;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * @author DustW
 */
@Slf4j
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

    public void stop(double tick) {
        process(tick);
        particleQueue.clear();
    }

    public void process(double tick) {
        Entity entity = MolangParser.getCurrentDataSource().get(Entity.class);

        if (entity == null)
            return;

        var curr = particleQueue.peek();

        if (curr != null && tick >= curr.getKey().getTick()) {
            ParticleEffect particleEffect = curr.getValue();

            ParticleFile particleFile = BedrockResourceManager.getInstance().getParticle(new ResourceLocation(particleEffect.getId()));

            if (particleFile == null) {
                log.error("can't found particle : " + particleEffect.getId());
            } else {
                ParticleEmitter emitter = ParticleEmitter.from(particleFile, Minecraft.getInstance().level, entity.position());

                emitter.setLocator(particleEffect.getLocator());
                emitter.setBindingEntity(entity);

                BedrockParticleManager.addParticle(emitter);
            }

            particleQueue.poll();
        }
    }
}
