package com.mindlab.worky.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mindlab.worky.model.CareerAnalysis;
import com.mindlab.worky.model.JobItem;
import com.mindlab.worky.model.ProfileMatchResult;

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
            @Nullable String modelo
    ) throws IOException {
        String encodedCargo = URLEncoder.encode(cargo.trim(), StandardCharsets.UTF_8.name());
        String source = (fonte == null || fonte.trim().isEmpty()) ? "google" : fonte.trim();
        StringBuilder url = new StringBuilder(ApiConfig.BASE_URL)
                .append("/carreira?cargo=").append(encodedCargo)
                .append("&fonte=").append(source);
        appendIfPresent(url, "local", local);
        appendIfPresent(url, "modelo", modelo);

        Request request = new Request.Builder()
                .url(url.toString())
                .get()
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("API " + response.code() + ": " + truncate(raw));
            }
            CareerAnalysis analysis = gson.fromJson(raw, CareerAnalysis.class);
            if (analysis == null || analysis.carreira == null || analysis.carreira.trim().isEmpty()) {
                throw new IOException("Resposta da API sem carreira valida.");
            }
            return analysis;
        }
    }

    @NonNull
    public CareerAnalysis fetchCareer(@NonNull String cargo, @Nullable String fonte) throws IOException {
        return fetchCareer(cargo, fonte, null, null);
    }

    @NonNull
    public List<JobItem> fetchJobs(
            @NonNull String cargo,
            @Nullable String fonte,
            @Nullable String local,
            @Nullable String modelo
    ) throws IOException {
        String encodedCargo = URLEncoder.encode(cargo.trim(), StandardCharsets.UTF_8.name());
        String source = (fonte == null || fonte.trim().isEmpty()) ? "google" : fonte.trim();
        StringBuilder url = new StringBuilder(ApiConfig.BASE_URL)
                .append("/buscar?cargo=").append(encodedCargo)
                .append("&fonte=").append(source);
        appendIfPresent(url, "local", local);
        appendIfPresent(url, "modelo", modelo);

        Request request = new Request.Builder()
                .url(url.toString())
                .get()
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body != null ? body.string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("API " + response.code() + ": " + truncate(raw));
            }
            List<JobItem> jobs = gson.fromJson(raw, jobListType);
            return jobs != null ? jobs : new ArrayList<>();
        }
    }

    @NonNull
    public List<JobItem> fetchJobs(@NonNull String cargo, @Nullable String fonte) throws IOException {
        return fetchJobs(cargo, fonte, null, null);
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
            if (!response.isSuccessful()) {
                throw new IOException("API " + response.code() + ": " + truncate(raw));
            }
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

    private static String truncate(String value) {
        if (value == null) return "";
        return value.length() > 180 ? value.substring(0, 180) + "..." : value;
    }
}
