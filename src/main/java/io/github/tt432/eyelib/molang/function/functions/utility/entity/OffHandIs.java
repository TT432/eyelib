package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import net.minecraft.world.InteractionHand;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.off_hand_is")
public class OffHandIs extends HandIs {
    public OffHandIs() throws IllegalArgumentException {
        super(InteractionHand.OFF_HAND);
    }
}
