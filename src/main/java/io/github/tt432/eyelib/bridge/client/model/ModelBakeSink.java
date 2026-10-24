package io.github.tt432.eyelib.bridge.client.model;

import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.util.List;

/**
 * 模型顶点回调：ACL 侧的烘焙实现只看到几何基元，不接触 application 层的渲染类型。
 *
 * <p>配合 {@link ModelBakeTraversal} 使用：遍历由 application 层实现，回调落到这里，
 * 于是 bridge 不再依赖 {@code client.render.*}（ADR-0016 §2 反向依赖禁令）。
 *
 * @author TT432
 */
public interface ModelBakeSink {
    /** 一个四边形开始：后续 {@link #vertex} 调用属于该面。 */
    void face(List<Vector3fc> vertexes, List<Vector2fc> uvs, Vector3fc normal);

    /** 单个顶点（已由遍历侧按面顺序展开）。 */
    void vertex(Vector3fc vertex, Vector2fc uv, Vector3fc normal);
}
