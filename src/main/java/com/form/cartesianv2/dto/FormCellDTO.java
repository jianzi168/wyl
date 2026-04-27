package com.form.cartesianv2.dto;

public class FormCellDTO {
    private Long[] memberIds;
    private String dataValue;

    public FormCellDTO(Long[] memberIds, String dataValue) {
        this.memberIds = memberIds;
        this.dataValue = dataValue;
    }

    public FormCellDTO() {
    }

    public String getDataValue() {
        return dataValue;
    }

    public Long[] getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(Long[] memberIds) {
        this.memberIds = memberIds;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }
}
