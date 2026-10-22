package com.mindlab.worky.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.model.CareerAnalysis;
import com.mindlab.worky.model.SearchQuota;
import com.mindlab.worky.network.WorkyApiClient;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CareerRepository {

    private final CareerCacheDao dao;
    private final WorkyApiClient api;
    private final SessionManager sessionManager;

    public CareerRepository(@NonNull Context context) {
        dao = AppDatabase.getInstance(context).careerCacheDao();
        api = new WorkyApiClient();
        sessionManager = new SessionManager(context);
    }

    @WorkerThread
    @NonNull
    public CareerAnalysis analyze(
            @NonNull String cargo,
            @Nullable String fonte,
            @Nullable String local,
            @Nullable String modelo,
            boolean preferCache
    ) throws IOException {
        String key = normalizeKey(cargo) + "|"
                + safe(fonte) + "|"
                + safe(local) + "|"
                + safe(modelo);
        if (preferCache) {
            CareerCacheEntity cached = dao.findByCargo(key);
            if (cached != null) {
                CareerAnalysis fromCache = api.fromJson(cached.jsonPayload);
                if (fromCache != null) {
                    return fromCache;
                }
            }
        }

        CareerAnalysis fresh = api.fetchCareer(cargo, fonte, local, modelo, accessToken());
        dao.upsert(new CareerCacheEntity(
                key,
                fresh.carreira != null ? fresh.carreira : cargo.trim(),
                api.toJson(fresh),
                System.currentTimeMillis()
        ));
        return fresh;
    }

    @WorkerThread
    @NonNull
    public CareerAnalysis analyze(@NonNull String cargo, @Nullable String fonte, boolean preferCache) throws IOException {
        return analyze(cargo, fonte, null, null, preferCache);
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @WorkerThread
    @Nullable
    public CareerAnalysis loadCached(@NonNull String cargo) {
        CareerCacheEntity cached = dao.findByCargo(normalizeKey(cargo));
        if (cached == null) return null;
        return api.fromJson(cached.jsonPayload);
    }

    @WorkerThread
    @Nullable
    public SearchQuota quota() {
        return api.fetchSearchQuota(accessToken());
    }

    @WorkerThread
    @NonNull
    public List<CareerCacheEntity> recent() {
        return dao.listRecent();
    }

    @Nullable
    private String accessToken() {
        AuthSession session = sessionManager.ensureValidSession();
        if (session == null || !com.mindlab.worky.auth.JwtAal.hasAal2(session.accessToken)) {
            return null;
        }
        return session.accessToken;
    }

    private static String normalizeKey(String cargo) {
        return cargo.trim().toLowerCase(Locale.ROOT);
    }
}
