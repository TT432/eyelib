package io.github.tt432.eyelib.client.model.bedrock.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TT432
 */
public record ModelMaterial(
        ResourceLocation[] textures,
        ResourceLocation renderType
) {
    public static ModelMaterial parse(JsonObject object) {
        JsonElement texturesJsonObject = object.get("textures");

        ResourceLocation[] textures;

        if (texturesJsonObject.isJsonArray()) {
            JsonArray texturesArray = texturesJsonObject.getAsJsonArray();
            textures = new ResourceLocation[texturesArray.size()];

            for (int i = 0; i < texturesArray.size(); i++) {
                textures[i] = new ResourceLocation(texturesArray.get(i).getAsString());
            }
        } else {
            textures = new ResourceLocation[0];
        }

        ResourceLocation renderType = new ResourceLocation(object.get("render_type").getAsString());

        return new ModelMaterial(textures, renderType);
    }
}
