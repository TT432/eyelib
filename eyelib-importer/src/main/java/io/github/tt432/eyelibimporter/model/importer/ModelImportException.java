package io.github.tt432.eyelibimporter.model.importer;

import java.io.IOException;

/** 模型导入过程中抛出的异常。
 * @author TT432 */
@org.jspecify.annotations.NullMarked
/** @author TT432 */
public class ModelImportException extends IOException {
    public ModelImportException(String message) {
        super(message);
    }

    public ModelImportException(String message, Throwable cause) {
        super(message, cause);
    }
}