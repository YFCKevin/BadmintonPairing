package com.yfckevin.badmintonPairing.dto;

import java.util.ArrayList;
import java.util.List;

public class MateDTO {
    private String courtId;
    private List<String> postIdList = new ArrayList<>();

    public String getCourtId() {
        return courtId;
    }

    public void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    public List<String> getPostIdList() {
        return postIdList;
    }

    public void setPostIdList(List<String> postIdList) {
        this.postIdList = postIdList;
    }
}
