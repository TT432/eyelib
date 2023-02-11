package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author DustW
 */
@NoArgsConstructor
public class MolangDataSource {
    @Getter
    Entity entity;
    @Getter
    LivingEntity living;
    @Getter
    Player player;
    @Getter
    ItemStack stack;
    @Getter
    Animatable animatable;
    int id;

    public AnimationData getData() {
        return animatable.getFactory().getOrCreateAnimationData(id);
    }

    public void addSource(Object o, int id) {
        this.id = id;

        if (o instanceof Entity e) {
            entity = e;

            if (o instanceof LivingEntity l) {
                living = l;

                if (o instanceof Player p) {
                    player = p;
                }
            }
        }

        if (o instanceof ItemStack i) {
            stack = i;
        }

        if (o instanceof Animatable a) {
            animatable = a;
        }
    }

    public void clear() {
        entity = null;
        living = null;
        player = null;
        stack = null;
        animatable = null;
    }
}
