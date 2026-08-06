/**
 * LDLib2（1.21.1 / 26.1.2）节点图工作台：编辑器内嵌的工具条、导入对话框、
 * 资产检查器侧栏、molang 调试侧栏与画布节点值徽标（规格 nodegraph-workbench §W1/W2/W3）。
 *
 * <p>重逻辑全部在版本无关共享层：反编译（{@code nodegraph.decompile}）、
 * 徽标模型（{@code workbench.NodeDebugOverlayModel}）、
 * 资产/调试服务（{@code jsonview.EntityJsonService} / {@code molangdebug.MolangDebugService}）；
 * 本包只做 LDLib2 UIElement 薄壳（全版本共享，ldlib1 已退役）。
 */
@NullMarked
package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;

import org.jspecify.annotations.NullMarked;
