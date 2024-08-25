package io.github.tt432.eyelib.client.model.transformer;

import io.github.tt432.eyelib.client.model.ModelPartModel;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author TT432
 */
public class ModelPartTransformer implements ModelTransformer<ModelPartModel.Bone, ModelPartModel.Data> {
    public static final ModelPartTransformer INSTANCE = new ModelPartTransformer();

    private static final float POS_MULTIPLIER = 1F / 16F;

    @Override
    public Vector3fc initPosition(ModelPartModel.Bone model, ModelPartModel.Data data) {
        PartPose initialPose = model.modelPart().getInitialPose();
        return new Vector3f(-initialPose.x * POS_MULTIPLIER, initialPose.y * POS_MULTIPLIER, initialPose.z * POS_MULTIPLIER);
    }

    @Override
    public Vector3fc position(ModelPartModel.Bone model, ModelPartModel.Data data) {
        var part = model.modelPart();
        return new Vector3f(-part.x * POS_MULTIPLIER, part.y * POS_MULTIPLIER, part.z * POS_MULTIPLIER);
    }

    @Override
    public void position(ModelPartModel.Bone model, ModelPartModel.Data data, float x, float y, float z) {
        var part = model.modelPart();
        part.setPos(-x * 16, y * 16, z * 16);
    }

    @Override
    public Vector3fc initRotation(ModelPartModel.Bone model, ModelPartModel.Data data) {
        var part = model.modelPart();
        PartPose initialPose = part.getInitialPose();
        return new Vector3f(-initialPose.xRot, -initialPose.yRot, initialPose.zRot);
    }

    @Override
    public Vector3fc rotation(ModelPartModel.Bone model, ModelPartModel.Data data) {
        var part = model.modelPart();
        return new Vector3f(-part.xRot, -part.yRot, part.zRot);
    }

    @Override
    public void rotation(ModelPartModel.Bone model, ModelPartModel.Data data, float x, float y, float z) {
        var part = model.modelPart();
        part.setRotation(-x, -y, z);
    }

    @Override
    public Vector3fc initScale(ModelPartModel.Bone model, ModelPartModel.Data data) {
        return new Vector3f(1, 1, 1);
    }

    @Override
    public Vector3fc scale(ModelPartModel.Bone model, ModelPartModel.Data data) {
        var part = model.modelPart();
        return new Vector3f(part.xScale, part.yScale, part.zScale);
    }

    @Override
    public void scale(ModelPartModel.Bone model, ModelPartModel.Data data, float x, float y, float z) {
        var part = model.modelPart();
        part.xScale = x;
        part.yScale = y;
        part.zScale = z;
    }
}
