package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import net.minecraft.world.InteractionHand;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.main_hand_is")
public class MainHandIs extends HandIs {
    public MainHandIs() throws IllegalArgumentException {
        super(InteractionHand.MAIN_HAND);
    }
}
