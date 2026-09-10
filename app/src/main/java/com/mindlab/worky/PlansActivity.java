package com.mindlab.worky;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mindlab.worky.network.ApiConfig;
import com.mindlab.worky.ui.WorkyNav;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Planos candidato/empresa espelhados da PlansPage web. */
public class PlansActivity extends AppCompatActivity {

    private LinearLayout layoutCards;

    private static final class Plan {
        final String id;
        final String name;
        final String price;
        final List<String> features;
        final String cta;
        final boolean featured;
        final boolean current;

        Plan(String id, String name, String price, List<String> features, String cta, boolean featured, boolean current) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.features = features;
            this.cta = cta;
            this.featured = featured;
            this.current = current;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);
        WorkyNav.bindFromContent(this, getString(R.string.plans_title));

        layoutCards = findViewById(R.id.layoutPlansCards);
        RadioGroup audience = findViewById(R.id.radioPlansAudience);
        boolean company = getIntent().getBooleanExtra("company", false);
        if (company) {
            audience.check(R.id.radioPlansCompany);
        }
        audience.setOnCheckedChangeListener((group, checkedId) -> render(checkedId == R.id.radioPlansCompany));
        render(company);
    }

    private void render(boolean company) {
        layoutCards.removeAllViews();
        List<Plan> plans = company ? companyPlans() : candidatePlans();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Plan plan : plans) {
            View card = inflater.inflate(R.layout.item_plan_card, layoutCards, false);
            TextView badge = card.findViewById(R.id.textPlanBadge);
            TextView name = card.findViewById(R.id.textPlanName);
            TextView price = card.findViewById(R.id.textPlanPrice);
            TextView features = card.findViewById(R.id.textPlanFeatures);
            MaterialButton action = card.findViewById(R.id.btnPlanAction);

            badge.setVisibility(plan.featured ? View.VISIBLE : View.GONE);
            name.setText(plan.name);
            price.setText(plan.price);
            StringBuilder sb = new StringBuilder();
            for (String f : plan.features) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(f);
            }
            features.setText(sb.toString());
            action.setText(plan.cta);
            action.setEnabled(!plan.current);
            if (plan.current) {
                action.setAlpha(0.6f);
            }
            final String planId = plan.id;
            action.setOnClickListener(v -> onPlanClick(company, planId));
            layoutCards.addView(card);
        }
    }

    private void onPlanClick(boolean company, String planId) {
        if ("free".equals(planId) || "starter".equals(planId)) {
            Toast.makeText(this, R.string.plans_already_free, Toast.LENGTH_SHORT).show();
            return;
        }
        // Checkout/upgrade completo ainda e no site
        String path = company ? "/planos?tipo=empresa" : "/planos";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ApiConfig.WEB_URL + path)));
    }

    private static List<Plan> candidatePlans() {
        List<Plan> list = new ArrayList<>();
        list.add(new Plan("free", "Free", "R$ 0/mes",
                Arrays.asList("5 buscas/mes", "Analise basica de perfil"),
                "Plano Atual", false, true));
        list.add(new Plan("pro", "Pro", "R$ 9/mes",
                Arrays.asList("Buscas ilimitadas", "Simulador de Match de perfil", "Alertas em tempo real"),
                "Assinar Pro", true, false));
        return list;
    }

    private static List<Plan> companyPlans() {
        List<Plan> list = new ArrayList<>();
        list.add(new Plan("starter", "Starter", "R$ 0/mes",
                Arrays.asList("Cadastro de vaga ilimitado", "Vaga visivel para os candidatos"),
                "Plano Atual", false, true));
        list.add(new Plan("pro", "Pro", "R$ 199/mes",
                Arrays.asList("Lista de candidatos compativeis desbloqueada", "Simulador de Match de perfil", "Alertas em tempo real"),
                "Assinar Pro", true, false));
        list.add(new Plan("enterprise", "Enterprise", "R$ 500/mes",
                Arrays.asList("Candidatos compativeis ilimitados", "Insights personalizados", "API de dados estruturados", "Suporte tecnico premium"),
                "Ativar Enterprise", false, false));
        return list;
    }
}
