package com.mindlab.worky.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.model.MatchProfileBuilder;
import com.mindlab.worky.network.SupabaseConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ProfileRepository {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;

    public ProfileRepository() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public static class ProfileDraft {
        public String nome = "";
        public String bio = "";
        public String cidade = "";
        public String skillsCsv = "";
        public boolean completed;
        public String completedAt;
        public Map<String, Object> raw;
    }

    @NonNull
    public ProfileDraft load(@NonNull AuthSession session) throws IOException {
        assertConfigured();
        String path = "professional_profiles?select=profile_json,completed_at"
                + "&user_id=eq." + enc(session.userId)
                + "&limit=1";

        Request request = new Request.Builder()
                .url(SupabaseConfig.url() + "/rest/v1/" + path)
                .get()
                .header("apikey", SupabaseConfig.anonKey())
                .header("Authorization", "Bearer " + session.accessToken)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = read(response);
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw));
            }
            JsonArray rows = gson.fromJson(raw, JsonArray.class);
            ProfileDraft draft = new ProfileDraft();
            if (rows == null || rows.size() == 0) {
                draft.nome = session.displayName() != null ? session.displayName() : "";
                return draft;
            }
            JsonObject row = rows.get(0).getAsJsonObject();
            draft.completedAt = text(row, "completed_at");
            draft.completed = draft.completedAt != null && !draft.completedAt.isEmpty();
            if (row.has("profile_json") && row.get("profile_json").isJsonObject()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = gson.fromJson(row.get("profile_json"), Map.class);
                draft.raw = profile;
                draft.bio = bioFrom(profile);
                draft.nome = nomeFrom(profile);
                draft.cidade = cidadeFrom(profile);
                draft.skillsCsv = skillsFrom(profile);
            }
            if (draft.nome.isEmpty() && session.displayName() != null) {
                draft.nome = session.displayName();
            }
            return draft;
        }
    }

    public void save(
            @NonNull AuthSession session,
            @NonNull String nome,
            @NonNull String bio,
            @NonNull String cidade,
            @NonNull String skillsCsv
    ) throws IOException {
        assertConfigured();
        if (MatchProfileBuilder.splitSkills(skillsCsv).isEmpty()) {
            throw new IOException("Informe ao menos uma competencia tecnica.");
        }

        Map<String, Object> form = new HashMap<>();
        form.put("nome", nome.trim());
        form.put("bio", bio.trim());
        form.put("cidade", cidade.trim());
        form.put("estado", "");
        form.put("email", session.email != null ? session.email : "");

        List<Map<String, Object>> skills = new ArrayList<>();
        for (String label : MatchProfileBuilder.splitSkills(skillsCsv)) {
            Map<String, Object> skill = new HashMap<>();
            skill.put("label", label);
            skill.put("type", "tech");
            skills.add(skill);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("avatarUrl", "");
        profile.put("form", form);
        profile.put("skills", skills);
        profile.put("experiences", new ArrayList<>());
        profile.put("educations", new ArrayList<>());
        profile.put("certs", new ArrayList<>());
        String completedAt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .format(new java.util.Date());
        profile.put("completedAt", completedAt);

        JsonObject body = new JsonObject();
        body.addProperty("user_id", session.userId);
        body.add("profile_json", gson.toJsonTree(profile));
        body.addProperty("completed_at", completedAt);

        Request request = new Request.Builder()
                .url(SupabaseConfig.url() + "/rest/v1/professional_profiles?on_conflict=user_id")
                .post(RequestBody.create(body.toString(), JSON))
                .header("apikey", SupabaseConfig.anonKey())
                .header("Authorization", "Bearer " + session.accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = read(response);
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw));
            }
        }
    }

    private static String bioFrom(Map<String, Object> profile) {
        Object form = profile.get("form");
        if (form instanceof Map) {
            Object bio = ((Map<?, ?>) form).get("bio");
            return bio == null ? "" : String.valueOf(bio).trim();
        }
        return "";
    }

    private static String nomeFrom(Map<String, Object> profile) {
        Object form = profile.get("form");
        if (form instanceof Map) {
            Object nome = ((Map<?, ?>) form).get("nome");
            return nome == null ? "" : String.valueOf(nome).trim();
        }
        return "";
    }

    private static String cidadeFrom(Map<String, Object> profile) {
        Object form = profile.get("form");
        if (form instanceof Map) {
            Object cidade = ((Map<?, ?>) form).get("cidade");
            return cidade == null ? "" : String.valueOf(cidade).trim();
        }
        return "";
    }

    private static String skillsFrom(Map<String, Object> profile) {
        Object skillsObj = profile.get("skills");
        if (!(skillsObj instanceof List)) return "";
        List<String> labels = new ArrayList<>();
        for (Object item : (List<?>) skillsObj) {
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

    private static String read(Response response) throws IOException {
        ResponseBody body = response.body();
        return body != null ? body.string() : "";
    }

    private static String text(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        JsonElement el = obj.get(key);
        return el.isJsonPrimitive() ? el.getAsString().trim() : "";
    }

    private static String enc(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }

    private static void assertConfigured() throws IOException {
        if (!SupabaseConfig.isConfigured()) {
            throw new IOException("Supabase nao configurado.");
        }
    }

    private static String extractError(String raw) {
        try {
            JsonObject obj = new Gson().fromJson(raw, JsonObject.class);
            if (obj == null) return "Falha ao salvar perfil.";
            for (String key : new String[]{"message", "msg", "hint", "error"}) {
                if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                    String v = obj.get(key).getAsString();
                    if (v != null && !v.trim().isEmpty()) return v.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "Falha ao salvar perfil.";
    }
}
