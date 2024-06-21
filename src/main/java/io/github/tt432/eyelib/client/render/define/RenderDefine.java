package io.github.tt432.eyelib.client.render.define;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * @author TT432
 */
public record RenderDefine(
        ResourceLocation target,
        ResourceLocation model,
        ResourceLocation material,
        RDAnimationController animationControllerEntry,
        List<ResourceLocation> visitors
) {

    public record RDAnimationController(
            String name,
            String animation
    ) {
    }

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

        ResourceLocation model = ResourceLocation.parse(object.get("model") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty");

        ResourceLocation material = object.get("material") instanceof JsonPrimitive jp
                ? ResourceLocation.parse(jp.getAsString())
                : ResourceLocation.fromNamespaceAndPath(model.getNamespace(), model.getPath() + ".material");

        List<ResourceLocation> visitors = object.get("visitors") instanceof JsonArray ja
                ? ja.asList().stream().map(je -> ResourceLocation.parse(je.getAsString())).toList()
                : List.of(ResourceLocation.parse("eyelib:blank"));

        return new RenderDefine(
                ResourceLocation.parse(object.get("target") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                model,
                material,
                entry,
                visitors
        );
    }
}
