package com.mindlab.worky;

import android.content.Intent;
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
import com.mindlab.worky.data.ProfileRepository;
import com.mindlab.worky.model.MatchProfileBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText inputName;
    private TextInputEditText inputCity;
    private TextInputEditText inputBio;
    private TextInputEditText inputSkills;
    private TextView textStatus;
    private ProgressBar progress;
    private MaterialButton btnSave;
    private MaterialButton btnToMatch;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, getString(R.string.profile_title));

        inputName = findViewById(R.id.inputProfileName);
        inputCity = findViewById(R.id.inputProfileCity);
        inputBio = findViewById(R.id.inputProfileBio);
        inputSkills = findViewById(R.id.inputProfileSkills);
        textStatus = findViewById(R.id.textProfileStatus);
        progress = findViewById(R.id.progressProfile);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnToMatch = findViewById(R.id.btnProfileToMatch);

        SessionManager sessions = new SessionManager(this);
        if (!sessions.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnSave.setOnClickListener(v -> save());
        btnToMatch.setOnClickListener(v -> {
            Intent intent = new Intent(this, MatchActivity.class);
            intent.putExtra(MatchActivity.EXTRA_SKILLS, textOf(inputSkills));
            startActivity(intent);
        });

        load();
    }

    private void load() {
        setLoading(true, "Carregando perfil…");
        SessionManager sessions = new SessionManager(this);
        ProfileRepository repo = new ProfileRepository();
        io.execute(() -> {
            try {
                AuthSession session = sessions.requireValidSession();
                ProfileRepository.ProfileDraft draft = repo.load(session);
                runOnUiThread(() -> {
                    inputName.setText(draft.nome);
                    inputCity.setText(draft.cidade);
                    inputBio.setText(draft.bio);
                    inputSkills.setText(draft.skillsCsv);
                    setLoading(false, draft.completed
                            ? getString(R.string.profile_status_complete)
                            : getString(R.string.profile_status_incomplete));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.profile_error));
                    Toast.makeText(this, R.string.profile_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void save() {
        String nome = textOf(inputName);
        String cidade = textOf(inputCity);
        String bio = textOf(inputBio);
        String skills = textOf(inputSkills);
        if (nome.isEmpty()) {
            Toast.makeText(this, R.string.profile_name_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (MatchProfileBuilder.splitSkills(skills).isEmpty()) {
            Toast.makeText(this, R.string.empty_skills, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, getString(R.string.profile_saving));
        SessionManager sessions = new SessionManager(this);
        ProfileRepository repo = new ProfileRepository();
        io.execute(() -> {
            try {
                AuthSession session = sessions.requireValidSession();
                repo.save(session, nome, bio, cidade, skills);
                runOnUiThread(() -> {
                    setLoading(false, getString(R.string.profile_status_complete));
                    Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.profile_error));
                    Toast.makeText(this, R.string.profile_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading, String status) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        textStatus.setText(status);
        btnSave.setEnabled(!loading);
        btnToMatch.setEnabled(!loading);
    }

    private static String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
