package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;

/**
 * 图文档库：一个 JSON 文件 = 1 主图 + N 命名子图（规格 D5）。
 *
 * @param formatVersion schema 版本（当前 1）
 * @param kind          库种类
 * @param main          主图名（graphs 中的键）
 * @param graphs        全部图（主图 + 子图）
 */
public record GraphLibrary(
        int formatVersion,
        GraphKind kind,
        String main,
        Map<String, GraphData> graphs
) {
    public static final int CURRENT_FORMAT_VERSION = 1;

    public static final Codec<GraphLibrary> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("format_version", CURRENT_FORMAT_VERSION).forGetter(GraphLibrary::formatVersion),
            GraphKind.CODEC.fieldOf("kind").forGetter(GraphLibrary::kind),
            Codec.STRING.optionalFieldOf("main", "root").forGetter(GraphLibrary::main),
            Codec.unboundedMap(Codec.STRING, GraphData.CODEC).fieldOf("graphs").forGetter(GraphLibrary::graphs)
    ).apply(ins, GraphLibrary::new));

    public GraphData mainGraph() {
        GraphData data = graphs.get(main);
        if (data == null) {
            throw new IllegalStateException("main graph '" + main + "' not found in library");
        }
        return data;
    }

    public Optional<GraphData> graph(String name) {
        return Optional.ofNullable(graphs.get(name));
    }

    /** 子图接口解析器（供动态端口推导）。 */
    public NodeType.SubgraphResolver subgraphResolver() {
        return name -> graph(name).flatMap(GraphData::graphInterface);
    }
}
