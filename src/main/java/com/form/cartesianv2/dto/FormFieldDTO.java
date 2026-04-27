package com.form.cartesianv2.dto;


import java.util.List;

public class FormFieldDTO {
    private String position;
    private List<FieldDimDTO> memberDTOS;

    public void setPosition(String position) {
        this.position = position;
    }

    public void setMemberDTOS(List<FieldDimDTO> memberDTOS) {
        this.memberDTOS = memberDTOS;
    }

    public String getPosition() {
        return position;
    }

    public List<FieldDimDTO> getMemberDTOS() {
        return memberDTOS;
    }

    public FormFieldDTO(String position, List<FieldDimDTO> memberDTOS) {
        this.position = position;
        this.memberDTOS = memberDTOS;
    }

    public FormFieldDTO() {
    }
}
