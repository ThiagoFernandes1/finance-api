package com.thiago.financeapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Acesso ao usuario autenticado sem espalhar SecurityContextHolder pelos services. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID requireId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Nenhum usuario autenticado no contexto");
        }
        return user.getId();
    }
}
