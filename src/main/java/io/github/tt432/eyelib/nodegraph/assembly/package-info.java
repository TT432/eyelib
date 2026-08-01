/**
 * 图 → Bedrock JSON 组装（规格 §2.5/§2.6/T6）：EVM 图库 → client_entity / render_controller /
 * animation_controller 文档 JSON（Gson 树）。
 *
 * <p>本包只产 Gson JSON 树并透传代码生成诊断；BrClientEntity 等的 CODEC 往返解析在
 * client 层完成，本包不依赖 {@code io.github.tt432.eyelib.importer}。
 */
@NullMarked
package io.github.tt432.eyelib.nodegraph.assembly;

import org.jspecify.annotations.NullMarked;
