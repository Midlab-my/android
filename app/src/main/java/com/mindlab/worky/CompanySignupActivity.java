package com.mindlab.worky;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.JwtAal;
import com.mindlab.worky.auth.PendingCompanySignup;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.auth.SupabaseAuthClient;
import com.mindlab.worky.data.CompanyRepository;
import com.mindlab.worky.model.CompanyProfile;
import com.mindlab.worky.network.CepClient;
import com.mindlab.worky.network.SupabaseConfig;
import com.mindlab.worky.ui.WorkyNav;
import com.mindlab.worky.util.BrDocs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompanySignupActivity extends AppCompatActivity {

    public static final String EXTRA_COMPLETE_EXISTING = "complete_existing";

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
    private TextInputLayout layoutCnpj;
    private TextInputLayout layoutCep;
    private TextInputLayout layoutLocation;
    private TextInputEditText inputCnpj;
    private TextInputEditText inputCep;
    private TextInputEditText inputLocation;
    private TextInputEditText inputLinkedin;
    private Spinner spinnerSize;
    private Spinner spinnerSector;
    private CheckBox checkTerms;
    private ProgressBar progress;
    private TextView status;
    private MaterialButton btnSignup;
    private boolean maskingCnpj;
    private boolean maskingCep;
    private boolean completeExisting;
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
        layoutCnpj = findViewById(R.id.layoutCompanyCnpj);
        layoutCep = findViewById(R.id.layoutCompanyCep);
        layoutLocation = findViewById(R.id.layoutCompanyLocation);
        inputCnpj = findViewById(R.id.inputCompanyCnpj);
        inputCep = findViewById(R.id.inputCompanyCep);
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

        bindCnpjMask();
        bindCepLookup();

        if (!SupabaseConfig.isConfigured()) {
            status.setText(R.string.login_missing_config);
            btnNext.setEnabled(false);
            btnSignup.setEnabled(false);
            return;
        }

        btnNext.setOnClickListener(v -> goStep2());
        btnBackStep.setOnClickListener(v -> goStep1());
        btnSignup.setOnClickListener(v -> submit());

        AuthSession existing = new SessionStore(this).get();
        completeExisting = getIntent().getBooleanExtra(EXTRA_COMPLETE_EXISTING, false)
                && existing != null
                && existing.accessToken != null
                && !existing.accessToken.isEmpty();
        if (completeExisting) {
            WorkyNav.bindFromContent(this, getString(R.string.company_complete_title));
            if (existing.name != null && !existing.name.isEmpty()) {
                inputName.setText(existing.name);
            }
            hideAuthFields();
        }
        showStep(1);
    }

    private void hideAuthFields() {
        View emailLayout = (View) inputEmail.getParent();
        View passwordLayout = (View) inputPassword.getParent();
        View confirmLayout = (View) inputPasswordConfirm.getParent();
        if (emailLayout != null) emailLayout.setVisibility(View.GONE);
        if (passwordLayout != null) passwordLayout.setVisibility(View.GONE);
        if (confirmLayout != null) confirmLayout.setVisibility(View.GONE);
    }

    private void bindCnpjMask() {
        inputCnpj.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (maskingCnpj) return;
                maskingCnpj = true;
                String formatted = BrDocs.formatCnpj(s.toString());
                if (!formatted.equals(s.toString())) {
                    inputCnpj.setText(formatted);
                    inputCnpj.setSelection(formatted.length());
                }
                layoutCnpj.setError(null);
                maskingCnpj = false;
            }
        });
    }

    private void bindCepLookup() {
        inputCep.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (maskingCep) return;
                maskingCep = true;
                String formatted = BrDocs.formatCep(s.toString());
                if (!formatted.equals(s.toString())) {
                    inputCep.setText(formatted);
                    inputCep.setSelection(formatted.length());
                }
                layoutCep.setError(null);
                maskingCep = false;

                if (BrDocs.onlyDigits(formatted, 8).length() == 8) {
                    lookupCep(formatted);
                }
            }
        });
    }

    private void lookupCep(String cep) {
        layoutCep.setHelperText(getString(R.string.cep_looking_up));
        io.execute(() -> {
            try {
                CepClient.CepResult result = new CepClient().lookup(cep);
                runOnUiThread(() -> {
                    inputLocation.setText(result.locationLabel);
                    layoutLocation.setError(null);
                    layoutCep.setHelperText(getString(R.string.cep_ok));
                    layoutCep.setError(null);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    inputLocation.setText("");
                    layoutCep.setHelperText(null);
                    layoutCep.setError(e.getMessage() != null ? e.getMessage() : "CEP invalido.");
                });
            }
        });
    }

    private void goStep2() {
        String name = textOf(inputName);
        if (name.length() < 3) {
            Toast.makeText(this, R.string.company_signup_step1_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!completeExisting) {
            String email = textOf(inputEmail);
            String password = textOf(inputPassword);
            String confirm = textOf(inputPasswordConfirm);
            String emailErr = BrDocs.emailErrorMessage(email);
            if (emailErr != null) {
                Toast.makeText(this, emailErr, Toast.LENGTH_SHORT).show();
                return;
            }
            String passErr = BrDocs.passwordErrorMessage(password);
            if (passErr != null) {
                Toast.makeText(this, passErr, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, R.string.signup_password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }
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
        TextView title = findViewById(R.id.textCompanySignupTitle);
        title.setVisibility(View.VISIBLE);
        title.setText(completeExisting ? R.string.company_complete_title : R.string.company_signup_title);
        TextView subtitle = findViewById(R.id.textCompanySignupSubtitle);
        if (completeExisting) {
            subtitle.setText(step == 1
                    ? getString(R.string.company_complete_subtitle)
                    : getString(R.string.company_signup_step2_subtitle));
        } else {
            subtitle.setText(step == 1
                    ? getString(R.string.company_signup_subtitle)
                    : getString(R.string.company_signup_step2_subtitle));
        }
        btnSignup.setText(completeExisting ? R.string.btn_complete_company_profile : R.string.btn_company_signup);
    }

    private void submit() {
        layoutCnpj.setError(null);
        layoutCep.setError(null);
        layoutLocation.setError(null);

        String name = textOf(inputName);
        String email = textOf(inputEmail);
        String password = textOf(inputPassword);
        String cnpjRaw = textOf(inputCnpj);
        String cep = textOf(inputCep);
        String location = textOf(inputLocation);
        String linkedin = textOf(inputLinkedin);
        String size = SIZES[Math.max(0, spinnerSize.getSelectedItemPosition())];
        String sector = SECTORS[Math.max(0, spinnerSector.getSelectedItemPosition())];

        String cnpjErr = BrDocs.cnpjErrorMessage(cnpjRaw);
        if (cnpjErr != null) {
            layoutCnpj.setError(cnpjErr);
            return;
        }
        String cepErr = BrDocs.cepErrorMessage(cep);
        if (cepErr != null) {
            layoutCep.setError(cepErr);
            return;
        }
        if (location.isEmpty()) {
            layoutLocation.setError("Consulte um CEP valido para preencher a cidade.");
            return;
        }
        String linkedInErr = BrDocs.linkedInErrorMessage(linkedin);
        if (linkedInErr != null) {
            Toast.makeText(this, linkedInErr, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkTerms.isChecked()) {
            Toast.makeText(this, R.string.terms_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String cnpj = BrDocs.onlyDigits(cnpjRaw, 14);

        setLoading(true, getString(completeExisting ? R.string.job_saving : R.string.signing_up));
        SupabaseAuthClient auth = new SupabaseAuthClient();
        CompanyRepository companyRepo = new CompanyRepository();
        SessionStore store = new SessionStore(this);

        io.execute(() -> {
            try {
                AuthSession companySession;
                if (completeExisting) {
                    AuthSession session = store.get();
                    if (session == null) {
                        throw new Exception(getString(R.string.login_required));
                    }
                    companySession = session.withAccountType("empresa");
                    store.save(companySession);
                    if (!JwtAal.hasAal2(companySession.accessToken)) {
                        new PendingCompanySignup(name, size, cnpj, location, sector, linkedin).save(this);
                        runOnUiThread(() -> {
                            startActivity(new Intent(this, MfaActivity.class));
                            finish();
                        });
                        return;
                    }
                    JsonObject data = new JsonObject();
                    data.addProperty("full_name", name);
                    data.addProperty("name", name);
                    data.addProperty("account_type", "empresa");
                    data.addProperty("company_size", size);
                    data.addProperty("company_cnpj", cnpj);
                    data.addProperty("company_location", location);
                    data.addProperty("company_sector", sector);
                    data.addProperty("company_linkedin", linkedin);
                    auth.mergeUserMetadata(companySession, data);
                    CompanyProfile existingProfile = companyRepo.fetchCompanyProfile(companySession);
                    if (existingProfile == null) {
                        companyRepo.createCompanyProfile(companySession, name, size, cnpj, location, sector, linkedin);
                    }
                } else {
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

                    companySession = session.withAccountType("empresa");
                    store.save(companySession);
                    new PendingCompanySignup(name, size, cnpj, location, sector, linkedin).save(this);
                    if (!JwtAal.hasAal2(companySession.accessToken)) {
                        runOnUiThread(() -> {
                            startActivity(new Intent(this, MfaActivity.class));
                            finish();
                        });
                        return;
                    }
                    companyRepo.createCompanyProfile(companySession, name, size, cnpj, location, sector, linkedin);
                    PendingCompanySignup.clear(this);
                }
                store.save(companySession);

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
