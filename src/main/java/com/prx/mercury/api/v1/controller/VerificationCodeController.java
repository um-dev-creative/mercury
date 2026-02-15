package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.VerificationCodeService;
import com.prx.mercury.api.v1.to.VerificationCodeRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/verification-code")
public class VerificationCodeController implements VerificationCodeApi {

    private final VerificationCodeService verificationCodeService;

    public VerificationCodeController(VerificationCodeService verificationCodeService) {
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    public ResponseEntity<Void> sendVerificationCode(VerificationCodeRequest verificationCodeRequest) {
        return verificationCodeService.confirmCode(verificationCodeRequest);
    }

    @Override
    public ResponseEntity<Boolean> getLatestIsVerifiedStatus(String userId) {
        return verificationCodeService.getLatestIsVerifiedStatus(userId);
    }
}
