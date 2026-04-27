package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;
import com.form.cartesian.CartesianBuilder;
import com.form.cartesian.CartesianBuilderV2;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能基准测试：100万单元格场景
 */
public class PerformanceBenchmark {

    public static void main(String[] args) {
        System.out.println("=== 性能基准测试：100万单元格 ===\n");

        // 测试配置
        int rowCount = 1000;
        int colCount = 1000;
        int dimCount = 4; // 每个大行/大列4个维度
        int membersPerDim = 10; // 每个维度10个成员

        // 构建测试数据
        List<List<FormFieldInfoDTO>> rowFieldInfoList = buildTestData("R", 1, dimCount, membersPerDim, rowCount);
        List<List<FormFieldInfoDTO>> colFieldInfoList = buildTestData("C", 1, dimCount, membersPerDim, colCount);

        long totalCells = (long) rowCount * colCount;
        System.out.println("测试配置：");
        System.out.println("  总行数: " + rowCount);
        System.out.println("  总列数: " + colCount);
        System.out.println("  总单元格: " + totalCells);
        System.out.println("  每组合维度: " + dimCount * 2);
        System.out.println();

        // ===== 测试 V1 =====
        System.out.println("【V1: CartesianBuilder】");
        System.gc();
        long v1StartMem = getUsedMemory();

        long v1StartTime = System.currentTimeMillis();
        CartesianBuilder v1 = new CartesianBuilder();
        List<List<CellDTO>> v1Result = v1.buildCrossProduct(rowFieldInfoList, colFieldInfoList);
        long v1EndTime = System.currentTimeMillis();

        long v1EndMem = getUsedMemory();
        long v1MemUsed = v1EndMem - v1StartMem;

        System.out.println("  构建时间: " + (v1EndTime - v1StartTime) + " ms");
        System.out.println("  内存占用: " + bytesToMB(v1MemUsed) + " MB");
        System.out.println("  结果验证: " + (v1Result.size() == rowCount && v1Result.get(0).size() == colCount ? "✅" : "❌"));
        System.out.println();

        // ===== 测试 V2 =====
        System.out.println("【V2: CartesianBuilderV2】");
        System.gc();
        long v2StartMem = getUsedMemory();

        long v2StartTime = System.currentTimeMillis();
        CartesianBuilderV2 v2 = new CartesianBuilderV2();
        List<CartesianBuilderV2.CellIndex> v2Result = v2.buildCrossProduct(rowFieldInfoList, colFieldInfoList);
        long v2EndTime = System.currentTimeMillis();

        long v2EndMem = getUsedMemory();
        long v2MemUsed = v2EndMem - v2StartMem;

        System.out.println("  构建时间: " + (v2EndTime - v2StartTime) + " ms");
        System.out.println("  内存占用: " + bytesToMB(v2MemUsed) + " MB");
        System.out.println("  结果验证: " + (v2Result.size() == rowCount && v2Result.get(0).size() == colCount ? "✅" : "❌"));
        System.out.println();

        // ===== 对比 =====
        System.out.println("【性能对比】");
        System.out.println("  时间提升: " + ((double)(v1EndTime - v1StartTime) / (v2EndTime - v2StartTime)) + "x");
        System.out.println("  内存节省: " + ((1 - (double)v2MemUsed / v1MemUsed) * 100) + "%");
    }

    /**
     * 构建测试数据
     */
    private static List<List<FormFieldInfoDTO>> buildTestData(String prefix, int groupCount,
                                                                int dimCount, int membersPerDim, int targetTotal) {
        List<List<FormFieldInfoDTO>> result = new ArrayList<>();

        // 计算每个大行/大列需要展开的小行/小列数
        int combosPerGroup = (int) Math.pow(membersPerDim, dimCount);
        int groupsNeeded = (targetTotal + combosPerGroup - 1) / combosPerGroup;

        for (int g = 1; g <= groupsNeeded; g++) {
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
     * 获取当前JVM已用内存（MB）
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
