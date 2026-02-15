package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.prx.mercury.mapper.TemplateDefinedMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TemplateDefinedServiceImpl implements TemplateDefinedService {

    private final TemplateDefinedRepository templateDefinedRepository;
    private final TemplateDefinedMapper templateDefinedMapper;

    public TemplateDefinedServiceImpl(TemplateDefinedRepository templateDefinedRepository, TemplateDefinedMapper templateDefinedMapper) {
        this.templateDefinedRepository = templateDefinedRepository;
        this.templateDefinedMapper = templateDefinedMapper;
    }

    @Override
    //    @Cacheable(cacheManager = "templateDefinedCacheManager")
    public TemplateDefinedTO find(UUID templateDefinedId) {
        var optionalTemplateEntity = templateDefinedRepository.findById(templateDefinedId);
        if (optionalTemplateEntity.isEmpty()) {
            throw new IllegalArgumentException("Template defined not found");
        }
        return templateDefinedMapper.toTemplateDefinedTO(optionalTemplateEntity.get());
    }
}
