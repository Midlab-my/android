package com.mindlab.worky;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.auth.SupabaseAuthClient;
import com.mindlab.worky.network.SupabaseConfig;
import com.mindlab.worky.util.BrDocs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutName;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText inputName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private ProgressBar progressLogin;
    private TextView textLoginStatus;
    private TextView textAuthTitle;
    private TextView textAuthSubtitle;
    private MaterialButton btnLogin;
    private MaterialButton btnToggleMode;
    private MaterialButton btnCompanySignupLink;
    private MaterialButtonToggleGroup toggleAccountType;
    private boolean signupMode;
    private boolean companyMode;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, getString(R.string.login_title));

        layoutName = findViewById(R.id.layoutName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        progressLogin = findViewById(R.id.progressLogin);
        textLoginStatus = findViewById(R.id.textLoginStatus);
        textAuthTitle = findViewById(R.id.textAuthTitle);
        textAuthSubtitle = findViewById(R.id.textAuthSubtitle);
        btnLogin = findViewById(R.id.btnLogin);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        btnCompanySignupLink = findViewById(R.id.btnCompanySignupLink);
        toggleAccountType = findViewById(R.id.toggleAccountType);

        if (!SupabaseConfig.isConfigured()) {
            textLoginStatus.setText(R.string.login_missing_config);
            btnLogin.setEnabled(false);
            btnToggleMode.setEnabled(false);
            btnCompanySignupLink.setEnabled(false);
            return;
        }

        toggleAccountType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            companyMode = checkedId == R.id.btnTypeEmpresa;
            refreshModeLabels();
        });
        toggleAccountType.check(R.id.btnTypeCandidato);

        btnLogin.setOnClickListener(v -> {
            if (signupMode) {
                if (companyMode) {
                    startActivity(new Intent(this, CompanySignupActivity.class));
                } else {
                    doSignup();
                }
            } else {
                doLogin();
            }
        });
        btnToggleMode.setOnClickListener(v -> setSignupMode(!signupMode));
        btnCompanySignupLink.setOnClickListener(v ->
                startActivity(new Intent(this, CompanySignupActivity.class)));
        setSignupMode(false);
    }

    private void setSignupMode(boolean signup) {
        signupMode = signup;
        refreshModeLabels();
        textLoginStatus.setText("");
        clearFieldErrors();
    }

    private void refreshModeLabels() {
        layoutName.setVisibility(signupMode && !companyMode ? View.VISIBLE : View.GONE);
        btnCompanySignupLink.setVisibility(companyMode ? View.VISIBLE : View.GONE);

        if (companyMode) {
            textAuthTitle.setText(signupMode ? R.string.company_signup_title : R.string.login_title_empresa);
            textAuthSubtitle.setText(signupMode ? R.string.login_subtitle_empresa_signup : R.string.login_subtitle_empresa);
            btnLogin.setText(signupMode ? R.string.btn_go_company_signup_short : R.string.btn_login);
        } else {
            textAuthTitle.setText(signupMode ? R.string.signup_title : R.string.login_title);
            textAuthSubtitle.setText(signupMode ? R.string.signup_subtitle : R.string.login_subtitle);
            btnLogin.setText(signupMode ? R.string.btn_signup : R.string.btn_login);
        }
        btnToggleMode.setText(signupMode ? R.string.btn_go_login : R.string.btn_go_signup);
    }

    private void clearFieldErrors() {
        layoutName.setError(null);
        layoutEmail.setError(null);
        layoutPassword.setError(null);
    }

    private void doLogin() {
        clearFieldErrors();
        String email = textOf(inputEmail);
        String password = textOf(inputPassword);

        String emailErr = BrDocs.emailErrorMessage(email);
        if (emailErr != null) {
            layoutEmail.setError(emailErr);
            return;
        }
        if (password.isEmpty()) {
            layoutPassword.setError("Informe a senha.");
            return;
        }

        setLoading(true, getString(R.string.logging_in));
        SupabaseAuthClient auth = new SupabaseAuthClient();
        SessionStore store = new SessionStore(this);

        io.execute(() -> {
            try {
                AuthSession session = auth.signIn(email, password);
                if (companyMode && !session.isCompany()) {
                    store.clear();
                    throw new Exception(getString(R.string.login_not_company));
                }
                if (!companyMode && session.isCompany()) {
                    store.clear();
                    throw new Exception(getString(R.string.login_is_company));
                }
                store.save(session);
                runOnUiThread(() -> {
                    setLoading(false, "Logado como " + session.displayName());
                    Toast.makeText(this, R.string.login_ok, Toast.LENGTH_SHORT).show();
                    if (session.isCompany()) {
                        startActivity(new Intent(this, CompanyActivity.class));
                    }
                    finish();
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : getString(R.string.login_error);
                runOnUiThread(() -> {
                    setLoading(false, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void doSignup() {
        clearFieldErrors();
        String name = textOf(inputName);
        String email = textOf(inputEmail);
        String password = textOf(inputPassword);

        boolean ok = true;
        if (name.length() < 3) {
            layoutName.setError("Informe seu nome completo.");
            ok = false;
        }
        String emailErr = BrDocs.emailErrorMessage(email);
        if (emailErr != null) {
            layoutEmail.setError(emailErr);
            ok = false;
        }
        String passErr = BrDocs.passwordErrorMessage(password);
        if (passErr != null) {
            layoutPassword.setError(passErr);
            ok = false;
        }
        if (!ok) return;

        setLoading(true, getString(R.string.signing_up));
        SupabaseAuthClient auth = new SupabaseAuthClient();
        SessionStore store = new SessionStore(this);

        io.execute(() -> {
            try {
                AuthSession session = auth.signUp(name, email, password);
                runOnUiThread(() -> {
                    if (session != null) {
                        store.save(session);
                        setLoading(false, "Conta criada. Bem-vindo, " + session.displayName());
                        Toast.makeText(this, R.string.signup_ok, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        setLoading(false, getString(R.string.signup_confirm_email));
                        Toast.makeText(this, R.string.signup_confirm_email, Toast.LENGTH_LONG).show();
                        setSignupMode(false);
                    }
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : getString(R.string.signup_error);
                runOnUiThread(() -> {
                    setLoading(false, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading, String status) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        textLoginStatus.setText(status);
        btnLogin.setEnabled(!loading);
        btnToggleMode.setEnabled(!loading);
        toggleAccountType.setEnabled(!loading);
        if (btnCompanySignupLink != null) {
            btnCompanySignupLink.setEnabled(!loading);
        }
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
