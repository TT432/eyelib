package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 端口引用：节点 uid + 端口 id。
 *
 * @param node 节点 uid
 * @param port 端口 id
 */
public record PortRef(String node, String port) {
    public static final Codec<PortRef> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("node").forGetter(PortRef::node),
            Codec.STRING.fieldOf("port").forGetter(PortRef::port)
    ).apply(ins, PortRef::new));

    @Override
    public String toString() {
        return node + "." + port;
    }
}
