package com.mindlab.worky;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.data.CompanyRepository;
import com.mindlab.worky.model.CompanyJob;
import com.mindlab.worky.network.CepClient;
import com.mindlab.worky.util.BrDocs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobFormActivity extends AppCompatActivity {

    public static final String EXTRA_JOB_ID = "job_id";
    public static final String EXTRA_TITULO = "titulo";
    public static final String EXTRA_LOCAL = "local";
    public static final String EXTRA_MODELO = "modelo";
    public static final String EXTRA_REQUISITOS = "requisitos";
    public static final String EXTRA_DESCRICAO = "descricao";

    private static final String[] MODELOS = {"Remoto", "Híbrido", "Presencial"};

    private TextInputLayout layoutTitle;
    private TextInputLayout layoutCep;
    private TextInputLayout layoutLocal;
    private TextInputEditText inputTitle;
    private TextInputEditText inputCep;
    private TextInputEditText inputLocal;
    private TextInputEditText inputRequisitos;
    private TextInputEditText inputDescricao;
    private Spinner spinnerModelo;
    private ProgressBar progress;
    private TextView status;
    private MaterialButton btnSave;
    private boolean maskingCep;

    private String editingJobId;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_form);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, "Vaga");

        TextView titleView = findViewById(R.id.textJobFormTitle);
        layoutTitle = findViewById(R.id.layoutJobTitle);
        layoutCep = findViewById(R.id.layoutJobCep);
        layoutLocal = findViewById(R.id.layoutJobLocal);
        inputTitle = findViewById(R.id.inputJobTitle);
        inputCep = findViewById(R.id.inputJobCep);
        inputLocal = findViewById(R.id.inputJobLocal);
        inputRequisitos = findViewById(R.id.inputJobRequisitos);
        inputDescricao = findViewById(R.id.inputJobDescricao);
        spinnerModelo = findViewById(R.id.spinnerModelo);
        progress = findViewById(R.id.progressJobForm);
        status = findViewById(R.id.textJobFormStatus);
        btnSave = findViewById(R.id.btnSaveJob);

        spinnerModelo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MODELOS));
        bindCepLookup();

        editingJobId = getIntent().getStringExtra(EXTRA_JOB_ID);
        boolean editing = editingJobId != null && !editingJobId.trim().isEmpty();
        titleView.setText(editing ? R.string.job_form_edit_title : R.string.job_form_create_title);

        if (editing) {
            inputTitle.setText(safeExtra(EXTRA_TITULO));
            inputLocal.setText(safeExtra(EXTRA_LOCAL));
            inputRequisitos.setText(safeExtra(EXTRA_REQUISITOS));
            inputDescricao.setText(safeExtra(EXTRA_DESCRICAO));
            selectModelo(safeExtra(EXTRA_MODELO));
        }

        btnSave.setOnClickListener(v -> save());
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
                    inputLocal.setText(result.locationLabel);
                    layoutLocal.setError(null);
                    layoutCep.setHelperText(getString(R.string.cep_ok));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    layoutCep.setHelperText(null);
                    layoutCep.setError(e.getMessage() != null ? e.getMessage() : "CEP invalido.");
                });
            }
        });
    }

    private void selectModelo(String modelo) {
        for (int i = 0; i < MODELOS.length; i++) {
            if (MODELOS[i].equalsIgnoreCase(modelo)
                    || ("Hibrido".equalsIgnoreCase(modelo) && MODELOS[i].startsWith("H"))) {
                spinnerModelo.setSelection(i);
                return;
            }
        }
        spinnerModelo.setSelection(0);
    }

    private void save() {
        layoutTitle.setError(null);
        String titulo = textOf(inputTitle);
        String local = textOf(inputLocal);
        String requisitos = textOf(inputRequisitos);
        String descricao = textOf(inputDescricao);
        String modelo = MODELOS[Math.max(0, spinnerModelo.getSelectedItemPosition())];

        if (titulo.isEmpty()) {
            layoutTitle.setError(getString(R.string.job_title_required));
            return;
        }

        setLoading(true, getString(R.string.job_saving));
        SessionManager sessions = new SessionManager(this);
        CompanyRepository repository = new CompanyRepository();
        boolean editing = editingJobId != null && !editingJobId.isEmpty();

        io.execute(() -> {
            try {
                AuthSession session = sessions.requireValidSession();
                CompanyJob saved;
                if (editing) {
                    saved = repository.updateJob(session, editingJobId, titulo, local, modelo, requisitos, descricao);
                } else {
                    saved = repository.createJob(session, titulo, local, modelo, requisitos, descricao);
                }
                runOnUiThread(() -> {
                    setLoading(false, "Salvo: " + saved.titulo);
                    Toast.makeText(this, R.string.job_saved, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, e.getMessage() != null ? e.getMessage() : getString(R.string.job_save_error));
                    Toast.makeText(this, R.string.job_save_error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading, String message) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        status.setText(message);
        btnSave.setEnabled(!loading);
    }

    private String safeExtra(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value;
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
