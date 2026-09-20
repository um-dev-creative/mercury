package com.umdc.mercury.jpa.nosql.repository;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmailMessageNSRepository extends MongoRepository<EmailMessageDocument, String> {
    List<EmailMessageDocument> findByDeliveryStatus(DeliveryStatusType deliveryStatus);

    void deleteByIdEqualsIgnoreCase(String id);

    long deleteByIdEquals(String id);
}
