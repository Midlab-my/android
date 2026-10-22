package com.mindlab.worky.auth;

import java.io.IOException;

/** Sessao aal1. Nao e conta sem perfil: o RLS worky_require_aal2 barra ate o MFA. */
public final class MfaRequiredException extends IOException {

    public MfaRequiredException() {
        super("Confirme o autenticador para continuar.");
    }
}
