package com.mindlab.worky;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.model.PublicCatalogCompany;
import com.mindlab.worky.network.WorkyApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompaniesActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthSession session = new SessionStore(this).get();
        if (session != null && session.isCompany()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_companies);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, getString(R.string.companies_title));

        RecyclerView recycler = findViewById(R.id.recyclerCompanies);
        ProgressBar progress = findViewById(R.id.progressCompanies);
        TextView empty = findViewById(R.id.textCompaniesEmpty);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        CatalogAdapter adapter = new CatalogAdapter(company -> {
            Intent intent = new Intent(this, CompanyPublicActivity.class);
            intent.putExtra(CompanyPublicActivity.EXTRA_JSON, gson.toJson(company));
            startActivity(intent);
        });
        recycler.setAdapter(adapter);

        io.execute(() -> {
            List<PublicCatalogCompany> companies = new WorkyApiClient().fetchPublicCatalog();
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (companies.isEmpty()) {
                    empty.setVisibility(View.VISIBLE);
                    recycler.setVisibility(View.GONE);
                } else {
                    empty.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                    adapter.submit(companies);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private static class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.Holder> {
        interface Listener {
            void onClick(PublicCatalogCompany company);
        }

        private final Listener listener;
        private final List<PublicCatalogCompany> data = new ArrayList<>();

        CatalogAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<PublicCatalogCompany> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_partner_company, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PublicCatalogCompany company = data.get(position);
            holder.initials.setText(company.initials());
            holder.name.setText(company.displayName());
            holder.meta.setText(company.metaLine());
            holder.highlight.setText(company.highlight());
            holder.jobs.setText(holder.itemView.getContext().getString(R.string.company_jobs_open, company.jobsOpen()));
            holder.itemView.setOnClickListener(v -> listener.onClick(company));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView initials;
            final TextView name;
            final TextView meta;
            final TextView highlight;
            final TextView jobs;

            Holder(@NonNull View itemView) {
                super(itemView);
                initials = itemView.findViewById(R.id.textPartnerInitials);
                name = itemView.findViewById(R.id.textPartnerName);
                meta = itemView.findViewById(R.id.textPartnerMeta);
                highlight = itemView.findViewById(R.id.textPartnerHighlight);
                jobs = itemView.findViewById(R.id.textPartnerJobs);
            }
        }
    }
}
