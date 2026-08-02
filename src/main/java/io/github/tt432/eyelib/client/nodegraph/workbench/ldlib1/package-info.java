//? if <1.20.6 {

/**
 * LDLib 1.x（1.20.1）节点图工作台薄 UI 层（规格 nodegraph-workbench §W1/W3）：
 * 导入对话框、资产检查器侧栏、调试侧栏与画布节点值徽标。
 *
 * <p>重逻辑全部在版本无关层：反编译在 domain（{@code nodegraph.decompile}），
 * 徽标模型在 {@code client.nodegraph.workbench.NodeDebugOverlayModel}，
 * 数据源为 EntityJsonService / MolangDebugService；本包只做 LDLib1 widget 装配与绘制。
 */
@NullMarked
package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import org.jspecify.annotations.NullMarked;
//?}
