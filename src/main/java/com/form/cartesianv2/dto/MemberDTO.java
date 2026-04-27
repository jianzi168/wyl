package com.form.cartesianv2.dto;

import java.util.List;

public class MemberDTO {
    private String position;
    private List<MemberValueDTO> memberVals;

    public void setPosition(String position) {
        this.position = position;
    }

    public void setMemberVals(List<MemberValueDTO> memberVals) {
        this.memberVals = memberVals;
    }

    public String getPosition() {
        return position;
    }

    public List<MemberValueDTO> getMemberVals() {
        return memberVals;
    }

    public MemberDTO(List<MemberValueDTO> memberVals, String position) {
        this.memberVals = memberVals;
        this.position = position;
    }

    public MemberDTO() {
    }
}
