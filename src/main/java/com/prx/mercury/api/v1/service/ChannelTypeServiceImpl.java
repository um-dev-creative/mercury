package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.ChannelTypeTO;
import com.prx.mercury.jpa.sql.repository.ChannelTypeRepository;
import com.prx.mercury.mapper.ChannelTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChannelTypeServiceImpl implements ChannelTypeService {

    private final ChannelTypeRepository channelTypeRepository;
    private final ChannelTypeMapper channelTypeMapper;
    private static final String CHANNEL_TYPE_NOT_FOUND = "Channel type not found: ";

    public ChannelTypeServiceImpl(
            ChannelTypeRepository channelTypeRepository,
            ChannelTypeMapper channelTypeMapper) {
        this.channelTypeRepository = channelTypeRepository;
        this.channelTypeMapper = channelTypeMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelTypeTO> findAll() {
        return channelTypeRepository.findAll().stream()
                .map(channelTypeMapper::toChannelTypeTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelTypeTO> findEnabled() {
        return channelTypeRepository.findByEnabledTrueAndActiveTrue().stream()
                .map(channelTypeMapper::toChannelTypeTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelTypeTO findByCode(String code) {
        return channelTypeRepository.findByCode(code)
                .map(channelTypeMapper::toChannelTypeTO)
                .orElseThrow(() -> new IllegalArgumentException(CHANNEL_TYPE_NOT_FOUND + code));
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelTypeTO findById(UUID id) {
        return channelTypeRepository.findById(id)
                .map(channelTypeMapper::toChannelTypeTO)
                .orElseThrow(() -> new IllegalArgumentException(CHANNEL_TYPE_NOT_FOUND + id));
    }

    @Override
    public ChannelTypeTO create(ChannelTypeTO channelTypeTO) {
        var entity = channelTypeMapper.toChannelTypeEntity(channelTypeTO);
        var saved = channelTypeRepository.save(entity);
        return channelTypeMapper.toChannelTypeTO(saved);
    }

    @Override
    public ChannelTypeTO update(UUID id, ChannelTypeTO channelTypeTO) {
        var existing = channelTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CHANNEL_TYPE_NOT_FOUND + id));

        existing.setName(channelTypeTO.name());
        existing.setDescription(channelTypeTO.description());
        existing.setIcon(channelTypeTO.icon());
        existing.setEnabled(channelTypeTO.enabled());
        existing.setImplementationClass(channelTypeTO.implementationClass());

        var updated = channelTypeRepository.save(existing);
        return channelTypeMapper.toChannelTypeTO(updated);
    }

    @Override
    public void toggleEnabled(UUID id, boolean enabled) {
        var entity = channelTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CHANNEL_TYPE_NOT_FOUND + id));
        entity.setEnabled(enabled);
        channelTypeRepository.save(entity);
    }
}
