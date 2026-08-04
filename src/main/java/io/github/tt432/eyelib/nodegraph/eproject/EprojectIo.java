package io.github.tt432.eyelib.nodegraph.eproject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphMigrations;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * eproject（OPC 布局）读写工具。两种物理形态共享同一条目布局：
 *
 * <pre>
 * [Content_Types].xml   OPC 标准内容类型声明（json/xml 默认映射）
 * _rels/.rels           OPC 包级关系：rId1 → project.json
 * project.json          {"format":"eyelib-eproject","version":N,"name":"..."}（Gson pretty）
 * libraries/&lt;libId&gt;.json  每个 GraphLibrary 一份（GraphLibrary.CODEC + JsonOps + Gson pretty）
 * </pre>
 *
 * <p>形态判定：
 * <ul>
 *   <li>读取：目录 → 目录形态；常规文件（建议 {@code .eproject} 后缀）→ zip 形态。</li>
 *   <li>写入：目标已存在且为目录 → 目录形态；否则 → {@code .eproject} 单文件（zip）。</li>
 * </ul>
 *
 * <p>健壮性约定：
 * <ul>
 *   <li>读取时忽略未知条目（前向兼容）；{@code format} 不匹配、{@code version} 高于
 *       {@link Eproject#CURRENT_VERSION}、库 JSON 编解码失败均抛 {@link EprojectException}。</li>
 *   <li>库 id 必须匹配 {@code [a-z0-9_./-]+} 且不含 {@code ".."}（写入时校验，读取时非法条目按未知忽略）。</li>
 *   <li>zip 写入先落同目录临时文件，再原子移动（{@code ATOMIC_MOVE} 不支持时回落
 *       {@code REPLACE_EXISTING}），避免半写文件。</li>
 *   <li>目录形态写入后清理 {@code libraries/} 下不在模型中的陈旧 {@code .json}（其它文件不动）。</li>
 * </ul>
 */
public final class EprojectIo {
    private EprojectIo() {
    }

    /** 单文件形态后缀。 */
    public static final String FILE_SUFFIX = ".eproject";
    /** project.json 的 format 标识。 */
    public static final String FORMAT = "eyelib-eproject";

    private static final String CONTENT_TYPES_XML = "[Content_Types].xml";
    private static final String RELS_XML = "_rels/.rels";
    private static final String PROJECT_JSON = "project.json";
    private static final String LIBRARIES_DIR = "libraries";
    private static final String JSON_SUFFIX = ".json";

    private static final String CONTENT_TYPES_BODY = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="json" ContentType="application/json"/>
              <Default Extension="xml" ContentType="application/xml"/>
            </Types>
            """;

    private static final String RELS_BODY = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="https://eyelib.tt432.io/relationships/project" Target="project.json"/>
            </Relationships>
            """;

    private static final Pattern LIB_ID_PATTERN = Pattern.compile("[a-z0-9_./-]+");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---------- 形态探测 ----------

    /** 是否 eproject 目录形态：是目录且含 {@code project.json}。 */
    public static boolean isEprojectDir(Path path) {
        return Files.isDirectory(path) && Files.isRegularFile(path.resolve(PROJECT_JSON));
    }

    /** 是否 eproject 单文件形态：是常规文件且文件名以 {@code .eproject} 结尾。 */
    public static boolean isEprojectFile(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName() != null
                && path.getFileName().toString().endsWith(FILE_SUFFIX);
    }

    // ---------- 读取 ----------

    /**
     * 读取 eproject（目录形态与 zip 形态自动识别）。
     *
     * @param path 目录或 {@code .eproject} 文件
     * @return 项目模型（库按条目名排序，保证确定性）
     * @throws EprojectException 路径不是 eproject、format 不匹配、版本过高、编解码失败或 IO 错误
     */
    public static Eproject read(Path path) {
        if (Files.isDirectory(path)) {
            try {
                return readEntries(path, new DirAccess(path));
            } catch (IOException e) {
                throw new EprojectException(path, "读取目录形态 eproject 失败: " + e.getMessage(), e);
            }
        }
        if (Files.isRegularFile(path)) {
            try (ZipFile zip = new ZipFile(path.toFile())) {
                return readEntries(path, new ZipAccess(zip));
            } catch (IOException e) {
                throw new EprojectException(path, "读取 zip 形态 eproject 失败: " + e.getMessage(), e);
            }
        }
        throw new EprojectException(path, "不是 eproject 目录或 .eproject 文件");
    }

    /** 统一条目读取流程：两种形态共享。 */
    private static Eproject readEntries(Path source, Access access) throws IOException {
        byte[] projectBytes = access.read(PROJECT_JSON)
                .orElseThrow(() -> new EprojectException(source, "缺少 " + PROJECT_JSON));

        JsonObject projectJson;
        try {
            projectJson = JsonParser.parseString(new String(projectBytes, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new EprojectException(source, PROJECT_JSON + " 不是合法 JSON 对象: " + e.getMessage(), e);
        }

        JsonElement format = projectJson.get("format");
        if (format == null || !format.isJsonPrimitive() || !FORMAT.equals(format.getAsString())) {
            throw new EprojectException(source,
                    PROJECT_JSON + " format 字段缺失或不匹配（期望 " + FORMAT + "）");
        }
        int version = projectJson.has("version") ? projectJson.get("version").getAsInt() : 0;
        if (version > Eproject.CURRENT_VERSION) {
            throw new EprojectException(source,
                    "eproject 版本 " + version + " 高于当前支持版本 " + Eproject.CURRENT_VERSION + "（未来格式，拒绝读取）");
        }
        String name = projectJson.has("name") ? projectJson.get("name").getAsString() : "";

        // 条目名排序后读取：zip 条目序不确定，保证库顺序确定
        List<String> libraryEntries = new ArrayList<>(access.libraryEntries());
        Collections.sort(libraryEntries);
        Map<String, GraphLibrary> libraries = new LinkedHashMap<>();
        for (String entry : libraryEntries) {
            String libId = entry.substring(LIBRARIES_DIR.length() + 1, entry.length() - JSON_SUFFIX.length());
            if (!isValidLibId(libId)) {
                continue; // 非法/未知条目忽略（前向兼容）
            }
            byte[] body = access.read(entry)
                    .orElseThrow(() -> new EprojectException(source, "条目读取失败: " + entry));
            libraries.put(libId, decodeLibrary(source, entry, body));
        }
        return new Eproject(name, version, libraries);
    }

    /** 库 JSON → GraphLibrary（CODEC + JsonOps + GraphMigrations 链式迁移）。 */
    private static GraphLibrary decodeLibrary(Path source, String entry, byte[] body) {
        JsonElement json;
        try {
            json = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
        } catch (JsonParseException e) {
            throw new EprojectException(source, "库条目 " + entry + " 不是合法 JSON: " + e.getMessage(), e);
        }
        GraphLibrary parsed = GraphLibrary.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> {
                    throw new EprojectException(source, "库条目 " + entry + " CODEC 解析失败: " + err);
                })
                .orElseThrow(() -> new EprojectException(source, "库条目 " + entry + " CODEC 解析失败"));
        return GraphMigrations.migrate(parsed);
    }

    // ---------- 写入 ----------

    /**
     * 写出 eproject。目标已存在且为目录 → 目录形态；否则 → {@code .eproject} 单文件（zip）。
     *
     * <p>目录形态额外清理 {@code libraries/} 下不在模型中的陈旧 {@code .json}；
     * zip 形态先写同目录临时文件再原子移动（不支持原子移动时回落覆盖移动）。
     *
     * @param target  目标目录或目标文件路径
     * @param project 项目模型（库 id 先经 {@code [a-z0-9_./-]+} 且不含 {@code ".."} 校验）
     * @throws EprojectException 库 id 非法、编解码失败或 IO 错误
     */
    public static void write(Path target, Eproject project) {
        // 先整体校验 + 编码，避免写一半才失败
        Map<String, String> encodedLibraries = new LinkedHashMap<>();
        for (Map.Entry<String, GraphLibrary> e : project.libraries().entrySet()) {
            String libId = e.getKey();
            if (!isValidLibId(libId)) {
                throw new EprojectException(target,
                        "非法库 id '" + libId + "'（需匹配 [a-z0-9_./-]+ 且不含 \"..\"）");
            }
            encodedLibraries.put(libId, encodeLibrary(target, libId, e.getValue()));
        }
        String projectJson = encodeProjectJson(project);

        if (Files.isDirectory(target)) {
            writeFolder(target, project, projectJson, encodedLibraries);
        } else {
            writeZip(target, projectJson, encodedLibraries);
        }
    }

    private static String encodeProjectJson(Eproject project) {
        JsonObject json = new JsonObject();
        json.addProperty("format", FORMAT);
        json.addProperty("version", project.version());
        json.addProperty("name", project.name());
        return GSON.toJson(json);
    }

    /** GraphLibrary → 库 JSON 文本（CODEC + JsonOps + Gson pretty，与 GraphJson 惯例一致）。 */
    private static String encodeLibrary(Path target, String libId, GraphLibrary library) {
        JsonElement encoded = GraphLibrary.CODEC.encodeStart(JsonOps.INSTANCE, library)
                .resultOrPartial(err -> {
                    throw new EprojectException(target, "库 '" + libId + "' CODEC 编码失败: " + err);
                })
                .orElseThrow(() -> new EprojectException(target, "库 '" + libId + "' CODEC 编码失败"));
        return GSON.toJson(encoded);
    }

    // ---------- 目录形态 ----------

    private static void writeFolder(Path dir, Eproject project, String projectJson,
                                    Map<String, String> encodedLibraries) {
        Path librariesDir = dir.resolve(LIBRARIES_DIR);
        try {
            Files.createDirectories(librariesDir);
            Files.createDirectories(dir.resolve("_rels"));
            Files.writeString(dir.resolve(CONTENT_TYPES_XML), CONTENT_TYPES_BODY, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(RELS_XML), RELS_BODY, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(PROJECT_JSON), projectJson, StandardCharsets.UTF_8);

            for (Map.Entry<String, String> e : encodedLibraries.entrySet()) {
                Path libFile = libraryPath(librariesDir, e.getKey());
                if (libFile.getParent() != null) {
                    Files.createDirectories(libFile.getParent());
                }
                Files.writeString(libFile, e.getValue(), StandardCharsets.UTF_8);
            }

            cleanStaleLibraries(dir, librariesDir, project.libraries().keySet());
        } catch (IOException e) {
            throw new EprojectException(dir, "写入目录形态 eproject 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理 {@code libraries/} 下不在模型中的陈旧 .json（仅删 .json 常规文件，
     * 其它文件与目录一律不动）。
     */
    private static void cleanStaleLibraries(Path dir, Path librariesDir, Set<String> modelIds) throws IOException {
        Set<String> keep = new HashSet<>();
        for (String libId : modelIds) {
            keep.add(libId + JSON_SUFFIX);
        }
        List<Path> stale = new ArrayList<>();
        Files.walkFileTree(librariesDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && file.getFileName().toString().endsWith(JSON_SUFFIX)) {
                    String rel = librariesDir.relativize(file).toString().replace('\\', '/');
                    if (!keep.contains(rel)) {
                        stale.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        for (Path file : stale) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new EprojectException(dir, "清理陈旧库文件失败: " + file, e);
            }
        }
    }

    /** 库 id → libraries/ 下路径（id 已校验，仍防御性归一化并确认未逃逸）。 */
    private static Path libraryPath(Path librariesDir, String libId) {
        Path resolved = librariesDir.resolve(libId + JSON_SUFFIX).normalize();
        if (!resolved.startsWith(librariesDir.normalize())) {
            throw new EprojectException(librariesDir, "库 id '" + libId + "' 解析路径逃逸 libraries/");
        }
        return resolved;
    }

    // ---------- zip 形态 ----------

    private static void writeZip(Path target, String projectJson, Map<String, String> encodedLibraries) {
        Path absolute = target.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new EprojectException(target, "目标路径无父目录，无法落临时文件");
        }
        Path temp;
        try {
            Files.createDirectories(parent);
            temp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        } catch (IOException e) {
            throw new EprojectException(target, "创建临时文件失败: " + e.getMessage(), e);
        }
        boolean moved = false;
        try {
            try (OutputStream out = Files.newOutputStream(temp);
                 ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
                putEntry(zip, CONTENT_TYPES_XML, CONTENT_TYPES_BODY);
                putEntry(zip, RELS_XML, RELS_BODY);
                putEntry(zip, PROJECT_JSON, projectJson);
                for (Map.Entry<String, String> e : encodedLibraries.entrySet()) {
                    putEntry(zip, LIBRARIES_DIR + "/" + e.getKey() + JSON_SUFFIX, e.getValue());
                }
            }
            try {
                Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } catch (IOException e) {
            throw new EprojectException(target, "写入 zip 形态 eproject 失败: " + e.getMessage(), e);
        } finally {
            if (!moved) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响主错误
                }
            }
        }
    }

    private static void putEntry(ZipOutputStream zip, String name, String body) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ---------- 库 id 校验 ----------

    /** 库 id 合法性：{@code [a-z0-9_./-]+} 且不含 {@code ".."}（防路径逃逸）。 */
    static boolean isValidLibId(String libId) {
        return LIB_ID_PATTERN.matcher(libId).matches() && !libId.contains("..");
    }

    // ---------- 条目访问抽象（目录 / zip 统一） ----------

    private interface Access {
        /** 读取条目内容；不存在 → {@link Optional#empty()}。 */
        Optional<byte[]> read(String entryName) throws IOException;

        /** libraries/ 下全部 .json 条目名（正斜杠分隔、相对包根）。 */
        List<String> libraryEntries() throws IOException;
    }

    private record DirAccess(Path root) implements Access {
        @Override
        public Optional<byte[]> read(String entryName) throws IOException {
            Path file = root.resolve(entryName).normalize();
            if (!file.startsWith(root.normalize()) || !Files.isRegularFile(file)) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(file));
        }

        @Override
        public List<String> libraryEntries() throws IOException {
            Path librariesDir = root.resolve(LIBRARIES_DIR);
            if (!Files.isDirectory(librariesDir)) {
                return List.of();
            }
            List<String> entries = new ArrayList<>();
            Files.walkFileTree(librariesDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && file.getFileName().toString().endsWith(JSON_SUFFIX)) {
                        entries.add(LIBRARIES_DIR + "/"
                                + librariesDir.relativize(file).toString().replace('\\', '/'));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return entries;
        }
    }

    private record ZipAccess(ZipFile zip) implements Access {
        @Override
        public Optional<byte[]> read(String entryName) throws IOException {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                return Optional.empty();
            }
            try (var in = zip.getInputStream(entry)) {
                return Optional.of(in.readAllBytes());
            }
        }

        @Override
        public List<String> libraryEntries() {
            String prefix = LIBRARIES_DIR + "/";
            List<String> entries = new ArrayList<>();
            var enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (!entry.isDirectory()
                        && entry.getName().startsWith(prefix)
                        && entry.getName().endsWith(JSON_SUFFIX)) {
                    entries.add(entry.getName());
                }
            }
            return entries;
        }
    }
}
