/**
 * three.js r134 最小子集逐字移植（wintersky 的渲染/数学依赖层）。
 *
 * <p>仅包含 wintersky 触达的类与方法；语义（含 Euler↔Quaternion onChange 联动、
 * Float32Array 精度截断、lookAt/updateWorldMatrix 行为）与 three r134 逐字对齐。
 * 不含 WebGL 渲染后端：Mesh/ShaderMaterial/Texture 仅作场景图状态。
 */
@NullMarked
package io.github.tt432.eyelib.wintersky.three;

import org.jspecify.annotations.NullMarked;
