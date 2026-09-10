package com.mindlab.worky.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.mindlab.worky.R;

/** Seta Voltar consistente (tema NoActionBar). */
public final class WorkyNav {

    private WorkyNav() {}

    public static void bindToolbar(
            @NonNull AppCompatActivity activity,
            @Nullable MaterialToolbar toolbar,
            @Nullable CharSequence title,
            boolean showUp
    ) {
        if (toolbar == null) return;
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(title != null ? title : "");
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(showUp);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(showUp);
        }
        if (showUp) {
            toolbar.setNavigationOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());
        }
        View root = activity.findViewById(android.R.id.content);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            WorkyInsets.apply(activity, ((ViewGroup) root).getChildAt(0), 8, 12);
        }
    }

    public static void bindFromContent(@NonNull AppCompatActivity activity, @Nullable CharSequence title) {
        MaterialToolbar toolbar = activity.findViewById(R.id.toolbarWorky);
        bindToolbar(activity, toolbar, title, true);
    }

    public static void finishSafely(@NonNull AppCompatActivity activity) {
        activity.finish();
    }
}
