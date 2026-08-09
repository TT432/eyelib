package io.github.tt432.eyelib.nodegraph;

/**
 * 引用资产存在性判定（域层接缝）：域的 GraphValidator 不依赖客户端注册表，
 * 存在性由调用方注入——编辑器/构建路径传注册表实现，域测试传表驱动假实现。
 *
 * @author TT432
 */
@FunctionalInterface
public interface RefExistenceChecker {
    /**
     * @param refNodeTypeId ref 节点类型 id（ref.geometry / ref.texture / …）
     * @param identifier    引用值（identifier / path / material 选项值）
     */
    boolean exists(String refNodeTypeId, String identifier);

    /** 全放行（validate(GraphLibrary) 默认：域层不做资产存在性检查）。 */
    static RefExistenceChecker alwaysExists() {
        return (type, id) -> true;
    }
}
