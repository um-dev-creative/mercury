package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.to.ReferenceItemResponse;
import com.umdc.mercury.api.v1.to.TemplateDefinedItemResponse;
import com.umdc.mercury.jpa.sql.entity.SeverityTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateTypeEntity;
import com.umdc.mercury.jpa.sql.repository.SeverityTypeRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateTypeEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateReferenceServiceImplTest {

    private static final String TOKEN = "session";

    @Mock private TemplateTypeEntityRepository templateTypes;
    @Mock private SeverityTypeRepository severityTypes;
    @Mock private TemplateDefinedRepository defined;
    @Mock private ApplicationAuthorizationService authorization;

    private TemplateReferenceServiceImpl service;
    private final UUID applicationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TemplateReferenceServiceImpl(templateTypes, severityTypes, defined, authorization, "TEMPLATE_READ");
    }

    private TemplateTypeEntity type(String name, Boolean active) {
        TemplateTypeEntity entity = new TemplateTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setActive(active);
        return entity;
    }

    private SeverityTypeEntity severity(String name, Boolean active) {
        SeverityTypeEntity entity = new SeverityTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setActive(active);
        return entity;
    }

    private TemplateDefinedEntity definition(String description, Boolean templateActive, TemplateTypeEntity templateType) {
        TemplateEntity template = new TemplateEntity();
        template.setId(UUID.randomUUID());
        template.setDescription(description);
        template.setLocation(description + ".ftl");
        template.setFileFormat("FTL");
        template.setActive(templateActive);
        template.setTemplateType(templateType);
        TemplateDefinedEntity entity = new TemplateDefinedEntity();
        entity.setId(UUID.randomUUID());
        entity.setTemplate(template);
        return entity;
    }

    @Test
    void templateTypesAreActiveOnesSortedByName() {
        when(templateTypes.findAll()).thenReturn(List.of(type("welcome", true), type("Alert", true), type("old", false), type("nullFlag", null)));

        List<ReferenceItemResponse> result = service.listTemplateTypes();

        assertThat(result).extracting(ReferenceItemResponse::name).containsExactly("Alert", "nullFlag", "welcome");
    }

    @Test
    void severityTypesAreActiveOnesSortedByName() {
        when(severityTypes.findAll()).thenReturn(List.of(severity("WARN", true), severity("INFO", true), severity("gone", false)));

        assertThat(service.listSeverityTypes()).extracting(ReferenceItemResponse::name).containsExactly("INFO", "WARN");
    }

    @Test
    void definedChecksThePermissionThenListsActiveTemplatesSorted() {
        TemplateTypeEntity verification = type("VERIFICATION", true);
        when(defined.findByApplicationIdAndIsActiveTrue(applicationId)).thenReturn(List.of(
                definition("zeta", true, verification),
                definition("Alpha", true, null),
                definition("inactive base", false, verification)));

        List<TemplateDefinedItemResponse> result = service.listDefined(applicationId, TOKEN);

        verify(authorization).assertPermission(TOKEN, applicationId, "TEMPLATE_READ");
        assertThat(result).extracting(TemplateDefinedItemResponse::description).containsExactly("Alpha", "zeta");
        assertThat(result.get(0).templateTypeName()).isNull();
        assertThat(result.get(1).templateTypeName()).isEqualTo("VERIFICATION");
        assertThat(result.get(1).location()).isEqualTo("zeta.ftl");
    }

    @Test
    void definedSkipsAnEntryWithoutTemplate() {
        TemplateDefinedEntity orphan = new TemplateDefinedEntity();
        orphan.setId(UUID.randomUUID());
        when(defined.findByApplicationIdAndIsActiveTrue(applicationId)).thenReturn(List.of(orphan));

        assertThat(service.listDefined(applicationId, TOKEN)).isEmpty();
    }

    @Test
    void definedRequiresAnApplication() {
        assertThrows(IllegalArgumentException.class, () -> service.listDefined(null, TOKEN));
        verifyNoInteractions(authorization, defined);
    }

    @Test
    void definedFailsClosedWhenThePermissionIsDenied() {
        doThrow(new ForbiddenException("no")).when(authorization).assertPermission(eq(TOKEN), eq(applicationId), any());

        assertThrows(ForbiddenException.class, () -> service.listDefined(applicationId, TOKEN));
        verifyNoInteractions(defined);
    }
}
