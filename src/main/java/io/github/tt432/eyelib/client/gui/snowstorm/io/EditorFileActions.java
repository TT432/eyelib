package io.github.tt432.eyelib.client.gui.snowstorm.io;
//? if >=1.20.1 {

import com.google.gson.JsonParser;
import io.github.tt432.eyelib.client.gui.manager.io.FileDialogService;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.io.SnowstormExport;
import io.github.tt432.eyelib.snowstorm.io.SnowstormImport;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.JsonValues;
import net.minecraft.client.Minecraft;
//? if <1.20.6 {
import net.minecraftforge.fml.loading.FMLPaths;
//?} else {
import net.neoforged.fml.loading.FMLPaths;
//?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Snowstorm 导入/导出文件闭环（ADR-0036 D6；export.js downloadFile / import.js importFile 的
 * MC 侧等价——浏览器下载与 VSCode 通道剔除后的落盘实现）。
 *
 * <ul>
 *   <li>导出：{@code compileJSON(generateFile())} → 写 {@code <gamedir>/snowstorm_exports/
 *       <getName()>.particle.json} + 复制剪贴板（CodeViewerView 同款
 *       {@code keyboardHandler.setClipboard} 先例）。</li>
 *   <li>导入：{@link FileDialogService} 对话框或 Screen.onFilesDrop 拖拽 → Gson parse →
 *       {@code SnowstormImport.loadFile}（其内部已 {@code Emitter.stop(true)} +
 *       {@code PlaybackController.start()}，再补一次 restartEmitter 对齐 P6 约定）。</li>
 *   <li>I5：所有 File IO 失败仅告警日志，不抛穿 Screen。</li>
 * </ul>
 *
 * <p>接缝（Main 集成接线）：MenuBar 的 Export/Import 菜单项 → {@link #exportCurrent()} /
 * {@link #importViaDialog()}；SnowstormEditorScreen.onFilesDrop → {@link #importFromDroppedFiles(List)}。
 * 注意 {@code SnowstormImport.confirmClear} 默认拒绝（不清空当前工作），Screen 需安装确认对话框
 * 实现，否则 loadFile(confirm=true) 的导入会被取消（as-is JS confirm 接缝）。</p>
 */
public final class EditorFileActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorFileActions.class);

    private EditorFileActions() {
    }

    /** export.js downloadFile 的 MC 侧等价：序列化 + 落盘 + 剪贴板。 */
    public static void exportCurrent() {
        try {
            String content = SnowstormUtil.compileJSON(SnowstormExport.generateFile());
            Path dir = FMLPaths.GAMEDIR.get().resolve("snowstorm_exports");
            Files.createDirectories(dir);
            Path file = dir.resolve(SnowstormExport.getName() + ".particle.json");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            Minecraft.getInstance().keyboardHandler.setClipboard(content);
            LOGGER.info("[snowstorm] exported {} (also copied to clipboard)", file);
        } catch (Exception | LinkageError e) {
            LOGGER.warn("[snowstorm] export failed", e);
        }
    }

    /** import.js importFile 的 MC 侧等价：文件对话框选 .json。 */
    public static void importViaDialog() {
        FileDialogService.selectJsonFile("Import particle effect", null)
                .whenComplete((optional, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warn("[snowstorm] import dialog failed", throwable);
                        return;
                    }
                    optional.ifPresent(selected ->
                            // 对话框在未来线程完成；编辑器状态变更走 client 线程
                            Minecraft.getInstance().execute(() -> importFile(selected)));
                });
    }

    /** Screen.onFilesDrop(List&lt;Path&gt;) 拖拽导入通道（1.20.1 已验证存在，核对点 C3）。 */
    public static void importFromDroppedFiles(List<Path> files) {
        for (Path file : files) {
            if (file.getFileName().toString().endsWith(".json")) {
                importFile(file);
                return;
            }
        }
    }

    /** 读文件 → JSON.parse（Gson + JsonValues.toJava）→ loadFile → 重启预览。失败仅告警（I5）。 */
    private static void importFile(Path file) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) JsonValues.toJava(JsonParser.parseString(raw));
            SnowstormImport.loadFile(data);
            EditorRuntime.restartEmitter();
            // 导入后 Sidebar 重建（Vue 响应式替代，ADR R3）
            io.github.tt432.eyelib.client.gui.snowstorm.inputs.InputViewFactory.notifyChanged();
        } catch (Exception | LinkageError e) {
            // 导入 JSON 不合法 / IO 失败：WarningDialog 属 P5；此处告警不崩（I5）
            LOGGER.warn("[snowstorm] import failed: {}", file, e);
        }
    }
}
//?}
