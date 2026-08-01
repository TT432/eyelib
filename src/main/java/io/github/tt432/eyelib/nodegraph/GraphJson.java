package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

/** nodegraph 内部 JSON 工具（独立于 importer，保持模块自包含）。 */
final class GraphJson {
    private GraphJson() {
    }

    /** JsonElement 直通 codec。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final Codec<JsonElement> ELEMENT_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> (JsonElement) dynamic.convert(JsonOps.INSTANCE).getValue(),
            jsonElement -> new Dynamic<>(JsonOps.INSTANCE, jsonElement)
    );
}
