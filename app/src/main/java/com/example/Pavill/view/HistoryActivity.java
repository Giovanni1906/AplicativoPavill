package com.example.Pavill.view;
import com.example.Pavill.R;
import com.example.Pavill.components.NavigationHeaderInfo;
import com.google.android.material.navigation.NavigationView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class HistoryActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Obtener referencia del NavigationView
        navigationView = findViewById(R.id.nav_view);

        // Configurar NavigationView
        setupNavigationView();

        // Obtener referencia del botón de menú
        CardView btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
        if (btnOpenSidebar != null) {
            btnOpenSidebar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(navigationView);
                    }
                }
            });
        }

        //Lógica del history
    }

    private void setupNavigationView() {
        // Configurar el encabezado
        NavigationHeaderInfo.setupHeader(this, navigationView);

        // Manejar los eventos de click en los elementos del menú del NavigationView
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_profile) {
                    // Lógica para navegar al historial
                    Intent historyIntent = new Intent(HistoryActivity.this, ProfileActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_maps) {
                    // Lógica para navegar al mapa
                    Intent historyIntent = new Intent(HistoryActivity.this, MapActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_points) {
                    // Lógica para navegar al points
                    Intent historyIntent = new Intent(HistoryActivity.this, PointsActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_logout) {
                    // Lógica para cerrar sesión
                    SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();

                    Intent logoutIntent = new Intent(HistoryActivity.this, MainActivity.class);
                    logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logoutIntent);
                    finish();
                }

                // Cerrar el drawer después de seleccionar una opción
                drawerLayout.closeDrawer(navigationView);
                return true;
            }
        });
    }
}