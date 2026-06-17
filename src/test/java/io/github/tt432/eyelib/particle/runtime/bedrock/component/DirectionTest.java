package io.github.tt432.eyelibparticle.runtime.bedrock.component;

import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class DirectionTest {
    @Test
    void outwardsDirectionHasPositiveXComponent() {
        assertTrue(new Direction(Direction.Type.OUTWARDS, null)
                .getVec(new io.github.tt432.eyelibmolang.MolangScope(), new Vector3f(), new Vector3f(1, 0, 0)).x() > 0);
    }
}
