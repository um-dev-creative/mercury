package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.MessageStatusTypeTO;
import com.umdc.mercury.jpa.sql.repository.MessageStatusTypeRepository;
import com.umdc.mercury.mapper.MessageStatusTypeMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MessageStatusTypeServiceImpl implements MessageStatusTypeService {

    private final MessageStatusTypeRepository messageStatusTypeRepository;
    private final MessageStatusTypeMapper messageStatusTypeMapper;

    public MessageStatusTypeServiceImpl(MessageStatusTypeRepository messageStatusTypeRepository, MessageStatusTypeMapper messageStatusTypeMapper) {
        this.messageStatusTypeRepository = messageStatusTypeRepository;
        this.messageStatusTypeMapper = messageStatusTypeMapper;
    }


    @Override
    public MessageStatusTypeTO findByName(String messageStatusTypeName) {
        if(Objects.isNull(messageStatusTypeName)) {
            throw new IllegalArgumentException("Message status type name is empty");
        }
        return messageStatusTypeMapper.toMessageStatusTypeTO(messageStatusTypeRepository.findByName(messageStatusTypeName));
    }
}
