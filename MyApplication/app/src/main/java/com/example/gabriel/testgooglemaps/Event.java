package com.example.gabriel.testgooglemaps;

class Event {
    public double latitude = 0.0;
    public double longitude = 0.0;
    public double radius = 0.0;

    public String name = "";

    public Event(){}

    public Event(String name, double radius, double latitude, double longitude){
        this.name = name;
        this.radius = radius;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
