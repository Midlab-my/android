package com.mindlab.worky;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.mindlab.worky.model.CareerAnalysis;
import com.mindlab.worky.model.JobItem;
import com.mindlab.worky.model.SalaryProgression;
import com.mindlab.worky.network.ApiConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CareerResultActivity extends AppCompatActivity {

    public static final String EXTRA_JSON = "career_json";
    public static final String EXTRA_FROM_CACHE = "from_cache";
    public static final String EXTRA_FONTE = "fonte";
    public static final String EXTRA_LOCAL = "local";
    public static final String EXTRA_MODELO = "modelo";

    private static final int JOBS_PREVIEW_LIMIT = 6;
    private static final int[] LOGO_COLORS = {
            0xFF1E293B, 0xFF374151, 0xFF4F46E5, 0xFF005858, 0xFF003EC7, 0xFF7C3AED
    };

    private CareerAnalysis analysis;
    private String fonte = "";
    private String local = "";
    private String modelo = "";
    private List<JobItem> jobs = new ArrayList<>();
    private boolean showAllJobs;
    private LinearLayout layoutJobs;
    private MaterialButton btnMoreJobs;
    private TextView textJobsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_career_result);

        String json = getIntent().getStringExtra(EXTRA_JSON);
        analysis = new Gson().fromJson(json, CareerAnalysis.class);
        if (analysis == null) {
            finish();
            return;
        }
        fonte = extra(EXTRA_FONTE);
        local = extra(EXTRA_LOCAL);
        modelo = extra(EXTRA_MODELO);

        String careerName = safe(analysis.carreira);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, careerName);

        bindTitle(careerName);
        bindFilters();
        ((TextView) findViewById(R.id.textInsight)).setText(safe(analysis.insightIA));
        bindStats();
        bindSalary();
        bindSkills();
        bindCerts();
        bindCourses();

        findViewById(R.id.btnMatch).setOnClickListener(v -> openMatch());

        layoutJobs = findViewById(R.id.layoutJobs);
        btnMoreJobs = findViewById(R.id.btnMoreJobs);
        textJobsEmpty = findViewById(R.id.textJobsEmpty);
        jobs = filterAndSort(analysis.jobs(), local, modelo);
        bindDemand();
        renderJobs();
        btnMoreJobs.setOnClickListener(v -> {
            showAllJobs = !showAllJobs;
            renderJobs();
        });
    }

    private void bindTitle(String careerName) {
        TextView title = findViewById(R.id.textCarreira);
        String[] words = careerName.replace(";", " ").trim().split("\\s+");
        if (words.length <= 1) {
            title.setText(careerName);
            return;
        }
        String accent = words[words.length - 1];
        String prefix = careerName.substring(0, careerName.length() - accent.length());
        SpannableString span = new SpannableString(prefix + accent);
        span.setSpan(
                new ForegroundColorSpan(getColor(R.color.worky_blue)),
                prefix.length(),
                span.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        title.setText(span);
    }

    private void bindFilters() {
        ChipGroup group = findViewById(R.id.layoutFilterChips);
        group.removeAllViews();
        if (!local.isEmpty()) addFilterChip(group, local);
        if (!modelo.isEmpty()) addFilterChip(group, modelo);
        String fonteLabel = fonteLabel(fonte);
        if (!fonteLabel.isEmpty()) addFilterChip(group, fonteLabel);
        group.setVisibility(group.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void addFilterChip(ChipGroup group, String label) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(0xFFF1F5F9));
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColor(ColorStateList.valueOf(0xFFE2E8F0));
        chip.setChipCornerRadius(8f * getResources().getDisplayMetrics().density);
        chip.setTextColor(0xFF334155);
        group.addView(chip);
    }

    private void bindStats() {
        ((TextView) findViewById(R.id.textStatJobs)).setText(
                String.format(Locale.forLanguageTag("pt-BR"), "%d", analysis.vagasAbertas)
        );
        TextView caption = findViewById(R.id.textStatJobsCaption);
        if (analysis.crescimentoMensal != null && !analysis.crescimentoMensal.trim().isEmpty()) {
            caption.setText(analysis.crescimentoMensal.trim());
            caption.setVisibility(View.VISIBLE);
        } else {
            caption.setVisibility(View.GONE);
        }
        ((TextView) findViewById(R.id.textStatDemand)).setText(safe(analysis.nivelDemanda));
        String growth = analysis.crescimentoAnual;
        ((TextView) findViewById(R.id.textStatGrowth)).setText(
                growth == null || growth.trim().isEmpty() ? "Não informado" : growth.trim()
        );
    }

    private void bindSalary() {
        LinearLayout layout = findViewById(R.id.layoutSalaryBands);
        layout.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (SalaryProgression.Band band : SalaryProgression.from(analysis.mediaSalarial)) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = Math.round(8 * density);
            row.setPadding(0, pad, 0, pad);

            View dot = new View(this);
            int size = Math.round(8 * density);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(size, size);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundResource(R.drawable.bg_salary_dot);
            if (band.active) {
                dot.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            } else {
                dot.setBackgroundTintList(ColorStateList.valueOf(0x99FFFFFF));
            }
            row.addView(dot);

            TextView label = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            labelParams.setMarginStart(Math.round(10 * density));
            label.setLayoutParams(labelParams);
            label.setText(band.label);
            label.setTextColor(Color.WHITE);
            label.setTextSize(14);
            if (band.active) label.setTypeface(label.getTypeface(), Typeface.BOLD);
            row.addView(label);

            TextView range = new TextView(this);
            range.setText(band.range);
            range.setTextColor(band.active ? Color.WHITE : 0xC7FFFFFF);
            range.setTextSize(13);
            row.addView(range);
            layout.addView(row);
        }
    }

    private void bindSkills() {
        fillSkillChips(findViewById(R.id.chipHardSkills), analysis.techSkills());
        fillSkillChips(findViewById(R.id.chipSoftSkills), analysis.softSkills());
    }

    private void fillSkillChips(ChipGroup group, List<String> items) {
        group.removeAllViews();
        List<String> values = items == null || items.isEmpty()
                ? Collections.singletonList(getString(R.string.result_analyzing))
                : items;
        for (String item : values) {
            Chip chip = new Chip(this);
            chip.setText(item);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.WHITE));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColor(ColorStateList.valueOf(0xFFE2E8F0));
            chip.setChipCornerRadius(8f * getResources().getDisplayMetrics().density);
            chip.setTextColor(0xFF334155);
            chip.setEnsureMinTouchTargetSize(false);
            group.addView(chip);
        }
    }

    private void bindCerts() {
        LinearLayout layout = findViewById(R.id.layoutCerts);
        layout.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        List<CareerAnalysis.Certificacao> certs = analysis.certificacoesRecomendadas;
        if (certs == null || certs.isEmpty()) {
            View view = inflater.inflate(R.layout.item_result_cert, layout, false);
            ((TextView) view.findViewById(R.id.textCertInitials)).setText("WY");
            ((TextView) view.findViewById(R.id.textCertIssuer)).setText("Worky");
            ((TextView) view.findViewById(R.id.textCertName)).setText(R.string.result_analyzing);
            layout.addView(view);
            return;
        }
        for (CareerAnalysis.Certificacao cert : certs) {
            View view = inflater.inflate(R.layout.item_result_cert, layout, false);
            String issuer = blank(cert.empresa) ? "Worky" : cert.empresa.trim();
            ((TextView) view.findViewById(R.id.textCertInitials)).setText(initials(issuer));
            ((TextView) view.findViewById(R.id.textCertIssuer)).setText(issuer);
            ((TextView) view.findViewById(R.id.textCertName)).setText(blank(cert.nome) ? getString(R.string.result_analyzing) : cert.nome.trim());
            view.setOnClickListener(v -> openExternal(cert.url));
            layout.addView(view);
        }
    }

    private void bindCourses() {
        LinearLayout layout = findViewById(R.id.layoutCourses);
        layout.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        List<CareerAnalysis.Curso> courses = analysis.cursosRecomendados;
        if (courses == null || courses.isEmpty()) {
            View view = inflater.inflate(R.layout.item_result_course, layout, false);
            ((TextView) view.findViewById(R.id.textCourseInitials)).setText("WY");
            ((TextView) view.findViewById(R.id.textCoursePlatform)).setText("Worky");
            ((TextView) view.findViewById(R.id.textCourseName)).setText(R.string.result_courses_empty);
            view.findViewById(R.id.textCourseArea).setVisibility(View.GONE);
            layout.addView(view);
            return;
        }
        for (CareerAnalysis.Curso course : courses) {
            View view = inflater.inflate(R.layout.item_result_course, layout, false);
            String platform = blank(course.plataforma) ? "Worky" : course.plataforma.trim();
            ((TextView) view.findViewById(R.id.textCourseInitials)).setText(initials(platform));
            ((TextView) view.findViewById(R.id.textCoursePlatform)).setText(platform);
            ((TextView) view.findViewById(R.id.textCourseName)).setText(blank(course.nome) ? getString(R.string.result_analyzing) : course.nome.trim());
            TextView area = view.findViewById(R.id.textCourseArea);
            if (blank(course.area)) {
                area.setVisibility(View.GONE);
            } else {
                area.setText(course.area.trim());
            }
            view.setOnClickListener(v -> openExternal(course.url));
            layout.addView(view);
        }
    }

    private void bindDemand() {
        LinearLayout layout = findViewById(R.id.layoutDemand);
        layout.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (JobItem job : jobs) {
            String key = demandLabel(job);
            Integer current = counts.get(key);
            counts.put(key, current == null ? 1 : current + 1);
        }
        List<Map.Entry<String, Integer>> rows = new ArrayList<>(counts.entrySet());
        Collections.sort(rows, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int total = Math.max(jobs.size(), 1);
        int shown = 0;
        for (Map.Entry<String, Integer> row : rows) {
            if (shown >= 3) break;
            View view = inflater.inflate(R.layout.item_result_demand, layout, false);
            int pct = Math.round((row.getValue() * 100f) / total);
            ((TextView) view.findViewById(R.id.textDemandLabel)).setText(row.getKey());
            ((TextView) view.findViewById(R.id.textDemandPct)).setText(pct + "%");
            ((ProgressBar) view.findViewById(R.id.progressDemand)).setProgress(pct);
            layout.addView(view);
            shown++;
        }
        if (shown == 0) {
            View view = inflater.inflate(R.layout.item_result_demand, layout, false);
            ((TextView) view.findViewById(R.id.textDemandLabel)).setText(R.string.result_analyzing);
            ((TextView) view.findViewById(R.id.textDemandPct)).setText("0%");
            ((ProgressBar) view.findViewById(R.id.progressDemand)).setProgress(0);
            layout.addView(view);
        }
    }

    private void renderJobs() {
        layoutJobs.removeAllViews();
        if (jobs.isEmpty()) {
            textJobsEmpty.setVisibility(View.VISIBLE);
            btnMoreJobs.setVisibility(View.GONE);
            return;
        }
        textJobsEmpty.setVisibility(View.GONE);
        int limit = showAllJobs ? jobs.size() : Math.min(JOBS_PREVIEW_LIMIT, jobs.size());
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < limit; i++) {
            JobItem job = jobs.get(i);
            View view = inflater.inflate(R.layout.item_result_job, layoutJobs, false);
            TextView initials = view.findViewById(R.id.textJobInitials);
            String seed = blank(job.empresa) ? job.displayTitle() : job.empresa.trim();
            initials.setText(initials(seed));
            if (initials.getBackground() != null) {
                initials.getBackground().mutate().setTint(colorFromText(seed));
            }
            ((TextView) view.findViewById(R.id.textJobTitle)).setText(job.displayTitle());
            TextView company = view.findViewById(R.id.textJobCompany);
            if (blank(job.empresa)) {
                company.setVisibility(View.GONE);
            } else {
                company.setText(job.empresa.trim());
            }
            TextView badge = view.findViewById(R.id.textJobBadge);
            if (job.isWorkyJob()) {
                badge.setVisibility(View.VISIBLE);
                badge.setText("Worky");
            } else if (!blank(job.tag) || !blank(job.fonte)) {
                badge.setVisibility(View.VISIBLE);
                badge.setText(!blank(job.tag) ? job.tag.trim() : job.fonte.trim());
            } else {
                badge.setVisibility(View.GONE);
            }
            String location = job.displayLocal();
            if ("-".equals(location) && !blank(job.modalidade)) location = job.modalidade.trim();
            if ("-".equals(location)) location = getString(R.string.result_job_location_fallback);
            ((TextView) view.findViewById(R.id.textJobLocal)).setText(location);
            String salary = blank(job.salario) ? getString(R.string.result_job_salary_unknown) : job.salario.trim();
            String contract = !blank(job.tipoContrato)
                    ? job.tipoContrato.trim()
                    : (!blank(job.modalidade) ? job.modalidade.trim() : getString(R.string.result_job_contract_unknown));
            ((TextView) view.findViewById(R.id.textJobMeta)).setText(salary + " · " + contract);
            MaterialButton open = view.findViewById(R.id.btnJobOpen);
            open.setText(job.isWorkyJob() ? R.string.result_job_apply : R.string.result_job_open);
            open.setOnClickListener(v -> openJob(job));
            view.setOnClickListener(v -> openJob(job));
            layoutJobs.addView(view);
        }
        int hidden = jobs.size() - JOBS_PREVIEW_LIMIT;
        if (hidden > 0) {
            btnMoreJobs.setVisibility(View.VISIBLE);
            btnMoreJobs.setText(showAllJobs
                    ? getString(R.string.result_jobs_hide)
                    : getString(R.string.result_jobs_more_count, hidden));
        } else {
            btnMoreJobs.setVisibility(View.GONE);
        }
    }

    private void openMatch() {
        Intent intent = new Intent(this, MatchActivity.class);
        intent.putExtra(MatchActivity.EXTRA_CARGO, safe(analysis.carreira));
        intent.putExtra(MatchActivity.EXTRA_SKILLS, joinCsv(analysis.techSkills()));
        startActivity(intent);
    }

    private void openJob(JobItem job) {
        String link = job.link == null ? "" : job.link.trim();
        boolean internal = job.isWorkyJob() || link.startsWith("/");
        if (internal) {
            String jobId = job.id == null ? "" : String.valueOf(job.id);
            if (blank(jobId) && link.startsWith("/vagas/")) {
                jobId = link.replace("/vagas/", "").split("\\?")[0];
            }
            String path = !blank(jobId) ? "/vagas/" + jobId : (link.startsWith("/") ? link : "/");
            Intent intent = new Intent(this, FullSiteActivity.class);
            intent.putExtra(FullSiteActivity.EXTRA_URL, ApiConfig.WEB_URL + path);
            startActivity(intent);
            return;
        }
        openExternal(link);
    }

    private void openExternal(String url) {
        if (blank(url)) {
            Toast.makeText(this, R.string.result_link_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        String target = url.trim();
        if (target.startsWith("/")) {
            target = ApiConfig.WEB_URL + target;
        }
        Uri uri = Uri.parse(target);
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            Toast.makeText(this, R.string.result_link_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    private String extra(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value.trim();
    }

    private static List<JobItem> filterAndSort(List<JobItem> source, String local, String modelo) {
        List<JobItem> filtered = new ArrayList<>();
        String localLower = local == null ? "" : local.toLowerCase(Locale.ROOT).trim();
        String modeloLower = modelo == null ? "" : modelo.toLowerCase(Locale.ROOT).trim();
        for (JobItem job : source) {
            if (job == null) continue;
            String jobLocal = job.displayLocal().toLowerCase(Locale.ROOT);
            String jobModelo = job.modalidade == null ? "" : job.modalidade.toLowerCase(Locale.ROOT);
            if (!localLower.isEmpty() && !"brasil".equals(localLower)) {
                if (!jobLocal.contains(localLower)) continue;
            }
            if (!modeloLower.isEmpty() && !"qualquer".equals(modeloLower)) {
                if (modeloLower.contains("híbrid") || modeloLower.contains("hibrid")) {
                    if (!jobModelo.contains("híbrid") && !jobModelo.contains("hibrid")) continue;
                } else if (modeloLower.contains("remoto")) {
                    if (!jobModelo.contains("remoto")) continue;
                } else if (modeloLower.contains("presencial")) {
                    if (!jobModelo.contains("presencial")) continue;
                }
            }
            filtered.add(job);
        }
        Collections.sort(filtered, (a, b) -> Boolean.compare(b.isWorkyJob(), a.isWorkyJob()));
        return filtered;
    }

    private static String demandLabel(JobItem job) {
        String raw = !blank(job.localidade) ? job.localidade.trim()
                : (!blank(job.modalidade) ? job.modalidade.trim() : "Não informado");
        int slash = raw.indexOf('/');
        if (slash > 0) raw = raw.substring(0, slash).trim();
        return raw.isEmpty() ? "Não informado" : raw;
    }

    private String fonteLabel(String fonte) {
        if ("scrape".equals(fonte)) return getString(R.string.fonte_worky);
        if ("all".equals(fonte)) return getString(R.string.fonte_ambos);
        if ("google".equals(fonte)) return getString(R.string.fonte_google_jobs);
        return "";
    }

    private static String initials(String text) {
        if (blank(text)) return "WY";
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length && sb.length() < 2; i++) {
            if (!words[i].isEmpty()) sb.append(Character.toUpperCase(words[i].charAt(0)));
        }
        return sb.length() == 0 ? "WY" : sb.toString();
    }

    private static int colorFromText(String text) {
        int total = 0;
        String seed = blank(text) ? "WY" : text;
        for (int i = 0; i < seed.length(); i++) total += seed.charAt(i);
        return LOGO_COLORS[Math.abs(total) % LOGO_COLORS.length];
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return blank(value) ? "-" : value.trim();
    }

    private static String joinCsv(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
