package com.gin.modegg;

import android.content.Intent;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.gin.modegg.ui.home.HomeFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private FirebaseAuth mAuth;
    private Button toggleButton;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //configuramos el floting acction buton del nav_bar
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            // Reemplaza con la URL que deseas abrir
            String url = "https://g.co/kgs/ezGNjYp";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mAuth = FirebaseAuth.getInstance();

        // Actualizar UI con información del usuario
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView navUserName = headerView.findViewById(R.id.textViewUser);
            TextView navUserEmail = headerView.findViewById(R.id.textViewEmail);
            ImageView navUserPhoto = headerView.findViewById(R.id.imageView);

            String displayName = user.getDisplayName();
            String email = user.getEmail();

            if (displayName != null) {
                navUserName.setText(displayName);
            } else {
                navUserName.setText("Nombre de usuario no disponible");
            }

            if (email != null) {
                navUserEmail.setText(email);
            } else {
                navUserEmail.setText("Correo electrónico no disponible");
            }

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.placeholder_image) // Placeholder si la imagen tarda en cargar
                        .error(R.drawable.error_image) // Imagen de error si no se puede cargar la imagen
                        .into(navUserPhoto);
            } else {
                // Manejar caso donde no hay URL de imagen de perfil
                navUserPhoto.setImageResource(R.drawable.default_profile_image);
            }
        } else {
            // Manejar el caso donde el usuario no está autenticado
            Log.d(TAG, "Usuario no autenticado");
        }

        // Maneja la selección del elemento del menú
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment_content_home,
                    new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        toggleButton = findViewById(R.id.btnActivModeGG);
        toggleButton.setOnClickListener(v -> {
            toggleBatterySaverMode();
            toggleGPS();
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment_content_home,
                    new HomeFragment()).commit();
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void toggleBatterySaverMode() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        if (batteryLevel < 50) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Battery saver mode is not supported on this device.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Battery level is above 50%", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleGPS() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
}
