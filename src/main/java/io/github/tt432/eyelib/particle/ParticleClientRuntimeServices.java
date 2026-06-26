package io.github.tt432.eyelib.particle;

/**
 * 客户端线程提交 Port，由 bridge 实现具体的线程适配。
 */
@FunctionalInterface
/** @author TT432 */
public interface ParticleClientRuntimeServices {
    void submit(Runnable action);

    static ParticleClientRuntimeServices immediate() {
        return Runnable::run;
    }
}
