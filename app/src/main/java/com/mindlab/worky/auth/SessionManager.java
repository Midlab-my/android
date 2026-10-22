package com.mindlab.worky.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.gson.JsonObject;
import com.mindlab.worky.data.CompanyRepository;
import com.mindlab.worky.model.CompanyProfile;

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

    public boolean isAuthenticated() {
        AuthSession session = store.get();
        return session != null && JwtAal.hasAal2(session.accessToken);
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
            return hydrateAccountType(session);
        }
        if (session.refreshToken == null || session.refreshToken.isEmpty()) {
            store.clear();
            return null;
        }
        try {
            AuthSession refreshed = auth.refresh(session.refreshToken);
            if ((refreshed.accountType == null || refreshed.accountType.isEmpty())
                    && session.accountType != null && !session.accountType.isEmpty()) {
                refreshed = new AuthSession(
                        refreshed.accessToken,
                        refreshed.refreshToken,
                        refreshed.expiresAt,
                        refreshed.tokenType,
                        refreshed.userId,
                        refreshed.email,
                        refreshed.name,
                        session.accountType
                );
            }
            store.save(refreshed);
            return hydrateAccountType(refreshed);
        } catch (IOException e) {
            store.clear();
            return null;
        }
    }

    /**
     * Conta da web sem account_type no JWT ainda e empresa se existir company_profiles.
     */
    @WorkerThread
    @NonNull
    public AuthSession hydrateAccountType(@NonNull AuthSession session) {
        if (!JwtAal.hasAal2(session.accessToken)) {
            return session;
        }
        if (session.isCompany() || "candidato".equals(session.accountType)) {
            return session;
        }
        try {
            CompanyProfile profile = new CompanyRepository().fetchCompanyProfile(session);
            if (profile != null) {
                AuthSession patched = session.withAccountType("empresa");
                store.save(patched);
                return patched;
            }
            JsonObject meta = auth.fetchUserMetadata(session);
            String type = "";
            if (meta.has("account_type") && meta.get("account_type").isJsonPrimitive()) {
                type = meta.get("account_type").getAsString();
            } else if (meta.has("accountType") && meta.get("accountType").isJsonPrimitive()) {
                type = meta.get("accountType").getAsString();
            }
            if ("empresa".equalsIgnoreCase(type)) {
                AuthSession patched = session.withAccountType("empresa");
                store.save(patched);
                return patched;
            }
            AuthSession candidato = session.withAccountType("candidato");
            store.save(candidato);
            return candidato;
        } catch (IOException ignored) {
            return session;
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

    @WorkerThread
    @NonNull
    public AuthSession requireAal2Session() throws IOException {
        AuthSession session = requireValidSession();
        if (!JwtAal.hasAal2(session.accessToken)) {
            throw new MfaRequiredException();
        }
        return session;
    }
}
