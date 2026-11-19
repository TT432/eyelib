package io.github.tt432.eyelib.importer.entity;

/**
 * 客户端实体解码后的整实体归一化钩子。
 * <p>
 * 用于在反序列化出口统一修补 Bedrock 数据里的历史写法（例如字段别名、缺省折叠），
 * 使得调用方无需在每个 loader 里各写一份补丁逻辑。
 * <p>
 * 约定：
 * <ul>
 *     <li>纯函数：不得依赖时序外部状态，同一输入恒得同一输出；</li>
 *     <li>幂等：{@code normalize(normalize(x))} 与 {@code normalize(x)} 等价；</li>
 *     <li>不得修改 {@code identifier}（实体名是聚合与覆盖的键）；</li>
 *     <li>未改动时必须原样返回入参本身（引用相等），以便上游据此跳过后续处理。</li>
 * </ul>
 * 钩子注册见 {@link BrClientEntityNormalizations}。
 *
 * @author TT432
 */
@FunctionalInterface
public interface BrClientEntityNormalizer {
    /**
     * 解码后整实体归一化。
     *
     * @param decoded 刚解码出的实体（调用方不应再依赖其可变性）
     * @return 归一化后的实体；未改动时返回 {@code decoded} 本身
     */
    BrClientEntity normalize(BrClientEntity decoded);
}
