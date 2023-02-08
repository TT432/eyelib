package io.github.tt432.eyelib.common.bedrock.particle.pojo;

/**
 * Component Concept<p>
 * 组件概念<p>
 * The particle system is component-based. This means that particle effects are made up of a set of components.
 * For an effect to do something, it needs a component that handles that aspect of the effect.<p>
 * 粒子系统是基于组件的。这意味着，粒子效果是由一组组件组成的。对于一个效果来说，它需要一个组件来处理效果的那个方面。
 * <p>
 * For example, an emitter usually needs to have rules for its lifetime,
 * so the effect should have one or more lifetime components that handle lifetime duties for the emitter and emitted particles.<p>
 * 例如，一个发射器通常需要有关于其寿命的规则，所以效果应该有一个或多个寿命组件来处理发射器和发射的粒子的寿命职责。
 * <p>
 * The idea is that new components can be added later and you can combine components (where it makes sense) to get different behaviors.
 * For example, a particle might have a Dynamic component for moving around, and a Collision component for handling interaction with the terrain.<p>
 * 我们的想法是，以后可以添加新的组件，而且你可以组合组件（在有意义的地方）来获得不同的行为。例如，一个粒子可能有一个动态组件用于移动，还有一个碰撞组件用于处理与地形的交互。
 * <p>
 * Think of components as telling the particle system what you want the emitter or particle to do rather
 * than exposing a list of particle parameters and having to wrangle those parameters to get the desired behavior.<p>
 * 把组件看作是告诉粒子系统你想让发射器或粒子做什么，而不是暴露一个粒子参数的列表，然后不得不处理这些参数来获得所需的行为。
 *
 * @author DustW
 */
public class ParticleComponents {
}
