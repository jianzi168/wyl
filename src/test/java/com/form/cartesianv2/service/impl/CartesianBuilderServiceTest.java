package com.form.cartesianv2.service.impl;

import com.form.cartesianv2.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CartesianBuilderService 单元测试
 */
class CartesianBuilderServiceTest {

    private CartesianBuilderService service;

    @BeforeEach
    void setUp() {
        service = new CartesianBuilderService();
    }

    @Test
    void testBuildCrossProduct_basicCase() {
        // R1: 产品{P11,P12} × 版本{V11,V12} -> 4个小行
        // C1: 年{2023,2024} × 期间{Q1,Q2} -> 4个小列
        // 期望: 4行 × 4列 = 16格

        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        // 行配置 - R1大行
        rowFieldGroups.add(buildRowGroupR1());

        // 列配置 - C1大列
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        assertNotNull(result);
        assertEquals(4, result.size(), "应该有4行");
        for (List<FormCellDTO> row : result) {
            assertEquals(4, row.size(), "每行应该有4列");
        }
    }

    @Test
    void testBuildCrossProduct_twoRowGroups_twoColGroups() {
        // R1: 产品{P11,P12} × 版本{V11,V12} -> 4个小行
        // R2: 产品{P21,P22} × 版本{V21,V22} -> 4个小行
        // C1: 年{2023,2024} × 期间{Q1,Q2} -> 4个小列
        // C2: 年{2025,2026} × 期间{Q3,Q4} -> 4个小列
        // 期望: 8行 × 8列 = 64格

        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        rowFieldGroups.add(buildRowGroupR2());

        colFieldGroups.add(buildColGroupC1());
        colFieldGroups.add(buildColGroupC2());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        assertNotNull(result);
        assertEquals(8, result.size(), "应该有8行");
        for (List<FormCellDTO> row : result) {
            assertEquals(8, row.size(), "每行应该有8列");
        }
    }

    @Test
    void testBuildCrossProduct_memberMergeInSameDim() {
        // 测试同维度下成员合并取valueId
        // R1: 产品维度有重复valueId的成员 -> 应该去重

        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        // R1: 产品维度有成员 A(valueId=101), B(valueId=102), A(valueId=101重复)
        //     版本维度有成员 C(valueId=201), D(valueId=202)
        //     期望: 产品{101, 102} × 版本{201, 202} = 4个小行
        List<FormFieldDTO> r1Group = new ArrayList<>();

        FormFieldDTO productDim = new FormFieldDTO();
        productDim.setPosition("R1");

        List<FieldDimDTO> productDims = new ArrayList<>();
        FieldDimDTO productFieldDim = new FieldDimDTO();
        productFieldDim.setDimId(1L);
        productFieldDim.setDimName("产品");

        // 成员A, B, A(valueId重复)
        List<MemberDTO> productMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(101L, 102L)),  // 成员1: valueId=101,102
                createMemberWithValueIds(Arrays.asList(102L, 103L)),  // 成员2: valueId=102,103 (102重复)
                createMemberWithValueIds(Arrays.asList(101L))         // 成员3: valueId=101 (101重复)
        );
        productFieldDim.setMemberDTOS(productMembers);
        productDims.add(productFieldDim);

        FormFieldDTO versionDim = new FormFieldDTO();
        versionDim.setPosition("R1");

        List<FieldDimDTO> versionDims = new ArrayList<>();
        FieldDimDTO versionFieldDim = new FieldDimDTO();
        versionFieldDim.setDimId(2L);
        versionFieldDim.setDimName("版本");

        List<MemberDTO> versionMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(201L)),  // 成员1: valueId=201
                createMemberWithValueIds(Arrays.asList(202L))   // 成员2: valueId=202
        );
        versionFieldDim.setMemberDTOS(versionMembers);
        versionDims.add(versionFieldDim);

        productDim.setMemberDTOS(productDims);
        versionDim.setMemberDTOS(versionDims);

        r1Group.add(productDim);
        r1Group.add(versionDim);

        rowFieldGroups.add(r1Group);

        // C1: 年维度 + 期间维度
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        assertNotNull(result);
        // 产品去重后{101,102,103} × 版本{201,202} = 3×2=6个小行
        // 6小行 × 4小列 = 24格
        assertEquals(6, result.size(), "产品维度去重后应有6小行");
        for (List<FormCellDTO> row : result) {
            assertEquals(4, row.size());
        }
    }

    @Test
    void testBuildCrossProduct_emptyInput() {
        assertEquals(Collections.emptyList(), service.buildCrossProduct(null, null));
        assertEquals(Collections.emptyList(), service.buildCrossProduct(new ArrayList<>(), new ArrayList<>()));
        assertEquals(Collections.emptyList(), service.buildCrossProduct(Arrays.asList(new ArrayList<>()), new ArrayList<>()));
    }

    @Test
    void testBuildCrossProduct_firstCell() {
        // 验证第一个单元格的内容顺序
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        FormCellDTO firstCell = result.get(0).get(0);
        Long[] memberIds = firstCell.getMemberIds();

        assertNotNull(memberIds);
        // R1: 产品(P11->101,P12->102) × 版本(V11->201,V12->202) 按配置顺序
        // C1: 年(2023->301,2024->302) × 期间(Q1->401,Q2->402) 按配置顺序
        // 期望顺序: [101, 201, 301, 401] (产品valueId, 版本valueId, 年valueId, 期间valueId)
        assertEquals(4, memberIds.length);
        System.out.println("第一格成员IDs: " + Arrays.toString(memberIds));
    }

    @Test
    void testBuildCrossProduct_lastCell() {
        // 验证最后一个单元格的内容顺序
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        rowFieldGroups.add(buildRowGroupR2());
        colFieldGroups.add(buildColGroupC1());
        colFieldGroups.add(buildColGroupC2());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        FormCellDTO lastCell = result.get(7).get(7);
        Long[] memberIds = lastCell.getMemberIds();

        assertNotNull(memberIds);
        // Each cell has 4 memberIds: 2 from row (product+version) + 2 from column (year+period)
        assertEquals(4, memberIds.length);
        // Last cell should be R2's last combination with C2's last combination
        // R2: (302, 402), C2: (702, 802)
        assertEquals(302L, memberIds[0]);
        assertEquals(402L, memberIds[1]);
        assertEquals(702L, memberIds[2]);
        assertEquals(802L, memberIds[3]);
    }

    // ==================== Helper Methods ====================

    /**
     * 构建R1大行的字段配置:
     * 产品维度: P11(valueId=101), P12(valueId=102)
     * 版本维度: V11(valueId=201), V12(valueId=202)
     */
    private List<FormFieldDTO> buildRowGroupR1() {
        List<FormFieldDTO> group = new ArrayList<>();

        // 产品维度
        FormFieldDTO productField = new FormFieldDTO();
        productField.setPosition("R1");

        List<FieldDimDTO> productDims = new ArrayList<>();
        FieldDimDTO productDim = new FieldDimDTO();
        productDim.setDimId(1L);
        productDim.setDimName("产品");

        List<MemberDTO> productMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(101L)),
                createMemberWithValueIds(Arrays.asList(102L))
        );
        productDim.setMemberDTOS(productMembers);
        productDims.add(productDim);
        productField.setMemberDTOS(productDims);
        group.add(productField);

        // 版本维度
        FormFieldDTO versionField = new FormFieldDTO();
        versionField.setPosition("R1");

        List<FieldDimDTO> versionDims = new ArrayList<>();
        FieldDimDTO versionDim = new FieldDimDTO();
        versionDim.setDimId(2L);
        versionDim.setDimName("版本");

        List<MemberDTO> versionMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(201L)),
                createMemberWithValueIds(Arrays.asList(202L))
        );
        versionDim.setMemberDTOS(versionMembers);
        versionDims.add(versionDim);
        versionField.setMemberDTOS(versionDims);
        group.add(versionField);

        return group;
    }

    /**
     * 构建R2大行的字段配置:
     * 产品维度: P21(valueId=301), P22(valueId=302)
     * 版本维度: V21(valueId=401), V22(valueId=402)
     */
    private List<FormFieldDTO> buildRowGroupR2() {
        List<FormFieldDTO> group = new ArrayList<>();

        // 产品维度
        FormFieldDTO productField = new FormFieldDTO();
        productField.setPosition("R2");

        List<FieldDimDTO> productDims = new ArrayList<>();
        FieldDimDTO productDim = new FieldDimDTO();
        productDim.setDimId(3L);
        productDim.setDimName("产品");

        List<MemberDTO> productMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(301L)),
                createMemberWithValueIds(Arrays.asList(302L))
        );
        productDim.setMemberDTOS(productMembers);
        productDims.add(productDim);
        productField.setMemberDTOS(productDims);
        group.add(productField);

        // 版本维度
        FormFieldDTO versionField = new FormFieldDTO();
        versionField.setPosition("R2");

        List<FieldDimDTO> versionDims = new ArrayList<>();
        FieldDimDTO versionDim = new FieldDimDTO();
        versionDim.setDimId(4L);
        versionDim.setDimName("版本");

        List<MemberDTO> versionMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(401L)),
                createMemberWithValueIds(Arrays.asList(402L))
        );
        versionDim.setMemberDTOS(versionMembers);
        versionDims.add(versionDim);
        versionField.setMemberDTOS(versionDims);
        group.add(versionField);

        return group;
    }

    /**
     * 构建C1大列的字段配置:
     * 年维度: 2023(valueId=501), 2024(valueId=502)
     * 期间维度: Q1(valueId=601), Q2(valueId=602)
     */
    private List<FormFieldDTO> buildColGroupC1() {
        List<FormFieldDTO> group = new ArrayList<>();

        // 年维度
        FormFieldDTO yearField = new FormFieldDTO();
        yearField.setPosition("C1");

        List<FieldDimDTO> yearDims = new ArrayList<>();
        FieldDimDTO yearDim = new FieldDimDTO();
        yearDim.setDimId(5L);
        yearDim.setDimName("年");

        List<MemberDTO> yearMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(501L)),
                createMemberWithValueIds(Arrays.asList(502L))
        );
        yearDim.setMemberDTOS(yearMembers);
        yearDims.add(yearDim);
        yearField.setMemberDTOS(yearDims);
        group.add(yearField);

        // 期间维度
        FormFieldDTO periodField = new FormFieldDTO();
        periodField.setPosition("C1");

        List<FieldDimDTO> periodDims = new ArrayList<>();
        FieldDimDTO periodDim = new FieldDimDTO();
        periodDim.setDimId(6L);
        periodDim.setDimName("期间");

        List<MemberDTO> periodMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(601L)),
                createMemberWithValueIds(Arrays.asList(602L))
        );
        periodDim.setMemberDTOS(periodMembers);
        periodDims.add(periodDim);
        periodField.setMemberDTOS(periodDims);
        group.add(periodField);

        return group;
    }

    /**
     * 构建C2大列的字段配置:
     * 年维度: 2025(valueId=701), 2026(valueId=702)
     * 期间维度: Q3(valueId=801), Q4(valueId=802)
     */
    private List<FormFieldDTO> buildColGroupC2() {
        List<FormFieldDTO> group = new ArrayList<>();

        // 年维度
        FormFieldDTO yearField = new FormFieldDTO();
        yearField.setPosition("C2");

        List<FieldDimDTO> yearDims = new ArrayList<>();
        FieldDimDTO yearDim = new FieldDimDTO();
        yearDim.setDimId(7L);
        yearDim.setDimName("年");

        List<MemberDTO> yearMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(701L)),
                createMemberWithValueIds(Arrays.asList(702L))
        );
        yearDim.setMemberDTOS(yearMembers);
        yearDims.add(yearDim);
        yearField.setMemberDTOS(yearDims);
        group.add(yearField);

        // 期间维度
        FormFieldDTO periodField = new FormFieldDTO();
        periodField.setPosition("C2");

        List<FieldDimDTO> periodDims = new ArrayList<>();
        FieldDimDTO periodDim = new FieldDimDTO();
        periodDim.setDimId(8L);
        periodDim.setDimName("期间");

        List<MemberDTO> periodMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(801L)),
                createMemberWithValueIds(Arrays.asList(802L))
        );
        periodDim.setMemberDTOS(periodMembers);
        periodDims.add(periodDim);
        periodField.setMemberDTOS(periodDims);
        group.add(periodField);

        return group;
    }

    /**
     * 创建一个MemberDTO，包含指定的valueIds
     */
    private MemberDTO createMemberWithValueIds(List<Long> valueIds) {
        List<MemberValueDTO> memberVals = new ArrayList<>();
        for (Long valueId : valueIds) {
            memberVals.add(new MemberValueDTO(valueId, "value_" + valueId, 0));
        }
        return new MemberDTO(memberVals, null);
    }
}
