package com.mindlab.worky.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.mindlab.worky.model.MatchProfileBuilder;
import com.mindlab.worky.model.ProfileMatchResult;
import com.mindlab.worky.network.WorkyApiClient;

import java.io.IOException;
import java.util.Map;

public class MatchRepository {

    private final MatchCacheDao dao;
    private final WorkyApiClient api;

    public MatchRepository(@NonNull Context context) {
        dao = AppDatabase.getInstance(context).matchCacheDao();
        api = new WorkyApiClient();
    }

    @WorkerThread
    @NonNull
    public ProfileMatchResult calculate(
            @NonNull String carreira,
            @Nullable String bio,
            @Nullable String skillsCsv,
            boolean preferCache
    ) throws IOException {
        return calculate(carreira, bio, skillsCsv, null, preferCache);
    }

    @WorkerThread
    @NonNull
    public ProfileMatchResult calculate(
            @NonNull String carreira,
            @Nullable String bio,
            @Nullable String skillsCsv,
            @Nullable Map<String, Object> fullProfile,
            boolean preferCache
    ) throws IOException {
        String key = MatchProfileBuilder.cacheKey(carreira, bio, skillsCsv);
        if (preferCache) {
            MatchCacheEntity cached = dao.findByQuery(key);
            if (cached != null) {
                ProfileMatchResult fromCache = api.matchFromJson(cached.jsonPayload);
                if (fromCache != null) return fromCache;
            }
        }

        Map<String, Object> profile = fullProfile != null
                ? fullProfile
                : MatchProfileBuilder.build(bio, skillsCsv);
        ProfileMatchResult fresh = api.fetchMatch(profile, carreira);
        dao.upsert(new MatchCacheEntity(
                key,
                carreira.trim(),
                api.toJson(fresh),
                fresh.pct,
                System.currentTimeMillis()
        ));
        return fresh;
    }
}
