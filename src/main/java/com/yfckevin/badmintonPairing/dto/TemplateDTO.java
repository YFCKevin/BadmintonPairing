package com.yfckevin.badmintonPairing.dto;

import com.yfckevin.badmintonPairing.entity.Follower;
import com.yfckevin.badmintonPairing.enums.TemplateType;

import java.util.ArrayList;
import java.util.List;

public class TemplateDTO {

    //subject
    private String subjectId;
    private String subjectTitle;
    private String subjectAltText;
    private List<String> userIds = new ArrayList<>();   //傳送對象
    private String sendDate;    //發送日期時間
    private String creationDate;
    private String templateType;
    //detail
    private List<TemplateDetailDTO> detailDTOList = new ArrayList<>();

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectTitle() {
        return subjectTitle;
    }

    public void setSubjectTitle(String subjectTitle) {
        this.subjectTitle = subjectTitle;
    }

    public String getSendDate() {
        return sendDate;
    }

    public void setSendDate(String sendDate) {
        this.sendDate = sendDate;
    }

    public List<TemplateDetailDTO> getDetailDTOList() {
        return detailDTOList;
    }

    public void setDetailDTOList(List<TemplateDetailDTO> detailDTOList) {
        this.detailDTOList = detailDTOList;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getSubjectAltText() {
        return subjectAltText;
    }

    public void setSubjectAltText(String subjectAltText) {
        this.subjectAltText = subjectAltText;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    @Override
    public String toString() {
        return "TemplateDTO{" +
                "subjectId='" + subjectId + '\'' +
                ", subjectTitle='" + subjectTitle + '\'' +
                ", subjectAltText='" + subjectAltText + '\'' +
                ", userIds=" + userIds +
                ", sendDate='" + sendDate + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", templateType='" + templateType + '\'' +
                ", detailDTOList=" + detailDTOList +
                '}';
    }
}
