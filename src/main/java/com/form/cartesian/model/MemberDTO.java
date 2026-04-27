package com.form.cartesian.model;

public class MemberDTO {
    private String memberId;
    private String memberCnName;
    private String memberEnName;

    public MemberDTO() {}

    public MemberDTO(String memberId) {
        this.memberId = memberId;
    }

    public MemberDTO(String memberId, String memberCnName, String memberEnName) {
        this.memberId = memberId;
        this.memberCnName = memberCnName;
        this.memberEnName = memberEnName;
    }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberCnName() { return memberCnName; }
    public void setMemberCnName(String memberCnName) { this.memberCnName = memberCnName; }
    public String getMemberEnName() { return memberEnName; }
    public void setMemberEnName(String memberEnName) { this.memberEnName = memberEnName; }
}