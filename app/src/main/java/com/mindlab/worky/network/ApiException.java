package com.mindlab.worky.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

/** Erro HTTP do backend Worky, com recado ja pronto pra UI. */
public class ApiException extends IOException {

    public final int httpStatus;
    public final String code;

    public ApiException(int httpStatus, @Nullable String code, @NonNull String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code != null ? code : "";
    }

    public boolean isQuota() {
        return httpStatus == 429 && "search_quota".equals(code);
    }

    public boolean isRateLimit() {
        return httpStatus == 429 && !isQuota();
    }

    public boolean isInvalid() {
        return httpStatus == 422;
    }

    public boolean isLogin() {
        return httpStatus == 401;
    }

    @NonNull
    public static ApiException from(int httpStatus, @Nullable String raw) {
        ParsedDetail parsed = parseDetail(raw);
        String message = parsed.message;
        if (message == null || message.trim().isEmpty()) {
            message = fallbackMessage(httpStatus, parsed.code);
        }
        return new ApiException(httpStatus, parsed.code, message.trim());
    }

    @NonNull
    static ParsedDetail parseDetail(@Nullable String raw) {
        ParsedDetail parsed = new ParsedDetail();
        if (raw == null || raw.trim().isEmpty()) return parsed;
        try {
            JsonElement rootEl = JsonParser.parseString(raw);
            if (rootEl == null || !rootEl.isJsonObject()) return parsed;
            JsonObject root = rootEl.getAsJsonObject();
            if (!root.has("detail")) return parsed;
            JsonElement detail = root.get("detail");
            if (detail == null || detail.isJsonNull()) return parsed;
            if (detail.isJsonPrimitive()) {
                parsed.message = detail.getAsString();
                return parsed;
            }
            if (detail.isJsonObject()) {
                JsonObject obj = detail.getAsJsonObject();
                if (obj.has("code") && obj.get("code").isJsonPrimitive()) {
                    parsed.code = obj.get("code").getAsString();
                }
                if (obj.has("message") && obj.get("message").isJsonPrimitive()) {
                    parsed.message = obj.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            // corpo nao JSON: UI usa o fallback do status
        }
        return parsed;
    }

    @NonNull
    private static String fallbackMessage(int httpStatus, @Nullable String code) {
        if (httpStatus == 401) {
            return "Para analisar um cargo novo, entre na sua conta Worky.";
        }
        if (httpStatus == 429 && "search_quota".equals(code)) {
            return "Voce usou as buscas novas deste mes. O limite renova no proximo mes.";
        }
        if (httpStatus == 429) {
            return "Limite de buscas por enquanto. Aguarde alguns minutos e tente de novo.";
        }
        if (httpStatus == 422) {
            return "Use um cargo ou area profissional, sem dados pessoais.";
        }
        return "Tente de novo daqui a pouco.";
    }

    static final class ParsedDetail {
        String code = "";
        String message = "";
    }
}
