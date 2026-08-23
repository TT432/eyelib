package io.github.tt432.eyelib.client.ui.preview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.client.nodegraph.preview.NodeAssetPreview;
import io.github.tt432.eyelib.importer.addon.BrUiFile;
import io.github.tt432.eyelib.importer.addon.UiAssetRegistry;
import io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UIScrollPanel;
import io.github.tt432.eyelib.util.PortResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Bedrock JSON-UI 静态预览屏：渲染 {@link UiAssetRegistry} 中已加载 ui 文件的控件树。
 *
 * <p>静态子集：panel / stack_panel / grid / label / image / button（按静态 panel 渲染，
 * 按下/悬停态不求值）；size/offset 支持 px 数字、{@code "%"} 百分比与简单四则表达式；
 * anchor_from/anchor_to 九点定位；image 纹理解析走 AddonTextureRegistry + 资源管理器双查惯例
 * （{@link NodeAssetPreview#hasUsableTexture}，路径无扩展名补 .png、小写）。
 *
 * <p>绑定（bindings/#文本）、factory/collection、动画（anims）、{@code $} 变量一律不求值，
 * 在屏内诊断区列出。每帧检查 {@link UiAssetRegistry#version()}，变化则重建控件树。
 *
 * @author TT432
 */
public final class UiPreviewScreen implements UIScreen {
    private static final int PAD = 8;
    private static final int TITLE_H = 16;
    private static final int DIAG_H = 64;
    private static final int ROW_H = 12;
    /** 继承链深度上限（防环）。 */
    private static final int MAX_INHERIT_DEPTH = 16;

    @Nullable
    private UIScreenContext ctx;

    private long seenVersion = -1;

    private List<String> fileKeys = List.of();
    private List<String> elementKeys = List.of();
    @Nullable
    private String selectedFile;
    @Nullable
    private String selectedElement;
    @Nullable
    private PreviewNode previewRoot;
    /** 屏内诊断：注册表诊断 + 构建期收集的未求值特性。 */
    private List<String> notes = List.of();

    @Nullable
    private ListPanel filePanel;
    @Nullable
    private ListPanel elementPanel;
    @Nullable
    private ListPanel diagPanel;

    public static UiPreviewScreen create() {
        return new UiPreviewScreen();
    }

    private int listWidth() {
        return ctx != null ? Math.min(220, Math.round(ctx.width() * 0.3F)) : 220;
    }

    private int canvasX() {
        return PAD + listWidth() + PAD;
    }

    @Override
    public void onInit(UIScreenContext ctx) {
        this.ctx = ctx;
        int listW = listWidth();
        int listsH = ctx.height() - TITLE_H - DIAG_H - PAD * 3;
        int half = listsH / 2;
        filePanel = new ListPanel(PAD, TITLE_H + PAD, listW, half, "ui 文件", this::onFilePicked);
        elementPanel = new ListPanel(PAD, TITLE_H + PAD + half + PAD, listW, listsH - half - PAD,
                "元素（命名空间内）", this::onElementPicked);
        diagPanel = new ListPanel(PAD, ctx.height() - DIAG_H, ctx.width() - PAD * 2, DIAG_H - PAD,
                "诊断（未求值特性按字面量/默认值渲染）", null);
        ctx.addWidget(filePanel);
        ctx.addWidget(elementPanel);
        ctx.addWidget(diagPanel);
        refresh();
    }

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (UiAssetRegistry.version() != seenVersion) {
            // addon 热重载后整体替换：重建文件/元素列表与控件树
            refresh();
        }
        String title = "UI 预览";
        if (selectedFile != null) {
            title += " — " + selectedFile + (selectedElement != null ? " :: " + selectedElement : "");
        }
        gfx.drawText(title, PAD, 4, 0xFFFFFFFF);
        if (ctx == null) {
            return;
        }
        int cx = canvasX();
        int cy = TITLE_H + PAD;
        int cw = ctx.width() - cx - PAD;
        int ch = ctx.height() - DIAG_H - PAD - cy;
        if (cw <= 0 || ch <= 0) {
            return;
        }
        gfx.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xFF555555);
        gfx.fill(cx, cy, cx + cw, cy + ch, 0xFF202020);
        if (previewRoot == null) {
            gfx.drawText(fileKeys.isEmpty()
                            ? "未加载 ui 资源：请先通过「监控资源文件夹」导入含 ui/ 的 addon"
                            : "在左侧选择要预览的元素",
                    cx + 4, cy + 4, 0xFFAAAAAA);
            return;
        }
        gfx.enableScissor(cx, cy, cw, ch);
        renderNode(gfx, previewRoot, cx, cy, cw, ch);
        gfx.disableScissor();
    }

    // ---- 选择与重建 ----

    private void onFilePicked(int index) {
        if (index >= 0 && index < fileKeys.size()) {
            selectedFile = fileKeys.get(index);
            selectedElement = null;
            refresh();
        }
    }

    private void onElementPicked(int index) {
        if (index >= 0 && index < elementKeys.size()) {
            selectedElement = elementKeys.get(index);
            refresh();
        }
    }

    private void refresh() {
        seenVersion = UiAssetRegistry.version();
        Map<String, BrUiFile> files = UiAssetRegistry.filesView();
        fileKeys = List.copyOf(files.keySet());
        if (selectedFile == null || !files.containsKey(selectedFile)) {
            selectedFile = fileKeys.isEmpty() ? null : fileKeys.get(0);
        }
        BrUiFile file = selectedFile != null ? files.get(selectedFile) : null;
        elementKeys = file != null ? List.copyOf(file.elements().keySet()) : List.of();
        if (selectedElement == null || !elementKeys.contains(selectedElement)) {
            selectedElement = elementKeys.isEmpty() ? null : elementKeys.get(0);
        }

        List<String> buildNotes = new ArrayList<>();
        previewRoot = null;
        if (file != null && selectedFile != null && selectedElement != null) {
            Map<String, BrUiFile> byNamespace = namespaceIndex(files);
            previewRoot = buildNode(selectedElement, file.elements().get(selectedElement),
                    file, byNamespace, buildNotes, 0);
        }

        List<String> all = new ArrayList<>(UiAssetRegistry.diagnostics());
        all.addAll(buildNotes);
        notes = List.copyOf(all);

        if (filePanel != null) {
            List<String> display = new ArrayList<>();
            for (String key : fileKeys) {
                BrUiFile f = files.get(key);
                display.add(key + "  [" + (f != null ? f.effectiveNamespace(key) : "?") + "]");
            }
            filePanel.setEntries(display, fileKeys.indexOf(selectedFile));
        }
        if (elementPanel != null) {
            elementPanel.setEntries(elementKeys, elementKeys.indexOf(selectedElement));
        }
        if (diagPanel != null) {
            diagPanel.setEntries(notes.isEmpty() ? List.of("（无诊断）") : notes, -1);
        }
    }

    /** namespace → 文件（同 ns 冲突后者覆盖先者，与 vanilla 包优先级一致）。 */
    private static Map<String, BrUiFile> namespaceIndex(Map<String, BrUiFile> files) {
        Map<String, BrUiFile> index = new LinkedHashMap<>();
        files.forEach((path, file) -> index.put(file.effectiveNamespace(path), file));
        return index;
    }

    // ---- 控件树构建（含继承展开与诊断收集） ----

    private static final class PreviewNode {
        final String name;
        final String type;
        final JsonObject props;
        final List<PreviewNode> children;

        PreviewNode(String name, String type, JsonObject props, List<PreviewNode> children) {
            this.name = name;
            this.type = type;
            this.props = props;
            this.children = children;
        }
    }

    private @Nullable PreviewNode buildNode(String key, JsonObject obj, BrUiFile file,
                                            Map<String, BrUiFile> byNamespace,
                                            List<String> buildNotes, int depth) {
        String name = BrUiFile.namePart(key);
        if (depth > MAX_INHERIT_DEPTH) {
            buildNotes.add("控件 " + name + ": 继承链过深或成环，截断");
            return new PreviewNode(name, "panel", new JsonObject(), List.of());
        }
        JsonObject merged = resolveMerged(key, obj, file, byNamespace, buildNotes, depth);
        String type = merged.has("type") && merged.get("type").isJsonPrimitive()
                ? merged.get("type").getAsString() : "panel";

        switch (type) {
            case "panel", "stack_panel", "grid", "label", "image", "button", "screen" -> {
            }
            default -> buildNotes.add("控件 " + name + ": 类型 " + type + " 不在静态子集，按 panel 渲染");
        }
        if ("button".equals(type)) {
            buildNotes.add("控件 " + name + ": button 按静态 panel 渲染（按下/悬停态不求值）");
        }
        if (merged.has("bindings")) {
            buildNotes.add("控件 " + name + ": bindings 未求值（静态预览取字面量/默认值）");
        }
        if (merged.has("anims")) {
            buildNotes.add("控件 " + name + ": 动画 anims 未求值");
        }
        if (merged.has("factory")) {
            buildNotes.add("控件 " + name + ": factory 未求值");
        }
        if (merged.has("collection_name")) {
            buildNotes.add("控件 " + name + ": collection 未求值");
        }
        scanExprNote(merged.get("size"), name, "size", buildNotes);
        scanExprNote(merged.get("offset"), name, "offset", buildNotes);
        JsonElement text = merged.get("text");
        if (text != null && text.isJsonPrimitive() && text.getAsString().startsWith("#")) {
            buildNotes.add("控件 " + name + ": text 为绑定引用 " + text.getAsString() + "，按原文显示");
        }
        JsonElement localize = merged.get("localize");
        if (localize != null && boolProp(merged, "localize")) {
            buildNotes.add("控件 " + name + ": localize 未求值，显示原文");
        }

        List<PreviewNode> children = new ArrayList<>();
        JsonObject controls = controlsToObject(merged.get("controls"));
        for (Map.Entry<String, JsonElement> child : controls.entrySet()) {
            if (!child.getValue().isJsonObject()) {
                continue;
            }
            PreviewNode node = buildNode(child.getKey(), child.getValue().getAsJsonObject(),
                    file, byNamespace, buildNotes, depth + 1);
            if (node != null) {
                children.add(node);
            }
        }
        return new PreviewNode(name, type, merged, List.copyOf(children));
    }

    /**
     * controls 归一化为 JsonObject：BE 规格是单键对象数组 [{name: {...}}]，
     * 宽松起见也接受直接的 JsonObject 形态。
     */
    private static JsonObject controlsToObject(@Nullable JsonElement controls) {
        JsonObject result = new JsonObject();
        if (controls == null) {
            return result;
        }
        if (controls.isJsonObject()) {
            return controls.getAsJsonObject();
        }
        if (controls.isJsonArray()) {
            for (JsonElement element : controls.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    element.getAsJsonObject().entrySet().forEach(e -> result.add(e.getKey(), e.getValue()));
                }
            }
        }
        return result;
    }

    /** 应用 key 的 {@code @ns.parent} 继承：base 属性垫底，自身覆盖（controls 递归合并，自身优先）。 */
    private JsonObject resolveMerged(String key, JsonObject obj, BrUiFile file,
                                     Map<String, BrUiFile> byNamespace,
                                     List<String> buildNotes, int depth) {
        JsonObject merged = new JsonObject();
        int at = key.indexOf('@');
        if (at >= 0 && depth <= MAX_INHERIT_DEPTH) {
            JsonObject base = resolveRef(key.substring(at + 1), file, byNamespace, buildNotes, depth + 1);
            if (base != null) {
                mergeInto(merged, base);
            }
        }
        mergeInto(merged, obj);
        return merged;
    }

    /** 解析继承目标为「已合并」的属性对象；失败返回 null 并记诊断。 */
    private @Nullable JsonObject resolveRef(String ref, BrUiFile file,
                                            Map<String, BrUiFile> byNamespace,
                                            List<String> buildNotes, int depth) {
        if (depth > MAX_INHERIT_DEPTH) {
            buildNotes.add("继承目标 " + ref + ": 继承链过深或成环，截断");
            return null;
        }
        int dot = ref.indexOf('.');
        String parentName;
        BrUiFile targetFile;
        if (dot > 0) {
            String ns = ref.substring(0, dot);
            parentName = ref.substring(dot + 1);
            targetFile = byNamespace.get(ns);
            if (targetFile == null) {
                buildNotes.add("继承目标 " + ref + ": 命名空间 " + ns + " 未加载");
                return null;
            }
        } else {
            // 无 ns 前缀视为同文件继承（宽松处理；官方写法总是带 ns）
            parentName = ref;
            targetFile = file;
        }
        for (Map.Entry<String, JsonObject> entry : targetFile.elements().entrySet()) {
            if (BrUiFile.namePart(entry.getKey()).equals(parentName)) {
                return resolveMerged(entry.getKey(), entry.getValue(), targetFile, byNamespace, buildNotes, depth);
            }
        }
        buildNotes.add("继承目标 " + ref + ": 元素不存在");
        return null;
    }

    private static void mergeInto(JsonObject dst, JsonObject src) {
        for (Map.Entry<String, JsonElement> entry : src.entrySet()) {
            JsonElement existing = dst.get(entry.getKey());
            if ("controls".equals(entry.getKey()) && existing != null) {
                // controls 逐子项合并（数组/对象两种形态先归一）：同名子控件自身覆盖 base
                JsonObject mergedControls = controlsToObject(existing);
                JsonObject incoming = controlsToObject(entry.getValue());
                for (Map.Entry<String, JsonElement> child : incoming.entrySet()) {
                    if (!mergedControls.has(child.getKey())) {
                        mergedControls.add(child.getKey(), child.getValue());
                    }
                }
                dst.add("controls", mergedControls);
            } else if (!dst.has(entry.getKey())) {
                dst.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void scanExprNote(@Nullable JsonElement expr, String name, String prop,
                                     List<String> buildNotes) {
        if (expr == null) {
            return;
        }
        List<JsonElement> leaves = new ArrayList<>();
        if (expr.isJsonArray()) {
            expr.getAsJsonArray().forEach(leaves::add);
        } else {
            leaves.add(expr);
        }
        for (JsonElement leaf : leaves) {
            if (!leaf.isJsonPrimitive() || !leaf.getAsJsonPrimitive().isString()) {
                continue;
            }
            String s = leaf.getAsString();
            if (s.contains("$") || s.contains("(")
                    || s.contains("%c") || s.contains("%x") || s.contains("%y")
                    || s.contains("%sm") || s.contains("%cm")) {
                buildNotes.add("控件 " + name + ": " + prop + " 表达式 \"" + s
                        + "\" 含未支持特性（$变量/函数/高级百分比），按近似值渲染");
            }
        }
    }

    // ---- 渲染 ----

    private void renderNode(UIGraphics gfx, PreviewNode node, int px, int py, int pw, int ph) {
        int[] rect = layoutRect(node, px, py, pw, ph);
        int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
        switch (node.type) {
            case "label" -> renderLabel(gfx, node, x, y, w, h);
            case "image" -> renderImage(gfx, node, x, y, w, h);
            case "stack_panel" -> renderStack(gfx, node, x, y, w, h);
            case "grid" -> renderGrid(gfx, node, x, y, w, h);
            default -> {
                // panel / screen / button（静态）及其他类型：仅渲染子控件
                for (PreviewNode child : node.children) {
                    renderNode(gfx, child, x, y, w, h);
                }
            }
        }
    }

    /** anchor 九点定位 + offset：子矩形 = 父矩形 anchor_to 点 - 自身 anchor_from 点 + offset。 */
    private int[] layoutRect(PreviewNode node, int px, int py, int pw, int ph) {
        float[] size = resolveSize(node, pw, ph);
        float w = size[0];
        float h = size[1];
        float[] offset = resolvePair(node.props.get("offset"), pw, ph);
        String anchorFrom = stringProp(node.props, "anchor_from", "top_left");
        String anchorTo = stringProp(node.props, "anchor_to", "top_left");
        float x = px + fracX(anchorTo) * pw - fracX(anchorFrom) * w + offset[0];
        float y = py + fracY(anchorTo) * ph - fracY(anchorFrom) * h + offset[1];
        return new int[]{Math.round(x), Math.round(y), Math.round(w), Math.round(h)};
    }

    private float[] resolveSize(PreviewNode node, int pw, int ph) {
        JsonElement size = node.props.get("size");
        float defaultW;
        float defaultH;
        switch (node.type) {
            case "label" -> {
                // label 缺省尺寸 = 文本尺寸（ctx 度量与渲染同字体）
                String text = stringProp(node.props, "text", "");
                defaultW = ctx != null ? ctx.textWidth(text) : 0;
                defaultH = ctx != null ? ctx.fontHeight() : 9;
            }
            case "image" -> {
                // image 缺省尺寸 = 纹理尺寸；纹理未知时回落父尺寸
                int[] tex = textureSize(node.props);
                defaultW = tex != null ? tex[0] : pw;
                defaultH = tex != null ? tex[1] : ph;
            }
            default -> {
                defaultW = pw;
                defaultH = ph;
            }
        }
        return resolvePair(size, pw, ph, defaultW, defaultH);
    }

    private float[] resolvePair(@Nullable JsonElement el, float parentW, float parentH) {
        return resolvePair(el, parentW, parentH, 0, 0);
    }

    /** 解析 [w, h] 对："fill"=父尺寸，"default"=调用方缺省，单值=两维同值。 */
    private float[] resolvePair(@Nullable JsonElement el, float parentW, float parentH,
                                float defaultW, float defaultH) {
        if (el == null) {
            return new float[]{defaultW, defaultH};
        }
        if (el.isJsonArray()) {
            var arr = el.getAsJsonArray();
            float w = arr.size() > 0 ? evalExpr(arr.get(0), parentW, defaultW) : defaultW;
            float h = arr.size() > 1 ? evalExpr(arr.get(1), parentH, defaultH) : defaultH;
            return new float[]{w, h};
        }
        float both = evalExpr(el, parentW, defaultW);
        return new float[]{both, both};
    }

    /** 尺寸/偏移表达式求值：px 数字、N%、简单 + - * / 四则；不支持特性回落缺省/0。 */
    private static float evalExpr(@Nullable JsonElement el, float parent, float fallback) {
        if (el == null) {
            return fallback;
        }
        if (el.isJsonPrimitive()) {
            var prim = el.getAsJsonPrimitive();
            if (prim.isNumber()) {
                return prim.getAsFloat();
            }
            if (prim.isString()) {
                return evalExprString(prim.getAsString(), parent, fallback);
            }
        }
        return fallback;
    }

    private static float evalExprString(String raw, float parent, float fallback) {
        String s = raw.strip();
        if (s.isEmpty()) {
            return fallback;
        }
        if ("fill".equals(s)) {
            return parent;
        }
        if ("default".equals(s)) {
            return fallback;
        }
        if (s.contains("$") || s.contains("(")) {
            // $ 变量 / 函数调用不求值
            return fallback;
        }
        // 简单四则：按 + - 切项（项内首字符的负号归项本身），项内按 * / 左结合
        float sum = 0;
        int sign = 1;
        int start = 0;
        boolean any = false;
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : '+';
            if ((c == '+' || c == '-') && i > start) {
                float term = evalTerm(s.substring(start, i), parent);
                if (Float.isNaN(term)) {
                    return fallback;
                }
                sum += sign * term;
                any = true;
                sign = c == '-' ? -1 : 1;
                start = i + 1;
            }
        }
        return any ? sum : fallback;
    }

    private static float evalTerm(String term, float parent) {
        float value = Float.NaN;
        char op = '*';
        int start = 0;
        for (int i = 0; i <= term.length(); i++) {
            char c = i < term.length() ? term.charAt(i) : '*';
            if (c == '*' || c == '/') {
                if (i == start) {
                    return Float.NaN; // 空因子（如结尾多余运算符）
                }
                float factor = evalFactor(term.substring(start, i).strip(), parent);
                if (Float.isNaN(factor)) {
                    return Float.NaN;
                }
                if (Float.isNaN(value)) {
                    value = factor;
                } else if (op == '*') {
                    value *= factor;
                } else {
                    value = factor == 0 ? 0 : value / factor;
                }
                op = c;
                start = i + 1;
            }
        }
        return value;
    }

    private static float evalFactor(String factor, float parent) {
        if (factor.isEmpty()) {
            return Float.NaN;
        }
        if (factor.endsWith("%")) {
            // 高级百分比（%c/%x/%y/%sm/%cm）不支持，按普通 % 前的数字部分失败处理
            try {
                return parent * Float.parseFloat(factor.substring(0, factor.length() - 1)) / 100F;
            } catch (NumberFormatException e) {
                return Float.NaN;
            }
        }
        try {
            return Float.parseFloat(factor);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    private void renderLabel(UIGraphics gfx, PreviewNode node, int x, int y, int w, int h) {
        String text = stringProp(node.props, "text", "");
        if (text.isEmpty()) {
            return;
        }
        int color = parseColor(node.props.get("color"));
        boolean shadow = boolProp(node.props, "shadow");
        if (shadow) {
            // MC 阴影色惯例：RGB >> 2，保留 alpha
            int shadowColor = (color & 0xFCFCFC) >> 2 | (color & 0xFF000000);
            gfx.drawText(text, x + 1, y + 1, shadowColor);
        }
        gfx.drawText(text, x, y, color);
    }

    private void renderImage(UIGraphics gfx, PreviewNode node, int x, int y, int w, int h) {
        String texture = stringProp(node.props, "texture", "");
        if (texture.isBlank() || w <= 0 || h <= 0) {
            return;
        }
        String trimmed = texture.strip();
        PortResourceLocation port = PortResourceLocation.parse(trimmed);
        String path = port.path().toLowerCase(Locale.ROOT);
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        ImportedImageData image = AddonTextureRegistry.get(path);
        // 双查惯例：addon 注册表优先，原版资源管理器兜底（NodeAssetPreview.hasUsableTexture）
        boolean usable = image != null || NodeAssetPreview.hasUsableTexture(trimmed);
        if (!usable) {
            gfx.fill(x, y, x + w, y + h, 0xFFF800F8);
            gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
            gfx.drawCenteredText("?", x + w / 2, y + Math.max(0, h / 2 - 4), 0xFFFFFFFF);
            return;
        }
        PortResourceLocation location = PortResourceLocation.of(port.namespace(), path);
        int texW = image != null ? image.width() : Math.max(1, w);
        int texH = image != null ? image.height() : Math.max(1, h);
        JsonElement uvSize = node.props.get("uv_size");
        if (image == null && uvSize != null && uvSize.isJsonArray() && uvSize.getAsJsonArray().size() >= 2
                && uvSize.getAsJsonArray().get(0).isJsonPrimitive() && uvSize.getAsJsonArray().get(1).isJsonPrimitive()
                && uvSize.getAsJsonArray().get(0).getAsJsonPrimitive().isNumber()
                && uvSize.getAsJsonArray().get(1).getAsJsonPrimitive().isNumber()) {
            texW = Math.max(1, uvSize.getAsJsonArray().get(0).getAsInt());
            texH = Math.max(1, uvSize.getAsJsonArray().get(1).getAsInt());
        }
        int nineslice = ninesliceBorder(node.props.get("nineslice_size"));
        if (nineslice > 0) {
            gfx.blitNineSlice(location, x, y, w, h, texW, texH, nineslice);
        } else {
            gfx.blitScaled(location, x, y, w, h, texW, texH);
        }
    }

    private void renderStack(UIGraphics gfx, PreviewNode node, int x, int y, int w, int h) {
        boolean horizontal = "horizontal".equals(stringProp(node.props, "orientation", "vertical"));
        int cursor = 0;
        for (PreviewNode child : node.children) {
            int[] rect = layoutRect(child, x + (horizontal ? cursor : 0), y + (horizontal ? 0 : cursor), w, h);
            renderNode(gfx, child, x + (horizontal ? cursor : 0), y + (horizontal ? 0 : cursor), w, h);
            cursor += horizontal ? rect[2] : rect[3];
        }
    }

    private void renderGrid(UIGraphics gfx, PreviewNode node, int x, int y, int w, int h) {
        int cols = 1;
        JsonElement dims = node.props.get("grid_dimensions");
        if (dims != null && dims.isJsonArray() && dims.getAsJsonArray().size() >= 2
                && dims.getAsJsonArray().get(0).isJsonPrimitive()
                && dims.getAsJsonArray().get(0).getAsJsonPrimitive().isNumber()) {
            cols = Math.max(1, dims.getAsJsonArray().get(0).getAsInt());
        }
        int cellW = Math.max(1, w / cols);
        int cellH = Math.max(1, h / Math.max(1, (int) Math.ceil(node.children.size() / (double) cols)));
        for (int i = 0; i < node.children.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            renderNode(gfx, node.children.get(i), x + col * cellW, y + row * cellH, cellW, cellH);
        }
    }

    // ---- 属性读取小工具 ----

    private static String stringProp(JsonObject props, String key, String fallback) {
        JsonElement el = props.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : fallback;
    }

    /** 容错布尔属性：接受 true/false 字面量与 "true" 字符串；其他形态视为 false。 */
    private static boolean boolProp(JsonObject props, String key) {
        JsonElement el = props.get(key);
        if (el == null || !el.isJsonPrimitive()) {
            return false;
        }
        var prim = el.getAsJsonPrimitive();
        if (prim.isBoolean()) {
            return prim.getAsBoolean();
        }
        return prim.isString() && Boolean.parseBoolean(prim.getAsString());
    }

    /** 九点定位 X 分量：left=0、right=1、middle/center=0.5（注意 right_middle 以 right 开头）。 */
    private static float fracX(String anchor) {
        if (anchor.endsWith("_right") || anchor.startsWith("right")) {
            return 1F;
        }
        if (anchor.endsWith("_left") || anchor.startsWith("left")) {
            return 0F;
        }
        return 0.5F;
    }

    /** 九点定位 Y 分量：top=0、bottom=1、middle/center=0.5。 */
    private static float fracY(String anchor) {
        if (anchor.startsWith("bottom")) {
            return 1F;
        }
        if (anchor.startsWith("top")) {
            return 0F;
        }
        return 0.5F;
    }

    /** label 颜色：[r,g,b(,a)] 0..1 浮点数组，或 "#RRGGBB"；缺省/非法 → 白色。 */
    private static int parseColor(@Nullable JsonElement el) {
        try {
            if (el == null) {
                return 0xFFFFFFFF;
            }
            if (el.isJsonArray()) {
                var arr = el.getAsJsonArray();
                if (arr.size() >= 3) {
                    int r = Math.round(arr.get(0).getAsFloat() * 255);
                    int g = Math.round(arr.get(1).getAsFloat() * 255);
                    int b = Math.round(arr.get(2).getAsFloat() * 255);
                    int a = arr.size() >= 4 ? Math.round(arr.get(3).getAsFloat() * 255) : 255;
                    return a << 24 | r << 16 | g << 8 | b;
                }
            }
            if (el.isJsonPrimitive()) {
                String s = el.getAsString();
                if (s.startsWith("#") && s.length() == 7) {
                    return 0xFF000000 | Integer.parseInt(s.substring(1), 16);
                }
            }
        } catch (RuntimeException ignored) {
            // 非法颜色按白色渲染（预览容错）
        }
        return 0xFFFFFFFF;
    }

    private static int ninesliceBorder(@Nullable JsonElement el) {
        try {
            if (el == null) {
                return 0;
            }
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return Math.max(0, el.getAsInt());
            }
            if (el.isJsonArray() && !el.getAsJsonArray().isEmpty()) {
                return Math.max(0, el.getAsJsonArray().get(0).getAsInt());
            }
        } catch (RuntimeException ignored) {
            // 非法 nineslice_size 按 0 处理
        }
        return 0;
    }

    /** image 纹理像素尺寸（仅 addon 注册表可知；vanilla 纹理无法预知，返回 null）。 */
    private static int @Nullable [] textureSize(JsonObject props) {
        JsonElement texture = props.get("texture");
        if (texture == null || !texture.isJsonPrimitive()) {
            return null;
        }
        PortResourceLocation port = PortResourceLocation.parse(texture.getAsString().strip());
        String path = port.path().toLowerCase(Locale.ROOT);
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        ImportedImageData image = AddonTextureRegistry.get(path);
        return image != null ? new int[]{image.width(), image.height()} : null;
    }

    // ---- 列表面板 ----

    /**
     * 只读/单选滚动列表：标题固定在顶部（overlay），条目可滚动；onPick 为 null 时纯展示。
     */
    private static final class ListPanel extends UIScrollPanel {
        private final String title;
        @Nullable
        private final IntConsumer onPick;
        private List<String> entries = List.of();
        private int selected = -1;

        ListPanel(int x, int y, int width, int height, String title, @Nullable IntConsumer onPick) {
            super(x, y, width, height);
            this.title = title;
            this.onPick = onPick;
        }

        void setEntries(List<String> entries, int selected) {
            this.entries = entries;
            this.selected = selected;
        }

        @Override
        protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            for (int i = 0; i < entries.size(); i++) {
                int rowY = y + ROW_H + i * ROW_H;
                if (i == selected) {
                    gfx.fill(x + 1, rowY, x + width - 1, rowY + ROW_H, 0xFF3A5F8A);
                } else if (mouseX >= x && mouseX < x + width
                        && mouseY >= rowY && mouseY < rowY + ROW_H) {
                    gfx.fill(x + 1, rowY, x + width - 1, rowY + ROW_H, 0xFF2A2A2A);
                }
                gfx.drawText(entries.get(i), x + 3, rowY + 2, 0xFFDDDDDD);
            }
        }

        @Override
        protected void renderOverlay(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            // 标题栏覆盖在滚动内容之上
            gfx.fill(x, y, x + width, y + ROW_H, 0xFF101010);
            gfx.drawText(title, x + 3, y + 2, 0xFFFFFFFF);
            gfx.fill(x, y + height - 1, x + width, y + height, 0xFF555555);
        }

        @Override
        public int getContentHeight() {
            return ROW_H * (entries.size() + 1);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (onPick == null || !contains(mouseX, mouseY)) {
                return false;
            }
            int row = (int) ((mouseY - y - ROW_H + scrollDistance) / ROW_H);
            if (row >= 0 && row < entries.size()) {
                selected = row;
                onPick.accept(row);
                return true;
            }
            return false;
        }
    }
}
