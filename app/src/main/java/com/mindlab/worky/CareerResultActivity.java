package com.mindlab.worky;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.mindlab.worky.model.CareerAnalysis;
import com.mindlab.worky.network.ApiConfig;

import java.util.List;

public class CareerResultActivity extends AppCompatActivity {

    public static final String EXTRA_JSON = "career_json";
    public static final String EXTRA_FROM_CACHE = "from_cache";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_career_result);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, "Relatorio");

        String json = getIntent().getStringExtra(EXTRA_JSON);
        boolean fromCache = getIntent().getBooleanExtra(EXTRA_FROM_CACHE, false);
        CareerAnalysis analysis = new Gson().fromJson(json, CareerAnalysis.class);
        if (analysis == null) {
            finish();
            return;
        }

        TextView textCarreira = findViewById(R.id.textCarreira);
        TextView textMeta = findViewById(R.id.textMeta);
        TextView textSalario = findViewById(R.id.textSalario);
        TextView textInsight = findViewById(R.id.textInsight);
        TextView textSkills = findViewById(R.id.textSkills);
        TextView textCerts = findViewById(R.id.textCerts);
        MaterialButton btnJobs = findViewById(R.id.btnJobs);
        MaterialButton btnMatch = findViewById(R.id.btnMatch);
        MaterialButton btnOpenWeb = findViewById(R.id.btnOpenWeb);

        textCarreira.setText(safe(analysis.carreira));
        String meta = "Demanda: " + safe(analysis.nivelDemanda)
                + " · Vagas: " + analysis.vagasAbertas
                + " · Crescimento: " + safe(analysis.crescimentoAnual);
        if (fromCache) {
            meta = meta + " · (cache local)";
        }
        textMeta.setText(meta);
        textSalario.setText(safe(analysis.mediaSalarial));
        textInsight.setText(safe(analysis.insightIA));
        textSkills.setText(join(analysis.techSkills()));
        textCerts.setText(joinCerts(analysis.certificacoesRecomendadas));

        btnJobs.setOnClickListener(v -> {
            Intent intent = new Intent(this, JobsActivity.class);
            intent.putExtra(JobsActivity.EXTRA_CARGO, safe(analysis.carreira));
            intent.putExtra(JobsActivity.EXTRA_FONTE, "google");
            startActivity(intent);
        });

        btnMatch.setOnClickListener(v -> {
            Intent intent = new Intent(this, MatchActivity.class);
            intent.putExtra(MatchActivity.EXTRA_CARGO, safe(analysis.carreira));
            intent.putExtra(MatchActivity.EXTRA_SKILLS, joinCsv(analysis.techSkills()));
            startActivity(intent);
        });

        btnOpenWeb.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullSiteActivity.class);
            intent.putExtra(FullSiteActivity.EXTRA_URL, ApiConfig.WEB_URL + "/carreira?cargo=" + safe(analysis.carreira).replace(" ", "+"));
            startActivity(intent);
        });
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private static String join(List<String> items) {
        if (items == null || items.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(" · ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static String joinCsv(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static String joinCerts(List<CareerAnalysis.Certificacao> items) {
        if (items == null || items.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (CareerAnalysis.Certificacao cert : items) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("• ").append(safe(cert.nome));
            if (cert.empresa != null && !cert.empresa.isEmpty()) {
                sb.append(" (").append(cert.empresa).append(")");
            }
        }
        return sb.toString();
    }
}
