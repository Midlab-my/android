package com.mindlab.worky.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Dados RH esperando aal2 depois do cadastro empresa. Nao cria conta Auth nova. */
public final class PendingCompanySignup {

    private static final String PREFS = "worky_pending_company";
    private static final String K_NAME = "name";
    private static final String K_SIZE = "size";
    private static final String K_CNPJ = "cnpj";
    private static final String K_LOCATION = "location";
    private static final String K_SECTOR = "sector";
    private static final String K_LINKEDIN = "linkedin";

    public final String name;
    public final String size;
    public final String cnpj;
    public final String location;
    public final String sector;
    public final String linkedin;

    public PendingCompanySignup(
            String name,
            String size,
            String cnpj,
            String location,
            String sector,
            String linkedin
    ) {
        this.name = name != null ? name : "";
        this.size = size != null ? size : "";
        this.cnpj = cnpj != null ? cnpj : "";
        this.location = location != null ? location : "";
        this.sector = sector != null ? sector : "";
        this.linkedin = linkedin != null ? linkedin : "";
    }

    public void save(@NonNull Context context) {
        prefs(context).edit()
                .putString(K_NAME, name)
                .putString(K_SIZE, size)
                .putString(K_CNPJ, cnpj)
                .putString(K_LOCATION, location)
                .putString(K_SECTOR, sector)
                .putString(K_LINKEDIN, linkedin)
                .apply();
    }

    @Nullable
    public static PendingCompanySignup load(@NonNull Context context) {
        SharedPreferences prefs = prefs(context);
        String name = prefs.getString(K_NAME, "");
        if (name == null || name.trim().isEmpty()) return null;
        return new PendingCompanySignup(
                name,
                prefs.getString(K_SIZE, ""),
                prefs.getString(K_CNPJ, ""),
                prefs.getString(K_LOCATION, ""),
                prefs.getString(K_SECTOR, ""),
                prefs.getString(K_LINKEDIN, "")
        );
    }

    public static void clear(@NonNull Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
