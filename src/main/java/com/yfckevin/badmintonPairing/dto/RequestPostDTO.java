package com.yfckevin.badmintonPairing.dto;

public class RequestPostDTO {
    private String name;    //團主名
    private int like;   //按讚數
    private String postContent;     //貼文內容
    private String link;    //團主fb個人首頁
    private String userId;  //團主編號
    private String creationDate;    //資料匯入general file的日期
//    private PostType postType;  //零打or轉讓

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLike() {
        return like;
    }

    public void setLike(int like) {
        this.like = like;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
//    public PostType getPostType() {
//        return postType;
//    }
//
//    public void setPostType(PostType postType) {
//        this.postType = postType;
//    }

    @Override
    public String toString() {
        return "RequestPostDTO{" +
                "name='" + name + '\'' +
                ", like=" + like +
                ", postContent='" + postContent + '\'' +
                ", link='" + link + '\'' +
                ", userId='" + userId + '\'' +
                ", creationDate='" + creationDate + '\'' +
                '}';
    }
}
