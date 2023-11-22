package io.github.tt432.eyelib.client.render.define;

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
    private ResourceLocation material;
    private RDAnimationControllerEntry animationControllerEntry;

    public static RenderDefine parse(JsonObject object) {
        RDAnimationControllerEntry entry;

        if (object.get("animation_controller") instanceof JsonObject jo) {
            entry = new RDAnimationControllerEntry(
                    jo.get("name") instanceof JsonPrimitive jp ? jp.getAsString() : "",
                    jo.get("animation") instanceof JsonPrimitive jp ? jp.getAsString() : ""
            );
        } else {
            entry = new RDAnimationControllerEntry("", "");
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
