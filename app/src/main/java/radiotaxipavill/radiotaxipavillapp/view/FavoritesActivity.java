package radiotaxipavill.radiotaxipavillapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.FavoritesAdapter;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;

public class FavoritesActivity extends AppCompatActivity {

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
}