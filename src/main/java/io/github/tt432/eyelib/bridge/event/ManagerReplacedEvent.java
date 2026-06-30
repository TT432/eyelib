package io.github.tt432.eyelib.bridge.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
//? if <1.20.6 {
import net.minecraftforge.eventbus.api.Event;
//?} else {
import net.neoforged.bus.api.Event;
//?}
/**
 * 管理器批量替换事件，表示某管理器的条目集被合并写入。
 *
 * @author TT432
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ManagerReplacedEvent extends Event {
    private final String managerName;
}
