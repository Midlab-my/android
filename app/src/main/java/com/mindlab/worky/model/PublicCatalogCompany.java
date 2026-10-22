package com.mindlab.worky.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Empresa do catalogo publico GET /catalogo/empresas. Sem CNPJ nem plano. */
public class PublicCatalogCompany {

    public String id;
    public String name;
    public String sector;
    public String location;
    public String size;
    public List<PublicCatalogJob> jobs;

    public static class PublicCatalogJob {
        public String id;
        public String titulo;
        public String local;
        public String modelo;
        public String requisitos;
        public String descricao;
    }

    @NonNull
    public List<PublicCatalogJob> jobsOrEmpty() {
        return jobs != null ? jobs : new ArrayList<>();
    }

    @NonNull
    public String displayName() {
        return name != null && !name.trim().isEmpty() ? name.trim() : "Empresa Worky";
    }

    @NonNull
    public String initials() {
        String[] parts = displayName().split("\\s+");
        if (parts.length == 0) return "W";
        if (parts.length == 1) {
            String word = parts[0];
            return word.length() <= 2 ? word.toUpperCase() : word.substring(0, 2).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts[1].substring(0, 1);
        return (a + b).toUpperCase();
    }

    @NonNull
    public String highlight() {
        if (!jobsOrEmpty().isEmpty()) {
            String titulo = jobsOrEmpty().get(0).titulo;
            if (titulo != null && !titulo.trim().isEmpty()) return titulo.trim();
        }
        return sector != null ? sector : "";
    }

    @NonNull
    public String metaLine() {
        return join(" · ", sector, location, size);
    }

    public int jobsOpen() {
        return jobsOrEmpty().size();
    }

    @NonNull
    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(part.trim());
        }
        return sb.toString();
    }
}
