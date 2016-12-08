package com.example.gabriel.testgooglemaps;

import java.util.Date;

class Event {
    public int id = 0;
    public String name = "no_name";
    public String address = "no_address";
    public double latitude = 0.0;
    public double longitude = 0.0;
    public String description = "no_desription";
    public long start_time = 0;
    public double radius = 0.0;
    public String event_type = "no_type";
    public long end_time = 0;
    public String owner_name = "no_name";
    public String owner_email = "no_email";

    public Event(){}

    public Event(String name,
                 double latitude,
                 double longitude,
                 double radius,
                 long start_time,
                 long end_time,
                 int id,
                 String owner_email,
                 String owner_name,
                 String address,
                 String description,
                 String event_type) {

        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius > 0 ? radius : 0;
        this.start_time = start_time;
        this.end_time = end_time;
        this.id = id;
        this.owner_email = owner_email;
        this.owner_name = owner_name;
        this.address = address;
        this.description = description;
        this.event_type = event_type;
    }

    public String toString(){
        return "name:" + name + " radius:" + radius + " latitude:" + latitude + " longitude:" + longitude;
    }

    public boolean equals(Object e) {
        return e instanceof Event && id == ((Event) e).id;
    }
}
