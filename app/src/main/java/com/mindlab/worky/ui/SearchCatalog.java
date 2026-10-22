package com.mindlab.worky.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catalogo espelhado do Dashboard web (paises, estados, sugestoes). */
public final class SearchCatalog {

    public static final String[] MODELOS = {"Qualquer", "Remoto", "Híbrido", "Presencial"};
    public static final List<String> POPULAR = Arrays.asList(
            "Desenvolvedor", "Designer", "Analista de Dados", "Marketing", "Vendas"
    );
    private static final List<String> SUGGESTIONS = Arrays.asList(
            "Administrador", "Advogado", "Analista", "Analista de Dados", "Assistente",
            "Auxiliar", "Backend", "Bug Bounty", "Contador", "Coordenador", "Desenvolvedor",
            "Desenvolvedor Backend", "Desenvolvedor Frontend", "Desenvolvedor Fullstack",
            "Designer", "Direito", "Enfermeiro", "Engenharia Civil", "Engenheiro",
            "Engenheiro de Software", "Estágio", "Finanças", "Frontend", "Fullstack",
            "Gerente", "Help Desk", "Juridico", "Logística", "Marketing", "Medico",
            "Mobile", "Pedagogia", "Product Management", "Product Owner", "QA", "RH",
            "Recursos Humanos", "Segurança da Informação", "Service Desk",
            "Solutions Architect", "SRE", "Suporte", "Técnico", "Tecnologia da Informação",
            "UI Design", "UX", "UX Research", "Vendas"
    );

    private static final Map<String, List<String>> LOCATION = new LinkedHashMap<>();

    static {
        LOCATION.put("Brasil", Arrays.asList(
                "Acre", "Alagoas", "Amapa", "Amazonas", "Bahia", "Ceara", "Distrito Federal",
                "Espirito Santo", "Goias", "Maranhao", "Mato Grosso", "Mato Grosso do Sul",
                "Minas Gerais", "Para", "Paraiba", "Parana", "Pernambuco", "Piaui",
                "Rio de Janeiro", "Rio Grande do Norte", "Rio Grande do Sul", "Rondonia",
                "Roraima", "Santa Catarina", "Sao Paulo", "Sergipe", "Tocantins"
        ));
        LOCATION.put("Estados Unidos", Arrays.asList(
                "California", "Florida", "New York", "Texas", "Washington"
        ));
        LOCATION.put("Portugal", Arrays.asList(
                "Lisboa", "Porto", "Braga", "Coimbra", "Faro"
        ));
        LOCATION.put("Espanha", Arrays.asList(
                "Madrid", "Catalunha", "Valencia", "Andaluzia"
        ));
        LOCATION.put("Canada", Arrays.asList(
                "Ontario", "Quebec", "British Columbia"
        ));
    }

    private SearchCatalog() {}

    @NonNull
    public static List<String> countries() {
        return Collections.unmodifiableList(Arrays.asList(LOCATION.keySet().toArray(new String[0])));
    }

    @NonNull
    public static List<String> regionsFor(@NonNull String country) {
        List<String> regions = LOCATION.get(country);
        return regions != null ? regions : Collections.emptyList();
    }

    @NonNull
    public static List<String> suggest(@Nullable String query) {
        String folded = fold(query);
        if (folded.length() < 2) return POPULAR;
        List<String> prefix = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String label : SUGGESTIONS) {
            String cargo = fold(label);
            if (cargo.startsWith(folded) || cargo.equals(folded)) {
                if (!prefix.contains(label)) prefix.add(label);
            } else if (cargo.contains(folded) && !contains.contains(label)) {
                contains.add(label);
            }
            if (prefix.size() >= 5) break;
        }
        List<String> out = new ArrayList<>(prefix);
        for (String item : contains) {
            if (out.size() >= 5) break;
            if (!out.contains(item)) out.add(item);
        }
        return out;
    }

    @NonNull
    public static String fold(@Nullable String value) {
        if (value == null) return "";
        String norm = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return norm.replaceAll("\\p{M}+", "").toLowerCase();
    }
}
