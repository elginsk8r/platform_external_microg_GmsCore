package org.microg.gms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.BuildConfig;
import com.google.android.gms.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.microg.gms.common.Constants;
import org.microg.gms.ui.settings.SettingsProvider;

import static org.microg.gms.ui.UtilsKt.buildAlertDialog;
import static org.microg.gms.ui.settings.SettingsProviderKt.getAllSettingsProviders;

public class MainSettingsActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;

    private static final String FIRST_RUN_MASTER = "org.microg.gms_firstRun";
    private static final String FIRST_RUN_PREF = "as_run";

    private NavController getNavController() {
        return ((NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.navhost)).getNavController();
    }

    private void showDialogIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(FIRST_RUN_MASTER, MODE_PRIVATE);
        if (BuildConfig.APPLICATION_ID == Constants.USER_MICROG_PACKAGE_NAME &&
                prefs.getBoolean(FIRST_RUN_PREF, true)) {
            buildAlertDialog(this)
                    .setMessage(R.string.limited_services_dialog_information)
                    .setTitle(R.string.limited_services_app_name)
                    .setPositiveButton(R.string.limited_services_dialog_information_ack, (dialog, id) -> {
                        prefs.edit().putBoolean(FIRST_RUN_PREF, false).apply();
                    })
                    .create()
                    .show();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        for (SettingsProvider settingsProvider : getAllSettingsProviders(this)) {
            settingsProvider.preProcessSettingsIntent(intent);
        }

        setContentView(R.layout.settings_root_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            enableEdgeToEdge();
        }
        if (Build.VERSION.SDK_INT >= 36) {
            // Expressive theme: show preference groups as rounded cards
            getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
                    if (f instanceof PreferenceFragmentCompat) {
                        ((PreferenceFragmentCompat) f).getListView().addItemDecoration(new ExpressiveGroupDecoration(f.requireContext()));
                    }
                }
            }, true);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);

        for (SettingsProvider settingsProvider : getAllSettingsProviders(this)) {
            settingsProvider.extendNavigation(getNavController());
        }

        // Deep links are handled when the navigation host is created, before settings providers extended the graph,
        // so links to their screens are handled again here.
        NavController navController = getNavController();
        if (savedInstanceState == null && intent.getData() != null && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == navController.getGraph().getStartDestinationId()) {
            navController.handleDeepLink(intent);
        }

        appBarConfiguration = new AppBarConfiguration.Builder(getNavController().getGraph()).build();
        NavigationUI.setupWithNavController(toolbarLayout, toolbar, getNavController(), appBarConfiguration);
        showDialogIfNeeded();
    }

    private void enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View navHost = findViewById(R.id.navhost);
        // Handle insets at the content root: with legacy inset dispatch, the collapsing toolbar would consume them
        // before they reach the scrolling content.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, windowInsets) -> {
            int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets insets = windowInsets.getInsets(types);
            view.setPadding(insets.left, 0, insets.right, 0);
            navHost.setPadding(navHost.getPaddingLeft(), navHost.getPaddingTop(), navHost.getPaddingRight(), insets.bottom);
            return new WindowInsetsCompat.Builder(windowInsets)
                    .setInsets(types, Insets.of(0, insets.top, 0, 0))
                    .build();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(getNavController(), appBarConfiguration) || super.onSupportNavigateUp();
    }
}
