package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.MessageStatusTypeTO;
import com.prx.mercury.jpa.sql.repository.MessageStatusTypeRepository;
import com.prx.mercury.mapper.MessageStatusTypeMapper;
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
