package com.mindlab.worky.auth;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MfaRulesTest {

    @Test
    public void sixDigitsOnly() {
        assertTrue(MfaRules.isSixDigits("123456"));
        assertFalse(MfaRules.isSixDigits("12345"));
        assertFalse(MfaRules.isSixDigits("1234567"));
        assertFalse(MfaRules.isSixDigits("12a456"));
        assertFalse(MfaRules.isSixDigits(""));
        assertFalse(MfaRules.isSixDigits(null));
    }

    @Test
    public void firstVerifiedIgnoresUnverified() {
        TotpFactor unverified = new TotpFactor("u1", "Worky", "totp", "unverified");
        TotpFactor verified = new TotpFactor("v1", "Worky", "totp", "verified");
        assertEquals("v1", MfaRules.firstVerified(Arrays.asList(unverified, verified)).id);
        assertNull(MfaRules.firstVerified(Collections.singletonList(unverified)));
    }

    @Test
    public void enrollBlockedWhenVerifiedExists() {
        TotpFactor verified = new TotpFactor("v1", "Worky", "totp", "verified");
        try {
            MfaRules.assertCanEnroll(Collections.singletonList(verified));
        } catch (IOException e) {
            assertEquals("Use o autenticador já configurado.", e.getMessage());
            return;
        }
        throw new AssertionError("expected enroll block");
    }

    @Test
    public void enrollAllowedWithoutVerified() throws Exception {
        TotpFactor unverified = new TotpFactor("u1", "Worky", "totp", "unverified");
        MfaRules.assertCanEnroll(Collections.singletonList(unverified));
        MfaRules.assertCanEnroll(Collections.emptyList());
    }
}
