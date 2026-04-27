package com.form.cartesianv2.dto;

public class MemberValueDTO {
    private Long valueId;
    private String valueName;
    private Integer level;

    public Long getValueId() {
        return valueId;
    }

    public String getValueName() {
        return valueName;
    }

    public Integer getLevel() {
        return level;
    }

    public MemberValueDTO() {
    }

    public MemberValueDTO(Long valueId, String valueName, Integer level) {
        this.valueId = valueId;
        this.valueName = valueName;
        this.level = level;
    }

    public void setValueId(Long valueId) {
        this.valueId = valueId;
    }

    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
