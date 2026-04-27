package com.form.cartesianv2.service.impl;

import com.form.cartesianv2.service.ICartesianBuilderService;
import com.form.cartesianv2.dto.FieldDimDTO;
import com.form.cartesianv2.dto.FormCellDTO;
import com.form.cartesianv2.dto.FormFieldDTO;
import com.form.cartesianv2.dto.MemberDTO;
import com.form.cartesianv2.dto.MemberValueDTO;

import java.util.*;

public class CartesianBuilderService implements ICartesianBuilderService {
    @Override
    public List<List<FormCellDTO>> buildCrossProduct(List<List<FormFieldDTO>> rowFieldGroups, List<List<FormFieldDTO>> colFieldGroups) {
        if (rowFieldGroups == null || rowFieldGroups.isEmpty() ||
            colFieldGroups == null || colFieldGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: 按大行分组生成所有小行的笛卡尔组合
        // 例如: R1(产品×版本) -> 4个小行, R2(产品×版本) -> 4个小行
        List<Long[]> allRowCombinations = new ArrayList<>();
        for (List<FormFieldDTO> rowGroup : rowFieldGroups) {
            allRowCombinations.addAll(generateCartesianFromFields(rowGroup));
        }

        // Step 2: 按大列分组生成所有小列的笛卡尔组合
        // 例如: C1(年×期间) -> 4个小列, C2(年×期间) -> 4个小列
        List<Long[]> allColCombinations = new ArrayList<>();
        for (List<FormFieldDTO> colGroup : colFieldGroups) {
            allColCombinations.addAll(generateCartesianFromFields(colGroup));
        }

        if (allRowCombinations.isEmpty() || allColCombinations.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 3: 构建二维矩阵 result[rowIndex][colIndex]
        // 每个单元格 = 该行的小行成员 + 该列的小列成员
        int totalRows = allRowCombinations.size();
        int totalCols = allColCombinations.size();

        List<List<FormCellDTO>> result = new ArrayList<>(totalRows);
        for (int r = 0; r < totalRows; r++) {
            List<FormCellDTO> rowCells = new ArrayList<>(totalCols);
            Long[] rowCombo = allRowCombinations.get(r);
            for (int c = 0; c < totalCols; c++) {
                Long[] colCombo = allColCombinations.get(c);
                // 合并行成员数组和列成员数组
                Long[] allMembers = mergeMembers(rowCombo, colCombo);
                rowCells.add(new FormCellDTO(allMembers, null));
            }
            result.add(rowCells);
        }

        return result;
    }

    /**
     * 从字段列表生成笛卡尔积，同维度下成员合并取唯一valueId
     * @param fields 一个大行/大列下的所有字段配置
     * @return 笛卡尔积结果，每项是各维度成员valueId的组合
     */
    private List<Long[]> generateCartesianFromFields(List<FormFieldDTO> fields) {
        if (fields == null || fields.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集每个维度的成员列表(去重后)
        List<List<Long>> dimMembers = new ArrayList<>();

        for (FormFieldDTO field : fields) {
            List<FieldDimDTO> dims = field.getMemberDTOS();
            if (dims == null || dims.isEmpty()) {
                continue;
            }

            // 遍历每个FieldDimDTO(维度)
            for (FieldDimDTO dim : dims) {
                // 使用LinkedHashSet去重，同时保持插入顺序
                Set<Long> uniqueValueIds = new LinkedHashSet<>();
                List<MemberDTO> members = dim.getMemberDTOS();
                if (members == null || members.isEmpty()) {
                    continue;
                }

                // 遍历该维度下的所有成员
                for (MemberDTO member : members) {
                    List<MemberValueDTO> memberVals = member.getMemberVals();
                    if (memberVals == null || memberVals.isEmpty()) {
                        continue;
                    }
                    // 提取每个MemberValueDTO的valueId
                    for (MemberValueDTO val : memberVals) {
                        if (val != null && val.getValueId() != null) {
                            uniqueValueIds.add(val.getValueId());
                        }
                    }
                }

                if (!uniqueValueIds.isEmpty()) {
                    dimMembers.add(new ArrayList<>(uniqueValueIds));
                }
            }
        }

        if (dimMembers.isEmpty()) {
            return Collections.emptyList();
        }

        // 递归生成笛卡尔积
        List<Long[]> result = new ArrayList<>();
        Long[] current = new Long[dimMembers.size()];
        cartesianRecursive(result, current, dimMembers, 0);
        return result;
    }

    /**
     * 深度优先递归生成笛卡尔积，保持配置顺序
     * @param result 存储结果的列表
     * @param current 当前组合的临时数组
     * @param dimMemberLists 各维度的成员列表
     * @param depth 当前递归深度(维度索引)
     */
    private void cartesianRecursive(List<Long[]> result, Long[] current,
                                    List<List<Long>> dimMemberLists, int depth) {
        // 所有维度都处理完毕，保存当前组合
        if (depth == dimMemberLists.size()) {
            result.add(current.clone());
            return;
        }
        // 遍历当前维度的每个成员
        for (Long memberId : dimMemberLists.get(depth)) {
            current[depth] = memberId;
            // 递归处理下一个维度
            cartesianRecursive(result, current, dimMemberLists, depth + 1);
        }
    }

    /**
     * 合并行成员数组和列成员数组
     * @param rowMembers 行小组合的成员IDs
     * @param colMembers 列小组合的成员IDs
     * @return 合并后的成员IDs数组
     */
    private Long[] mergeMembers(Long[] rowMembers, Long[] colMembers) {
        Long[] result = Arrays.copyOf(rowMembers, rowMembers.length + colMembers.length);
        System.arraycopy(colMembers, 0, result, rowMembers.length, colMembers.length);
        return result;
    }
}
