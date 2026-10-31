package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.ChannelTypeService;
import com.umdc.mercury.api.v1.to.ChannelTypeTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelTypeControllerTest {

    private static final UUID ID = UUID.randomUUID();

    private final ChannelTypeService service = mock(ChannelTypeService.class);
    private final ChannelTypeController controller = new ChannelTypeController(service);
    private final ChannelTypeTO channelType = mock(ChannelTypeTO.class);

    @Test
    void getAllReturnsEveryChannelType() {
        when(service.findAll()).thenReturn(List.of(channelType));

        assertEquals(List.of(channelType), okBody(controller.getAllChannelTypes()));
    }

    @Test
    void getEnabledReturnsOnlyWhatTheServiceSelects() {
        when(service.findEnabled()).thenReturn(List.of(channelType));

        assertEquals(List.of(channelType), okBody(controller.getEnabledChannelTypes()));
    }

    @Test
    void getByCodeReturnsTheChannelType() {
        when(service.findByCode("sms")).thenReturn(channelType);

        assertSame(channelType, okBody(controller.getChannelTypeByCode("sms")));
    }

    @Test
    void getByIdReturnsTheChannelType() {
        when(service.findById(ID)).thenReturn(channelType);

        assertSame(channelType, okBody(controller.getChannelTypeById(ID)));
    }

    @Test
    void createReturnsTheStoredChannelType() {
        ChannelTypeTO stored = mock(ChannelTypeTO.class);
        when(service.create(channelType)).thenReturn(stored);

        assertSame(stored, okBody(controller.createChannelType(channelType)));
    }

    @Test
    void updateReturnsTheStoredChannelType() {
        ChannelTypeTO stored = mock(ChannelTypeTO.class);
        when(service.update(ID, channelType)).thenReturn(stored);

        assertSame(stored, okBody(controller.updateChannelType(ID, channelType)));
    }

    @Test
    void toggleAnswersNoContent() {
        assertEquals(HttpStatus.NO_CONTENT, controller.toggleChannelType(ID, true).getStatusCode());

        verify(service).toggleEnabled(ID, true);
    }

    private static <T> T okBody(ResponseEntity<T> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }
}
