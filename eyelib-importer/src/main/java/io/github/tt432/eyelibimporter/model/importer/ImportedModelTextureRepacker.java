package io.github.tt432.eyelibimporter.model.importer;

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
