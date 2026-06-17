package io.github.tt432.eyelib.molang.port;

import org.jspecify.annotations.NullMarked;

/**
 * 世界/关卡抽象，提供 Molang query.* 所需的运行时数据。
 *
 * @author TT432
 */
@NullMarked
public interface PortLevel {
    long getDayTime();
    long getGameTime();
    int getPlayerCount();
    float getMoonPhase();
}
