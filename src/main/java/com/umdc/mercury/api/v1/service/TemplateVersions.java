package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.TemplateNotFoundException;
import com.umdc.mercury.api.v1.to.TemplateVersionResponse;
import com.umdc.mercury.jpa.sql.entity.TemplateVersionEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateVersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The history of the content of the templates. Keeps the latest {@code umdc.templates.max-versions} per template.
 */
@Component
public class TemplateVersions {

    /** A normal save. */
    public static final String SAVE = "SAVE";
    /** A restore of an older version. */
    public static final String RESTORE = "RESTORE";
    /** The content a template had before its first edit from the API. */
    public static final String BASELINE = "BASELINE";

    private static final int FIRST_VERSION = 1;

    private final TemplateVersionRepository repository;
    private final int maxVersions;

    public TemplateVersions(TemplateVersionRepository repository, @Value("${umdc.templates.max-versions:50}") int maxVersions) {
        this.repository = repository;
        this.maxVersions = Math.max(1, maxVersions);
    }

    /** @return whether the template has any recorded version. */
    public boolean hasAny(UUID templateId) {
        return repository.findFirstByTemplateIdOrderByVersionDesc(templateId).isPresent();
    }

    /**
     * Records a content as the next version and drops the versions that fall out of the limit.
     *
     * @return the recorded version number
     */
    @Transactional
    public int append(UUID templateId, String content, String etag, String reason, UUID createdBy) {
        int next = repository.findFirstByTemplateIdOrderByVersionDesc(templateId).map(v -> v.getVersion() + 1).orElse(FIRST_VERSION);
        TemplateVersionEntity entity = new TemplateVersionEntity();
        entity.setTemplateId(templateId);
        entity.setVersion(next);
        entity.setEtag(etag);
        entity.setContent(content);
        entity.setSizeBytes(content.getBytes(StandardCharsets.UTF_8).length);
        entity.setReason(reason);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(entity);
        int oldestKept = next - maxVersions + 1;
        if (oldestKept > FIRST_VERSION) {
            repository.deleteByTemplateIdAndVersionLessThan(templateId, oldestKept);
        }
        return next;
    }

    /** @return the versions of a template, newest first, without their content. */
    @Transactional(readOnly = true)
    public List<TemplateVersionResponse> list(UUID templateId) {
        return repository.findByTemplateIdOrderByVersionDesc(templateId).stream().map(v -> toResponse(v, false)).toList();
    }

    /**
     * @return one version with its content
     * @throws TemplateNotFoundException if the template has no such version
     */
    @Transactional(readOnly = true)
    public TemplateVersionResponse get(UUID templateId, int version) {
        return repository.findByTemplateIdAndVersion(templateId, version)
                .map(v -> toResponse(v, true))
                .orElseThrow(() -> new TemplateNotFoundException("Version not found: " + version));
    }

    private static TemplateVersionResponse toResponse(TemplateVersionEntity entity, boolean withContent) {
        return new TemplateVersionResponse(entity.getVersion(), entity.getEtag(), entity.getSizeBytes(), entity.getReason(),
                entity.getCreatedBy(), entity.getCreatedAt(), withContent && Objects.nonNull(entity.getContent()) ? entity.getContent() : null);
    }
}
