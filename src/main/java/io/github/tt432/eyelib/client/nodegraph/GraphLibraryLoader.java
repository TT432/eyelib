package io.github.tt432.eyelib.client.nodegraph;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.bridge.client.loader.ResourceLoader;
import io.github.tt432.eyelib.client.loader.BrResourcesLoader;
import io.github.tt432.eyelib.client.loader.LoaderParsingOps;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphMigrations;
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

        // format_version 1 → 2：变量节点化迁移（规格 nodegraph-eproject-variables §3.4）
        LinkedHashMap<String, GraphLibrary> flattened = new LinkedHashMap<>();
        parsed.forEach((key, library) -> flattened.put(key, GraphMigrations.migrate(library)));
        // 与 eproject 项目库并存（规格 §2.3）：replaceAll 是整表替换，
        // 需把 EprojectService 持有的项目库增量并入，否则资源重载会清掉项目库；
        // 键形态可区分（项目键 "name/libId" vs 资源包键 "namespace:path"），实践中不会碰撞
        EprojectService.projectLibraries().forEach(flattened::put);
        GraphLibraryManager.INSTANCE.replaceAll(flattened);
    }
}
