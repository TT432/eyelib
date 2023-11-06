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
    private Object animation_controllers;// TODO

    public static RenderDefine parse(JsonObject object) {
        ResourceLocation[] textures;

        if (object.get("textures") instanceof JsonArray jp) {
            textures = new ResourceLocation[jp.size()];
            for (int i = 0; i < jp.size(); i++) {
                textures[i] = new ResourceLocation(jp.get(i).getAsString());
            }
        } else {
            textures = new ResourceLocation[0];
        }

        return new RenderDefine(
                new ResourceLocation(object.get("target") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                new ResourceLocation(object.get("model") instanceof JsonPrimitive jp ? jp.getAsString() : "__empty"),
                textures,
                null
        );
    }
}
