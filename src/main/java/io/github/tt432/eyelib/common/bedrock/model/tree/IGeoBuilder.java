package io.github.tt432.eyelib.common.bedrock.model.tree;

import io.github.tt432.eyelib.common.bedrock.model.pojo.ModelProperties;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoBone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;

public interface IGeoBuilder {
    GeoModel constructGeoModel(RawGeometryTree geometryTree);

    GeoBone constructBone(RawBoneGroup bone, ModelProperties properties, GeoBone parent);
}
