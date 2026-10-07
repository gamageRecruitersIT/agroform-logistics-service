package com.example.logistics.security;

import com.example.logistics.exception.UnauthorizedException;
import com.example.logistics.exception.UnauthorizedRoleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * Identity source, in order (task list: "Prefer JWT claims first"):
 *  1. The JWT - JwtAuthenticationFilter (Navodya) puts the subject (user id) and a ROLE_<role>
 *     authority into the SecurityContext.
 *  2. X-User-Id / X-User-Role headers - ONLY when logistics.security.allow-header-auth=true
 *     (local Postman testing / a gateway that validates the JWT itself). Off by default because
 *     a client could spoof these headers if the service is reachable directly.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private final boolean allowHeaderAuth;

    public CurrentUserArgumentResolver(
            @Value("${logistics.security.allow-header-auth:false}") boolean allowHeaderAuth) {
        this.allowHeaderAuth = allowHeaderAuth;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String rawId = null;
        String rawRole = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            rawId = auth.getName();
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority() != null && authority.getAuthority().startsWith("ROLE_")) {
                    rawRole = authority.getAuthority();
                    break;
                }
            }
        } else if (allowHeaderAuth) {
            rawId = webRequest.getHeader(USER_ID_HEADER);
            rawRole = webRequest.getHeader(USER_ROLE_HEADER);
        }

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
        while (normalized.startsWith("ROLE_")) {          // tolerate ROLE_ROLE_X if the token already had a prefix
            normalized = normalized.substring(5);
        }
        UserRole role;
        try {
            role = UserRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedRoleException(
                    "Role '" + rawRole + "' is not allowed to use the logistics tracking APIs");
        }
        return new CurrentUser(userId, role);
    }
}
