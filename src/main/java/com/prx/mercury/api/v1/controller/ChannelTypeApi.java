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
    @Operation(description = "Get all channel types", operationId = "getAllChannelTypes")
    @GetMapping()
    ResponseEntity<List<ChannelTypeTO>> getAllChannelTypes();

    @Operation(description = "Get enabled channel types only", operationId = "getEnabledChannelTypes")
    @GetMapping("/enabled")
    ResponseEntity<List<ChannelTypeTO>> getEnabledChannelTypes();

    @Operation(description = "Get channel type by code", operationId = "getChannelTypeByCode")
    @GetMapping("/code/{code}")
    ResponseEntity<ChannelTypeTO> getChannelTypeByCode(@PathVariable String code);

    @Operation(description = "Get channel type by ID", operationId = "getChannelTypeById")
    @GetMapping("/{id}")
    ResponseEntity<ChannelTypeTO> getChannelTypeById(@PathVariable UUID id);

    @Operation(description = "Create new channel type", operationId = "createChannelType")
    @PostMapping()
    ResponseEntity<ChannelTypeTO> createChannelType(@RequestBody ChannelTypeTO channelTypeTO);

    @Operation(description = "Update channel type", operationId = "updateChannelType")
    @PutMapping("/{id}")
    ResponseEntity<ChannelTypeTO> updateChannelType(
            @PathVariable UUID id,
            @RequestBody ChannelTypeTO channelTypeTO
    );

    @Operation(description = "Enable/Disable channel type", operationId = "toggleChannelType")
    @PatchMapping("/{id}/toggle")
    ResponseEntity<Void> toggleChannelType(
            @PathVariable UUID id,
            @RequestParam boolean enabled
    );
}
