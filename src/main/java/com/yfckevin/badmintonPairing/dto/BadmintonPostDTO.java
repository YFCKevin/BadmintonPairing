package com.yfckevin.badmintonPairing.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.yfckevin.badmintonPairing.enums.AirConditionerType;

public class BadmintonPostDTO {
    private String name;
    private String userId;
    private String place;
    private String startTime;
    private String endTime;
    private String level;
    private int fee;
    private int duration;
    private String brand;
    private String contact;
    private String parkInfo;
    private String type;
    private AirConditionerType airConditioner;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getFee() {
        return fee;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getParkInfo() {
        return parkInfo;
    }

    public void setParkInfo(String parkInfo) {
        this.parkInfo = parkInfo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AirConditionerType getAirConditioner() {
        return airConditioner;
    }
    @JsonSetter("airConditioner")
    public void setAirConditioner(AirConditionerType airConditioner) {
        this.airConditioner = airConditioner;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "BadmintonPostDTO{" +
                "name='" + name + '\'' +
                ", userId='" + userId + '\'' +
                ", place='" + place + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", level='" + level + '\'' +
                ", fee=" + fee +
                ", duration=" + duration +
                ", brand='" + brand + '\'' +
                ", contact='" + contact + '\'' +
                ", parkInfo='" + parkInfo + '\'' +
                ", type='" + type + '\'' +
                ", airConditioner=" + airConditioner +
                '}';
    }
}
