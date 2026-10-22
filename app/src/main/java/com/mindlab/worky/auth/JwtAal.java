package com.mindlab.worky.auth;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okio.ByteString;

/** Le aal/exp do access_token. Sem aal2 o RLS worky_require_aal2 barra as tabelas. */
public final class JwtAal {

    private JwtAal() {}

    public static boolean hasAal2(@Nullable String accessToken) {
        JsonObject payload = payload(accessToken);
        if (payload == null) return false;
        if (!"aal2".equals(text(payload, "aal"))) return false;
        if (payload.has("exp") && payload.get("exp").isJsonPrimitive()) {
            try {
                long exp = payload.get("exp").getAsLong();
                if (exp > 0 && exp <= (System.currentTimeMillis() / 1000L)) {
                    return false;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    static JsonObject payload(@Nullable String accessToken) {
        if (accessToken == null) return null;
        String token = accessToken.trim();
        int first = token.indexOf('.');
        int second = token.indexOf('.', first + 1);
        if (first <= 0 || second <= first) return null;
        String part = token.substring(first + 1, second);
        try {
            ByteString decoded = ByteString.decodeBase64(pad(part.replace('-', '+').replace('_', '/')));
            if (decoded == null) return null;
            return JsonParser.parseString(decoded.utf8()).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String pad(String value) {
        int mod = value.length() % 4;
        if (mod == 0) return value;
        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < 4 - mod; i++) sb.append('=');
        return sb.toString();
    }

    private static String text(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) return "";
        return obj.get(key).getAsString().trim();
    }
}
