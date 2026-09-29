package com.umdc.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds the composed {@link Specification} used to search {@link TemplateEntity} rows.
 *
 * <p>The application scope is always applied — callers must never build a template search
 * without it, since it is the isolation boundary between applications.</p>
 */
public final class TemplateSpecifications {

    private TemplateSpecifications() {
        // Utility class
    }

    /**
     * Composes the search predicate for the given filters. {@code applicationId} is mandatory;
     * every other parameter is optional and, when {@code null}, is left out of the predicate.
     *
     * @param applicationId  mandatory application scope; every result belongs to this application.
     * @param q              optional case-insensitive substring match against description or location.
     * @param templateTypeId optional exact template type filter.
     * @param severityTypeId optional exact severity type filter.
     * @param active         optional exact active-state filter.
     */
    public static Specification<TemplateEntity> search(UUID applicationId, String q, UUID templateTypeId,
                                                         UUID severityTypeId, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("application").get("id"), applicationId));

            if (Objects.nonNull(active)) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (Objects.nonNull(templateTypeId)) {
                predicates.add(cb.equal(root.get("templateType").get("id"), templateTypeId));
            }
            if (Objects.nonNull(severityTypeId)) {
                predicates.add(cb.equal(root.get("severityType").get("id"), severityTypeId));
            }
            if (Objects.nonNull(q) && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("location")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
