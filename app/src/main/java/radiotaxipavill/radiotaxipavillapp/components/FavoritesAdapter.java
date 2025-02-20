package radiotaxipavill.radiotaxipavillapp.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private List<MapController.FavoriteDestination> favorites;
    private final OnFavoriteClickListener listener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(MapController.FavoriteDestination favorite);
    }

    public FavoritesAdapter(List<MapController.FavoriteDestination> favorites, OnFavoriteClickListener listener) {
        this.favorites = favorites;
        this.listener = listener;
    }

    public void updateFavorites(List<MapController.FavoriteDestination> newFavorites) {
        this.favorites = newFavorites;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        MapController.FavoriteDestination favorite = favorites.get(position);
        holder.bind(favorite, listener);
    }

    @Override
    public int getItemCount() {
        return favorites != null ? favorites.size() : 0;
    }

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
}
