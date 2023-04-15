package io.github.tt432.eyelib.example.client.renderer.armor;

import io.github.tt432.eyelib.common.bedrock.renderer.GeoArmorRenderer;
import io.github.tt432.eyelib.example.client.model.armor.GeckoArmorModel;
import io.github.tt432.eyelib.example.item.GeckoArmorItem;

public class GeckoArmorRenderer extends GeoArmorRenderer<GeckoArmorItem> {
    public GeckoArmorRenderer() {
        super(new GeckoArmorModel());

        // These values are what each bone name is in blockbench. So if your head bone
        // is named "bone545", make sure to do this.headBone = "bone545";
        this.headBone = "armorHead";
        this.bodyBone = "armorBody";
        this.rightArmBone = "armorRightArm";
        this.leftArmBone = "armorLeftArm";
        this.rightLegBone = "armorRightLeg";
        this.leftLegBone = "armorLeftLeg";
        this.rightBootBone = "armorRightBoot";
        this.leftBootBone = "armorLeftBoot";
    }
}
