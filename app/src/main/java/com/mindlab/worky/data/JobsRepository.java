package com.mindlab.worky.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.model.JobItem;
import com.mindlab.worky.network.ApiConfig;
import com.mindlab.worky.network.WorkyApiClient;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class JobsRepository {

    private final JobsCacheDao dao;
    private final WorkyApiClient api;
    private final SessionManager sessionManager;

    public JobsRepository(@NonNull Context context) {
        dao = AppDatabase.getInstance(context).jobsCacheDao();
        api = new WorkyApiClient();
        sessionManager = new SessionManager(context);
    }

    @WorkerThread
    @NonNull
    public List<JobItem> search(
            @NonNull String cargo,
            @Nullable String fonte,
            @Nullable String local,
            @Nullable String modelo,
            boolean preferCache
    ) throws IOException {
        String key = normalizeKey(cargo) + "|"
                + (fonte == null ? ApiConfig.DEFAULT_FONTE : fonte.trim().toLowerCase(Locale.ROOT)) + "|"
                + (local == null ? "" : local.trim().toLowerCase(Locale.ROOT)) + "|"
                + (modelo == null ? "" : modelo.trim().toLowerCase(Locale.ROOT));
        if (preferCache) {
            JobsCacheEntity cached = dao.findByQuery(key);
            if (cached != null) {
                return api.jobsFromJson(cached.jsonPayload);
            }
        }

        AuthSession session = sessionManager.ensureValidSession();
        String token = session != null ? session.accessToken : null;
        List<JobItem> fresh = api.fetchJobs(cargo, fonte, local, modelo, token);
        dao.upsert(new JobsCacheEntity(
                key,
                cargo.trim(),
                api.toJson(fresh),
                fresh.size(),
                System.currentTimeMillis()
        ));
        return fresh;
    }

    @WorkerThread
    @NonNull
    public List<JobItem> search(@NonNull String cargo, @Nullable String fonte, boolean preferCache) throws IOException {
        return search(cargo, fonte, null, null, preferCache);
    }

    private static String normalizeKey(String cargo) {
        return cargo.trim().toLowerCase(Locale.ROOT);
    }
}
