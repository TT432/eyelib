package io.github.tt432.eyelib.bridge.client.dnd;

import java.nio.file.Path;
import java.util.List;

/**
 * 支持带落点坐标的拖放目标。
 * <p>Windows hover 模式下，{@link DragDropManager} 在松手时调用此方法，
 * 目标根据落点位置（如左/右半屏）决定如何处理文件。
 * <p>未实现此接口的 Screen 仍会被 {@code DragDropManager} 透明转发到
 * {@link net.minecraft.client.gui.screens.Screen#onFilesDrop}，保留原 MC 行为。
 */
public interface PositionedDropTarget {
    /**
     * @param files 拖入的文件列表
     * @param dropX 松手时鼠标在窗口客户区的 x 坐标
     * @param dropY 松手时鼠标在窗口客户区的 y 坐标
     */
    void onFilesDropWithPosition(List<Path> files, int dropX, int dropY);
}
