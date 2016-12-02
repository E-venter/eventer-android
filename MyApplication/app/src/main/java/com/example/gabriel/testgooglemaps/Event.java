package com.example.gabriel.testgooglemaps;

class Event {
    public String name = "";
    public double latitude = 0.0;
    public double longitude = 0.0;
    public double radius = 0.0;

    public Event(){}

    public Event(String name, double radius, double latitude, double longitude){
        this.name = name;
        this.radius = radius;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String toString(){
        return "name:" + name + " radius:" + radius + " latitude:" + latitude + " longitude:" + longitude;
    }
}
