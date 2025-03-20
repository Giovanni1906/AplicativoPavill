package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.FavoritesAdapter;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.NavigationHeaderInfo;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;

public class FavoritesActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerViewFavorites;
    private FavoritesAdapter favoritesAdapter;
    private List<MapController.FavoriteDestination> favoriteList = new ArrayList<>();
    private final List<String> selectedFavorites = new ArrayList<>();
    private List<String> unselectedFavorites = new ArrayList<>();
    private AppCompatButton saveButton;


    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Inicializar LoadingDialog
        loadingDialog = new LoadingDialog(this);

        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        saveButton = findViewById(R.id.save_button);

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

        saveButton.setOnClickListener(v -> {
            loadingDialog.show();
            List<String> selectedFavoritesIds  = new ArrayList<>(selectedFavorites);
            List<String> unselectedFavoritesIds = new ArrayList<>();

            for (MapController.FavoriteDestination fav : favoriteList) {
                if (!selectedFavoritesIds.contains(fav.getId())) {
                    unselectedFavoritesIds.add(fav.getId());
                }
            }

            // Log para depuración: Ver qué se está enviando realmente
            Log.d("FavoritesActivity", "Seleccionados (Estado 3): " + selectedFavoritesIds );
            Log.d("FavoritesActivity", "No seleccionados (Estado 4): " + unselectedFavoritesIds);

            // Enviar al servidor
            updateFavoritesState(selectedFavoritesIds, unselectedFavoritesIds);
        });
    }

    private void updateFavoritesState(List<String> selectedIds, List<String> unselectedIds) {
        if (selectedIds.isEmpty() && unselectedIds.isEmpty()) {
            loadingDialog.dismiss();
            Toast.makeText(this, "No hay cambios para guardar.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar un diálogo de carga mientras se procesa
        loadingDialog.show();

        new MapController().updateFavoriteStates(this, selectedIds, unselectedIds, new MapController.UpdateFavoritesCallback() {
            @Override
            public void onSuccess(String message) {
                loadingDialog.dismiss();
                Toast.makeText(FavoritesActivity.this, "Cambios guardados correctamente.", Toast.LENGTH_SHORT).show();
                fetchAllFavorites(); // 🔄 Recargar lista después de actualizar
            }

            @Override
            public void onFailure(String errorMessage) {
                loadingDialog.dismiss();
                Toast.makeText(FavoritesActivity.this, "Error al actualizar: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Obtiene todos los favoritos del usuario desde el servidor.
     */
    private void fetchAllFavorites() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String clienteId = sharedPreferences.getString("ClienteId", "");

        // Mostrar el ProgressBar antes de hacer la solicitud
        ProgressBar progressBar = findViewById(R.id.progressBarFavorites);
        progressBar.setVisibility(View.VISIBLE);

        new MapController().fetchFavoriteDestinations(this, clienteId, true, new MapController.FavoriteDestinationsCallback() {

            @Override
            public void onFavoritesReceived(List<MapController.FavoriteDestination> origins, List<MapController.FavoriteDestination> destinations) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE); // Ocultar ProgressBar
                    favoriteList.clear();
                    favoriteList.addAll(origins);
                    favoriteList.addAll(destinations);

                    favoritesAdapter = new FavoritesAdapter(
                            favoriteList,
                            selectedFavorites, // ← Agregar esta lista
                            favorite -> {
                                // Alternar selección/deselección
                                if (selectedFavorites.contains(favorite.getId())) {
                                    selectedFavorites.remove(favorite.getId());
                                } else {
                                    if (selectedFavorites.size() < 2) {
                                        selectedFavorites.add(favorite.getId());
                                    } else {
                                        Toast.makeText(FavoritesActivity.this, "Máximo dos direcciones favoritas", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                favoritesAdapter.notifyDataSetChanged();
                            },
                            favorite -> {
                                loadingDialog.show();
                                // ⚡ Llamar al método de eliminación
                                new MapController().deleteFavorite(FavoritesActivity.this, favorite.getId(), new MapController.FavoriteDeleteCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        Toast.makeText(FavoritesActivity.this, message, Toast.LENGTH_SHORT).show();
                                        fetchAllFavorites(); // 🔄 Recargar lista después de eliminar
                                        loadingDialog.dismiss();
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        loadingDialog.dismiss();
                                        Toast.makeText(FavoritesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            },
                            true // Usa item_favorite_menu.xml
                    );



                    recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(FavoritesActivity.this));
                    recyclerViewFavorites.setAdapter(favoritesAdapter);
                });
            }

            @Override
            public void onNoFavoritesFound() {
                progressBar.setVisibility(View.GONE); // Ocultar ProgressBar
                runOnUiThread(() -> Toast.makeText(FavoritesActivity.this, "No hay favoritos guardados.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE); // Ocultar ProgressBar
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
                    // Lógica para navegar a perfil
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

