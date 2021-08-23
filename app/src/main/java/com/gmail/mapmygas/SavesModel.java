package com.gmail.mapmygas;

public class SavesModel {

    private String station_name;
    private String station_address;
    private String station_status;
    private String station_id;

    private double longitude;
    private double latitude;

    public SavesModel() {}

    public SavesModel(String station_name, String station_address, String station_status, String station_id, double longitude, double latitude) {
        this.station_name = station_name;
        this.station_address = station_address;
        this.station_status = station_status;
        this.station_id = station_id;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }

    public String getStation_address() {
        return station_address;
    }

    public void setStation_address(String station_address) {
        this.station_address = station_address;
    }

    public String getStation_status() {
        return station_status;
    }

    public void setStation_status(String station_status) {
        this.station_status = station_status;
    }

    public String getStation_id() {
        return station_id;
    }

    public void setStation_id(String station_id) {
        this.station_id = station_id;
    }
}
