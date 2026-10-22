package com.mindlab.worky.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class TotpFactor {

    public final String id;
    public final String friendlyName;
    public final String factorType;
    public final String status;

    public TotpFactor(
            @NonNull String id,
            @Nullable String friendlyName,
            @Nullable String factorType,
            @Nullable String status
    ) {
        this.id = id;
        this.friendlyName = friendlyName != null ? friendlyName : "";
        this.factorType = factorType != null ? factorType : "";
        this.status = status != null ? status.trim().toLowerCase() : "";
    }

    public boolean isTotp() {
        return "totp".equalsIgnoreCase(factorType);
    }

    public boolean isVerified() {
        return "verified".equals(status);
    }

    public boolean isUnverified() {
        return "unverified".equals(status);
    }

    @NonNull
    public String label() {
        return friendlyName.isEmpty() ? id : friendlyName;
    }
}
