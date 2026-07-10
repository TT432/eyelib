package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

@ClientSmoke(
        description = "蜘蛛实体渲染到 FBO 截图",
        priority = 10
)
public class SpiderCaptureSmoke {
    public SpiderCaptureSmoke() {
        EntityCaptureBase.captureSingle(
                Minecraft.getInstance(), EntityType.SPIDER,
                200, 225, "Spider");
    }
}
