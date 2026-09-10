package com.mindlab.worky;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/** Forca tema claro Worky (azul). Evita Material3 DayNight cair no roxo do dark. */
public class WorkyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
