package io.github.tt432.eyelib.client.gl;

import io.github.tt432.eyelib.client.material.Material;

/**
 * @author TT432
 */
public interface GlState {
    void open(Material material);

    void close(Material material);
}
