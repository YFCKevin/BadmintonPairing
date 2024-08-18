package com.yfckevin.badmintonPairing.dto;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

public class TemplateSubjectDTO {
    private String id;
    private String title;
    private String altText;    //短描述
    private List<String> userIds = new ArrayList<>();   //傳送對象
    private String sendDate;    //發送日期時間
    private List<String> detailIds = new ArrayList<>();
    private String creationDate;
    private String templateType;

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public String getSendDate() {
        return sendDate;
    }

    public void setSendDate(String sendDate) {
        this.sendDate = sendDate;
    }

    public List<String> getDetailIds() {
        return detailIds;
    }

    public void setDetailIds(List<String> detailIds) {
        this.detailIds = detailIds;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }
}
