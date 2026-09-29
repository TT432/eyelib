package io.github.tt432.eyelib.snowstorm.io;

import com.google.gson.JsonParser;
import io.github.tt432.eyelib.snowstorm.curve.Curve;
import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.event.EventList;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.input.InputType;
import io.github.tt432.eyelib.snowstorm.texture.TextureClass;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Snowstorm import.js 逐字移植：粒子 JSON 导入（updateConfig/updateInputsFromConfig/
 * loadFile/startNewProject/loadPreset + 8 个内置示例）。
 *
 * <p>偏离（vs JS）：
 * <ul>
 *   <li>vscode 消息通道（updateContent/setState/message 监听）与浏览器文件 IO
 *       （{@code importFile()} 的 IO.import、拖拽 ondrop、{@code confirm()} 对话框）剔除
 *       （ADR-0036 D6：文件 IO 归 MC 侧）。confirm 对话框 → {@link #confirmClear} 接缝。</li>
 *   <li>JS 顶层 {@code readystatechange} 监听（Emitter.start + PlaybackController.start）
 *       → 由 MC 侧编辑器 Screen 打开时调用 {@link EditorRuntime#PlaybackController} 的 start()。</li>
 *   <li>示例 JSON 的 webpack 静态 import → classpath 资源
 *       {@code snowstorm/examples/<id>.particle.json}，首次访问解析并缓存
 *       （与 JS 相同：同一 Map 引用被复用/别名化）。</li>
 *   <li>事件回填用 {@link EventList#updateFromConfig()}（条目为 EditorEvent 模型，
 *       JS 为裸 {uuid,id,event} 字面量）；回填后镜像到 InputStructure events 组列表。</li>
 * </ul>
 */
public final class SnowstormImport {

    private SnowstormImport() {
    }

    // ==================================================================
    // import.js:21-30 Samples（webpack JSON import → classpath 资源缓存）
    // ==================================================================

    /** JS Samples 的 key 集合（import.js:8-15 的 8 个示例）。 */
    private static final String[] SAMPLE_IDS = {
            "fire", "loading", "rainbow", "magic", "rain", "snow", "trail", "billboard"};

    /** 解析缓存：与 JS 模块级 import 相同，同一 Map 引用被复用（别名化为 as-is 语义）。 */
    private static final Map<String, Map<String, Object>> SAMPLES = new LinkedHashMap<>();

    /** JS {@code Samples[id]}：未知 id 返回 null（JS undefined）。 */
    public static @Nullable Map<String, Object> sample(String id) {
        Map<String, Object> cached = SAMPLES.get(id);
        if (cached != null) return cached;
        for (String known : SAMPLE_IDS) {
            if (known.equals(id)) {
                Map<String, Object> parsed = parseJsonObject(readResource(
                        "/snowstorm/examples/" + id + ".particle.json"));
                SAMPLES.put(id, parsed);
                return parsed;
            }
        }
        return null;
    }

    private static String readResource(String path) {
        try (InputStream in = SnowstormImport.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("缺少内置资源 " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** JS JSON.parse：Gson 解析后经 JsonValues.toJava 转为 Map/List/Double/String/Boolean。 */
    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> parseJsonObject(String raw) {
        return (Map<String, Object>) JsonValues.toJava(JsonParser.parseString(raw));
    }

    // ==================================================================
    // import.js:32-75 updateInputsFromConfig
    // ==================================================================

    public static void updateInputsFromConfig() {
        Config config = EditorRuntime.Config;
        InputStructure.forEachInput((input, key) -> {
            // JS: input.value = Config[input.id]（value setter 内含 Config.set 自写回，as-is）
            input.setValue(input.id != null ? Input.configGet(config, input.id) : null);
            Object value = input.getValue();
            if (input.type == InputType.MOLANG && JsSemantics.truthy(value)) {
                // import.js lineify 局部函数：含 ';' 的字符串断行并置 input.expanded = true
                if (value instanceof List<?> list) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i) instanceof String s && !s.isEmpty() && s.contains(";")) {
                            input.expanded = true;
                            @SuppressWarnings("unchecked")
                            List<Object> mutable = (List<Object>) list;
                            mutable.set(i, SnowstormUtil.lineify(s));
                        }
                    }
                } else if (value instanceof String s && !s.isEmpty() && s.contains(";")) {
                    input.expanded = true;
                    input.setValue(SnowstormUtil.lineify(s));
                }
            }
            input.update(InputStructure.Data);
        });

        // import.js:54-62 曲线重建
        InputStructure.Group curvesGroup = InputStructure.Data.get("variables").group("curves");
        if (curvesGroup != null) {
            curvesGroup.curves.clear(); // JS: splice(0, Infinity)
            for (Map.Entry<String, Config.Curve> e : config.curves.entrySet()) {
                String id = e.getKey();
                Curve curve = new Curve(e.getValue());
                Input idInput = curve.inputs.get("id");
                if (idInput != null) idInput.setValue(id); // JS: curve.inputs.id.value = id
                curvesGroup.curves.add(curve);
                config.curves.put(id, curve.config); // JS: Config.curves[id] = curve.config（同键覆写）
                curve.updateMinMax();
            }
        }

        // import.js:63-70 事件重建（EventList 模型 + 镜像到 events 组列表，见类 doc 偏离）
        EventList eventList = new EventList(config);
        eventList.updateFromConfig();
        InputStructure.Group eventsGroup = InputStructure.Data.get("events").group("events");
        if (eventsGroup != null) {
            eventsGroup.events.clear(); // JS: splice(0)
            eventsGroup.events.addAll(eventList.events);
        }

        // import.js:72 Data.effect.meta.inputs.identifier.onchange()
        InputStructure.Group metaGroup = InputStructure.Data.get("effect").group("meta");
        Input identifier = metaGroup != null ? metaGroup.inputs.get("identifier") : null;
        if (identifier != null && identifier.onchange != null) {
            identifier.onchange.call(null);
        }

        // import.js:74 View.updateVariablePlaceholderList()
        EditorRuntime.updateVariablePlaceholderList();
    }

    // ==================================================================
    // import.js:77-91 updateConfig
    // ==================================================================

    public static void updateConfig(Map<String, Object> data) {
        EditorRuntime.unsupported_fields = new LinkedHashMap<>();
        EditorRuntime.Config.setFromJSON(data);

        Map<String, Object> particleEffect = JsonValues.asMap(data.get("particle_effect"));
        if (particleEffect != null) {
            EditorRuntime.unsupported_fields.put("events", particleEffect.get("events"));
            // JS: data.particle_effect.components[...]（components 缺失时 JS 抛 TypeError，此处 NPE 等价）
            Map<String, Object> components = JsonValues.asMap(particleEffect.get("components"));
            EditorRuntime.unsupported_fields.put("emitter_lifetime_events",
                    components != null ? components.get("minecraft:emitter_lifetime_events") : null);
            EditorRuntime.unsupported_fields.put("particle_lifetime_events",
                    components != null ? components.get("minecraft:particle_lifetime_events") : null);
            Map<String, Object> motionCollision =
                    components != null ? JsonValues.asMap(components.get("minecraft:particle_motion_collision")) : null;
            if (motionCollision != null) {
                EditorRuntime.unsupported_fields.put("collision_events", motionCollision.get("events"));
            }
        }

        updateInputsFromConfig();
    }

    // ==================================================================
    // import.js:92-101 loadFile
    // ==================================================================

    /** JS {@code loadFile(data, confirmNewProject=true)}。 */
    public static void loadFile(@Nullable Map<String, Object> data) {
        loadFile(data, true);
    }

    public static void loadFile(@Nullable Map<String, Object> data, boolean confirmNewProject) {
        if (data != null && data.get("particle_effect") != null && (!confirmNewProject || startNewProject())) {
            TextureClass.Texture.reset();
            EditorRuntime.QuickSetup.resetAll.run();
            updateConfig(data);
            EditorRuntime.Emitter.stop(true);
            EditorRuntime.PlaybackController.start();
            EditHistory.registerEdit("load file");
        }
    }

    // ==================================================================
    // import.js:103-113 window.loadFileFromParentEffect
    // ==================================================================

    /** JS window.loadFileFromParentEffect(raw_json, texture_url)。 */
    public static void loadFileFromParentEffect(String rawJson, @Nullable String textureUrl) {
        loadFile(parseJsonObject(rawJson), false);
        if (textureUrl != null && !textureUrl.isEmpty()) {
            InputStructure.Group textureGroup = InputStructure.Data.get("texture").group("texture");
            Input input = textureGroup != null ? textureGroup.inputs.get("image") : null;
            Input.ImageData image = input != null ? input.image : null;
            if (image != null) {
                image.data = textureUrl;
                TextureClass.Texture.source = textureUrl;
                image.loaded = true;
                TextureClass.Texture.updateCanvasFromSource();
                EditorRuntime.Config.updateTexture();
                EditorRuntime.Emitter.updateMaterial();
            }
        }
    }

    // ==================================================================
    // import.js:115-123 importFile / 136-139 readystatechange —— 剔除（见类 doc 偏离）
    // ==================================================================

    // ==================================================================
    // import.js:124-134 startNewProject
    // ==================================================================

    /**
     * JS {@code confirm('This action may clear your current work. ...')} 对话框接缝
     * （浏览器 UI，由 MC 侧 Screen 安装实现）。默认拒绝（不清空当前工作）。
     */
    public static BooleanSupplier confirmClear = () -> false;

    /** JS {@code startNewProject()}（force 未传 → 走 confirm）。 */
    public static boolean startNewProject() {
        return startNewProject(false);
    }

    public static boolean startNewProject(boolean force) {
        // JS: vscode || force || confirm(...) —— vscode 分支剔除
        if (force || confirmClear.getAsBoolean()) {
            EditorRuntime.unsupported_fields = new LinkedHashMap<>();
            EditorRuntime.Config.reset();
            TextureClass.Texture.reset();
            EditorRuntime.QuickSetup.resetAll.run();
            updateInputsFromConfig();
            EditHistory.registerEdit("new file");
            return true;
        }
        return false; // JS 隐式返回 undefined
    }

    // ==================================================================
    // import.js:141-143 loadPreset
    // ==================================================================

    public static void loadPreset(String id) {
        loadFile(sample(id));
    }
}
