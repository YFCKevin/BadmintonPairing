package com.yfckevin.badmintonPairing.entity;

import com.yfckevin.badmintonPairing.enums.AirConditionerType;
import jakarta.persistence.Id;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Document(collection = "post")
public class Post {
    @Id
    private String id;
    private String userId;
    private String name;
    private String place;
    private String startTime;
    private String endTime;
    private String level;
    private int fee;
    private double duration;
    private String brand;
    private String contact;
    private String parkInfo;
    private String type;
    private AirConditionerType airConditioner;
    private String dayOfWeek;
    private String creationDate;
    private String modificationDate;
    private String deletionDate;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String startTime) {
        if (StringUtils.isNotBlank(startTime)) {
            LocalDateTime dateTime = LocalDateTime.parse(startTime, formatter);
            this.dayOfWeek = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
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

    public void setAirConditioner(AirConditionerType airConditioner) {
        this.airConditioner = airConditioner;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(String modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getDeletionDate() {
        return deletionDate;
    }

    public void setDeletionDate(String deletionDate) {
        this.deletionDate = deletionDate;
    }
}
