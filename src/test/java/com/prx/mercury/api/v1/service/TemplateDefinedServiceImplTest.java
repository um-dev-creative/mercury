package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.service.TemplateDefinedServiceImpl;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.api.v1.to.TemplateTO;
import com.umdc.mercury.api.v1.to.TemplateTypeTO;
import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.umdc.mercury.mapper.TemplateDefinedMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateDefinedServiceImplTest {

    @Mock
    private TemplateDefinedRepository templateDefinedRepository;

    @Mock
    private TemplateDefinedMapper templateDefinedMapper;

    @InjectMocks
    private TemplateDefinedServiceImpl templateDefinedServiceImpl;

    @Test
    @DisplayName("Find TemplateDefinedTO with valid ID")
    void findTemplateDefinedTOWithValidId() {
        UUID templateDefinedId = UUID.randomUUID();
        TemplateDefinedEntity entity = new TemplateDefinedEntity();
        UUID id = UUID.randomUUID();
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "Description", "Location", "File Format", new TemplateTypeTO(UUID.randomUUID(), "Type", "Description", LocalDateTime.now(), LocalDateTime.now(), true), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), true);
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;
        UUID frequencyTypeId = UUID.randomUUID();

        TemplateDefinedTO templateDefinedTO = new TemplateDefinedTO(
                id, template, userId, applicationId, now, now, now.plusDays(1), active, frequencyTypeId
        );

        when(templateDefinedRepository.findById(any(UUID.class))).thenReturn(Optional.of(entity));
        when(templateDefinedMapper.toTemplateDefinedTO(any(TemplateDefinedEntity.class))).thenReturn(templateDefinedTO);

        TemplateDefinedTO result = templateDefinedServiceImpl.find(templateDefinedId);

        assertNotNull(result);
        assertEquals(templateDefinedTO, result);
        verify(templateDefinedRepository).findById(templateDefinedId);
        verify(templateDefinedMapper).toTemplateDefinedTO(entity);
    }

    @Test
    @DisplayName("Find TemplateDefinedTO with invalid ID")
    void findTemplateDefinedTOWithInvalidId() {
        UUID templateDefinedId = UUID.randomUUID();

        when(templateDefinedRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> templateDefinedServiceImpl.find(templateDefinedId));
        verify(templateDefinedRepository).findById(templateDefinedId);
        verify(templateDefinedMapper, never()).toTemplateDefinedTO(any());
    }

}
