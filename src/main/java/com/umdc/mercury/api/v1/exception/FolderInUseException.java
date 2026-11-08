package com.umdc.mercury.api.v1.exception;

/// Thrown when a template folder is already used by another application. Mapped to `409 Conflict` by the global exception handler.
public class FolderInUseException extends RuntimeException {

    /// @param folder the folder that was asked for
    public FolderInUseException(String folder) {
        super("The template folder '" + folder + "' already belongs to another application");
    }

    /// @param folder the folder that was asked for
    /// @param cause  what made the store refuse it
    public FolderInUseException(String folder, Throwable cause) {
        super("The template folder '" + folder + "' already belongs to another application", cause);
    }
}
