package com.mindlab.worky.network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApiExceptionTest {

    @Test
    public void parseQuotaDetail() {
        ApiException err = ApiException.from(429,
                "{\"detail\":{\"code\":\"search_quota\",\"message\":\"Voce usou as 5 buscas novas deste mes.\"}}");
        assertTrue(err.isQuota());
        assertEquals("search_quota", err.code);
        assertEquals("Voce usou as 5 buscas novas deste mes.", err.getMessage());
    }

    @Test
    public void parsePrivacyString() {
        ApiException err = ApiException.from(422,
                "{\"detail\":\"Use um cargo ou área profissional, sem dados pessoais.\"}");
        assertTrue(err.isInvalid());
        assertEquals("Use um cargo ou área profissional, sem dados pessoais.", err.getMessage());
    }

    @Test
    public void parseEmptyBodyUsesFallback() {
        ApiException err = ApiException.from(401, "");
        assertTrue(err.isLogin());
        assertEquals("Para analisar um cargo novo, entre na sua conta Worky.", err.getMessage());
    }
}
