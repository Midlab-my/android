package com.mindlab.worky.auth;

import androidx.annotation.Nullable;

public class AuthSession {

    public final String accessToken;
    public final String refreshToken;
    public final long expiresAt;
    public final String tokenType;
    public final String userId;
    public final String email;
    public final String name;
    /** candidato | empresa (metadata account_type). */
    public final String accountType;

    public AuthSession(
            String accessToken,
            String refreshToken,
            long expiresAt,
            String tokenType,
            String userId,
            String email,
            String name
    ) {
        this(accessToken, refreshToken, expiresAt, tokenType, userId, email, name, "");
    }

    public AuthSession(
            String accessToken,
            String refreshToken,
            long expiresAt,
            String tokenType,
            String userId,
            String email,
            String name,
            String accountType
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.tokenType = tokenType != null ? tokenType : "bearer";
        this.userId = userId != null ? userId : "";
        this.email = email != null ? email : "";
        this.name = name != null ? name : "";
        this.accountType = accountType != null ? accountType.trim().toLowerCase() : "";
    }

    public boolean isCompany() {
        return "empresa".equals(accountType);
    }

    public boolean isExpired() {
        // margem de 60s
        return expiresAt > 0 && (System.currentTimeMillis() / 1000L) >= (expiresAt - 60);
    }

    @Nullable
    public String displayName() {
        if (name != null && !name.trim().isEmpty()) return name.trim();
        if (email != null && email.contains("@")) return email.substring(0, email.indexOf('@'));
        return email;
    }
}
