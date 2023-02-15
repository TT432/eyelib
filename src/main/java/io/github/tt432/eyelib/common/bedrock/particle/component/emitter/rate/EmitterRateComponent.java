package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.rate;

import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangVariableScope;

/**
 * @author DustW
 */
public abstract class EmitterRateComponent extends ParticleComponent {
    /**
     * 返回当前需要生成的粒子数量
     * @param scope 变量域
     * @return 数量，<= 0 则不生成
     */
    public abstract int shootAmount(MolangVariableScope scope);
}
