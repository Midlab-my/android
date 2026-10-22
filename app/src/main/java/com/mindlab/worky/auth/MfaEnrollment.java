package com.mindlab.worky.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MfaEnrollment {

    public final String factorId;
    public final String qrCode;
    public final String secret;
    public final String uri;

    public MfaEnrollment(
            @NonNull String factorId,
            @Nullable String qrCode,
            @Nullable String secret,
            @Nullable String uri
    ) {
        this.factorId = factorId;
        this.qrCode = qrCode != null ? qrCode : "";
        this.secret = secret != null ? secret : "";
        this.uri = uri != null ? uri : "";
    }
}
