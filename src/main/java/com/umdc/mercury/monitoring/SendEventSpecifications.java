package com.umdc.mercury.monitoring;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/** The search of the message table. A null application is every application (the monitoring of Mercury as a whole). */
public final class SendEventSpecifications {

    private SendEventSpecifications() {
        // utility class
    }

    /** Every other argument is optional and left out of the predicate when {@code null}. */
    public static Specification<SendEventEntity> search(UUID applicationId, String channel, String status, String kind, UUID templateDefinedId,
                                                        LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (applicationId != null) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (kind != null) {
                predicates.add(cb.equal(root.get("kind"), kind));
            }
            if (templateDefinedId != null) {
                predicates.add(cb.equal(root.get("templateDefinedId"), templateDefinedId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
