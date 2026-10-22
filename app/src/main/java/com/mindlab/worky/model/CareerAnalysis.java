package com.mindlab.worky.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class CareerAnalysis {

    @SerializedName("carreira")
    public String carreira;

    @SerializedName("insightIA")
    public String insightIA;

    @SerializedName("mediaSalarial")
    public String mediaSalarial;

    @SerializedName("vagasAbertas")
    public int vagasAbertas;

    @SerializedName("nivelDemanda")
    public String nivelDemanda;

    @SerializedName("crescimentoAnual")
    public String crescimentoAnual;

    @SerializedName("crescimentoMensal")
    public String crescimentoMensal;

    @SerializedName("competenciasDesejadas")
    public Competencias competenciasDesejadas;

    @SerializedName("certificacoesRecomendadas")
    public List<Certificacao> certificacoesRecomendadas;

    @SerializedName("oportunidadesDestaque")
    public List<JobItem> oportunidadesDestaque;

    @SerializedName("todasVagas")
    public List<JobItem> todasVagas;

    @SerializedName("cursosRecomendados")
    public List<Curso> cursosRecomendados;

    public static class Competencias {
        @SerializedName("habilidadesTecnicas")
        public List<String> habilidadesTecnicas;

        @SerializedName("softSkills")
        public List<String> softSkills;
    }

    public static class Certificacao {
        @SerializedName("empresa")
        public String empresa;

        @SerializedName("nome")
        public String nome;

        @SerializedName("descricao")
        public String descricao;

        @SerializedName("url")
        public String url;
    }

    public static class Curso {
        @SerializedName("plataforma")
        public String plataforma;

        @SerializedName("nome")
        public String nome;

        @SerializedName("url")
        public String url;

        @SerializedName("area")
        public String area;
    }

    public List<String> techSkills() {
        if (competenciasDesejadas == null || competenciasDesejadas.habilidadesTecnicas == null) {
            return new ArrayList<>();
        }
        return competenciasDesejadas.habilidadesTecnicas;
    }

    public List<String> softSkills() {
        if (competenciasDesejadas == null || competenciasDesejadas.softSkills == null) {
            return new ArrayList<>();
        }
        return competenciasDesejadas.softSkills;
    }

    public List<JobItem> jobs() {
        if (todasVagas != null && !todasVagas.isEmpty()) return todasVagas;
        if (oportunidadesDestaque != null) return oportunidadesDestaque;
        return new ArrayList<>();
    }
}
