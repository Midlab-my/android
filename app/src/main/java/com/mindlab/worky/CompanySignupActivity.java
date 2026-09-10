package com.mindlab.worky;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.auth.SupabaseAuthClient;
import com.mindlab.worky.data.CompanyRepository;
import com.mindlab.worky.network.SupabaseConfig;
import com.mindlab.worky.ui.WorkyNav;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompanySignupActivity extends AppCompatActivity {

    private static final String[] SIZES = {
            "1-10 funcionarios",
            "11-50 funcionarios",
            "51-200 funcionarios",
            "200+ funcionarios"
    };

    private static final String[] SECTORS = {
            "Tecnologia", "Financeiro", "Varejo", "Saude", "Educacao", "Industria", "Outro"
    };

    private int step = 1;
    private View layoutStep1;
    private View layoutStep2;
    private TextView textStep;
    private ProgressBar progressSteps;
    private TextInputEditText inputName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private TextInputEditText inputPasswordConfirm;
    private TextInputEditText inputCnpj;
    private TextInputEditText inputLocation;
    private TextInputEditText inputLinkedin;
    private Spinner spinnerSize;
    private Spinner spinnerSector;
    private CheckBox checkTerms;
    private ProgressBar progress;
    private TextView status;
    private MaterialButton btnSignup;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_signup);
        WorkyNav.bindFromContent(this, getString(R.string.company_signup_title));

        layoutStep1 = findViewById(R.id.layoutCompanyStep1);
        layoutStep2 = findViewById(R.id.layoutCompanyStep2);
        textStep = findViewById(R.id.textCompanyStep);
        progressSteps = findViewById(R.id.progressCompanySteps);
        inputName = findViewById(R.id.inputCompanyName);
        inputEmail = findViewById(R.id.inputCompanyEmail);
        inputPassword = findViewById(R.id.inputCompanyPassword);
        inputPasswordConfirm = findViewById(R.id.inputCompanyPasswordConfirm);
        inputCnpj = findViewById(R.id.inputCompanyCnpj);
        inputLocation = findViewById(R.id.inputCompanyLocation);
        inputLinkedin = findViewById(R.id.inputCompanyLinkedin);
        spinnerSize = findViewById(R.id.spinnerCompanySize);
        spinnerSector = findViewById(R.id.spinnerCompanySector);
        checkTerms = findViewById(R.id.checkCompanyTerms);
        progress = findViewById(R.id.progressCompanySignup);
        status = findViewById(R.id.textCompanySignupStatus);
        btnSignup = findViewById(R.id.btnCompanySignup);
        MaterialButton btnNext = findViewById(R.id.btnCompanyNext);
        MaterialButton btnBackStep = findViewById(R.id.btnCompanyBackStep);

        spinnerSize.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SIZES));
        spinnerSector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SECTORS));

        if (!SupabaseConfig.isConfigured()) {
            status.setText(R.string.login_missing_config);
            btnNext.setEnabled(false);
            btnSignup.setEnabled(false);
            return;
        }

        btnNext.setOnClickListener(v -> goStep2());
        btnBackStep.setOnClickListener(v -> goStep1());
        btnSignup.setOnClickListener(v -> submit());
        showStep(1);
    }

    private void goStep2() {
        String name = textOf(inputName);
        String email = textOf(inputEmail);
        String password = textOf(inputPassword);
        String confirm = textOf(inputPasswordConfirm);

        if (name.length() < 3 || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.company_signup_step1_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 8) {
            Toast.makeText(this, R.string.signup_password_short_8, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.signup_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }
        showStep(2);
    }

    private void goStep1() {
        showStep(1);
    }

    private void showStep(int next) {
        step = next;
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        textStep.setText(step == 1 ? R.string.company_step_1 : R.string.company_step_2);
        progressSteps.setProgress(step == 1 ? 50 : 100);
        findViewById(R.id.textCompanySignupTitle).setVisibility(View.VISIBLE);
        TextView subtitle = findViewById(R.id.textCompanySignupSubtitle);
        subtitle.setText(step == 1
                ? getString(R.string.company_signup_subtitle)
                : getString(R.string.company_signup_step2_subtitle));
    }

    private void submit() {
        String name = textOf(inputName);
        String email = textOf(inputEmail);
        String password = textOf(inputPassword);
        String cnpj = textOf(inputCnpj);
        String location = textOf(inputLocation);
        String linkedin = textOf(inputLinkedin);
        String size = SIZES[Math.max(0, spinnerSize.getSelectedItemPosition())];
        String sector = SECTORS[Math.max(0, spinnerSector.getSelectedItemPosition())];

        if (cnpj.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, R.string.company_signup_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkTerms.isChecked()) {
            Toast.makeText(this, R.string.terms_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, getString(R.string.signing_up));
        SupabaseAuthClient auth = new SupabaseAuthClient();
        CompanyRepository companyRepo = new CompanyRepository();
        SessionStore store = new SessionStore(this);

        io.execute(() -> {
            try {
                AuthSession session = auth.signUp(
                        name,
                        email,
                        password,
                        "empresa",
                        size,
                        cnpj,
                        location,
                        sector,
                        linkedin
                );

                if (session == null) {
                    runOnUiThread(() -> {
                        setLoading(false, getString(R.string.signup_confirm_email));
                        Toast.makeText(this, R.string.signup_confirm_email, Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                companyRepo.createCompanyProfile(session, name, size, cnpj, location, sector, linkedin);
                store.save(session);

                runOnUiThread(() -> {
                    setLoading(false, "Empresa criada.");
                    Toast.makeText(this, R.string.company_signup_ok, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, CompanyActivity.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.company_signup_error));
                    Toast.makeText(this, R.string.company_signup_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading, String message) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        status.setText(message);
        btnSignup.setEnabled(!loading);
        findViewById(R.id.btnCompanyNext).setEnabled(!loading);
        findViewById(R.id.btnCompanyBackStep).setEnabled(!loading);
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
