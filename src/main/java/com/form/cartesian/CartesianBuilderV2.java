package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;

import java.util.*;

/**
 * 笛卡尔积构建器 - 性能优化版本（JDK 21）
 *
 * 优化策略：
 * 1. CellDTO 存储行/列索引而非完整数组，避免每格重复分配
 * 2. 预生成所有行/列组合，单元格仅引用索引
 * 3. 迭代式笛卡尔积生成，避免递归栈溢出
 * 4. 使用容量预分配的ArrayList
 * 5. 采用record类型存储不变数据，减少对象头开销
 */
public class CartesianBuilderV2 {

    /**
     * 生成笛卡尔积单元格 - 二维矩阵版本
     *
     * @param rowFieldInfoList 大行配置列表（外层按大行顺序排列，内层为该大行下的维度列表）
     * @param colFieldInfoList 大列配置列表（外层按大列顺序排列，内层为该大列下的维度列表）
     * @return 二维矩阵，外层是所有小行，内层是所有小列
     */
    public List<List<CellIndex>> buildCrossProduct(
            List<List<FormFieldInfoDTO>> rowFieldInfoList,
            List<List<FormFieldInfoDTO>> colFieldInfoList) {

        if (rowFieldInfoList == null || rowFieldInfoList.isEmpty() ||
            colFieldInfoList == null || colFieldInfoList.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 预生成所有行/列组合
        String[][] rowCombos = generateAllRowCombinations(rowFieldInfoList);
        String[][] colCombos = generateAllColCombinations(colFieldInfoList);

        int totalRows = rowCombos.length;
        int totalCols = colCombos.length;

        // 2. 构建二维矩阵，存储索引而非完整数据
        List<List<CellIndex>> result = new ArrayList<>(totalRows);
        for (int r = 0; r < totalRows; r++) {
            List<CellIndex> rowCells = new ArrayList<>(totalCols);
            for (int c = 0; c < totalCols; c++) {
                rowCells.add(new CellIndex(r, c));
            }
            result.add(rowCells);
        }

        return result;
    }

    /**
     * 获取所有行组合的完整数据
     * 注意：返回的是预生成的二维数组，调用方不应修改
     */
    public String[][] getRowCombinations(List<List<FormFieldInfoDTO>> rowFieldInfoList) {
        return generateAllRowCombinations(rowFieldInfoList);
    }

    /**
     * 获取所有列组合的完整数据
     */
    public String[][] getColCombinations(List<List<FormFieldInfoDTO>> colFieldInfoList) {
        return generateAllColCombinations(colFieldInfoList);
    }

    /**
     * 根据索引获取单元格对应的完整成员ID数组
     */
    public String[] getCellMemberIds(CellIndex cell, String[][] rowCombos, String[][] colCombos) {
        String[] rowMembers = rowCombos[cell.rowIndex()];
        String[] colMembers = colCombos[cell.colIndex()];
        String[] result = new String[rowMembers.length + colMembers.length];
        System.arraycopy(rowMembers, 0, result, 0, rowMembers.length);
        System.arraycopy(colMembers, 0, result, rowMembers.length, colMembers.length);
        return result;
    }

    /**
     * 生成所有行组合
     */
    private String[][] generateAllRowCombinations(List<List<FormFieldInfoDTO>> rowFieldInfoList) {
        List<String[]> allCombos = new ArrayList<>();
        for (List<FormFieldInfoDTO> dims : rowFieldInfoList) {
            List<String[]> combos = generateCartesianIterative(dims);
            allCombos.addAll(combos);
        }
        return allCombos.toArray(new String[0][]);
    }

    /**
     * 生成所有列组合
     */
    private String[][] generateAllColCombinations(List<List<FormFieldInfoDTO>> colFieldInfoList) {
        List<String[]> allCombos = new ArrayList<>();
        for (List<FormFieldInfoDTO> dims : colFieldInfoList) {
            List<String[]> combos = generateCartesianIterative(dims);
            allCombos.addAll(combos);
        }
        return allCombos.toArray(new String[0][]);
    }

    /**
     * 迭代式笛卡尔积生成（替代递归，避免栈溢出）
     */
    private List<String[]> generateCartesianIterative(List<FormFieldInfoDTO> dims) {
        if (dims.isEmpty()) {
            return Collections.singletonList(new String[0]);
        }

        // 提取各维度的成员列表
        List<String[]> dimMembers = new ArrayList<>();
        int totalMembers = 1;
        for (FormFieldInfoDTO dim : dims) {
            String[] members = extractMemberIds(dim);
            dimMembers.add(members);
            totalMembers *= members.length;
        }

        List<String[]> result = new ArrayList<>(totalMembers);
        int[] indices = new int[dims.size()];

        while (true) {
            // 构建当前组合
            String[] combo = new String[dims.size()];
            for (int i = 0; i < dims.size(); i++) {
                combo[i] = dimMembers.get(i)[indices[i]];
            }
            result.add(combo);

            // 递增索引
            int pos = dims.size() - 1;
            while (pos >= 0) {
                indices[pos]++;
                if (indices[pos] < dimMembers.get(pos).length) {
                    break;
                }
                indices[pos] = 0;
                pos--;
            }
            if (pos < 0) {
                break; // 所有组合已生成
            }
        }

        return result;
    }

    /**
     * 提取维度的成员ID数组
     */
    private String[] extractMemberIds(FormFieldInfoDTO dim) {
        if (dim.getMembers() == null || dim.getMembers().isEmpty()) {
            return new String[0];
        }
        return dim.getMembers().stream()
                .map(MemberDTO::getMemberId)
                .toArray(String[]::new);
    }

    /**
     * 计算总单元格数
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
     * 获取总行数
     */
    public int getTotalRows(List<List<FormFieldInfoDTO>> rowFieldInfoList) {
        int total = 0;
        for (List<FormFieldInfoDTO> dims : rowFieldInfoList) {
            total += countCombinationsForDims(dims);
        }
        return total;
    }

    /**
     * 获取总列数
     */
    public int getTotalCols(List<List<FormFieldInfoDTO>> colFieldInfoList) {
        int total = 0;
        for (List<FormFieldInfoDTO> dims : colFieldInfoList) {
            total += countCombinationsForDims(dims);
        }
        return total;
    }

    /**
     * 单元格索引 record（JDK 21 record类型）
     * 只存储行/列索引，不存储完整数据，极大减少内存
     */
    public record CellIndex(int rowIndex, int colIndex) {
        // 自动生成 equals, hashCode, toString, 访问器
    }
}
