package com.yfckevin.badmintonPairing.enums;

public enum PostType {
    disposable(1, "零打"), release(2, "場地轉讓");

    private int value;
    private String label;

    private PostType(){
    }

    private PostType(int value, String label) {
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

