package com.umdc.mercury.jpa.nosql.repository;

import com.umdc.mercury.jpa.nosql.document.MailTrackingDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface MailTrackingNSRepository extends MongoRepository<MailTrackingDocument, String> {
    Optional<MailTrackingDocument> findByMessageId(UUID messageId);
}
