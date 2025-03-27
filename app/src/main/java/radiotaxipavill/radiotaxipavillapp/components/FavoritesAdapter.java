package radiotaxipavill.radiotaxipavillapp.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.controller.MapController;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private List<MapController.FavoriteDestination> favorites;
    private final OnFavoriteDeleteListener deleteListener;
    private final OnFavoriteSendClickListener sendListener;

    public interface OnFavoriteDeleteListener {
        void onFavoriteDelete(MapController.FavoriteDestination favorite);
    }

    public interface OnFavoriteSendClickListener {
        void onSendFavorite(MapController.FavoriteDestination favorite);
    }

    public FavoritesAdapter(List<MapController.FavoriteDestination> favorites, OnFavoriteDeleteListener deleteListener, OnFavoriteSendClickListener sendListener) {
        this.favorites = favorites;
        this.deleteListener = deleteListener;
        this.sendListener = sendListener;
    }

    public void updateFavorites(List<MapController.FavoriteDestination> newFavorites) {
        this.favorites = newFavorites;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        MapController.FavoriteDestination favorite = favorites.get(position);
        holder.favoriteAddress.setText(favorite.getAddress());
        holder.deleteButton.setOnClickListener(v -> deleteListener.onFavoriteDelete(favorite));
        holder.sendButton.setOnClickListener(v -> sendListener.onSendFavorite(favorite));
    }

    @Override
    public int getItemCount() {
        return favorites != null ? favorites.size() : 0;
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView favoriteAddress;
        ImageButton deleteButton;
        ImageButton sendButton;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            favoriteAddress = itemView.findViewById(R.id.favoriteAddress);
            deleteButton = itemView.findViewById(R.id.btnDeleteFavorite);
            sendButton = itemView.findViewById(R.id.btnSendFavorite);

        }
    }
}
