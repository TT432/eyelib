package io.github.tt432.eyelib.wintersky;

import io.github.tt432.eyelib.wintersky.three.Object3D;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * wintersky scene.js 逐字移植。
 *
 * <p>钩子（对应 JS options）：
 * <ul>
 *   <li>fetchTexture: (config) -&gt; url 字符串或 {@link java.util.concurrent.CompletionStage CompletionStage&lt;String&gt;}</li>
 *   <li>fetchParticleFile: (identifier, config) -&gt; json Map / {file_path, json} / CompletionStage</li>
 * </ul>
 */
public class Scene {

    /** JS constructor options 对象。字段缺省语义与 JS 完全一致（0/false 触发 {@code ||} 缺省值）。 */
    public static final class Options {
        public @Nullable Function<Config, Object> fetchTexture;
        public @Nullable BiFunction<String, Config, Object> fetchParticleFile;
        public int max_emitter_particles;
        public int tick_rate;
        public @Nullable String loop_mode;
        public @Nullable String parent_mode;
        /** JS {@code options.ground_collision != false}：null 视为未传入。 */
        public @Nullable Boolean ground_collision;
    }

    /** JS global_options 对象；scale 的 setter 副作用（同步所有 emitter 空间缩放）在 setScale 中复刻。 */
    public static final class GlobalOptions {
        private final Scene scene;

        public int max_emitter_particles;
        public int tick_rate;
        public String loop_mode;
        public String parent_mode;
        public boolean ground_collision;
        public double _scale = 1;

        GlobalOptions(Scene scene, Options options) {
            this.scene = scene;
            this.max_emitter_particles = options.max_emitter_particles != 0 ? options.max_emitter_particles : 30000;
            this.tick_rate = options.tick_rate != 0 ? options.tick_rate : 30;
            this.loop_mode = options.loop_mode != null && !options.loop_mode.isEmpty() ? options.loop_mode : "auto";
            this.parent_mode = options.parent_mode != null && !options.parent_mode.isEmpty() ? options.parent_mode : "world";
            this.ground_collision = options.ground_collision == null || options.ground_collision;
        }

        public double getScale() {
            return _scale;
        }

        public void setScale(double val) {
            this._scale = val;
            for (Emitter emitter : scene.emitters) {
                emitter.local_space.scale.set(val, val, val);
                emitter.global_space.scale.set(val, val, val);
            }
        }
    }

    public final List<Emitter> emitters = new ArrayList<>();
    public final Map<String, Config> child_configs = new LinkedHashMap<>();
    public final Object3D space = new Object3D();
    public final GlobalOptions global_options;

    private final @Nullable Function<Config, Object> _fetchTexture;
    private final @Nullable BiFunction<String, Config, Object> _fetchParticleFile;

    public Scene() {
        this(new Options());
    }

    public Scene(Options options) {
        this._fetchTexture = options.fetchTexture;
        this._fetchParticleFile = options.fetchParticleFile;
        this.global_options = new GlobalOptions(this, options);
    }

    /** JS fetchTexture：未设置钩子时返回 null（对应 undefined）。 */
    public @Nullable Object fetchTexture(Config config) {
        return _fetchTexture != null ? _fetchTexture.apply(config) : null;
    }

    /** JS fetchParticleFile：未设置钩子时返回 null。 */
    public @Nullable Object fetchParticleFile(String identifier, Config config) {
        return _fetchParticleFile != null ? _fetchParticleFile.apply(identifier, config) : null;
    }

    /** JS {@code this.scene._fetchParticleFile} 真值检查（emitter.js runEvent 使用）。 */
    boolean hasFetchParticleFile() {
        return _fetchParticleFile != null;
    }

    public void updateFacingRotation(Object3D camera) {
        for (Emitter emitter : emitters) {
            emitter.updateFacingRotation(camera);
        }
    }
}
