package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import net.minecraft.world.InteractionHand;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.main_hand_is")
public class MainHandIs extends HandIs {
    public MainHandIs(MolangValue[] values, String name) throws IllegalArgumentException {
        super(InteractionHand.MAIN_HAND, values, name);
    }
}
