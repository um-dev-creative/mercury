package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.MessageRecordTO;
import com.prx.mercury.jpa.sql.entity.MessageRecordEntity;
import com.prx.mercury.jpa.sql.repository.MessageRecordRepository;
import com.prx.mercury.mapper.MessageRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class MessageRecordServiceImplTest {

    @Mock
    private MessageRecordRepository messageRecordRepository;

    @Mock
    private MessageRecordMapper messageRecordMapper;

    @InjectMocks
    private MessageRecordServiceImpl messageRecordServiceImpl;

    @Test
    @DisplayName("Create MessageRecordTO with valid data")
    void createMessageRecordTOWithValidData() {
        UUID id = UUID.randomUUID();
        UUID templateDefinedId = UUID.randomUUID();
        String from = "test@example.com";
        String content = "Test content";
        String subject = "Test subject";
        LocalDateTime now = LocalDateTime.now();
        UUID messageStatusTypeId = UUID.randomUUID();
        List<EmailContact> to = List.of(new EmailContact("to@example.com", "nameTo", "aliasTo"));
        List<EmailContact> cc = List.of(new EmailContact("cc@example.com", "nameCC", "aliasCC"));

        MessageRecordTO messageRecordTO = new MessageRecordTO(
                id, templateDefinedId, from, content, subject, now, now, messageStatusTypeId, to, cc
        );
        MessageRecordEntity messageRecordEntity = new MessageRecordEntity();
        MessageRecordEntity savedEntity = new MessageRecordEntity();

        when(messageRecordMapper.toMessageRecordEntity(any(MessageRecordTO.class))).thenReturn(messageRecordEntity);
        when(messageRecordRepository.save(any(MessageRecordEntity.class))).thenReturn(savedEntity);
        when(messageRecordMapper.toMessageRecordTO(any(MessageRecordEntity.class))).thenReturn(messageRecordTO);

        MessageRecordTO result = messageRecordServiceImpl.create(messageRecordTO);

        assertNotNull(result);
        assertEquals(messageRecordTO, result);
        verify(messageRecordMapper).toMessageRecordEntity(messageRecordTO);
        verify(messageRecordRepository).save(messageRecordEntity);
        verify(messageRecordMapper).toMessageRecordTO(savedEntity);
    }
}
