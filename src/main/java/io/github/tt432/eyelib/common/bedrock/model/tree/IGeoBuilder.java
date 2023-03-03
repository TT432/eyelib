package io.github.tt432.eyelib.common.bedrock.model.tree;

import io.github.tt432.eyelib.common.bedrock.model.element.GeoBone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.common.bedrock.model.pojo.ModelProperties;

public interface IGeoBuilder {
    GeoModel constructGeoModel(RawGeometryTree geometryTree);

    GeoBone constructBone(RawBoneGroup bone, ModelProperties properties, GeoBone parent);
}
