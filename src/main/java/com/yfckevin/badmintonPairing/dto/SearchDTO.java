package com.yfckevin.badmintonPairing.dto;

public class SearchDTO {
    private String keyword;
    private String startDate;
    private String endDate;
    private String labelCourt;

    public String getLabelCourt() {
        return labelCourt;
    }

    public void setLabelCourt(String labelCourt) {
        this.labelCourt = labelCourt;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
