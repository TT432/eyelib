package io.github.tt432.eyelib.client.model.importer;

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
