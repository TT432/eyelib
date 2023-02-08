package io.github.tt432.eyelib.common.bedrock.particle.pojo.curve;

import java.util.List;

/**
 * Curves <p>
 * 曲线 <p>
 * Curves are interpolation values with inputs from 0 to 1, and outputs based on the curve.
 * The result of the curve is a Molang variable of the same name that can be referenced with Molang in components. <p>
 * 曲线是插值，其输入是0到1，输出是基于曲线的。曲线的结果是一个同名的 Molang 变量，可以在组件中用 Molang 进行引用。
 * <p>
 * For each rendering frame of each particle, the curves are evaluated
 * and the result is placed in a Molang variable with the same name as the curve. <p>
 * 对于每个粒子的每一个渲染帧，都会对曲线进行计算，计算的结果会放在一个与曲线同名的 Molang 变量中。
 *
 * @author DustW
 */
public class ParticleCurves {
    List<ParticleCurve> curves;
}
