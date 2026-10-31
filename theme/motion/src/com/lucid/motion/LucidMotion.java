package com.lucid.motion;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lucid Motion: shared motion language for all first-party Lucid applications.
 * Restrained violet-era motion: quick tactile response, soft staggered entrances,
 * spring return and interruption-safe property animation. Respects system animator scale.
 */
public final class LucidMotion {
    private static final TimeInterpolator EMPHASIZED = new PathInterpolator(0.2f, 0f, 0f, 1f);
    private static final TimeInterpolator SPRING = new OvershootInterpolator(0.72f);
    private static final long ENTER_DURATION = 420L;
    private static final long STAGGER = 34L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private LucidMotion() {}

    public static void apply(Activity activity) {
        if (activity == null) return;
        final View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            boolean done;
            @Override public boolean onPreDraw() {
                if (done) return true;
                done = true;
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                if (animationsDisabled(activity)) return true;
                AtomicInteger index = new AtomicInteger(0);
                prepare(root, index);
                return true;
            }
        });
    }

    private static void prepare(View view, AtomicInteger index) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view.isClickable() && view.getStateListAnimator() == null) addPressMotion(view);
        if (view.getParent() instanceof ViewGroup && view.getParent() != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent.getId() == android.R.id.content || parent instanceof android.widget.LinearLayout
                    || parent instanceof android.widget.FrameLayout || parent instanceof android.widget.ScrollView) {
                int order = index.getAndIncrement();
                view.setAlpha(0f);
                view.setTranslationY(dp(view, 13));
                view.animate().alpha(1f).translationY(0f)
                    .setStartDelay(Math.min(order * STAGGER, 260L))
                    .setDuration(ENTER_DURATION).setInterpolator(EMPHASIZED).start();
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) prepare(group.getChildAt(i), index);
        }
    }

    private static void addPressMotion(View view) {
        StateListAnimator states = new StateListAnimator();
        AnimatorSet pressed = new AnimatorSet();
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.975f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.975f);
        pressed.playTogether(sx, sy);
        pressed.setDuration(100L);
        pressed.setInterpolator(EMPHASIZED);
        AnimatorSet released = new AnimatorSet();
        ObjectAnimator rx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f);
        ObjectAnimator ry = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f);
        released.playTogether(rx, ry);
        released.setDuration(280L);
        released.setInterpolator(SPRING);
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, released);
        view.setStateListAnimator(states);
    }

    public static void transition(View outgoing, View incoming) {
        if (incoming == null) return;
        if (animationsDisabled(incoming.getContext())) { incoming.setAlpha(1f); incoming.setVisibility(View.VISIBLE); return; }
        incoming.setVisibility(View.VISIBLE);
        incoming.setAlpha(0f);
        incoming.setTranslationY(dp(incoming, 18));
        incoming.animate().alpha(1f).translationY(0f).setDuration(360L).setInterpolator(EMPHASIZED).start();
        if (outgoing != null) outgoing.animate().alpha(0f).translationY(-dp(outgoing, 8))
            .setDuration(180L).setInterpolator(EMPHASIZED).withEndAction(() -> outgoing.setVisibility(View.GONE)).start();
    }

    private static boolean animationsDisabled(android.content.Context context) {
        try { return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f; }
        catch (Exception ignored) { return false; }
    }
    private static float dp(View v, float n) { return n * v.getResources().getDisplayMetrics().density; }
}
