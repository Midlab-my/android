package com.mindlab.worky.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;

/**
 * Centraliza leitura/refresh da sessao. Usar em background thread.
 */
public class SessionManager {

    private final SessionStore store;
    private final SupabaseAuthClient auth;

    public SessionManager(@NonNull Context context) {
        store = new SessionStore(context);
        auth = new SupabaseAuthClient();
    }

    public SessionStore store() {
        return store;
    }

    @Nullable
    public AuthSession peek() {
        return store.get();
    }

    public boolean isLoggedIn() {
        return store.isLoggedIn();
    }

    public void clear() {
        store.clear();
    }

    /**
     * Se houver sessao e ela estiver perto de expirar, renova com refresh_token.
     * Se o refresh falhar, limpa a sessao e retorna null.
     */
    @WorkerThread
    @Nullable
    public AuthSession ensureValidSession() {
        AuthSession session = store.get();
        if (session == null) {
            return null;
        }
        if (!session.isExpired()) {
            return session;
        }
        if (session.refreshToken == null || session.refreshToken.isEmpty()) {
            store.clear();
            return null;
        }
        try {
            AuthSession refreshed = auth.refresh(session.refreshToken);
            store.save(refreshed);
            return refreshed;
        } catch (IOException e) {
            store.clear();
            return null;
        }
    }

    /**
     * Igual a ensureValidSession, mas lanca se nao houver sessao valida.
     */
    @WorkerThread
    @NonNull
    public AuthSession requireValidSession() throws IOException {
        AuthSession session = ensureValidSession();
        if (session == null) {
            throw new IOException("Sessao expirada. Entre de novo.");
        }
        return session;
    }
}
