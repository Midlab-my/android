package com.mindlab.worky;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.data.CareerCacheEntity;
import com.mindlab.worky.data.CareerRepository;
import com.mindlab.worky.model.CareerAnalysis;
import com.mindlab.worky.ui.LoadingTicker;
import com.mindlab.worky.ui.SearchFiltersSheet;
import com.mindlab.worky.ui.WorkyInsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText inputCargo;
    private RadioGroup radioFonte;
    private View layoutLoading;
    private ProgressBar progress;
    private TextView textLoadingStep;
    private TextView statusText;
    private TextView textAuthStatus;
    private View layoutAccountActions;
    private MaterialButton btnAuth;
    private RecentAdapter recentAdapter;
    private SessionStore sessionStore;
    private SessionManager sessionManager;
    private LoadingTicker loadingTicker;
    private String selectedLocal = "";
    private String selectedModelo = "";
    private String selectedFonte = "google";
    private HorizontalScrollView scrollAds;
    private final Handler adHandler = new Handler(Looper.getMainLooper());
    private Runnable adTicker;

    private static final List<String> SPONSORS = Arrays.asList(
            "Curso Full Stack",
            "Bootcamp Data",
            "Certificacao Cloud",
            "Mentoria de Carreira",
            "Plataforma de Vagas",
            "English for Tech"
    );

    private CareerRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WorkyInsets.apply(this, findViewById(R.id.main), 4, 16);

        repository = new CareerRepository(this);
        sessionStore = new SessionStore(this);
        sessionManager = new SessionManager(this);

        inputCargo = findViewById(R.id.inputCargo);
        radioFonte = findViewById(R.id.radioFonte);
        layoutLoading = findViewById(R.id.layoutLoading);
        progress = findViewById(R.id.progress);
        textLoadingStep = findViewById(R.id.textLoadingStep);
        statusText = findViewById(R.id.statusText);
        textAuthStatus = findViewById(R.id.textAuthStatus);
        layoutAccountActions = findViewById(R.id.layoutAccountActions);
        btnAuth = findViewById(R.id.btnAuth);
        scrollAds = findViewById(R.id.scrollAds);
        loadingTicker = LoadingTicker.forCareer();
        MaterialButton btnAnalisar = findViewById(R.id.btnAnalisar);
        MaterialButton btnVagas = findViewById(R.id.btnVagas);
        MaterialButton btnProfile = findViewById(R.id.btnProfile);
        MaterialButton btnCompany = findViewById(R.id.btnCompany);
        MaterialButton btnPlans = findViewById(R.id.btnPlans);
        MaterialButton btnOpenSearch = findViewById(R.id.btnOpenSearch);
        MaterialButton btnHomeProfileCta = findViewById(R.id.btnHomeProfileCta);
        TextView textFilterSummary = findViewById(R.id.textFilterSummary);
        RecyclerView recyclerRecent = findViewById(R.id.recyclerRecent);

        recentAdapter = new RecentAdapter(entity -> openResult(gson.fromJson(entity.jsonPayload, CareerAnalysis.class), true));
        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecent.setAdapter(recentAdapter);

        btnAnalisar.setOnClickListener(v -> runAnalysis(false));
        btnVagas.setOnClickListener(v -> openJobs());
        btnProfile.setOnClickListener(v -> openProfile());
        btnCompany.setOnClickListener(v -> openCompany());
        btnPlans.setOnClickListener(v -> startActivity(new Intent(this, PlansActivity.class)));
        btnAuth.setOnClickListener(v -> onAuthClick());
        btnOpenSearch.setOnClickListener(v -> openSearchSheet());
        inputCargo.setOnClickListener(v -> openSearchSheet());
        btnHomeProfileCta.setOnClickListener(v -> openProfile());
        findViewById(R.id.cardSalary).setOnClickListener(v -> openSearchSheet());
        findViewById(R.id.cardSkills).setOnClickListener(v -> openSearchSheet());
        findViewById(R.id.cardJobsLive).setOnClickListener(v -> openJobs());
        findViewById(R.id.cardProfile).setOnClickListener(v -> openProfile());

        setupHomeTags();
        setupAdMarquee();

        reloadRecent();
        refreshAuthUi();
        refreshSessionInBackground();
        updateFilterSummary(textFilterSummary);
    }

    private void setupHomeTags() {
        ChipGroup group = findViewById(R.id.chipHomeTags);
        String[] tags = {"#Tech", "#Finance", "#Design", "#DataScience"};
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                inputCargo.setText(tag.replace("#", ""));
                openSearchSheet();
            });
            group.addView(chip);
        }
    }

    private void setupAdMarquee() {
        LinearLayout layout = findViewById(R.id.layoutAdChips);
        LayoutInflater inflater = LayoutInflater.from(this);
        // Duplica a lista pra parecer loop infinito
        List<String> loop = new ArrayList<>(SPONSORS);
        loop.addAll(SPONSORS);
        for (String item : loop) {
            View chip = inflater.inflate(R.layout.item_ad_chip, layout, false);
            TextView label = chip.findViewById(R.id.textAdChip);
            label.setText(item);
            layout.addView(chip);
        }

        adTicker = new Runnable() {
            @Override
            public void run() {
                if (scrollAds == null) return;
                int max = Math.max(0, layout.getWidth() - scrollAds.getWidth());
                if (max <= 0) {
                    adHandler.postDelayed(this, 800);
                    return;
                }
                int next = scrollAds.getScrollX() + 2;
                if (next >= max) {
                    scrollAds.scrollTo(0, 0);
                } else {
                    scrollAds.scrollTo(next, 0);
                }
                adHandler.postDelayed(this, 28);
            }
        };
        adHandler.postDelayed(adTicker, 600);
    }

    private void openSearchSheet() {
        SearchFiltersSheet sheet = SearchFiltersSheet.newInstance(
                inputCargo.getText() != null ? inputCargo.getText().toString() : "",
                selectedFonte
        );
        sheet.setListener(selection -> {
            inputCargo.setText(selection.cargo);
            selectedLocal = selection.local != null ? selection.local : "";
            selectedModelo = selection.modelo != null ? selection.modelo : "";
            selectedFonte = selection.fonte != null ? selection.fonte : "google";
            if ("scrape".equals(selectedFonte)) radioFonte.check(R.id.radioScrape);
            else if ("all".equals(selectedFonte)) radioFonte.check(R.id.radioAll);
            else radioFonte.check(R.id.radioGoogle);
            updateFilterSummary(findViewById(R.id.textFilterSummary));
            runAnalysis(false);
        });
        sheet.show(getSupportFragmentManager(), SearchFiltersSheet.TAG);
    }

    private void updateFilterSummary(TextView textFilterSummary) {
        if (textFilterSummary == null) return;
        String fonteLabel = "google".equals(selectedFonte) ? "Google"
                : "scrape".equals(selectedFonte) ? "Scraping" : "Todas";
        String local = selectedLocal.isEmpty() ? "Qualquer local" : selectedLocal;
        String modelo = selectedModelo.isEmpty() ? "Qualquer modelo" : selectedModelo;
        textFilterSummary.setText(local + " · " + modelo + " · " + fonteLabel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadRecent();
        refreshAuthUi();
    }

    private String selectedFonte() {
        return selectedFonte;
    }

    private void refreshSessionInBackground() {
        if (!sessionStore.isLoggedIn()) return;
        io.execute(() -> {
            AuthSession session = sessionManager.ensureValidSession();
            runOnUiThread(() -> {
                refreshAuthUi();
                if (session == null && sessionStore.get() == null) {
                    Toast.makeText(this, R.string.session_expired, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void onAuthClick() {
        if (sessionStore.isLoggedIn()) {
            sessionManager.clear();
            refreshAuthUi();
            Toast.makeText(this, "Sessao encerrada.", Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void refreshAuthUi() {
        AuthSession session = sessionStore.get();
        if (session != null) {
            textAuthStatus.setText(getString(R.string.auth_logged, session.displayName()));
            btnAuth.setText(R.string.btn_logout);
            layoutAccountActions.setVisibility(View.VISIBLE);
        } else {
            textAuthStatus.setText(R.string.auth_guest);
            btnAuth.setText(R.string.btn_login);
            layoutAccountActions.setVisibility(View.GONE);
        }
    }

    private void runAnalysis(boolean preferCache) {
        String cargo = inputCargo.getText() != null ? inputCargo.getText().toString().trim() : "";
        if (cargo.isEmpty()) {
            Toast.makeText(this, R.string.empty_cargo, Toast.LENGTH_SHORT).show();
            return;
        }

        String fonte = selectedFonte;
        setLoading(true);
        io.execute(() -> {
            try {
                CareerAnalysis analysis = repository.analyze(cargo, fonte, selectedLocal, selectedModelo, preferCache);
                runOnUiThread(() -> {
                    setLoading(false);
                    openResult(analysis, preferCache);
                    reloadRecent();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    layoutLoading.setVisibility(View.VISIBLE);
                    progress.setProgress(0);
                    textLoadingStep.setText(R.string.error_generic);
                    statusText.setText(e.getMessage() != null ? e.getMessage() : getString(R.string.error_generic));
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openResult(CareerAnalysis analysis, boolean fromCache) {
        Intent intent = new Intent(this, CareerResultActivity.class);
        intent.putExtra(CareerResultActivity.EXTRA_JSON, gson.toJson(analysis));
        intent.putExtra(CareerResultActivity.EXTRA_FROM_CACHE, fromCache);
        startActivity(intent);
    }

    private void openJobs() {
        String cargo = inputCargo.getText() != null ? inputCargo.getText().toString().trim() : "";
        if (cargo.isEmpty()) {
            Toast.makeText(this, R.string.empty_cargo, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, JobsActivity.class);
        intent.putExtra(JobsActivity.EXTRA_CARGO, cargo);
        intent.putExtra(JobsActivity.EXTRA_FONTE, selectedFonte);
        intent.putExtra(JobsActivity.EXTRA_LOCAL, selectedLocal);
        intent.putExtra(JobsActivity.EXTRA_MODELO, selectedModelo);
        startActivity(intent);
    }

    private void openProfile() {
        if (!sessionStore.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void openCompany() {
        if (!sessionStore.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        startActivity(new Intent(this, CompanyActivity.class));
    }

    private void reloadRecent() {
        io.execute(() -> {
            List<CareerCacheEntity> items = repository.recent();
            runOnUiThread(() -> recentAdapter.submit(items));
        });
    }

    private void setLoading(boolean loading) {
        findViewById(R.id.btnAnalisar).setEnabled(!loading);
        findViewById(R.id.btnVagas).setEnabled(!loading);
        inputCargo.setEnabled(!loading);
        for (int i = 0; i < radioFonte.getChildCount(); i++) {
            radioFonte.getChildAt(i).setEnabled(!loading);
        }

        if (loading) {
            layoutLoading.setVisibility(View.VISIBLE);
            progress.setProgress(8);
            textLoadingStep.setText(R.string.analyzing);
            statusText.setText("");
            loadingTicker.start((elapsed, remaining, percent, stepLabel) -> {
                textLoadingStep.setText(stepLabel);
                progress.setProgress(percent);
                if (remaining <= 0) {
                    statusText.setText(R.string.loading_almost);
                } else {
                    statusText.setText(getString(
                            R.string.loading_timer,
                            LoadingTicker.formatDuration(elapsed),
                            LoadingTicker.formatDuration(remaining)
                    ));
                }
            });
        } else {
            loadingTicker.stop();
            layoutLoading.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (adTicker != null) adHandler.removeCallbacks(adTicker);
        if (loadingTicker != null) loadingTicker.stop();
        super.onDestroy();
        io.shutdownNow();
    }

    private static class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.Holder> {
        interface Listener {
            void onClick(CareerCacheEntity entity);
        }

        private final Listener listener;
        private final List<CareerCacheEntity> data = new ArrayList<>();

        RecentAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<CareerCacheEntity> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CareerCacheEntity entity = data.get(position);
            holder.text.setText(entity.cargoLabel);
            holder.itemView.setOnClickListener(v -> listener.onClick(entity));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView text;

            Holder(@NonNull View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.textRecent);
            }
        }
    }
}
