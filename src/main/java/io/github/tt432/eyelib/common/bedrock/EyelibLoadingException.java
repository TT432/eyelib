package io.github.tt432.eyelib.common.bedrock;

import net.minecraft.resources.ResourceLocation;

import java.io.Serial;

public class EyelibLoadingException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EyelibLoadingException(ResourceLocation fileLocation, String message) {
        super(fileLocation + ": " + message);
    }
}
