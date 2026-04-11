package io.github.tt432.eyelibimporter.model.importer;

import java.util.List;

public record ImportedCubeData(
        List<ImportedFaceData> faces
) {
}
