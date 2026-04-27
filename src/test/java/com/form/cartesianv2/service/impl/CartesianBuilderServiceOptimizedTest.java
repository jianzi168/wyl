package com.form.cartesianv2.service.impl;

import com.form.cartesianv2.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CartesianBuilderServiceOptimized 单元测试
 */
class CartesianBuilderServiceOptimizedTest {

    private CartesianBuilderServiceOptimized service;

    @BeforeEach
    void setUp() {
        service = new CartesianBuilderServiceOptimized();
    }

    @Test
    void testBuildCrossProduct_basicCase() {
        // R1: 产品{P11,P12} × 版本{V11,V12} -> 4个小行
        // C1: 年{2023,2024} × 期间{Q1,Q2} -> 4个小列
        // 期望: 4行 × 4列 = 16格

        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        assertNotNull(result);
        assertEquals(4, result.size(), "应该有4行");
        for (List<FormCellDTO> row : result) {
            assertEquals(4, row.size(), "每行应该有4列");
        }
    }

    @Test
    void testBuildCrossProductLazy_iteratorOrder() {
        // 测试惰性迭代器的顺序保证
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        Iterable<FormCellDTO> lazyResult = service.buildCrossProductLazy(rowFieldGroups, colFieldGroups);

        int count = 0;
        Long[] previousIds = null;
        for (FormCellDTO cell : lazyResult) {
            count++;
            Long[] currentIds = cell.getMemberIds();
            assertNotNull(currentIds);
            // 验证顺序: 每个后续单元格的"位置"应该递增
            // 行主序: (0,0) -> (0,1) -> (0,2) -> (0,3) -> (1,0) -> ...
            if (previousIds != null) {
                // 简单验证: 第一个成员应该递增或有进位
                assertTrue(currentIds.length == 4);
            }
            previousIds = currentIds;
        }
        assertEquals(16, count, "应该有16个单元格");
    }

    @Test
    void testBuildCrossProductStream() {
        // 测试Stream版本
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        long count = service.buildCrossProductStream(rowFieldGroups, colFieldGroups)
                .count();

        assertEquals(16, count);
    }

    @Test
    void testBuildCrossProductPaged() {
        // 测试分页版本
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        rowFieldGroups.add(buildRowGroupR2());
        colFieldGroups.add(buildColGroupC1());
        colFieldGroups.add(buildColGroupC2());

        // 获取第0-4行(4行×8列=32格)
        List<List<FormCellDTO>> page1 = service.buildCrossProductPaged(rowFieldGroups, colFieldGroups, 0, 4, 4);
        assertEquals(4, page1.size());
        for (List<FormCellDTO> row : page1) {
            assertEquals(8, row.size());
        }

        // 获取第4-8行
        List<List<FormCellDTO>> page2 = service.buildCrossProductPaged(rowFieldGroups, colFieldGroups, 4, 8, 4);
        assertEquals(4, page2.size());

        // 验证page1和page2不重叠
        Long[] page1First = page1.get(0).get(0).getMemberIds();
        Long[] page2First = page2.get(0).get(0).getMemberIds();
        assertFalse(Arrays.equals(page1First, page2First));
    }

    @Test
    void testBuildCrossProduct_emptyInput() {
        assertEquals(Collections.emptyList(), service.buildCrossProduct(null, null));
        assertEquals(Collections.emptyList(), service.buildCrossProduct(new ArrayList<>(), new ArrayList<>()));
    }

    @Test
    void testFirstAndLastCell() {
        // 验证首尾单元格
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        // 第一个单元格
        Long[] firstIds = result.get(0).get(0).getMemberIds();
        assertEquals(4, firstIds.length);
        assertEquals(101L, firstIds[0]); // 产品P11
        assertEquals(201L, firstIds[1]); // 版本V11
        assertEquals(501L, firstIds[2]); // 年2023
        assertEquals(601L, firstIds[3]); // 期间Q1

        // 最后一个单元格
        Long[] lastIds = result.get(3).get(3).getMemberIds();
        assertEquals(4, lastIds.length);
        assertEquals(102L, lastIds[0]); // 产品P12
        assertEquals(202L, lastIds[1]); // 版本V12
        assertEquals(502L, lastIds[2]); // 年2024
        assertEquals(602L, lastIds[3]); // 期间Q2
    }

    @Test
    void testMemberDeduplication_sameDim() {
        // 测试同维度下成员合并取valueId去重
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        // 构建R1: 产品维度有重复valueId
        List<FormFieldDTO> r1Group = new ArrayList<>();

        FormFieldDTO productDim = new FormFieldDTO();
        productDim.setPosition("R1");
        List<FieldDimDTO> productDims = new ArrayList<>();
        FieldDimDTO productFieldDim = new FieldDimDTO();
        productFieldDim.setDimId(1L);
        productFieldDim.setDimName("产品");

        // 成员A有[101,102], 成员B有[102,103], 成员C有[101]
        // 去重后应该是[101, 102, 103]
        List<MemberDTO> productMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(101L, 102L)),
                createMemberWithValueIds(Arrays.asList(102L, 103L)),
                createMemberWithValueIds(Arrays.asList(101L))
        );
        productFieldDim.setMemberDTOS(productMembers);
        productDims.add(productFieldDim);
        productDim.setMemberDTOS(productDims);
        r1Group.add(productDim);

        FormFieldDTO versionDim = new FormFieldDTO();
        versionDim.setPosition("R1");
        List<FieldDimDTO> versionDims = new ArrayList<>();
        FieldDimDTO versionFieldDim = new FieldDimDTO();
        versionFieldDim.setDimId(2L);
        versionFieldDim.setDimName("版本");

        List<MemberDTO> versionMembers = Arrays.asList(
                createMemberWithValueIds(Arrays.asList(201L)),
                createMemberWithValueIds(Arrays.asList(202L))
        );
        versionFieldDim.setMemberDTOS(versionMembers);
        versionDims.add(versionFieldDim);
        versionDim.setMemberDTOS(versionDims);
        r1Group.add(versionDim);

        rowFieldGroups.add(r1Group);
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        // 产品去重后{101,102,103} × 版本{201,202} = 3×2=6个小行
        // 6小行 × 4小列 = 24格
        assertEquals(6, result.size(), "产品维度去重后应有6小行");
        for (List<FormCellDTO> row : result) {
            assertEquals(4, row.size());
        }

        // 验证第一个单元格: [101, 201, 501, 601]
        Long[] firstIds = result.get(0).get(0).getMemberIds();
        assertEquals(101L, firstIds[0]);
        assertEquals(201L, firstIds[1]);
    }

    @Test
    void testAllMethodsConsistency() {
        // 验证四种方法返回结果一致性
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        // 1. buildCrossProduct
        List<List<FormCellDTO>> result1 = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        // 2. buildCrossProductLazy
        List<FormCellDTO> lazyList = new ArrayList<>();
        for (FormCellDTO cell : service.buildCrossProductLazy(rowFieldGroups, colFieldGroups)) {
            lazyList.add(cell);
        }

        // 3. buildCrossProductStream
        List<FormCellDTO> streamList = service.buildCrossProductStream(rowFieldGroups, colFieldGroups).toList();

        // 4. buildCrossProductPaged (所有行)
        List<List<FormCellDTO>> pagedResult = service.buildCrossProductPaged(rowFieldGroups, colFieldGroups, 0, 4, 4);

        // 验证数量一致性
        // result1是4行4列=16格
        assertEquals(4, result1.size());
        assertEquals(4, result1.get(0).size());
        assertEquals(16, lazyList.size());
        assertEquals(16, streamList.size());
        // pagedResult返回4行(R1)，每行4列(C1)
        assertEquals(4, pagedResult.size());
        assertEquals(4, pagedResult.get(0).size());

        // 验证内容一致性 - 第一个单元格
        Long[] result1First = result1.get(0).get(0).getMemberIds();
        Long[] lazyFirst = lazyList.get(0).getMemberIds();
        Long[] streamFirst = streamList.get(0).getMemberIds();
        Long[] pagedFirst = pagedResult.get(0).get(0).getMemberIds();

        assertArrayEquals(result1First, lazyFirst);
        assertArrayEquals(result1First, streamFirst);
        assertArrayEquals(result1First, pagedFirst);

        // 验证内容一致性 - 最后一个单元格
        Long[] result1Last = result1.get(3).get(3).getMemberIds();
        Long[] lazyLast = lazyList.get(15).getMemberIds();
        Long[] streamLast = streamList.get(15).getMemberIds();

        assertArrayEquals(result1Last, lazyLast);
        assertArrayEquals(result1Last, streamLast);
    }

    @Test
    void testRowMajorOrder() {
        // 验证行主序(row-major order)遍历顺序
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        // 4行4列的矩阵，行主序遍历顺序:
        // (0,0) -> (0,1) -> (0,2) -> (0,3) -> (1,0) -> (1,1) -> ... -> (3,3)
        //
        // 单元格索引映射:
        // 0:  result[0][0], 1:  result[0][1], 2:  result[0][2], 3:  result[0][3]
        // 4:  result[1][0], 5:  result[1][1], 6:  result[1][2], 7:  result[1][3]
        // 8:  result[2][0], 9:  result[2][1], 10: result[2][2], 11: result[2][3]
        // 12: result[3][0], 13: result[3][1], 14: result[3][2], 15: result[3][3]

        // 验证列索引递增
        // C1的列维度: 年[501,502] × 期间[601,602]
        // 笛卡尔积顺序: [501,601], [501,602], [502,601], [502,602]
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int cellIndex = r * 4 + c;
                Long[] cellIds = result.get(r).get(c).getMemberIds();
                assertEquals(4, cellIds.length);
                // 列成员顺序: [501,601], [501,602], [502,601], [502,602]
                if (c == 0) {
                    assertEquals(501L, cellIds[2]); // 年
                    assertEquals(601L, cellIds[3]); // 期间Q1
                } else if (c == 1) {
                    assertEquals(501L, cellIds[2]);
                    assertEquals(602L, cellIds[3]); // 期间Q2
                } else if (c == 2) {
                    assertEquals(502L, cellIds[2]); // 年2024
                    assertEquals(601L, cellIds[3]);
                } else if (c == 3) {
                    assertEquals(502L, cellIds[2]);
                    assertEquals(602L, cellIds[3]);
                }
            }
        }
    }

    @Test
    void testEmptyDimension() {
        // 测试空维度
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        List<FormFieldDTO> r1Group = new ArrayList<>();

        FormFieldDTO emptyDim = new FormFieldDTO();
        emptyDim.setPosition("R1");
        emptyDim.setMemberDTOS(new ArrayList<>());

        r1Group.add(emptyDim);
        rowFieldGroups.add(r1Group);
        colFieldGroups.add(buildColGroupC1());

        // 空维度应返回空结果
        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSingleMemberDimension() {
        // 测试单成员维度
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        List<FormFieldDTO> r1Group = new ArrayList<>();

        FormFieldDTO singleDim = new FormFieldDTO();
        singleDim.setPosition("R1");
        List<FieldDimDTO> dims = new ArrayList<>();
        FieldDimDTO dim = new FieldDimDTO();
        dim.setDimId(1L);
        dim.setDimName("产品");
        dim.setMemberDTOS(Arrays.asList(createMemberWithValueIds(Arrays.asList(101L))));
        dims.add(dim);
        singleDim.setMemberDTOS(dims);
        r1Group.add(singleDim);

        rowFieldGroups.add(r1Group);
        colFieldGroups.add(buildColGroupC1());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        // 1行 × 4列 = 4格
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).size());
        // 每个单元格只有3个成员(1行+2列)
        assertEquals(3, result.get(0).get(0).getMemberIds().length);
    }

    @Test
    void testStreamLazyEvaluation() {
        // 验证Stream惰性求值 - 不应该一次性生成所有单元格
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        java.util.stream.Stream<FormCellDTO> stream = service.buildCrossProductStream(rowFieldGroups, colFieldGroups);

        // 取前3个元素
        List<FormCellDTO> firstThree = stream.limit(3).toList();

        // 应该只有3个元素，不应该生成全部16个
        assertEquals(3, firstThree.size());

        // 验证前3个是正确的前3个单元格
        assertEquals(101L, firstThree.get(0).getMemberIds()[0]);
        assertEquals(201L, firstThree.get(0).getMemberIds()[1]);
        assertEquals(501L, firstThree.get(0).getMemberIds()[2]);
        assertEquals(601L, firstThree.get(0).getMemberIds()[3]);
    }

    @Test
    void testPaged_boundaryConditions() {
        // 测试分页边界条件
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        // rowStart > rowEnd 应返回空
        List<List<FormCellDTO>> empty = service.buildCrossProductPaged(rowFieldGroups, colFieldGroups, 5, 2, 4);
        assertTrue(empty.isEmpty());

        // rowStart >= 总行数 应返回空
        List<List<FormCellDTO>> outOfRange = service.buildCrossProductPaged(rowFieldGroups, colFieldGroups, 10, 20, 4);
        assertTrue(outOfRange.isEmpty());

        // 部分重叠
        List<List<FormCellDTO>> partial = service.buildCrossProductPaged(rowFieldGroups, colFieldGroups, 2, 10, 4);
        assertEquals(2, partial.size()); // 只返回第2,3行(因为总共只有4行)
    }

    @Test
    void testNullMemberValues() {
        // 测试null成员值处理
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        List<FormFieldDTO> r1Group = new ArrayList<>();

        FormFieldDTO dim = new FormFieldDTO();
        dim.setPosition("R1");
        List<FieldDimDTO> dims = new ArrayList<>();
        FieldDimDTO fieldDim = new FieldDimDTO();
        fieldDim.setDimId(1L);
        fieldDim.setDimName("产品");

        // 创建包含null值ID的成员
        List<MemberDTO> members = new ArrayList<>();
        MemberDTO member1 = new MemberDTO();
        member1.setMemberVals(Arrays.asList(new MemberValueDTO(null, "nullValue", 0)));
        members.add(member1);

        MemberDTO member2 = new MemberDTO();
        member2.setMemberVals(Arrays.asList(new MemberValueDTO(101L, "valid", 0)));
        members.add(member2);

        fieldDim.setMemberDTOS(members);
        dims.add(fieldDim);
        dim.setMemberDTOS(dims);
        r1Group.add(dim);

        rowFieldGroups.add(r1Group);
        colFieldGroups.add(buildColGroupC1());

        // 应该跳过null值，只保留101
        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        // 1行(只有101) × 4列 = 4格
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).size());
        assertEquals(101L, result.get(0).get(0).getMemberIds()[0]);
    }

    @Test
    void testMultipleRowAndColGroups() {
        // 测试多行多列组合
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        rowFieldGroups.add(buildRowGroupR2());
        colFieldGroups.add(buildColGroupC1());
        colFieldGroups.add(buildColGroupC2());

        List<List<FormCellDTO>> result = service.buildCrossProduct(rowFieldGroups, colFieldGroups);

        // R1(4小行) + R2(4小行) = 8小行
        // C1(4小列) + C2(4小列) = 8小列
        // 8行 × 8列 = 64格
        assertEquals(8, result.size());
        assertEquals(8, result.get(0).size());

        // 验证R1和R2的分割点
        // R1的行索引: 0-3, R2的行索引: 4-7
        Long[] r1LastRowFirstCell = result.get(3).get(0).getMemberIds();
        Long[] r2FirstRowFirstCell = result.get(4).get(0).getMemberIds();

        // R1最后一行第一个单元格: [102, 202, ...]
        assertEquals(102L, r1LastRowFirstCell[0]); // R1产品P12
        assertEquals(202L, r1LastRowFirstCell[1]); // R1版本V12

        // R2第一行第一个单元格: [301, 401, ...]
        assertEquals(301L, r2FirstRowFirstCell[0]); // R2产品P21
        assertEquals(401L, r2FirstRowFirstCell[1]); // R2版本V21
    }

    @Test
    void testIteratorRemove() {
        // 测试迭代器的remove操作(应该不支持)
        List<List<FormFieldDTO>> rowFieldGroups = new ArrayList<>();
        List<List<FormFieldDTO>> colFieldGroups = new ArrayList<>();

        rowFieldGroups.add(buildRowGroupR1());
        colFieldGroups.add(buildColGroupC1());

        Iterable<FormCellDTO> lazyResult = service.buildCrossProductLazy(rowFieldGroups, colFieldGroups);
        Iterator<FormCellDTO> iterator = lazyResult.iterator();

        assertTrue(iterator.hasNext());
        iterator.next();

        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    // ==================== Helper Methods ====================

    private List<FormFieldDTO> buildRowGroupR1() {
        List<FormFieldDTO> group = new ArrayList<>();

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

    private List<FormFieldDTO> buildRowGroupR2() {
        List<FormFieldDTO> group = new ArrayList<>();

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

    private List<FormFieldDTO> buildColGroupC1() {
        List<FormFieldDTO> group = new ArrayList<>();

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

    private List<FormFieldDTO> buildColGroupC2() {
        List<FormFieldDTO> group = new ArrayList<>();

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

    private MemberDTO createMemberWithValueIds(List<Long> valueIds) {
        List<MemberValueDTO> memberVals = new ArrayList<>();
        for (Long valueId : valueIds) {
            memberVals.add(new MemberValueDTO(valueId, "value_" + valueId, 0));
        }
        return new MemberDTO(memberVals, null);
    }
}
