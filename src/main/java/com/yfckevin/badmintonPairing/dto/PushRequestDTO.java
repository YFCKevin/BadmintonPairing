package com.yfckevin.badmintonPairing.dto;

import java.util.HashMap;
import java.util.List;

public class PushRequestDTO {
    private List<String> userIdList;    //要傳送訊息的追蹤者名單
    private String subjectId;    //模板編號

    public List<String> getUserIdList() {
        return userIdList;
    }

    public void setUserIdList(List<String> userIdList) {
        this.userIdList = userIdList;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
}
