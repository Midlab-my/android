package com.mindlab.worky;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.mindlab.worky.auth.AuthSession;
import com.mindlab.worky.auth.SessionManager;
import com.mindlab.worky.auth.SessionStore;
import com.mindlab.worky.data.CareerCacheEntity;
import com.mindlab.worky.data.CareerRepository;
import com.mindlab.worky.model.CareerAnalysis;
import com.mindlab.worky.model.SearchQuota;
import com.mindlab.worky.network.ApiException;
import com.mindlab.worky.ui.LoadingTicker;
import com.mindlab.worky.network.ApiConfig;
import com.mindlab.worky.ui.SearchCatalog;
import com.mindlab.worky.ui.WorkyInsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText inputCargo;
    private View layoutLoading;
    private View layoutSearchBar;
    private NestedScrollView scrollHome;
    private View layoutSearchDropdown;
    private View layoutFiltersPanel;
    private View layoutFontePanel;
    private View layoutDropdownRecents;
    private ProgressBar progress;
    private TextView textLoadingStep;
    private TextView statusText;
    private TextView textAuthStatus;
    private TextView textSearchQuota;
    private TextView textSuggestTitle;
    private TextView textSuggestEmpty;
    private TextView textFiltersValue;
    private TextView textFonteValue;
    private ImageView iconFiltersChevron;
    private ImageView iconFonteChevron;
    private View layoutAccountActions;
    private View cardCompanies;
    private MaterialButton btnProfile;
    private MaterialButton btnCompany;
    private MaterialButton btnNavEmpresas;
    private View layoutRecents;
    private ChipGroup chipRecents;
    private ChipGroup chipDropdownSuggest;
    private ChipGroup chipDropdownRecents;
    private MaterialButton btnAuth;
    private MaterialButton btnFonteWorky;
    private MaterialButton btnFonteGoogle;
    private MaterialButton btnFonteAll;
    private MaterialButton btnPickCountry;
    private MaterialButton btnPickRegion;
    private MaterialButton btnPickModelo;
    private SessionStore sessionStore;
    private SessionManager sessionManager;
    private LoadingTicker loadingTicker;
    private String selectedCountry = "";
    private String selectedRegion = "";
    private String selectedModelo = "";
    private String selectedFonte = ApiConfig.DEFAULT_FONTE;
    private boolean ignoreScrollClose;
    private List<CareerCacheEntity> recentItems = new ArrayList<>();

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
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutSearchBar = findViewById(R.id.layoutSearchBar);
        scrollHome = findViewById(R.id.scrollHome);
        layoutSearchDropdown = findViewById(R.id.layoutSearchDropdown);
        layoutFiltersPanel = findViewById(R.id.layoutFiltersPanel);
        layoutFontePanel = findViewById(R.id.layoutFontePanel);
        layoutDropdownRecents = findViewById(R.id.layoutDropdownRecents);
        progress = findViewById(R.id.progress);
        textLoadingStep = findViewById(R.id.textLoadingStep);
        statusText = findViewById(R.id.statusText);
        textAuthStatus = findViewById(R.id.textAuthStatus);
        textSearchQuota = findViewById(R.id.textSearchQuota);
        textSuggestTitle = findViewById(R.id.textSuggestTitle);
        textSuggestEmpty = findViewById(R.id.textSuggestEmpty);
        textFiltersValue = findViewById(R.id.textFiltersValue);
        textFonteValue = findViewById(R.id.textFonteValue);
        iconFiltersChevron = findViewById(R.id.iconFiltersChevron);
        iconFonteChevron = findViewById(R.id.iconFonteChevron);
        layoutAccountActions = findViewById(R.id.layoutAccountActions);
        cardCompanies = findViewById(R.id.cardCompanies);
        btnProfile = findViewById(R.id.btnProfile);
        btnCompany = findViewById(R.id.btnCompany);
        btnNavEmpresas = findViewById(R.id.btnNavEmpresas);
        layoutRecents = findViewById(R.id.layoutRecents);
        chipRecents = findViewById(R.id.chipRecents);
        chipDropdownSuggest = findViewById(R.id.chipDropdownSuggest);
        chipDropdownRecents = findViewById(R.id.chipDropdownRecents);
        btnAuth = findViewById(R.id.btnAuth);
        btnFonteWorky = findViewById(R.id.btnFonteWorky);
        btnFonteGoogle = findViewById(R.id.btnFonteGoogle);
        btnFonteAll = findViewById(R.id.btnFonteAll);
        btnPickCountry = findViewById(R.id.btnPickCountry);
        btnPickRegion = findViewById(R.id.btnPickRegion);
        btnPickModelo = findViewById(R.id.btnPickModelo);
        loadingTicker = LoadingTicker.forCareer();

        findViewById(R.id.btnAnalisar).setOnClickListener(v -> runAnalysis(false));
        btnProfile.setOnClickListener(v -> openProfile());
        btnCompany.setOnClickListener(v -> openCompany());
        findViewById(R.id.btnPlans).setOnClickListener(v -> startActivity(new Intent(this, PlansActivity.class)));
        btnNavEmpresas.setOnClickListener(v -> openCompaniesCatalog());
        btnAuth.setOnClickListener(v -> onAuthClick());
        findViewById(R.id.btnCtaStart).setOnClickListener(v -> openSearchFromCta());
        findViewById(R.id.cardSalary).setOnClickListener(v -> openSearchFromCta());
        findViewById(R.id.cardSkills).setOnClickListener(v -> openSearchFromCta());
        findViewById(R.id.cardJobsLive).setOnClickListener(v -> openSearchFromCta());
        findViewById(R.id.cardProfile).setOnClickListener(v -> openProfile());
        findViewById(R.id.cardCompanies).setOnClickListener(v -> openCompaniesCatalog());
        findViewById(R.id.btnToggleFilters).setOnClickListener(v -> toggleFilters());
        findViewById(R.id.btnToggleFonte).setOnClickListener(v -> toggleFonte());
        btnFonteWorky.setOnClickListener(v -> setFonte("scrape"));
        btnFonteGoogle.setOnClickListener(v -> setFonte("google"));
        btnFonteAll.setOnClickListener(v -> setFonte("all"));

        bindFilterPickers();
        bindSearchInput();
        setFonte(ApiConfig.DEFAULT_FONTE);
        updateFilterLabels();
        View.OnLayoutChangeListener relayout = (v, l, t, r, b, ol, ot, or, ob) -> {
            if (layoutSearchDropdown.getVisibility() == View.VISIBLE) pinDropdown();
        };
        if (layoutSearchBar != null) layoutSearchBar.addOnLayoutChangeListener(relayout);
        View root = findViewById(R.id.main);
        if (root != null) root.addOnLayoutChangeListener(relayout);
        if (scrollHome != null) {
            scrollHome.setOnScrollChangeListener((NestedScrollView v, int x, int y, int ox, int oy) -> {
                if (layoutSearchDropdown.getVisibility() != View.VISIBLE) return;
                if (ignoreScrollClose) {
                    pinDropdown();
                    return;
                }
                if (!isSearchBarOnScreen()) {
                    hideSearchDropdown();
                    inputCargo.clearFocus();
                    hideKeyboard();
                    return;
                }
                pinDropdown();
            });
        }

        reloadRecent();
        refreshAuthUi();
        refreshSessionInBackground();
        refreshQuota();
    }

    private void bindSearchInput() {
        inputCargo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showSearchDropdown();
        });
        inputCargo.setOnClickListener(v -> showSearchDropdown());
        inputCargo.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                runAnalysis(false);
                return true;
            }
            return false;
        });
        inputCargo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (layoutSearchDropdown.getVisibility() == View.VISIBLE) {
                    renderSuggestions();
                }
            }
        });
    }

    private void bindFilterPickers() {
        refreshFilterPickerLabels();
        btnPickCountry.setOnClickListener(v -> pickFilter(
                getString(R.string.search_country),
                countryOptions(),
                selectedCountry.isEmpty() ? getString(R.string.search_qualquer_pais) : selectedCountry,
                chosen -> {
                    selectedCountry = getString(R.string.search_qualquer_pais).equals(chosen) ? "" : chosen;
                    selectedRegion = "";
                    refreshFilterPickerLabels();
                    updateFilterLabels();
                }
        ));
        btnPickRegion.setOnClickListener(v -> {
            if (selectedCountry.isEmpty()) return;
            pickFilter(
                    getString(R.string.search_region),
                    regionOptions(),
                    selectedRegion.isEmpty() ? getString(R.string.search_qualquer_estado) : selectedRegion,
                    chosen -> {
                        selectedRegion = getString(R.string.search_qualquer_estado).equals(chosen) ? "" : chosen;
                        refreshFilterPickerLabels();
                        updateFilterLabels();
                    }
            );
        });
        btnPickModelo.setOnClickListener(v -> pickFilter(
                getString(R.string.search_modelo),
                Arrays.asList(SearchCatalog.MODELOS),
                selectedModelo.isEmpty() ? SearchCatalog.MODELOS[0] : selectedModelo,
                chosen -> {
                    selectedModelo = SearchCatalog.MODELOS[0].equalsIgnoreCase(chosen) ? "" : chosen;
                    refreshFilterPickerLabels();
                    updateFilterLabels();
                }
        ));
    }

    private List<String> countryOptions() {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.search_qualquer_pais));
        items.addAll(SearchCatalog.countries());
        return items;
    }

    private List<String> regionOptions() {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.search_qualquer_estado));
        if (!selectedCountry.isEmpty()) {
            items.addAll(SearchCatalog.regionsFor(selectedCountry));
        }
        return items;
    }

    private void refreshFilterPickerLabels() {
        btnPickCountry.setText(selectedCountry.isEmpty()
                ? getString(R.string.search_qualquer_pais) : selectedCountry);
        btnPickRegion.setText(selectedRegion.isEmpty()
                ? getString(R.string.search_qualquer_estado) : selectedRegion);
        btnPickRegion.setEnabled(!selectedCountry.isEmpty());
        btnPickRegion.setAlpha(selectedCountry.isEmpty() ? 0.45f : 1f);
        btnPickModelo.setText(selectedModelo.isEmpty() ? SearchCatalog.MODELOS[0] : selectedModelo);
    }

    private void pickFilter(String title, List<String> items, String current, java.util.function.Consumer<String> onPick) {
        hideKeyboard();
        int checked = Math.max(0, items.indexOf(current));
        CharSequence[] labels = items.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    onPick.accept(items.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openSearchFromCta() {
        if (scrollHome == null || scrollHome.getScrollY() <= 8) {
            showSearchDropdown();
            return;
        }
        ignoreScrollClose = true;
        scrollHome.smoothScrollTo(0, 0);
        scrollHome.postDelayed(() -> {
            showSearchDropdown();
            scrollHome.postDelayed(() -> ignoreScrollClose = false, 240);
        }, 320);
    }

    private void showSearchDropdown() {
        pinDropdown();
        if (layoutSearchDropdown.getVisibility() != View.VISIBLE) {
            layoutSearchDropdown.animate().cancel();
            layoutSearchDropdown.setAlpha(0f);
            layoutSearchDropdown.setVisibility(View.VISIBLE);
            layoutSearchDropdown.animate()
                    .alpha(1f)
                    .setDuration(160)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        inputCargo.requestFocus();
        renderSuggestions();
        renderDropdownRecents();
        updateFilterLabels();
        layoutSearchDropdown.post(this::pinDropdown);
    }

    private void hideSearchDropdown() {
        if (layoutSearchDropdown == null || layoutSearchDropdown.getVisibility() != View.VISIBLE) {
            collapseAccordion();
            return;
        }
        layoutSearchDropdown.animate().cancel();
        collapseAccordion();
        layoutSearchDropdown.animate()
                .alpha(0f)
                .setDuration(120)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(this::hideSearchDropdownNow)
                .start();
    }

    private void hideSearchDropdownNow() {
        if (layoutSearchDropdown == null) return;
        layoutSearchDropdown.animate().cancel();
        collapseAccordion();
        layoutSearchDropdown.setVisibility(View.GONE);
        layoutSearchDropdown.setAlpha(1f);
    }

    private void collapseAccordion() {
        if (layoutFiltersPanel != null) layoutFiltersPanel.setVisibility(View.GONE);
        if (layoutFontePanel != null) layoutFontePanel.setVisibility(View.GONE);
        updateAccordionIcons();
    }

    private void pinDropdown() {
        if (layoutSearchBar == null || layoutSearchDropdown == null) return;
        View root = findViewById(R.id.main);
        if (root == null || layoutSearchBar.getWidth() == 0) return;
        int[] barLoc = new int[2];
        int[] rootLoc = new int[2];
        layoutSearchBar.getLocationOnScreen(barLoc);
        root.getLocationOnScreen(rootLoc);
        float density = getResources().getDisplayMetrics().density;
        int gap = Math.round(6 * density);
        int left = barLoc[0] - rootLoc[0] - root.getPaddingLeft();
        int top = barLoc[1] + layoutSearchBar.getHeight() - rootLoc[1] - root.getPaddingTop() + gap;
        int width = layoutSearchBar.getWidth();
        int computedMax = root.getHeight() - root.getPaddingTop() - root.getPaddingBottom() - top - Math.round(12 * density);
        int maxH = Math.max(120, computedMax);
        int contentH = measureDropdownContent(width);
        int height = contentH > 0 ? Math.min(contentH, maxH) : ViewGroup.LayoutParams.WRAP_CONTENT;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) layoutSearchDropdown.getLayoutParams();
        if (lp.width == width && lp.leftMargin == left && lp.topMargin == top && lp.height == height) {
            return;
        }
        lp.width = width;
        lp.leftMargin = left;
        lp.topMargin = top;
        lp.height = height;
        layoutSearchDropdown.setLayoutParams(lp);
        layoutSearchDropdown.scrollTo(0, 0);
    }

    private int measureDropdownContent(int width) {
        if (!(layoutSearchDropdown instanceof ViewGroup)) return 0;
        View inner = ((ViewGroup) layoutSearchDropdown).getChildAt(0);
        if (inner == null || width <= 0) return 0;
        inner.forceLayout();
        int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        inner.measure(wSpec, hSpec);
        return inner.getMeasuredHeight();
    }

    private boolean isSearchBarOnScreen() {
        if (layoutSearchBar == null) return false;
        View root = findViewById(R.id.main);
        if (root == null) return false;
        int[] barLoc = new int[2];
        int[] rootLoc = new int[2];
        layoutSearchBar.getLocationOnScreen(barLoc);
        root.getLocationOnScreen(rootLoc);
        int barBottom = barLoc[1] + layoutSearchBar.getHeight();
        int visibleTop = rootLoc[1] + root.getPaddingTop();
        int visibleBottom = rootLoc[1] + root.getHeight() - root.getPaddingBottom();
        return barBottom > visibleTop + 8 && barLoc[1] < visibleBottom - 8;
    }

    private void toggleFilters() {
        boolean open = layoutFiltersPanel.getVisibility() != View.VISIBLE;
        layoutFiltersPanel.setVisibility(open ? View.VISIBLE : View.GONE);
        if (open) layoutFontePanel.setVisibility(View.GONE);
        updateAccordionIcons();
        relayoutDropdown();
    }

    private void toggleFonte() {
        boolean open = layoutFontePanel.getVisibility() != View.VISIBLE;
        layoutFontePanel.setVisibility(open ? View.VISIBLE : View.GONE);
        if (open) layoutFiltersPanel.setVisibility(View.GONE);
        updateAccordionIcons();
        relayoutDropdown();
    }

    private void relayoutDropdown() {
        if (layoutSearchDropdown == null || layoutSearchDropdown.getVisibility() != View.VISIBLE) {
            return;
        }
        layoutSearchDropdown.scrollTo(0, 0);
        pinDropdown();
        layoutSearchDropdown.post(this::pinDropdown);
    }

    private void updateAccordionIcons() {
        boolean filtersOpen = layoutFiltersPanel.getVisibility() == View.VISIBLE;
        boolean fonteOpen = layoutFontePanel.getVisibility() == View.VISIBLE;
        iconFiltersChevron.setRotation(filtersOpen ? 180f : 0f);
        iconFonteChevron.setRotation(fonteOpen ? 180f : 0f);
    }

    private void setFonte(String fonte) {
        selectedFonte = fonte;
        styleFonteButton(btnFonteWorky, "scrape".equals(fonte));
        styleFonteButton(btnFonteGoogle, "google".equals(fonte));
        styleFonteButton(btnFonteAll, "all".equals(fonte));
        updateFilterLabels();
    }

    private void styleFonteButton(MaterialButton button, boolean active) {
        if (active) {
            button.setBackgroundTintList(ColorStateList.valueOf(0xFFEFF6FF));
            button.setStrokeColor(ColorStateList.valueOf(0xFF2563EB));
            button.setTextColor(0xFF1D4ED8);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            button.setStrokeColor(ColorStateList.valueOf(0xFFDBE5F4));
            button.setTextColor(0xFF475569);
        }
    }

    private void updateFilterLabels() {
        String local = !selectedRegion.isEmpty() ? selectedRegion
                : !selectedCountry.isEmpty() ? selectedCountry
                : "Qualquer local";
        String modelo = selectedModelo.isEmpty() ? "Qualquer modelo" : selectedModelo;
        if (textFiltersValue != null) textFiltersValue.setText(local + " · " + modelo);
        if (textFonteValue != null) {
            textFonteValue.setText(
                    "scrape".equals(selectedFonte) ? getString(R.string.fonte_worky)
                            : "all".equals(selectedFonte) ? getString(R.string.fonte_ambos)
                            : getString(R.string.fonte_google_jobs)
            );
        }
    }

    private String selectedLocal() {
        if (!selectedRegion.isEmpty()) return selectedRegion;
        return selectedCountry;
    }

    private void renderSuggestions() {
        String query = textOf(inputCargo);
        boolean typing = SearchCatalog.fold(query).length() >= 2;
        List<String> items = SearchCatalog.suggest(query);
        textSuggestTitle.setText(typing ? R.string.search_suggestions : R.string.search_popular);
        chipDropdownSuggest.removeAllViews();
        if (typing && items.isEmpty()) {
            textSuggestEmpty.setVisibility(View.VISIBLE);
            return;
        }
        textSuggestEmpty.setVisibility(View.GONE);
        for (String item : items) {
            chipDropdownSuggest.addView(makeChip(item, v -> {
                inputCargo.setText(item);
                inputCargo.setSelection(item.length());
                runAnalysis(false);
            }));
        }
    }

    private void renderDropdownRecents() {
        chipDropdownRecents.removeAllViews();
        if (recentItems.isEmpty()) {
            layoutDropdownRecents.setVisibility(View.GONE);
            return;
        }
        layoutDropdownRecents.setVisibility(View.VISIBLE);
        for (CareerCacheEntity entity : recentItems) {
            chipDropdownRecents.addView(makeChip(entity.cargoLabel, v -> {
                hideSearchDropdownNow();
                inputCargo.clearFocus();
                hideKeyboard();
                inputCargo.setText(entity.cargoLabel);
                openResult(gson.fromJson(entity.jsonPayload, CareerAnalysis.class), true);
            }));
        }
    }

    private Chip makeChip(String label, View.OnClickListener listener) {
        float density = getResources().getDisplayMetrics().density;
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipMinHeight(28f * density);
        chip.setChipStartPadding(6f * density);
        chip.setChipEndPadding(6f * density);
        chip.setTextStartPadding(4f * density);
        chip.setTextEndPadding(4f * density);
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.WHITE));
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColor(ColorStateList.valueOf(0xFFDBE5F4));
        chip.setChipCornerRadius(8f * density);
        chip.setTextColor(0xFF2563EB);
        chip.setTextSize(12f);
        chip.setOnClickListener(listener);
        return chip;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN
                && layoutSearchDropdown != null
                && layoutSearchDropdown.getVisibility() == View.VISIBLE
                && !isTouchInside(layoutSearchBar, ev)
                && !isTouchInside(layoutSearchDropdown, ev)) {
            hideSearchDropdown();
            inputCargo.clearFocus();
            hideKeyboard();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private static boolean isTouchInside(View view, MotionEvent ev) {
        if (view == null) return false;
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        float x = ev.getRawX();
        float y = ev.getRawY();
        return x >= loc[0] && x <= loc[0] + view.getWidth()
                && y >= loc[1] && y <= loc[1] + view.getHeight();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null || inputCargo == null) return;
        imm.hideSoftInputFromWindow(inputCargo.getWindowToken(), 0);
    }

    @Override
    protected void onPause() {
        hideSearchDropdownNow();
        if (inputCargo != null) inputCargo.clearFocus();
        hideKeyboard();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadRecent();
        refreshAuthUi();
        refreshQuota();
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
        AuthSession session = sessionStore.get();
        if (session != null && !com.mindlab.worky.auth.JwtAal.hasAal2(session.accessToken)) {
            startActivity(new Intent(this, MfaActivity.class));
            return;
        }
        if (sessionStore.isLoggedIn()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.logout_confirm_title)
                    .setMessage(R.string.logout_confirm_message)
                    .setPositiveButton(R.string.btn_logout, (d, w) -> {
                        sessionManager.clear();
                        refreshAuthUi();
                        Toast.makeText(this, R.string.logout_ok, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void refreshAuthUi() {
        AuthSession session = sessionStore.get();
        boolean aal2 = session != null && com.mindlab.worky.auth.JwtAal.hasAal2(session.accessToken);
        boolean company = aal2 && session.isCompany();
        if (session != null && aal2) {
            textAuthStatus.setText(getString(R.string.auth_logged, session.displayName()));
            btnAuth.setText(R.string.btn_logout);
            layoutAccountActions.setVisibility(View.VISIBLE);
            btnProfile.setVisibility(company ? View.GONE : View.VISIBLE);
            btnCompany.setVisibility(company ? View.VISIBLE : View.GONE);
        } else if (session != null) {
            textAuthStatus.setText(getString(R.string.auth_logged, session.displayName()));
            btnAuth.setText(R.string.mfa_continue);
            layoutAccountActions.setVisibility(View.GONE);
        } else {
            textAuthStatus.setText(R.string.auth_guest);
            btnAuth.setText(R.string.btn_login);
            layoutAccountActions.setVisibility(View.GONE);
        }
        btnNavEmpresas.setVisibility(company ? View.GONE : View.VISIBLE);
        cardCompanies.setVisibility(company ? View.GONE : View.VISIBLE);
    }

    private void runAnalysis(boolean preferCache) {
        String cargo = textOf(inputCargo);
        if (cargo.isEmpty()) {
            Toast.makeText(this, R.string.empty_cargo, Toast.LENGTH_SHORT).show();
            showSearchDropdown();
            return;
        }

        hideSearchDropdown();
        setLoading(true);
        io.execute(() -> {
            try {
                CareerAnalysis analysis = repository.analyze(
                        cargo, selectedFonte, selectedLocal(), selectedModelo, preferCache
                );
                runOnUiThread(() -> {
                    setLoading(false);
                    openResult(analysis, preferCache);
                    reloadRecent();
                    refreshQuota();
                });
            } catch (Exception e) {
                runOnUiThread(() -> handleSearchError(e));
            }
        });
    }

    private void openResult(CareerAnalysis analysis, boolean fromCache) {
        Intent intent = new Intent(this, CareerResultActivity.class);
        intent.putExtra(CareerResultActivity.EXTRA_JSON, gson.toJson(analysis));
        intent.putExtra(CareerResultActivity.EXTRA_FROM_CACHE, fromCache);
        intent.putExtra(CareerResultActivity.EXTRA_FONTE, selectedFonte);
        intent.putExtra(CareerResultActivity.EXTRA_LOCAL, selectedLocal());
        intent.putExtra(CareerResultActivity.EXTRA_MODELO, selectedModelo);
        startActivity(intent);
    }

    private void openProfile() {
        if (!sessionStore.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        AuthSession pending = sessionStore.get();
        if (pending != null && !com.mindlab.worky.auth.JwtAal.hasAal2(pending.accessToken)) {
            startActivity(new Intent(this, MfaActivity.class));
            return;
        }
        AuthSession session = sessionStore.get();
        if (session != null && session.isCompany()) {
            startActivity(new Intent(this, CompanyActivity.class));
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
        AuthSession pending = sessionStore.get();
        if (pending != null && !com.mindlab.worky.auth.JwtAal.hasAal2(pending.accessToken)) {
            startActivity(new Intent(this, MfaActivity.class));
            return;
        }
        AuthSession session = sessionStore.get();
        if (session == null || !session.isCompany()) {
            startActivity(new Intent(this, ProfileActivity.class));
            return;
        }
        startActivity(new Intent(this, CompanyActivity.class));
    }

    private void openCompaniesCatalog() {
        AuthSession session = sessionStore.get();
        if (session != null && session.isCompany()) {
            return;
        }
        startActivity(new Intent(this, CompaniesActivity.class));
    }

    private void refreshQuota() {
        io.execute(() -> {
            SearchQuota quota = repository.quota();
            runOnUiThread(() -> {
                if (quota == null || textSearchQuota == null) {
                    if (textSearchQuota != null) textSearchQuota.setVisibility(View.GONE);
                    return;
                }
                textSearchQuota.setVisibility(View.VISIBLE);
                textSearchQuota.setText(getString(R.string.search_quota_label, quota.used, quota.limit));
            });
        });
    }

    private void handleSearchError(Exception e) {
        setLoading(false);
        String msg = e.getMessage() != null ? e.getMessage() : getString(R.string.error_generic);
        if (e instanceof ApiException) {
            ApiException api = (ApiException) e;
            if (api.isLogin()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            if (api.isQuota()) refreshQuota();
        }
        layoutLoading.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        textLoadingStep.setText(R.string.error_generic);
        statusText.setText(msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void reloadRecent() {
        io.execute(() -> {
            List<CareerCacheEntity> items = repository.recent();
            runOnUiThread(() -> {
                recentItems = items != null ? items : new ArrayList<>();
                chipRecents.removeAllViews();
                if (recentItems.isEmpty()) {
                    layoutRecents.setVisibility(View.GONE);
                } else {
                    layoutRecents.setVisibility(View.VISIBLE);
                    for (CareerCacheEntity entity : recentItems) {
                        chipRecents.addView(makeChip(entity.cargoLabel, v -> openResult(
                                gson.fromJson(entity.jsonPayload, CareerAnalysis.class),
                                true
                        )));
                    }
                }
                if (layoutSearchDropdown.getVisibility() == View.VISIBLE) {
                    renderDropdownRecents();
                    renderSuggestions();
                    layoutSearchDropdown.post(this::pinDropdown);
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        findViewById(R.id.btnAnalisar).setEnabled(!loading);
        inputCargo.setEnabled(!loading);

        if (loading) {
            hideSearchDropdown();
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

    private static String textOf(EditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        if (loadingTicker != null) loadingTicker.stop();
        super.onDestroy();
        io.shutdownNow();
    }
}
