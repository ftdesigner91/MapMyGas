package com.gmail.mapmygas;

public class ServicesModel {

    private String station_id;
    private String millis;
    private String service_image;
    private String service_title;
    private String service_description;

    public ServicesModel() {}
    public ServicesModel(String service_image, String service_title, String service_description, String station_id, String millis) {
        this.service_image = service_image;
        this.service_title = service_title;
        this.service_description = service_description;
        this.station_id = station_id;
        this.millis = millis;
    }

    public String getService_image() {
        return service_image;
    }

    public void setService_image(String service_image) {
        this.service_image = service_image;
    }

    public String getService_title() {
        return service_title;
    }

    public void setService_title(String service_title) {
        this.service_title = service_title;
    }

    public String getService_description() {
        return service_description;
    }

    public void setService_description(String service_description) {
        this.service_description = service_description;
    }

    public String getStation_id() {
        return station_id;
    }

    public void setStation_id(String station_id) {
        this.station_id = station_id;
    }

    public String getMillis() {
        return millis;
    }

    public void setMillis(String millis) {
        this.millis = millis;
    }
}
