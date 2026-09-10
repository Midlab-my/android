package com.mindlab.worky;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.data.CompanyRepository;
import com.mindlab.worky.model.CompanyCandidate;
import com.mindlab.worky.model.CompanyJob;
import com.mindlab.worky.model.CompanyProfile;
import com.mindlab.worky.network.ApiConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompanyActivity extends AppCompatActivity {

    private TextView textCompanyName;
    private TextView textCompanyMeta;
    private TextView textCompanyStatus;
    private ProgressBar progressCompany;
    private Spinner spinnerJobs;
    private MaterialButton btnEditJob;
    private MaterialButton btnDeleteJob;
    private CandidateAdapter adapter;

    private SessionStore sessionStore;
    private SessionManager sessionManager;
    private CompanyRepository repository;
    private CompanyProfile company;
    private final List<CompanyJob> jobs = new ArrayList<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean ignoreSpinner;
    private boolean needsReload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, getString(R.string.company_title));

        sessionStore = new SessionStore(this);
        sessionManager = new SessionManager(this);
        repository = new CompanyRepository();

        textCompanyName = findViewById(R.id.textCompanyName);
        textCompanyMeta = findViewById(R.id.textCompanyMeta);
        textCompanyStatus = findViewById(R.id.textCompanyStatus);
        progressCompany = findViewById(R.id.progressCompany);
        spinnerJobs = findViewById(R.id.spinnerJobs);
        MaterialButton btnNewJob = findViewById(R.id.btnNewJob);
        btnEditJob = findViewById(R.id.btnEditJob);
        btnDeleteJob = findViewById(R.id.btnDeleteJob);
        MaterialButton btnManageWeb = findViewById(R.id.btnManageWeb);
        RecyclerView recyclerCandidates = findViewById(R.id.recyclerCandidates);

        adapter = new CandidateAdapter();
        recyclerCandidates.setLayoutManager(new LinearLayoutManager(this));
        recyclerCandidates.setAdapter(adapter);

        btnNewJob.setOnClickListener(v -> openJobForm(null));
        btnEditJob.setOnClickListener(v -> openJobForm(selectedJob()));
        btnDeleteJob.setOnClickListener(v -> confirmDelete());
        btnManageWeb.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullSiteActivity.class);
            intent.putExtra(FullSiteActivity.EXTRA_URL, ApiConfig.WEB_URL + "/empresa");
            startActivity(intent);
        });

        spinnerJobs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignoreSpinner || position < 0 || position >= jobs.size()) {
                    updateJobActionsEnabled();
                    return;
                }
                updateJobActionsEnabled();
                loadCandidates(jobs.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateJobActionsEnabled();
            }
        });

        if (!sessionStore.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadPanel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needsReload) {
            needsReload = false;
            loadPanel();
        }
    }

    private void openJobForm(CompanyJob job) {
        if (company == null) {
            Toast.makeText(this, R.string.company_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, JobFormActivity.class);
        if (job != null) {
            intent.putExtra(JobFormActivity.EXTRA_JOB_ID, job.id);
            intent.putExtra(JobFormActivity.EXTRA_TITULO, job.titulo);
            intent.putExtra(JobFormActivity.EXTRA_LOCAL, job.local);
            intent.putExtra(JobFormActivity.EXTRA_MODELO, job.modelo);
            intent.putExtra(JobFormActivity.EXTRA_REQUISITOS, job.requisitos);
            intent.putExtra(JobFormActivity.EXTRA_DESCRICAO, job.descricao);
        }
        needsReload = true;
        startActivity(intent);
    }

    private CompanyJob selectedJob() {
        int pos = spinnerJobs.getSelectedItemPosition();
        if (pos < 0 || pos >= jobs.size()) return null;
        return jobs.get(pos);
    }

    private void confirmDelete() {
        CompanyJob job = selectedJob();
        if (job == null) {
            Toast.makeText(this, R.string.company_no_jobs, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_delete_job)
                .setMessage(getString(R.string.job_delete_confirm, job.titulo))
                .setPositiveButton(R.string.btn_delete_job, (d, w) -> deleteJob(job))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteJob(CompanyJob job) {
        setLoading(true, "Excluindo vaga…");
        io.execute(() -> {
            try {
                AuthSession session = ensureSession();
                repository.deleteJob(session, job.id);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.job_deleted, Toast.LENGTH_SHORT).show();
                    loadPanel();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.company_error));
                    Toast.makeText(this, R.string.company_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadPanel() {
        setLoading(true, "Carregando painel RH…");
        io.execute(() -> {
            try {
                AuthSession session = ensureSession();
                CompanyProfile profile = repository.fetchCompanyProfile(session);
                if (profile == null) {
                    runOnUiThread(() -> {
                        setLoading(false, getString(R.string.company_not_found));
                        textCompanyName.setText(R.string.company_title);
                        textCompanyMeta.setText("");
                        jobs.clear();
                        adapter.submit(new ArrayList<>());
                        updateJobActionsEnabled();
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.company_title)
                                .setMessage(R.string.company_not_found_action)
                                .setPositiveButton(R.string.btn_go_company_signup, (d, w) ->
                                        startActivity(new Intent(this, CompanySignupActivity.class)))
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    });
                    return;
                }
                List<CompanyJob> nextJobs = repository.listJobs(session);
                runOnUiThread(() -> {
                    company = profile;
                    textCompanyName.setText(profile.companyName.isEmpty() ? "Empresa" : profile.companyName);
                    textCompanyMeta.setText(
                            "Plano " + profile.planLabel()
                                    + (profile.location.isEmpty() ? "" : " · " + profile.location)
                                    + (profile.sector.isEmpty() ? "" : " · " + profile.sector)
                    );
                    jobs.clear();
                    jobs.addAll(nextJobs);
                    ignoreSpinner = true;
                    List<String> labels = new ArrayList<>();
                    if (jobs.isEmpty()) {
                        labels.add(getString(R.string.company_no_jobs));
                    } else {
                        for (CompanyJob job : jobs) {
                            labels.add(job.toString());
                        }
                    }
                    spinnerJobs.setAdapter(new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            labels
                    ));
                    ignoreSpinner = false;
                    updateJobActionsEnabled();
                    setLoading(false, jobs.isEmpty()
                            ? getString(R.string.company_no_jobs_create)
                            : jobs.size() + " vaga(s)");
                    if (!jobs.isEmpty()) {
                        loadCandidates(jobs.get(0));
                    } else {
                        adapter.submit(new ArrayList<>());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.company_error));
                    Toast.makeText(this, R.string.company_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadCandidates(CompanyJob job) {
        if (company == null) return;
        setLoading(true, "Buscando candidatos…");
        io.execute(() -> {
            try {
                AuthSession session = ensureSession();
                List<CompanyCandidate> candidates = repository.listCandidates(session, job, company);
                runOnUiThread(() -> {
                    adapter.submit(candidates);
                    String note = company.canUnlockCandidates()
                            ? candidates.size() + " candidato(s)"
                            : getString(R.string.company_plan_locked);
                    setLoading(false, note);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.company_error));
                });
            }
        });
    }

    private void updateJobActionsEnabled() {
        boolean hasJob = selectedJob() != null;
        btnEditJob.setEnabled(hasJob);
        btnDeleteJob.setEnabled(hasJob);
    }

    private AuthSession ensureSession() throws Exception {
        return sessionManager.requireValidSession();
    }

    private void setLoading(boolean loading, String status) {
        progressCompany.setVisibility(loading ? View.VISIBLE : View.GONE);
        textCompanyStatus.setText(status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private static class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.Holder> {
        private final List<CompanyCandidate> data = new ArrayList<>();

        void submit(List<CompanyCandidate> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_candidate, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CompanyCandidate c = data.get(position);
            holder.name.setText(c.locked ? "Candidato bloqueado" : safe(c.name));
            holder.match.setText(c.match + "%");
            String meta = safe(c.role);
            if (c.location != null && !c.location.isEmpty()) meta = meta + " · " + c.location;
            if (!c.locked && c.email != null && !c.email.isEmpty()) meta = meta + " · " + c.email;
            holder.meta.setText(meta);
            if (c.locked || c.skills == null || c.skills.isEmpty()) {
                holder.skills.setText(c.locked ? "" : "-");
            } else {
                holder.skills.setText(String.join(" · ", c.skills));
            }
            holder.lock.setVisibility(c.locked ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private static String safe(String value) {
            return value == null || value.trim().isEmpty() ? "-" : value.trim();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView match;
            final TextView meta;
            final TextView skills;
            final TextView lock;

            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.textCandidateName);
                match = itemView.findViewById(R.id.textCandidateMatch);
                meta = itemView.findViewById(R.id.textCandidateMeta);
                skills = itemView.findViewById(R.id.textCandidateSkills);
                lock = itemView.findViewById(R.id.textCandidateLock);
            }
        }
    }
}
