package com.yfckevin.badmintonPairing.enums;

public enum TemplateType {
    textImage(1, "圖文訊息"), carousel(2, "輪詢訊息");

    private int value;
    private String label;

    private TemplateType(){
    }

    private TemplateType(int value, String label) {
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
}


