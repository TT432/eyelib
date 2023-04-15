package io.github.tt432.eyelib.common.bedrock.renderer;

import io.github.tt432.eyelib.molang.MolangVariableScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeoRenderers {
    public static void setPartialTick(float partialTick, MolangVariableScope scope) {
        scope.setValue("query.partial_tick", partialTick);
    }

    static void renderPartialTick(MolangVariableScope scope) {
        scope.removeValue("query.partial_tick");
    }
}
