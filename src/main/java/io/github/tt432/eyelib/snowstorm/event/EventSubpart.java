package io.github.tt432.eyelib.snowstorm.event;

import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EventSubpart.vue 递归模型 as-is 移植（仅数据操作逻辑，UI 不移植）。
 *
 * <p>JS 中 subpart 是普通对象，键按需增删（Vue.set/Vue.delete）：
 * sequence / randomize / particle_effect / sound_effect / expression。
 * 此处以 @Nullable 字段表达「键不存在」。</p>
 *
 * <p>互斥规则（模板 create_bar 条件，模型方法本身不守卫，as-is）：</p>
 * <ul>
 *   <li>叶子（particle/sound/expression）创建按钮：{@code !sequence && !randomize} 且该叶子不存在；</li>
 *   <li>sequence/randomize 创建按钮：{@code !particle_effect && !sound_effect && expression == undefined}
 *       且 {@code !sequence && !randomize}；</li>
 * </ul>
 * 即 sequence|randomize 与叶子互斥；sequence 与 randomize 互斥。
 */
public class EventSubpart {

    /** JS {@code v-model} 编辑事件冒泡：{@code this.$emit('modify_event', event, type)}。 */
    public interface ModifyListener {
        /** @param type JS 第二参数，'text' 表示文本输入（registerEdit 走 typing 合并） */
        void modifyEvent(@Nullable Object event, @Nullable String type);
    }

    /** EventSubpart.vue emitter_type_options（插入序）。 */
    public static final Map<String, String> EMITTER_TYPE_OPTIONS = new LinkedHashMap<>();

    static {
        EMITTER_TYPE_OPTIONS.put("emitter", "Emitter");
        EMITTER_TYPE_OPTIONS.put("emitter_bound", "Emitter Bound");
        EMITTER_TYPE_OPTIONS.put("particle", "Particle");
        EMITTER_TYPE_OPTIONS.put("particle_with_velocity", "Particle with Velocity");
    }

    /** JS data is_extension: !!vscode。vscode 扩展宿主不移植，恒 false，保留字段以对齐判断逻辑。 */
    public static boolean is_extension = false;

    /** particle_effect 子对象：{effect, type, pre_effect_expression}。 */
    public static final class ParticleEffect {
        public String effect = "";
        public String type = "emitter";
        public String pre_effect_expression = "";
    }

    /** sound_effect 子对象：{event_name}。 */
    public static final class SoundEffect {
        public String event_name = "";
    }

    /** 编辑器侧 id（sequence/randomize option 创建时 guid()；导出时 cleanEvent 删除）。 */
    public @Nullable String uuid;
    /** randomize option 权重（JS 仅 randomize option 带 weight 键）。 */
    public @Nullable Double weight;

    public @Nullable List<EventSubpart> sequence;
    public @Nullable List<EventSubpart> randomize;
    public @Nullable ParticleEffect particle_effect;
    public @Nullable SoundEffect sound_effect;
    public @Nullable String expression;

    /** modify_event 冒泡接缝，由父级（EventList 或外层 subpart）设置。 */
    public @Nullable ModifyListener modify_listener;

    // ---- create_bar 增节（Vue.set） ----

    public void createSequenceSection() {
        EventSubpart option = new EventSubpart();
        option.uuid = SnowstormUtil.guid();
        sequence = new ArrayList<>();
        sequence.add(option);
        propagateModifyListener();
        modifyEvent(null, null);
    }

    public void createRandomizeSection() {
        EventSubpart option = new EventSubpart();
        option.uuid = SnowstormUtil.guid();
        option.weight = 1.0;
        randomize = new ArrayList<>();
        randomize.add(option);
        propagateModifyListener();
        modifyEvent(null, null);
    }

    public void createParticleSection() {
        particle_effect = new ParticleEffect();
        modifyEvent(null, null);
    }

    public void createSoundSection() {
        sound_effect = new SoundEffect();
        modifyEvent(null, null);
    }

    public void createExpressionSection() {
        expression = "";
        modifyEvent(null, null);
    }

    // ---- create_bar 可见性（互斥规则，模板条件 as-is） ----

    /** 模板：{@code v-if="!subpart.sequence && !subpart.randomize && !subpart.particle_effect"}。 */
    public boolean canCreateParticleSection() {
        return sequence == null && randomize == null && particle_effect == null;
    }

    /** 模板：{@code v-if="!subpart.sequence && !subpart.randomize && !subpart.sound_effect"}。 */
    public boolean canCreateSoundSection() {
        return sequence == null && randomize == null && sound_effect == null;
    }

    /** 模板：{@code v-if="!subpart.sequence && !subpart.randomize && !subpart.expression"}（falsy：null/''）。 */
    public boolean canCreateExpressionSection() {
        return sequence == null && randomize == null && !JsSemantics.truthy(expression);
    }

    /** 模板：叶子均不存在（expression 用 == undefined，'' 仍阻挡）且非 sequence/randomize。 */
    public boolean canCreateSequenceSection() {
        return particle_effect == null && sound_effect == null && expression == null
                && sequence == null && randomize == null;
    }

    /** 同 {@link #canCreateSequenceSection()}。 */
    public boolean canCreateRandomizeSection() {
        return canCreateSequenceSection();
    }

    // ---- sequence/randomize option 增删 ----

    public void addSequenceOption() {
        EventSubpart option = new EventSubpart();
        option.uuid = SnowstormUtil.guid();
        if (sequence == null) sequence = new ArrayList<>(); // JS 调用前提是 sequence 存在；防御
        sequence.add(option);
        propagateModifyListener();
        modifyEvent(null, null);
    }

    public void addRandomizeOption() {
        EventSubpart option = new EventSubpart();
        option.uuid = SnowstormUtil.guid();
        option.weight = 1.0;
        if (randomize == null) randomize = new ArrayList<>();
        randomize.add(option);
        propagateModifyListener();
        modifyEvent(null, null);
    }

    /** JS：remove(option) 后若空则 Vue.delete(subpart, 'sequence')。 */
    public void removeSequenceOption(EventSubpart option) {
        if (sequence != null) {
            sequence.remove(option);
            if (sequence.isEmpty()) {
                sequence = null;
            }
        }
        modifyEvent(null, null);
    }

    public void removeRandomizeOption(EventSubpart option) {
        if (randomize != null) {
            randomize.remove(option);
            if (randomize.isEmpty()) {
                randomize = null;
            }
        }
        modifyEvent(null, null);
    }

    // ---- 禁用节（Vue.delete） ----

    public void disableParticleSection() {
        particle_effect = null;
        modifyEvent(null, null);
    }

    public void disableSoundSection() {
        sound_effect = null;
        modifyEvent(null, null);
    }

    public void disableExpressionSection() {
        expression = null;
        modifyEvent(null, null);
    }

    // ---- 拖拽排序（sort.js → 模型方法） ----

    /** JS sortList(subpart.sequence, event) 的模型侧：见 {@link Sort#move}。 */
    public boolean moveSequenceOption(int originalIndex, int hoverIndex) {
        boolean moved = sequence != null && Sort.move(sequence, originalIndex, hoverIndex);
        return moved;
    }

    public boolean moveRandomizeOption(int originalIndex, int hoverIndex) {
        boolean moved = randomize != null && Sort.move(randomize, originalIndex, hoverIndex);
        return moved;
    }

    // ---- 子效果文件操作（IO/对话框为宿主接缝，数据操作 as-is） ----

    /**
     * selectParticleFile 的数据部分：JS 先弹文件对话框 loadEventSubEffect()，
     * 此处由调用方传入得到的 identifier（取消 → null）。
     * JS quirk：连续两次 {@code this.is_extension = !this.is_extension}（无操作）。
     */
    public void applySelectedParticleFile(@Nullable String identifier) {
        if (particle_effect != null && JsSemantics.truthy(identifier)
                && !identifier.equals(particle_effect.effect)) {
            particle_effect.effect = identifier;
            modifyEvent(null, null);
        }
    }

    /** createNewParticleFile 的数据部分：注册空子效果 + 写入 effect（对话框关闭为 UI）。 */
    public void applyCreateNewParticleFile(@Nullable String identifier) {
        if (identifier != null) {
            EventSubEffects.createEventSubEffect(identifier);
        }
        applySelectedParticleFile(identifier);
    }

    /** selectParticleTexture 的数据部分：image_url 由调用方传入。 */
    public void applySelectedParticleTexture(@Nullable String imageUrl) {
        if (particle_effect != null) {
            EventSubEffects.loadEventSubEffectTexture(particle_effect.effect, imageUrl);
        }
    }

    /** editParticleFile as-is：editEventSubEffect(effect)。 */
    public void editParticleFile() {
        if (particle_effect != null) {
            EventSubEffects.editEventSubEffect(particle_effect.effect);
        }
    }

    /** JS canEditParticleFile：{@code is_extension || EventSubEffects[effect] != undefined}。 */
    public boolean canEditParticleFile() {
        // Todo（as-is 注释）: Remove extension override and make sure effects are instantly auto loaded
        return is_extension
                || (particle_effect != null && EventSubEffects.get(particle_effect.effect) != null);
    }

    // ---- modify_event 冒泡 ----

    /** JS {@code this.$emit('modify_event', event, type)}。 */
    public void modifyEvent(@Nullable Object event, @Nullable String type) {
        if (modify_listener != null) {
            modify_listener.modifyEvent(event, type);
        }
    }

    /** 新建 option 后把自身的冒泡接缝传给子级（对应 Vue 嵌套 event-subpart 的 @modify_event 链）。 */
    private void propagateModifyListener() {
        ModifyListener listener = modify_listener;
        List<EventSubpart> seq = sequence;
        if (seq != null) {
            for (EventSubpart option : seq) {
                option.modify_listener = listener;
            }
        }
        List<EventSubpart> rnd = randomize;
        if (rnd != null) {
            for (EventSubpart option : rnd) {
                option.modify_listener = listener;
            }
        }
    }

    // ---- 序列化（Config.events 存储形态 / import 回填） ----

    /**
     * 转 JS 对象字面量形态（LinkedHashMap，键序按 JS 属性插入序：
     * uuid → weight → sequence/randomize（创建时）→ particle_effect → sound_effect → expression）。
     * 与 JS 一样保留 uuid/weight（export.js cleanEvent 才删 uuid）。
     */
    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (uuid != null) map.put("uuid", uuid);
        if (weight != null) map.put("weight", weight);
        if (sequence != null) {
            List<Object> list = new ArrayList<>(sequence.size());
            for (EventSubpart option : sequence) {
                list.add(option.toJson());
            }
            map.put("sequence", list);
        }
        if (randomize != null) {
            List<Object> list = new ArrayList<>(randomize.size());
            for (EventSubpart option : randomize) {
                list.add(option.toJson());
            }
            map.put("randomize", list);
        }
        ParticleEffect pe = particle_effect;
        if (pe != null) {
            Map<String, Object> peMap = new LinkedHashMap<>();
            peMap.put("effect", pe.effect);
            peMap.put("type", pe.type);
            peMap.put("pre_effect_expression", pe.pre_effect_expression);
            map.put("particle_effect", peMap);
        }
        SoundEffect se = sound_effect;
        if (se != null) {
            Map<String, Object> seMap = new LinkedHashMap<>();
            seMap.put("event_name", se.event_name);
            map.put("sound_effect", seMap);
        }
        if (expression != null) map.put("expression", expression);
        return map;
    }

    /**
     * 从 Config.events 的 JSON 形态回填（import.js 事件定义侧）。
     * JS quirk as-is：导入不为 option 生成 uuid（Vue :key 为 undefined）。
     */
    public static EventSubpart fromJson(@Nullable Object json) {
        EventSubpart subpart = new EventSubpart();
        Map<String, Object> map = JsonValues.asMap(json);
        if (map == null) return subpart;
        Object uuid = map.get("uuid");
        if (uuid != null) subpart.uuid = JsSemantics.toJsString(uuid);
        Object weight = map.get("weight");
        if (weight instanceof Number n) subpart.weight = n.doubleValue();
        List<Object> sequence = JsonValues.asList(map.get("sequence"));
        if (sequence != null) {
            subpart.sequence = new ArrayList<>(sequence.size());
            for (Object option : sequence) {
                subpart.sequence.add(fromJson(option));
            }
        }
        List<Object> randomize = JsonValues.asList(map.get("randomize"));
        if (randomize != null) {
            subpart.randomize = new ArrayList<>(randomize.size());
            for (Object option : randomize) {
                subpart.randomize.add(fromJson(option));
            }
        }
        Map<String, Object> pe = JsonValues.asMap(map.get("particle_effect"));
        if (pe != null) {
            ParticleEffect particleEffect = new ParticleEffect();
            Object effect = pe.get("effect");
            if (effect != null) particleEffect.effect = JsSemantics.toJsString(effect);
            Object type = pe.get("type");
            if (type != null) particleEffect.type = JsSemantics.toJsString(type);
            Object preExpression = pe.get("pre_effect_expression");
            if (preExpression != null) particleEffect.pre_effect_expression = JsSemantics.toJsString(preExpression);
            subpart.particle_effect = particleEffect;
        }
        Map<String, Object> se = JsonValues.asMap(map.get("sound_effect"));
        if (se != null) {
            SoundEffect soundEffect = new SoundEffect();
            Object eventName = se.get("event_name");
            if (eventName != null) soundEffect.event_name = JsSemantics.toJsString(eventName);
            subpart.sound_effect = soundEffect;
        }
        Object expression = map.get("expression");
        if (expression instanceof String s) subpart.expression = s;
        return subpart;
    }
}
