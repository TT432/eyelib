package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

@ClientSmoke(
        description = "苦力怕实体渲染到 FBO 截图",
        priority = 13
)
public class CreeperCaptureSmoke {
    public CreeperCaptureSmoke() {
        EntityCaptureBase.captureSingle(
                Minecraft.getInstance(), EntityType.CREEPER,
                100, 0, "Creeper");
    }
}
