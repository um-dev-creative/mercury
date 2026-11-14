package com.umdc.mercury.security;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Decides who is calling {@code /api/v1/templates/**}. Two callers may manage templates:
 * <ul>
 *   <li>a <b>person</b> (Iris): the request carries the Backbone Bearer of the calling service, which {@code ManagedClientSecurityConfig} already
 *       authenticated, plus the person's {@code session-token}; their rights in each application are checked against Backbone as before;</li>
 *   <li>an <b>application</b> (M2M): no {@code Authorization} header, the Mercury M2M token in {@code session-token}. It needs the Mercury scope of the
 *       call ({@code write} to create, edit or delete; {@code read} or {@code write} to look) and can only touch the application Backbone ties its
 *       client to ({@code ApplicationAuthorizationServiceImpl}).</li>
 * </ul>
 * Anything else, such as a person's session token with no service Bearer, is refused: the Bearer layer of the person's flow must not be skipped by
 * leaving the header out.
 */
@Component
public class TemplateCallerInterceptor implements HandlerInterceptor {

    private final SessionJwtServiceImpl sessionJwtService;

    public TemplateCallerInterceptor(SessionJwtServiceImpl sessionJwtService) {
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof BearerTokenAuthentication && authentication.isAuthenticated()) {
            return true;
        }
        MachineCaller caller = sessionJwtService.getMachineCaller(request.getHeader(SESSION_TOKEN_KEY))
                .orElseThrow(() -> new InvalidSessionTokenException("A service Bearer token or a backend (M2M) token is required"));
        if (!caller.allows(MachineTokenInterceptor.changes(request))) {
            throw new ForbiddenException("The token lacks the Mercury scope this call needs");
        }
        request.setAttribute(MachineTokenInterceptor.CALLER, caller.applicationId().toString());
        request.setAttribute(MachineTokenInterceptor.MACHINE, caller);
        return true;
    }
}
