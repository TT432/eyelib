package io.github.tt432.eyelib.importer.addon;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bedrock JSON-UI 资产注册表（随 BedrockAddonRuntimeBridge 整体替换，语义同
 * {@link SoundAssetRegistry#stageSounds}）。
 *
 * <p>每次 {@link #stageUiFiles} 自增 {@link #version()}（UiPreviewScreen 据此重建预览），
 * 并重新生成轻量诊断（{@code @} 继承目标缺失、未知 key 形态）；绑定/动画/collection 等
 * 运行时语义的「未求值」提示由预览屏在构建控件树时自行收集，不在此处。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UiAssetRegistry {
    private static Map<String, BrUiFile> files = Map.of();
    private static List<String> diagnostics = List.of();
    private static long version = 0;

    /** 整体替换（key = 包内相对路径，如 ui/hud_screen.json）。 */
    public static void stageUiFiles(Map<String, BrUiFile> uiFiles) {
        files = Map.copyOf(uiFiles);
        diagnostics = computeDiagnostics(files);
        version++;
    }

    /** 已加载 ui 文件视图：key = 包内相对路径。 */
    public static Map<String, BrUiFile> filesView() {
        return files;
    }

    /** 每次 stage +1；预览屏据此判断重建。 */
    public static long version() {
        return version;
    }

    /** stage 时生成的轻量诊断（@ 引用目标缺失、未知 key 形态）。 */
    public static List<String> diagnostics() {
        return diagnostics;
    }

    /**
     * 有效命名空间索引：namespace → 文件（同 ns 冲突后者覆盖先者，与 vanilla 包优先级一致）。
     */
    private static Map<String, Map.Entry<String, BrUiFile>> namespaceIndex(Map<String, BrUiFile> files) {
        Map<String, Map.Entry<String, BrUiFile>> index = new LinkedHashMap<>();
        files.forEach((path, file) ->
                index.put(file.effectiveNamespace(path), Map.entry(path, file)));
        return index;
    }

    private static List<String> computeDiagnostics(Map<String, BrUiFile> files) {
        List<String> result = new ArrayList<>();
        Map<String, Map.Entry<String, BrUiFile>> namespaces = namespaceIndex(files);
        files.forEach((path, file) -> {
            for (String key : file.elements().keySet()) {
                int at = key.indexOf('@');
                if (at < 0) {
                    continue;
                }
                String name = key.substring(0, at);
                String ref = key.substring(at + 1);
                if (name.isEmpty() || ref.isEmpty() || ref.indexOf('@') >= 0) {
                    result.add(path + ": 未知 key 形态 \"" + key + "\"（期望 name 或 name@ns.parent）");
                    continue;
                }
                int dot = ref.indexOf('.');
                if (dot == 0) {
                    result.add(path + ": 未知 key 形态 \"" + key + "\"（@ 后 ns 为空）");
                    continue;
                }
                if (dot < 0) {
                    // 无 ns 前缀视为同文件继承（宽松处理；Bedrock 官方写法总是带 ns）
                    if (!file.elements().containsKey(ref)
                            && file.elements().keySet().stream().noneMatch(k -> BrUiFile.namePart(k).equals(ref))) {
                        result.add(path + ": @ 引用目标 \"" + ref + "\" 在本文件命名空间中不存在（key=\"" + key + "\"）");
                    }
                    continue;
                }
                if (dot == ref.length() - 1) {
                    result.add(path + ": 未知 key 形态 \"" + key + "\"（@ 后不是 ns.parent 形态）");
                    continue;
                }
                String ns = ref.substring(0, dot);
                String parent = ref.substring(dot + 1);
                Map.Entry<String, BrUiFile> target = namespaces.get(ns);
                if (target == null) {
                    result.add(path + ": @ 引用命名空间 \"" + ns + "\" 未加载（key=\"" + key + "\"）");
                } else if (target.getValue().elements().keySet().stream()
                        .noneMatch(k -> BrUiFile.namePart(k).equals(parent))) {
                    result.add(path + ": @ 引用目标 \"" + ref + "\" 在命名空间 " + ns + " 中不存在（key=\"" + key + "\"）");
                }
            }
        });
        return List.copyOf(result);
    }
}
