package io.github.tt432.eyelib.client.gui.snowstorm.io;
//? if >=1.20.1 {

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormEditorGate;
import io.github.tt432.eyelib.snowstorm.event.EventSubEffects;
import io.github.tt432.eyelib.snowstorm.io.SnowstormExport;
import io.github.tt432.eyelib.snowstorm.io.SnowstormImport;
import io.github.tt432.eyelib.snowstorm.texture.TextureClass;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.JsonValues;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 子效果堆叠编辑（ADR-0036 D6：对应 Snowstorm 浏览器版"新标签页编辑子效果"）。
 *
 * <p>Java 侧 EditorRuntime 为静态单例（无浏览器多标签隔离），堆叠编辑以状态栈实现：
 * <ol>
 *   <li>{@link #openSubEffect}（{@code EventSubEffects.edit_callback} 接缝实现）：
 *       快照当前（父）编辑器状态入栈 → {@link SnowstormEditorGate#openEditor()} 开新 Screen
 *       实例 → {@code SnowstormImport.loadFileFromParentEffect} 载入子效果；</li>
 *   <li>{@link #onEditorScreenClosed}（Screen onClose 接缝，Main 接线）：子编辑器当前状态
 *       回灌 {@code EventSubEffects} 注册表（父编辑器事件经
 *       {@code fetchParticleFile → EventSubEffects.get} 消费，闭环已在 domain 侧就位）→
 *       弹栈恢复父编辑器状态。</li>
 * </ol>
 *
 * <p>接缝（Main 集成接线）：{@code EventSubEffects.edit_callback = SubEffectEditorActions::openSubEffect}
 * （编辑器 Screen 打开时装配）；SnowstormEditorScreen 关闭时调 {@link #onEditorScreenClosed()}。</p>
 */
public final class SubEffectEditorActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubEffectEditorActions.class);
    private static final Gson GSON = new Gson();

    /** 编辑器状态快照（generateFile JSON 文本 + 纹理 source data URL）。 */
    private record EditorSnapshot(String json, @Nullable String texture, String subEffectIdentifier) {
    }

    /** 父编辑器状态栈（每层子效果编辑一层）。 */
    private static final Deque<EditorSnapshot> STACK = new ArrayDeque<>();

    private SubEffectEditorActions() {
    }

    /** 当前堆叠深度（0 = 顶层编辑器）。 */
    public static int stackDepth() {
        return STACK.size();
    }

    /** {@code EventSubEffects.edit_callback} 接缝实现：开子编辑器。 */
    public static void openSubEffect(String identifier) {
        if (!SnowstormEditorGate.isEditorAvailable()) {
            return;
        }
        try {
            EventSubEffects.Entry entry = EventSubEffects.getOrCreate(identifier);
            // 1) 父状态入栈
            STACK.push(new EditorSnapshot(
                    SnowstormUtil.compileJSON(SnowstormExport.generateFile()),
                    TextureClass.Texture.source,
                    identifier));
            // 2) 新 Screen 实例（JS window.open 新标签页）
            SnowstormEditorGate.openEditor();
            // 3) 载入子效果（JS 新 tab 的 window.loadFileFromParentEffect(raw_json, texture_url)）
            if (entry.json != null) {
                SnowstormImport.loadFileFromParentEffect(GSON.toJson(entry.json), entry.texture);
            }
            // entry.json 为 null（仅 '' quirk 条目）：子编辑器保持当前配置（JS 同样无文件可载）
        } catch (Exception | LinkageError e) {
            STACK.poll();
            LOGGER.warn("[snowstorm] open sub-effect editor failed: {}", identifier, e);
        }
    }

    /**
     * 编辑器 Screen 关闭接缝：非顶层（栈非空）时先回灌子效果注册表再恢复父编辑器；
     * 顶层关闭为 no-op（会话级注册表随编辑器弃置，I4 由 Screen 集成负责释放）。
     */
    public static void onEditorScreenClosed() {
        EditorSnapshot snapshot = STACK.poll();
        if (snapshot == null) {
            return;
        }
        try {
            // 1) 子效果回灌注册表（getSubEffectDataForParentEffect 的写侧；父事件消费链不变）
            EventSubEffects.Entry entry = EventSubEffects.getOrCreate(snapshot.subEffectIdentifier());
            entry.json = JsonValues.toJava(JsonParser.parseString(
                    SnowstormUtil.compileJSON(SnowstormExport.generateFile())));
            entry.texture = TextureClass.Texture.source;
            // 2) 恢复父编辑器（loadFileFromParentEffect 内部 loadFile(confirm=false)）
            SnowstormImport.loadFileFromParentEffect(snapshot.json(), snapshot.texture());
            // 恢复父编辑器后 Sidebar 重建（Vue 响应式替代，ADR R3）
            io.github.tt432.eyelib.client.gui.snowstorm.inputs.InputViewFactory.notifyChanged();
        } catch (Exception | LinkageError e) {
            LOGGER.warn("[snowstorm] close sub-effect editor failed: {}", snapshot.subEffectIdentifier(), e);
        }
    }
}
//?}
