package com.yfckevin.badmintonPairing.enums;

public enum StickerResourceType {
    static_type("STATIC", "靜態圖片貼圖"),
    animation("ANIMATION", "動畫貼圖"),
    sound("SOUND", "含有聲音的貼圖"),
    animation_sound("ANIMATION_SOUND", "動畫加聲音的貼圖"),
    popup("POPUP", "彈出式貼圖"),
    popup_sound("POPUP_SOUND", "彈出加聲音的貼圖"),
    message("MESSAGE", "可在聊天訊息中展示的貼圖"),
    rich("RICH", "富貼圖");

    private final String value;
    private final String label;

    StickerResourceType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}

