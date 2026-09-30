package io.github.tt432.eyelib.snowstorm.texture;

import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.Emitter;
import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;
import io.github.tt432.eyelib.wintersky.three.Texture;
import io.github.tt432.eyelib.wintersky.three.TextureImage;
import io.github.tt432.eyelib.wintersky.tinycolor.TinyColor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Snowstorm v3.2.2 {@code texture_edit.js} 的 {@code TextureClass} 逐字移植
 * （原作者 JannisX11，GPL-3.0）。
 *
 * <p>canvas 2D → {@link RasterCanvas} 纯 int[] ABGR 栅格；brush/eraser（插值连线、shift 直线）、
 * flood fill、pickColor、createEmpty/save/reload、history undo/redo 栈语义全部 as-is。
 *
 * <p>偏离记录：
 * <ul>
 *   <li><b>R4 抗锯齿</b>：canvas brush 依赖浏览器抗锯齿，纯栅格下无对应，移植为硬边像素语义；
 *       brush 尺寸/插值步长规则保留（{@code r = 0} 硬编码，区域为 {@code (1+r)×(1+r)} 单像素）。</li>
 *   <li><b>source 抽象</b>（规格 §2-D5）：JS 的 PNG dataURL 串改为 codec 定义的不透明串，
 *       编解码走 {@link TextureSourceCodec} 接缝（PNG 留给 MC 侧）；未安装 codec 时
 *       {@link #canvasToDataURL()}/{@link #updateCanvasFromSource()}（非空 source）抛
 *       {@link IllegalStateException}（JS 无此状态，浏览器恒可编解码）。</li>
 *   <li><b>DOM 事件</b>：usePaintTool 的 pointermove/pointerup 监听移植为显式
 *       {@link PaintStroke} 对象（onMove/onEnd 由 UI 侧驱动）；模块级 keydown（Ctrl+Z/Y）
 *       与 window message 监听不移植，由 MC UI 切片接线到 {@link #undo()}/{@link #redo()}/
 *       {@link #save()}。</li>
 *   <li><b>save 二分统一</b>：JS 的 vscode postMessage 分支（带 internal_changes 判断）与浏览器
 *       IO.export 分支统一为 {@link SaveHandler} 接缝，安装即导出；文件对话框/剪贴板由 MC 侧实现。</li>
 *   <li><b>textureHexToArray</b>：JS 逐段子串 parseInt，非法 hex 得 NaN（写入 Uint8ClampedArray
 *       归 0）；移植用 {@link TinyColor}（契约指定），非法输入归一化为不透明黑。合法 #rrggbbaa
 *       输入两者逐值一致。</li>
 *   <li><b>line_start</b>：JS 模块级全局；单例等价下放为实例字段（reset 时置空，as-is）。</li>
 *   <li><b>Math.random</b> → {@link WinterskyRandom}（同 ADR-0034 先例）。</li>
 * </ul>
 */
public final class TextureClass {

    /** JS {@code export const Texture = new TextureClass()}。 */
    public static final TextureClass Texture = new TextureClass();

    // ---------------------------------------------------------------- JS 构造器字段

    /** JS {@code this.canvas}（16×16 起始）。 */
    public final RasterCanvas canvas = new RasterCanvas(16, 16);
    /** JS {@code this.source = ''}：codec 定义的不透明串，空串 = JS ''。 */
    public String source = "";
    /** JS {@code this.history = []}。 */
    public final List<HistoryEntry> history = new ArrayList<>();
    /** JS {@code this.history_index = 0}。 */
    public int history_index = 0;
    /** JS {@code this.internal_changes = false}。 */
    public boolean internal_changes = false;

    // ---------------------------------------------------------------- 接缝

    /** JS {@code this.name}（texture_edit.js 从未赋值；save 时回退 {@code 'Texture'}）。 */
    public @Nullable String name;
    /** source 串编解码接缝（PNG 在 MC 侧）。 */
    public @Nullable TextureSourceCodec sourceCodec;
    /** JS IO.export / vscode postMessage 的统一出口。 */
    public @Nullable SaveHandler saveHandler;

    // ---------------------------------------------------------------- 模块级/内部状态

    /** JS 模块级 {@code let line_start = null}：shift 直线起点，跨笔画保留。 */
    private int @Nullable [] line_start;
    /** JS {@code this.current_undo_entry}（beforeEdit/afterEdit 之间）。 */
    private @Nullable HistoryEntry current_undo_entry;
    /** JS 模块级 {@code main_config}。 */
    private @Nullable Config main_config;
    /** JS 模块级 {@code main_emitter}（texture_edit.js 只赋值不使用，as-is 保留）。 */
    private @Nullable Emitter main_emitter;
    /** JS {@code this.texture = config.texture}（linkEmitter）。 */
    private @Nullable Texture texture;
    /** JS {@code this.img = config.texture.image}（linkEmitter）。 */
    private @Nullable TextureImage img;

    // ---------------------------------------------------------------- JS 模块级函数

    /**
     * JS {@code textureHexToArray(hex)}：'#rrggbbaa' → [r, g, b, a]（0-255）。
     * 契约指定用 {@link TinyColor} 解析；合法 hex8 输入与 JS 逐值一致。
     */
    static int[] textureHexToArray(String hex) {
        TinyColor.Rgba rgb = new TinyColor(hex).toRgb();
        return new int[]{(int) rgb.r, (int) rgb.g, (int) rgb.b, (int) Math.round(rgb.a * 255)};
    }

    /** JS {@code getIdentifier()}：两个随机大写字母（fromCharCode 截断语义 = Java (char) 强转）。 */
    private static String getIdentifier() {
        return String.valueOf((char) (65 + WinterskyRandom.nextDouble() * 26))
                + (char) (65 + WinterskyRandom.nextDouble() * 26);
    }

    private TextureSourceCodec codec() {
        TextureSourceCodec c = this.sourceCodec;
        if (c == null) {
            throw new IllegalStateException("TextureSourceCodec 未安装（source 串编解码接缝在 MC 侧）");
        }
        return c;
    }

    // ---------------------------------------------------------------- linkEmitter / source 同步

    /** JS {@code linkEmitter(emitter, config)}。 */
    public void linkEmitter(Emitter emitter, Config config) {
        this.texture = config.texture;
        this.img = config.texture.image;
        main_config = config;
        main_emitter = emitter;
    }

    /** JS {@code canvasToDataURL()}：{@code this.source = this.canvas.toDataURL()}。 */
    public void canvasToDataURL() {
        this.source = codec().encode(canvas.pixels, canvas.width, canvas.height);
    }

    /**
     * JS {@code updateCanvasFromSource()}（async 移植为同步）：空 source → 重置 16×16
     * （JS 赋 canvas.width/height 会清空位图）；否则解码 source 替换整个位图，
     * 解码失败 canvas 不变（JS img.onerror → 未捕获 reject）。
     */
    public void updateCanvasFromSource() {
        if (this.source.isEmpty()) {
            canvas.resize(16, 16);
            return;
        }
        TextureSourceCodec.DecodedImage decoded = codec().decode(this.source);
        if (decoded == null) return;
        canvas.resize(decoded.width, decoded.height);
        canvas.replacePixels(decoded.abgr);
    }

    /** JS {@code update()}：{@code this.img.src = this.source}（未 linkEmitter 时 JS 抛 TypeError）。 */
    public void update() {
        Objects.requireNonNull(this.img, "linkEmitter 未调用").setSrc(this.source);
    }

    /** JS {@code reset()}。 */
    public void reset() {
        this.source = "";
        this.internal_changes = false;
        line_start = null;
        this.history.clear();
        this.history_index = 0;
        this.updateCanvasFromSource();
    }

    /** JS {@code reload()}（未 linkEmitter 时 JS 抛 TypeError）。 */
    public void reload() {
        this.internal_changes = false;
        Objects.requireNonNull(main_config, "linkEmitter 未调用").updateTexture();
        this.updateCanvasFromSource();
    }

    // ---------------------------------------------------------------- createEmpty / save

    /** JS {@code createEmpty()}：默认 16×16。 */
    public void createEmpty() {
        createEmpty(16, 16);
    }

    /** JS {@code createEmpty(width, height)}。 */
    public void createEmpty(int width, int height) {
        canvas.resize(width, height);
        syncTextureSizeInput(width, height);
        this.canvasToDataURL();
        this.internal_changes = true;
        this.update();
    }

    /**
     * JS {@code Data.texture.uv.inputs.size.value.splice(0, 2, width, height)}：
     * 原地改数组，不走 value setter（不触发 Config.set/updatePreview，quirk as-is）。
     */
    @SuppressWarnings("unchecked")
    private static void syncTextureSizeInput(int width, int height) {
        // JS 直接链式解引用，缺节点时 TypeError；Java 等价 NPE（as-is）
        InputStructure.Subject texture = Objects.requireNonNull(InputStructure.Data.get("texture"));
        InputStructure.Group uv = Objects.requireNonNull(texture.group("uv"));
        Input size_input = Objects.requireNonNull(uv.inputs.get("size"));
        List<Object> size = (List<Object>) Objects.requireNonNull(size_input.getValue());
        // splice(0, 2, w, h)：删头部至多两个元素，头部插入 w、h
        for (int i = 0; i < 2 && !size.isEmpty(); i++) {
            size.remove(0);
        }
        size.add(0, (double) height);
        size.add(0, (double) width);
    }

    /** JS {@code save()} 接缝出口：{@code IO.export({name, extensions, savetype, content})}。 */
    public interface SaveHandler {
        void save(String name, String extension, String source);
    }

    /**
     * JS {@code save()}：空 source 直接返回；否则经 {@link #saveHandler} 导出并
     * {@link #markAsSaved()}。JS 的 vscode/浏览器二分统一为单一接缝（见类级偏离记录）。
     */
    public void save() {
        if (this.source.isEmpty()) return;
        SaveHandler handler = this.saveHandler;
        if (handler != null) {
            // JS: this.name || 'Texture'
            handler.save(this.name != null && !this.name.isEmpty() ? this.name : "Texture",
                    "png", this.source);
        }
        this.markAsSaved();
    }

    /** JS {@code markAsSaved()}。 */
    public void markAsSaved() {
        this.internal_changes = false;
    }

    // ---------------------------------------------------------------- pickColor

    /**
     * JS {@code pickColor(event, context)}（event 未使用）：读 {@code context.position}
     * 处像素，返回 '#rrggbbaa'；越界按 getImageData 语义读透明黑（'#00000000'）。
     */
    public String pickColor(int x, int y) {
        int p = canvas.getPixel(x, y);
        StringBuilder string = new StringBuilder("#");
        appendHex(string, RasterCanvas.r(p));
        appendHex(string, RasterCanvas.g(p));
        appendHex(string, RasterCanvas.b(p));
        appendHex(string, RasterCanvas.a(p));
        return string.toString();
    }

    /** JS：{@code value.toString(16)}，长度 1 时补 '0'。 */
    private static void appendHex(StringBuilder sb, int value) {
        String s = Integer.toString(value, 16);
        if (s.length() == 1) sb.append('0');
        sb.append(s);
    }

    // ---------------------------------------------------------------- usePaintTool

    /** JS TextureInput.vue 传入的 context 对象（{@code wrapper} 为 DOM 引用，剔除）。 */
    public static final class PaintContext {
        /** JS {@code context.position}（Vue 侧 live 绑定，笔画中逐次更新）。 */
        public static final class Position {
            public int x;
            public int y;
        }

        public final Position position = new Position();
        /** JS {@code context.tool}：'brush' 走画笔分支，其余（'eraser'）走 clearRect 分支。 */
        public String tool = "brush";
        /** JS {@code context.color}：'#rrggbbaa'。 */
        public @Nullable String color;
    }

    /**
     * JS {@code usePaintTool(e1, context)}（e1 仅取 shiftKey）：开始一次 brush/eraser 笔画，
     * 立即在 {@code context.position} 落第一笔（JS 注册监听后立即 {@code onMove(e1)}）。
     * 返回的 {@link PaintStroke} 对应 JS 的 pointermove/pointerup 监听对，由 UI 侧驱动。
     */
    public PaintStroke usePaintTool(boolean shiftKey, PaintContext context) {
        this.beforeEdit();
        PaintStroke stroke = new PaintStroke(context,
                shiftKey && line_start != null ? line_start : null);
        stroke.onMove();
        return stroke;
    }

    /**
     * JS usePaintTool 闭包内的 onMove/onEnd 监听对。指针移动时先更新
     * {@link PaintContext#position} 再调 {@link #onMove()}（或直接调 {@link #onMove(int, int)}），
     * 指针抬起时调 {@link #onEnd()} 一次。
     */
    public final class PaintStroke {
        private final PaintContext context;
        /** JS 闭包 {@code color}（context.color 为 falsy 时 undefined；brush 分支解引用即抛，as-is）。 */
        private final int @Nullable [] color;
        /** JS 闭包 {@code last_coords}（{} 起点用 has_last=false 表示）。 */
        private int last_x;
        private int last_y;
        private boolean has_last;
        /** JS pointerup 后移除监听保证 onEnd 单次。 */
        private boolean ended;

        private PaintStroke(PaintContext context, int @Nullable [] lineStart) {
            this.context = context;
            this.color = context.color != null ? textureHexToArray(context.color) : null;
            if (lineStart != null) {
                // JS: if (e1.shiftKey && line_start) last_coords = {x: line_start[0], y: line_start[1]}
                this.last_x = lineStart[0];
                this.last_y = lineStart[1];
                this.has_last = true;
            }
        }

        /** JS {@code onMove(e2)}：从 {@code context.position} 读 live 坐标。 */
        public void onMove() {
            onMove(context.position.x, context.position.y);
        }

        /** 先更新 {@code context.position} 再走 JS onMove 逻辑（对应 Vue 的 live pixel_position）。 */
        public void onMove(int x, int y) {
            context.position.x = x;
            context.position.y = y;
            // JS: if (last_coords.x == coords.x && last_coords.y == coords.y) return;
            if (has_last && last_x == x && last_y == y) return;

            double diff_x = last_x - x;
            double diff_y = last_y - y;
            double length = Math.sqrt(Math.pow(diff_x, 2) + Math.pow(diff_y, 2));
            // JS: length > 1.5 && last_coords.x != undefined
            // （has_last=false 时 JS 中 length 为 NaN，NaN > 1.5 为 false，落 else）
            if (length > 1.5 && has_last) {
                double interval = 1;
                if (Math.abs(diff_x) > Math.abs(diff_y)) {
                    interval = Math.sqrt(Math.pow(diff_y / diff_x, 2) + 1);
                } else {
                    interval = Math.sqrt(Math.pow(diff_x / diff_y, 2) + 1);
                }
                for (double i = 0; i <= length; i += interval) {
                    int px = length != 0 ? (int) Math.round(last_x - diff_x / length * i) : x;
                    int py = length != 0 ? (int) Math.round(last_y - diff_y / length * i) : y;
                    paint(px, py);
                }
            } else {
                paint(x, y);
            }

            canvasToDataURL();
            internal_changes = true;
            update();

            last_x = x;
            last_y = y;
            has_last = true;
            line_start = new int[]{x, y};
        }

        /** JS 闭包 {@code paint(x, y, context)}：brush 覆写 / 否则 clearRect。 */
        private void paint(int x, int y) {
            double r = 0;
            if ("brush".equals(context.tool)) {
                // JS: getImageData 区域 → 全通道覆写 → putImageData 原处（越界裁剪）。
                // color 为 null 时此处 NPE，对应 JS color[0] TypeError（as-is）
                int[] c = Objects.requireNonNull(color);
                int packed = RasterCanvas.pack(c[0], c[1], c[2], c[3]);
                int origin_x = x - (int) Math.round(r);
                int origin_y = y - (int) Math.round(r);
                for (int dy = 0; dy < 1 + r; dy++) {
                    for (int dx = 0; dx < 1 + r; dx++) {
                        canvas.setPixel(origin_x + dx, origin_y + dy, packed);
                    }
                }
            } else {
                canvas.clearRect(x - (int) Math.round(r), y - (int) Math.round(r),
                        (int) (1 + r), (int) (1 + r));
            }
        }

        /** JS {@code onEnd(e2)}：{@code afterEdit()}（label 为 undefined）+ 移除监听。 */
        public void onEnd() {
            if (ended) return;
            ended = true;
            afterEdit(null);
        }
    }

    // ---------------------------------------------------------------- useFillTool

    /**
     * JS {@code useFillTool(event, context)}（event 未使用）：flood fill。
     * 递归 expand 语义 as-is（JS 同为递归，大画布同样爆栈）。
     */
    public void useFillTool(PaintContext context) {
        this.beforeEdit();

        int w = canvas.width;
        int h = canvas.height;
        // JS: let data = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height)
        int[] data = canvas.copyPixels();
        // JS: context.color 必非 null（Vue 恒传 paint_color.hex8）；否则 textureHexToArray 抛（as-is）
        int[] fill_color = textureHexToArray(Objects.requireNonNull(context.color));

        int px = context.position.x;
        int py = context.position.y;
        // JS matrix：{[x]: {[y]: true|false}}；0 未访问 / 1 match / 2 mismatch
        byte[] matrix = new byte[w * h];
        boolean start_in_bounds = px >= 0 && py >= 0 && px < w && py < h;
        // JS: OOB 起点 target_color = [undefined×4]，Array.equals 对任何颜色均 false → 不填充
        int target_color = start_in_bounds ? data[py * w + px] : 0;
        // JS: matrix = {[position.x]: {[position.y]: true}}（种子无条件标记 match）
        if (start_in_bounds) {
            matrix[py * w + px] = 1;
        }
        fillExpandFrom(px, py, w, h, data, matrix, target_color, start_in_bounds);

        // JS: 遍历 matrix 把 match 的像素写成 fill_color，putImageData(data, 0, 0)
        int packed = RasterCanvas.pack(fill_color[0], fill_color[1], fill_color[2], fill_color[3]);
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == 1) {
                data[i] = packed;
            }
        }
        canvas.replacePixels(data);

        this.canvasToDataURL();
        this.internal_changes = true;
        this.update();

        this.afterEdit(null);
    }

    /** JS 闭包 {@code expandFrom(x, y)}。 */
    private void fillExpandFrom(int x, int y, int w, int h, int[] data, byte[] matrix,
                                int target_color, boolean start_in_bounds) {
        fillExpandTo(x + 1, y, w, h, data, matrix, target_color, start_in_bounds);
        fillExpandTo(x - 1, y, w, h, data, matrix, target_color, start_in_bounds);
        fillExpandTo(x, y + 1, w, h, data, matrix, target_color, start_in_bounds);
        fillExpandTo(x, y - 1, w, h, data, matrix, target_color, start_in_bounds);
    }

    /** JS 闭包 {@code expandTo(x, y)}：已访问跳过 → 越界跳过 → 标记 match 并递归。 */
    private void fillExpandTo(int x, int y, int w, int h, int[] data, byte[] matrix,
                              int target_color, boolean start_in_bounds) {
        // JS: if (matrix[x] && matrix[x][y] != undefined) return;
        if (x >= 0 && y >= 0 && x < w && y < h && matrix[y * w + x] != 0) return;
        // JS: if (x < 0 || y < 0 || x >= width || y >= height) return;
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        // JS: let match = target_color.equals(color)（util.js 的 Array.prototype.equals，逐通道 ===）
        boolean match = start_in_bounds && data[y * w + x] == target_color;
        matrix[y * w + x] = (byte) (match ? 1 : 2);
        if (match) {
            fillExpandFrom(x, y, w, h, data, matrix, target_color, start_in_bounds);
        }
    }

    // ---------------------------------------------------------------- history（edits 栈语义 as-is）

    /** JS history 条目：{@code {id, label, data_before, data_after}}。 */
    public static final class HistoryEntry {
        public String id = "";
        /** JS {@code entry.label}（afterEdit 参数；paint/fill 路径恒 undefined）。 */
        public @Nullable String label;
        public String data_before = "";
        public String data_after = "";
    }

    /** JS {@code beforeEdit()}。 */
    public void beforeEdit() {
        HistoryEntry undo_entry = new HistoryEntry();
        undo_entry.id = getIdentifier();
        undo_entry.data_before = this.source;
        this.current_undo_entry = undo_entry;
    }

    /** JS {@code afterEdit()}（label undefined）。 */
    public void afterEdit() {
        afterEdit(null);
    }

    /** JS {@code afterEdit(label)}：截断 redo 尾后压栈，history_index 推到栈顶。 */
    public void afterEdit(@Nullable String label) {
        HistoryEntry entry = Objects.requireNonNull(this.current_undo_entry, "beforeEdit 未调用");
        entry.label = label;
        entry.data_after = this.source;

        // JS: if (this.history.length > this.history_index) this.history.length = this.history_index;
        while (this.history.size() > this.history_index) {
            this.history.remove(this.history.size() - 1);
        }
        this.history.add(entry);
        this.history_index = this.history.size();

        this.current_undo_entry = null;
    }

    /** JS {@code cancelEdit()}（texture_edit.js 内无调用方，公开 API as-is 保留）。 */
    public void cancelEdit() {
        this.source = Objects.requireNonNull(this.current_undo_entry, "beforeEdit 未调用").data_before;
        this.updateCanvasFromSource();
        this.current_undo_entry = null;
    }

    /** JS {@code undo()}：注意取的是 {@code before.data_after} 而非 data_before（as-is quirk）。 */
    public void undo() {
        HistoryEntry after = history_index - 1 >= 0 && history_index - 1 < history.size()
                ? history.get(history_index - 1) : null;
        HistoryEntry before = history_index - 2 >= 0 && history_index - 2 < history.size()
                ? history.get(history_index - 2) : null;
        if (after == null) return;
        this.history_index = Math.max(0, this.history_index - 1);

        this.source = before != null ? before.data_after : after.data_before;
        // JS: if (this.source) —— 空串 falsy
        this.internal_changes = !this.source.isEmpty();
        this.updateCanvasFromSource();
    }

    /**
     * JS {@code redo()}：空 data_after 走 {@code update()} 而非 updateCanvasFromSource
     * （canvas 保留旧像素的 quirk，as-is；正常路径 data_after 恒非空，不可达）。
     */
    public void redo() {
        if (this.history_index >= this.history.size()) return;
        HistoryEntry change = this.history.get(this.history_index);
        this.history_index += 1;

        this.source = change.data_after;
        if (!this.source.isEmpty()) {
            this.internal_changes = true;
            this.updateCanvasFromSource();
        } else {
            this.internal_changes = false;
            this.update();
        }
    }
}
