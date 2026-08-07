package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.nodegraph.EmolangFunction;
import io.github.tt432.eyelib.nodegraph.EmolangParser;
import io.github.tt432.eyelib.nodegraph.EmolangRegistry;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * .emolang 自定义函数加载器（规格 nodegraph-emolang-functions §3）：扫描
 * {@code config/emolang/*.emolang}，逐个解析并整体替换 {@link EmolangRegistry}。
 * 客户端启动与节点图编辑器打开时调用（改文件后重开编辑器即生效，无需重启游戏）。
 *
 * @author TT432
 */
public final class EmolangLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmolangLoader.class);

    private EmolangLoader() {
    }

    /** 扫描并重载；返回成功加载的函数数。 */
    public static int reloadAll() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("emolang");
        List<EmolangFunction> loaded = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            List<Path> files;
            try (Stream<Path> stream = Files.list(dir)) {
                files = stream
                        .filter(p -> p.getFileName().toString().endsWith(".emolang"))
                        .sorted()
                        .toList();
            } catch (IOException e) {
                LOGGER.warn("[emolang] 扫描 {} 失败", dir, e);
                files = List.of();
            }
            for (Path file : files) {
                try {
                    EmolangParser.Result result = EmolangParser.parse(
                            file.getFileName().toString(), Files.readString(file));
                    if (result.function() != null) {
                        loaded.add(result.function());
                    } else {
                        LOGGER.warn("[emolang] 解析失败: {}", result.error());
                    }
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("[emolang] 读取 {} 失败", file, e);
                }
            }
        }
        EmolangRegistry.replaceAll(loaded);
        if (!loaded.isEmpty()) {
            LOGGER.info("[emolang] 已加载 {} 个自定义函数: {}", loaded.size(),
                    loaded.stream().map(EmolangFunction::name).sorted().toList());
        }
        return loaded.size();
    }
}
