package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.service.MessageStatusTypeServiceImpl;
import com.umdc.mercury.api.v1.to.MessageStatusTypeTO;
import com.umdc.mercury.jpa.sql.entity.MessageStatusTypeEntity;
import com.umdc.mercury.jpa.sql.repository.MessageStatusTypeRepository;
import com.umdc.mercury.mapper.MessageStatusTypeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageStatusTypeServiceImplTest {

    @Mock
    private MessageStatusTypeRepository messageStatusTypeRepository;

    @Mock
    private MessageStatusTypeMapper messageStatusTypeMapper;

    @InjectMocks
    private MessageStatusTypeServiceImpl messageStatusTypeServiceImpl;

    @Test
    @DisplayName("Find by name with valid name")
    void findByNameWithValidName() {
        UUID id = UUID.randomUUID();
        String name = "Status Name";
        String description = "Status Description";
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;

        MessageStatusTypeEntity messageStatusTypeEntity = new MessageStatusTypeEntity();
        messageStatusTypeEntity.setId(id);
        messageStatusTypeEntity.setName(name);
        messageStatusTypeEntity.setDescription(description);
        messageStatusTypeEntity.setCreatedAt(now);
        messageStatusTypeEntity.setUpdatedAt(now);
        messageStatusTypeEntity.setActive(active);


        MessageStatusTypeTO messageStatusTypeTO = new MessageStatusTypeTO(
                id, name, description, now, now, active
        );

        when(messageStatusTypeRepository.findByName(anyString())).thenReturn(messageStatusTypeEntity);
        when(messageStatusTypeMapper.toMessageStatusTypeTO(any(MessageStatusTypeEntity.class))).thenReturn(messageStatusTypeTO);

        MessageStatusTypeTO result = messageStatusTypeServiceImpl.findByName(name);

        assertNotNull(result);
        assertEquals(messageStatusTypeTO, result);
        verify(messageStatusTypeRepository).findByName(name);
        verify(messageStatusTypeMapper).toMessageStatusTypeTO(messageStatusTypeEntity);
    }

    @Test
    @DisplayName("Find by name with null name")
    void findByNameWithNullName() {
        assertThrows(IllegalArgumentException.class, () -> messageStatusTypeServiceImpl.findByName(null));
    }

}
