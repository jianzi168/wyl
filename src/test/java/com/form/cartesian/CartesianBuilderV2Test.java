package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;

import java.util.*;

public class CartesianBuilderV2Test {
    public static void main(String[] args) {
        CartesianBuilderV2 builder = new CartesianBuilderV2();

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

        // 测试 V2 版本
        String[][] rowCombos = builder.getRowCombinations(rowFieldInfoList);
        String[][] colCombos = builder.getColCombinations(colFieldInfoList);

        System.out.println("=== CartesianBuilderV2 测试 ===");
        System.out.println("行组合数: " + rowCombos.length);
        System.out.println("列组合数: " + colCombos.length);
        System.out.println("预估单元格数: " + builder.estimateCellCount(rowFieldInfoList, colFieldInfoList));
        System.out.println();

        // 构建矩阵
        List<List<CartesianBuilderV2.CellIndex>> result = builder.buildCrossProduct(rowFieldInfoList, colFieldInfoList);
        System.out.println("矩阵大小: " + result.size() + "行 × " + result.get(0).size() + "列");

        // 验证关键位置
        System.out.println("\n=== 关键位置验证 ===");
        verifyKeyPositions(result, rowCombos, colCombos);
    }

    private static void verifyKeyPositions(List<List<CartesianBuilderV2.CellIndex>> result,
                                          String[][] rowCombos, String[][] colCombos) {
        CartesianBuilderV2 builder = new CartesianBuilderV2();

        // 位置 [0][0]
        CartesianBuilderV2.CellIndex cell00 = result.get(0).get(0);
        System.out.println("result[0][0] -> rowIdx=" + cell00.rowIndex() + ", colIdx=" + cell00.colIndex());
        System.out.println("  成员: " + Arrays.toString(builder.getCellMemberIds(cell00, rowCombos, colCombos)));
        System.out.println("  期望: [P11, V11, 2023, Q1]");

        // 位置 [0][7]
        CartesianBuilderV2.CellIndex cell07 = result.get(0).get(7);
        System.out.println("result[0][7] -> rowIdx=" + cell07.rowIndex() + ", colIdx=" + cell07.colIndex());
        System.out.println("  成员: " + Arrays.toString(builder.getCellMemberIds(cell07, rowCombos, colCombos)));
        System.out.println("  期望: [P11, V11, 2026, Q4]");

        // 位置 [3][0]
        CartesianBuilderV2.CellIndex cell30 = result.get(3).get(0);
        System.out.println("result[3][0] -> rowIdx=" + cell30.rowIndex() + ", colIdx=" + cell30.colIndex());
        System.out.println("  成员: " + Arrays.toString(builder.getCellMemberIds(cell30, rowCombos, colCombos)));
        System.out.println("  期望: [P12, V12, 2023, Q1]");

        // 位置 [7][7]
        CartesianBuilderV2.CellIndex cell77 = result.get(7).get(7);
        System.out.println("result[7][7] -> rowIdx=" + cell77.rowIndex() + ", colIdx=" + cell77.colIndex());
        System.out.println("  成员: " + Arrays.toString(builder.getCellMemberIds(cell77, rowCombos, colCombos)));
        System.out.println("  期望: [P22, V22, 2026, Q4]");
    }
}
