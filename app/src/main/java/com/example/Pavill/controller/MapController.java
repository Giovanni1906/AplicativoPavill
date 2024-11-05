package com.example.Pavill.controller;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapController {

    private SelectionState currentSelection = SelectionState.NONE;

    public MapController() {

    }

    public enum SelectionState {
        ORIGIN,
        DESTINATION,
        NONE
    }



    public void toggleSelection(boolean isForOrigin) {
        if (isForOrigin) {
            currentSelection = (currentSelection == SelectionState.ORIGIN) ? SelectionState.NONE : SelectionState.ORIGIN;
        } else {
            currentSelection = (currentSelection == SelectionState.DESTINATION) ? SelectionState.NONE : SelectionState.DESTINATION;
        }
    }

    public boolean isActiveSelection(boolean isForOrigin) {
        return (isForOrigin && currentSelection == SelectionState.ORIGIN) ||
                (!isForOrigin && currentSelection == SelectionState.DESTINATION);
    }

}