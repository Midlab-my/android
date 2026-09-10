package com.mindlab.worky.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SessionStore {

    private static final String PREFS = "worky_auth";
    private static final String K_ACCESS = "access_token";
    private static final String K_REFRESH = "refresh_token";
    private static final String K_EXPIRES = "expires_at";
    private static final String K_TYPE = "token_type";
    private static final String K_USER_ID = "user_id";
    private static final String K_EMAIL = "email";
    private static final String K_NAME = "name";

    private final SharedPreferences prefs;

    public SessionStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(@NonNull AuthSession session) {
        prefs.edit()
                .putString(K_ACCESS, session.accessToken)
                .putString(K_REFRESH, session.refreshToken)
                .putLong(K_EXPIRES, session.expiresAt)
                .putString(K_TYPE, session.tokenType)
                .putString(K_USER_ID, session.userId)
                .putString(K_EMAIL, session.email)
                .putString(K_NAME, session.name)
                .apply();
    }

    @Nullable
    public AuthSession get() {
        String access = prefs.getString(K_ACCESS, null);
        String refresh = prefs.getString(K_REFRESH, null);
        if (access == null || access.isEmpty() || refresh == null || refresh.isEmpty()) {
            return null;
        }
        return new AuthSession(
                access,
                refresh,
                prefs.getLong(K_EXPIRES, 0L),
                prefs.getString(K_TYPE, "bearer"),
                prefs.getString(K_USER_ID, ""),
                prefs.getString(K_EMAIL, ""),
                prefs.getString(K_NAME, "")
        );
    }

    public boolean isLoggedIn() {
        AuthSession session = get();
        return session != null && !session.accessToken.isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
