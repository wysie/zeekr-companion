package com.kooo.evcam.settings;

import android.view.View;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import com.google.android.material.tabs.TabLayout;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.zeekr.SideSettingsPreview;

/** Presentation around the existing preferences. Tabs never write or reset configuration. */
final class SideCameraSettingsPane extends LinearLayout {
    static final String[] SECTIONS = {"screen_blind_spot_camera", "sub_side_image", "sub_side_window", "sub_side_hud"};
    private final View preferences;
    private final LinearLayout body;
    private final SideSettingsPreview preview;
    private boolean expanded;

    static boolean handles(String section) {
        for (String candidate : SECTIONS) if (candidate.equals(section)) return true;
        return false;
    }

    SideCameraSettingsPane(SettingsPreferenceFragment fragment, View preferences, String section) {
        super(fragment.requireContext());
        android.widget.FrameLayout preferenceHost = new android.widget.FrameLayout(getContext());
        preferenceHost.setId(R.id.side_settings_preferences);
        preferenceHost.addView(preferences);
        this.preferences = preferenceHost;
        setOrientation(VERTICAL);
        TabLayout tabs = new TabLayout(new android.view.ContextThemeWrapper(getContext(), R.style.ThemeOverlay_Cam_SettingsTabs));
        tabs.setId(R.id.side_settings_tabs);
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabs.setTabGravity(TabLayout.GRAVITY_START);
        tabs.setTabTextColors(ContextCompat.getColor(getContext(), R.color.text_secondary),
                ContextCompat.getColor(getContext(), R.color.text_primary));
        tabs.setSelectedTabIndicatorColor(ContextCompat.getColor(getContext(), R.color.energy));
        tabs.setSelectedTabIndicatorHeight(dp(2));
        tabs.setTabRippleColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.nav_ripple_color)));
        int[] labels = {R.string.side_tab_behavior, R.string.side_tab_picture, R.string.side_tab_window, R.string.side_tab_hud};
        for (int i = 0; i < labels.length; i++) tabs.addTab(tabs.newTab().setText(labels[i]), SECTIONS[i].equals(section));
        addView(tabs, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                Fragment parent = fragment.getParentFragment();
                if (parent instanceof SettingsShellFragment)
                    ((SettingsShellFragment) parent).showSection(SECTIONS[tab.getPosition()]);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        View line = new View(getContext()); line.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.line));
        addView(line, new LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        body = new LinearLayout(getContext()); body.setId(R.id.side_settings_body);
        body.setBaselineAligned(false);
        addView(body, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        body.addView(this.preferences);
        preview = "sub_side_image".equals(section) && fragment.getActivity() instanceof MainActivity
                ? new SideSettingsPreview((MainActivity) fragment.requireActivity(), this::expandPreview) : null;
        if (preview != null) body.addView(preview);
        arrange(0);
    }

    void startPreview() { if (preview != null) preview.start(); }
    void stopPreview() { if (preview != null) preview.stop(); }

    private void expandPreview(boolean expanded) { this.expanded = expanded; arrange(getWidth()); }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) post(() -> arrange(w));
    }

    private void arrange(int width) {
        boolean horizontal = width >= dp(760);
        body.setOrientation(horizontal ? HORIZONTAL : VERTICAL);
        preferences.setVisibility(expanded ? GONE : VISIBLE);
        if (preview == null) {
            preferences.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        } else if (horizontal) {
            preferences.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, .44f));
            preview.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, expanded ? 1f : .56f));
        } else {
            preferences.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
            preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, expanded ? LayoutParams.MATCH_PARENT : dp(290)));
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
