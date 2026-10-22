package com.mindlab.worky;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.JwtAal;
import com.mindlab.worky.auth.MfaEnrollment;
import com.mindlab.worky.auth.MfaRules;
import com.mindlab.worky.auth.PendingCompanySignup;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.auth.SupabaseAuthClient;
import com.mindlab.worky.auth.TotpFactor;
import com.mindlab.worky.data.CompanyRepository;
import com.mindlab.worky.model.CompanyProfile;
import com.mindlab.worky.ui.WorkyNav;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MfaActivity extends AppCompatActivity {

    public static final String EXTRA_ALLOW_ENROLL = "allow_enroll";

    private TextView textError;
    private TextView textSecret;
    private TextView textRecoveryHint;
    private TextView textLostHint;
    private ProgressBar progress;
    private MaterialButton btnEnroll;
    private MaterialButton btnVerify;
    private MaterialButton btnCancel;
    private View layoutQr;
    private View layoutCode;
    private ImageView imageQr;
    private WebView webQr;
    private Spinner spinnerFactors;
    private TextInputEditText inputCode;

    private SessionStore sessionStore;
    private boolean allowEnroll = true;
    private boolean busy;
    private String factorId = "";
    private final List<TotpFactor> verifiedFactors = new ArrayList<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfa);
        WorkyNav.bindFromContent(this, getString(R.string.mfa_title));

        sessionStore = new SessionStore(this);
        allowEnroll = getIntent().getBooleanExtra(EXTRA_ALLOW_ENROLL, true);

        textError = findViewById(R.id.textMfaError);
        textSecret = findViewById(R.id.textMfaSecret);
        textRecoveryHint = findViewById(R.id.textMfaRecoveryHint);
        textLostHint = findViewById(R.id.textMfaLostHint);
        progress = findViewById(R.id.progressMfa);
        btnEnroll = findViewById(R.id.btnMfaEnroll);
        btnVerify = findViewById(R.id.btnMfaVerify);
        btnCancel = findViewById(R.id.btnMfaCancel);
        layoutQr = findViewById(R.id.layoutMfaQr);
        layoutCode = findViewById(R.id.layoutMfaCode);
        imageQr = findViewById(R.id.imageMfaQr);
        webQr = findViewById(R.id.webMfaQr);
        spinnerFactors = findViewById(R.id.spinnerMfaFactors);
        inputCode = findViewById(R.id.inputMfaCode);

        webQr.getSettings().setJavaScriptEnabled(false);

        AuthSession session = sessionStore.get();
        if (session == null) {
            finish();
            return;
        }
        if (JwtAal.hasAal2(session.accessToken)) {
            finish();
            return;
        }

        btnEnroll.setOnClickListener(v -> enroll());
        btnVerify.setOnClickListener(v -> verify());
        btnCancel.setOnClickListener(v -> cancelAndExit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelAndExit();
            }
        });

        loadFactors();
    }

    private void loadFactors() {
        AuthSession session = sessionStore.get();
        if (session == null) {
            finish();
            return;
        }
        setBusy(true);
        showError(null);
        io.execute(() -> {
            try {
                List<TotpFactor> factors = new SupabaseAuthClient().listTotpFactors(session);
                List<TotpFactor> verified = MfaRules.verified(factors);
                runOnUiThread(() -> {
                    verifiedFactors.clear();
                    verifiedFactors.addAll(verified);
                    factorId = verified.isEmpty() ? "" : verified.get(0).id;
                    renderReady();
                    setBusy(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    showError(getString(R.string.mfa_list_error));
                });
            }
        });
    }

    private void renderReady() {
        boolean hasVerified = !factorId.isEmpty();
        btnEnroll.setVisibility(!hasVerified && allowEnroll ? View.VISIBLE : View.GONE);
        textRecoveryHint.setVisibility(!hasVerified && !allowEnroll ? View.VISIBLE : View.GONE);
        layoutCode.setVisibility(hasVerified ? View.VISIBLE : View.GONE);
        layoutQr.setVisibility(View.GONE);
        textLostHint.setVisibility(hasVerified ? View.VISIBLE : View.GONE);
        bindFactorSpinner();
    }

    private void bindFactorSpinner() {
        if (verifiedFactors.size() > 1) {
            List<String> labels = new ArrayList<>();
            for (TotpFactor factor : verifiedFactors) {
                labels.add(factor.label());
            }
            spinnerFactors.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    labels
            ));
            spinnerFactors.setVisibility(View.VISIBLE);
        } else {
            spinnerFactors.setVisibility(View.GONE);
        }
    }

    private void enroll() {
        if (busy) return;
        AuthSession session = sessionStore.get();
        if (session == null) return;
        setBusy(true);
        showError(null);
        io.execute(() -> {
            try {
                MfaEnrollment enrollment = new SupabaseAuthClient().enrollTotp(session);
                runOnUiThread(() -> {
                    factorId = enrollment.factorId;
                    layoutQr.setVisibility(View.VISIBLE);
                    layoutCode.setVisibility(View.VISIBLE);
                    btnEnroll.setVisibility(View.GONE);
                    textLostHint.setVisibility(View.GONE);
                    textSecret.setText(enrollment.secret);
                    showQr(enrollment.qrCode);
                    setBusy(false);
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : getString(R.string.mfa_enroll_error);
                runOnUiThread(() -> {
                    setBusy(false);
                    showError(msg);
                });
            }
        });
    }

    private void verify() {
        if (busy) return;
        AuthSession session = sessionStore.get();
        if (session == null) return;
        String selectedId = selectedFactorId();
        String code = inputCode.getText() != null ? inputCode.getText().toString().trim() : "";
        if (!MfaRules.isSixDigits(code)) {
            showError(getString(R.string.mfa_code_invalid));
            return;
        }
        setBusy(true);
        showError(null);
        io.execute(() -> {
            try {
                AuthSession next = new SupabaseAuthClient().verifyTotp(session, selectedId, code);
                next = new SessionManager(this).hydrateAccountType(next);
                sessionStore.save(next);
                finishPendingCompany(next);
                runOnUiThread(() -> {
                    setBusy(false);
                    Toast.makeText(this, R.string.mfa_ok, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    inputCode.setText("");
                    showError(getString(R.string.mfa_verify_error));
                });
            }
        });
    }

    private void finishPendingCompany(AuthSession session) throws Exception {
        PendingCompanySignup pending = PendingCompanySignup.load(this);
        if (pending == null) return;
        AuthSession companySession = session.withAccountType("empresa");
        JsonObject data = new JsonObject();
        data.addProperty("full_name", pending.name);
        data.addProperty("name", pending.name);
        data.addProperty("account_type", "empresa");
        data.addProperty("company_size", pending.size);
        data.addProperty("company_cnpj", pending.cnpj);
        data.addProperty("company_location", pending.location);
        data.addProperty("company_sector", pending.sector);
        data.addProperty("company_linkedin", pending.linkedin);
        new SupabaseAuthClient().mergeUserMetadata(companySession, data);
        CompanyRepository repo = new CompanyRepository();
        CompanyProfile existing = repo.fetchCompanyProfile(companySession);
        if (existing == null) {
            repo.createCompanyProfile(
                    companySession,
                    pending.name,
                    pending.size,
                    pending.cnpj,
                    pending.location,
                    pending.sector,
                    pending.linkedin
            );
        }
        sessionStore.save(companySession);
        PendingCompanySignup.clear(this);
        runOnUiThread(() -> startActivity(new Intent(this, CompanyActivity.class)));
    }

    private String selectedFactorId() {
        if (verifiedFactors.size() > 1) {
            int pos = spinnerFactors.getSelectedItemPosition();
            if (pos >= 0 && pos < verifiedFactors.size()) {
                return verifiedFactors.get(pos).id;
            }
        }
        return factorId;
    }

    private void showQr(String qr) {
        imageQr.setVisibility(View.GONE);
        webQr.setVisibility(View.GONE);
        if (qr == null || qr.trim().isEmpty()) return;
        String value = qr.trim();
        if (value.startsWith("data:image/png") || value.startsWith("data:image/jpeg") || value.startsWith("data:image/jpg")) {
            int comma = value.indexOf(',');
            if (comma > 0) {
                try {
                    byte[] bytes = Base64.decode(value.substring(comma + 1), Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) {
                        imageQr.setImageBitmap(bmp);
                        imageQr.setVisibility(View.VISIBLE);
                        return;
                    }
                } catch (Exception ignored) {
                    // cai no WebView
                }
            }
        }
        String src = value.startsWith("data:image/")
                ? value
                : "data:image/svg+xml;charset=utf-8," + Uri.encode(value);
        String html = "<html><body style='margin:0;background:#fff;display:flex;align-items:center;justify-content:center'>"
                + "<img alt='QR' style='width:240px;height:240px' src='" + src.replace("'", "") + "'/>"
                + "</body></html>";
        webQr.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        webQr.setVisibility(View.VISIBLE);
    }

    private void cancelAndExit() {
        sessionStore.clear();
        PendingCompanySignup.clear(this);
        finish();
    }

    private void setBusy(boolean loading) {
        busy = loading;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnEnroll.setEnabled(!loading);
        btnVerify.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
        inputCode.setEnabled(!loading);
    }

    private void showError(@Nullable String message) {
        if (message == null || message.trim().isEmpty()) {
            textError.setVisibility(View.GONE);
            textError.setText("");
            return;
        }
        textError.setVisibility(View.VISIBLE);
        textError.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
