package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.FavoritesAdapter;
import radiotaxipavill.radiotaxipavillapp.components.NavigationHeaderInfo;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;

public class FavoritesActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerViewFavorites;
    private FavoritesAdapter favoritesAdapter;
    private List<MapController.FavoriteDestination> favoriteList = new ArrayList<>();
    private final List<String> selectedFavorites = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));

        fetchAllFavorites();

        // Obtener referencia del DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
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
    }

    /**
     * Obtiene todos los favoritos del usuario desde el servidor.
     */
    private void fetchAllFavorites() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", "");

        new MapController().fetchFavoriteDestinations(this, clienteId, true, new MapController.FavoriteDestinationsCallback() {
            @Override
            public void onFavoritesReceived(List<MapController.FavoriteDestination> origins, List<MapController.FavoriteDestination> destinations) {
                runOnUiThread(() -> {
                    favoriteList.clear();
                    favoriteList.addAll(origins);
                    favoriteList.addAll(destinations);

                    favoritesAdapter = new FavoritesAdapter(
                            favoriteList,
                            selectedFavorites, // ← Agregar esta lista
                            favorite -> {
                                // Alternar selección/deselección
                                if (selectedFavorites.contains(favorite.getAddress())) {
                                    selectedFavorites.remove(favorite.getAddress());
                                } else {
                                    if (selectedFavorites.size() < 2) {
                                        selectedFavorites.add(favorite.getAddress());
                                    }
                                }
                                favoritesAdapter.notifyDataSetChanged();
                            },
                            favorite -> {
                                // Acción para eliminar favorito
                            },
                            true // Usa item_favorite_menu.xml
                    );



                    recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(FavoritesActivity.this));
                    recyclerViewFavorites.setAdapter(favoritesAdapter);
                });
            }

            @Override
            public void onNoFavoritesFound() {
                runOnUiThread(() -> Toast.makeText(FavoritesActivity.this, "No hay favoritos guardados.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(FavoritesActivity.this, "Error al obtener favoritos.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupNavigationView() {
        // Configurar el encabezado
        NavigationHeaderInfo.setupHeader(this, navigationView);

        // Manejar los eventos de click en los elementos del menú del NavigationView
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_history) {
                    // Lógica para navegar al historial
                    Intent historyIntent = new Intent(FavoritesActivity.this, HistoryActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_maps) {
                    // Lógica para navegar al mapa
                    Intent historyIntent = new Intent(FavoritesActivity.this, MapActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_points) {
                    // Lógica para navegar a points
                    Intent historyIntent = new Intent(FavoritesActivity.this, PointsActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_profile) {
                    // Lógica para navegar a favoritos
                    Intent historyIntent = new Intent(FavoritesActivity.this, ProfileActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_logout) {
                    // Lógica para cerrar sesión
                    SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();

                    Intent logoutIntent = new Intent(FavoritesActivity.this, MainActivity.class);
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

