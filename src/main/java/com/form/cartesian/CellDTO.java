package com.form.cartesian;

public class CellDTO {
    private String[] memberIds;
    private String dataValue;

    public CellDTO() {}

    public CellDTO(String[] memberIds) {
        this.memberIds = memberIds;
        this.dataValue = null;
    }

    public CellDTO(String[] memberIds, String dataValue) {
        this.memberIds = memberIds;
        this.dataValue = dataValue;
    }

    public String[] getMemberIds() { return memberIds; }
    public void setMemberIds(String[] memberIds) { this.memberIds = memberIds; }
    public String getDataValue() { return dataValue; }
    public void setDataValue(String dataValue) { this.dataValue = dataValue; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        if (memberIds != null) {
            for (int i = 0; i < memberIds.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(memberIds[i]);
            }
        }
        sb.append("]");
        if (dataValue != null) {
            sb.append("=").append(dataValue);
        }
        return sb.toString();
    }
}