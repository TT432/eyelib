package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.material.Material;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaterialManager extends Manager<Material> {
    public static final MaterialManager INSTANCE = new MaterialManager();
}
