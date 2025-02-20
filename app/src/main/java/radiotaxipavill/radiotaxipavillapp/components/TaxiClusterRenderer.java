package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class TaxiClusterRenderer extends DefaultClusterRenderer<MyClusterItem> {

    private final BitmapDescriptor taxiIcon;

    public TaxiClusterRenderer(Context context, GoogleMap map, ClusterManager<MyClusterItem> clusterManager, BitmapDescriptor taxiIcon) {
        super(context, map, clusterManager);
        this.taxiIcon = taxiIcon;
    }

    @Override
    protected void onBeforeClusterItemRendered(MyClusterItem item, MarkerOptions markerOptions) {
        markerOptions.icon(taxiIcon);
        markerOptions.title(item.getTitle());
        markerOptions.snippet(item.getSnippet());
        super.onBeforeClusterItemRendered(item, markerOptions);
    }
}