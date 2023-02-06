package io.github.tt432.eyelib.api.bedrock.model;

import io.github.tt432.eyelib.util.BoneSnapshot;

public interface Bone {
    float getRotationX();

    float getRotationY();

    float getRotationZ();

    float getPositionX();

    float getPositionY();

    float getPositionZ();

    float getScaleX();

    float getScaleY();

    float getScaleZ();

    default void setRotation(float x, float y, float z) {
        setRotationX(x);
        setRotationY(y);
        setRotationZ(z);
    }

    void setRotationX(float value);

    void setRotationY(float value);

    void setRotationZ(float value);

    default void setPosition(float x, float y, float z) {
        setPositionX(x);
        setPositionY(y);
        setPositionZ(z);
    }

    void setPositionX(float value);

    void setPositionY(float value);

    void setPositionZ(float value);

    default void setScale(float x, float y, float z) {
        setScaleX(x);
        setScaleY(y);
        setScaleZ(z);
    }

    void setScaleX(float value);

    void setScaleY(float value);

    void setScaleZ(float value);

    void setPivotX(float value);

    void setPivotY(float value);

    void setPivotZ(float value);

    float getPivotX();

    float getPivotY();

    float getPivotZ();

    boolean isHidden();

    boolean cubesAreHidden();

    boolean childBonesAreHiddenToo();

    void setHidden(boolean hidden);

    void setCubesHidden(boolean hidden);

    void setHidden(boolean selfHidden, boolean skipChildRendering);

    void setModelRendererName(String modelRendererName);

    void saveInitialSnapshot();

    BoneSnapshot getInitialSnapshot();

    default BoneSnapshot saveSnapshot() {
        return new BoneSnapshot(this);
    }

    String getName();
}
