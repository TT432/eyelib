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
    private ResourceLocation texture;
    private Object animation_controllers;// TODO

    public static RenderDefine parse(JsonObject object) {
        return new RenderDefine(
                new ResourceLocation(object.get("target") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                new ResourceLocation(object.get("model") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                new ResourceLocation(object.get("texture") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                null
        );
    }
}
