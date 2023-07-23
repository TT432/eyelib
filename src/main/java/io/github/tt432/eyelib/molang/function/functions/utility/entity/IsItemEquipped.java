package io.github.tt432.eyelib.molang.function.functions.utility.entity;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.entity.LivingEntity;

import static io.github.tt432.eyelib.molang.MolangValue.FALSE;
import static io.github.tt432.eyelib.molang.MolangValue.TRUE;

/**
 * takes one optional hand slot as a parameter (0 or 'main_hand' for main hand, 1 or 'off_hand' for off hand),
 * and returns 1.0 if there is an item in the requested slot (defaulting to the main hand if no parameter is supplied),
 * otherwise returns 0.0.
 * <p>
 * 接收一个可选的手槽作为参数（0或'main_hand'表示主手，1或'off_hand'表示非主手），
 * 如果在请求的手槽中有一个项目，则返回1.0（如果没有提供参数则默认为主手），否则返回0.0。
 *
 * @author DustW
 */
@Slf4j
@MolangFunctionHolder("query.is_item_equipped")
public class IsItemEquipped extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        String arg = params.svalue(0);

        if (!(params.scope().getOwner().instance() instanceof LivingEntity living))
            return FALSE;

        if (arg.equals("main_hand") || arg.equals("0")) {
            return living.getMainHandItem().isEmpty() ? FALSE : TRUE;
        } else if (arg.equals("off_hand") || arg.equals("1")) {
            return living.getOffhandItem().isEmpty() ? FALSE : TRUE;
        }

        log.error("query.is_item_equipped arg error : " + arg);

        return FALSE;
    }
}
