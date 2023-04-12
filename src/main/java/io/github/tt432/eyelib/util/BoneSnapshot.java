/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.util;

import com.mojang.math.Vector3d;
import io.github.tt432.eyelib.api.bedrock.model.Bone;

public class BoneSnapshot {
    public BoneSnapshot(Bone modelRenderer) {
        rotationValueX = modelRenderer.getRotationX();
        rotationValueY = modelRenderer.getRotationY();
        rotationValueZ = modelRenderer.getRotationZ();

        positionOffsetX = modelRenderer.getPositionX();
        positionOffsetY = modelRenderer.getPositionY();
        positionOffsetZ = modelRenderer.getPositionZ();

        scaleValueX = modelRenderer.getScaleX();
        scaleValueY = modelRenderer.getScaleY();
        scaleValueZ = modelRenderer.getScaleZ();

        this.name = modelRenderer.getName();
    }

    public String name;

    public float scaleValueX;
    public float scaleValueY;
    public float scaleValueZ;

    public float positionOffsetX;
    public float positionOffsetY;
    public float positionOffsetZ;

    public float rotationValueX;
    public float rotationValueY;
    public float rotationValueZ;

    public Vector3d r() {
        return new Vector3d(rotationValueX, rotationValueY, rotationValueZ);
    }

    public Vector3d p() {
        return new Vector3d(positionOffsetX, positionOffsetY, positionOffsetZ);
    }

    public Vector3d s() {
        return new Vector3d(scaleValueX, scaleValueY, scaleValueZ);
    }

    public void apply(Bone bone) {
        bone.setRotationX(rotationValueX);
        bone.setRotationY(rotationValueY);
        bone.setRotationZ(rotationValueZ);

        bone.setPositionX(positionOffsetX);
        bone.setPositionY(positionOffsetY);
        bone.setPositionZ(positionOffsetZ);

        bone.setScaleX(scaleValueX);
        bone.setScaleY(scaleValueY);
        bone.setScaleZ(scaleValueZ);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BoneSnapshot that = (BoneSnapshot) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
