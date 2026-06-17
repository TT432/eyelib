package io.github.tt432.eyelibbehavior.component;

import com.google.gson.JsonObject;

/**
 * 兜底组件，保留 importer 层的原始 JSON 数据。
 * 当组件尚未有 typed codec 时使用，保证数据不丢失。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RawComponent(
        String componentId,
        JsonObject rawData
) implements Component {
    @Override
    public String id() {
        return "raw:" + componentId;
    }
}
