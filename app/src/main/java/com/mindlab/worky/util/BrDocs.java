package com.mindlab.worky.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/** Digitos, mascara e validacao de CNPJ/CEP/e-mail/LinkedIn (paridade com web br-docs.ts). */
public final class BrDocs {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern ALL_SAME = Pattern.compile("^(\\d)\\1{13}$");

    private BrDocs() {
    }

    @NonNull
    public static String onlyDigits(@Nullable String value, int max) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length() && sb.length() < max; i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    @NonNull
    public static String onlyDigits(@Nullable String value) {
        return onlyDigits(value, Integer.MAX_VALUE);
    }

    @NonNull
    public static String formatCnpj(@Nullable String value) {
        String digits = onlyDigits(value, 14);
        if (digits.length() <= 2) return digits;
        if (digits.length() <= 5) {
            return digits.substring(0, 2) + "." + digits.substring(2);
        }
        if (digits.length() <= 8) {
            return digits.substring(0, 2) + "." + digits.substring(2, 5) + "." + digits.substring(5);
        }
        if (digits.length() <= 12) {
            return digits.substring(0, 2) + "." + digits.substring(2, 5) + "."
                    + digits.substring(5, 8) + "/" + digits.substring(8);
        }
        return digits.substring(0, 2) + "." + digits.substring(2, 5) + "."
                + digits.substring(5, 8) + "/" + digits.substring(8, 12) + "-" + digits.substring(12);
    }

    public static boolean isValidCnpj(@Nullable String value) {
        String digits = onlyDigits(value, 14);
        if (digits.length() != 14) return false;
        if (ALL_SAME.matcher(digits).matches()) return false;

        int first = calcCnpjCheck(digits, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        if (first != Character.getNumericValue(digits.charAt(12))) return false;
        int second = calcCnpjCheck(digits, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return second == Character.getNumericValue(digits.charAt(13));
    }

    private static int calcCnpjCheck(String base, int[] factors) {
        int sum = 0;
        for (int i = 0; i < factors.length; i++) {
            sum += Character.getNumericValue(base.charAt(i)) * factors[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @Nullable
    public static String cnpjErrorMessage(@Nullable String value) {
        String digits = onlyDigits(value);
        if (digits.isEmpty()) return "Informe o CNPJ.";
        if (digits.length() < 14) return "CNPJ incompleto. Use 14 digitos.";
        if (!isValidCnpj(digits)) return "CNPJ invalido. Confira os digitos.";
        return null;
    }

    @NonNull
    public static String formatCep(@Nullable String value) {
        String digits = onlyDigits(value, 8);
        if (digits.length() <= 5) return digits;
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }

    public static boolean isValidCepFormat(@Nullable String value) {
        return onlyDigits(value, 8).length() == 8;
    }

    @Nullable
    public static String cepErrorMessage(@Nullable String value) {
        String digits = onlyDigits(value);
        if (digits.isEmpty()) return "Informe o CEP.";
        if (digits.length() < 8) return "CEP incompleto. Use 8 digitos.";
        return null;
    }

    public static boolean isValidEmail(@Nullable String email) {
        if (email == null) return false;
        return EMAIL.matcher(email.trim()).matches();
    }

    @Nullable
    public static String emailErrorMessage(@Nullable String email) {
        String trimmed = email == null ? "" : email.trim();
        if (trimmed.isEmpty()) return "Informe seu e-mail.";
        if (!isValidEmail(trimmed)) return "Digite um e-mail valido.";
        return null;
    }

    public static boolean isValidLinkedInUrl(@Nullable String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) return true;
        try {
            String url = trimmed.startsWith("http") ? trimmed : "https://" + trimmed;
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.replaceFirst("^www\\.", "").toLowerCase(Locale.ROOT);
            return host.equals("linkedin.com") || host.endsWith(".linkedin.com");
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static String linkedInErrorMessage(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return null;
        if (!isValidLinkedInUrl(value)) {
            return "Informe uma URL valida do LinkedIn (ex: https://linkedin.com/company/...).";
        }
        return null;
    }

    /** Senha alinhada ao web: 8+ com maiuscula, numero e especial. */
    @Nullable
    public static String passwordErrorMessage(@Nullable String password) {
        String value = password == null ? "" : password;
        if (value.isEmpty()) return "Informe a senha.";
        if (value.length() < 8) return "Senha com no minimo 8 caracteres.";
        if (!value.matches(".*[A-Z].*")) return "Inclua ao menos uma letra maiuscula.";
        if (!value.matches(".*\\d.*")) return "Inclua ao menos um numero.";
        if (!value.matches(".*[^A-Za-z0-9].*")) return "Inclua ao menos um caractere especial.";
        return null;
    }

    @NonNull
    public static String translateAuthError(@Nullable String message, @NonNull String fallback) {
        String raw = message == null || message.trim().isEmpty() ? fallback : message.trim();
        String normalized = raw.toLowerCase(Locale.ROOT);

        if (normalized.contains("email not confirmed")) {
            return "E-mail ainda nao confirmado. Confirme no Supabase (Users) ou desative Confirm email em Providers.";
        }
        if (normalized.contains("invalid login credentials")) {
            return "E-mail ou senha incorretos.";
        }
        if (normalized.contains("user already registered")) {
            return "Este e-mail ja possui conta. Faca login ou use outro e-mail.";
        }
        if (normalized.contains("failed to fetch")
                || normalized.contains("networkerror")
                || normalized.contains("fetch failed")
                || normalized.contains("unable to resolve host")
                || normalized.contains("timeout")) {
            return "Falha de rede ao falar com o Supabase. Confira internet e as chaves.";
        }
        if (normalized.contains("email rate limit")) {
            return "Muitas tentativas de e-mail. Aguarde alguns minutos e tente de novo.";
        }
        return raw;
    }
}
