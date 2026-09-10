package com.mindlab.worky;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mindlab.worky.data.JobsRepository;
import com.mindlab.worky.model.JobItem;
import com.mindlab.worky.ui.LoadingTicker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobsActivity extends AppCompatActivity {

    public static final String EXTRA_CARGO = "cargo";
    public static final String EXTRA_FONTE = "fonte";
    public static final String EXTRA_LOCAL = "local";
    public static final String EXTRA_MODELO = "modelo";
    public static final String EXTRA_PREFER_CACHE = "prefer_cache";

    private View layoutLoadingJobs;
    private ProgressBar progressJobs;
    private TextView textLoadingStepJobs;
    private TextView textLoadingMetaJobs;
    private TextView textJobsTitle;
    private TextView textJobsMeta;
    private JobsAdapter adapter;
    private LoadingTicker loadingTicker;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs);

        String cargo = getIntent().getStringExtra(EXTRA_CARGO);
        String fonte = getIntent().getStringExtra(EXTRA_FONTE);
        String local = getIntent().getStringExtra(EXTRA_LOCAL);
        String modelo = getIntent().getStringExtra(EXTRA_MODELO);
        boolean preferCache = getIntent().getBooleanExtra(EXTRA_PREFER_CACHE, false);
        if (cargo == null || cargo.trim().isEmpty()) {
            Toast.makeText(this, R.string.empty_cargo, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        layoutLoadingJobs = findViewById(R.id.layoutLoadingJobs);
        progressJobs = findViewById(R.id.progressJobs);
        textLoadingStepJobs = findViewById(R.id.textLoadingStepJobs);
        textLoadingMetaJobs = findViewById(R.id.textLoadingMetaJobs);
        textJobsTitle = findViewById(R.id.textJobsTitle);
        textJobsMeta = findViewById(R.id.textJobsMeta);
        RecyclerView recyclerJobs = findViewById(R.id.recyclerJobs);
        com.google.android.material.button.MaterialButton btnMatchFromJobs = findViewById(R.id.btnMatchFromJobs);
        loadingTicker = LoadingTicker.forJobs();

        textJobsTitle.setText("Vagas: " + cargo.trim());
        adapter = new JobsAdapter(this::openJobLink);
        recyclerJobs.setLayoutManager(new LinearLayoutManager(this));
        recyclerJobs.setAdapter(adapter);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, "Vagas");

        final String cargoFinal = cargo.trim();
        final String localFinal = local != null ? local : "";
        final String modeloFinal = modelo != null ? modelo : "";
        btnMatchFromJobs.setOnClickListener(v -> {
            Intent intent = new Intent(this, MatchActivity.class);
            intent.putExtra(MatchActivity.EXTRA_CARGO, cargoFinal);
            startActivity(intent);
        });

        loadJobs(cargoFinal, fonte, localFinal, modeloFinal, preferCache);
    }

    private void loadJobs(String cargo, String fonte, String local, String modelo, boolean preferCache) {
        setLoading(true, preferCache);
        JobsRepository repository = new JobsRepository(this);

        io.execute(() -> {
            try {
                List<JobItem> jobs = repository.search(cargo, fonte, local, modelo, preferCache);
                runOnUiThread(() -> {
                    setLoading(false, preferCache);
                    adapter.submit(jobs);
                    textJobsMeta.setText(jobs.size() + " vaga(s)" + (preferCache ? " · cache" : ""));
                    if (jobs.isEmpty()) {
                        Toast.makeText(this, "Nenhuma vaga encontrada.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, preferCache);
                    textJobsMeta.setText(e.getMessage() != null ? e.getMessage() : "Erro ao buscar vagas.");
                    Toast.makeText(this, "Falha ao buscar vagas.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading, boolean preferCache) {
        if (loading) {
            layoutLoadingJobs.setVisibility(View.VISIBLE);
            textJobsMeta.setText(preferCache ? "Lendo cache local…" : getString(R.string.searching_jobs));
            progressJobs.setProgress(8);
            loadingTicker.start((elapsed, remaining, percent, stepLabel) -> {
                textLoadingStepJobs.setText(preferCache ? "Lendo cache local…" : stepLabel);
                progressJobs.setProgress(preferCache ? 90 : percent);
                if (preferCache) {
                    textLoadingMetaJobs.setText("Cache no celular…");
                } else if (remaining <= 0) {
                    textLoadingMetaJobs.setText(R.string.loading_almost);
                } else {
                    textLoadingMetaJobs.setText(getString(
                            R.string.loading_timer,
                            LoadingTicker.formatDuration(elapsed),
                            LoadingTicker.formatDuration(remaining)
                    ));
                }
            });
        } else {
            loadingTicker.stop();
            layoutLoadingJobs.setVisibility(View.GONE);
        }
    }

    private void openJobLink(JobItem job) {
        if (job.link == null || job.link.trim().isEmpty()) {
            Toast.makeText(this, "Link indisponivel.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(job.link.trim())));
    }

    @Override
    protected void onDestroy() {
        if (loadingTicker != null) loadingTicker.stop();
        super.onDestroy();
        io.shutdownNow();
    }

    private static class JobsAdapter extends RecyclerView.Adapter<JobsAdapter.Holder> {
        interface Listener {
            void onClick(JobItem job);
        }

        private final Listener listener;
        private final List<JobItem> data = new ArrayList<>();

        JobsAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<JobItem> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            JobItem job = data.get(position);
            holder.title.setText(job.displayTitle());
            String meta = safe(job.empresa) + " · " + job.displayLocal() + " · " + safe(job.modalidade);
            if (job.fonte != null && !job.fonte.isEmpty()) {
                meta = meta + " · " + job.fonte;
            }
            holder.company.setText(meta);
            boolean worky = job.destaqueWorky || "Worky".equalsIgnoreCase(job.tag);
            holder.badge.setVisibility(worky ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> listener.onClick(job));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private static String safe(String value) {
            return value == null || value.trim().isEmpty() ? "-" : value.trim();
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
