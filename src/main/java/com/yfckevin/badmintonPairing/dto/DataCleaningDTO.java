package com.yfckevin.badmintonPairing.dto;

public class DataCleaningDTO {
    private String name;    //團主名
    private String postContent;     //貼文內容
    private String userId;  //團主編號

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
