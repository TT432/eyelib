package io.github.tt432.eyelib.client.render.define;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.util.ResourceLocations;
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

        ResourceLocation model = ResourceLocations.of(object.get("model") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty");

        ResourceLocation material = object.get("material") instanceof JsonPrimitive jp
                ? ResourceLocations.of(jp.getAsString())
                : ResourceLocations.of(model.getNamespace(), model.getPath() + ".material");

        List<ResourceLocation> visitors = object.get("visitors") instanceof JsonArray ja
                ? ja.asList().stream().map(je -> ResourceLocations.of(je.getAsString())).toList()
                : List.of(ResourceLocations.of("eyelib:blank"));

        return new RenderDefine(
                ResourceLocations.of(object.get("target") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                model,
                material,
                entry,
                visitors
        );
    }
}
