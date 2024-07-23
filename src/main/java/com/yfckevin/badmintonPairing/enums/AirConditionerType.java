package com.yfckevin.badmintonPairing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AirConditionerType {
    present(1, "有冷氣"), absent(2, "無冷氣"), no_mention(3, "未標示");

    private int value;
    private String label;

    private AirConditionerType(){
    }

    private AirConditionerType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonCreator
    public static AirConditionerType fromString(String key) {
        if (key == null || key.isEmpty()) {
            return AirConditionerType.no_mention; // 默認值
        }
        for (AirConditionerType type : AirConditionerType.values()) {
            if (type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return AirConditionerType.no_mention; // 無效值默認為 no_mention
    }
}


