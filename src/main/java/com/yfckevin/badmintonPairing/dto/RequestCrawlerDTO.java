package com.yfckevin.badmintonPairing.dto;

import java.util.List;

public class RequestCrawlerDTO {
    private String email;
    private String password;
    private List<String> urls;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
