package com.mindlab.worky.model;

public class CompanyJob {

    public String id;
    public String companyUserId;
    public String titulo;
    public String local;
    public String modelo;
    public String requisitos;
    public String descricao;
    public String createdAt;
    public String updatedAt;

    @Override
    public String toString() {
        return titulo != null && !titulo.isEmpty() ? titulo : "Vaga";
    }
}
