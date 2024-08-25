package io.github.tt432.eyelib.client.model.transformer;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author TT432
 */
public interface ModelTransformer<M extends Model.Bone, R extends ModelRuntimeData<M, ?, R>> {
    default Vector3fc pivot(M model, R data) {
        return new Vector3f();
    }

    Vector3fc initPosition(M model, R data);

    /**
     * init + offset
     */
    Vector3fc position(M model, R data);

    void position(M model, R data, float x, float y, float z);

    default void position(M model, R data, Vector3fc pos) {
        position(model, data, pos.x(), pos.y(), pos.z());
    }

    Vector3fc initRotation(M model, R data);

    /**
     * init + offset
     */
    Vector3fc rotation(M model, R data);

    void rotation(M model, R data, float x, float y, float z);

    default void rotation(M model, R data, Vector3fc rotation) {
        rotation(model, data, rotation.x(), rotation.y(), rotation.z());
    }

    Vector3fc initScale(M model, R data);

    /**
     * init + offset
     */
    Vector3fc scale(M model, R data);

    void scale(M model, R data, float x, float y, float z);

    default void scale(M model, R data, Vector3fc scale) {
        scale(model, data, scale.x(), scale.y(), scale.z());
    }
}
