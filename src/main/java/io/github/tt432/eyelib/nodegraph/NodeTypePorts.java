package io.github.tt432.eyelib.nodegraph;

import java.util.Optional;

/** execIn、execOut、slotIn/slotOut 端口构造助手。 */
final class NodeTypePorts {
    private NodeTypePorts() {
    }

    static PortDef execIn() {
        return new PortDef("exec_in", PortDirection.IN, PortType.EXEC, Optional.empty(), false);
    }

    static PortDef execOut() {
        return new PortDef("exec_out", PortDirection.OUT, PortType.EXEC, Optional.empty(), false);
    }

    static PortDef slotOut(String id) {
        return PortDef.outSingle(id, PortType.SLOT);
    }

    static PortDef slotIn(String id) {
        return PortDef.inMulti(id, PortType.SLOT);
    }
}
