package io.github.tt432.eyelib.molang.functions.utility.entity;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import net.minecraft.world.InteractionHand;

/**
 * @author DustW
 */
@MolangFunctionHolder("query.off_hand_is")
public class OffHandIs extends HandIs {
    public OffHandIs(MolangValue[] values, String name) throws IllegalArgumentException {
        super(InteractionHand.OFF_HAND, values, name);
    }
}
