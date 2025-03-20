package radiotaxipavill.radiotaxipavillapp.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;

public class FavoritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SIMPLE = 0; // Para item_favorite.xml
    private static final int VIEW_TYPE_MENU = 1; // Para item_favorite_menu.xml

    private List<MapController.FavoriteDestination> favorites;
    private final OnFavoriteClickListener clickListener;
    private final OnFavoriteDeleteListener deleteListener;
    private final boolean useMenuLayout;
    private final List<String> selectedFavorites; // IDs de favoritos seleccionados

    public interface OnFavoriteClickListener {
        void onFavoriteClick(MapController.FavoriteDestination favorite);
    }

    public interface OnFavoriteDeleteListener {
        void onFavoriteDelete(MapController.FavoriteDestination favorite);
    }

    public FavoritesAdapter(List<MapController.FavoriteDestination> favorites,
                            List<String> selectedFavorites,
                            OnFavoriteClickListener clickListener,
                            OnFavoriteDeleteListener deleteListener,
                            boolean useMenuLayout) {
        this.favorites = new ArrayList<>();
        this.selectedFavorites = selectedFavorites;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.useMenuLayout = useMenuLayout;
        setFavorites(favorites);
    }

    public void updateFavorites(List<MapController.FavoriteDestination> newFavorites) {
        setFavorites(newFavorites);
    }

    private void setFavorites(List<MapController.FavoriteDestination> newFavorites) {
        if (newFavorites == null) {
            this.favorites.clear();
        } else {
            // Ordenar: Primero estado 3, luego estado 4
            Collections.sort(newFavorites, Comparator.comparingInt(MapController.FavoriteDestination::getEstado));
            this.favorites = new ArrayList<>(newFavorites);

            if (useMenuLayout) {
                // Limpiar selección y seleccionar hasta 2 favoritos con estado 3
                selectedFavorites.clear();
                int count = 0;
                for (MapController.FavoriteDestination fav : favorites) {
                    if (fav.getEstado() == 3) {
                        selectedFavorites.add(fav.getId());
                        count++;
                        if (count == 2) break;
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return useMenuLayout ? VIEW_TYPE_MENU : VIEW_TYPE_SIMPLE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_MENU) {
            View view = inflater.inflate(R.layout.item_favorite_menu, parent, false);
            return new FavoriteMenuViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_favorite, parent, false);
            return new FavoriteViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MapController.FavoriteDestination favorite = favorites.get(position);
        if (holder instanceof FavoriteMenuViewHolder) {
            ((FavoriteMenuViewHolder) holder).bind(favorite, clickListener, deleteListener, selectedFavorites.contains(favorite.getId()));
        } else if (holder instanceof FavoriteViewHolder) {
            ((FavoriteViewHolder) holder).bind(favorite, clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return favorites != null ? favorites.size() : 0;
    }

    // ViewHolder para item_favorite.xml
    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final TextView favoriteAddress;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            favoriteAddress = itemView.findViewById(R.id.favoriteAddress);
        }

        public void bind(MapController.FavoriteDestination favorite, OnFavoriteClickListener listener) {
            favoriteAddress.setText(favorite.getAddress());
            itemView.setOnClickListener(v -> listener.onFavoriteClick(favorite));
        }
    }

    // ViewHolder para item_favorite_menu.xml (con RadioButton)
    class FavoriteMenuViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatRadioButton favoriteRadioButton;
        private final View deleteButton;

        public FavoriteMenuViewHolder(@NonNull View itemView) {
            super(itemView);
            favoriteRadioButton = itemView.findViewById(R.id.favoriteAddress);
            deleteButton = itemView.findViewById(R.id.btnDeleteFavorite);
        }

        public void bind(MapController.FavoriteDestination favorite,
                         OnFavoriteClickListener listener,
                         OnFavoriteDeleteListener deleteListener,
                         boolean isSelected) {
            favoriteRadioButton.setText(favorite.getAddress());
            favoriteRadioButton.setChecked(isSelected);

            favoriteRadioButton.setOnClickListener(v -> {
                if (selectedFavorites.contains(favorite.getId())) {
                    selectedFavorites.remove(favorite.getId()); // Deseleccionar
                } else {
                    if (selectedFavorites.size() < 2) {
                        selectedFavorites.add(favorite.getId()); // Seleccionar
                    }
                }
                notifyDataSetChanged(); // Refrescar UI
            });

            deleteButton.setOnClickListener(v -> deleteListener.onFavoriteDelete(favorite));
        }
    }
}
