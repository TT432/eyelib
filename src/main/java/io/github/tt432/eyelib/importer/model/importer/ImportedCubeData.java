package io.github.tt432.eyelib.importer.model.importer;

import java.util.List;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record ImportedCubeData(
        List<ImportedFaceData> faces
) {
}
