package com.prx.mercury.api.v1.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messaging")
public class ChannelTypeController implements ChannelTypeApi {

    private final ChannelTypeService channelTypeService;

    public ChannelTypeController(ChannelTypeService channelTypeService) {
        this.channelTypeService = channelTypeService;
    }

    @Override
    public ResponseEntity<List<ChannelTypeTO>> getAllChannelTypes() {
        return ResponseEntity.ok(channelTypeService.findAll());
    }

    @Override
    public ResponseEntity<List<ChannelTypeTO>> getEnabledChannelTypes() {
        return ResponseEntity.ok(channelTypeService.findEnabled());
    }

    @Override
    public ResponseEntity<ChannelTypeTO> getChannelTypeByCode(String code) {
        return ResponseEntity.ok(channelTypeService.findByCode(code));
    }

    @Override
    public ResponseEntity<ChannelTypeTO> getChannelTypeById(UUID id) {
        return ResponseEntity.ok(channelTypeService.findById(id));
    }

    @Override
    public ResponseEntity<ChannelTypeTO> createChannelType(ChannelTypeTO channelTypeTO) {
        return ResponseEntity.ok(channelTypeService.create(channelTypeTO));
    }

    @Override
    public ResponseEntity<ChannelTypeTO> updateChannelType(UUID id, ChannelTypeTO channelTypeTO) {
        return ResponseEntity.ok(channelTypeService.update(id, channelTypeTO));
    }

    @Override
    public ResponseEntity<Void> toggleChannelType(UUID id, boolean enabled) {
        channelTypeService.toggleEnabled(id, enabled);
        return ResponseEntity.noContent().build();
    }
}
