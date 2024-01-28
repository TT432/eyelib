package io.github.tt432.eyelib.client.render.define;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TT432
 */
public record RenderDefine(
        ResourceLocation target,
        ResourceLocation model,
        ResourceLocation material,
        RDAnimationController animationControllerEntry
) {
    public static RenderDefine parse(JsonObject object) {
        RDAnimationController entry;

        if (object.get("animation_controller") instanceof JsonObject jo) {
            entry = new RDAnimationController(
                    jo.get("name") instanceof JsonPrimitive jp ? jp.getAsString() : "",
                    jo.get("animation") instanceof JsonPrimitive jp ? jp.getAsString() : ""
            );
        } else {
            entry = new RDAnimationController("", "");
        }

        ResourceLocation model = new ResourceLocation(object.get("model") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty");

        ResourceLocation material = object.get("material") instanceof JsonPrimitive jp
                ? new ResourceLocation(jp.getAsString())
                : new ResourceLocation(model.getNamespace(), model.getPath() + ".material");

        return new RenderDefine(
                new ResourceLocation(object.get("target") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                model,
                material,
                entry
        );
    }
}
