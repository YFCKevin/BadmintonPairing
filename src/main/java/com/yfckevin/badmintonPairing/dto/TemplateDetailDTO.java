package com.yfckevin.badmintonPairing.dto;

import org.springframework.web.multipart.MultipartFile;

public class TemplateDetailDTO {
    private String id;
    private String subjectId;   //主包裝id
    private String mainTitle;   //主標題
    private String subTitle;    //副標題
    private String textContent; //文字內容
    private MultipartFile multipartFile;    //圖片
    private String coverPath;    //封面圖路徑(前端呈現用)
    private String creationDate;
    private String buttonName;   //按鈕名稱
    private String buttonUrl;    //按鈕導向的網址

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public MultipartFile getMultipartFile() {
        return multipartFile;
    }

    public void setMultipartFile(MultipartFile multipartFile) {
        this.multipartFile = multipartFile;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getButtonName() {
        return buttonName;
    }

    public void setButtonName(String buttonName) {
        this.buttonName = buttonName;
    }

    public String getButtonUrl() {
        return buttonUrl;
    }

    public void setButtonUrl(String buttonUrl) {
        this.buttonUrl = buttonUrl;
    }

    @Override
    public String toString() {
        return "TemplateDetailDTO{" +
                "id='" + id + '\'' +
                ", subjectId='" + subjectId + '\'' +
                ", mainTitle='" + mainTitle + '\'' +
                ", subTitle='" + subTitle + '\'' +
                ", textContent='" + textContent + '\'' +
                ", multipartFile=" + multipartFile +
                ", coverPath='" + coverPath + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", buttonName='" + buttonName + '\'' +
                ", buttonUrl='" + buttonUrl + '\'' +
                '}';
    }
}
