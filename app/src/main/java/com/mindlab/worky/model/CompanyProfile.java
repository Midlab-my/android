package com.mindlab.worky.model;

public class CompanyProfile {

    public String userId;
    public String companyName;
    public String size;
    public String cnpj;
    public String location;
    public String sector;
    public String linkedin;
    public String plan;
    public String createdAt;
    public String updatedAt;

    public boolean canUnlockCandidates() {
        return "pro".equalsIgnoreCase(plan) || "enterprise".equalsIgnoreCase(plan);
    }

    public String planLabel() {
        return plan == null || plan.isEmpty() ? "starter" : plan;
    }
}
