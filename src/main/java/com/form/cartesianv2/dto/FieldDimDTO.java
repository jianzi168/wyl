package com.form.cartesianv2.dto;


import java.util.List;

public class FieldDimDTO {
    private Long dimId;
    private String dimName;
    private List<MemberDTO> memberDTOS;

    public Long getDimId() {
        return dimId;
    }

    public void setDimId(Long dimId) {
        this.dimId = dimId;
    }

    public String getDimName() {
        return dimName;
    }

    public void setDimName(String dimName) {
        this.dimName = dimName;
    }

    public List<MemberDTO> getMemberDTOS() {
        return memberDTOS;
    }

    public void setMemberDTOS(List<MemberDTO> memberDTOS) {
        this.memberDTOS = memberDTOS;
    }
}
