package com.example.gabriel.testgooglemaps;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class EventMarker {
    private static final int INITIAL_SIZE_RADIUS = 100;

    private Marker marker;
    private Circle circle;

    String s = "potato";

    public EventMarker(String title, LatLng latLng, GoogleMap googleMap){
        this(title, latLng, INITIAL_SIZE_RADIUS, googleMap);
    }

    public EventMarker(String title, LatLng latLng, int radius, GoogleMap googleMap){
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                                                          .title(title)
                                                          .draggable(false);
        CircleOptions circleOptions = new CircleOptions().center(latLng)
                                                          .radius(radius)
                                                          .strokeWidth(5f);
        marker = googleMap.addMarker(markerOptions);
        circle = googleMap.addCircle(circleOptions);

        marker.setTag(this);
    }

    public Marker getMarker(){
        return marker;
    }

    public Circle getCircle(){
        return circle;
    }

}
