package com.umdc.mercury.jpa.nosql.repository;

import com.umdc.mercury.constant.DeliveryStatusType;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.repository.Query;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code messages} collection holds e-mails and the Telegram / SMS / WhatsApp / Push documents together. Each repository must read only its
 * own kind (verified against a real MongoDB: before, a Telegram or SMS document was returned as an e-mail without template and made the e-mail
 * processor fail on every run, and an e-mail made the Telegram dispatch fail). Channel documents carry {@code channelType}; e-mails do not.
 */
class SharedCollectionQueriesTest {

    private static String query(Class<?> repository) throws NoSuchMethodException {
        return repository.getMethod("findByDeliveryStatus", DeliveryStatusType.class).getAnnotation(Query.class).value();
    }

    @Test
    void theEmailRepositoryLeavesOutTheChannelDocuments() throws NoSuchMethodException {
        String q = query(EmailMessageNSRepository.class);
        assertTrue(q.contains("'deliveryStatus': ?0") && q.contains("'channelType': { $exists: false }"), q);
    }

    @Test
    void theChannelRepositoryLeavesOutTheEmails() throws NoSuchMethodException {
        String q = query(MessageNSRepository.class);
        assertTrue(q.contains("'deliveryStatus': ?0") && q.contains("'channelType': { $exists: true }"), q);
    }
}
