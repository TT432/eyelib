package io.github.tt432.eyelib.client.loader;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.bridge.client.loader.ResourceLoader;
import io.github.tt432.eyelib.bridge.client.loader.SimpleJsonWithSuffixResourceReloadListener;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModel;
import io.github.tt432.eyelib.importer.model.bbmodel.BbModelAnimations;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 加载 bbmodels 目录下 .bbmodel 文件的内嵌动画，转换坐标约定后注册
 * （动画 id = 资源命名空间 + "." + 动画名）。以低优先级暂存：同名 bedrock
 * 动画 json 覆盖内嵌版本，便于对个别动画手调覆盖。
 *
 * @author TT432
 */
@ResourceLoader
public class BbModelAnimationLoader extends SimpleJsonWithSuffixResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BbModelAnimationLoader.class);

    BbModelAnimationLoader() {
        super(new GsonBuilder().setLenient().create(), "bbmodels", "bbmodel");
    }

    @Override
    protected void applyJson(Map<String, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<String, BBModel> parsedModels =
                LoaderParsingOps.parseBySourceKey(pObject, BBModel.CODEC, LOGGER, "bbmodel animation");
        Map<String, BrAnimation> parsedAnimations = new HashMap<>();
        parsedModels.forEach((location, model) -> {
            if (model.animations().isEmpty()) {
                return;
            }
            String namespace = location.substring(0, location.indexOf(':'));
            Map<String, BrAnimationEntrySchema> entries = new LinkedHashMap<>();
            model.animations().forEach(animation -> {
                try {
                    entries.put(namespace + "." + animation.name(), BbModelAnimations.toEntrySchema(animation));
                } catch (Exception e) {
                    LOGGER.error("can't convert bbmodel animation {} in {}", animation.name(), location, e);
                }
            });
            if (!entries.isEmpty()) {
                parsedAnimations.put(location, BrAnimation.fromSchemaSet(new BrAnimationSet(entries)));
            }
        });
        AnimationAssetRegistry.stageAnimationsLowPriority("bbmodel-animation-loader", parsedAnimations);
    }
}
