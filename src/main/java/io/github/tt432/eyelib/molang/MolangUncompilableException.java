package io.github.tt432.eyelib.molang;

/**
 * @author TT432
 */
public class MolangUncompilableException extends RuntimeException{
    public MolangUncompilableException() {
    }

    public MolangUncompilableException(String message) {
        super(message);
    }

    public MolangUncompilableException(String message, Throwable cause) {
        super(message, cause);
    }

    public MolangUncompilableException(Throwable cause) {
        super(cause);
    }

    public MolangUncompilableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
