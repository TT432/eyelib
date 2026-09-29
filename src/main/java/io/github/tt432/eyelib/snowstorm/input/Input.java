package io.github.tt432.eyelib.snowstorm.input;

import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.texture.TextureClass;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.three.TextureImage;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * input.js 逐字移植：编辑器表单输入。value setter → {@code Config.set(id, v)} 回写、
 * select meta_value 反查、event_timeline 维护 {@code timeline[{uuid,time,event[]}]}、
 * change() → onchange/updatePreview/registerEdit。
 *
 * <p>JS 动态值一律 {@link Object}（String / Double / Boolean / {@code List<Object>} /
 * {@code Map<String,Object>}）。浏览器 API（DOM Event、FileReader、document.title、
 * ExpressionBar）用接缝接口/静态钩子替代，UI 层注入（见偏离记录）。
 */
public class Input {

    // ==================================================================
    // 接缝（浏览器/Vue API，UI 层后续接入）
    // ==================================================================

    /** JS DOM Event 接缝：{@code e instanceof Event} 且 {@code e.target} 可取。 */
    public interface UiEvent {
        /** {@code e.target.nodeName}（select 元素为 {@code "SELECT"}）。 */
        String nodeName();

        /** {@code e.target.selectedOptions[0].id}；无选中或未实现返回 null。 */
        @Nullable String selectedOptionId();

        /** {@code e.target.files[0]}；无文件返回 null。 */
        @Nullable UiFile firstFile();

        /**
         * JS {@code e instanceof InputEvent || e instanceof KeyboardEvent}
         * （registerEdit 的 typing 600ms 合并判定；EditHistory 移植为 2 参 API，判定移到调用点）。
         */
        default boolean isTypingEvent() {
            return false;
        }
    }

    /** JS File 接缝（{@code e.target.files[0]} 或 {@code new File([bytes], 'unknown.png')}）。 */
    public interface UiFile {
        String name();

        /** Blob type；JS 中 {@code new File([bytes], name)} 未指定 type 时为 {@code ""}。 */
        String type();

        byte[] bytes();
    }

    /** ExpressionBar.vue 的 {@code ExpandedInput} 接缝。 */
    public interface ExpressionBar {
        /** {@code ExpandedInput.setup}（ExpressionBar mounted 后为 true）。 */
        boolean setup();

        /** {@code ExpandedInput.axis}。 */
        int axis();

        void axis(int axis);

        void input(@Nullable Input input);

        /** {@code ExpandedInput.updateText(text, language, focusing)}。 */
        void updateText(@Nullable Object text, String language, boolean focusing);
    }

    /** {@code ExpandedInput} 实例，UI 层（ExpressionBar 视图 mounted 时）注入。 */
    public static @Nullable ExpressionBar expressionBar;

    /** {@code document.title = ...} 接缝（identifier 输入的 onchange 使用）。 */
    public static @Nullable Consumer<String> documentTitle;

    // ==================================================================
    // 数据袋（JS constructor 的 data 对象字面量）
    // ==================================================================

    /** onchange / updatePreview 回调（JS 函数）。 */
    @FunctionalInterface
    public interface InputCallback {
        void call(@Nullable Object event);
    }

    /** condition 回调（JS 函数，参数为所属 group）。 */
    @FunctionalInterface
    public interface InputCondition {
        boolean test(InputStructure.Group group);
    }

    /** JS {@code new Input({...})} 的对象字面量。公开字段 + 同名链式 wither。 */
    public static final class Data {
        public @Nullable String id;
        public @Nullable String label;
        public @Nullable String info;
        public @Nullable String placeholder;
        public @Nullable InputType type;
        public int axis_count;
        public boolean required;
        public boolean expanded;
        public @Nullable Object value;
        public @Nullable Map<String, String> options;
        /** JS array：元素为 {@code [subject, group]} 二元 List（未解析）或 Group（update 后）。 */
        public @Nullable List<Object> mode_groups;
        /** 元素为 String 或 Boolean（JS 混排，如 {@code [true]}）。 */
        public @Nullable List<Object> enabled_modes;
        public @Nullable InputCondition condition;
        public @Nullable InputCallback updatePreview;
        public @Nullable InputCallback onchange;
        public @Nullable Double step;
        public @Nullable Double min;
        public @Nullable Double max;
        public @Nullable Boolean allow_upload;

        public Data id(String v) { id = v; return this; }

        public Data label(String v) { label = v; return this; }

        public Data info(String v) { info = v; return this; }

        public Data placeholder(String v) { placeholder = v; return this; }

        public Data type(InputType v) { type = v; return this; }

        public Data axis_count(int v) { axis_count = v; return this; }

        public Data required(boolean v) { required = v; return this; }

        public Data expanded(boolean v) { expanded = v; return this; }

        public Data value(@Nullable Object v) { value = v; return this; }

        /** 成对传入 key/label：{@code options("steady", "Steady", ...)}。 */
        public Data options(String... keyLabels) {
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i + 1 < keyLabels.length; i += 2) {
                map.put(keyLabels[i], keyLabels[i + 1]);
            }
            options = map;
            return this;
        }

        /** JS {@code mode_groups: ['subject', 'group']}（未归一化的单层数组，构造器里再包一层）。 */
        public Data mode_groups(String subject, String group) {
            List<Object> flat = new ArrayList<>();
            flat.add(subject);
            flat.add(group);
            mode_groups = flat;
            return this;
        }

        public Data enabled_modes(Object... modes) {
            enabled_modes = new ArrayList<>(List.of(modes));
            return this;
        }

        public Data condition(InputCondition v) { condition = v; return this; }

        public Data updatePreview(InputCallback v) { updatePreview = v; return this; }

        public Data onchange(InputCallback v) { onchange = v; return this; }

        public Data step(double v) { step = v; return this; }

        public Data min(double v) { min = v; return this; }

        public Data max(double v) { max = v; return this; }

        public Data allow_upload(boolean v) { allow_upload = v; return this; }
    }

    /** event_timeline 的 {@code timeline[{uuid, time, event[]}]} 条目。 */
    public static final class TimelineEntry {
        public String uuid = "";
        public double time;
        public List<Object> event = new ArrayList<>();
    }

    /** type==image 的 {@code this.image} 子对象。 */
    public static final class ImageData {
        public String name = "";
        public String data = "";
        public boolean hidden;
        /** JS 中由 change() 动态添加（初始 undefined）。 */
        public boolean loaded;
    }

    // ==================================================================
    // 字段（与 JS 实例属性同名）
    // ==================================================================

    public final InputType type;
    public boolean isInput = true;
    public final @Nullable String id;
    public @Nullable String label;
    public @Nullable String info;
    public @Nullable String placeholder;
    public final int axis_count;
    public boolean required;
    public boolean expanded;
    public boolean expandable;
    protected @Nullable Object _value;
    public @Nullable Map<String, String> options;
    /** 元素为未解析的 {@code [subject, group]} List 或已解析的 {@link InputStructure.Group}。 */
    public @Nullable List<Object> mode_groups;
    public @Nullable List<Object> enabled_modes;
    public @Nullable InputCondition condition;
    public @Nullable InputCallback updatePreview;
    public @Nullable InputCallback onchange;
    // select / select_custom
    public @Nullable Object meta_value;
    // image
    public @Nullable ImageData image;
    public @Nullable TextureImage image_element;
    public @Nullable Boolean allow_upload;
    // number
    public @Nullable Double step;
    public @Nullable Double min;
    public @Nullable Double max;
    // event_timeline
    public final List<TimelineEntry> timeline = new ArrayList<>();

    // ==================================================================
    // 构造（input.js constructor）
    // ==================================================================

    public Input(Data data) {
        this.type = data.type != null ? data.type : InputType.MOLANG;
        this.isInput = true;
        this.id = data.id;
        this.label = data.label;
        this.info = data.info;
        this.placeholder = data.placeholder;
        this.axis_count = data.axis_count != 0 ? data.axis_count : 1; // JS: data.axis_count || 1
        this.required = data.required; // JS: data.required == true
        this.expanded = data.expanded;
        this.expandable = (this.type == InputType.MOLANG || this.type == InputType.TEXT || this.type == InputType.NUMBER)
                && this.axis_count != -1;
        if (this.type == InputType.GRADIENT) setValue(data.value != null ? data.value : new ArrayList<>());

        this.options = data.options;
        this.mode_groups = data.mode_groups;
        if (this.mode_groups != null && !this.mode_groups.isEmpty() && !(this.mode_groups.get(0) instanceof List)) {
            List<Object> wrapped = new ArrayList<>();
            wrapped.add(this.mode_groups);
            this.mode_groups = wrapped;
        }
        this.enabled_modes = data.enabled_modes;
        this.condition = data.condition;

        this.updatePreview = data.updatePreview;
        this.onchange = data.onchange;

        if (this.type == InputType.IMAGE) {
            this.image = new ImageData();
            this.image_element = EditorRuntime.Emitter.config.texture.image;
            this.allow_upload = data.allow_upload;
        }
        if (this.type == InputType.NUMBER) {
            this.step = data.step;
            this.min = data.min;
            this.max = data.max;
        }
        // event_timeline：JS 为 this.timeline = []，Java 字段已初始化

        if (this.id != null) {
            setValue(configGet(EditorRuntime.Config, this.id));
        } else if (data.value != null) { // JS: data.value != undefined（loose，null 等价 undefined）
            setValue(data.value);
        } else if (this.type == InputType.MOLANG || this.type == InputType.TEXT) {
            setValue("");
        }
    }

    // ==================================================================
    // value getter/setter（input.js get value / set value）
    // ==================================================================

    public @Nullable Object getValue() {
        return this._value;
    }

    public void setValue(@Nullable Object v) {
        this._value = v;
        if (this.type == InputType.NUMBER) {
            if (v instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) v; // JS: v.forEach((n, i) => v[i] = parseFloat(n)) 原地改
                for (int i = 0; i < list.size(); i++) {
                    list.set(i, parseFloat(list.get(i)));
                }
            } else {
                v = parseFloat(v); // 注意 as-is：_value 保留原值，只有 Config 收到 parse 后的值
            }
        }
        if (this.id != null) {
            // JS Config.set 对数组是 splice 自拷贝（同引用时 no-op）；Java Config.set 先 clear 目标列表，
            // 传同引用会被清空，故列表一律传拷贝（内容语义与 JS 相同）。
            Object arg = v instanceof List<?> list ? new ArrayList<>(list) : v;
            EditorRuntime.Config.set(this.id, arg);
        }
        if (this.type == InputType.SELECT_CUSTOM && (this.options == null || this.options.get(String.valueOf(v)) == null)) {
            this.meta_value = this.options != null ? this.options.get("custom") : null;
        } else if (this.type == InputType.SELECT || this.type == InputType.SELECT_CUSTOM) {
            this.meta_value = this.options != null ? this.options.get(String.valueOf(v)) : null;
        }
        if (this.type == InputType.EVENT_TIMELINE) {
            this.timeline.clear();
            if (this._value instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> entry) {
                        TimelineEntry copy = new TimelineEntry();
                        copy.uuid = SnowstormUtil.guid();
                        copy.time = JsSemantics.toNumber(entry.get("distance"));
                        Object effects = entry.get("effects");
                        copy.event = asEventList(effects);
                        this.timeline.add(copy);
                    }
                }
            } else if (this._value instanceof Map<?, ?> map) {
                // JS: for (let key in this._value)
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    TimelineEntry entry = new TimelineEntry();
                    entry.uuid = SnowstormUtil.guid();
                    entry.time = parseFloat(String.valueOf(e.getKey()));
                    entry.event = asEventList(e.getValue());
                    this.timeline.add(entry);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asEventList(@Nullable Object effects) {
        // JS: effects instanceof Array ? effects : [effects]（数组保持同引用）
        if (effects instanceof List<?>) return (List<Object>) effects;
        List<Object> list = new ArrayList<>();
        list.add(effects);
        return list;
    }

    // ==================================================================
    // 方法（与 JS 同名）
    // ==================================================================

    public Input toggleExpand() {
        if (this.expandable) {
            this.expanded = !this.expanded;
        }
        return this;
    }

    public Input update() {
        return update(null);
    }

    /** input.js update(Data)：mode_groups 解析为 Group 并回写 {@code _selected_mode}。 */
    public Input update(@Nullable Map<String, InputStructure.Subject> Data) {
        if (this.mode_groups != null) {
            if (this.type == InputType.SELECT || this.type == InputType.SELECT_CUSTOM) {
                for (int i = 0; i < this.mode_groups.size(); i++) {
                    Object g = this.mode_groups.get(i);
                    InputStructure.Group group;
                    if (g instanceof List<?> ref) {
                        group = resolveGroup(Data, ref);
                        this.mode_groups.set(i, group);
                    } else {
                        group = (InputStructure.Group) g;
                    }
                    group._selected_mode = getValue();
                }
            } else if (this.type == InputType.CHECKBOX) {
                for (int i = 0; i < this.mode_groups.size(); i++) {
                    Object g = this.mode_groups.get(i);
                    InputStructure.Group group;
                    if (g instanceof List<?> ref) {
                        group = resolveGroup(Data, ref);
                        this.mode_groups.set(i, group);
                    } else {
                        group = (InputStructure.Group) g;
                    }
                    group._selected_mode = getValue();
                }
            }
        }
        return this;
    }

    /** JS {@code Data[group[0]][group[1]]}。 */
    private static InputStructure.Group resolveGroup(@Nullable Map<String, InputStructure.Subject> Data, List<?> ref) {
        if (Data == null) throw new NullPointerException("Data required to resolve mode_groups");
        InputStructure.Subject subject = Data.get((String) ref.get(0));
        InputStructure.Group group = subject != null ? subject.group((String) ref.get(1)) : null;
        if (group == null) throw new NullPointerException("Unknown mode_group " + ref);
        return group;
    }

    /**
     * input.js emitInput()。JS 引用全局 {@code window.event}；Java 无全局事件，
     * 传 null（偏离：user-input 分支不触发，UI 层应直接调 {@link #change(Object)} 传事件）。
     */
    public Input emitInput() {
        return change(null);
    }

    public Input change() {
        return change(null, null);
    }

    public Input change(@Nullable Object e) {
        return change(e, null);
    }

    /**
     * input.js change(e, node)。{@code e} 为 {@link UiEvent}（DOM Event）、{@code byte[]}
     * （Uint8Array）或 null；{@code node} 为 UI 输入节点（仅用于 color sliding 判定，非 null 即 truthy）。
     */
    public Input change(@Nullable Object e, @Nullable Object node) {
        if (this.type == InputType.IMAGE && e != null) {
            UiFile file = e instanceof byte[] bytes
                    ? new ByteFile("unknown.png", "", bytes) // JS: new File([e], 'unknown.png')
                    : e instanceof UiEvent event ? event.firstFile() : null;
            if (file != null) {
                // JS FileReader.readAsDataURL：data:<type>;base64,...（type 为空时 application/octet-stream）
                String mime = file.type().isEmpty() ? "application/octet-stream" : file.type();
                String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(file.bytes());
                this.image().name = file.name();
                this.image().data = dataUrl;
                TextureClass.Texture.source = dataUrl;
                TextureClass.Texture.updateCanvasFromSource();
                this.image().loaded = true;
                this.image().hidden = true; // JS 连续两次赋值（Vue 响应式 poke），as-is 保留
                this.image().hidden = false;
                EditorRuntime.Emitter.config.updateTexture();
            }
        }
        if (this.type == InputType.SELECT || this.type == InputType.SELECT_CUSTOM) {
            if (e instanceof UiEvent event && "SELECT".equals(event.nodeName())) {
                setValue(event.selectedOptionId());
            }
        }
        if (this.type == InputType.COLOR) {
            // JS: if (typeof this.value == 'object') this.value = this.value.hex8（取色器对象）
            if (getValue() instanceof Map<?, ?> color) {
                setValue(color.get("hex8"));
            }
        }
        if (this.type == InputType.EVENT_TIMELINE) {
            if (this._value instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) this._value;
                list.clear(); // JS: this._value.empty()
                this.timeline.sort(Comparator.comparingDouble(t -> t.time));
                for (TimelineEntry entry : this.timeline) {
                    Map<String, Object> entry2 = new LinkedHashMap<>();
                    entry2.put("distance", entry.time);
                    entry2.put("effects", entry.event);
                    list.add(entry2);
                }
            } else if (this._value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) this._value;
                map.clear();
                this.timeline.sort(Comparator.comparingDouble(t -> t.time));
                for (TimelineEntry entry : this.timeline) {
                    map.put(toFixed(entry.time, 2), entry.event); // JS: entry.time.toFixed(2)
                }
            }
        }
        if (this.onchange != null) {
            this.onchange.call(e);
        }
        if (this.updatePreview != null) {
            this.updatePreview.call(getValue());
        }
        // JS 死变量 color_input_sliding（计算后未使用）不移植
        if (e instanceof UiEvent || e instanceof byte[] || (this.type == InputType.COLOR && node != null)) {
            // User Input
            if (expressionBar != null && expressionBar.setup()
                    && (this.type == InputType.MOLANG || this.type == InputType.TEXT)) {
                updateExpressionBar(false);
            }
            // JS: registerEdit('change input', e, this.type == 'color' && node)
            // typing 合并条件 = (e instanceof InputEvent || KeyboardEvent) || cooldown；EditHistory 2 参 API
            boolean typingOrCooldown = (e instanceof UiEvent event && event.isTypingEvent())
                    || (this.type == InputType.COLOR && node != null);
            EditHistory.registerEdit("change input", typingOrCooldown);
        }
        this.update();
        return this;
    }

    /** input.js set(value)：赋值并触发 change()。 */
    public Input set(@Nullable Object value) {
        // JS: if (value === undefined) return; —— Java null 同时对应 undefined（见偏离记录）
        if (value == null) return this;
        if (this.type == InputType.SELECT || this.type == InputType.SELECT_CUSTOM) {
            setValue(value);
            if (this.type == InputType.SELECT_CUSTOM
                    && (this.options == null || this.options.get(String.valueOf(getValue())) == null)) {
                this.meta_value = this.options != null ? this.options.get("custom") : null;
            } else {
                this.meta_value = this.options != null ? this.options.get(String.valueOf(getValue())) : null;
            }
        } else {
            if (getValue() instanceof List<?>) {
                if (value instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Object> target = (List<Object>) getValue();
                    target.clear(); // JS: this.value.splice(0, Infinity, ...value)
                    target.addAll(list);
                }
            } else {
                setValue(value);
            }
        }
        change();
        return this;
    }

    public Input reset() {
        if (this.type == InputType.IMAGE) {
            this.image().data = "";
            TextureClass.Texture.source = "";
            TextureClass.Texture.internal_changes = false;
            TextureClass.Texture.updateCanvasFromSource();
            this.image().name = "";
            this.image().loaded = false;
            this.image().hidden = true;
            this.image().hidden = false;
        }
        if (this.updatePreview != null) {
            this.updatePreview.call(getValue());
        }
        return this;
    }

    public Input updateExpressionBar(boolean focusing) {
        if (expressionBar == null) return this;
        Object val;
        if (this.axis_count == 1) {
            val = getValue();
        } else if (getValue() instanceof List<?> list) {
            int axis = expressionBar.axis();
            val = axis >= 0 && axis < list.size() ? list.get(axis) : null;
        } else {
            val = null; // JS: 非数组按索引取 → undefined（字符串会取到字符，见偏离记录）
        }
        expressionBar.updateText(val, this.type == InputType.MOLANG ? "molang" : "generic", focusing);
        return this;
    }

    public boolean isVisible(InputStructure.Group group) {
        if (this.condition != null) {
            return this.condition.test(group);
        } else {
            return this.enabled_modes == null
                    || group._selected_mode == null // JS === null；初值 undefined 哨兵不等于 null，as-is
                    || this.enabled_modes.contains(group._selected_mode);
        }
    }

    public Input focus(int axis) {
        return focus(Integer.valueOf(axis));
    }

    public Input focus() {
        return focus(null);
    }

    /** input.js focus(axis)：ExpressionBar 联动（经 {@link #expressionBar} 接缝）。 */
    public Input focus(@Nullable Integer axis) {
        if (expressionBar == null) return this;
        expressionBar.input(this);
        if (axis != null) expressionBar.axis(axis);
        updateExpressionBar(true);
        return this;
    }

    // ==================================================================
    // 内部助手
    // ==================================================================

    private ImageData image() {
        ImageData img = this.image;
        if (img == null) throw new NullPointerException("image");
        return img;
    }
    /**
     * JS {@code Config[key]} 动态属性访问。wintersky Config 无 get(key) API，
     * 字段名与 key 一致，用反射读取；不存在的 key → null（JS undefined）。
     * 共用助手（input/io 包统一入口）。
     */
    public static @Nullable Object configGet(Config config, String key) {
        try {
            Field field = Config.class.getField(key);
            return field.get(config);
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            return null;
        }
    }

    /** JS parseFloat（统一用 wintersky 根包 JsonValues.jsParseFloat）。 */
    static double parseFloat(@Nullable Object v) {
        return JsonValues.jsParseFloat(v);
    }

    /** JS Number.prototype.toFixed(digits)（常规数值范围；NaN/Infinity/≥1e21 边界见偏离记录）。 */
    static String toFixed(double v, int digits) {
        return BigDecimal.valueOf(v).setScale(digits, RoundingMode.HALF_UP).toPlainString();
    }

    /** byte[]（JS Uint8Array）包装成的 File。 */
    private record ByteFile(String name, String type, byte[] bytes) implements UiFile {
    }
}
