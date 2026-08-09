package io.github.tt432.eyelib.nodegraph.eproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * EprojectIo（OPC 布局 eproject 容器）读写单测。
 */
class EprojectIoTest {

    @TempDir
    Path tempDir;

    // ---------- 测试数据 ----------

    private static GraphLibrary lib(String mainName, String... extraGraphs) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        graphs.put(mainName, new GraphData(
                List.of(new NodeInstance("n1", "const.number", 10, 20,
                        Map.of("value", new JsonPrimitive("1")), Map.of())),
                List.of(), List.of(), List.of(), List.of(), Optional.empty()));
        for (String g : extraGraphs) {
            graphs.put(g, GraphData.empty());
        }
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, GraphKind.CLIENT_ENTITY, mainName, graphs);
    }

    private static Eproject sampleProject() {
        Map<String, GraphLibrary> libraries = new LinkedHashMap<>();
        libraries.put("alpha", lib("root", "sub_a"));
        libraries.put("beta", lib("main", "sub_b", "sub_c"));
        return Eproject.of("示例项目", libraries);
    }

    @Test
    void libraryJsonAlwaysCarriesFormatVersion() throws IOException {
        // optionalFieldOf 在值等于默认值时不落盘——encodeLibrary 显式补写，文件必须自描述
        Path dir = tempDir.resolve("proj");
        Files.createDirectories(dir);
        EprojectIo.write(dir, sampleProject());
        String text = Files.readString(dir.resolve("libraries/alpha.json"), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        assertTrue(json.has("format_version"), "库 JSON 必须带 format_version");
        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, json.get("format_version").getAsInt());
    }

    @Test
    void missingFormatVersionWithArgCountMigratesFromV10() throws IOException {
        // 旧构建写出的文件缺 format_version（encode 省略默认值）——读取侧按 arg_count 内容
        // 探针定代为 v10 并走迁移（否则被 CODEC 缺省值误当当前版本跳过迁移）
        Path dir = tempDir.resolve("proj");
        Files.createDirectories(dir);
        EprojectIo.write(dir, sampleProject());

        Map<String, JsonElement> options = new LinkedHashMap<>();
        options.put("function", new JsonPrimitive("query.is_name_any"));
        options.put("arg_count", new JsonPrimitive(2));
        Map<String, JsonElement> constants = new LinkedHashMap<>();
        constants.put("arg1", new JsonPrimitive("a"));
        constants.put("arg2", new JsonPrimitive("b"));
        GraphLibrary v10 = new GraphLibrary(10, GraphKind.CLIENT_ENTITY, "root",
                Map.of("root", new GraphData(
                        List.of(new NodeInstance("q", "query.call", 0, 0, options, constants)),
                        List.of(), List.of(), List.of(), List.of(), Optional.empty())));
        JsonObject legacyJson = GraphLibrary.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, v10)
                .result().orElseThrow().getAsJsonObject();
        legacyJson.remove("format_version"); // 模拟旧构建输出（默认值不落盘）
        Files.writeString(dir.resolve("libraries/alpha.json"),
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(legacyJson),
                StandardCharsets.UTF_8);

        Eproject readBack = EprojectIo.read(dir);
        GraphLibrary migrated = readBack.libraries().get("alpha");
        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion(), "应迁移到当前版本");
        NodeInstance q = migrated.mainGraph().findNode("q").orElseThrow();
        assertFalse(q.options().containsKey("arg_count"), "arg_count 已消除");
        assertNotNull(q.options().get("args"), "变长参数收进 args 列表");
        assertEquals(2, q.options().get("args").getAsJsonArray().size());
    }

    // ---------- round-trip ----------

    @Test
    void folderRoundTrip() throws IOException {
        Path dir = tempDir.resolve("proj");
        Files.createDirectories(dir); // 目标已存在为目录 → 目录形态
        Eproject project = sampleProject();

        EprojectIo.write(dir, project);

        assertTrue(EprojectIo.isEprojectDir(dir));
        assertTrue(Files.isRegularFile(dir.resolve("[Content_Types].xml")));
        assertTrue(Files.isRegularFile(dir.resolve("_rels/.rels")));
        assertTrue(Files.isRegularFile(dir.resolve("project.json")));
        assertTrue(Files.isRegularFile(dir.resolve("libraries/alpha.json")));
        assertTrue(Files.isRegularFile(dir.resolve("libraries/beta.json")));

        Eproject readBack = EprojectIo.read(dir);
        assertEquals(project, readBack);
        // 逐字段核对（多库 + 子图）
        assertEquals("示例项目", readBack.name());
        assertEquals(Eproject.CURRENT_VERSION, readBack.version());
        assertEquals(List.of("alpha", "beta"), List.copyOf(readBack.libraries().keySet()));
        GraphLibrary alpha = readBack.libraries().get("alpha");
        assertEquals(GraphKind.CLIENT_ENTITY, alpha.kind());
        assertEquals("root", alpha.main());
        assertEquals(List.of("root", "sub_a"), List.copyOf(alpha.graphs().keySet()));
        assertEquals("n1", alpha.mainGraph().nodes().get(0).uid());
        assertEquals("1", alpha.mainGraph().nodes().get(0).options().get("value").getAsString());
    }

    @Test
    void zipRoundTrip() {
        Path file = tempDir.resolve("proj.eproject");
        Eproject project = sampleProject();

        EprojectIo.write(file, project);

        assertTrue(EprojectIo.isEprojectFile(file));
        assertEquals(project, EprojectIo.read(file));
    }

    @Test
    void zipIsValidOpc() throws Exception {
        Path file = tempDir.resolve("proj.eproject");
        EprojectIo.write(file, sampleProject());

        try (ZipFile zip = new ZipFile(file.toFile())) {
            assertNotNull(zip.getEntry("[Content_Types].xml"));
            assertNotNull(zip.getEntry("_rels/.rels"));
            assertNotNull(zip.getEntry("project.json"));
            assertNotNull(zip.getEntry("libraries/alpha.json"));

            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var builder = factory.newDocumentBuilder();

            var types = builder.parse(zip.getInputStream(zip.getEntry("[Content_Types].xml")));
            assertEquals("Types", types.getDocumentElement().getLocalName());
            assertEquals("http://schemas.openxmlformats.org/package/2006/content-types",
                    types.getDocumentElement().getNamespaceURI());

            var rels = builder.parse(zip.getInputStream(zip.getEntry("_rels/.rels")));
            assertEquals("Relationships", rels.getDocumentElement().getLocalName());
            assertEquals("http://schemas.openxmlformats.org/package/2006/relationships",
                    rels.getDocumentElement().getNamespaceURI());
        }
    }

    // ---------- 目录形态清理 ----------

    @Test
    void folderWriteCleansStaleLibraries() throws IOException {
        Path dir = tempDir.resolve("proj");
        Files.createDirectories(dir);
        Eproject twoLibs = sampleProject();
        EprojectIo.write(dir, twoLibs);
        assertTrue(Files.isRegularFile(dir.resolve("libraries/beta.json")));

        // 非 .json 文件与项目外文件必须保留
        Files.writeString(dir.resolve("libraries/keep.txt"), "别动我", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("notes.md"), "也别动我", StandardCharsets.UTF_8);

        Map<String, GraphLibrary> oneLib = new LinkedHashMap<>();
        oneLib.put("alpha", lib("root"));
        Eproject shrunk = Eproject.of("缩容", oneLib);
        EprojectIo.write(dir, shrunk);

        assertTrue(Files.isRegularFile(dir.resolve("libraries/alpha.json")));
        assertFalse(Files.exists(dir.resolve("libraries/beta.json")), "陈旧库文件应被清理");
        assertTrue(Files.isRegularFile(dir.resolve("libraries/keep.txt")), "非 .json 文件不动");
        assertTrue(Files.isRegularFile(dir.resolve("notes.md")), "libraries/ 之外文件不动");
        assertEquals(shrunk, EprojectIo.read(dir));
    }

    // ---------- 坏格式 / 未来版本 ----------

    @Test
    void badFormatRejected() throws IOException {
        Path dir = tempDir.resolve("bad_format");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("project.json"),
                "{\"format\":\"not-eproject\",\"version\":1,\"name\":\"x\"}", StandardCharsets.UTF_8);

        var ex = assertThrows(EprojectException.class, () -> EprojectIo.read(dir));
        assertTrue(ex.getMessage().contains("format"));
        assertTrue(ex.getMessage().contains(dir.toString()), "异常应携带路径信息");
    }

    @Test
    void futureVersionRejected() throws IOException {
        Path dir = tempDir.resolve("future");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("project.json"),
                "{\"format\":\"eyelib-eproject\",\"version\":99,\"name\":\"x\"}", StandardCharsets.UTF_8);

        var ex = assertThrows(EprojectException.class, () -> EprojectIo.read(dir));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void missingProjectJsonRejected() {
        Path dir = tempDir.resolve("empty");
        assertThrows(EprojectException.class, () -> EprojectIo.read(dir));
    }

    // ---------- 库 id 校验 ----------

    @Test
    void illegalLibIdRejected() {
        Map<String, GraphLibrary> bad = new LinkedHashMap<>();
        bad.put("../escape", lib("root"));
        Eproject project = Eproject.of("bad", bad);

        assertThrows(EprojectException.class, () -> EprojectIo.write(tempDir.resolve("x"), project));
        assertThrows(EprojectException.class, () -> EprojectIo.write(tempDir.resolve("x.eproject"), project));
    }

    @Test
    void uppercaseLibIdRejected() {
        Map<String, GraphLibrary> bad = new LinkedHashMap<>();
        bad.put("Alpha", lib("root"));
        Eproject project = Eproject.of("bad", bad);

        assertThrows(EprojectException.class, () -> EprojectIo.write(tempDir.resolve("x"), project));
    }
}
