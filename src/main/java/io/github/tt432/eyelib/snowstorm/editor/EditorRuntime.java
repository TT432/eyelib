package io.github.tt432.eyelib.snowstorm.editor;

import io.github.tt432.eyelib.snowstorm.event.EventSubEffects;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.texture.TextureClass;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.Emitter;
import io.github.tt432.eyelib.wintersky.Scene;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.molang.MathUtil;
import io.github.tt432.eyelib.wintersky.molang.Molang;
import io.github.tt432.eyelib.wintersky.three.Object3D;
import io.github.tt432.eyelib.wintersky.three.TextureImage;
import io.github.tt432.eyelib.wintersky.three.Vector3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Snowstorm emitter.js 逐字移植：编辑器侧 wintersky 装配（Scene/Config/Emitter 单例 +
 * fetchTexture/fetchParticleFile 钩子 + molang 全局函数 + placeholder 变量兜底）。
 *
 * <p>JS 模块级 const（Scene/Config/Emitter/QuickSetup）映射为同名 public static 字段
 * （非 final：{@link #resetForTesting()} 需要整体重建，调用方不得把引用缓存进静态字段）。
 * JS 模块加载时的顶层语句按原顺序在 {@link #init()} 中执行。
 *
 * <p>偏离（vs JS）：
 * <ul>
 *   <li>vscode 分支剔除（fetchTexture/fetchParticleFile 的 postMessage/Promise 通道，
 *       ADR-0036 D6：VSCode 消息通道不适用）。</li>
 *   <li>JS 动态属性 {@code Config.unsupported_fields} 无处可挂（Java Config 静态类型）
 *       → 提升为 {@link #unsupported_fields}。</li>
 *   <li>Preview.vue 的 View（three.js 舞台）不属于数据层：{@code View.camera.position}
 *       → {@link #cameraPosition}（MC 舞台每帧写入）；{@code View.placeholder_variables}
 *       → {@link #placeholder_variables}；{@code initParticles(View)} →
 *       {@link #initParticles(Object3D)} 参数化。</li>
 *   <li>{@code window.Emitter} 浏览器全局导出 → 静态字段即全局。</li>
 *   <li>JS 无全局 updatePreview（per-Input 回调由 Input.updatePreview 承担）；
 *       {@link #updatePreview(Object)} 是设计稿 §D3 的全局钩子，行为 = 重启发射器
 *       （同 import.js {@code Emitter.stop(true)} + PlaybackController.start()）。</li>
 *   <li>{@code Texture.source = Config.texture.image.src}：src 为 null 时 JS 赋 undefined，
 *       Java 归一为 ""（两者均 falsy，下游行为等价）。</li>
 *   <li>{@code Texture.updateCanvasFromSource()} 要求 MC 桥已安装 TextureSourceCodec，
 *       否则非空 source 抛 IllegalStateException（texture 切片设计）。</li>
 * </ul>
 */
public final class EditorRuntime {

    private EditorRuntime() {
    }

    // ==================================================================
    // emitter.js 模块级 const（JS 同名）
    // ==================================================================

    /** emitter.js:8 {@code const Scene = new Wintersky.Scene({...})} */
    public static Scene Scene;

    /** emitter.js:73 {@code const Config = new Wintersky.Config(Scene)} */
    public static Config Config;

    /** emitter.js:74 {@code const Emitter = new Wintersky.Emitter(Scene, Config, {loop_mode:'auto', parent_mode:'world'})} */
    public static Emitter Emitter;

    /**
     * emitter.js:78 QuickSetup。{@code resetAll} 初始为 stub（"in case the tab was never opened"），
     * QuickSetup 标签页打开时由 UI 层替换（JS 为方法重赋值）。
     */
    public static final class QuickSetup {
        /** JS {@code QuickSetup.resetAll}：stub，UI 打开 QuickSetup 标签页后替换。 */
        public static Runnable resetAll = () -> {
            // stub in case the tab was never opened
        };
    }

    /**
     * emitter.js:85 {@code Config.unsupported_fields = {}}。JS 为 Config 实例上的动态属性；
     * Java Config 无此字段，提升为本类静态字段（偏离已记录）。import 流程（io 切片）读写此 map。
     */
    public static Map<String, Object> unsupported_fields = new LinkedHashMap<>();

    // ==================================================================
    // View（Preview.vue）接缝：UI 层不移植，状态由 MC 舞台喂入
    // ==================================================================

    /** JS {@code View.camera.position}：预览相机位置，MC 舞台每帧写入。molang 全局函数读取其 length()。 */
    public static final Vector3 cameraPosition = new Vector3();

    /** JS {@code View.placeholder_variables}：placeholder 变量兜底表（Preview.vue placeholder 栏填充）。 */
    public static final Map<String, Object> placeholder_variables = new LinkedHashMap<>();

    // ==================================================================
    // 模块加载顶层语句（emitter.js:8-153，原顺序）
    // ==================================================================

    static {
        init();
    }

    private static void init() {
        // emitter.js:8-72 const Scene = new Wintersky.Scene({fetchTexture, fetchParticleFile})
        Scene.Options sceneOptions = new Scene.Options();
        sceneOptions.fetchTexture = EditorRuntime::fetchTexture;
        sceneOptions.fetchParticleFile = EditorRuntime::fetchParticleFile;
        Scene = new Scene(sceneOptions);

        // emitter.js:73 const Config = new Wintersky.Config(Scene)
        Config = new Config(Scene);

        // emitter.js:74-77 const Emitter = new Wintersky.Emitter(Scene, Config, {loop_mode:'auto', parent_mode:'world'})
        Emitter.Options emitterOptions = new Emitter.Options();
        emitterOptions.loop_mode = "auto";
        emitterOptions.parent_mode = "world";
        Emitter = new Emitter(Scene, Config, emitterOptions);

        // emitter.js:78-82 QuickSetup stub
        QuickSetup.resetAll = () -> {
            // stub in case the tab was never opened
        };

        // emitter.js:83 window.Emitter = Emitter —— 浏览器全局导出，静态字段即全局（偏离已记录）

        // emitter.js:84 Texture.linkEmitter(Emitter, Config)
        TextureClass.Texture.linkEmitter(Emitter, Config);

        // emitter.js:85 Config.unsupported_fields = {} —— 提升为本类字段（偏离已记录）
        unsupported_fields = new LinkedHashMap<>();

        // emitter.js:87 Config.reset()
        Config.reset();

        // emitter.js:92-106 Config.onTextureUpdate
        Config.onTextureUpdate = EditorRuntime::onTextureUpdate;

        // emitter.js:108-145 Emitter.Molang.global_variables
        installGlobalVariables();

        // emitter.js:147-153 variableHandler 兜底包装
        Molang.VariableHandler original_variable_handler = Emitter.Molang.variableHandler;
        Emitter.Molang.variableHandler = (key, params, args) -> {
            Object computed_value = original_variable_handler != null
                    ? original_variable_handler.handle(key, params, args) : null;
            if (computed_value != null) return computed_value;
            return placeholder_variables.get(key);
        };

        // event_sub_effects.js 的 `import {Scene} from './emitter'` 单例接缝
        EventSubEffects.scene = Scene;
    }

    /** 测试接缝：整体重建 Scene/Config/Emitter 单例并复位 View 接缝状态（等价于 JS 重新加载模块）。 */
    public static void resetForTesting() {
        init();
        placeholder_variables.clear();
        cameraPosition.set(0, 0, 0);
    }

    // ==================================================================
    // emitter.js:9-45 fetchTexture(config)
    // ==================================================================

    private static @Nullable Object fetchTexture(Config config) {
        boolean is_main_effect = config == Config;
        if (!is_main_effect) {
            EventSubEffects.Entry sub = EventSubEffects.get(config.identifier);
            // JS truthy 检查：EventSubEffects[id] && EventSubEffects[id].texture（空串为 falsy）
            if (sub != null && sub.texture != null && !sub.texture.isEmpty()) {
                return sub.texture;
            }
        }
        if (is_main_effect && TextureClass.Texture.internal_changes) {
            return TextureClass.Texture.source;
        }
        // JS: if (!window.Data) return —— 守卫 Data 模块未加载（或类初始化重入时 Data 仍为 null）
        if (InputStructure.Data == null) return null;
        // vscode && path 分支剔除（ADR-0036 D6，偏离已记录）
        if (is_main_effect) {
            Input.ImageData image = imageInputImage();
            if (image != null && image.loaded) {
                return image.data;
            }
        }
        return null;
    }

    /**
     * JS {@code window.Data.texture.texture.inputs.image.image} 访问路径（map 风格）。
     * JS 不防御中间层（Data 加载后结构固定）；Java 侧为满足 nullness 做空守卫，行为不变。
     */
    private static Input.@Nullable ImageData imageInputImage() {
        InputStructure.Subject subject = InputStructure.Data.get("texture");
        InputStructure.Group group = subject != null ? subject.group("texture") : null;
        Input imageInput = group != null ? group.inputs.get("image") : null;
        return imageInput != null ? imageInput.image : null;
    }

    // ==================================================================
    // emitter.js:46-71 fetchParticleFile(identifier)
    // ==================================================================

    private static @Nullable Object fetchParticleFile(String identifier, Config config) {
        // JS: if (!identifier) return —— '' 同为 falsy
        if (identifier.isEmpty()) return null;
        // vscode 分支剔除（ADR-0036 D6，偏离已记录）；保留 else 分支
        EventSubEffects.Entry sub = EventSubEffects.get(identifier);
        // JS: EventSubEffects[identifier]?.json || null
        return sub != null && sub.json != null ? sub.json : null;
    }

    // ==================================================================
    // emitter.js:89-91 initParticles(View)
    // ==================================================================

    /** emitter.js initParticles(View)：{@code View.scene.add(Scene.space)}。View 舞台为 MC 侧接缝，以参数传入场景根。 */
    public static void initParticles(Object3D viewScene) {
        viewScene.add(Scene.space);
    }

    // ==================================================================
    // emitter.js:92-106 Config.onTextureUpdate
    // ==================================================================

    private static void onTextureUpdate() {
        // JS: if (!window.Data) return
        if (InputStructure.Data == null) return;

        if (!TextureClass.Texture.internal_changes) {
            if ("placeholder".equals(Config.texture_source_category)) {
                TextureClass.Texture.source = "";
            } else {
                TextureImage image = Config.texture.image;
                String src = image != null ? image.getSrc() : null;
                // JS 此处会赋 undefined；'' 与 undefined 均 falsy，下游等价（偏离已记录）
                TextureClass.Texture.source = src != null ? src : "";
            }
            TextureClass.Texture.updateCanvasFromSource();
        }

        Input.ImageData image = imageInputImage();
        if (image != null) {
            image.hidden = true;
            image.hidden = false;
        }
    }

    // ==================================================================
    // emitter.js:108-145 Emitter.Molang.global_variables
    // ==================================================================

    private static void installGlobalVariables() {
        // JS getter 'query.distance_from_camera'：返回 View.camera.position.length()
        Emitter.Molang.global_variables.put("query.distance_from_camera",
                (Molang.ContextFunction) args -> cameraPosition.length());

        Emitter.Molang.global_variables.put("query.lod_index",
                (Molang.ContextFunction) EditorRuntime::lodIndex);

        Emitter.Molang.global_variables.put("query.camera_distance_range_lerp",
                (Molang.ContextFunction) EditorRuntime::cameraDistanceRangeLerp);
    }

    /** emitter.js:122-130 'query.lod_index'(indices)。 */
    private static double lodIndex(Object[] args) {
        // JS indices.sort((a, b) => a - b) 原地排序；molang args 为每次调用新建数组，复制排序无可观察差异
        double[] indices = new double[args.length];
        for (int i = 0; i < args.length; i++) {
            indices[i] = JsSemantics.toNumber(args[i]);
        }
        Arrays.sort(indices);
        double distance = cameraPosition.length();
        double index = indices.length;
        // JS forEachReverse：从末尾向开头遍历
        for (int i = indices.length - 1; i >= 0; i--) {
            if (distance < indices[i]) index = i;
        }
        return index;
    }

    /** emitter.js:131-144 'query.camera_distance_range_lerp'(a, b)。 */
    private static double cameraDistanceRangeLerp(Object[] args) {
        // JS 缺参时实参为 undefined → 算术得 NaN；此处以 NaN 复刻
        double a = args.length > 0 ? JsSemantics.toNumber(args[0]) : Double.NaN;
        double b = args.length > 1 ? JsSemantics.toNumber(args[1]) : Double.NaN;
        double distance = cameraPosition.length();
        // Prevent division by zero
        double denominator = b - a;
        if (Math.abs(denominator) < 1e-8) {
            return distance < a ? 0 : 1;
        }
        // Interpolation of x between 0 and 1 in range [a, b]: (x-a)/(b-a)
        // JS Math.clamp（Blockbench 扩展）= clamp(x, 0, 1)
        return MathUtil.clamp((distance - a) / denominator, 0, 1);
    }

    // ==================================================================
    // 编辑器预览生命周期（设计稿 §D3 数据流 + Preview.vue PlaybackController as-is）
    // ==================================================================

    /**
     * 设计稿 §D3 updatePreview：Input 变更 → Config 已回写（Emitter.config 共享引用，下一 tick 生效）
     * → 重启发射器使变更立即可见（同 import.js:97-98 {@code Emitter.stop(true); PlaybackController.start()}）。
     */
    public static void updatePreview() {
        updatePreview(null);
    }

    /** 同 {@link #updatePreview()}；value 对应 JS per-Input {@code updatePreview(this.value)} 的实参（全局钩子不使用）。 */
    public static void updatePreview(@Nullable Object value) {
        restartEmitter();
    }

    /** 重启预览发射器：{@code View.PlaybackController.stop().start()}（Preview.vue startAnimation）。 */
    public static void restartEmitter() {
        PlaybackController.stop().start();
    }

    /**
     * Preview.vue PlaybackController 逐字移植。JS 方法返回 View.PlaybackController 以链式调用，
     * Java 以单例实例方法复刻（{@code PlaybackController.stop().start()} 同 JS 写法）。
     */
    public static final PlaybackController PlaybackController = new PlaybackController();

    /** Preview.vue:98-118 PlaybackController。 */
    public static final class PlaybackController {

        private PlaybackController() {
        }

        public PlaybackController start() {
            if (!Emitter.initialized || Emitter.age == 0) {
                Emitter.start();
            }
            Emitter.paused = false;
            return this;
        }

        public PlaybackController toggle() {
            Emitter.paused = !Emitter.paused;
            if (!Emitter.paused) {
                start();
            }
            return this;
        }

        public PlaybackController stop() {
            Emitter.stop(true);
            Emitter.paused = true;
            return this;
        }
    }

    /** Preview.vue:172-174 startAnimation（播放按钮/空格键）。 */
    public static void startAnimation() {
        PlaybackController.stop().start();
    }

    /** Preview.vue:175-177 togglePause（暂停按钮）。 */
    public static void togglePause() {
        PlaybackController.toggle();
    }
}
