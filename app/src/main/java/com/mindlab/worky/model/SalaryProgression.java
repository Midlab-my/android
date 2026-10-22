package com.mindlab.worky.model;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SalaryProgression {

    public static final class Band {
        public final String label;
        public final String range;
        public final boolean active;

        public Band(String label, String range, boolean active) {
            this.label = label;
            this.range = range;
            this.active = active;
        }
    }

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[.,]\\d+)?)(?:\\s*(mil|k))?", Pattern.CASE_INSENSITIVE);

    private SalaryProgression() {}

    public static List<Band> from(String mediaSalarial) {
        List<Double> values = parseSalaryValues(mediaSalarial);
        List<Band> bands = new ArrayList<>();
        if (values.isEmpty()) {
            bands.add(new Band("Júnior", "Sob consulta", false));
            bands.add(new Band("Pleno", "Sob consulta", true));
            bands.add(new Band("Sênior", "Sob consulta", false));
            return bands;
        }
        if (values.size() == 1) {
            double base = values.get(0);
            bands.add(new Band("Júnior", formatRange(base * 0.6, base * 0.82), false));
            bands.add(new Band("Pleno", formatRange(base * 0.9, base * 1.2), true));
            bands.add(new Band("Sênior", formatRange(base * 1.28, base * 1.75), false));
            return bands;
        }
        double rawMin = values.get(0);
        double rawMax = values.get(1);
        double min = Math.min(rawMin, rawMax);
        double max = Math.max(rawMin, rawMax);
        double width = Math.max(max - min, max * 0.25);
        bands.add(new Band("Júnior", formatRange(min - width * 0.35, min + width * 0.2), false));
        bands.add(new Band("Pleno", formatRange(min + width * 0.3, max), true));
        bands.add(new Band("Sênior", formatRange(max + width * 0.18, max + width * 0.9), false));
        return bands;
    }

    static List<Double> parseSalaryValues(String text) {
        List<Double> values = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return values;
        boolean annual = Pattern.compile("\\b(ano|anual|a\\.a\\.?)\\b", Pattern.CASE_INSENSITIVE).matcher(text).find();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            String raw = matcher.group(1);
            String unit = matcher.group(2);
            if (raw == null) continue;
            String normalized = raw.replace(".", "").replace(",", ".");
            try {
                double value = Double.parseDouble(normalized);
                if (unit != null || (value > 0 && value < 100)) {
                    value *= 1000;
                }
                if (annual && value >= 12000) {
                    value /= 12;
                }
                values.add(value);
            } catch (NumberFormatException ignored) {
                // texto de mercado fora do padrao numerico
            }
        }
        return values;
    }

    static String formatRange(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max)) return "Consultar";
        long lower = Math.max(0, Math.round(Math.min(min, max) / 100.0) * 100);
        long upper = Math.max(lower, Math.round(Math.max(min, max) / 100.0) * 100);
        if (lower == upper) return formatBrl(lower);
        return formatBrl(lower) + " a " + formatBrl(upper);
    }

    private static String formatBrl(long value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        format.setMaximumFractionDigits(0);
        return format.format(value);
    }
}
