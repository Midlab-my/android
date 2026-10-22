package com.mindlab.worky.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MfaRules {

    private MfaRules() {}

    public static boolean isSixDigits(@Nullable String code) {
        return code != null && code.matches("^\\d{6}$");
    }

    @Nullable
    public static TotpFactor firstVerified(@Nullable List<TotpFactor> factors) {
        if (factors == null) return null;
        for (TotpFactor factor : factors) {
            if (factor != null && factor.isTotp() && factor.isVerified()) {
                return factor;
            }
        }
        return null;
    }

    @NonNull
    public static List<TotpFactor> verified(@Nullable List<TotpFactor> factors) {
        List<TotpFactor> out = new ArrayList<>();
        if (factors == null) return out;
        for (TotpFactor factor : factors) {
            if (factor != null && factor.isTotp() && factor.isVerified()) {
                out.add(factor);
            }
        }
        return out;
    }

    @NonNull
    public static List<TotpFactor> unverified(@Nullable List<TotpFactor> factors) {
        List<TotpFactor> out = new ArrayList<>();
        if (factors == null) return out;
        for (TotpFactor factor : factors) {
            if (factor != null && factor.isTotp() && factor.isUnverified()) {
                out.add(factor);
            }
        }
        return out;
    }

    public static void assertCanEnroll(@Nullable List<TotpFactor> factors) throws IOException {
        if (firstVerified(factors) != null) {
            throw new IOException("Use o autenticador já configurado.");
        }
    }
}
