package com.mindlab.worky.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mindlab.worky.util.BrDocs;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Lookup ViaCEP (paridade com web services/cep.ts). */
public class CepClient {

    public static final class CepResult {
        public final String cep;
        public final String city;
        public final String uf;
        public final String locationLabel;

        public CepResult(String cep, String city, String uf) {
            this.cep = cep;
            this.city = city;
            this.uf = uf;
            this.locationLabel = (city == null || city.isEmpty())
                    ? ""
                    : (uf == null || uf.isEmpty() ? city : city + ", " + uf);
        }
    }

    private final OkHttpClient client;
    private final Gson gson;

    public CepClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    @WorkerThread
    @NonNull
    public CepResult lookup(@Nullable String cep) throws IOException {
        String digits = BrDocs.onlyDigits(cep, 8);
        if (digits.length() != 8) {
            throw new IOException("CEP incompleto. Use 8 digitos.");
        }

        Request request = new Request.Builder()
                .url("https://viacep.com.br/ws/" + digits + "/json/")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = body(response);
            if (!response.isSuccessful()) {
                throw new IOException("Nao foi possivel consultar o CEP.");
            }
            JsonObject obj = gson.fromJson(raw, JsonObject.class);
            if (obj == null) {
                throw new IOException("CEP nao encontrado.");
            }
            if (obj.has("erro")) {
                try {
                    if (obj.get("erro").getAsBoolean()) {
                        throw new IOException("CEP nao encontrado.");
                    }
                } catch (Exception ignored) {
                    // ViaCEP as vezes manda string
                    if ("true".equalsIgnoreCase(obj.get("erro").toString().replace("\"", ""))) {
                        throw new IOException("CEP nao encontrado.");
                    }
                }
            }
            String city = text(obj, "localidade");
            String uf = text(obj, "uf");
            if (city.isEmpty()) {
                throw new IOException("CEP sem cidade. Confira o numero.");
            }
            return new CepResult(BrDocs.formatCep(digits), city, uf);
        }
    }

    private static String body(Response response) throws IOException {
        ResponseBody body = response.body();
        return body != null ? body.string() : "";
    }

    private static String text(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString().trim();
    }
}
