package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 验证笛卡尔积顺序的测试 - 二维矩阵版本
 * 大行R1配置产品维度成员P11/P12和版本维度成员V11/V12
 * 大行R2配置产品维度成员P21/P22和版本维度成员V21/V22
 * 大列C1配置年维度成员2023/2024和期间维度成员Q1/Q2
 * 大列C2配置年维度成员2025/2026和期间维度成员Q3/Q4
 *
 * 输出: 8行×8列 = 64格的二维矩阵
 */
public class CartesianBuilderTest {

    public static void main(String[] args) {
        CartesianBuilder builder = new CartesianBuilder();

        // 构建输入数据 - List<List<FormFieldInfoDTO>> 结构
        List<List<FormFieldInfoDTO>> rowFieldInfoList = new ArrayList<>();
        List<List<FormFieldInfoDTO>> colFieldInfoList = new ArrayList<>();

        // ========== 大行配置 ==========
        // R1: 产品{P11,P12} × 版本{V11,V12} -> 2×2=4个小行
        List<FormFieldInfoDTO> r1Dims = new ArrayList<>();
        FormFieldInfoDTO r1Product = new FormFieldInfoDTO();
        r1Product.setDimCode("R1_产品");
        r1Product.setMembers(Arrays.asList(new MemberDTO("P11"), new MemberDTO("P12")));
        r1Dims.add(r1Product);

        FormFieldInfoDTO r1Version = new FormFieldInfoDTO();
        r1Version.setDimCode("R1_版本");
        r1Version.setMembers(Arrays.asList(new MemberDTO("V11"), new MemberDTO("V12")));
        r1Dims.add(r1Version);
        rowFieldInfoList.add(r1Dims);

        // R2: 产品{P21,P22} × 版本{V21,V22} -> 2×2=4个小行
        List<FormFieldInfoDTO> r2Dims = new ArrayList<>();
        FormFieldInfoDTO r2Product = new FormFieldInfoDTO();
        r2Product.setDimCode("R2_产品");
        r2Product.setMembers(Arrays.asList(new MemberDTO("P21"), new MemberDTO("P22")));
        r2Dims.add(r2Product);

        FormFieldInfoDTO r2Version = new FormFieldInfoDTO();
        r2Version.setDimCode("R2_版本");
        r2Version.setMembers(Arrays.asList(new MemberDTO("V21"), new MemberDTO("V22")));
        r2Dims.add(r2Version);
        rowFieldInfoList.add(r2Dims);

        // ========== 大列配置 ==========
        // C1: 年{2023,2024} × 期间{Q1,Q2} -> 2×2=4个小列
        List<FormFieldInfoDTO> c1Dims = new ArrayList<>();
        FormFieldInfoDTO c1Year = new FormFieldInfoDTO();
        c1Year.setDimCode("C1_年");
        c1Year.setMembers(Arrays.asList(new MemberDTO("2023"), new MemberDTO("2024")));
        c1Dims.add(c1Year);

        FormFieldInfoDTO c1Period = new FormFieldInfoDTO();
        c1Period.setDimCode("C1_期间");
        c1Period.setMembers(Arrays.asList(new MemberDTO("Q1"), new MemberDTO("Q2")));
        c1Dims.add(c1Period);
        colFieldInfoList.add(c1Dims);

        // C2: 年{2025,2026} × 期间{Q3,Q4} -> 2×2=4个小列
        List<FormFieldInfoDTO> c2Dims = new ArrayList<>();
        FormFieldInfoDTO c2Year = new FormFieldInfoDTO();
        c2Year.setDimCode("C2_年");
        c2Year.setMembers(Arrays.asList(new MemberDTO("2025"), new MemberDTO("2026")));
        c2Dims.add(c2Year);

        FormFieldInfoDTO c2Period = new FormFieldInfoDTO();
        c2Period.setDimCode("C2_期间");
        c2Period.setMembers(Arrays.asList(new MemberDTO("Q3"), new MemberDTO("Q4")));
        c2Dims.add(c2Period);
        colFieldInfoList.add(c2Dims);

        // 执行笛卡尔积
        List<List<CellDTO>> result = builder.buildCrossProduct(rowFieldInfoList, colFieldInfoList);

        // 输出统计
        int totalRows = builder.getTotalRows(rowFieldInfoList);
        int totalCols = builder.getTotalCols(colFieldInfoList);
        System.out.println("=== 笛卡尔积结果 ===");
        System.out.println("总行数: " + totalRows + " (R1的4行 + R2的4行)");
        System.out.println("总列数: " + totalCols + " (C1的4列 + C2的4列)");
        System.out.println("总单元格数: " + result.size() + "行 × " + (result.isEmpty() ? 0 : result.get(0).size()) + "列 = " + (result.size() * result.get(0).size()));
        System.out.println();

        // 打印二维矩阵表格
        printMatrix(result, totalRows, totalCols);

        // 验证关键位置
        System.out.println("\n=== 关键位置验证 ===");
        verifyKeyPositions(result, totalRows, totalCols);
    }

    private static void printMatrix(List<List<CellDTO>> result, int totalRows, int totalCols) {
        // 打印列头
        System.out.print("         |");
        for (int c = 0; c < totalCols; c++) {
            if (c < 4) {
                System.out.printf("  C1[%d]  ", c);
            } else {
                System.out.printf("  C2[%d]  ", c - 4);
            }
        }
        System.out.println();
        System.out.println("---------|" + "-".repeat(totalCols * 8));

        // 打印每一行
        for (int r = 0; r < result.size(); r++) {
            String rowLabel = (r < 4) ? "R1[" + r + "]" : "R2[" + (r - 4) + "]";
            System.out.printf("%-8s|", rowLabel);

            List<CellDTO> row = result.get(r);
            for (int c = 0; c < row.size(); c++) {
                CellDTO cell = row.get(c);
                String[] ids = cell.getMemberIds();
                // 简化显示: 取每个维度成员ID的前2字符
                String shortId = "";
                if (ids != null && ids.length >= 4) {
                    shortId = ids[0].substring(0, 2) + "/" + ids[1].substring(0, 2) + "/" + ids[2] + "/" + ids[3];
                }
                System.out.printf("%-8s", shortId);
            }
            System.out.println();
        }
    }

    private static void verifyKeyPositions(List<List<CellDTO>> result, int totalRows, int totalCols) {
        System.out.println("验证预期顺序:");

        // 行0列0 -> R1小行1 × C1小列1 = P11/V11/2023/Q1
        CellDTO cell00 = result.get(0).get(0);
        System.out.println("result[0][0] = " + Arrays.toString(cell00.getMemberIds()));
        System.out.println("期望: [P11, V11, 2023, Q1]");

        // 行0列7 -> R1小行1 × C2小列4 = P11/V11/2026/Q4
        CellDTO cell07 = result.get(0).get(7);
        System.out.println("result[0][7] = " + Arrays.toString(cell07.getMemberIds()));
        System.out.println("期望: [P11, V11, 2026, Q4]");

        // 行3列0 -> R1小行4 × C1小列1 = P12/V12/2023/Q1
        CellDTO cell30 = result.get(3).get(0);
        System.out.println("result[3][0] = " + Arrays.toString(cell30.getMemberIds()));
        System.out.println("期望: [P12, V12, 2023, Q1]");

        // 行3列7 -> R1小行4 × C2小列4 = P12/V12/2026/Q4
        CellDTO cell37 = result.get(3).get(7);
        System.out.println("result[3][7] = " + Arrays.toString(cell37.getMemberIds()));
        System.out.println("期望: [P12, V12, 2026, Q4]");

        // 行4列0 -> R2小行1 × C1小列1 = P21/V21/2023/Q1
        CellDTO cell40 = result.get(4).get(0);
        System.out.println("result[4][0] = " + Arrays.toString(cell40.getMemberIds()));
        System.out.println("期望: [P21, V21, 2023, Q1]");

        // 行7列7 -> R2小行4 × C2小列4 = P22/V22/2026/Q4
        CellDTO cell77 = result.get(7).get(7);
        System.out.println("result[7][7] = " + Arrays.toString(cell77.getMemberIds()));
        System.out.println("期望: [P22, V22, 2026, Q4]");
    }
}