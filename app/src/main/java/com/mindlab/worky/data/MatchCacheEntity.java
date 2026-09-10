package com.mindlab.worky.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "match_cache")
public class MatchCacheEntity {

    @PrimaryKey
    @NonNull
    public String queryKey;

    @NonNull
    public String carreiraLabel;

    @NonNull
    public String jsonPayload;

    public int pct;

    public long updatedAt;

    public MatchCacheEntity(
            @NonNull String queryKey,
            @NonNull String carreiraLabel,
            @NonNull String jsonPayload,
            int pct,
            long updatedAt
    ) {
        this.queryKey = queryKey;
        this.carreiraLabel = carreiraLabel;
        this.jsonPayload = jsonPayload;
        this.pct = pct;
        this.updatedAt = updatedAt;
    }
}
