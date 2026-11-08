package com.umdc.mercury.api.v1.to;

import java.util.UUID;

/**
 * The folder of an application in the template storage.
 *
 * @param applicationId the application
 * @param folder        its folder, or {@code null} when it has none and its templates are at the root of the storage
 * @param registered    whether the application has a folder registered
 */
public record ApplicationFolderResponse(UUID applicationId, String folder, boolean registered) {
}
