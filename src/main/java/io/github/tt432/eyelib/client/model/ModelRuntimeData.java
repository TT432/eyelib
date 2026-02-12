package io.github.tt432.eyelib.client.model;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author TT432
 */
public interface ModelRuntimeData<B extends Model.Bone<B>> {
    default Vector3fc pivot(B model) {
        return new Vector3f();
    }

    Vector3fc initPosition(B model);

    /**
     * init + offset
     */
    Vector3fc position(B model);

    void position(B model, float x, float y, float z);

    default void position(B model, Vector3fc pos) {
        position(model, pos.x(), pos.y(), pos.z());
    }

    Vector3fc initRotation(B model);

    /**
     * init + offset
     */
    Vector3fc rotation(B model);

    void rotation(B model, float x, float y, float z);

    default void rotation(B model, Vector3fc rotation) {
        rotation(model, rotation.x(), rotation.y(), rotation.z());
    }

    Vector3fc initScale(B model);

    /**
     * init + offset
     */
    Vector3fc scale(B model);

    void scale(B model, float x, float y, float z);

    default void scale(B model, Vector3fc scale) {
        scale(model, scale.x(), scale.y(), scale.z());
    }
}
