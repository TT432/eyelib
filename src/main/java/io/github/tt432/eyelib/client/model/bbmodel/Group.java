package io.github.tt432.eyelib.client.model.bbmodel;

/**
 * @author TT432
 */
public record Group(
        String name,
        double[] origin,
        double[] rotation,
        String uuid,
        boolean export,
        boolean isOpen,
        boolean locked,
        boolean visibility,
        boolean mirror_uv,
        int color,
        int autouv,
        boolean shade
) {
}
