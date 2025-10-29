package io.github.tt432.eyelib.client.model.transformer;

import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author TT432
 */
public class BedrockTransformer implements ModelTransformer<BrBone, BoneRenderInfos> {
    public static final BedrockTransformer INSTANCE = new BedrockTransformer();

    @Override
    public Vector3fc pivot(BrBone model, BoneRenderInfos data) {
        return model.pivot();
    }

    @Override
    public Vector3fc initPosition(BrBone model, BoneRenderInfos data) {
        return new Vector3f();
    }

    @Override
    public Vector3fc position(BrBone model, BoneRenderInfos data) {
        return data.getData(model.id()).getRenderPosition();
    }

    @Override
    public void position(BrBone model, BoneRenderInfos data, float x, float y, float z) {
        data.getData(model.id()).getRenderPosition().set(x, y, z);
    }

    @Override
    public Vector3fc initRotation(BrBone model, BoneRenderInfos data) {
        return model.rotation();
    }

    @Override
    public Vector3fc rotation(BrBone model, BoneRenderInfos data) {
        return model.rotation().add(data.getData(model.id()).getRenderRotation(), new Vector3f());
    }

    @Override
    public void rotation(BrBone model, BoneRenderInfos data, float x, float y, float z) {
        Vector3f init = model.rotation();
        data.getData(model.id()).getRenderRotation().set(x - init.x, y - init.y, z - init.z);
    }

    @Override
    public Vector3fc initScale(BrBone model, BoneRenderInfos data) {
        return new Vector3f();
    }

    @Override
    public Vector3fc scale(BrBone model, BoneRenderInfos data) {
        return data.getData(model.id()).getRenderScala();
    }

    @Override
    public void scale(BrBone model, BoneRenderInfos data, float x, float y, float z) {
        data.getData(model.id()).getRenderScala().set(x, y, z);
    }
}
