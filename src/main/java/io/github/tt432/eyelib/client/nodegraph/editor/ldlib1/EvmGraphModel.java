//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.PortType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LDLib 1.x 画布状态的中间模型（纯 Java，零 LDLib 依赖）。
 *
 * <p>翻译分两段：{@code GraphData ↔ EvmGraphModel}（纯，{@link EvmGraphMapper}，可单测）与
 * {@code EvmGraphModel ↔ BaseGraph}（薄绑定，{@link Ldlib1GraphTranslator}）。
 * 画布上只有两类节点：EVM 节点（{@link EvmNode}，承载域节点类型）与
 * LDLib 参数节点（ParameterNode，旧参数面板拖拽产物，回译为本域 variable 节点）。
 *
 * @param nodes  画布节点
 * @param edges  连线（端口 id 均为域端口 id）
 * @param params 黑板变量（ExposedParameter 投影）
 */
public record EvmGraphModel(
        List<EvmNodeModel> nodes,
        List<EvmEdgeModel> edges,
        List<EvmParamModel> params
) {
    /**
     * 画布节点。
     *
     * @param uid         域 uid（EVM 节点为既有 uid 或新建节点的 GUID；PARAMETER 节点为 GUID）
     * @param kind        节点种类
     * @param typeId      域节点类型 id（仅 EVM）
     * @param parameterId 黑板变量标识符（仅 PARAMETER；= ExposedParameter.identifier = 分组全路径）
     * @param x           画布坐标 x
     * @param y           画布坐标 y
     * @param options     选项值（仅 EVM）
     * @param constants   未连接输入内联值（仅 EVM）
     */
    public record EvmNodeModel(
            String uid,
            Kind kind,
            @Nullable String typeId,
            @Nullable String parameterId,
            float x,
            float y,
            Map<String, JsonElement> options,
            Map<String, JsonElement> constants
    ) {
        public enum Kind {
            /** EVM 域节点（{@link EvmNode}）。 */
            EVM,
            /** LDLib 黑板参数节点（ParameterNode，accessor=Get）。 */
            PARAMETER
        }

        public static EvmNodeModel evm(String uid, String typeId, float x, float y,
                                       Map<String, JsonElement> options, Map<String, JsonElement> constants) {
            return new EvmNodeModel(uid, Kind.EVM, typeId, null, x, y,
                    Map.copyOf(options), Map.copyOf(constants));
        }

        public static EvmNodeModel parameter(String uid, String parameterId, float x, float y) {
            return new EvmNodeModel(uid, Kind.PARAMETER, null, parameterId, x, y, Map.of(), Map.of());
        }
    }

    /**
     * 连线（域端口 id 空间）。
     *
     * @param fromUid  输出侧节点 uid
     * @param fromPort 输出端口 id
     * @param toUid    输入侧节点 uid
     * @param toPort   输入端口 id
     */
    public record EvmEdgeModel(String fromUid, String fromPort, String toUid, String toPort) {
    }

    /**
     * 黑板变量投影。
     *
     * @param identifier   ExposedParameter 标识符 = 分组全路径（{@code "a/b/c"}，无分组即变量名）
     * @param type         值类型
     * @param defaultValue 默认值（空 = 0）
     */
    public record EvmParamModel(String identifier, PortType type, Optional<JsonElement> defaultValue) {
    }
}
//?}
