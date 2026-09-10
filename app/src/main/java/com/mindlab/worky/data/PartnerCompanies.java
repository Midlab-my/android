package com.mindlab.worky.data;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Parceiros ilustrativos (mesmo mock da home web /empresas). */
public final class PartnerCompanies {

    public static final class Partner {
        public final String id;
        public final String name;
        public final String sector;
        public final String location;
        public final String size;
        public final int jobsOpen;
        public final String highlight;
        public final String initials;

        public Partner(
                String id,
                String name,
                String sector,
                String location,
                String size,
                int jobsOpen,
                String highlight,
                String initials
        ) {
            this.id = id;
            this.name = name;
            this.sector = sector;
            this.location = location;
            this.size = size;
            this.jobsOpen = jobsOpen;
            this.highlight = highlight;
            this.initials = initials;
        }
    }

    private PartnerCompanies() {}

    @NonNull
    public static List<Partner> all() {
        return Collections.unmodifiableList(Arrays.asList(
                new Partner("norte-tech", "Norte Tech", "Software", "Sao Paulo, SP", "51-200", 8,
                        "Engenharia e produto B2B.", "NT"),
                new Partner("atlas-dados", "Atlas Dados", "Dados", "Remoto (Brasil)", "11-50", 5,
                        "Analistas e cientistas de dados.", "AD"),
                new Partner("horizon-rh", "Horizon RH", "Recursos Humanos", "Curitiba, PR", "201-500", 12,
                        "Recrutamento e people ops.", "HR"),
                new Partner("pulse-fintech", "Pulse Fintech", "Financas", "Rio de Janeiro, RJ", "51-200", 6,
                        "Risco, compliance e pagamentos.", "PF"),
                new Partner("orbital-design", "Orbital Design", "Design", "Belo Horizonte, MG", "11-50", 3,
                        "Product design e pesquisa.", "OD"),
                new Partner("campo-cloud", "Campo Cloud", "Infraestrutura", "Remoto (Brasil)", "51-200", 9,
                        "Cloud, SRE e seguranca.", "CC")
        ));
    }
}
