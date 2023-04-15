package io.github.tt432.eyelib.common.bedrock.model.tree;

import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoCube;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.common.bedrock.model.pojo.BoneFile;
import io.github.tt432.eyelib.common.bedrock.model.pojo.CubeFile;
import io.github.tt432.eyelib.common.bedrock.model.pojo.ModelProperties;
import io.github.tt432.eyelib.molang.MolangParser;
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
    public Bone constructBone(RawBoneGroup bone, ModelProperties properties, Bone parent) {
        Bone geoBone = new Bone();

        BoneFile rawBoneFile = bone.selfBone;
        Vector3f rotation = new Vector3f(VectorUtils.fromArray(rawBoneFile.getRotation()));
        Vector3f pivot = rawBoneFile.getPivot().toVec3f(MolangParser.scopeStack.last());
        rotation.mul(-1, -1, 1);

        geoBone.setMirror(rawBoneFile.isMirror());
        geoBone.setDontRender(rawBoneFile.getNeverRender());
        geoBone.reset = rawBoneFile.getReset();
        geoBone.setInflate(rawBoneFile.getInflate());
        geoBone.parent = parent;
        geoBone.setModelRendererName(rawBoneFile.getName());

        geoBone.setRotationX((float) Math.toRadians(rotation.x()));
        geoBone.setRotationY((float) Math.toRadians(rotation.y()));
        geoBone.setRotationZ((float) Math.toRadians(rotation.z()));

        geoBone.setPivotX(-pivot.x());
        geoBone.setPivotY(pivot.y());
        geoBone.setPivotZ(pivot.z());

        if (!ArrayUtils.isEmpty(rawBoneFile.getCubes())) {
            for (CubeFile cubeFile : rawBoneFile.getCubes()) {
                geoBone.childCubes.add(GeoCube.createFromPojoCube(cubeFile, properties,
                        geoBone.getInflate() == null ? null : geoBone.getInflate() / 16, geoBone.isMirror()));
            }
        }

        for (RawBoneGroup child : bone.children.values()) {
            geoBone.childBones.add(constructBone(child, properties, geoBone));
        }

        if (rawBoneFile.getLocators() != null)
            geoBone.locators = rawBoneFile.getLocators();

        return geoBone;
    }
}
