package com.mindlab.worky.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mindlab.worky.model.CareerAnalysis;
import com.mindlab.worky.model.JobItem;
import com.mindlab.worky.model.ProfileMatchResult;
import com.mindlab.worky.model.PublicCatalogCompany;
import com.mindlab.worky.model.SearchQuota;

import java.io.IOException;
import java.lang.reflect.Type;
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

public class WorkyApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final Type jobListType = new TypeToken<List<JobItem>>() {}.getType();

    public WorkyApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    @NonNull
    public CareerAnalysis fetchCareer(
            @NonNull String cargo,
            @Nullable String fonte,
            @Nullable String local,
            @Nullable String modelo,
            @Nullable String accessToken
    ) throws IOException {
        String encodedCargo = URLEncoder.encode(cargo.trim(), StandardCharsets.UTF_8.name());
        String source = (fonte == null || fonte.trim().isEmpty()) ? ApiConfig.DEFAULT_FONTE : fonte.trim();
        StringBuilder url = new StringBuilder(ApiConfig.BASE_URL)
                .append("/carreira?cargo=").append(encodedCargo)
                .append("&fonte=").append(source);
        appendIfPresent(url, "local", local);
        appendIfPresent(url, "modelo", modelo);

        Request request = apiGet(url.toString(), accessToken).build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            throwIfUnsuccessful(response, raw);
            CareerAnalysis analysis = gson.fromJson(raw, CareerAnalysis.class);
            if (analysis == null || analysis.carreira == null || analysis.carreira.trim().isEmpty()) {
                throw new IOException("Resposta da API sem carreira valida.");
            }
            return analysis;
        }
    }

    @NonNull
    public CareerAnalysis fetchCareer(@NonNull String cargo, @Nullable String fonte) throws IOException {
        return fetchCareer(cargo, fonte, null, null, null);
    }

    @NonNull
    public List<JobItem> fetchJobs(
            @NonNull String cargo,
            @Nullable String fonte,
            @Nullable String local,
            @Nullable String modelo,
            @Nullable String accessToken
    ) throws IOException {
        String encodedCargo = URLEncoder.encode(cargo.trim(), StandardCharsets.UTF_8.name());
        String source = (fonte == null || fonte.trim().isEmpty()) ? ApiConfig.DEFAULT_FONTE : fonte.trim();
        StringBuilder url = new StringBuilder(ApiConfig.BASE_URL)
                .append("/buscar?cargo=").append(encodedCargo)
                .append("&fonte=").append(source);
        appendIfPresent(url, "local", local);
        appendIfPresent(url, "modelo", modelo);

        Request request = apiGet(url.toString(), accessToken).build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            throwIfUnsuccessful(response, raw);
            List<JobItem> jobs = gson.fromJson(raw, jobListType);
            return jobs != null ? jobs : new ArrayList<>();
        }
    }

    @NonNull
    public List<JobItem> fetchJobs(@NonNull String cargo, @Nullable String fonte) throws IOException {
        return fetchJobs(cargo, fonte, null, null, null);
    }

    @Nullable
    public SearchQuota fetchSearchQuota(@Nullable String accessToken) {
        Request request = apiGet(ApiConfig.BASE_URL + "/carreira/quota", accessToken).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            if (!response.isSuccessful() || raw.isEmpty()) return null;
            JsonObject root = gson.fromJson(raw, JsonObject.class);
            return SearchQuota.fromJson(root);
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    public List<PublicCatalogCompany> fetchPublicCatalog() {
        Request request = apiGet(ApiConfig.BASE_URL + "/catalogo/empresas", null).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            if (!response.isSuccessful() || raw.isEmpty()) return new ArrayList<>();
            JsonObject root = gson.fromJson(raw, JsonObject.class);
            if (root == null || !root.has("empresas") || !root.get("empresas").isJsonArray()) {
                return new ArrayList<>();
            }
            JsonArray rows = root.getAsJsonArray("empresas");
            List<PublicCatalogCompany> companies = new ArrayList<>();
            for (JsonElement el : rows) {
                if (el == null || !el.isJsonObject()) continue;
                PublicCatalogCompany company = gson.fromJson(el, PublicCatalogCompany.class);
                if (company == null) continue;
                if (company.id == null || company.id.trim().isEmpty()) continue;
                if (company.name == null || company.name.trim().isEmpty()) continue;
                companies.add(company);
            }
            return companies;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void appendIfPresent(StringBuilder url, String key, @Nullable String value) throws IOException {
        if (value == null || value.trim().isEmpty()) return;
        url.append("&").append(key).append("=")
                .append(URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.name()));
    }

    @NonNull
    public ProfileMatchResult fetchMatch(@NonNull Map<String, Object> profile, @NonNull String carreira) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("profile", profile);
        payload.put("carreira", carreira.trim());

        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/carreira/match")
                .post(body)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String raw = responseBody != null ? responseBody.string() : "";
            throwIfUnsuccessful(response, raw);
            JsonObject root = gson.fromJson(raw, JsonObject.class);
            if (root == null || !root.has("match")) {
                throw new IOException("Resposta da API sem campo match.");
            }
            ProfileMatchResult match = gson.fromJson(root.get("match"), ProfileMatchResult.class);
            if (match == null) {
                throw new IOException("Match invalido na resposta.");
            }
            return match;
        }
    }

    public String toJson(Object value) {
        return gson.toJson(value);
    }

    public CareerAnalysis careerFromJson(String json) {
        return gson.fromJson(json, CareerAnalysis.class);
    }

    public List<JobItem> jobsFromJson(String json) {
        List<JobItem> jobs = gson.fromJson(json, jobListType);
        return jobs != null ? jobs : new ArrayList<>();
    }

    public ProfileMatchResult matchFromJson(String json) {
        return gson.fromJson(json, ProfileMatchResult.class);
    }

    public CareerAnalysis fromJson(String json) {
        return careerFromJson(json);
    }

    private Request.Builder apiGet(@NonNull String url, @Nullable String accessToken) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json");
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken.trim());
        }
        return builder;
    }

    private static void throwIfUnsuccessful(@NonNull Response response, @Nullable String raw) throws IOException {
        if (response.isSuccessful()) return;
        throw ApiException.from(response.code(), raw);
    }
}
