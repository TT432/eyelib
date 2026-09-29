package io.github.tt432.eyelib.snowstorm.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.Scene;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * event_sub_effects.js as-is 移植：事件子效果注册表 {@code {identifier: {json, texture}}}。
 *
 * <p>JS 中 {@code EventSubEffects} 是模块级单例对象，此处为静态注册表。
 * 浏览器文件对话框（IO.import）与新标签页编辑（window.open / vscode postMessage）
 * 属于宿主接缝：文件内容由调用方传入，编辑动作经 {@link #edit_callback} 回调。</p>
 */
public final class EventSubEffects {

    /** JS {@code EventSubEffects[identifier]} 条目：{json, texture}。json 为 JSON.parse 形态（LinkedHashMap）。 */
    public static final class Entry {
        public @Nullable Object json;
        public @Nullable String texture;
    }

    /** JS window.getSubEffectDataForParentEffect 的条目：{json: 字符串化, texture}。 */
    public static final class SubEffectData {
        public @Nullable String json;
        public @Nullable String texture;

        public SubEffectData(@Nullable String json, @Nullable String texture) {
            this.json = json;
            this.texture = texture;
        }
    }

    private static final Map<String, Entry> REGISTRY = new LinkedHashMap<>();
    private static final Gson GSON = new Gson();

    /** 对应 JS {@code import {Scene} from './emitter'} 模块单例；由编辑器装配（EditorRuntime）赋值。 */
    public static @Nullable Scene scene;

    /** editEventSubEffect 回调接缝：JS 为 vscode postMessage 或 window.open 新标签页。 */
    public static @Nullable Consumer<String> edit_callback;

    /** event_sub_effects.js EmptyFile（深拷贝后写入 identifier）。 */
    private static final JsonObject EMPTY_FILE = JsonParser.parseString("""
            {
            	"format_version": "1.10.0",
            	"particle_effect": {
            		"description": {
            			"identifier": "",
            			"basic_render_parameters": {
            				"texture": ""
            			}
            		},
            		"components": {}
            	}
            }
            """).getAsJsonObject();

    private EventSubEffects() {
    }

    /** JS {@code EventSubEffects[identifier]} 读取（不存在 → undefined → null）。 */
    public static @Nullable Entry get(String identifier) {
        return REGISTRY.get(identifier);
    }

    /** JS {@code if (!EventSubEffects[identifier]) EventSubEffects[identifier] = {}}。 */
    public static Entry getOrCreate(String identifier) {
        return REGISTRY.computeIfAbsent(identifier, k -> new Entry());
    }

    /** JS {@code EventSubEffects[identifier] = entry}。 */
    public static void put(String identifier, Entry entry) {
        REGISTRY.put(identifier, entry);
    }

    /** 注册表迭代（JS {@code for (let identifier in EventSubEffects)}，插入序）。 */
    public static Map<String, Entry> asMap() {
        return REGISTRY;
    }

    /**
     * loadEventSubEffect as-is（减去文件对话框）：调用方传入解析后的 particle JSON
     * （JSON.parse 形态；对话框取消对应 JS resolve(undefined)，传 null）。
     *
     * <p>JS quirk：json 为 undefined 时 identifier 取 ''，仍注册 {@code EventSubEffects[''].json = undefined}。</p>
     *
     * @return identifier
     */
    public static String loadEventSubEffect(@Nullable Object json) {
        Object rawIdentifier = null;
        Map<String, Object> map = JsonValues.asMap(json);
        if (map != null) {
            Map<String, Object> particleEffect = JsonValues.asMap(map.get("particle_effect"));
            if (particleEffect != null) {
                Map<String, Object> description = JsonValues.asMap(particleEffect.get("description"));
                if (description != null) {
                    rawIdentifier = description.get("identifier");
                }
            }
        }
        String identifier = JsSemantics.truthy(rawIdentifier) ? JsSemantics.toJsString(rawIdentifier) : "";
        Entry entry = getOrCreate(identifier);
        entry.json = json;
        requireScene().child_configs.remove(identifier);
        return identifier;
    }

    /** createEventSubEffect as-is：EmptyFile 深拷贝 + 写入 identifier。 */
    public static String createEventSubEffect(String identifier) {
        Entry entry = getOrCreate(identifier);
        JsonObject json = EMPTY_FILE.deepCopy();
        json.getAsJsonObject("particle_effect").getAsJsonObject("description").addProperty("identifier", identifier);
        entry.json = JsonValues.toJava(json);
        requireScene().child_configs.remove(identifier);
        return identifier;
    }

    /** loadEventSubEffectTexture as-is（减去文件对话框）：image_url 由调用方传入（取消 → null）。 */
    public static void loadEventSubEffectTexture(String identifier, @Nullable String imageUrl) {
        Entry entry = getOrCreate(identifier);
        entry.texture = imageUrl;

        Config childConfig = requireScene().child_configs.get(identifier);
        if (childConfig != null) {
            childConfig.updateTexture();
        }
    }

    /**
     * editEventSubEffect：JS 在 vscode 下 postMessage('open_particle_file_tab')，
     * 浏览器下 window.open 新标签页并维护 SubEffectEditors。Java 侧为回调接缝；
     * 未设置回调对应 JS popup 被拦截（{@code if (!editor_tab) return}）的 no-op。
     */
    public static void editEventSubEffect(String identifier) {
        if (edit_callback != null) {
            edit_callback.accept(identifier);
        }
    }

    /** JS window.getSubEffectDataForParentEffect：{identifier: {json: JSON.stringify(json), texture}}。 */
    public static Map<String, SubEffectData> getSubEffectDataForParentEffect() {
        Map<String, SubEffectData> data = new LinkedHashMap<>();
        for (Map.Entry<String, Entry> e : REGISTRY.entrySet()) {
            Entry entry = e.getValue();
            data.put(e.getKey(), new SubEffectData(
                    entry.json != null ? GSON.toJson(entry.json) : null,
                    entry.texture));
        }
        return data;
    }

    private static Scene requireScene() {
        Scene s = scene;
        if (s == null) {
            throw new IllegalStateException("EventSubEffects.scene 未装配（对应 JS emitter.js Scene 单例）");
        }
        return s;
    }
}
