package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@MolangMapping(value = "eyelib", pureFunction = false)
public class MolangEyelib {
    private static Map<String, Boolean> getGlowing(MolangScope scope) {
        return scope.getOrCreateExtraData("glowing", new HashMap<>());
    }

    public static float add_glow(MolangScope scope, Object... params) {
        Map<String, Boolean> glowing = getGlowing(scope);

        if (params.length > 0) {
            for (Object param : params) {
                glowing.put(param.toString(), true);
            }
        } else {
            glowing.put(scope.getExtraData("anim.current_bone", ""), true);
        }

        return 0;
    }

    public static float remove_glow(MolangScope scope, Object... params) {
        Map<String, Boolean> glowing = getGlowing(scope);

        if (params.length > 0) {
            for (Object param : params) {
                glowing.put(param.toString(), false);
            }
        } else {
            glowing.put(scope.getExtraData("anim.current_bone", ""), false);
        }

        return 0;
    }
}
