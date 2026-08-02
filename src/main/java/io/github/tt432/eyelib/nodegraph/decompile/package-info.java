/**
 * 节点图反编译（规格 {@code docs/specs/nodegraph-workbench.md} §W2）：
 * Molang 文本 / Bedrock JSON 资产（ClientEntity / RenderController / AnimationController）
 * → 图 IR（{@code GraphLibrary}）。
 *
 * <p>结构镜像 {@code nodegraph.assembly} 三个组装器的逆；纯 domain 模块：
 * 零 Minecraft / 零 LDLib import（gson / DFU 可用）。
 */
@NullMarked
package io.github.tt432.eyelib.nodegraph.decompile;

import org.jspecify.annotations.NullMarked;
