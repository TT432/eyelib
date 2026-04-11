package io.github.tt432.eyelibimporter.model.importer;

import java.io.IOException;

public class ModelImportException extends IOException {
    public ModelImportException(String message) {
        super(message);
    }

    public ModelImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
