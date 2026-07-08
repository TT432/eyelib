package io.github.tt432.eyelib.bridge.client;

import net.minecraft.client.Minecraft;

/**
 * 客户端任务提交 Port，屏蔽不同版本间 {@code mc.tell} / {@code mc.submit} 的 API 差异。
 *
 * @author TT432
 */
public interface ClientTaskPort {

    static void execute(Runnable task) {
        //? if <26.1 {
        Minecraft.getInstance().tell(task);
        //?} else {
        Minecraft.getInstance().submit(task);
        //?}
    }
}
