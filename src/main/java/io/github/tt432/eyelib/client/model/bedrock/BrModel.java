package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public record BrModel(
        List<BrModelEntry> models
) {

    public static BrModel parse(JsonObject object) {
        List<BrModelEntry> models = new ArrayList<>();

        if (object.has("minecraft:geometry")) {
            for (JsonElement jsonElement : object.getAsJsonArray("minecraft:geometry")) {
                models.add(BrModelEntry.parse(jsonElement.getAsJsonObject()));
            }
        } else {
            object.asMap().forEach((n, m) -> {
                if (n.startsWith("geometry")) {
                    models.add(BrModelEntry.parse2(n, m.getAsJsonObject()));
                }
            });
        }

        return new BrModel(models);
    }
}
