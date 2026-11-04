package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.api.v1.service.TemplateContentService;
import com.umdc.mercury.api.v1.to.PreviewTemplateRequest;
import com.umdc.mercury.api.v1.to.PreviewTemplateResponse;
import com.umdc.mercury.api.v1.to.SaveTemplateContentRequest;
import com.umdc.mercury.api.v1.to.TemplateContentResponse;
import com.umdc.mercury.api.v1.to.TemplateProblemsResponse;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.mercury.template.content.TemplateProblem;
import com.umdc.mercury.template.content.TemplateSyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateContentControllerTest {

    private static final String TOKEN = "session";

    @Mock private TemplateContentService service;
    @Mock private SessionJwtServiceImpl sessionJwtService;
    @InjectMocks private TemplateContentController controller;

    private final UUID id = UUID.randomUUID();
    private final UUID requester = UUID.randomUUID();

    private TemplateContentResponse content(String etag) {
        return new TemplateContentResponse(id, "a.ftl", "FREEMARKER", "x", etag, true, null, false, List.of());
    }

    @BeforeEach
    void setUp() {
        lenient().when(sessionJwtService.getVerifiedUid(TOKEN)).thenReturn(Optional.of(requester));
    }

    @Test
    void getReturnsTheContentWithItsQuotedEtag() {
        when(service.read(id, TOKEN)).thenReturn(content("abc"));

        ResponseEntity<TemplateContentResponse> response = controller.get(id, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"abc\"");
        assertThat(response.getBody().content()).isEqualTo("x");
    }

    @Test
    void anInvalidSessionIsRefused() {
        when(sessionJwtService.getVerifiedUid("bad")).thenReturn(Optional.empty());

        assertThrows(InvalidSessionTokenException.class, () -> controller.get(id, "bad"));
        assertThrows(InvalidSessionTokenException.class, () -> controller.put(id, "\"a\"", null, "bad", new SaveTemplateContentRequest("x")));
        assertThrows(InvalidSessionTokenException.class, () -> controller.preview(
                new PreviewTemplateRequest(id, "a.ftl", "x", Map.of()), "bad"));
        verifyNoInteractions(service);
    }

    @Test
    void putWithIfMatchSavesOverThatEtag() {
        when(service.save(id, "nuevo", "abc", requester, TOKEN)).thenReturn(content("def"));

        ResponseEntity<Object> response = controller.put(id, "\"abc\"", null, TOKEN, new SaveTemplateContentRequest("nuevo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"def\"");
    }

    @Test
    void putWithIfNoneMatchStarCreatesTheFile() {
        when(service.save(id, "nuevo", "", requester, TOKEN)).thenReturn(content("def"));

        assertThat(controller.put(id, null, "*", TOKEN, new SaveTemplateContentRequest("nuevo")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void putWithoutAPreconditionIs428AndWritesNothing() {
        ResponseEntity<Object> response = controller.put(id, null, null, TOKEN, new SaveTemplateContentRequest("x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
        verifyNoInteractions(service);
    }

    @Test
    void putTurnsSyntaxErrorsInto422WithTheProblems() {
        List<TemplateProblem> problems = List.of(new TemplateProblem(2, 3, "unclosed"));
        when(service.save(any(), any(), any(), any(), any())).thenThrow(new TemplateSyntaxException(problems));

        ResponseEntity<Object> response = controller.put(id, "\"abc\"", null, TOKEN, new SaveTemplateContentRequest("<#if x>"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(((TemplateProblemsResponse) response.getBody()).problems()).isEqualTo(problems);
    }

    @Test
    void previewReturnsTheResult() {
        PreviewTemplateRequest request = new PreviewTemplateRequest(id, "a.ftl", "x", Map.of());
        PreviewTemplateResponse result = new PreviewTemplateResponse(true, "FREEMARKER", "x", List.of(), List.of());
        when(service.preview(request, TOKEN)).thenReturn(result);

        assertThat(controller.preview(request, TOKEN).getBody()).isSameAs(result);
        verify(service).preview(request, TOKEN);
    }

    @Test
    void expectedEtagReadsTheHeaders() {
        assertThat(TemplateContentController.expectedEtag("\"abc\"", null)).isEqualTo("abc");
        assertThat(TemplateContentController.expectedEtag("W/\"abc\"", null)).isEqualTo("abc");
        assertThat(TemplateContentController.expectedEtag("abc", null)).isEqualTo("abc");
        assertThat(TemplateContentController.expectedEtag(" \"abc\" ", null)).isEqualTo("abc");
        assertThat(TemplateContentController.expectedEtag(null, " * ")).isEmpty();
        assertThat(TemplateContentController.expectedEtag("\"abc\"", "*")).isEmpty();
        assertThat(TemplateContentController.expectedEtag(null, null)).isNull();
        assertThat(TemplateContentController.expectedEtag("  ", null)).isNull();
        assertThat(TemplateContentController.expectedEtag(null, "\"x\"")).isNull();
    }

    @Test
    void theEtagHeaderIsAStandardOne() {
        assertThat(HttpHeaders.ETAG).isEqualTo("ETag");
    }
}
