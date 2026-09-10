package com.mindlab.worky.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mindlab.worky.network.SupabaseConfig;
import com.mindlab.worky.util.BrDocs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SupabaseAuthClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;

    public SupabaseAuthClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    @NonNull
    public AuthSession signIn(@NonNull String email, @NonNull String password) throws IOException {
        assertConfigured();

        JsonObject body = new JsonObject();
        body.addProperty("email", email.trim());
        body.addProperty("password", password);

        Request request = new Request.Builder()
                .url(SupabaseConfig.url() + "/auth/v1/token?grant_type=password")
                .post(RequestBody.create(body.toString(), JSON))
                .header("apikey", SupabaseConfig.anonKey())
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = readBody(response);
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw, "Falha no login."));
            }
            return parseSession(raw, false);
        }
    }

    /**
     * Cadastro candidato. Se o projeto exigir confirmacao de email, retorna sem tokens.
     * @return sessao se ja autenticou; null se precisa confirmar email
     */
    @Nullable
    public AuthSession signUp(@NonNull String name, @NonNull String email, @NonNull String password) throws IOException {
        return signUp(name, email, password, "candidato", null, null, null, null, null);
    }

    @Nullable
    public AuthSession signUp(
            @NonNull String name,
            @NonNull String email,
            @NonNull String password,
            @NonNull String accountType,
            @Nullable String companySize,
            @Nullable String companyCnpj,
            @Nullable String companyLocation,
            @Nullable String companySector,
            @Nullable String companyLinkedin
    ) throws IOException {
        assertConfigured();

        JsonObject data = new JsonObject();
        data.addProperty("full_name", name.trim());
        data.addProperty("name", name.trim());
        data.addProperty("account_type", accountType);
        if ("empresa".equals(accountType)) {
            data.addProperty("company_size", companySize == null ? "" : companySize.trim());
            data.addProperty("company_cnpj", companyCnpj == null ? "" : companyCnpj.trim());
            data.addProperty("company_location", companyLocation == null ? "" : companyLocation.trim());
            data.addProperty("company_sector", companySector == null ? "" : companySector.trim());
            data.addProperty("company_linkedin", companyLinkedin == null ? "" : companyLinkedin.trim());
        }

        JsonObject body = new JsonObject();
        body.addProperty("email", email.trim().toLowerCase());
        body.addProperty("password", password);
        body.add("data", data);

        Request request = new Request.Builder()
                .url(SupabaseConfig.url() + "/auth/v1/signup")
                .post(RequestBody.create(body.toString(), JSON))
                .header("apikey", SupabaseConfig.anonKey())
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = readBody(response);
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw, "Falha no cadastro."));
            }
            return parseSession(raw, true);
        }
    }

    @NonNull
    public AuthSession refresh(@NonNull String refreshToken) throws IOException {
        assertConfigured();

        JsonObject body = new JsonObject();
        body.addProperty("refresh_token", refreshToken);

        Request request = new Request.Builder()
                .url(SupabaseConfig.url() + "/auth/v1/token?grant_type=refresh_token")
                .post(RequestBody.create(body.toString(), JSON))
                .header("apikey", SupabaseConfig.anonKey())
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = readBody(response);
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw, "Sessao expirada. Entre de novo."));
            }
            return parseSession(raw, false);
        }
    }

    /**
     * Retorna o profile_json completo (mesmo shape do web) ou null se nao houver perfil completo.
     */
    @Nullable
    public Map<String, Object> fetchProfessionalProfile(@NonNull AuthSession session) throws IOException {
        assertConfigured();
        if (session.userId == null || session.userId.isEmpty()) {
            return null;
        }

        String url = SupabaseConfig.url()
                + "/rest/v1/professional_profiles"
                + "?select=profile_json,completed_at"
                + "&user_id=eq." + session.userId
                + "&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("apikey", SupabaseConfig.anonKey())
                .header("Authorization", "Bearer " + session.accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = readBody(response);
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw, "Falha ao carregar perfil."));
            }
            JsonArray rows = gson.fromJson(raw, JsonArray.class);
            if (rows == null || rows.size() == 0) return null;
            JsonObject row = rows.get(0).getAsJsonObject();
            if (!row.has("completed_at") || row.get("completed_at").isJsonNull()) {
                return null;
            }
            if (!row.has("profile_json") || row.get("profile_json").isJsonNull()) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = gson.fromJson(row.get("profile_json"), Map.class);
            return profile;
        }
    }

    public static String skillsCsvFromProfile(@Nullable Map<String, Object> profile) {
        if (profile == null) return "";
        Object skillsObj = profile.get("skills");
        if (!(skillsObj instanceof List)) return "";
        List<?> skills = (List<?>) skillsObj;
        List<String> labels = new ArrayList<>();
        for (Object item : skills) {
            if (item instanceof Map) {
                Object label = ((Map<?, ?>) item).get("label");
                if (label != null && !String.valueOf(label).trim().isEmpty()) {
                    labels.add(String.valueOf(label).trim());
                }
            } else if (item != null) {
                String text = String.valueOf(item).trim();
                if (!text.isEmpty()) labels.add(text);
            }
        }
        return String.join(", ", labels);
    }

    public static String bioFromProfile(@Nullable Map<String, Object> profile) {
        if (profile == null) return "";
        Object formObj = profile.get("form");
        if (!(formObj instanceof Map)) return "";
        Object bio = ((Map<?, ?>) formObj).get("bio");
        return bio != null ? String.valueOf(bio).trim() : "";
    }

    private AuthSession parseSession(String raw, boolean allowMissingTokens) throws IOException {
        JsonObject root = gson.fromJson(raw, JsonObject.class);
        if (root == null) throw new IOException("Resposta de auth vazia.");

        String access = text(root, "access_token");
        String refresh = text(root, "refresh_token");
        if (access.isEmpty() || refresh.isEmpty()) {
            if (allowMissingTokens) {
                // Cadastro com confirmacao de email: user criado, sem sessao ainda
                return null;
            }
            throw new IOException("Tokens ausentes na resposta.");
        }

        long expiresAt = 0L;
        if (root.has("expires_at") && root.get("expires_at").isJsonPrimitive()) {
            expiresAt = root.get("expires_at").getAsLong();
        } else if (root.has("expires_in") && root.get("expires_in").isJsonPrimitive()) {
            expiresAt = (System.currentTimeMillis() / 1000L) + root.get("expires_in").getAsLong();
        }

        String tokenType = text(root, "token_type");
        String userId = "";
        String email = "";
        String name = "";
        String accountType = "";
        if (root.has("user") && root.get("user").isJsonObject()) {
            JsonObject user = root.getAsJsonObject("user");
            userId = text(user, "id");
            email = text(user, "email");
            if (user.has("user_metadata") && user.get("user_metadata").isJsonObject()) {
                JsonObject meta = user.getAsJsonObject("user_metadata");
                name = text(meta, "full_name");
                if (name.isEmpty()) name = text(meta, "name");
                accountType = text(meta, "account_type");
                if (accountType.isEmpty()) accountType = text(meta, "accountType");
            }
        }

        return new AuthSession(access, refresh, expiresAt, tokenType, userId, email, name, accountType);
    }

    /**
     * Metadata do usuario autenticado (para hidratar company_profiles apos confirmacao de e-mail).
     */
    @NonNull
    public JsonObject fetchUserMetadata(@NonNull AuthSession session) throws IOException {
        assertConfigured();
        Request request = new Request.Builder()
                .url(SupabaseConfig.url() + "/auth/v1/user")
                .get()
                .header("apikey", SupabaseConfig.anonKey())
                .header("Authorization", "Bearer " + session.accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = readBody(response);
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw, "Falha ao carregar usuario."));
            }
            JsonObject user = gson.fromJson(raw, JsonObject.class);
            if (user == null) return new JsonObject();
            if (user.has("user_metadata") && user.get("user_metadata").isJsonObject()) {
                return user.getAsJsonObject("user_metadata");
            }
            return new JsonObject();
        }
    }

    private static void assertConfigured() throws IOException {
        if (!SupabaseConfig.isConfigured()) {
            throw new IOException(
                    "Supabase nao configurado. Adicione SUPABASE_URL e SUPABASE_ANON_KEY em local.properties e Sync Gradle."
            );
        }
    }

    private static String readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        return body != null ? body.string() : "";
    }

    private static String extractError(String raw, String fallback) {
        try {
            JsonObject obj = new Gson().fromJson(raw, JsonObject.class);
            if (obj == null) return BrDocs.translateAuthError(fallback, fallback);
            for (String key : new String[]{"msg", "message", "error_description", "error"}) {
                if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                    String value = obj.get(key).getAsString();
                    if (value != null && !value.trim().isEmpty()) {
                        return BrDocs.translateAuthError(value.trim(), fallback);
                    }
                }
            }
        } catch (Exception ignored) {
            // keep fallback
        }
        return BrDocs.translateAuthError(fallback, fallback);
    }

    private static String text(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        JsonElement el = obj.get(key);
        return el.isJsonPrimitive() ? el.getAsString().trim() : "";
    }
}
