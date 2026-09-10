package com.mindlab.worky.network;

import com.mindlab.worky.BuildConfig;

public final class SupabaseConfig {
    private SupabaseConfig() {}

    public static String url() {
        String value = BuildConfig.SUPABASE_URL;
        return value != null ? value.trim().replaceAll("/$", "") : "";
    }

    public static String anonKey() {
        String value = BuildConfig.SUPABASE_ANON_KEY;
        return value != null ? value.trim() : "";
    }

    public static boolean isConfigured() {
        return !url().isEmpty() && !anonKey().isEmpty();
    }
}
