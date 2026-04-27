package com.form.cartesian.model;

import java.util.List;

public class FormFieldInfoDTO {
    private String dimCode;
    private String dimId;
    private String dimCnName;
    private String dimEnName;
    private List<MemberDTO> members;

    public FormFieldInfoDTO() {}

    public FormFieldInfoDTO(String dimCode, String dimId, List<MemberDTO> members) {
        this.dimCode = dimCode;
        this.dimId = dimId;
        this.members = members;
    }

    public String getDimCode() { return dimCode; }
    public void setDimCode(String dimCode) { this.dimCode = dimCode; }
    public String getDimId() { return dimId; }
    public void setDimId(String dimId) { this.dimId = dimId; }
    public String getDimCnName() { return dimCnName; }
    public void setDimCnName(String dimCnName) { this.dimCnName = dimCnName; }
    public String getDimEnName() { return dimEnName; }
    public void setDimEnName(String dimEnName) { this.dimEnName = dimEnName; }
    public List<MemberDTO> getMembers() { return members; }
    public void setMembers(List<MemberDTO> members) { this.members = members; }
}