package com.mindlab.worky.model;

import com.google.gson.annotations.SerializedName;

public class JobItem {

    @SerializedName("titulo")
    public String titulo;

    @SerializedName("empresa")
    public String empresa;

    @SerializedName("local")
    public String local;

    @SerializedName("localidade")
    public String localidade;

    @SerializedName("modalidade")
    public String modalidade;

    @SerializedName("link")
    public String link;

    @SerializedName("fonte")
    public String fonte;

    @SerializedName("destaqueWorky")
    public boolean destaqueWorky;

    @SerializedName("tag")
    public String tag;

    public String displayLocal() {
        if (localidade != null && !localidade.trim().isEmpty()) return localidade.trim();
        if (local != null && !local.trim().isEmpty()) return local.trim();
        return "-";
    }

    public String displayTitle() {
        return titulo != null && !titulo.trim().isEmpty() ? titulo.trim() : "Vaga";
    }
}
