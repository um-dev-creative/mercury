package com.umdc.mercury.security;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Narrows the pure backend-to-backend endpoints ({@code /api/v1/mail/**}, {@code /api/v1/messages/**}, {@code /api/v1/verification-code/**}, Telegram)
 * to the Mercury M2M token an application obtained by exchanging its Backbone token ({@code POST /api/v1/auth/token}). The generic session interceptor
 * already accepts any trusted token, including an end user's backbone-rest session, which must not be able to make Mercury send mail.
 * <p>
 * The token must hold the Mercury scope the call needs: {@code mercury:message:write} to change anything, {@code read} or {@code write} to look
 * ({@code GET}/{@code HEAD}). The caller (the <em>application</em> Backbone ties the client to) is left in the {@link #CALLER} request attribute for
 * idempotency scoping and logs, and the {@link MachineCaller} in {@link #MACHINE}.
 * </p>
 */
@Component
public class MachineTokenInterceptor implements HandlerInterceptor {

    /** Request attribute holding the id (a UUID, as text) of the application that called. */
    public static final String CALLER = "mercury.caller";
    /** Request attribute holding the {@link MachineCaller}. */
    public static final String MACHINE = "mercury.machine";

    private final SessionJwtServiceImpl sessionJwtService;

    public MachineTokenInterceptor(SessionJwtServiceImpl sessionJwtService) {
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        MachineCaller caller = sessionJwtService.getMachineCaller(request.getHeader(SESSION_TOKEN_KEY))
                .orElseThrow(() -> new InvalidSessionTokenException("A backend (M2M) token is required"));
        if (!caller.allows(changes(request))) {
            throw new ForbiddenException("The token lacks the Mercury scope this call needs");
        }
        request.setAttribute(CALLER, caller.applicationId().toString());
        request.setAttribute(MACHINE, caller);
        return true;
    }

    /** Whether the request changes state: anything but {@code GET}/{@code HEAD}/{@code OPTIONS}. */
    static boolean changes(HttpServletRequest request) {
        String method = request.getMethod();
        return !("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method));
    }
}
