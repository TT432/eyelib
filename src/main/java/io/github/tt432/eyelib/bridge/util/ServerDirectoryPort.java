package io.github.tt432.eyelib.bridge.util;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

/**
 * 服务端目录查询 Port，屏蔽不同版本间 {@code getServerDirectory()} 返回类型的差异。
 *
 * @author TT432
 */
public interface ServerDirectoryPort {

    static Path getServerDirectory(MinecraftServer server) {
        //? if <1.20.6 {
        return server.getServerDirectory().toPath();
        //?} else {
        return server.getServerDirectory();
        //?}
    }
}
