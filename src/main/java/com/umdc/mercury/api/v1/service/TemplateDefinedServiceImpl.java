package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.umdc.mercury.mapper.TemplateDefinedMapper;
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
