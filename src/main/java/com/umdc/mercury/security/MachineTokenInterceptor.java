package com.umdc.mercury.security;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Narrows the pure backend-to-backend endpoints ({@code /api/v1/mail/**}, {@code /api/v1/verification-code/**})
 * to tokens Mercury minted itself for a registered login client ({@link SessionJwtServiceImpl#getMachineClaims}).
 * The generic session interceptor already accepts any trusted token, including an end user's backbone-rest
 * session, which must not be able to make Mercury send mail. The caller (token subject, the login-client alias)
 * is left in the {@link #CALLER} request attribute for idempotency scoping and logs.
 */
@Component
public class MachineTokenInterceptor implements HandlerInterceptor {

    /** Request attribute holding the login-client alias that called. */
    public static final String CALLER = "mercury.caller";

    private final SessionJwtServiceImpl sessionJwtService;

    public MachineTokenInterceptor(SessionJwtServiceImpl sessionJwtService) {
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Claims claims = sessionJwtService.getMachineClaims(request.getHeader(SESSION_TOKEN_KEY))
                .orElseThrow(() -> new InvalidSessionTokenException("A backend (M2M) token is required"));
        request.setAttribute(CALLER, claims.getSubject());
        return true;
    }
}
