package com.umdc.mercury.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sets the folder of an application.
 *
 * @param folder lower-case letters, digits, {@code _} and {@code -}, up to 64, starting with a letter or digit
 */
public record AssignFolderRequest(@NotBlank @Size(max = 64) String folder) {
}
