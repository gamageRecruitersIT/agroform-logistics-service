package com.example.logistics.security;

import com.example.logistics.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Locale;
import java.util.UUID;

/**
 * Resolves a {@link CurrentUser} controller argument.
 *
 * TEMPORARY SOURCE OF IDENTITY: the API Gateway (Dilmin) validates the JWT and
 * forwards the user id / role as X-User-Id and X-User-Role headers.
 * Navodya's JWT-claims work should replace ONLY the body of resolveArgument()
 * (read the claims from the SecurityContext instead) - nothing else in the
 * tracking module needs to change.
 *
 * The gateway must overwrite these headers on every request, and the service
 * must not be reachable directly from outside, otherwise a caller could spoof them.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String rawId = webRequest.getHeader(USER_ID_HEADER);
        String rawRole = webRequest.getHeader(USER_ROLE_HEADER);

        if (rawId == null || rawId.isBlank() || rawRole == null || rawRole.isBlank()) {
            throw new UnauthorizedException("Missing authenticated user information");
        }

        UUID userId;
        try {
            userId = UUID.fromString(rawId.trim());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid user id");
        }

        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        UserRole role;
        try {
            role = UserRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new com.example.logistics.exception.UnauthorizedRoleException(
                    "Role '" + rawRole + "' is not allowed to use the logistics tracking APIs");
        }
        return new CurrentUser(userId, role);
    }
}
