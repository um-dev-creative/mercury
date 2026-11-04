package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.TemplateNotFoundException;
import com.umdc.mercury.api.v1.to.PreviewTemplateRequest;
import com.umdc.mercury.api.v1.to.PreviewTemplateResponse;
import com.umdc.mercury.api.v1.to.TemplateContentResponse;
import com.umdc.mercury.api.v1.to.TemplateVersionResponse;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateRepository;
import com.umdc.mercury.template.content.ContentTooLargeException;
import com.umdc.mercury.template.content.TemplatePreview;
import com.umdc.mercury.template.content.TemplateProblem;
import com.umdc.mercury.template.content.TemplateSyntax;
import com.umdc.mercury.template.content.TemplateSyntaxException;
import com.umdc.mercury.template.markdown.MarkdownTemplates;
import com.umdc.mercury.template.storage.StoredTemplate;
import com.umdc.mercury.template.storage.TemplateMetadata;
import com.umdc.mercury.template.storage.TemplateNames;
import com.umdc.mercury.template.storage.TemplateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TemplateContentServiceImpl implements TemplateContentService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateContentServiceImpl.class);

    private final TemplateRepository templateRepository;
    private final TemplateStorage storage;
    private final ApplicationAuthorizationService authorization;
    private final TemplatePreview preview;
    private final String templateManagePermission;
    private final int maxContentBytes;
    private final List<String> protectedPrefixes;
    private final TemplateVersions versions;

    public TemplateContentServiceImpl(TemplateRepository templateRepository,
                                      TemplateStorage storage,
                                      TemplateVersions versions,
                                      ApplicationAuthorizationService authorization,
                                      @Value("${umdc.security.permissions.template-manage:TEMPLATE_MANAGE}") String templateManagePermission,
                                      @Value("${umdc.templates.max-content-bytes:262144}") int maxContentBytes,
                                      @Value("${umdc.templates.protected-prefixes:zb_,sk_,iris_,ds_}") List<String> protectedPrefixes,
                                      @Value("${umdc.templates.preview.max-output-chars:524288}") int previewMaxChars,
                                      @Value("${umdc.templates.preview.timeout-ms:3000}") long previewTimeoutMs) {
        this.templateRepository = templateRepository;
        this.storage = storage;
        this.versions = versions;
        this.authorization = authorization;
        this.templateManagePermission = templateManagePermission;
        this.maxContentBytes = maxContentBytes;
        this.protectedPrefixes = protectedPrefixes.stream().map(String::trim).filter(prefix -> !prefix.isEmpty()).toList();
        this.preview = new TemplatePreview(storage, previewMaxChars, previewTimeoutMs);
    }

    @Override
    public TemplateContentResponse read(UUID templateId, String sessionToken) {
        TemplateEntity template = authorized(templateId, sessionToken);
        String name = fileName(template);
        Optional<StoredTemplate> stored = storage.read(name);
        return response(template, name, stored.map(StoredTemplate::text).orElse(""), stored.map(StoredTemplate::metadata).orElse(null));
    }

    @Override
    public TemplateContentResponse save(UUID templateId, String content, String expectedEtag, boolean confirmSystem, UUID requesterId, String sessionToken) {
        return write(authorized(templateId, sessionToken), content, expectedEtag, confirmSystem, TemplateVersions.SAVE, requesterId);
    }

    @Override
    public List<TemplateVersionResponse> versions(UUID templateId, String sessionToken) {
        authorized(templateId, sessionToken);
        return versions.list(templateId);
    }

    @Override
    public TemplateVersionResponse version(UUID templateId, int version, String sessionToken) {
        authorized(templateId, sessionToken);
        return versions.get(templateId, version);
    }

    @Override
    public TemplateContentResponse restore(UUID templateId, int version, String expectedEtag, boolean confirmSystem, UUID requesterId, String sessionToken) {
        TemplateEntity template = authorized(templateId, sessionToken);
        TemplateVersionResponse old = versions.get(templateId, version);
        return write(template, old.content(), expectedEtag, confirmSystem, TemplateVersions.RESTORE, requesterId);
    }

    private TemplateContentResponse write(TemplateEntity template, String content, String expectedEtag, boolean confirmSystem, String reason, UUID requesterId) {
        String name = fileName(template);
        if (isProtected(name) && !confirmSystem) {
            throw new ForbiddenException("This is a system template other services depend on: confirm the change to save it");
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxContentBytes) {
            throw new ContentTooLargeException(maxContentBytes);
        }
        List<TemplateProblem> problems = TemplateSyntax.validate(name, content);
        if (!problems.isEmpty()) {
            throw new TemplateSyntaxException(problems);
        }
        // the content the template had before the API first changed it stays restorable
        Optional<StoredTemplate> before = storage.read(name);
        TemplateMetadata saved = storage.write(name, bytes, expectedEtag);
        if (before.isPresent() && !versions.hasAny(template.getId())) {
            versions.record(template.getId(), before.get().text(), before.get().metadata().etag(), TemplateVersions.BASELINE, null);
        }
        versions.record(template.getId(), content, saved.etag(), reason, requesterId);
        template.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        templateRepository.save(template);
        logger.info("Template content saved. templateId={}, requesterId={}, etag={}, reason={}", template.getId(), requesterId, saved.etag(), reason);
        return response(template, name, content, saved);
    }

    @Override
    public PreviewTemplateResponse preview(PreviewTemplateRequest request, String sessionToken) {
        authorization.assertPermission(sessionToken, request.applicationId(), templateManagePermission);
        String name = TemplateNames.require(request.name());
        if (request.content().getBytes(StandardCharsets.UTF_8).length > maxContentBytes) {
            throw new ContentTooLargeException(maxContentBytes);
        }
        TemplatePreview.Result result = preview.render(name, request.content(), request.params());
        return new PreviewTemplateResponse(result.ok(), formatOf(name), result.output(), result.problems(),
                TemplateSyntax.parameters(request.content()));
    }

    private TemplateEntity authorized(UUID templateId, String sessionToken) {
        TemplateEntity template = templateRepository.findById(templateId)
                .filter(entity -> !Boolean.FALSE.equals(entity.getActive()))
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
        authorization.assertPermission(sessionToken, template.getApplication().getId(), templateManagePermission);
        return template;
    }

    private static String fileName(TemplateEntity template) {
        String location = template.getLocation();
        if (!TemplateNames.isValid(location)) {
            throw new TemplateNotFoundException("The template has no editable file: its location is not a file name");
        }
        return location;
    }

    private boolean isProtected(String name) {
        return protectedPrefixes.stream().anyMatch(name::startsWith);
    }

    private TemplateContentResponse response(TemplateEntity template, String name, String content, TemplateMetadata metadata) {
        return new TemplateContentResponse(template.getId(), name, formatOf(name), content,
                Objects.isNull(metadata) ? TemplateStorage.ABSENT : metadata.etag(), Objects.nonNull(metadata),
                Objects.isNull(metadata) ? null : metadata.lastModified(), isProtected(name), TemplateSyntax.parameters(content));
    }

    private static String formatOf(String name) {
        return MarkdownTemplates.isMarkdown(name) ? "MARKDOWN" : "FREEMARKER";
    }
}
