package com.mindlab.worky.ui;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Espelha o contador do Dashboard web: passo + tempo decorrido + restante estimado.
 */
public final class LoadingTicker {

    public interface Listener {
        void onTick(int elapsedSeconds, int remainingSeconds, int percent, @NonNull String stepLabel);
    }

    public static final int CAREER_ESTIMATE_SEC = 60;
    public static final int JOBS_ESTIMATE_SEC = 45;
    public static final int MATCH_ESTIMATE_SEC = 40;

    private static final Step[] CAREER_STEPS = {
            new Step("Consultando fontes selecionadas…", 0),
            new Step("Priorizando vagas Worky…", 8),
            new Step("Coletando oportunidades externas…", 16),
            new Step("Conferindo salarios e demanda…", 28),
            new Step("Cruzando competencias e vagas…", 40),
            new Step("Montando o relatorio…", 52),
    };

    private static final Step[] JOBS_STEPS = {
            new Step("Consultando fontes selecionadas…", 0),
            new Step("Buscando vagas externas…", 10),
            new Step("Filtrando resultados…", 22),
            new Step("Organizando a lista…", 34),
    };

    private static final Step[] MATCH_STEPS = {
            new Step("Lendo seu perfil…", 0),
            new Step("Cruzando competencias com a carreira…", 10),
            new Step("Calculando gaps e alinhamentos…", 22),
            new Step("Montando o resultado do match…", 32),
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int estimateSec;
    private final Step[] steps;
    private long startedAt;
    private boolean running;
    @Nullable
    private Listener listener;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running || listener == null) return;
            int elapsed = (int) ((System.currentTimeMillis() - startedAt) / 1000L);
            int remaining = Math.max(0, estimateSec - elapsed);
            int percent = Math.min(96, Math.max(8, (elapsed * 100) / estimateSec));
            String step = stepFor(elapsed);
            listener.onTick(elapsed, remaining, percent, step);
            handler.postDelayed(this, 1000L);
        }
    };

    private LoadingTicker(int estimateSec, Step[] steps) {
        this.estimateSec = estimateSec;
        this.steps = steps;
    }

    public static LoadingTicker forCareer() {
        return new LoadingTicker(CAREER_ESTIMATE_SEC, CAREER_STEPS);
    }

    public static LoadingTicker forJobs() {
        return new LoadingTicker(JOBS_ESTIMATE_SEC, JOBS_STEPS);
    }

    public static LoadingTicker forMatch() {
        return new LoadingTicker(MATCH_ESTIMATE_SEC, MATCH_STEPS);
    }

    public void start(@NonNull Listener listener) {
        stop();
        this.listener = listener;
        this.startedAt = System.currentTimeMillis();
        this.running = true;
        tick.run();
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
        listener = null;
    }

    private String stepFor(int elapsed) {
        String label = steps[0].label;
        for (Step step : steps) {
            if (elapsed >= step.startsAt) {
                label = step.label;
            }
        }
        if (elapsed >= estimateSec) {
            return "Finalizando…";
        }
        return label;
    }

    public static String formatDuration(int totalSeconds) {
        int safe = Math.max(0, totalSeconds);
        int minutes = safe / 60;
        int seconds = safe % 60;
        if (minutes == 0) return seconds + "s";
        return minutes + "min " + (seconds < 10 ? "0" + seconds : String.valueOf(seconds)) + "s";
    }

    private static final class Step {
        final String label;
        final int startsAt;

        Step(String label, int startsAt) {
            this.label = label;
            this.startsAt = startsAt;
        }
    }
}
