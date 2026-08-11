package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * 节点图资产选择器候选供给（规格 nodegraph-eproject-variables §4.1/§4.2）：
 * {@code NodeOptionDef.suggestionKey} → 运行时注册表键集合。每次调用现查（注册表是 COW
 * 快照，不做缓存）；结果排序去重。下拉只是候选供给，不锁死自由输入（外部契约逃生舱）。
 *
 * <p>键形态对齐各消费方：
 * <ul>
 *   <li>{@code geometry}：{@link ModelManager} 的 Bedrock geometry 全名；</li>
 *   <li>{@code animation}/{@code ac}：{@link AnimationRegistries} 键按前缀
 *       {@code animation.} / {@code controller.animation.} 过滤；</li>
 *   <li>{@code rc}：{@link RenderControllerManager} 键；</li>
 *   <li>{@code material}：{@link MaterialManager} 条目的裸名（键 {@code name[:variant]}
 *       取冒号前，叠加条目的 {@code name} 字段，去重）；</li>
 *   <li>{@code texture}：{@link AddonTextureRegistry} 键（无命名空间相对路径，去
 *       {@code .png}）+ 原版/资源包 {@code textures/} 下全部 {@code .png}
 *       （去 {@code .png}；minecraft 命名空间省略前缀，其它命名空间保留
 *       {@code "ns:path"}——与 {@code NodeAssetPreview.resolveTexture} 经
 *       {@code PortResourceLocation.parse} 的解析期望对齐）。</li>
 * </ul>
 *
 * @author TT432
 */
public final class AssetSuggestions {
    private AssetSuggestions() {
    }

    /**
     * 候选缓存：{@code textures()} 的 listMatchingResources 是全包文件扫描，节点 UI 构建期每个
     * ref 字段都会调一次（编辑器打开耗时实测 51% 在建议供给，其中纹理扫描 40%）。
     * 缓存在编辑器重开（Ldlib2NodegraphEditor.open，同包调用）时失效——建议仅供编辑器下拉消费，
     * 编辑器外无消费者，故不需资源重载监听。
     */
    private static volatile @Nullable List<String> texturesCache;

    /** 编辑器重开时调用：丢弃候选缓存。 */
    public static void invalidateCaches() {
        texturesCache = null;
    }

    /** suggestionKey → 候选列表；未知 key → 空表。 */
    public static List<String> suggest(String key) {
        return switch (key) {
            case "geometry" -> sorted(ModelManager.INSTANCE.names());
            case "animation" -> animationsByPrefix("animation.");
            case "ac" -> animationsByPrefix("controller.animation.");
            case "rc" -> sorted(RenderControllerManager.INSTANCE.names());
            case "material" -> materials();
            case "texture" -> textures();
            case "molang.query" -> molangFunctions("query");
            case "molang.math" -> molangFunctions("math");
            case "molang.call" -> {
                // exec.call 可用 query/math 两侧函数（语句位调用）
                TreeSet<String> out = new TreeSet<>(molangFunctions("query"));
                out.addAll(molangFunctions("math"));
                yield List.copyOf(out);
            }
            default -> List.of();
        };
    }

    /**
     * molang 函数全名候选：编译器映射树该根下的全部函数 + 字段（零参 query 多为字段）
     * + 已注册的自定义函数（.emolang）。
     */
    private static List<String> molangFunctions(String root) {
        TreeSet<String> out = new TreeSet<>();
        var node = io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries
                .mappingTree().toplevelNode.children.get(root);
        if (node != null) {
            node.actualFunctions.keySet().forEach(name -> out.add(root + "." + name));
            node.cachedFields.keySet().forEach(name -> out.add(root + "." + name));
        }
        out.addAll(io.github.tt432.eyelib.nodegraph.MolangFunctionSignatures.customNames(root));
        return List.copyOf(out);
    }

    private static List<String> animationsByPrefix(String prefix) {
        TreeSet<String> out = new TreeSet<>();
        AnimationRegistries.animation().names().stream()
                .filter(name -> name.startsWith(prefix))
                .forEach(out::add);
        return List.copyOf(out);
    }

    private static List<String> materials() {
        TreeSet<String> out = new TreeSet<>();
        MaterialManager.INSTANCE.all().forEach((key, entry) -> {
            int colon = key.indexOf(':');
            out.add(colon >= 0 ? key.substring(0, colon) : key);
            if (entry.name() != null && !entry.name().isBlank()) {
                out.add(entry.name());
            }
        });
        return List.copyOf(out);
    }

    private static List<String> textures() {
        var cached = texturesCache;
        if (cached != null) {
            return cached;
        }
        TreeSet<String> out = new TreeSet<>();
        // addon 纹理（键已归一化：小写、.tga→.png，无命名空间）
        for (String path : AddonTextureRegistry.keys()) {
            out.add(stripPng(path));
        }
        // 原版/资源包纹理：FileToIdConverter 与资源重载监听器同款 API，三个版本编译稳定
        var converter = new FileToIdConverter("textures", ".png");
        for (var location : converter.listMatchingResources(Minecraft.getInstance().getResourceManager()).keySet()) {
            String stem = stripPng(location.getPath());
            // 无命名空间默认 minecraft（NodeAssetPreview 同约），故 minecraft 前缀省略
            out.add("minecraft".equals(location.getNamespace())
                    ? stem
                    : location.getNamespace() + ":" + stem);
        }
        var result = List.copyOf(out);
        texturesCache = result;
        return result;
    }

    private static String stripPng(String path) {
        return path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
    }

    private static List<String> sorted(Collection<String> names) {
        return List.copyOf(new TreeSet<>(names));
    }
}
