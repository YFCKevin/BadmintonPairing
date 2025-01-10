package com.yfckevin.badmintonPairing.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yfckevin.badmintonPairing.enums.AirConditionerType;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "post")
public class PostDoc {
    @Id
    private String id;
    @Field(type = FieldType.Keyword)
    private String name;
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String place;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String level;
    @Field(type = FieldType.Integer)
    private int fee;
    @Field(type = FieldType.Double, index = false)
    private double duration;
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String brand;
    @Field(type = FieldType.Keyword, index = false)
    private String contact;
    @Field(type = FieldType.Keyword, index = false)
    private String parkInfo;
    @Field(type = FieldType.Keyword)
    private AirConditionerType airConditioner;
    @Field(type = FieldType.Keyword)
    private String dayOfWeek;

    @Field(type = FieldType.Keyword, index = false)
    private String userId;

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

    public AirConditionerType getAirConditioner() {
        return airConditioner;
    }

    public void setAirConditioner(AirConditionerType airConditioner) {
        this.airConditioner = airConditioner;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "PostDoc{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", place='" + place + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", level='" + level + '\'' +
                ", fee=" + fee +
                ", duration=" + duration +
                ", brand='" + brand + '\'' +
                ", contact='" + contact + '\'' +
                ", parkInfo='" + parkInfo + '\'' +
                ", airConditioner=" + airConditioner +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
