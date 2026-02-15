package com.prx.mercury.jpa.nosql.repository;

import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmailMessageNSRepository extends MongoRepository<EmailMessageDocument, String> {
    List<EmailMessageDocument> findByDeliveryStatus(DeliveryStatusType deliveryStatus);

    void deleteByIdEqualsIgnoreCase(String id);

    long deleteByIdEquals(String id);
}
