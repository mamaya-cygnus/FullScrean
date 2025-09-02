package com.full.screan;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.full.screan.ui.home.HomeFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.full.screan.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Configurar listener personalizado para el navigation view
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                navController.navigate(R.id.nav_home);
                // Esperar un poco y luego recargar
                new Handler().postDelayed(() -> reloadHomeFragment(), 100);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            } else {
                // Para otros elementos, usar navegación normal
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (handled) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                return handled;
            }
        });

        // Configurar pantalla completa
        setupFullscreen();
    }

    private void setupFullscreen() {
        // Configurar la ventana para pantalla completa
        getWindow().setDecorFitsSystemWindows(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Para Android 11 y superior
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Para versiones anteriores
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        // Hacer las barras transparentes
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Asegurar que se mantenga en pantalla completa
        setupFullscreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupFullscreen();
        }
    }

    private void reloadHomeFragment() {
        // Obtener el fragment actual
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (currentFragment != null) {
            NavHostFragment navHostFragment = (NavHostFragment) currentFragment;
            Fragment homeFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);

            if (homeFragment instanceof HomeFragment) {
                ((HomeFragment) homeFragment).reloadWebView();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
    @Override
    public void onBackPressed() {
        // Verificar si estamos en el HomeFragment y si el WebView puede retroceder
        if (canWebViewGoBack()) {
            goBackInWebView();
        } else {
            // Comportamiento normal de retroceso
            super.onBackPressed();
        }
    }

    private boolean canWebViewGoBack() {
        try {
            // Obtener el fragment actual
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);

            if (currentFragment != null) {
                NavHostFragment navHostFragment = (NavHostFragment) currentFragment;
                List<Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();

                if (!fragments.isEmpty()) {
                    Fragment homeFragment = fragments.get(0);
                    if (homeFragment instanceof HomeFragment) {
                        return ((HomeFragment) homeFragment).canGoBack();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error checking WebView back", e);
        }
        return false;
    }

    private void goBackInWebView() {
        try {
            // Obtener el fragment actual
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);

            if (currentFragment != null) {
                NavHostFragment navHostFragment = (NavHostFragment) currentFragment;
                List<Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();

                if (!fragments.isEmpty()) {
                    Fragment homeFragment = fragments.get(0);
                    if (homeFragment instanceof HomeFragment) {
                        ((HomeFragment) homeFragment).goBack();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error going back in WebView", e);
        }
    }
}