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

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.FavoritesAdapter;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.NavigationHeaderInfo;
import radiotaxipavill.radiotaxipavillapp.components.TemporaryData;
import radiotaxipavill.radiotaxipavillapp.controller.CalcularTarifaController;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;
import radiotaxipavill.radiotaxipavillapp.controller.RequestTaxiController;

public class FavoritesActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerViewFavorites;
    private FavoritesAdapter favoritesAdapter;
    private LoadingDialog loadingDialog;

    TemporaryData temporaryData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        temporaryData = TemporaryData.getInstance();
        temporaryData.loadFromPreferences(this);

        // Inicializar LoadingDialog
        loadingDialog = new LoadingDialog(this);

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

        ProgressBar progressBar = findViewById(R.id.progressBarFavorites);
        progressBar.setVisibility(View.VISIBLE);

        new MapController().fetchFavoriteDestinations(this, clienteId, false, new MapController.FavoriteDestinationsCallback() {
            @Override
            public void onFavoritesReceived(List<MapController.FavoriteDestination> origins, List<MapController.FavoriteDestination> destinations) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    List<MapController.FavoriteDestination> allFavorites = new ArrayList<>();
                    allFavorites.addAll(origins);
                    allFavorites.addAll(destinations);

                    // Limitar a los primeros 5 favoritos si hay más
                    if (allFavorites.size() > 5) {
                        allFavorites = allFavorites.subList(0, 5);
                    }

                    favoritesAdapter = new FavoritesAdapter(
                            allFavorites,
                            favorite -> {
                                loadingDialog.show();
                                new MapController().deleteFavorite(FavoritesActivity.this, favorite.getId(), new MapController.FavoriteDeleteCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        loadingDialog.dismiss();
                                        Toast.makeText(FavoritesActivity.this, message, Toast.LENGTH_SHORT).show();
                                        fetchAllFavorites(); // Recargar
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        loadingDialog.dismiss();
                                        Toast.makeText(FavoritesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            },
                            favorite -> {
                                // 👉 Lógica al presionar el botón de taxi
                                LatLng originLatLng = new LatLng(favorite.getLatitude(), favorite.getLongitude());

                                // Validación: si no hay destino, no hacer nada
                                if (favorite.getLatitude() == 0 || favorite.getLongitude() == 0) {
                                    Toast.makeText(FavoritesActivity.this, "Favorito sin ubicación válida.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                temporaryData.setOriginCoordinates(originLatLng, FavoritesActivity.this);
                                temporaryData.setDestinationCoordinates(null, FavoritesActivity.this); // Por ahora null, puedes modificar si el favorito incluye destino

                                String originAddress = favorite.getAddress();
                                String destinationAddress = ""; // Modifica si tienes destino

                                double originLat = favorite.getLatitude();
                                double originLng = favorite.getLongitude();

                                // Mostrar loading
                                loadingDialog.show();

                                // ⚡ Calcular tarifa
                                new CalcularTarifaController().calcularTarifa(FavoritesActivity.this, originLat, originLng, 0.0, 0.0, new CalcularTarifaController.CalcularTarifaCallback() {
                                    @Override
                                    public void onSuccess(String tarifario, String respuesta) {
                                        temporaryData.setEstimatedCost(tarifario, FavoritesActivity.this);
                                        solicitarTaxi();
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        temporaryData.setEstimatedCost("N/A", FavoritesActivity.this);
                                        solicitarTaxi(); // Igual se hace el request
                                    }

                                    private void solicitarTaxi() {
                                        String reference = favorite.getReference() != null ? favorite.getReference() : "Referencia desconocida";

                                        new RequestTaxiController().requestTaxi(
                                                FavoritesActivity.this,
                                                originAddress,
                                                destinationAddress,
                                                originLat,
                                                originLng,
                                                0.0,
                                                0.0,
                                                reference,
                                                false,
                                                new RequestTaxiController.RequestTaxiCallback() {
                                                    @Override
                                                    public void onSuccess(String message) {
                                                        long requestTime = System.currentTimeMillis();
                                                        temporaryData.setRequestTime(requestTime, FavoritesActivity.this);
                                                        loadingDialog.dismiss();

                                                        Intent intent = new Intent(FavoritesActivity.this, SearchingActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                    }

                                                    @Override
                                                    public void onFailure(String errorMessage) {
                                                        loadingDialog.dismiss();
                                                        Toast.makeText(FavoritesActivity.this, "Error al solicitar taxi: " + errorMessage, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                        );
                                    }
                                });
                            }

                            );

                    recyclerViewFavorites.setAdapter(favoritesAdapter);
                });
            }

            @Override
            public void onNoFavoritesFound() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FavoritesActivity.this, "No hay favoritos guardados.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FavoritesActivity.this, "Error al obtener favoritos.", Toast.LENGTH_SHORT).show();
                });
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