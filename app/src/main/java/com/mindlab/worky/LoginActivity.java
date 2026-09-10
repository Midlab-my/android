package com.mindlab.worky;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.auth.SupabaseAuthClient;
import com.mindlab.worky.network.SupabaseConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutName;
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
    private boolean signupMode;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, getString(R.string.login_title));

        layoutName = findViewById(R.id.layoutName);
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

        if (!SupabaseConfig.isConfigured()) {
            textLoginStatus.setText(R.string.login_missing_config);
            btnLogin.setEnabled(false);
            btnToggleMode.setEnabled(false);
            btnCompanySignupLink.setEnabled(false);
            return;
        }

        btnLogin.setOnClickListener(v -> {
            if (signupMode) {
                doSignup();
            } else {
                doLogin();
            }
        });
        btnToggleMode.setOnClickListener(v -> setSignupMode(!signupMode));
        btnCompanySignupLink.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CompanySignupActivity.class)));
        setSignupMode(false);
    }

    private void setSignupMode(boolean signup) {
        signupMode = signup;
        layoutName.setVisibility(signup ? View.VISIBLE : View.GONE);
        textAuthTitle.setText(signup ? R.string.signup_title : R.string.login_title);
        textAuthSubtitle.setText(signup ? R.string.signup_subtitle : R.string.login_subtitle);
        btnLogin.setText(signup ? R.string.btn_signup : R.string.btn_login);
        btnToggleMode.setText(signup ? R.string.btn_go_login : R.string.btn_go_signup);
        textLoginStatus.setText("");
    }

    private void doLogin() {
        String email = textOf(inputEmail);
        String password = textOf(inputPassword);
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, getString(R.string.logging_in));
        SupabaseAuthClient auth = new SupabaseAuthClient();
        SessionStore store = new SessionStore(this);

        io.execute(() -> {
            try {
                AuthSession session = auth.signIn(email, password);
                store.save(session);
                runOnUiThread(() -> {
                    setLoading(false, "Logado como " + session.displayName());
                    Toast.makeText(this, R.string.login_ok, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.login_error));
                    Toast.makeText(this, R.string.login_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void doSignup() {
        String name = textOf(inputName);
        String email = textOf(inputEmail);
        String password = textOf(inputPassword);
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.signup_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, R.string.signup_password_short, Toast.LENGTH_SHORT).show();
            return;
        }

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
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.signup_error));
                    Toast.makeText(this, R.string.signup_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading, String status) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        textLoginStatus.setText(status);
        btnLogin.setEnabled(!loading);
        btnToggleMode.setEnabled(!loading);
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
