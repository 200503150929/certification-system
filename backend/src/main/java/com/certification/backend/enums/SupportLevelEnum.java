package com.certification.backend.enums;

public enum SupportLevelEnum {
    H("H", "强支撑"),
    M("M", "中支撑"),
    L("L", "弱支撑");

    private final String code;
    private final String desc;

    SupportLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static boolean isValid(String code) {
        if (code == null) return false;
        for (SupportLevelEnum level : values()) {
            if (level.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}