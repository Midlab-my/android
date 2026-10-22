package com.mindlab.worky.model;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SearchQuota {

    public int used;
    public int limit;
    public int remaining;
    public String resetsAt;

    @Nullable
    public static SearchQuota fromJson(@Nullable JsonObject row) {
        if (row == null) return null;
        SearchQuota quota = new SearchQuota();
        quota.used = intVal(row, "used");
        quota.limit = intVal(row, "limit");
        if (quota.limit <= 0) return null;
        if (row.has("remaining") && !row.get("remaining").isJsonNull()) {
            quota.remaining = intVal(row, "remaining");
        } else {
            quota.remaining = Math.max(quota.limit - quota.used, 0);
        }
        if (row.has("resetsAt") && row.get("resetsAt").isJsonPrimitive()) {
            quota.resetsAt = row.get("resetsAt").getAsString();
        } else if (row.has("resets_at") && row.get("resets_at").isJsonPrimitive()) {
            quota.resetsAt = row.get("resets_at").getAsString();
        } else {
            quota.resetsAt = "";
        }
        return quota;
    }

    private static int intVal(JsonObject row, String key) {
        if (!row.has(key) || row.get(key).isJsonNull()) return 0;
        JsonElement el = row.get(key);
        try {
            return el.getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }
}
