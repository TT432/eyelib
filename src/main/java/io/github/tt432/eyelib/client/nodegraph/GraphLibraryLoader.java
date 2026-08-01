package io.github.tt432.eyelib.client.nodegraph;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.bridge.client.loader.ResourceLoader;
import io.github.tt432.eyelib.client.loader.BrResourcesLoader;
import io.github.tt432.eyelib.client.loader.LoaderParsingOps;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 加载 {@code eyelib/nodegraph/*.json} 图文档库（规格 §3.2 资源包管线）。
 *
 * @author TT432
 */
@ResourceLoader
public class GraphLibraryLoader extends BrResourcesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphLibraryLoader.class);

    GraphLibraryLoader() {
        super("nodegraph", "json");
    }

    @Override
    protected void applyJson(Map<String, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, GraphLibrary> parsed = LoaderParsingOps.parseAndTranslate(
                object,
                GraphLibrary.CODEC,
                (sourceLocation, library) -> sourceLocation,
                LOGGER,
                "nodegraph"
        );

        LinkedHashMap<String, GraphLibrary> flattened = new LinkedHashMap<>(parsed);
        GraphLibraryManager.INSTANCE.replaceAll(flattened);
    }
}
