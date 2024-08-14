package com.yfckevin.badmintonPairing.dto;

public class NearByRequestDTO {
    private double myLat;
    private double myLon;
    private double radius;

    public double getMyLat() {
        return myLat;
    }

    public void setMyLat(double myLat) {
        this.myLat = myLat;
    }

    public double getMyLon() {
        return myLon;
    }

    public void setMyLon(double myLon) {
        this.myLon = myLon;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
