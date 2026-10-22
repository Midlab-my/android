package com.mindlab.worky.auth;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JwtAalTest {

    @Test
    public void aal2ValidToken() {
        assertTrue(JwtAal.hasAal2(jwt("{\"aal\":\"aal2\",\"exp\":9999999999}")));
    }

    @Test
    public void aal1IsNotEnough() {
        assertFalse(JwtAal.hasAal2(jwt("{\"aal\":\"aal1\",\"exp\":9999999999}")));
    }

    @Test
    public void expiredAal2IsRejected() {
        assertFalse(JwtAal.hasAal2(jwt("{\"aal\":\"aal2\",\"exp\":1}")));
    }

    @Test
    public void garbageIsRejected() {
        assertFalse(JwtAal.hasAal2(null));
        assertFalse(JwtAal.hasAal2("not-a-jwt"));
        assertFalse(JwtAal.hasAal2("a.b"));
    }

    private static String jwt(String payloadJson) {
        String header = b64("{\"alg\":\"none\"}");
        String payload = b64(payloadJson);
        return header + "." + payload + ".x";
    }

    private static String b64(String raw) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
