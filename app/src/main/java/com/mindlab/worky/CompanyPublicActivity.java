package com.mindlab.worky;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.model.PublicCatalogCompany;
import com.mindlab.worky.network.ApiConfig;

import java.util.ArrayList;
import java.util.List;

public class CompanyPublicActivity extends AppCompatActivity {

    public static final String EXTRA_JSON = "company_json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_public);

        String json = getIntent().getStringExtra(EXTRA_JSON);
        PublicCatalogCompany company = json != null ? new Gson().fromJson(json, PublicCatalogCompany.class) : null;
        if (company == null || company.id == null) {
            Toast.makeText(this, R.string.companies_empty, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, company.displayName());
        TextView meta = findViewById(R.id.textCompanyPublicMeta);
        TextView empty = findViewById(R.id.textCompanyPublicEmpty);
        RecyclerView recycler = findViewById(R.id.recyclerCompanyJobs);
        meta.setText(company.metaLine());

        List<PublicCatalogCompany.PublicCatalogJob> jobs = company.jobsOrEmpty();
        if (jobs.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            return;
        }

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new JobAdapter(jobs, this::openJob));
    }

    private void openJob(PublicCatalogCompany.PublicCatalogJob job) {
        String body = job.descricao != null && !job.descricao.trim().isEmpty()
                ? job.descricao.trim()
                : (job.requisitos != null ? job.requisitos.trim() : "");
        if (body.isEmpty()) body = getString(R.string.company_job_no_detail);
        new AlertDialog.Builder(this)
                .setTitle(job.titulo != null ? job.titulo : getString(R.string.company_job_fallback))
                .setMessage(body)
                .setPositiveButton(R.string.result_job_apply, (d, w) -> applyToJob(job))
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    private void applyToJob(PublicCatalogCompany.PublicCatalogJob job) {
        if (!new SessionStore(this).isLoggedIn()) {
            Toast.makeText(this, R.string.company_apply_login, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (job.id == null || job.id.trim().isEmpty()) {
            Toast.makeText(this, R.string.result_link_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FullSiteActivity.class);
        intent.putExtra(FullSiteActivity.EXTRA_URL, ApiConfig.WEB_URL + "/vagas/" + job.id.trim());
        startActivity(intent);
    }

    private static class JobAdapter extends RecyclerView.Adapter<JobAdapter.Holder> {
        interface Listener {
            void onClick(PublicCatalogCompany.PublicCatalogJob job);
        }

        private final List<PublicCatalogCompany.PublicCatalogJob> data;
        private final Listener listener;

        JobAdapter(List<PublicCatalogCompany.PublicCatalogJob> data, Listener listener) {
            this.data = data != null ? data : new ArrayList<>();
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PublicCatalogCompany.PublicCatalogJob job = data.get(position);
            holder.title.setText(job.titulo != null ? job.titulo : "Vaga");
            String local = job.local != null && !job.local.isEmpty() ? job.local : "-";
            String modelo = job.modelo != null && !job.modelo.isEmpty() ? job.modelo : "-";
            holder.company.setText(local + " · " + modelo);
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText("Worky");
            holder.itemView.setOnClickListener(v -> listener.onClick(job));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView company;
            final TextView badge;

            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.textJobTitle);
                company = itemView.findViewById(R.id.textJobCompany);
                badge = itemView.findViewById(R.id.textJobBadge);
            }
        }
    }
}
