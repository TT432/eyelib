package io.github.tt432.eyelib.common.bedrock.particle.pojo;

/**
 * Events<p>
 * 事件<p>
 * Events can be triggered elsewhere in the .json file and fire off new particle and sound effects.<p>
 * 事件可以在.json文件的其他地方被触发，并引发新的粒子和声音效果。
 * <p>
 * Particle effects have different types. If the type is "emitter",
 * this will create an emitter of "effect" type at the event's world position, in a fire-and-forget way.<p>
 * 粒子效果有不同的类型。如果类型是 "发射器"。
 * 这将在事件的世界位置创建一个 "效果 "类型的发射器，以发射和忘记的方式。
 * <p>
 * "emitter_bound" works similarly,
 * except if the spawning emitter is bound to an actor/locator,
 * the new emitter will be bound to the same actor/locator.<p>
 * "emitter_bound "的工作原理与此类似。
 * 但如果产生的发射器被绑定到一个角色/定位器上。
 * 新的发射器将被绑定到同一个角色/定位器。
 * <p>
 * If the type is "particle",
 * then the event will manually emit a particle on an emitter of "effect" type at the event location,
 * creating the emitter if it doesn't already exist (be sure to use minecraft:emitter_rate_manual for the spawned emitter effect).<p>
 * 如果类型是 "粒子"。
 * 那么该事件将在事件地点的 "效果 "类型的发射器上手动发射一个粒子。
 * 如果该发射器不存在，则创建它（请确保使用minecraft:emitter_rate_manual来生成发射器效果）。
 * <p>
 * particle_with_velocity will do the same as particle except the new particle will inherit the spawning particle's velocity.<p>
 * particle_with_velocity的作用与particle相同，只是新粒子将继承产卵粒子的速度。
 * <p>
 * Sound effects specify the specific "level sound event" to be played when the event fires.<p>
 * 声音效果指定特定的 "水平声音事件"，在事件发生时播放。
 * <p>
 * The events themselves consist of an optional node tree and/or an actual event.<p>
 * 事件本身由一个可选的节点树和/或一个实际事件组成。
 * <p>
 * When sequence is specified, that array will execute in order, with every element executing when that event fires.<p>
 * 当指定序列时，该数组将按顺序执行，每个元素在该事件发生时都会执行。
 * <p>
 * When using random, one element will be picked from the array based on the weight.<p>
 * 当使用随机时，将根据权重从数组中挑选一个元素。
 *
 * @author DustW
 */
public class ParticleEvents {
    // TODO: impl
}
