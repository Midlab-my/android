package com.mindlab.worky.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "jobs_cache")
public class JobsCacheEntity {

    @PrimaryKey
    @NonNull
    public String queryKey;

    @NonNull
    public String queryLabel;

    @NonNull
    public String jsonPayload;

    public int jobCount;

    public long updatedAt;

    public JobsCacheEntity(
            @NonNull String queryKey,
            @NonNull String queryLabel,
            @NonNull String jsonPayload,
            int jobCount,
            long updatedAt
    ) {
        this.queryKey = queryKey;
        this.queryLabel = queryLabel;
        this.jsonPayload = jsonPayload;
        this.jobCount = jobCount;
        this.updatedAt = updatedAt;
    }
}
