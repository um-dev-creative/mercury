package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.to.ChannelTypeTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "channel-types", description = "Channel Type Management API")
public interface ChannelTypeApi {
    @Operation(description = "Get all channel types")
    @GetMapping("/channel-types")
    ResponseEntity<List<ChannelTypeTO>> getAllChannelTypes();

    @Operation(description = "Get enabled channel types only")
    @GetMapping("/channel-types/enabled")
    ResponseEntity<List<ChannelTypeTO>> getEnabledChannelTypes();

    @Operation(description = "Get channel type by code")
    @GetMapping("/channel-types/code/{code}")
    ResponseEntity<ChannelTypeTO> getChannelTypeByCode(@PathVariable String code);

    @Operation(description = "Get channel type by ID")
    @GetMapping("/channel-types/{id}")
    ResponseEntity<ChannelTypeTO> getChannelTypeById(@PathVariable UUID id);

    @Operation(description = "Create new channel type")
    @PostMapping("/channel-types")
    ResponseEntity<ChannelTypeTO> createChannelType(@RequestBody ChannelTypeTO channelTypeTO);

    @Operation(description = "Update channel type")
    @PutMapping("/channel-types/{id}")
    ResponseEntity<ChannelTypeTO> updateChannelType(
            @PathVariable UUID id,
            @RequestBody ChannelTypeTO channelTypeTO
    );

    @Operation(description = "Enable/Disable channel type")
    @PatchMapping("/channel-types/{id}/toggle")
    ResponseEntity<Void> toggleChannelType(
            @PathVariable UUID id,
            @RequestParam boolean enabled
    );
}
