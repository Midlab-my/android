package com.mindlab.worky.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "career_cache")
public class CareerCacheEntity {

    @PrimaryKey
    @NonNull
    public String cargoKey;

    @NonNull
    public String cargoLabel;

    @NonNull
    public String jsonPayload;

    public long updatedAt;

    public CareerCacheEntity(@NonNull String cargoKey, @NonNull String cargoLabel, @NonNull String jsonPayload, long updatedAt) {
        this.cargoKey = cargoKey;
        this.cargoLabel = cargoLabel;
        this.jsonPayload = jsonPayload;
        this.updatedAt = updatedAt;
    }
}
