package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.particle.ParticleEmitter;
import io.github.tt432.eyelib.common.bedrock.particle.ParticleInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @author DustW
 */
public class MolangDataSource {
    private final Map<Class<?>, Object> byClass = new HashMap<>();
    int id;

    public MolangDataSource() {
        byClass.put(Entity.class, null);
        byClass.put(LivingEntity.class, null);
        byClass.put(Player.class, null);
        byClass.put(ItemStack.class, null);
        byClass.put(Animatable.class, null);
        byClass.put(ParticleEmitter.class, null);
        byClass.put(ParticleInstance.class, null);
    }

    @SuppressWarnings("all")
    public <T> T get(Class<T> clazz) {
        return (T) byClass.get(clazz);
    }

    public AnimationData getData() {
        return get(Animatable.class).getFactory().getOrCreateAnimationData(id);
    }

    public void addSource(Object o, int id) {
        this.id = id;

        byClass.entrySet().forEach(e -> {
            if (e.getKey().isInstance(o))
                e.setValue(o);
        });
    }

    public void addSource(Object o) {
        byClass.entrySet().forEach(e -> {
            if (e.getKey().isInstance(o))
                e.setValue(o);
        });
    }

    public void clear() {
        byClass.entrySet().forEach(e -> e.setValue(null));
    }
}
