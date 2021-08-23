package com.gmail.mapmygas;

public class StationModel {

    private String station_id;
    private String station_name;
    private String station_address;
    private String station_status;

    private double latitude;
    private double longitude;

    public StationModel() {}

    public StationModel(String station_name, String station_address, String station_status,
                        String station_id, double latitude, double longitude) {
        this.station_name = station_name;
        this.station_address = station_address;
        this.station_status = station_status;
        this.station_id = station_id;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }

    public String getstation_address() {
        return station_address;
    }

    public void setstation_address(String station_address) {
        this.station_address = station_address;
    }

    public String getStation_status() {
        return station_status;
    }

    public void setStation_status(String station_status) {
        this.station_status = station_status;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getStation_id() {
        return station_id;
    }

    public void setStation_id(String station_id) {
        this.station_id = station_id;
    }

}
