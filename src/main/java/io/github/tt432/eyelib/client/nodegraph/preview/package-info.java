/**
 * 节点图编辑器资源引用预览共享助手（规格 docs/specs/nodegraph-visual-molang.md §3.3）。
 *
 * <p>本包供 ldlib1 / ldlib2 两个编辑器适配层共用：允许 {@code import net.minecraft.*}，
 * 禁止 {@code import com.lowdragmc.*}（LDLib 类型留在 editor 包）。
 */
@NullMarked
package io.github.tt432.eyelib.client.nodegraph.preview;

import org.jspecify.annotations.NullMarked;
