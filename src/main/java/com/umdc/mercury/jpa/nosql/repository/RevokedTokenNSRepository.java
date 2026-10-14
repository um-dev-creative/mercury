package com.umdc.mercury.jpa.nosql.repository;

import com.umdc.mercury.jpa.nosql.document.RevokedTokenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RevokedTokenNSRepository extends MongoRepository<RevokedTokenDocument, String> {
}
