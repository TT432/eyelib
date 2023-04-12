package io.github.tt432.eyelib.common.bedrock.model.element;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector4f;
import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.common.bedrock.model.pojo.Locator;
import io.github.tt432.eyelib.util.BoneSnapshot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus.AvailableSince;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class GeoBone implements Bone {
    public GeoBone parent;

    public List<GeoBone> childBones = new ObjectArrayList<>();
    public List<GeoCube> childCubes = new ObjectArrayList<>();

    public Map<String, Locator> locators = new HashMap<>();

    public String name;
    private BoneSnapshot initialSnapshot;

    @Setter
    private boolean mirror;
    @Setter
    private Double inflate;
    @Setter
    private Boolean dontRender;
    @Setter
    private boolean hidden;
    @Setter
    private boolean cubesHidden = false;
    @Setter
    private boolean hideChildBonesToo;
    // I still have no idea what this field does, but it's in the json file so
    public Boolean reset;

    @Setter
    private float scaleX = 1;
    @Setter
    private float scaleY = 1;
    @Setter
    private float scaleZ = 1;

    @Setter
    private float positionX;
    @Setter
    private float positionY;
    @Setter
    private float positionZ;

    @Setter
    private float pivotX;
    @Setter
    private float pivotY;
    @Setter
    private float pivotZ;

    @Setter
    private float rotationX;
    @Setter
    private float rotationY;
    @Setter
    private float rotationZ;

    private final Matrix4f modelSpaceXform;
    private final Matrix4f localSpaceXform;
    private final Matrix4f worldSpaceXform;
    private final Matrix3f worldSpaceNormal;

    @Setter
    private boolean trackingXform;
    public final Matrix4f rotMat;

    public GeoBone() {
        modelSpaceXform = new Matrix4f();
        modelSpaceXform.setIdentity();
        localSpaceXform = new Matrix4f();
        localSpaceXform.setIdentity();
        worldSpaceXform = new Matrix4f();
        worldSpaceXform.setIdentity();
        worldSpaceNormal = new Matrix3f();
        worldSpaceNormal.setIdentity();
        trackingXform = false;
        rotMat = null;
    }

    @Override
    public void setModelRendererName(String modelRendererName) {
        this.name = modelRendererName;
    }

    @Override
    public void saveInitialSnapshot() {
        if (this.initialSnapshot == null) {
            this.initialSnapshot = new BoneSnapshot(this);
        }
    }

    // Boilerplate code incoming

    @Override
    public boolean cubesAreHidden() {
        return isCubesHidden();
    }

    @Override
    public boolean childBonesAreHiddenToo() {
        return isHideChildBonesToo();
    }

    @Override
    public void setHidden(boolean selfHidden, boolean skipChildRendering) {
        this.setHidden(selfHidden);
        this.setHideChildBonesToo(skipChildRendering);
    }

    /* Credit to BobMowzie for this section */
    public GeoBone getParent() {
        return parent;
    }

    public Matrix4f getModelSpaceXform() {
        setTrackingXform(true);
        return modelSpaceXform;
    }

    public void setModelSpaceXform(Matrix4f modelSpaceXform) {
        this.modelSpaceXform.load(modelSpaceXform);
    }

    /* Gets the postion of a bone relative to the model */
    @AvailableSince(value = "3.0.42")
    public Vector3d getModelPosition() {
        Matrix4f matrix = getModelSpaceXform();
        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.transform(matrix);
        return new Vector3d(-vec.x() * 16f, vec.y() * 16f, vec.z() * 16f);
    }

    public Matrix4f getLocalSpaceXform() {
        setTrackingXform(true);
        return localSpaceXform;
    }

    public void setLocalSpaceXform(Matrix4f localSpaceXform) {
        this.localSpaceXform.load(localSpaceXform);
    }

    /* Gets the postion of a bone relative to the entity */
    @AvailableSince(value = "3.0.42")
    public Vector3d getLocalPosition() {
        Matrix4f matrix = getLocalSpaceXform();
        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.transform(matrix);
        return new Vector3d(vec.x(), vec.y(), vec.z());
    }

    public Matrix4f getWorldSpaceXform() {
        setTrackingXform(true);
        return worldSpaceXform;
    }

    public void setWorldSpaceXform(Matrix4f worldSpaceXform) {
        this.worldSpaceXform.load(worldSpaceXform);
    }

    /* Gets the postion of a bone relative to the world */
    @AvailableSince(value = "3.0.42")
    public Vector3d getWorldPosition() {
        Matrix4f matrix = getWorldSpaceXform();
        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.transform(matrix);
        return new Vector3d(vec.x(), vec.y(), vec.z());
    }

    public void setModelPosition(Vector3d pos) {
        /* Doesn't work on bones with parent transforms */
        GeoBone parent = getParent();
        Matrix4f identity = new Matrix4f();
        identity.setIdentity();
        Matrix4f matrix = parent == null ? identity : parent.getModelSpaceXform().copy();
        matrix.invert();
        Vector4f vec = new Vector4f(-(float) pos.x / 16f, (float) pos.y / 16f, (float) pos.z / 16f, 1);
        vec.transform(matrix);
        setPosition(-vec.x() * 16f, vec.y() * 16f, vec.z() * 16f);
    }

    public Matrix4f getModelRotationMat() {
        Matrix4f matrix = getModelSpaceXform().copy();
        removeMatrixTranslation(matrix);
        return matrix;
    }

    public static void removeMatrixTranslation(Matrix4f matrix) {
        matrix.m03 = 0;
        matrix.m13 = 0;
        matrix.m23 = 0;
    }

    // Position utils
    public void addPosition(Vector3d vec) {
        addPosition((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void addPosition(float x, float y, float z) {
        addPositionX(x);
        addPositionY(y);
        addPositionZ(z);
    }

    public void addPositionX(float x) {
        setPositionX(getPositionX() + x);
    }

    public void addPositionY(float y) {
        setPositionY(getPositionY() + y);
    }

    public void addPositionZ(float z) {
        setPositionZ(getPositionZ() + z);
    }

    public void setPosition(Vector3d vec) {
        setPosition((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public Vector3d getPosition() {
        return new Vector3d(getPositionX(), getPositionY(), getPositionZ());
    }

    // Rotation utils
    public void addRotation(Vector3d vec) {
        addRotation((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void addRotation(float x, float y, float z) {
        addRotationX(x);
        addRotationY(y);
        addRotationZ(z);
    }

    public void addRotationX(float x) {
        setRotationX(getRotationX() + x);
    }

    public void addRotationY(float y) {
        setRotationY(getRotationY() + y);
    }

    public void addRotationZ(float z) {
        setRotationZ(getRotationZ() + z);
    }

    public void setRotation(Vector3d vec) {
        setRotation((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public Vector3d getRotation() {
        return new Vector3d(getRotationX(), getRotationY(), getRotationZ());
    }

    // Scale utils
    public void multiplyScale(Vector3d vec) {
        multiplyScale((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public void multiplyScale(float x, float y, float z) {
        setScaleX(getScaleX() * x);
        setScaleY(getScaleY() * y);
        setScaleZ(getScaleZ() * z);
    }

    public void setScale(Vector3d vec) {
        setScale((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public Vector3d getScale() {
        return new Vector3d(getScaleX(), getScaleY(), getScaleZ());
    }

    public void addRotationOffsetFromBone(GeoBone source) {
        setRotationX(getRotationX() + source.getRotationX() - source.getInitialSnapshot().rotationValueX);
        setRotationY(getRotationY() + source.getRotationY() - source.getInitialSnapshot().rotationValueY);
        setRotationZ(getRotationZ() + source.getRotationZ() - source.getInitialSnapshot().rotationValueZ);
    }
}
