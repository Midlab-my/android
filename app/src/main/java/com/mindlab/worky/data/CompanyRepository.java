package com.mindlab.worky.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.model.CompanyCandidate;
import com.mindlab.worky.model.CompanyJob;
import com.mindlab.worky.model.CompanyProfile;
import com.mindlab.worky.network.SupabaseConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CompanyRepository {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9+#.\\u00c0-\\u024f]+", Pattern.CASE_INSENSITIVE);

    private final OkHttpClient client;
    private final Gson gson;

    public CompanyRepository() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    @Nullable
    public CompanyProfile fetchCompanyProfile(@NonNull AuthSession session) throws IOException {
        assertConfigured();
        String path = "company_profiles?select=*&user_id=eq."
                + enc(session.userId)
                + "&limit=1";
        JsonArray rows = getArray(path, session.accessToken);
        if (rows.size() == 0) return null;
        return mapProfile(rows.get(0).getAsJsonObject());
    }

    @NonNull
    public List<CompanyJob> listJobs(@NonNull AuthSession session) throws IOException {
        assertConfigured();
        String path = "company_jobs?select=*&company_user_id=eq."
                + enc(session.userId)
                + "&order=updated_at.desc";
        JsonArray rows = getArray(path, session.accessToken);
        List<CompanyJob> jobs = new ArrayList<>();
        for (JsonElement el : rows) {
            if (el.isJsonObject()) {
                jobs.add(mapJob(el.getAsJsonObject()));
            }
        }
        return jobs;
    }

    @NonNull
    public CompanyProfile createCompanyProfile(
            @NonNull AuthSession session,
            @NonNull String companyName,
            @Nullable String size,
            @Nullable String cnpj,
            @Nullable String location,
            @Nullable String sector,
            @Nullable String linkedin
    ) throws IOException {
        assertConfigured();
        String name = companyName.trim();
        if (name.isEmpty()) {
            throw new IOException("Informe o nome da empresa.");
        }

        JsonObject body = new JsonObject();
        body.addProperty("user_id", session.userId);
        body.addProperty("company_name", name);
        body.addProperty("size", size == null ? "" : size.trim());
        body.addProperty("cnpj", cnpj == null ? "" : cnpj.trim());
        body.addProperty("location", location == null ? "" : location.trim());
        body.addProperty("sector", sector == null ? "" : sector.trim());
        body.addProperty("linkedin", linkedin == null ? "" : linkedin.trim());
        body.addProperty("plan", "starter");

        JsonArray rows = mutate("company_profiles", "POST", session.accessToken, body.toString());
        if (rows.size() == 0) {
            throw new IOException("Conta Auth criada, mas perfil RH nao foi salvo. Rode o SQL supabase_company_rh.sql.");
        }
        return mapProfile(rows.get(0).getAsJsonObject());
    }

    @NonNull
    public CompanyJob createJob(
            @NonNull AuthSession session,
            @NonNull String titulo,
            @NonNull String local,
            @NonNull String modelo,
            @NonNull String requisitos,
            @NonNull String descricao
    ) throws IOException {
        assertConfigured();
        String cleanTitle = titulo.trim();
        if (cleanTitle.isEmpty()) {
            throw new IOException("Informe o titulo da vaga.");
        }
        JsonObject body = new JsonObject();
        body.addProperty("company_user_id", session.userId);
        body.addProperty("titulo", cleanTitle);
        body.addProperty("local", local.trim());
        body.addProperty("modelo", normalizeModelo(modelo));
        body.addProperty("requisitos", requisitos.trim());
        body.addProperty("descricao", descricao.trim());

        JsonArray rows = mutate("company_jobs", "POST", session.accessToken, body.toString());
        if (rows.size() == 0) {
            throw new IOException("Nao foi possivel criar a vaga.");
        }
        return mapJob(rows.get(0).getAsJsonObject());
    }

    @NonNull
    public CompanyJob updateJob(
            @NonNull AuthSession session,
            @NonNull String jobId,
            @NonNull String titulo,
            @NonNull String local,
            @NonNull String modelo,
            @NonNull String requisitos,
            @NonNull String descricao
    ) throws IOException {
        assertConfigured();
        String cleanTitle = titulo.trim();
        if (cleanTitle.isEmpty()) {
            throw new IOException("Informe o titulo da vaga.");
        }
        JsonObject body = new JsonObject();
        body.addProperty("titulo", cleanTitle);
        body.addProperty("local", local.trim());
        body.addProperty("modelo", normalizeModelo(modelo));
        body.addProperty("requisitos", requisitos.trim());
        body.addProperty("descricao", descricao.trim());

        String path = "company_jobs?id=eq." + enc(jobId)
                + "&company_user_id=eq." + enc(session.userId);
        JsonArray rows = mutate(path, "PATCH", session.accessToken, body.toString());
        if (rows.size() == 0) {
            throw new IOException("Vaga nao encontrada.");
        }
        return mapJob(rows.get(0).getAsJsonObject());
    }

    public void deleteJob(@NonNull AuthSession session, @NonNull String jobId) throws IOException {
        assertConfigured();
        String path = "company_jobs?id=eq." + enc(jobId)
                + "&company_user_id=eq." + enc(session.userId);
        mutate(path, "DELETE", session.accessToken, null);
    }

    @NonNull
    public List<CompanyCandidate> listCandidates(
            @NonNull AuthSession session,
            @NonNull CompanyJob job,
            @NonNull CompanyProfile company
    ) throws IOException {
        if (!company.canUnlockCandidates()) {
            return buildLockedPlaceholders(job);
        }

        try {
            JsonArray rows = getArray(
                    "professional_profiles?select=user_id,profile_json,completed_at&order=updated_at.desc&limit=40",
                    session.accessToken
            );
            List<CompanyCandidate> candidates = new ArrayList<>();
            for (JsonElement el : rows) {
                if (!el.isJsonObject()) continue;
                JsonObject row = el.getAsJsonObject();
                Map<?, ?> profileJson = Collections.emptyMap();
                if (row.has("profile_json") && row.get("profile_json").isJsonObject()) {
                    profileJson = gson.fromJson(row.get("profile_json"), Map.class);
                }
                CompanyCandidate scored = scoreProfileAgainstJob(profileJson, job);
                scored.id = text(row, "user_id");
                if (scored.id.isEmpty()) {
                    scored.id = job.id + "-" + candidates.size();
                }
                scored.locked = false;
                candidates.add(scored);
            }
            candidates.sort(Comparator.comparingInt((CompanyCandidate c) -> c.match).reversed());
            if (candidates.size() > 8) {
                return new ArrayList<>(candidates.subList(0, 8));
            }
            if (!candidates.isEmpty()) {
                return candidates;
            }
        } catch (Exception ignored) {
            // fallback demo abaixo
        }

        List<CompanyCandidate> demo = buildLockedPlaceholders(job);
        for (int i = 0; i < demo.size(); i++) {
            CompanyCandidate c = demo.get(i);
            c.id = job.id + "-demo-" + i;
            c.name = "Candidato " + (i + 1);
            c.locked = false;
            List<String> req = tokenize(job.requisitos);
            c.skills = req.size() > 3 ? new ArrayList<>(req.subList(0, 3)) : req;
            c.summary = "Compatibilidade estimada com " + safe(job.titulo) + ".";
        }
        return demo;
    }

    private JsonArray getArray(String path, String accessToken) throws IOException {
        return mutate(path, "GET", accessToken, null);
    }

    private JsonArray mutate(String path, String method, String accessToken, String jsonBody) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(SupabaseConfig.url() + "/rest/v1/" + path)
                .header("apikey", SupabaseConfig.anonKey())
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json");

        if ("GET".equals(method)) {
            builder.get();
        } else if ("DELETE".equals(method)) {
            builder.delete();
        } else {
            builder.header("Content-Type", "application/json");
            builder.header("Prefer", "return=representation");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    jsonBody == null ? "{}" : jsonBody,
                    okhttp3.MediaType.get("application/json; charset=utf-8")
            );
            if ("POST".equals(method)) {
                builder.post(body);
            } else if ("PATCH".equals(method)) {
                builder.patch(body);
            } else {
                throw new IOException("Metodo HTTP nao suportado: " + method);
            }
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            if (!response.isSuccessful()) {
                throw new IOException(extractError(raw));
            }
            if (raw == null || raw.trim().isEmpty() || "null".equals(raw.trim())) {
                return new JsonArray();
            }
            JsonElement parsed = gson.fromJson(raw, JsonElement.class);
            if (parsed == null || parsed.isJsonNull()) {
                return new JsonArray();
            }
            if (parsed.isJsonArray()) {
                return parsed.getAsJsonArray();
            }
            if (parsed.isJsonObject()) {
                JsonArray single = new JsonArray();
                single.add(parsed.getAsJsonObject());
                return single;
            }
            return new JsonArray();
        }
    }

    private static String normalizeModelo(String modelo) {
        String value = modelo == null ? "" : modelo.trim();
        if ("Remoto".equals(value) || "Hibrido".equals(value) || "Híbrido".equals(value) || "Presencial".equals(value)) {
            return "Hibrido".equals(value) ? "Híbrido" : value;
        }
        return "Remoto";
    }

    private static CompanyProfile mapProfile(JsonObject row) {
        CompanyProfile p = new CompanyProfile();
        p.userId = text(row, "user_id");
        p.companyName = text(row, "company_name");
        p.size = text(row, "size");
        p.cnpj = text(row, "cnpj");
        p.location = text(row, "location");
        p.sector = text(row, "sector");
        p.linkedin = text(row, "linkedin");
        p.plan = text(row, "plan");
        if (p.plan.isEmpty()) p.plan = "starter";
        p.createdAt = text(row, "created_at");
        p.updatedAt = text(row, "updated_at");
        return p;
    }

    private static CompanyJob mapJob(JsonObject row) {
        CompanyJob j = new CompanyJob();
        j.id = text(row, "id");
        j.companyUserId = text(row, "company_user_id");
        j.titulo = text(row, "titulo");
        j.local = text(row, "local");
        j.modelo = text(row, "modelo");
        j.requisitos = text(row, "requisitos");
        j.descricao = text(row, "descricao");
        j.createdAt = text(row, "created_at");
        j.updatedAt = text(row, "updated_at");
        return j;
    }

    private static List<CompanyCandidate> buildLockedPlaceholders(CompanyJob job) {
        int count = 3 + (safe(job.titulo).length() % 3);
        List<CompanyCandidate> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompanyCandidate c = new CompanyCandidate();
            c.id = job.id + "-locked-" + i;
            c.name = "Candidato bloqueado";
            c.role = safe(job.titulo);
            c.match = Math.max(72, 94 - i * 5);
            c.location = !safe(job.local).isEmpty() ? job.local : safe(job.modelo);
            c.locked = true;
            list.add(c);
        }
        return list;
    }

    private static CompanyCandidate scoreProfileAgainstJob(Map<?, ?> profileJson, CompanyJob job) {
        List<String> skillLabels = new ArrayList<>();
        Object skillsRaw = profileJson.get("skills");
        if (skillsRaw instanceof List) {
            for (Object item : (List<?>) skillsRaw) {
                if (item instanceof String) {
                    skillLabels.add((String) item);
                } else if (item instanceof Map) {
                    Object label = ((Map<?, ?>) item).get("label");
                    if (label != null && !String.valueOf(label).trim().isEmpty()) {
                        skillLabels.add(String.valueOf(label).trim());
                    }
                }
            }
        }

        Map<?, ?> form = profileJson.get("form") instanceof Map
                ? (Map<?, ?>) profileJson.get("form")
                : Collections.emptyMap();

        String headline = pick(profileJson, "headline", "title", "cargo");
        if (headline.isEmpty()) headline = safe(job.titulo);

        String name = pick(profileJson, "fullName", "name", "nome");
        if (name.isEmpty()) name = str(form.get("nome"));
        if (name.isEmpty()) name = "Candidato";

        String email = pick(profileJson, "email");
        if (email.isEmpty()) email = str(form.get("email"));

        String location = pick(profileJson, "location", "cidade");
        if (location.isEmpty()) location = str(form.get("cidade"));
        if (location.isEmpty()) location = safe(job.local);
        if (location.isEmpty()) location = "Brasil";

        String about = pick(profileJson, "about", "bio", "summary");
        if (about.isEmpty()) about = str(form.get("bio"));

        Set<String> jobTokens = new HashSet<>(tokenize(job.titulo, job.requisitos, job.descricao));
        Set<String> profileTokens = new HashSet<>(tokenize(headline, about, String.join(" ", skillLabels)));
        int hits = 0;
        for (String token : jobTokens) {
            if (profileTokens.contains(token)) hits++;
        }
        int base = jobTokens.isEmpty() ? 70 : Math.round((hits * 100f) / jobTokens.size());
        int match = Math.max(55, Math.min(98, base + Math.min(skillLabels.size(), 8)));

        CompanyCandidate c = new CompanyCandidate();
        c.match = match;
        c.skills = skillLabels.size() > 4 ? new ArrayList<>(skillLabels.subList(0, 4)) : skillLabels;
        c.role = headline;
        c.name = name;
        c.email = email;
        c.location = location;
        c.summary = about.isEmpty() ? ("Perfil com afinidade para " + safe(job.titulo) + ".") : about;
        return c;
    }

    private static String pick(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            String value = str(map.get(key));
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static List<String> tokenize(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) sb.append(' ').append(part);
        }
        String[] raw = TOKEN_SPLIT.split(sb.toString().toLowerCase(Locale.ROOT));
        List<String> out = new ArrayList<>();
        for (String token : raw) {
            String t = token.trim();
            if (t.length() > 1) out.add(t);
        }
        return out;
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
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
            throw new IOException("Supabase nao configurado. Sync Gradle apos local.properties.");
        }
    }

    private static String extractError(String raw) {
        try {
            JsonObject obj = new Gson().fromJson(raw, JsonObject.class);
            if (obj == null) return "Falha no Supabase.";
            for (String key : new String[]{"message", "msg", "hint", "error"}) {
                if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                    String v = obj.get(key).getAsString();
                    if (v != null && !v.trim().isEmpty()) return v.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "Falha no Supabase.";
    }
}
