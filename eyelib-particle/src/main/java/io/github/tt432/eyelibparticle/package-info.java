/**
 * 粒子模块：粒子定义、运行时、组件系统、加载管线、客户端渲染。
 * <p>
 * io.github.tt432.eyelibimporter.particle.BrParticle is the canonical raw Bedrock particle schema/codec owner.
 * io.github.tt432.eyelibparticle.runtime.ParticleDefinition is the canonical module runtime definition owner.
 * The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves
 * mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
 * <p>
 * Phase 12 owns loading/publication through ParticleDefinitionRegistry + ParticleResourcePublication.
 * Phase 13 rewires command/network integration through root/MC adapters only.
 * Phase 14 owns final split verification with stable source tests and JetBrains MCP gates.
 */
@NullMarked
package io.github.tt432.eyelibparticle;

import org.jspecify.annotations.NullMarked;
