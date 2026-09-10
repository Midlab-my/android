package com.mindlab.worky;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.auth.SupabaseAuthClient;
import com.mindlab.worky.data.MatchRepository;
import com.mindlab.worky.model.MatchProfileBuilder;
import com.mindlab.worky.model.ProfileMatchResult;
import com.mindlab.worky.ui.LoadingTicker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MatchActivity extends AppCompatActivity {

    public static final String EXTRA_CARGO = "cargo";
    public static final String EXTRA_SKILLS = "skills";

    private TextInputEditText inputCargo;
    private TextInputEditText inputBio;
    private TextInputEditText inputSkills;
    private View layoutLoadingMatch;
    private ProgressBar progressMatch;
    private TextView textLoadingStepMatch;
    private TextView textMatchPct;
    private TextView textMatchExplanation;
    private TextView labelMatched;
    private TextView textMatched;
    private TextView labelGaps;
    private TextView textGaps;
    private TextView textMatchStatus;
    private MaterialButton btnCalculateMatch;
    private MaterialButton btnMatchCache;
    private MaterialButton btnLoadAccountProfile;
    private LoadingTicker loadingTicker;

    private Map<String, Object> accountProfile;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, getString(R.string.match_title));

        inputCargo = findViewById(R.id.inputMatchCargo);
        inputBio = findViewById(R.id.inputMatchBio);
        inputSkills = findViewById(R.id.inputMatchSkills);
        layoutLoadingMatch = findViewById(R.id.layoutLoadingMatch);
        progressMatch = findViewById(R.id.progressMatch);
        textLoadingStepMatch = findViewById(R.id.textLoadingStepMatch);
        textMatchPct = findViewById(R.id.textMatchPct);
        textMatchExplanation = findViewById(R.id.textMatchExplanation);
        labelMatched = findViewById(R.id.labelMatched);
        textMatched = findViewById(R.id.textMatched);
        labelGaps = findViewById(R.id.labelGaps);
        textGaps = findViewById(R.id.textGaps);
        textMatchStatus = findViewById(R.id.textMatchStatus);
        btnCalculateMatch = findViewById(R.id.btnCalculateMatch);
        btnMatchCache = findViewById(R.id.btnMatchCache);
        btnLoadAccountProfile = findViewById(R.id.btnLoadAccountProfile);
        loadingTicker = LoadingTicker.forMatch();

        String cargo = getIntent().getStringExtra(EXTRA_CARGO);
        String skills = getIntent().getStringExtra(EXTRA_SKILLS);
        if (cargo != null && !cargo.trim().isEmpty()) {
            inputCargo.setText(cargo.trim());
        }
        if (skills != null && !skills.trim().isEmpty()) {
            inputSkills.setText(skills.trim());
        }

        btnCalculateMatch.setOnClickListener(v -> runMatch(false));
        btnMatchCache.setOnClickListener(v -> runMatch(true));
        btnLoadAccountProfile.setOnClickListener(v -> loadAccountProfile());
    }

    private void loadAccountProfile() {
        SessionManager sessions = new SessionManager(this);
        if (!sessions.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, "Carregando perfil…", false);
        SupabaseAuthClient auth = new SupabaseAuthClient();

        io.execute(() -> {
            try {
                AuthSession active = sessions.requireValidSession();
                Map<String, Object> profile = auth.fetchProfessionalProfile(active);
                AuthSession finalActive = active;
                runOnUiThread(() -> {
                    setLoading(false, "", false);
                    if (profile == null) {
                        Toast.makeText(this, R.string.profile_missing, Toast.LENGTH_LONG).show();
                        textMatchStatus.setText(R.string.profile_missing);
                        return;
                    }
                    accountProfile = profile;
                    String bio = SupabaseAuthClient.bioFromProfile(profile);
                    String skillsCsv = SupabaseAuthClient.skillsCsvFromProfile(profile);
                    if (!bio.isEmpty()) inputBio.setText(bio);
                    if (!skillsCsv.isEmpty()) inputSkills.setText(skillsCsv);
                    textMatchStatus.setText(getString(R.string.profile_loaded) + " (" + finalActive.displayName() + ")");
                    Toast.makeText(this, R.string.profile_loaded, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.error_match), false);
                    Toast.makeText(this, R.string.error_match, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void runMatch(boolean preferCache) {
        String cargo = textOf(inputCargo);
        String bio = textOf(inputBio);
        String skillsCsv = textOf(inputSkills);

        if (cargo.isEmpty()) {
            Toast.makeText(this, R.string.empty_cargo, Toast.LENGTH_SHORT).show();
            return;
        }
        if (accountProfile == null && MatchProfileBuilder.splitSkills(skillsCsv).isEmpty()) {
            Toast.makeText(this, R.string.empty_skills, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, preferCache ? "Lendo cache local…" : getString(R.string.matching), !preferCache);
        MatchRepository repository = new MatchRepository(this);
        Map<String, Object> profileSnapshot = accountProfile;

        io.execute(() -> {
            try {
                ProfileMatchResult result = repository.calculate(
                        cargo,
                        bio,
                        skillsCsv,
                        profileSnapshot,
                        preferCache
                );
                runOnUiThread(() -> {
                    setLoading(false, preferCache ? "Match do cache." : "Match calculado.", false);
                    showResult(result);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.error_match), false);
                    Toast.makeText(this, R.string.error_match, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showResult(ProfileMatchResult result) {
        textMatchPct.setVisibility(View.VISIBLE);
        textMatchExplanation.setVisibility(View.VISIBLE);
        labelMatched.setVisibility(View.VISIBLE);
        textMatched.setVisibility(View.VISIBLE);
        labelGaps.setVisibility(View.VISIBLE);
        textGaps.setVisibility(View.VISIBLE);

        textMatchPct.setText(result.pct + "%");
        textMatchExplanation.setText(
                result.explanation != null && !result.explanation.trim().isEmpty()
                        ? result.explanation.trim()
                        : "-"
        );
        textMatched.setText(join(result.matchedSafe()));
        textGaps.setText(join(result.gapsSafe()));
    }

    private void setLoading(boolean loading, String status, boolean withTicker) {
        btnCalculateMatch.setEnabled(!loading);
        btnMatchCache.setEnabled(!loading);
        btnLoadAccountProfile.setEnabled(!loading);
        inputCargo.setEnabled(!loading);
        inputBio.setEnabled(!loading);
        inputSkills.setEnabled(!loading);

        if (loading) {
            layoutLoadingMatch.setVisibility(View.VISIBLE);
            progressMatch.setProgress(withTicker ? 8 : 60);
            textLoadingStepMatch.setText(status != null && !status.isEmpty() ? status : getString(R.string.matching));
            if (status != null && !status.isEmpty()) {
                textMatchStatus.setText(status);
            }
            if (withTicker) {
                loadingTicker.start((elapsed, remaining, percent, stepLabel) -> {
                    textLoadingStepMatch.setText(stepLabel);
                    progressMatch.setProgress(percent);
                    if (remaining <= 0) {
                        textMatchStatus.setText(R.string.loading_almost);
                    } else {
                        textMatchStatus.setText(getString(
                                R.string.loading_timer,
                                LoadingTicker.formatDuration(elapsed),
                                LoadingTicker.formatDuration(remaining)
                        ));
                    }
                });
            } else {
                loadingTicker.stop();
            }
        } else {
            loadingTicker.stop();
            layoutLoadingMatch.setVisibility(View.GONE);
            if (status != null && !status.isEmpty()) {
                textMatchStatus.setText(status);
            }
        }
    }

    private static String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
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

    @Override
    protected void onDestroy() {
        if (loadingTicker != null) loadingTicker.stop();
        super.onDestroy();
        io.shutdownNow();
    }
}
