package com.example.gabriel.testgooglemaps;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Date;

public class EventMarker {
    private static final int INITIAL_SIZE_RADIUS = 100;

    private Marker marker;
    private Circle circle;

    public Event event;

    public EventMarker(String title, LatLng latLng, Date startDate, Date endDate, GoogleMap googleMap, String userEmail){
        this(title, latLng, INITIAL_SIZE_RADIUS, startDate, endDate, googleMap, userEmail);
    }

    public EventMarker(String name, LatLng latLng, int radius, Date startDate, Date endDate, GoogleMap googleMap, String userEmail){
        this(new Event(name, radius, latLng.latitude, latLng.longitude, startDate.getTime(), endDate.getTime(), 0, "", "", "", "", "", false), googleMap, userEmail);
    }

    public EventMarker(Event baseEvent, GoogleMap googleMap, String userEmail){
        event = baseEvent.clone();

        LatLng latLng = new LatLng(event.latitude, event.longitude);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                .title(event.name)
                .draggable(false);

        if(baseEvent.owner_email.equals(userEmail)){
            markerOptions = markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        }else if(baseEvent.checked_in){
            markerOptions = markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }

        CircleOptions circleOptions = new CircleOptions().center(latLng)
                .radius(event.radius)
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
