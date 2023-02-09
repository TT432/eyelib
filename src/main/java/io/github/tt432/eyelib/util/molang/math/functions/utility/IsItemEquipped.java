package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.molang.MolangParser;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;
import lombok.extern.slf4j.Slf4j;

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
    public IsItemEquipped(MolangValue[] values, String name) throws IllegalArgumentException {
        super(values, name, 1);
    }

    @Override
    public double get() {
        String arg = getArgAsString(0);

        if (arg.equals("main_hand") || arg.equals("0")) {
            return MolangParser.getInstance().getVariable("query.is_item_equipped_mh").get();
        } else if (arg.equals("off_hand") || arg.equals("1")) {
            return MolangParser.getInstance().getVariable("query.is_item_equipped_fh").get();
        } else {
            log.error("query.is_item_equipped arg error : " + arg);
            return 0;
        }
    }
}
