package com.umdc.mercury.jpa.nosql.repository;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface EmailMessageNSRepository extends MongoRepository<EmailMessageDocument, String> {
    /**
     * The e-mails with that status. The {@code messages} collection is shared with the Telegram / SMS / WhatsApp / Push documents
     * (which all carry {@code channelType}); a plain derived query returned them too, mapped as e-mails without a template, and one of them
     * made {@code MessageProcessor} fail on every run, so no e-mail was processed nor its verification code stored.
     */
    @Query("{ 'deliveryStatus': ?0, 'channelType': { $exists: false } }")
    List<EmailMessageDocument> findByDeliveryStatus(DeliveryStatusType deliveryStatus);

    /** The failed e-mail with that message id (the one a retry puts back in the queue); the same collection guard as above. */
    @Query("{ 'messageId': ?0, 'deliveryStatus': 'FAILED', 'channelType': { $exists: false } }")
    List<EmailMessageDocument> findFailedByMessageId(java.util.UUID messageId);

    void deleteByIdEqualsIgnoreCase(String id);

    long deleteByIdEquals(String id);
}
