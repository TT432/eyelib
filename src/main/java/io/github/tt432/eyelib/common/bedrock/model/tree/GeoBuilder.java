package io.github.tt432.eyelib.common.bedrock.model.tree;

import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoBone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoCube;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.common.bedrock.model.pojo.Bone;
import io.github.tt432.eyelib.common.bedrock.model.pojo.Cube;
import io.github.tt432.eyelib.common.bedrock.model.pojo.ModelProperties;
import io.github.tt432.eyelib.util.VectorUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Map;

public class GeoBuilder implements IGeoBuilder {

    private static final Map<String, IGeoBuilder> moddedGeoBuilders = new Object2ObjectOpenHashMap<>();
    private static final IGeoBuilder defaultBuilder = new GeoBuilder();

    public static void registerGeoBuilder(String modID, IGeoBuilder builder) {
        moddedGeoBuilders.put(modID, builder);
    }

    public static IGeoBuilder getGeoBuilder(String modID) {
        IGeoBuilder builder = moddedGeoBuilders.get(modID);
        return builder == null ? defaultBuilder : builder;
    }

    @Override
    public GeoModel constructGeoModel(RawGeometryTree geometryTree) {
        GeoModel model = new GeoModel();
        model.properties = geometryTree.properties;
        for (RawBoneGroup rawBone : geometryTree.topLevelBones.values()) {
            model.topLevelBones.add(this.constructBone(rawBone, geometryTree.properties, null));
        }
        return model;
    }

    @Override
    public GeoBone constructBone(RawBoneGroup bone, ModelProperties properties, GeoBone parent) {
        GeoBone geoBone = new GeoBone();

        Bone rawBone = bone.selfBone;
        Vector3f rotation = new Vector3f(VectorUtils.fromArray(rawBone.getRotation()));
        Vector3f pivot = new Vector3f(VectorUtils.fromArray(rawBone.getPivot()));
        rotation.mul(-1, -1, 1);

        geoBone.mirror = rawBone.getMirror();
        geoBone.dontRender = rawBone.getNeverRender();
        geoBone.reset = rawBone.getReset();
        geoBone.inflate = rawBone.getInflate();
        geoBone.parent = parent;
        geoBone.setModelRendererName(rawBone.getName());

        geoBone.setRotationX((float) Math.toRadians(rotation.x()));
        geoBone.setRotationY((float) Math.toRadians(rotation.y()));
        geoBone.setRotationZ((float) Math.toRadians(rotation.z()));

        geoBone.rotationPointX = -pivot.x();
        geoBone.rotationPointY = pivot.y();
        geoBone.rotationPointZ = pivot.z();

        if (!ArrayUtils.isEmpty(rawBone.getCubes())) {
            for (Cube cube : rawBone.getCubes()) {
                geoBone.childCubes.add(GeoCube.createFromPojoCube(cube, properties,
                        geoBone.inflate == null ? null : geoBone.inflate / 16, geoBone.mirror));
            }
        }

        for (RawBoneGroup child : bone.children.values()) {
            geoBone.childBones.add(constructBone(child, properties, geoBone));
        }

        return geoBone;
    }
}
