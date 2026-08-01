package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

/**
 * 一张图（主图或子图）的完整数据。
 *
 * @param nodes       节点实例
 * @param wires       连线
 * @param variables   黑板变量（variable.* 根；temp.* 不进黑板）
 * @param placemats   画布分组
 * @param stickyNotes 便签
 * @param graphInterface 子图接口（存在 = 该图是子图）
 */
public record GraphData(
        List<NodeInstance> nodes,
        List<Wire> wires,
        List<VariableDecl> variables,
        List<Placemat> placemats,
        List<StickyNote> stickyNotes,
        Optional<GraphInterface> graphInterface
) {
    public static final Codec<GraphData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            NodeInstance.CODEC.listOf().optionalFieldOf("nodes", List.of()).forGetter(GraphData::nodes),
            Wire.CODEC.listOf().optionalFieldOf("wires", List.of()).forGetter(GraphData::wires),
            VariableDecl.CODEC.listOf().optionalFieldOf("variables", List.of()).forGetter(GraphData::variables),
            Placemat.CODEC.listOf().optionalFieldOf("placemats", List.of()).forGetter(GraphData::placemats),
            StickyNote.CODEC.listOf().optionalFieldOf("sticky_notes", List.of()).forGetter(GraphData::stickyNotes),
            GraphInterface.CODEC.optionalFieldOf("interface").forGetter(GraphData::graphInterface)
    ).apply(ins, GraphData::new));

    public static GraphData empty() {
        return new GraphData(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.empty());
    }

    public boolean isSubgraph() {
        return graphInterface.isPresent();
    }

    public Optional<NodeInstance> findNode(String uid) {
        return nodes.stream().filter(n -> n.uid().equals(uid)).findFirst();
    }
}
