package io.github.tt432.eyelib.util;

import io.netty.buffer.ByteBuf;
/**
 * 网络字节缓冲抽象，替代 MC 的 FriendlyByteBuf。
 * 仅暴露 ModelComponentSyncPacket 实际使用的方法。
 *
 * @author TT432
 */
public interface PortFriendlyByteBuf {
    void writeInt(int value);
    int readInt();
    void writeUtf(String value);
    String readUtf();
    ByteBuf underlying();
}
