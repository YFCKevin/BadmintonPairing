package com.yfckevin.badmintonPairing.dto;

import java.util.ArrayList;
import java.util.List;

public class MateDTO {
    private String courtId;
    private List<String> matePostIds = new ArrayList<>();

    public String getCourtId() {
        return courtId;
    }

    public void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    public List<String> getMatePostIds() {
        return matePostIds;
    }

    public void setMatePostIds(List<String> matePostIds) {
        this.matePostIds = matePostIds;
    }
}
