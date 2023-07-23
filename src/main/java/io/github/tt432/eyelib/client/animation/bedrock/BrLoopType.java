package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * @author TT432
 */
public enum BrLoopType {
    HOLD_ON_LAST_FRAME,
    LOOP,
    ONCE;
    
    public static BrLoopType parse(JsonElement json) {
        if (json instanceof JsonPrimitive jp) {
            if (jp.isString() && jp.getAsString().equals("hold_on_last_frame")){
                return BrLoopType.HOLD_ON_LAST_FRAME;
            } else if ((jp.isBoolean() && jp.getAsBoolean()) || (jp.isString() && jp.getAsString().equals("true"))) {
                return BrLoopType.LOOP;
            } else {
                return BrLoopType.ONCE;
            }
        } else {
            return BrLoopType.ONCE;
        }
    }
}
