package io.github.tt432.eyelib.client.render.define;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TT432
 */
@AllArgsConstructor
@Getter
public class RenderDefine {
    private ResourceLocation target;
    private ResourceLocation model;
    private ResourceLocation[] textures;
    private RDAnimationControllerEntry animationControllerEntry;

    public static RenderDefine parse(JsonObject object) {
        ResourceLocation[] textures;
        RDAnimationControllerEntry entry;

        if (object.get("textures") instanceof JsonArray jp) {
            textures = new ResourceLocation[jp.size()];
            for (int i = 0; i < jp.size(); i++) {
                textures[i] = new ResourceLocation(jp.get(i).getAsString());
            }
        } else {
            textures = new ResourceLocation[0];
        }

        if (object.get("animation_controller") instanceof JsonObject jo) {
            entry = new RDAnimationControllerEntry(
                    jo.get("name") instanceof JsonPrimitive jp ? jp.getAsString() : "",
                    jo.get("animation") instanceof JsonPrimitive jp ? jp.getAsString() : ""
            );
        } else {
            entry = new RDAnimationControllerEntry("", "");
        }

        return new RenderDefine(
                new ResourceLocation(object.get("target") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                new ResourceLocation(object.get("model") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                textures,
                entry
        );
    }
}
