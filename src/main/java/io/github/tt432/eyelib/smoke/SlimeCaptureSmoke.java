package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

@ClientSmoke(description = "史莱姆实体渲染到 FBO 截图", priority = 11)
public class SlimeCaptureSmoke {
    public SlimeCaptureSmoke() {
        EntityCaptureBase.captureSingle(
                Minecraft.getInstance(), EntityType.SLIME,
                300, -45, "Slime");
    }
}
