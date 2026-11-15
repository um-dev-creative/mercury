package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.MessageRecordTO;
import com.umdc.mercury.jpa.sql.repository.MessageRecordRepository;
import com.umdc.mercury.mapper.MessageRecordMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Implementation of the {@link MessageRecordService} interface.
 * This service is responsible for managing message records in the database.
 * It provides operations to create and persist message records.
 *
 * @version 1.0.0
 * @since 11
 */
@Service
public class MessageRecordServiceImpl implements MessageRecordService {

    private final MessageRecordRepository messageRecordRepository;
    private final MessageRecordMapper messageRecordMapper;

    /**
     * Constructs a MessageRecordServiceImpl with required dependencies.
     *
     * @param messageRecordRepository Repository for database operations on message records
     * @param messageRecordMapper     Mapper for converting between transfer objects and entities
     */
    public MessageRecordServiceImpl(MessageRecordRepository messageRecordRepository, MessageRecordMapper messageRecordMapper) {
        this.messageRecordRepository = messageRecordRepository;
        this.messageRecordMapper = messageRecordMapper;
    }

    /**
     * Creates a new message record in the database.
     *
     * @param messageRecordTO The message record transfer object containing data to be persisted
     * @return The created message record transfer object with populated ID
     * @throws IllegalArgumentException if the provided message record is null
     */
    @Override
    public MessageRecordTO create(MessageRecordTO messageRecordTO) {
        if (Objects.isNull(messageRecordTO)) {
            throw new IllegalArgumentException("Message record is empty");
        }
        final var messageRecordEntity = messageRecordMapper.toMessageRecordEntity(messageRecordTO);
        if (Objects.isNull(messageRecordEntity.getId())) {
            messageRecordEntity.setId(java.util.UUID.randomUUID());   // the id is assigned (see MessageRecordEntity)
        }
        // saveAndFlush: the verification code of this message references the record, and Hibernate treats a record whose INSERT has not run yet
        // (assigned id, same transaction) as a transient instance ("references an unsaved transient instance of MessageRecordEntity")
        final var result = messageRecordRepository.saveAndFlush(messageRecordEntity);

        return messageRecordMapper.toMessageRecordTO(result);
    }
}
