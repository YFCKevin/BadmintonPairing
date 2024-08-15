package com.yfckevin.badmintonPairing.dto;

import java.util.ArrayList;
import java.util.List;

public class CourtDTO {
    private String id;
    private String name;    //球館名稱
    private String address; //地理位置地址
    private double latitude;    //緯度
    private double longitude;   //經度
    private List<PostDTO> postDTOList = new ArrayList<>();  //貼文資訊
    private String postId; //貼文集，用逗號相連
    private String creationDate;

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public List<PostDTO> getPostDTOList() {
        return postDTOList;
    }

    public void setPostDTOList(List<PostDTO> postDTOList) {
        this.postDTOList = postDTOList;
    }
}
