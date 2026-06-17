package io.github.tt432.eyelibimporter.model.importer;

/** 将多纹理的中间模型数据重打包为单纹理图集。
 * @author TT432 */
@org.jspecify.annotations.NullMarked
public final class ImportedModelTextureRepacker {
    private ImportedModelTextureRepacker() {
    }

    public static ImportedModelData repack(ImportedModelData data) {
        if (data.textures().size() <= 1) {
            return data;
        }

        return data.repackTextures();
    }
}