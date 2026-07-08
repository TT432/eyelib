package io.github.tt432.eyelib.bridge.client;

/**
 * 客户端 tick 查询 Port，隔离 application 对 ClientTickHandler 具体类的直接依赖。
 */
public interface ClientTickPort {
    static int getTick() {
        return ClientTickHandler.getTick();
    }
}
