package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.ApplicationFolderResponse;
import com.umdc.mercury.template.ApplicationTemplateFolders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads and sets the folder an application keeps its templates in. Like the rest of the template API it is authorized against the
 * caller's role in that application (backbone-rest's IAM): the template-read permission to look at it, template-manage to change it.
 */
@Service
public class ApplicationTemplateFolderService {

    private final ApplicationTemplateFolders folders;
    private final ApplicationAuthorizationService authorization;
    private final String templateManagePermission;
    private final String templateReadPermission;

    public ApplicationTemplateFolderService(ApplicationTemplateFolders folders,
                                            ApplicationAuthorizationService authorization,
                                            @Value("${umdc.security.permissions.template-manage:TEMPLATE_MANAGE}") String templateManagePermission,
                                            @Value("${umdc.security.permissions.template-read:${umdc.security.permissions.template-manage:TEMPLATE_MANAGE}}")
                                            String templateReadPermission) {
        this.folders = folders;
        this.authorization = authorization;
        this.templateManagePermission = templateManagePermission;
        this.templateReadPermission = templateReadPermission;
    }

    public ApplicationFolderResponse get(UUID applicationId, String sessionToken) {
        authorization.assertPermission(sessionToken, applicationId, templateReadPermission);
        Optional<String> folder = folders.folderOf(applicationId);
        return new ApplicationFolderResponse(applicationId, folder.orElse(null), folder.isPresent());
    }

    public ApplicationFolderResponse assign(UUID applicationId, String folder, String sessionToken) {
        authorization.assertPermission(sessionToken, applicationId, templateManagePermission);
        return new ApplicationFolderResponse(applicationId, folders.assign(applicationId, folder), true);
    }
}
