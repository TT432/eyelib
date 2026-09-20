package io.github.tt432.eyelib.particle.runtime.bedrock.component;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class DirectionTest {
    @Test
    void outwardsDirectionHasPositiveXComponent() {
        assertTrue(new Direction(Direction.Type.OUTWARDS, null)
                .getVec(new io.github.tt432.eyelib.molang.MolangScope(), new Vector3f(), new Vector3f(1, 0, 0)).x() > 0);
    }

    /**
     * 回归（9.19 反馈：粒子"定格"）：CUSTOM 显式方向向量必须与 INWARDS/OUTWARDS 一样
     * 归一化后乘 16（运动积分在 1/16 单位空间，见 ParticleMotionDynamic），否则初速度
     * 只有预期的 1/16。零向量不得产生 NaN。
     */
    @Test
    void customDirectionIsNormalizedAndScaledBy16() {
        var scope = new io.github.tt432.eyelib.molang.MolangScope();
        Vector3f v = new Direction(Direction.Type.CUSTOM,
                new MolangValue3(MolangValue.getConstant(3), MolangValue.getConstant(0), MolangValue.getConstant(0)))
                .getVec(scope, new Vector3f(), new Vector3f());
        assertEquals(16.0f, v.x(), 1.0E-4f);
        assertEquals(0.0f, v.y(), 1.0E-4f);
        assertEquals(0.0f, v.z(), 1.0E-4f);

        Vector3f zero = new Direction(Direction.Type.CUSTOM, MolangValue3.ZERO)
                .getVec(scope, new Vector3f(), new Vector3f());
        assertEquals(0.0f, zero.length(), 1.0E-6f);
    }
}
