package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;

/** 资源引用区块：ref.geometry/texture/material/animation/ac/rc/particle/sound 与变量引用端口（read:/write:）派生。 */
final class NodeTypesRefs {
    private NodeTypesRefs() {
    }

    static final NodeType REF_GEOMETRY = NodeTypes.register(NodeType.of(
            "ref.geometry", NodeType.Kind.REF_GEOMETRY, NodeTypes.CAT_REF,
            List.of(
                    // short_name 空 = 派生（ShortNames.effective）；非空 = 显式覆盖（外部契约逃生舱）
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "geometry.example.model", "geometry")),
            List.of(),
            List.of(PortDef.out("ref", PortType.GEOMETRY_REF))));

    static final NodeType REF_TEXTURE = NodeTypes.register(NodeType.of(
            "ref.texture", NodeType.Kind.REF_TEXTURE, NodeTypes.CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("path", "textures/entity/example", "texture")),
            List.of(),
            List.of(PortDef.out("ref", PortType.TEXTURE_REF))));

    static final NodeType REF_MATERIAL = NodeTypes.register(NodeType.of(
            "ref.material", NodeType.Kind.REF_MATERIAL, NodeTypes.CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("material", "entity_alphatest", "material")),
            List.of(),
            List.of(PortDef.out("ref", PortType.MATERIAL_REF))));

    /** 变量引用声明端口判定（EmitSession/GraphValidator 的豁免口径）。 */
    static boolean isVarRefPort(String portId) {
        return portId.startsWith(NodeTypes.VAR_READ_PREFIX) || portId.startsWith(NodeTypes.VAR_WRITE_PREFIX);
    }

    /**
     * 变量引用端口 id：名字中的 '.' 编码为 ':'（LDLib2 端口 id 禁用 '.'——保留给子端口；
     * molang 变量名是 IDENT(.IDENT)* 不含 ':'，编码在可达名字集上无碰撞）。
     * 连线两端与 {@link #varRefReadPorts}/{@link #varRefWritePorts} 必须同走本函数。
     */
    static String varRefPortId(String prefix, String name) {
        return prefix + name.replace('.', ':');
    }

    /** 从选项快照派生命名变量端口（无快照 = 无端口）。 */
    static List<PortDef> varRefPorts(NodeInstance instance) {
        List<PortDef> ports = new ArrayList<>(varRefReadPorts(instance));
        ports.addAll(varRefWritePorts(instance));
        return ports;
    }

    /** 读端口（左侧输入）：快照 reads 集。 */
    static List<PortDef> varRefReadPorts(NodeInstance instance) {
        com.google.gson.JsonElement raw = instance.options().get(NodeTypes.VAR_REFS_OPTION);
        if (raw == null || !raw.isJsonObject()) {
            return List.of();
        }
        com.google.gson.JsonArray reads = raw.getAsJsonObject().getAsJsonArray("reads");
        if (reads == null) {
            return List.of();
        }
        List<PortDef> ports = new ArrayList<>();
        for (com.google.gson.JsonElement e : reads) {
            String name = e.getAsString();
            // 左读右写（v9）：读 = 左侧输入，写 = 右侧输出；位置即语义，不加文字标记
            ports.add(PortDef.inLabeled(varRefPortId(NodeTypes.VAR_READ_PREFIX, name),
                    PortType.VARIABLE, "v." + name));
        }
        return ports;
    }

    /** 写端口（右侧输出）：快照 writes 集。 */
    static List<PortDef> varRefWritePorts(NodeInstance instance) {
        com.google.gson.JsonElement raw = instance.options().get(NodeTypes.VAR_REFS_OPTION);
        if (raw == null || !raw.isJsonObject()) {
            return List.of();
        }
        com.google.gson.JsonArray writes = raw.getAsJsonObject().getAsJsonArray("writes");
        if (writes == null) {
            return List.of();
        }
        List<PortDef> ports = new ArrayList<>();
        for (com.google.gson.JsonElement e : writes) {
            String name = e.getAsString();
            ports.add(PortDef.outLabeled(varRefPortId(NodeTypes.VAR_WRITE_PREFIX, name),
                    PortType.VARIABLE, "v." + name));
        }
        return ports;
    }

    static final NodeType REF_ANIMATION = NodeTypes.register(NodeType.dynamic(
            "ref.animation", NodeType.Kind.REF_ANIMATION, NodeTypes.CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "animation.example.walk", "animation")),
            (instance, resolver) -> varRefReadPorts(instance),
            (instance, resolver) -> {
                List<PortDef> outs = new ArrayList<>();
                outs.add(PortDef.out("ref", PortType.ANIMATION_REF));
                outs.addAll(varRefWritePorts(instance));
                return outs;
            }));

    static final NodeType REF_AC = NodeTypes.register(NodeType.of(
            "ref.ac", NodeType.Kind.REF_AC, NodeTypes.CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "controller.animation.example.main", "ac")),
            // v8：ref.ac 不建变量命名端口——AC 的变量引用由 AC 图自身承担（ac.state 的
            // on_entry/on_exit exec 链内 variable 节点即真实引用）；动画无图形态，
            // ref.animation 保留 read:/write: 命名端口（规格 §2.1，用户决策 2026-08-08）
            List.of(),
            List.of(PortDef.out("ref", PortType.AC_REF))));

    static final NodeType REF_RC = NodeTypes.register(NodeType.of(
            "ref.rc", NodeType.Kind.REF_RC, NodeTypes.CAT_REF,
            List.of(NodeOptionDef.asset("identifier", "controller.render.example", "rc")),
            List.of(
                    // v4：外部 RC 的挂载条件 + 为它准备的 geo/tex/mat 表行（规格 §2.2）
                    PortDef.in("condition", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.inMulti("decl_geometries", PortType.GEOMETRY_REF),
                    PortDef.inMulti("decl_textures", PortType.TEXTURE_REF),
                    PortDef.inMulti("decl_materials", PortType.MATERIAL_REF)),
            List.of(PortDef.out("ref", PortType.RC_REF))));

    /** ref.particle：粒子效果引用（v10）。实体级声明接 entity.root.particles；
     * AC state 侧经 particle.entry.ref 引用。 */
    static final NodeType REF_PARTICLE = NodeTypes.register(NodeType.of(
            "ref.particle", NodeType.Kind.REF_PARTICLE, NodeTypes.CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "example:my_particle", "particle")),
            List.of(),
            List.of(PortDef.out("ref", PortType.PARTICLE_REF))));

    /** ref.sound：音效引用（v10）。实体级声明接 entity.root.sounds；AC state 直接接 sounds 槽。 */
    static final NodeType REF_SOUND = NodeTypes.register(NodeType.of(
            "ref.sound", NodeType.Kind.REF_SOUND, NodeTypes.CAT_REF,
            List.of(
                    NodeOptionDef.string("short_name", ""),
                    NodeOptionDef.asset("identifier", "example:my_sound", "sound")),
            List.of(),
            List.of(PortDef.out("ref", PortType.SOUND_REF))));
}
