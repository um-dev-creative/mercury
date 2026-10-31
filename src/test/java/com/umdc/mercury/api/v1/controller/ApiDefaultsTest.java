package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.ChannelTypeService;
import com.umdc.mercury.api.v1.service.EmailService;
import com.umdc.mercury.api.v1.service.VerificationCodeService;
import com.umdc.mercury.api.v1.to.ChannelTypeTO;
import com.umdc.mercury.api.v1.to.MailStatusResponse;
import com.umdc.mercury.api.v1.to.VerificationCodeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** An endpoint or service left unimplemented must fail visibly rather than answer as if it worked. */
class ApiDefaultsTest {

    private static final UUID ID = UUID.randomUUID();

    private final ChannelTypeService channelTypes = new ChannelTypeService() {
    };
    private final VerificationCodeRequest codeRequest = mock(VerificationCodeRequest.class);

    @Test
    void channelTypeServiceRejectsEveryUnimplementedOperation() {
        ChannelTypeTO channelType = mock(ChannelTypeTO.class);

        assertThrows(UnsupportedOperationException.class, channelTypes::findAll);
        assertThrows(UnsupportedOperationException.class, channelTypes::findEnabled);
        assertThrows(UnsupportedOperationException.class, () -> channelTypes.findByCode("sms"));
        assertThrows(UnsupportedOperationException.class, () -> channelTypes.findById(ID));
        assertThrows(UnsupportedOperationException.class, () -> channelTypes.create(channelType));
        assertThrows(UnsupportedOperationException.class, () -> channelTypes.update(ID, channelType));
        assertThrows(UnsupportedOperationException.class, () -> channelTypes.toggleEnabled(ID, true));
    }

    @Test
    void defaultMailApiAnswersNotImplemented() {
        MailApi api = new MailApi() {
        };

        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.status(ID, "caller").getStatusCode());
    }

    @Test
    void mailControllerDelegatesStatusToTheService() {
        EmailService service = mock(EmailService.class);
        ResponseEntity<MailStatusResponse> answer = ResponseEntity.ok().build();
        when(service.mailStatus(ID, "caller")).thenReturn(answer);

        assertSame(answer, new MailController(service).status(ID, "caller"));
    }

    @Test
    void defaultVerificationCodeApiRejectsTheRequest() {
        VerificationCodeApi api = new VerificationCodeApi() {
            @Override
            public ResponseEntity<Boolean> getLatestIsVerifiedStatus(String userId) {
                return ResponseEntity.ok(true);
            }
        };

        assertThrows(UnsupportedOperationException.class, () -> api.sendVerificationCode(codeRequest));
    }

    @Test
    void verificationCodeServiceRejectsUnimplementedStatusLookup() {
        VerificationCodeService service = new VerificationCodeService() {
        };

        assertThrows(UnsupportedOperationException.class, () -> service.getLatestIsVerifiedStatus("user"));
    }

    @Test
    void verificationCodeControllerDelegatesToTheService() {
        VerificationCodeService service = mock(VerificationCodeService.class);
        ResponseEntity<Void> confirmed = ResponseEntity.noContent().build();
        ResponseEntity<Boolean> status = ResponseEntity.ok(true);
        when(service.confirmCode(codeRequest)).thenReturn(confirmed);
        when(service.getLatestIsVerifiedStatus("user")).thenReturn(status);
        VerificationCodeController controller = new VerificationCodeController(service);

        assertSame(confirmed, controller.sendVerificationCode(codeRequest));
        assertSame(status, controller.getLatestIsVerifiedStatus("user"));
    }
}
