package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.eproject.Eproject;
import io.github.tt432.eyelib.nodegraph.eproject.EprojectException;
import io.github.tt432.eyelib.nodegraph.eproject.EprojectIo;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * eproject 项目服务（规格 nodegraph-eproject-variables §2.3 加载管线）：
 * 扫描 {@code config/eyelib/} 下的项目（目录含 {@code project.json} = 文件夹形态，
 * {@code *.eproject} = 单文件 zip 形态），把项目库注册进 {@link GraphLibraryManager}，
 * 键 = {@code "{projectName}/{libId}"}（与资源包键 {@code "namespace:path"} 形态可区分、不会碰撞），
 * 并维护「库键 → 项目」绑定供 Ctrl+S 写回。
 *
 * <p>与资源包管线（{@link GraphLibraryLoader}）并存：loader 的
 * {@code replaceAll} 会整表替换，故其在应用资源包库时经
 * {@link #projectLibraries()} 把项目库增量并入（重载后项目库不丢失）。
 *
 * <p>写回形态：{@link EprojectIo#write} 按目标已存在的形态落盘（目录 → 文件夹形态，
 * 文件 → zip），因此 {@link #saveProjectOf} 天然按来源形态写回。
 *
 * @author TT432
 */
public final class EprojectService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EprojectService.class);

    /** 库键 → 所属项目绑定。 */
    private static final Map<String, ProjectRef> BINDINGS = new ConcurrentHashMap<>();
    /** 已加载/新建的项目（注册顺序）。 */
    private static final List<ProjectRef> PROJECTS = new CopyOnWriteArrayList<>();

    private EprojectService() {
    }

    /**
     * eproject 项目引用。
     *
     * @param name          项目名（project.json 的 name；库键前缀）
     * @param path          项目路径（目录形态 = 项目目录；单文件形态 = .eproject 文件）
     * @param directoryForm true = 文件夹形态；false = 单文件 zip 形态
     * @param libraryKeys   项目内全部库在 {@link GraphLibraryManager} 中的键（{@code name/libId}）
     */
    public record ProjectRef(String name, java.nio.file.Path path, boolean directoryForm,
                             java.util.List<String> libraryKeys) {
        public ProjectRef {
            libraryKeys = List.copyOf(libraryKeys);
        }
    }

    /**
     * 扫描 {@code config/eyelib/} 并加载全部项目（客户端初始化时调用一次，见
     * {@code ClientBootstrap#wire}）。单个项目读取失败只记日志跳过，不影响其它项目。
     *
     * <p>重复调用会重建绑定表，但 {@link GraphLibraryManager} 无按键删除能力，
     * 已被磁盘删除的项目的旧库键会残留到下一次资源重载（loader 并入时按新绑定表收敛）。
     */
    public static void loadAll() {
        BINDINGS.clear();
        PROJECTS.clear();
        Path root = configRoot();
        if (!Files.isDirectory(root)) {
            return;
        }
        List<Path> children;
        try (Stream<Path> stream = Files.list(root)) {
            children = stream.sorted().toList();
        } catch (IOException e) {
            LOGGER.error("[nodegraph] 扫描 eproject 目录失败: {}", root, e);
            return;
        }
        for (Path child : children) {
            boolean directoryForm = EprojectIo.isEprojectDir(child);
            if (!directoryForm && !EprojectIo.isEprojectFile(child)) {
                continue;
            }
            try {
                registerProject(EprojectIo.read(child), child, directoryForm);
            } catch (EprojectException e) {
                LOGGER.error("[nodegraph] eproject 读取失败，已跳过: {}", child, e);
            }
        }
    }

    /** 已加载/新建的全部项目（快照）。 */
    public static List<ProjectRef> projects() {
        return List.copyOf(PROJECTS);
    }

    /** 库键所属的项目；无绑定 = 资源包库或内存导入库。 */
    public static Optional<ProjectRef> projectOf(String libraryKey) {
        return Optional.ofNullable(BINDINGS.get(libraryKey));
    }

    /**
     * 把项目内全部库按来源形态写回（库内容取 {@link GraphLibraryManager} 当前值）。
     *
     * @return true = 写回成功；false = 库键无项目绑定或写回失败（失败已记日志）
     */
    public static boolean saveProjectOf(String libraryKey) {
        ProjectRef ref = BINDINGS.get(libraryKey);
        if (ref == null) {
            return false;
        }
        Map<String, GraphLibrary> libraries = new LinkedHashMap<>();
        for (String key : ref.libraryKeys()) {
            GraphLibrary library = GraphLibraryManager.INSTANCE.get(key);
            if (library == null) {
                LOGGER.warn("[nodegraph] 项目 '{}' 的库 '{}' 不在注册表中，跳过写回", ref.name(), key);
                continue;
            }
            libraries.put(key.substring(ref.name().length() + 1), library);
        }
        try {
            EprojectIo.write(ref.path(), Eproject.of(ref.name(), libraries));
            return true;
        } catch (EprojectException e) {
            LOGGER.error("[nodegraph] 项目 '{}' 写回失败", ref.name(), e);
            return false;
        }
    }

    /**
     * 把不来自任何项目的库另存为新的文件夹形态项目（规格 §2.3）：
     * 在 {@code config/eyelib/<name>/} 下建目录形态项目，名字冲突自动加 {@code -2}/{@code -3} 后缀；
     * 库以新键 {@code "<name>/<libId>"}（libId = 原库键末段）注册进 {@link GraphLibraryManager}
     * 并记录绑定。原库键保持不动（资源包管线只读源不变），调用方应切换到返回引用的库键。
     *
     * @param libraryKey  当前库键（必须在 {@link GraphLibraryManager} 中已注册）
     * @param projectName 期望项目名（一般取库键末段；非法字符替换为 {@code _}）
     * @return 新项目的引用（{@link ProjectRef#libraryKeys()} 单元素 = 新库键）
     * @throws IllegalArgumentException 库键未注册
     * @throws EprojectException        建目录或写盘失败
     */
    public static ProjectRef saveAsProject(String libraryKey, String projectName) {
        GraphLibrary library = GraphLibraryManager.INSTANCE.get(libraryKey);
        if (library == null) {
            throw new IllegalArgumentException("[nodegraph] 未知库键 '" + libraryKey + "'，无法另存为项目");
        }
        Path root = configRoot();
        String baseName = projectName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (baseName.isEmpty()) {
            baseName = "project";
        }
        String name = baseName;
        Path dir = root.resolve(name);
        for (int i = 2; Files.exists(dir); i++) {
            name = baseName + "-" + i;
            dir = root.resolve(name);
        }
        String libId = libIdOf(libraryKey);
        try {
            // 先建目录：EprojectIo.write 按「目标已存在且为目录」落文件夹形态
            Files.createDirectories(dir);
            EprojectIo.write(dir, Eproject.of(name, Map.of(libId, library)));
        } catch (IOException e) {
            throw new EprojectException(dir, "创建项目目录失败: " + e.getMessage(), e);
        }
        String newKey = name + "/" + libId;
        ProjectRef ref = new ProjectRef(name, dir, true, List.of(newKey));
        BINDINGS.put(newKey, ref);
        PROJECTS.add(ref);
        GraphLibraryManager.INSTANCE.put(newKey, library);
        LOGGER.info("[nodegraph] 库 '{}' 已另存为项目 '{}'（键 '{}'）", libraryKey, name, newKey);
        return ref;
    }

    /**
     * 项目库当前内容快照（键 = {@code name/libId}），供 {@link GraphLibraryLoader}
     * 在资源重载应用资源包库时增量并入——直接读 {@link GraphLibraryManager} 现值，
     * 编辑器已同步进注册表的未保存修改也能在重载后保留。
     */
    static Map<String, GraphLibrary> projectLibraries() {
        Map<String, GraphLibrary> out = new LinkedHashMap<>();
        BINDINGS.forEach((key, ref) -> {
            GraphLibrary library = GraphLibraryManager.INSTANCE.get(key);
            if (library != null) {
                out.put(key, library);
            }
        });
        return out;
    }

    private static void registerProject(Eproject project, Path path, boolean directoryForm) {
        List<String> keys = new ArrayList<>();
        Map<String, GraphLibrary> batch = new LinkedHashMap<>();
        project.libraries().forEach((libId, library) -> {
            String key = project.name() + "/" + libId;
            keys.add(key);
            batch.put(key, library);
        });
        ProjectRef ref = new ProjectRef(project.name(), path, directoryForm, keys);
        keys.forEach(key -> BINDINGS.put(key, ref));
        PROJECTS.add(ref);
        GraphLibraryManager.INSTANCE.putAll(batch);
        LOGGER.info("[nodegraph] eproject '{}' 已加载（{} 个库，{}）",
                project.name(), keys.size(), directoryForm ? "文件夹形态" : "单文件形态");
    }

    /** 库键末段 → 合法 libId（小写、非法字符与 ".." 序列替换为 "_"，空回落 "main"）。 */
    private static String libIdOf(String libraryKey) {
        int cut = Math.max(libraryKey.lastIndexOf('/'), libraryKey.lastIndexOf(':'));
        String tail = cut >= 0 ? libraryKey.substring(cut + 1) : libraryKey;
        String sanitized = tail.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_")
                .replace("..", "_");
        return sanitized.isEmpty() ? "main" : sanitized;
    }

    /** config/eyelib 根（与 BedrockAddonAutoLoader 同款版本中性路径，等价 FMLPaths.CONFIGDIR 默认布局）。 */
    private static Path configRoot() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("eyelib");
    }
}
