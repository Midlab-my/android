package com.mindlab.worky.ui;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catalogo espelhado do Dashboard web (paises, estados, categorias). */
public final class SearchCatalog {

    public static final class Category {
        public final String id;
        public final String label;
        public final String description;
        public final List<String> items;

        public Category(String id, String label, String description, List<String> items) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.items = items;
        }
    }

    public static final String[] MODELOS = {"Qualquer", "Remoto", "Hibrido", "Presencial"};

    private static final Map<String, List<String>> LOCATION = new LinkedHashMap<>();
    private static final List<Category> CATEGORIES;

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

        CATEGORIES = Arrays.asList(
                new Category("popular", "Populares", "Buscas frequentes no mercado brasileiro",
                        Arrays.asList("Desenvolvedor", "Designer", "Analista de Dados", "Marketing", "Vendas", "Contador", "Advogado", "RH")),
                new Category("technology", "Tecnologia", "Desenvolvimento, infraestrutura, seguranca e qualidade",
                        Arrays.asList("Desenvolvedor Frontend", "Desenvolvedor Backend", "Desenvolvedor Full Stack", "DevOps", "Engenheiro de Software", "Mobile", "QA", "Seguranca da Informacao")),
                new Category("data-ai", "Dados e IA", "Analise, engenharia, ciencia de dados e inteligencia artificial",
                        Arrays.asList("Analista de Dados", "Engenheiro de Dados", "Cientista de Dados", "Analista de BI", "Engenheiro de Machine Learning", "Especialista em IA", "Analytics Engineer")),
                new Category("design-product", "Design e Produto", "Experiencia do usuario, produto digital e pesquisa",
                        Arrays.asList("UX Designer", "UI Designer", "Product Designer", "Product Manager", "Product Owner", "Scrum Master")),
                new Category("marketing", "Marketing", "Crescimento, conteudo, midia paga e performance",
                        Arrays.asList("Analista de Marketing", "Social Media", "Growth Hacker", "SEO", "Copywriter")),
                new Category("commercial", "Comercial", "Vendas e relacionamento com clientes",
                        Arrays.asList("Vendedor", "Executivo de Contas", "Account Manager", "SDR", "Gerente Comercial")),
                new Category("finance", "Financeiro", "Controladoria, credito e planejamento",
                        Arrays.asList("Contador", "Analista Financeiro", "Controller", "Gerente Financeiro")),
                new Category("people", "Pessoas e RH", "Recrutamento, cultura e desenvolvimento",
                        Arrays.asList("Analista de RH", "Recrutador", "HRBP", "Gerente de RH"))
        );
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
    public static List<Category> categories() {
        return CATEGORIES;
    }
}
