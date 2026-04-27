package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;

import java.util.*;

public class CartesianBuilder {

    /**
     * 生成笛卡尔积单元格 - 二维矩阵版本
     *
     * 输入结构说明：
     * - rowFieldInfoList: 外层List按大行顺序排列，内层List为该大行下的维度列表
     * - colFieldInfoList: 外层List按大列顺序排列，内层List为该大列下的维度列表
     *
     * 示例：2个大行(R1,R2)，每个大行2个维度
     * rowFieldInfoList = [
     *   [R1维度1, R1维度2],  // R1展开后的小行数 = 维度1成员数 × 维度2成员数
     *   [R2维度1, R2维度2]   // R2展开后的小行数 = 维度1成员数 × 维度2成员数
     * ]
     *
     * @param rowFieldInfoList 大行配置列表
     * @param colFieldInfoList 大列配置列表
     * @return 二维矩阵 List<List<CellDTO>>，外层是所有小行（按大行展开），内层是所有小列（按大列展开）
     */
    public List<List<CellDTO>> buildCrossProduct(
            List<List<FormFieldInfoDTO>> rowFieldInfoList,
            List<List<FormFieldInfoDTO>> colFieldInfoList) {

        if (rowFieldInfoList == null || rowFieldInfoList.isEmpty() ||
            colFieldInfoList == null || colFieldInfoList.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 生成所有小行的笛卡尔组合（按大行顺序展开）
        List<String[]> allRowCombinations = new ArrayList<>();
        for (List<FormFieldInfoDTO> dims : rowFieldInfoList) {
            List<String[]> combos = generateCartesianFromDims(dims);
            allRowCombinations.addAll(combos);
        }

        // 2. 生成所有小列的笛卡尔组合（按大列顺序展开）
        List<String[]> allColCombinations = new ArrayList<>();
        for (List<FormFieldInfoDTO> dims : colFieldInfoList) {
            List<String[]> combos = generateCartesianFromDims(dims);
            allColCombinations.addAll(combos);
        }

        // 3. 构建二维矩阵 result[rowIndex][colIndex]
        int totalRows = allRowCombinations.size();
        int totalCols = allColCombinations.size();

        List<List<CellDTO>> result = new ArrayList<>(totalRows);
        for (int r = 0; r < totalRows; r++) {
            List<CellDTO> rowCells = new ArrayList<>(totalCols);
            String[] rowCombo = allRowCombinations.get(r);
            for (int c = 0; c < totalCols; c++) {
                String[] colCombo = allColCombinations.get(c);
                String[] allMembers = mergeMembers(rowCombo, colCombo);
                rowCells.add(new CellDTO(allMembers));
            }
            result.add(rowCells);
        }

        return result;
    }

    /**
     * 从维度列表生成笛卡尔积
     * @param dims 单个大行/大列下的维度列表
     * @return 所有可能的成员组合，每项包含各维度的memberId
     */
    private List<String[]> generateCartesianFromDims(List<FormFieldInfoDTO> dims) {
        List<List<String>> dimMembers = new ArrayList<>();
        for (FormFieldInfoDTO dim : dims) {
            List<String> members = new ArrayList<>();
            if (dim.getMembers() != null) {
                for (MemberDTO member : dim.getMembers()) {
                    members.add(member.getMemberId());
                }
            }
            dimMembers.add(members);
        }

        List<String[]> result = new ArrayList<>();
        String[] current = new String[dimMembers.size()];
        cartesianRecursive(result, current, dimMembers, 0);
        return result;
    }

    /**
     * 深度优先递归笛卡尔积，保持配置顺序
     */
    private void cartesianRecursive(List<String[]> result, String[] current,
                                    List<List<String>> dimMemberLists, int depth) {
        if (depth == dimMemberLists.size()) {
            result.add(current.clone());
            return;
        }
        for (String memberId : dimMemberLists.get(depth)) {
            current[depth] = memberId;
            cartesianRecursive(result, current, dimMemberLists, depth + 1);
        }
    }

    /**
     * 合并行成员数组和列成员数组
     */
    private String[] mergeMembers(String[] rowMembers, String[] colMembers) {
        String[] result = Arrays.copyOf(rowMembers, rowMembers.length + colMembers.length);
        System.arraycopy(colMembers, 0, result, rowMembers.length, colMembers.length);
        return result;
    }

    /**
     * 计算总单元格数（估算）
     * 总单元格数 = 所有大行展开的小行数 × 所有大列展开的小列数
     */
    public long estimateCellCount(
            List<List<FormFieldInfoDTO>> rowFieldInfoList,
            List<List<FormFieldInfoDTO>> colFieldInfoList) {

        long totalRows = 0;
        for (List<FormFieldInfoDTO> dims : rowFieldInfoList) {
            totalRows += countCombinationsForDims(dims);
        }

        long totalCols = 0;
        for (List<FormFieldInfoDTO> dims : colFieldInfoList) {
            totalCols += countCombinationsForDims(dims);
        }

        return totalRows * totalCols;
    }

    /**
     * 计算单个大行/大列下所有维度的笛卡尔积组合数
     * 即各维度成员数的乘积
     */
    private long countCombinationsForDims(List<FormFieldInfoDTO> dims) {
        long product = 1;
        for (FormFieldInfoDTO dim : dims) {
            if (dim.getMembers() != null) {
                product *= dim.getMembers().size();
            }
        }
        return product;
    }

    /**
     * 获取总行数（所有大行展开的小行数之和）
     */
    public int getTotalRows(List<List<FormFieldInfoDTO>> rowFieldInfoList) {
        int total = 0;
        for (List<FormFieldInfoDTO> dims : rowFieldInfoList) {
            total += countCombinationsForDims(dims);
        }
        return total;
    }

    /**
     * 获取总列数（所有大列展开的小列数之和）
     */
    public int getTotalCols(List<List<FormFieldInfoDTO>> colFieldInfoList) {
        int total = 0;
        for (List<FormFieldInfoDTO> dims : colFieldInfoList) {
            total += countCombinationsForDims(dims);
        }
        return total;
    }
}