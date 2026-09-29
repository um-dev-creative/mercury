package com.prx.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.controller.TemplateController;
import com.umdc.mercury.api.v1.service.TemplateSearchCriteria;
import com.umdc.mercury.api.v1.service.TemplateService;
import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateSearchResponse;
import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateController unit tests")
class TemplateControllerTest {

    private static final String SESSION_TOKEN = "session-token-value";

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateController templateController;

    private UUID requesterId;
    private UUID applicationId;
    private MockedStatic<com.umdc.commons.util.JwtUtil> jwtUtilStatic;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        jwtUtilStatic = Mockito.mockStatic(com.umdc.commons.util.JwtUtil.class);
        jwtUtilStatic.when(() -> com.umdc.commons.util.JwtUtil.getUidFromToken(SESSION_TOKEN)).thenReturn(requesterId);
    }

    @AfterEach
    void tearDown() {
        if (jwtUtilStatic != null) {
            jwtUtilStatic.close();
        }
    }

    private TemplateDetailResponse buildDetail(UUID id) {
        LocalDateTime now = LocalDateTime.now();
        return new TemplateDetailResponse(id, "desc", "loc", "HTML", UUID.randomUUID(), "Type",
                applicationId, UUID.randomUUID(), "High", true, now, now);
    }

    @Nested
    @DisplayName("createTemplate")
    class CreateTemplate {

        @Test
        @DisplayName("returns 201 with Location header and delegates the resolved requester id")
        void createTemplate_returns201() {
            UUID newId = UUID.randomUUID();
            TemplateDetailResponse response = buildDetail(newId);
            var request = new CreateTemplateRequest("desc", "loc", "HTML", UUID.randomUUID(), applicationId, UUID.randomUUID());
            when(templateService.createTemplate(eq(request), eq(requesterId), eq(SESSION_TOKEN)))
                    .thenReturn(CompletableFuture.completedFuture(response));

            ResponseEntity<TemplateDetailResponse> result = templateController.createTemplate(request, SESSION_TOKEN);

            assertAll("create response",
                    () -> assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                    () -> assertThat(result.getBody()).isSameAs(response),
                    () -> assertThat(result.getHeaders().getLocation()).hasToString("/api/v1/templates/" + newId)
            );
        }
    }

    @Nested
    @DisplayName("updateTemplate")
    class UpdateTemplate {

        @Test
        @DisplayName("returns 200 with the updated representation")
        void updateTemplate_returns200() {
            UUID id = UUID.randomUUID();
            TemplateDetailResponse response = buildDetail(id);
            var request = new UpdateTemplateRequest("new", null, null, null, null);
            when(templateService.updateTemplate(eq(id), eq(request), eq(requesterId), eq(SESSION_TOKEN))).thenReturn(response);

            ResponseEntity<TemplateDetailResponse> result = templateController.updateTemplate(id, SESSION_TOKEN, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    class DeleteTemplate {

        @Test
        @DisplayName("returns 204 and delegates to the service")
        void deleteTemplate_returns204() {
            UUID id = UUID.randomUUID();

            ResponseEntity<Void> result = templateController.deleteTemplate(id, SESSION_TOKEN);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(templateService).deleteTemplate(id, requesterId, SESSION_TOKEN);
        }
    }

    @Nested
    @DisplayName("getTemplateById")
    class GetTemplateById {

        @Test
        @DisplayName("returns 200 with the mapped representation")
        void getTemplateById_returns200() {
            UUID id = UUID.randomUUID();
            TemplateDetailResponse response = buildDetail(id);
            when(templateService.getById(id, requesterId, SESSION_TOKEN)).thenReturn(response);

            ResponseEntity<TemplateDetailResponse> result = templateController.getTemplateById(id, SESSION_TOKEN);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("searchTemplates")
    class SearchTemplates {

        @Test
        @DisplayName("builds the search criteria from query params and the resolved requester id")
        void searchTemplates_buildsCriteria() {
            TemplateSearchResponse response = new TemplateSearchResponse(List.of(), 0, 20, 0L, 0);
            ArgumentCaptor<TemplateSearchCriteria> captor = ArgumentCaptor.forClass(TemplateSearchCriteria.class);
            when(templateService.searchTemplates(any(TemplateSearchCriteria.class))).thenReturn(response);

            ResponseEntity<TemplateSearchResponse> result = templateController.searchTemplates(
                    applicationId, "welcome", UUID.randomUUID(), UUID.randomUUID(), true, 1, 10, "description,asc", SESSION_TOKEN);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
            verify(templateService).searchTemplates(captor.capture());
            assertAll("criteria",
                    () -> assertThat(captor.getValue().applicationId()).isEqualTo(applicationId),
                    () -> assertThat(captor.getValue().requesterId()).isEqualTo(requesterId),
                    () -> assertThat(captor.getValue().sessionToken()).isEqualTo(SESSION_TOKEN),
                    () -> assertThat(captor.getValue().q()).isEqualTo("welcome"),
                    () -> assertThat(captor.getValue().page()).isEqualTo(1),
                    () -> assertThat(captor.getValue().size()).isEqualTo(10),
                    () -> assertThat(captor.getValue().sort()).isEqualTo("description,asc")
            );
        }
    }
}
