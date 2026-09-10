package com.mindlab.worky.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.mindlab.worky.R;

import java.util.List;

/**
 * Modal de busca alinhado ao Dashboard web: pais, estado, modelo, fonte e categorias.
 */
public class SearchFiltersSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SearchFiltersSheet";

    public interface Listener {
        void onSearchReady(@NonNull SearchSelection selection);
    }

    public static final class SearchSelection {
        public final String cargo;
        public final String local;
        public final String modelo;
        public final String fonte;

        public SearchSelection(String cargo, String local, String modelo, String fonte) {
            this.cargo = cargo;
            this.local = local;
            this.modelo = modelo;
            this.fonte = fonte;
        }
    }

    private Listener listener;
    private TextInputEditText inputCargo;
    private Spinner spinnerCountry;
    private Spinner spinnerRegion;
    private Spinner spinnerModelo;
    private RadioGroup radioFonte;
    private ChipGroup chipCategories;
    private LinearLayout layoutCategoryItems;
    private TextView textCategoryHint;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_search_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputCargo = view.findViewById(R.id.inputSearchCargo);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerRegion = view.findViewById(R.id.spinnerRegion);
        spinnerModelo = view.findViewById(R.id.spinnerModelo);
        radioFonte = view.findViewById(R.id.radioSearchFonte);
        chipCategories = view.findViewById(R.id.chipCategories);
        layoutCategoryItems = view.findViewById(R.id.layoutCategoryItems);
        textCategoryHint = view.findViewById(R.id.textCategoryHint);
        MaterialButton btnSearch = view.findViewById(R.id.btnSheetSearch);
        MaterialButton btnClose = view.findViewById(R.id.btnSheetClose);
        WorkyInsets.applyBottomOnly(view, 20);

        List<String> countries = SearchCatalog.countries();
        spinnerCountry.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, countries));
        spinnerModelo.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, SearchCatalog.MODELOS));
        refreshRegions(countries.isEmpty() ? "Brasil" : countries.get(0));

        spinnerCountry.setOnItemSelectedListener(new SimpleSpinnerListener(() -> {
            Object selected = spinnerCountry.getSelectedItem();
            if (selected != null) refreshRegions(selected.toString());
        }));

        Bundle args = getArguments();
        if (args != null) {
            String cargo = args.getString("cargo", "");
            if (!cargo.isEmpty()) inputCargo.setText(cargo);
            String fonte = args.getString("fonte", "google");
            if ("scrape".equals(fonte)) {
                ((RadioButton) view.findViewById(R.id.radioSheetScrape)).setChecked(true);
            } else if ("all".equals(fonte)) {
                ((RadioButton) view.findViewById(R.id.radioSheetAll)).setChecked(true);
            } else {
                ((RadioButton) view.findViewById(R.id.radioSheetGoogle)).setChecked(true);
            }
        }

        for (SearchCatalog.Category category : SearchCatalog.categories()) {
            Chip chip = new Chip(requireContext());
            chip.setText(category.label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setOnClickListener(v -> showCategoryItems(category));
            chipCategories.addView(chip);
        }
        if (!SearchCatalog.categories().isEmpty()) {
            showCategoryItems(SearchCatalog.categories().get(0));
        }

        btnClose.setOnClickListener(v -> dismiss());
        btnSearch.setOnClickListener(v -> submit());
    }

    private void refreshRegions(String country) {
        List<String> regions = SearchCatalog.regionsFor(country);
        spinnerRegion.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, regions));
    }

    private void showCategoryItems(SearchCatalog.Category category) {
        textCategoryHint.setText(category.description);
        layoutCategoryItems.removeAllViews();
        for (String item : category.items) {
            MaterialButton btn = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(item);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = 8;
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> {
                inputCargo.setText(item);
                submit();
            });
            layoutCategoryItems.addView(btn);
        }
    }

    private void submit() {
        String cargo = inputCargo.getText() != null ? inputCargo.getText().toString().trim() : "";
        if (cargo.isEmpty()) {
            inputCargo.setError(getString(R.string.empty_cargo));
            return;
        }
        String country = spinnerCountry.getSelectedItem() != null ? spinnerCountry.getSelectedItem().toString() : "";
        String region = spinnerRegion.getSelectedItem() != null ? spinnerRegion.getSelectedItem().toString() : "";
        String local = !region.isEmpty() ? region : country;
        String modeloRaw = spinnerModelo.getSelectedItem() != null ? spinnerModelo.getSelectedItem().toString() : "Qualquer";
        String modelo = "Qualquer".equalsIgnoreCase(modeloRaw) ? "" : modeloRaw;
        if ("Hibrido".equalsIgnoreCase(modelo)) modelo = "Hibrido";

        String fonte = "google";
        int checked = radioFonte.getCheckedRadioButtonId();
        if (checked == R.id.radioSheetScrape) fonte = "scrape";
        else if (checked == R.id.radioSheetAll) fonte = "all";

        if (listener != null) {
            listener.onSearchReady(new SearchSelection(cargo, local, modelo, fonte));
        }
        dismiss();
    }

    public static SearchFiltersSheet newInstance(@Nullable String cargo, @Nullable String fonte) {
        SearchFiltersSheet sheet = new SearchFiltersSheet();
        Bundle args = new Bundle();
        args.putString("cargo", cargo != null ? cargo : "");
        args.putString("fonte", fonte != null ? fonte : "google");
        sheet.setArguments(args);
        return sheet;
    }

    private interface RunnableNoArg {
        void run();
    }

    private static final class SimpleSpinnerListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final RunnableNoArg onSelect;

        SimpleSpinnerListener(RunnableNoArg onSelect) {
            this.onSelect = onSelect;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onSelect.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }
}
