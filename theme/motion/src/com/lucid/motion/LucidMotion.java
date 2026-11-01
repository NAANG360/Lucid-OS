package com.lucid.motion;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.TimeInterpolator;
import android.app.Activity;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;

/**
 * Shared Lucid motion: quiet, short transitions that keep the interface responsive.
 * No cascading card choreography, bounce effects, or forced animation when system
 * animation scale is disabled.
 */
public final class LucidMotion {
    private static final TimeInterpolator CURVE = new PathInterpolator(0.2f, 0f, 0f, 1f);
    private LucidMotion() {}

    public static void apply(Activity activity) {
        if (activity == null || animationsDisabled(activity)) return;
        final View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        content.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            boolean done;
            @Override public boolean onPreDraw() {
                if (done) return true;
                done = true;
                content.getViewTreeObserver().removeOnPreDrawListener(this);
                content.setAlpha(0.985f);
                content.animate().alpha(1f).setDuration(150L).setInterpolator(CURVE).start();
                installPressStates(content);
                return true;
            }
        });
    }

    private static void installPressStates(View view) {
        if (view.isClickable() && view.getStateListAnimator() == null) {
            StateListAnimator states = new StateListAnimator();
            ObjectAnimator pressed = ObjectAnimator.ofFloat(view, View.ALPHA, 0.82f);
            pressed.setDuration(85L);
            ObjectAnimator released = ObjectAnimator.ofFloat(view, View.ALPHA, 1f);
            released.setDuration(120L);
            states.addState(new int[]{android.R.attr.state_pressed}, pressed);
            states.addState(new int[]{}, released);
            view.setStateListAnimator(states);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) installPressStates(group.getChildAt(i));
        }
    }

    public static void transition(View outgoing, View incoming) {
        if (incoming == null) return;
        if (animationsDisabled(incoming.getContext())) {
            incoming.setAlpha(1f);
            incoming.setVisibility(View.VISIBLE);
            if (outgoing != null) outgoing.setVisibility(View.GONE);
            return;
        }
        incoming.setVisibility(View.VISIBLE);
        incoming.setAlpha(0f);
        incoming.animate().alpha(1f).setDuration(170L).setInterpolator(CURVE).start();
        if (outgoing != null) outgoing.animate().alpha(0f).setDuration(120L)
            .setInterpolator(CURVE).withEndAction(() -> outgoing.setVisibility(View.GONE)).start();
    }

    private static boolean animationsDisabled(android.content.Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception ignored) { return false; }
    }
}
