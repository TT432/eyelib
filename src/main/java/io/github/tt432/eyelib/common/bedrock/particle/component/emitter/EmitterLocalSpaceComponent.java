package io.github.tt432.eyelib.common.bedrock.particle.component.emitter;

import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;

/**
 * This component specifies the frame of reference of the emitter. Applies only when the emitter is attached to an entity.<p>
 * When 'position' is true, the particles will simulate in entity space, otherwise they will simulate in world space.<p>
 * Rotation works the same way for rotation.<p>
 * Default is false for both, which makes the particles emit relative to the emitter, then simulate independently from the emitter.<p>
 * Note that rotation = true and position = false is an invalid option.<p>
 * Velocity will add the emitter's velocity to the initial particle velocity.
 * <p>
 * 这个组件指定了发射器的参考框架。只适用于发射器连接到一个实体的情况。<p>
 * 当 "position"为真时，粒子将在实体空间中进行模拟，否则它们将在世界空间中进行模拟。<p>
 * rotation 的工作方式与旋转相同。<p>
 * 两者的默认值都是false，这使得粒子相对于发射器发射，然后独立于发射器进行模拟。<p>
 * 注意，position = true 且 position = false 是一个无效的选项。<p>
 * velocity 将把发射器的速度添加到初始粒子速度中。
 *
 * @author DustW
 */
@ParticleComponentHolder("minecraft:emitter_local_space")
public class EmitterLocalSpaceComponent extends ParticleComponent {
    boolean position;
    boolean rotation;
    boolean velocity;
}
