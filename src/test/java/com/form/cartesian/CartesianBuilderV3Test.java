package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;
import com.form.cartesian.CartesianBuilder;
import com.form.cartesian.CartesianBuilderV2;
import com.form.cartesian.CartesianBuilderV3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * V3 性能对比测试
 * 对比 V1、V2、V3 在不同规模下的性能和内存表现
 */
public class CartesianBuilderV3Test {

    public static void main(String[] args) {
        System.out.println("=== 笛卡尔积构建器 V3 性能对比测试 ===\n");

        // 测试场景配置
        testAllVersions(100, 100, "小规模 (10K单元格)");
        System.out.println("\n" + "=".repeat(80) + "\n");

        testAllVersions(500, 500, "中规模 (250K单元格)");
        System.out.println("\n" + "=".repeat(80) + "\n");

        testAllVersions(1000, 1000, "大规模 (1M单元格)");
        System.out.println("\n" + "=".repeat(80) + "\n");

        // 验证顺序正确性
        testOrderCorrectness();
    }

    /**
     * 测试所有版本
     */
    private static void testAllVersions(int rowCount, int colCount, String scenario) {
        System.out.println("【" + scenario + "】");
        System.out.println("配置: " + rowCount + "行 × " + colCount + "列 = " +
                          (rowCount * colCount) + " 单元格");

        // 构建测试数据
        int dimCount = 4;
        int membersPerDim = (int) Math.pow(rowCount, 1.0 / dimCount) + 1;
        List<List<FormFieldInfoDTO>> rowFields =
            buildTestData("R", 1, dimCount, membersPerDim, rowCount);
        List<List<FormFieldInfoDTO>> colFields =
            buildTestData("C", 1, dimCount, membersPerDim, colCount);

        long expectedCells = (long) rowCount * colCount;

        // ===== V1 测试 =====
        System.out.println("\n【V1: CartesianBuilder】");
        long v1Time = testV1(rowFields, colFields, expectedCells);

        // ===== V2 测试 =====
        System.out.println("\n【V2: CartesianBuilderV2】");
        long v2Time = testV2(rowFields, colFields, expectedCells);

        // ===== V3 测试 =====
        System.out.println("\n【V3: CartesianBuilderV3】");
        long v3Time = testV3(rowFields, colFields, expectedCells);

        // ===== 性能对比 =====
        System.out.println("\n【性能对比】");
        System.out.printf("  V1: %d ms\n", v1Time);
        System.out.printf("  V2: %d ms (提升 %.2fx)\n", v2Time, (double) v1Time / v2Time);
        System.out.printf("  V3: %d ms (相比V1提升 %.2fx, 相比V2提升 %.2fx)\n",
                          v3Time, (double) v1Time / v3Time, (double) v2Time / v3Time);
    }

    /**
     * 测试 V1
     */
    private static long testV1(List<List<FormFieldInfoDTO>> rowFields,
                              List<List<FormFieldInfoDTO>> colFields,
                              long expectedCells) {
        System.gc();
        long startMem = getUsedMemory();
        long startTime = System.currentTimeMillis();

        CartesianBuilder builder = new CartesianBuilder();
        List<List<CellDTO>> result = builder.buildCrossProduct(rowFields, colFields);

        long endTime = System.currentTimeMillis();
        long endMem = getUsedMemory();

        long actualCells = result.size() * (result.isEmpty() ? 0 : result.get(0).size());
        System.out.println("  结果: " + actualCells + " 单元格 " +
                          (actualCells == expectedCells ? "✅" : "❌"));
        System.out.println("  内存: " + bytesToMB(endMem - startMem) + " MB");
        return endTime - startTime;
    }

    /**
     * 测试 V2
     */
    private static long testV2(List<List<FormFieldInfoDTO>> rowFields,
                              List<List<FormFieldInfoDTO>> colFields,
                              long expectedCells) {
        System.gc();
        long startMem = getUsedMemory();
        long startTime = System.currentTimeMillis();

        CartesianBuilderV2 builder = new CartesianBuilderV2();
        List<List<CartesianBuilderV2.CellIndex>> result =
            builder.buildCrossProduct(rowFields, colFields);

        long endTime = System.currentTimeMillis();
        long endMem = getUsedMemory();

        long actualCells = result.size() * (result.isEmpty() ? 0 : result.get(0).size());
        System.out.println("  结果: " + actualCells + " 单元格 " +
                          (actualCells == expectedCells ? "✅" : "❌"));
        System.out.println("  内存: " + bytesToMB(endMem - startMem) + " MB");
        return endTime - startTime;
    }

    /**
     * 测试 V3
     */
    private static long testV3(List<List<FormFieldInfoDTO>> rowFields,
                              List<List<FormFieldInfoDTO>> colFields,
                              long expectedCells) {
        System.gc();
        long startMem = getUsedMemory();
        long startTime = System.currentTimeMillis();

        CartesianBuilderV3 builder = new CartesianBuilderV3();
        CartesianBuilderV3.CartesianResult result =
            builder.buildCrossProduct(rowFields, colFields);

        long endTime = System.currentTimeMillis();
        long endMem = getUsedMemory();

        long actualCells = result.getTotalCells();
        System.out.println("  结果: " + actualCells + " 单元格 " +
                          (actualCells == expectedCells ? "✅" : "❌"));
        System.out.println("  内存: " + bytesToMB(endMem - startMem) + " MB");
        System.out.println("  存储: 扁平化long数组 (" + result.getFlatCells().length + " 个long)");
        System.out.println("  唯一成员数: " + result.getAllMemberIds().length);

        // 测试批量获取
        long batchStart = System.currentTimeMillis();
        String[][] batch = builder.getCellMemberIdsBatch(
            result.getFlatCells(),
            result.getRowMemberIds(),
            result.getColMemberIds(),
            result.getTotalCols(),
            Math.min(1000, (int) result.getTotalCells())
        );
        long batchTime = System.currentTimeMillis() - batchStart;
        System.out.println("  批量获取1000个单元格: " + batchTime + " ms");

        return endTime - startTime;
    }

    /**
     * 验证顺序正确性
     */
    private static void testOrderCorrectness() {
        System.out.println("【顺序正确性验证】\n");

        // 使用文档中的示例数据
        List<List<FormFieldInfoDTO>> rowFields = new ArrayList<>();
        List<List<FormFieldInfoDTO>> colFields = new ArrayList<>();

        // R1: 产品{P11,P12} × 版本{V11,V12}
        List<FormFieldInfoDTO> r1Dims = new ArrayList<>();
        FormFieldInfoDTO r1Product = new FormFieldInfoDTO();
        r1Product.setDimCode("R1_产品");
        r1Product.setMembers(Arrays.asList(
            new MemberDTO("P11"), new MemberDTO("P12")));
        r1Dims.add(r1Product);

        FormFieldInfoDTO r1Version = new FormFieldInfoDTO();
        r1Version.setDimCode("R1_版本");
        r1Version.setMembers(Arrays.asList(
            new MemberDTO("V11"), new MemberDTO("V12")));
        r1Dims.add(r1Version);
        rowFields.add(r1Dims);

        // R2: 产品{P21,P22} × 版本{V21,V22}
        List<FormFieldInfoDTO> r2Dims = new ArrayList<>();
        FormFieldInfoDTO r2Product = new FormFieldInfoDTO();
        r2Product.setDimCode("R2_产品");
        r2Product.setMembers(Arrays.asList(
            new MemberDTO("P21"), new MemberDTO("P22")));
        r2Dims.add(r2Product);

        FormFieldInfoDTO r2Version = new FormFieldInfoDTO();
        r2Version.setDimCode("R2_版本");
        r2Version.setMembers(Arrays.asList(
            new MemberDTO("V21"), new MemberDTO("V22")));
        r2Dims.add(r2Version);
        rowFields.add(r2Dims);

        // C1: 年{2023,2024} × 期间{Q1,Q2}
        List<FormFieldInfoDTO> c1Dims = new ArrayList<>();
        FormFieldInfoDTO c1Year = new FormFieldInfoDTO();
        c1Year.setDimCode("C1_年");
        c1Year.setMembers(Arrays.asList(
            new MemberDTO("2023"), new MemberDTO("2024")));
        c1Dims.add(c1Year);

        FormFieldInfoDTO c1Period = new FormFieldInfoDTO();
        c1Period.setDimCode("C1_期间");
        c1Period.setMembers(Arrays.asList(
            new MemberDTO("Q1"), new MemberDTO("Q2")));
        c1Dims.add(c1Period);
        colFields.add(c1Dims);

        // C2: 年{2025,2026} × 期间{Q3,Q4}
        List<FormFieldInfoDTO> c2Dims = new ArrayList<>();
        FormFieldInfoDTO c2Year = new FormFieldInfoDTO();
        c2Year.setDimCode("C2_年");
        c2Year.setMembers(Arrays.asList(
            new MemberDTO("2025"), new MemberDTO("2026")));
        c2Dims.add(c2Year);

        FormFieldInfoDTO c2Period = new FormFieldInfoDTO();
        c2Period.setDimCode("C2_期间");
        c2Period.setMembers(Arrays.asList(
            new MemberDTO("Q3"), new MemberDTO("Q4")));
        c2Dims.add(c2Period);
        colFields.add(c2Dims);

        CartesianBuilderV3 builder = new CartesianBuilderV3();
        CartesianBuilderV3.CartesianResult result =
            builder.buildCrossProduct(rowFields, colFields);

        // 验证关键位置
        System.out.println("验证关键位置:");
        verifyPosition(result, 0, 0, "[P11, V11, 2023, Q1]");
        verifyPosition(result, 0, 7, "[P11, V11, 2026, Q4]");
        verifyPosition(result, 3, 0, "[P12, V12, 2023, Q1]");
        verifyPosition(result, 3, 7, "[P12, V12, 2026, Q4]");
        verifyPosition(result, 4, 0, "[P21, V21, 2023, Q1]");
        verifyPosition(result, 7, 7, "[P22, V22, 2026, Q4]");
    }

    /**
     * 验证指定位置的单元格
     */
    private static void verifyPosition(CartesianBuilderV3.CartesianResult result,
                                       int row, int col, String expected) {
        String[] actual = result.getCellMemberIds(row, col);
        String actualStr = Arrays.toString(actual);
        boolean matches = expected.equals(actualStr);
        System.out.printf("  [%d][%d] = %s %s\n", row, col, actualStr,
                          matches ? "✅" : "❌ 期望: " + expected);
    }

    /**
     * 构建测试数据
     */
    private static List<List<FormFieldInfoDTO>> buildTestData(String prefix,
                                                              int startGroup,
                                                              int dimCount,
                                                              int membersPerDim,
                                                              int targetTotal) {
        List<List<FormFieldInfoDTO>> result = new ArrayList<>();

        int combosPerGroup = (int) Math.pow(membersPerDim, dimCount);
        int groupsNeeded = (targetTotal + combosPerGroup - 1) / combosPerGroup;

        for (int g = startGroup; g < startGroup + groupsNeeded; g++) {
            List<FormFieldInfoDTO> dims = new ArrayList<>();
            for (int d = 1; d <= dimCount; d++) {
                FormFieldInfoDTO dim = new FormFieldInfoDTO();
                dim.setDimCode(prefix + g + "_Dim" + d);
                dim.setDimId(prefix + g + "D" + d);
                dim.setDimCnName("维度" + d);
                dim.setDimEnName("Dim" + d);

                List<MemberDTO> members = new ArrayList<>();
                for (int m = 1; m <= membersPerDim; m++) {
                    MemberDTO member = new MemberDTO();
                    member.setMemberId(prefix + g + "D" + d + "M" + m);
                    member.setMemberCnName("成员" + m);
                    member.setMemberEnName("Member" + m);
                    members.add(member);
                }
                dim.setMembers(members);
                dims.add(dim);
            }
            result.add(dims);
        }

        return result;
    }

    /**
     * 获取当前JVM已用内存
     */
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * 字节转MB
     */
    private static double bytesToMB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }
}
