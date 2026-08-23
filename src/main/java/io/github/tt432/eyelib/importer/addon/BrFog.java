package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bedrock fog 定义（fogs/*.json 的 {@code minecraft:fog_settings}）最小语义子集：
 * {@code description.identifier} + {@code distance.{air,weather,water,lava,lava_resistance}}
 * （每项 fog_start / fog_end / render_distance_type / fog_color）。
 * {@code volumetric}（光线追踪）与 {@code transition_fog}（水下过渡）属增强能力，忽略不解析。
 * 手写 parse 风格与 {@link BrSoundDefinitions#parse} 一致；未知字段一律忽略，
 * 缺 identifier（或 identifier 无命名空间）抛 {@link IllegalArgumentException}
 * （BedrockAddonLoader.processEntry 会把该异常归档为 SCHEMA_PARSE_FAILED 警告）。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BrFog(
        String identifier,
        LinkedHashMap<String, DistanceSetting> distances
) {
    /** distance 子项键：空气介质（主渲染雾消费的子项）。 */
    public static final String MEDIUM_AIR = "air";

    /** distance 子项键全集（Bedrock 文档规定的五种介质）。 */
    public static final Set<String> MEDIUM_KEYS = Set.of("air", "weather", "water", "lava", "lava_resistance");

    public BrFog {
        distances = new LinkedHashMap<>(distances);
    }

    public static BrFog parse(JsonObject root) {
        JsonObject settings = root.getAsJsonObject("minecraft:fog_settings");
        if (settings == null) {
            throw new IllegalArgumentException("fog file missing minecraft:fog_settings object");
        }
        JsonObject description = settings.getAsJsonObject("description");
        JsonElement identifierElement = description == null ? null : description.get("identifier");
        if (identifierElement == null || !identifierElement.isJsonPrimitive()
                || identifierElement.getAsString().isBlank()) {
            throw new IllegalArgumentException("fog file missing description.identifier");
        }
        String identifier = identifierElement.getAsString();
        // Bedrock 文档要求每个 fog identifier 必带命名空间（minecraft: 仅限官方包）
        if (identifier.indexOf(':') < 0) {
            throw new IllegalArgumentException("fog identifier requires a namespace: " + identifier);
        }

        LinkedHashMap<String, DistanceSetting> distances = new LinkedHashMap<>();
        JsonObject distance = settings.getAsJsonObject("distance");
        if (distance != null) {
            for (Map.Entry<String, JsonElement> entry : distance.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                distances.put(entry.getKey().toLowerCase(Locale.ROOT),
                        DistanceSetting.parse(entry.getValue().getAsJsonObject()));
            }
        }
        return new BrFog(identifier, distances);
    }

    /** 取某介质的 distance 子项（缺省时为 null，消费方自行回落）。 */
    public @Nullable DistanceSetting distance(String medium) {
        return distances.get(medium);
    }

    /**
     * distance 子项：fog_start / fog_end / render_distance_type（fixed=块距 / render=渲染距离比例）
     * / fog_color（#RRGGBB）。字段缺省时回落到中性默认值（render 型 0→1.0、白色），
     * 与 Bedrock「未设置的值取下一优先级 fog」语义的最小近似。
     */
    @org.jspecify.annotations.NullMarked
    public record DistanceSetting(
            float fogStart,
            float fogEnd,
            String renderDistanceType,
            String fogColor
    ) {
        static DistanceSetting parse(JsonObject obj) {
            float fogStart = obj.has("fog_start") ? obj.get("fog_start").getAsFloat() : 0.0f;
            float fogEnd = obj.has("fog_end") ? obj.get("fog_end").getAsFloat() : 1.0f;
            String renderDistanceType = obj.has("render_distance_type")
                    ? obj.get("render_distance_type").getAsString() : "render";
            String fogColor = obj.has("fog_color") ? obj.get("fog_color").getAsString() : "#FFFFFF";
            return new DistanceSetting(fogStart, fogEnd, renderDistanceType, fogColor);
        }

        /** render_distance_type == "render"：start/end 是渲染距离比例而非块距。 */
        public boolean renderRelative() {
            return "render".equalsIgnoreCase(renderDistanceType);
        }

        /** fog_start 换算为块距（render 型 × 渲染距离块数，fixed 型原值）。 */
        public float fogStartBlocks(float renderDistanceBlocks) {
            return renderRelative() ? fogStart * renderDistanceBlocks : fogStart;
        }

        /** fog_end 换算为块距（render 型 × 渲染距离块数，fixed 型原值）。 */
        public float fogEndBlocks(float renderDistanceBlocks) {
            return renderRelative() ? fogEnd * renderDistanceBlocks : fogEnd;
        }

        /** fog_color（#RRGGBB）解析为 rgb 三浮点分量；格式非法时回落白色。 */
        public float[] rgbFloats() {
            String hex = fogColor.startsWith("#") ? fogColor.substring(1) : fogColor;
            if (hex.length() == 6) {
                try {
                    int rgb = Integer.parseInt(hex, 16);
                    return new float[]{
                            ((rgb >> 16) & 0xFF) / 255.0f,
                            ((rgb >> 8) & 0xFF) / 255.0f,
                            (rgb & 0xFF) / 255.0f
                    };
                } catch (NumberFormatException ignored) {
                    // fall through：非法颜色按白色处理
                }
            }
            return new float[]{1.0f, 1.0f, 1.0f};
        }
    }
}
