package com.mindcash.app.util;

import com.mindcash.app.config.AppUserDetails;
import com.mindcash.app.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails details) {
            return details.getUser();
        }
        throw new IllegalStateException("Usuário não autenticado");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
