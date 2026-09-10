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

    @SerializedName("competenciasDesejadas")
    public Competencias competenciasDesejadas;

    @SerializedName("certificacoesRecomendadas")
    public List<Certificacao> certificacoesRecomendadas;

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
    }

    public List<String> techSkills() {
        if (competenciasDesejadas == null || competenciasDesejadas.habilidadesTecnicas == null) {
            return new ArrayList<>();
        }
        return competenciasDesejadas.habilidadesTecnicas;
    }
}
