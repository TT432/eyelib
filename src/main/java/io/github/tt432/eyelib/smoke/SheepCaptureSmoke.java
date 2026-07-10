package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

@ClientSmoke(
        description = "羊实体渲染到 FBO 截图",
        priority = 12
)
public class SheepCaptureSmoke {
    public SheepCaptureSmoke() {
        EntityCaptureBase.captureSingle(
                Minecraft.getInstance(), EntityType.SHEEP,
                100, 30, "Sheep");
    }
}
