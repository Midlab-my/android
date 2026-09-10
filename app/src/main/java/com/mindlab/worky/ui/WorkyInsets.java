package com.mindlab.worky.ui;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Evita UI colada na status bar / gesture nav (safe area).
 */
public final class WorkyInsets {

    private WorkyInsets() {}

    public static void apply(@NonNull Activity activity, @Nullable View root) {
        apply(activity, root, 16, 16);
    }

    public static void apply(
            @NonNull Activity activity,
            @Nullable View root,
            int extraTopDp,
            int extraBottomDp
    ) {
        if (root == null) return;
        if (activity instanceof ComponentActivity) {
            EdgeToEdge.enable((ComponentActivity) activity);
        }

        final int extraTop = dp(activity, extraTopDp);
        final int extraBottom = dp(activity, extraBottomDp);
        final int extraSide = dp(activity, 16);

        final int baseL = root.getPaddingLeft();
        final int baseT = root.getPaddingTop();
        final int baseR = root.getPaddingRight();
        final int baseB = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    Math.max(baseL, bars.left + extraSide / 4),
                    bars.top + Math.max(baseT, extraTop),
                    Math.max(baseR, bars.right + extraSide / 4),
                    bars.bottom + Math.max(baseB, extraBottom)
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    /** Padding so no bottom sheet content fica sob a barra de gestos. */
    public static void applyBottomOnly(@NonNull View view, int extraBottomDp) {
        final int extraBottom = dp(view.getContext(), extraBottomDp);
        final int baseL = view.getPaddingLeft();
        final int baseT = view.getPaddingTop();
        final int baseR = view.getPaddingRight();
        final int baseB = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseL, baseT, baseR, baseB + bars.bottom + extraBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void padBottom(@NonNull View view, int extraBottomDp) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (lp != null) {
            lp.bottomMargin = dp(view.getContext(), extraBottomDp);
            view.setLayoutParams(lp);
        }
    }

    private static int dp(@NonNull android.content.Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }
}
