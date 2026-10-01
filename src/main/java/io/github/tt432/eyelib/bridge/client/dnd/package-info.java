/**
 * 客户端拖放（DND）基础设施（移植自 yessteveskill client/dnd，as-is）：
 * Windows OLE IDropTarget 接管 GLFW 窗口拖放（hover 状态 + 落点坐标），
 * 其它平台退回 MC 原生 {@code Screen#onFilesDrop}。
 */
@NullMarked
package io.github.tt432.eyelib.bridge.client.dnd;

import org.jspecify.annotations.NullMarked;
