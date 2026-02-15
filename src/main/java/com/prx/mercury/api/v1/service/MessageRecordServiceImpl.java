package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.MessageRecordTO;
import com.prx.mercury.jpa.sql.repository.MessageRecordRepository;
import com.prx.mercury.mapper.MessageRecordMapper;
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
        final var result = messageRecordRepository.save(messageRecordEntity);

        return messageRecordMapper.toMessageRecordTO(result);
    }
}
