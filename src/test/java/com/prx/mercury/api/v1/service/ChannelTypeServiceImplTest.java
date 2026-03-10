package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.ChannelTypeTO;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import com.prx.mercury.jpa.sql.repository.ChannelTypeRepository;
import com.prx.mercury.mapper.ChannelTypeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ChannelTypeServiceImplTest {

    @Mock
    private ChannelTypeRepository channelTypeRepository;

    @Mock
    private ChannelTypeMapper channelTypeMapper;

    @InjectMocks
    private ChannelTypeServiceImpl channelTypeServiceImpl;

    // ── helpers ──────────────────────────────────────────────────────────────

    private ChannelTypeEntity entity(String code) {
        ChannelTypeEntity e = new ChannelTypeEntity();
        e.setId(UUID.randomUUID());
        e.setCode(code);
        e.setName(code.toUpperCase());
        e.setEnabled(true);
        e.setActive(true);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private ChannelTypeTO toTO(ChannelTypeEntity e) {
        return new ChannelTypeTO(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getIcon(), e.getEnabled(), e.getImplementationClass(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getActive());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns all channel types")
    void findAll_returnsList() {
        ChannelTypeEntity email = entity("email");
        ChannelTypeEntity sms = entity("sms");
        ChannelTypeTO emailTO = toTO(email);
        ChannelTypeTO smsTO = toTO(sms);

        when(channelTypeRepository.findAll()).thenReturn(List.of(email, sms));
        when(channelTypeMapper.toChannelTypeTO(email)).thenReturn(emailTO);
        when(channelTypeMapper.toChannelTypeTO(sms)).thenReturn(smsTO);

        List<ChannelTypeTO> result = channelTypeServiceImpl.findAll();

        assertEquals(2, result.size());
        verify(channelTypeRepository).findAll();
    }

    @Test
    @DisplayName("findAll returns empty list when no channel types exist")
    void findAll_returnsEmptyList() {
        when(channelTypeRepository.findAll()).thenReturn(List.of());

        List<ChannelTypeTO> result = channelTypeServiceImpl.findAll();

        assertTrue(result.isEmpty());
    }

    // ── findEnabled ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findEnabled returns only enabled and active channel types")
    void findEnabled_returnsEnabledList() {
        ChannelTypeEntity email = entity("email");
        ChannelTypeTO emailTO = toTO(email);

        when(channelTypeRepository.findByEnabledTrueAndActiveTrue()).thenReturn(List.of(email));
        when(channelTypeMapper.toChannelTypeTO(email)).thenReturn(emailTO);

        List<ChannelTypeTO> result = channelTypeServiceImpl.findEnabled();

        assertEquals(1, result.size());
        verify(channelTypeRepository).findByEnabledTrueAndActiveTrue();
    }

    // ── findByCode ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByCode returns channel type when found")
    void findByCode_found() {
        ChannelTypeEntity email = entity("email");
        ChannelTypeTO emailTO = toTO(email);

        when(channelTypeRepository.findByCode("email")).thenReturn(Optional.of(email));
        when(channelTypeMapper.toChannelTypeTO(email)).thenReturn(emailTO);

        ChannelTypeTO result = channelTypeServiceImpl.findByCode("email");

        assertNotNull(result);
        assertEquals("email", result.code());
    }

    @Test
    @DisplayName("findByCode throws IllegalArgumentException when code not found")
    void findByCode_notFound() {
        when(channelTypeRepository.findByCode("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> channelTypeServiceImpl.findByCode("unknown"));
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns channel type when found")
    void findById_found() {
        ChannelTypeEntity email = entity("email");
        ChannelTypeTO emailTO = toTO(email);

        when(channelTypeRepository.findById(email.getId())).thenReturn(Optional.of(email));
        when(channelTypeMapper.toChannelTypeTO(email)).thenReturn(emailTO);

        ChannelTypeTO result = channelTypeServiceImpl.findById(email.getId());

        assertNotNull(result);
        assertEquals(email.getId(), result.id());
    }

    @Test
    @DisplayName("findById throws IllegalArgumentException when id not found")
    void findById_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(channelTypeRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> channelTypeServiceImpl.findById(unknownId));
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create persists entity and returns mapped TO")
    void create_success() {
        ChannelTypeEntity email = entity("email");
        ChannelTypeTO emailTO = toTO(email);

        when(channelTypeMapper.toChannelTypeEntity(emailTO)).thenReturn(email);
        when(channelTypeRepository.save(email)).thenReturn(email);
        when(channelTypeMapper.toChannelTypeTO(email)).thenReturn(emailTO);

        ChannelTypeTO result = channelTypeServiceImpl.create(emailTO);

        assertNotNull(result);
        assertEquals("email", result.code());
        verify(channelTypeRepository).save(email);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update modifies existing entity and returns updated TO")
    void update_found() {
        ChannelTypeEntity existing = entity("email");
        ChannelTypeTO updatedTO = new ChannelTypeTO(existing.getId(), "email", "Email Updated",
                "desc", "icon", true, "com.Example", null, null, true);

        when(channelTypeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(channelTypeRepository.save(existing)).thenReturn(existing);
        when(channelTypeMapper.toChannelTypeTO(existing)).thenReturn(updatedTO);

        ChannelTypeTO result = channelTypeServiceImpl.update(existing.getId(), updatedTO);

        assertNotNull(result);
        assertEquals("Email Updated", result.name());
        verify(channelTypeRepository).save(existing);
    }

    @Test
    @DisplayName("update throws IllegalArgumentException when id not found")
    void update_notFound() {
        UUID unknownId = UUID.randomUUID();
        ChannelTypeTO to = new ChannelTypeTO(unknownId, "email", "Email", null, null, true, null, null, null, true);

        when(channelTypeRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> channelTypeServiceImpl.update(unknownId, to));
    }

    // ── toggleEnabled ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleEnabled enables a channel type")
    void toggleEnabled_enable() {
        ChannelTypeEntity email = entity("email");
        email.setEnabled(false);

        when(channelTypeRepository.findById(email.getId())).thenReturn(Optional.of(email));
        when(channelTypeRepository.save(any(ChannelTypeEntity.class))).thenReturn(email);

        channelTypeServiceImpl.toggleEnabled(email.getId(), true);

        assertTrue(email.getEnabled());
        verify(channelTypeRepository).save(email);
    }

    @Test
    @DisplayName("toggleEnabled disables a channel type")
    void toggleEnabled_disable() {
        ChannelTypeEntity email = entity("email");

        when(channelTypeRepository.findById(email.getId())).thenReturn(Optional.of(email));
        when(channelTypeRepository.save(any(ChannelTypeEntity.class))).thenReturn(email);

        channelTypeServiceImpl.toggleEnabled(email.getId(), false);

        assertFalse(email.getEnabled());
        verify(channelTypeRepository).save(email);
    }

    @Test
    @DisplayName("toggleEnabled throws IllegalArgumentException when id not found")
    void toggleEnabled_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(channelTypeRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> channelTypeServiceImpl.toggleEnabled(unknownId, true));
    }
}
