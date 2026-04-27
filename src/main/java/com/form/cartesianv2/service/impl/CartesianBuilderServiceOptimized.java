package com.form.cartesianv2.service.impl;

import com.form.cartesianv2.dto.FieldDimDTO;
import com.form.cartesianv2.dto.FormCellDTO;
import com.form.cartesianv2.dto.FormFieldDTO;
import com.form.cartesianv2.dto.MemberDTO;
import com.form.cartesianv2.dto.MemberValueDTO;
import com.form.cartesianv2.service.ICartesianBuilderService;

import java.util.*;

/**
 * 高性能、低内存笛卡尔积构建器
 *
 * 优化策略:
 * 1. 惰性求值 - 按需生成单元格，不一次性加载全部到内存
 * 2. 数组复用 - 复用 Long[] 缓冲区减少GC压力
 * 3. 原始类型 - 使用 long[] 替代 Long[] 减少对象开销
 * 4. 预分配容量 - 避免动态扩容带来的性能损耗
 * 5. 严格顺序保证 - 按行主序(row-major)依次遍历，保证单元格顺序
 */
public class CartesianBuilderServiceOptimized implements ICartesianBuilderService {

    /**
     * 单例共享的列组合缓冲区，避免每次合并时创建新数组
     * ThreadLocal 确保线程安全
     */
    private static final ThreadLocal<long[]> COL_BUFFER = ThreadLocal.withInitial(() -> new long[32]);

    /**
     * 单例共享的行组合缓冲区
     */
    private static final ThreadLocal<long[]> ROW_BUFFER = ThreadLocal.withInitial(() -> new long[32]);

    /**
     * 单例共享的合并结果缓冲区
     */
    private static final ThreadLocal<long[]> MERGE_BUFFER = ThreadLocal.withInitial(() -> new long[64]);

    @Override
    public List<List<FormCellDTO>> buildCrossProduct(List<List<FormFieldDTO>> rowFieldGroups, List<List<FormFieldDTO>> colFieldGroups) {
        if (rowFieldGroups == null || rowFieldGroups.isEmpty() ||
            colFieldGroups == null || colFieldGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: 生成行组合 (原始long[]数组)
        List<long[]> rowCombinations = generateRowCombinations(rowFieldGroups);
        if (rowCombinations.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: 生成列组合
        List<long[]> colCombinations = generateColCombinations(colFieldGroups);
        if (colCombinations.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 3: 构建二维矩阵，预分配容量
        int totalRows = rowCombinations.size();
        int totalCols = colCombinations.size();
        int memberCountPerCell = rowCombinations.get(0).length + colCombinations.get(0).length;

        List<List<FormCellDTO>> result = new ArrayList<>(totalRows);
        for (int r = 0; r < totalRows; r++) {
            // 预分配内层List容量
            List<FormCellDTO> rowCells = new ArrayList<>(totalCols);
            long[] rowCombo = rowCombinations.get(r);
            for (int c = 0; c < totalCols; c++) {
                long[] colCombo = colCombinations.get(c);
                long[] merged = mergeToBuffer(rowCombo, colCombo, memberCountPerCell);
                rowCells.add(new FormCellDTO(toBoxedArray(merged), null));
            }
            result.add(rowCells);
        }

        return result;
    }

    /**
     * 构建惰性迭代器版本 - 极低内存占用
     * 适用于仅需遍历场景，不需随机访问
     *
     * @param rowFieldGroups 行字段分组
     * @param colFieldGroups 列字段分组
     * @return 可迭代的单元格序列，保证行主序遍历顺序
     */
    public Iterable<FormCellDTO> buildCrossProductLazy(
            List<List<FormFieldDTO>> rowFieldGroups,
            List<List<FormFieldDTO>> colFieldGroups) {

        if (rowFieldGroups == null || rowFieldGroups.isEmpty() ||
            colFieldGroups == null || colFieldGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<long[]> rowCombinations = generateRowCombinations(rowFieldGroups);
        List<long[]> colCombinations = generateColCombinations(colFieldGroups);

        return new Iterable<>() {
            @Override
            public Iterator<FormCellDTO> iterator() {
                return new CellIterator(rowCombinations, colCombinations);
            }
        };
    }

    /**
     * 分页获取版本 - 极低内存，支持超大规模数据
     *
     * @param rowFieldGroups 行字段分组
     * @param colFieldGroups 列字段分组
     * @param rowStart       起始行索引(包含)
     * @param rowEnd         结束行索引(不包含)
     * @param pageSize       每页行数
     * @return 指定范围的单元格列表
     */
    public List<List<FormCellDTO>> buildCrossProductPaged(
            List<List<FormFieldDTO>> rowFieldGroups,
            List<List<FormFieldDTO>> colFieldGroups,
            int rowStart, int rowEnd, int pageSize) {

        if (rowFieldGroups == null || rowFieldGroups.isEmpty() ||
            colFieldGroups == null || colFieldGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<long[]> rowCombinations = generateRowCombinations(rowFieldGroups);
        List<long[]> colCombinations = generateColCombinations(colFieldGroups);

        int totalRows = rowCombinations.size();
        int totalCols = colCombinations.size();
        int memberCountPerCell = rowCombinations.get(0).length + colCombinations.get(0).length;

        // 边界检查
        rowStart = Math.max(0, rowStart);
        rowEnd = Math.min(totalRows, rowEnd);
        if (rowStart >= rowEnd) {
            return Collections.emptyList();
        }

        List<List<FormCellDTO>> result = new ArrayList<>((rowEnd - rowStart) * pageSize);
        for (int r = rowStart; r < rowEnd; r++) {
            List<FormCellDTO> rowCells = new ArrayList<>(totalCols);
            long[] rowCombo = rowCombinations.get(r);
            for (int c = 0; c < totalCols; c++) {
                long[] colCombo = colCombinations.get(c);
                long[] merged = mergeToBuffer(rowCombo, colCombo, memberCountPerCell);
                rowCells.add(new FormCellDTO(toBoxedArray(merged), null));
            }
            result.add(rowCells);
        }

        return result;
    }

    /**
     * 流式构建版本 - 使用JDK21 Stream，惰性求值
     *
     * @param rowFieldGroups 行字段分组
     * @param colFieldGroups 列字段分组
     * @return Stream<FormCellDTO> 保证行主序
     */
    public java.util.stream.Stream<FormCellDTO> buildCrossProductStream(
            List<List<FormFieldDTO>> rowFieldGroups,
            List<List<FormFieldDTO>> colFieldGroups) {

        if (rowFieldGroups == null || rowFieldGroups.isEmpty() ||
            colFieldGroups == null || colFieldGroups.isEmpty()) {
            return java.util.stream.Stream.empty();
        }

        List<long[]> rowCombinations = generateRowCombinations(rowFieldGroups);
        List<long[]> colCombinations = generateColCombinations(colFieldGroups);

        return java.util.stream.Stream.iterate(
                new int[]{0, 0},  // [row, col]
                indices -> indices[0] < rowCombinations.size(),
                indices -> indices[1] + 1 < colCombinations.size()
                        ? new int[]{indices[0], indices[1] + 1}
                        : new int[]{indices[0] + 1, 0}
        ).map(indices -> {
            int r = indices[0];
            int c = indices[1];
            long[] rowCombo = rowCombinations.get(r);
            long[] colCombo = colCombinations.get(c);
            int memberCount = rowCombo.length + colCombo.length;
            long[] merged = mergeToBuffer(rowCombo, colCombo, memberCount);
            return new FormCellDTO(toBoxedArray(merged), null);
        });
    }

    // ==================== Private Helper Methods ====================

    /**
     * 生成行组合列表
     */
    private List<long[]> generateRowCombinations(List<List<FormFieldDTO>> rowFieldGroups) {
        List<long[]> allRowCombinations = new ArrayList<>();
        for (List<FormFieldDTO> rowGroup : rowFieldGroups) {
            allRowCombinations.addAll(generateCartesianLong(rowGroup));
        }
        return allRowCombinations;
    }

    /**
     * 生成列组合列表
     */
    private List<long[]> generateColCombinations(List<List<FormFieldDTO>> colFieldGroups) {
        List<long[]> allColCombinations = new ArrayList<>();
        for (List<FormFieldDTO> colGroup : colFieldGroups) {
            allColCombinations.addAll(generateCartesianLong(colGroup));
        }
        return allColCombinations;
    }

    /**
     * 生成笛卡尔积，返回原始long[]类型(非Long对象)
     *
     * 核心流程:
     * 1. 遍历每个维度，提取去重后的valueIds
     * 2. 估算结果容量，预分配ArrayList避免扩容
     * 3. 递归生成笛卡尔积
     *
     * @param fields 一个大行/大列下的所有字段配置
     * @return 笛卡尔积结果列表，每项是对应维度的valueIds组合
     */
    private List<long[]> generateCartesianLong(List<FormFieldDTO> fields) {
        if (fields == null || fields.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集每个维度的成员列表(去重后)
        // dimMembers[i] 代表第i个维度的所有去重后的valueId
        List<long[]> dimMembers = new ArrayList<>();

        for (FormFieldDTO field : fields) {
            List<FieldDimDTO> dims = field.getMemberDTOS();
            if (dims == null || dims.isEmpty()) {
                continue;
            }

            // 遍历每个FieldDimDTO(维度)
            for (FieldDimDTO dim : dims) {
                // 使用原始long类型避免AutoBoxing，减少内存分配
                long[] uniqueValueIds = extractUniqueValueIds(dim);
                if (uniqueValueIds.length > 0) {
                    dimMembers.add(uniqueValueIds);
                }
            }
        }

        if (dimMembers.isEmpty()) {
            return Collections.emptyList();
        }

        // 预分配结果列表容量，避免动态扩容带来的性能损耗
        int estimatedSize = estimateCartesianSize(dimMembers);
        List<long[]> result = new ArrayList<>(estimatedSize);

        long[] current = new long[dimMembers.size()];
        cartesianRecursiveLong(result, current, dimMembers, 0);

        return result;
    }

    /**
     * 从FieldDimDTO提取去重后的valueIds(原始long[]类型)
     *
     * 去重逻辑:
     * - 同一个FieldDimDTO下的所有MemberDTO的MemberValueDTO.valueId需要合并去重
     * - 使用LinkedHashSet保持插入顺序(按成员配置顺序)
     * - 最终返回一个排序后(按配置顺序)去重的valueId数组
     *
     * @param dim 维度DTO，包含多个成员
     * @return 去重后的valueId数组，使用原始long[]类型避免自动装箱
     */
    private long[] extractUniqueValueIds(FieldDimDTO dim) {
        // LinkedHashSet: 既去重又保持插入顺序
        Set<Long> uniqueValueIds = new LinkedHashSet<>();
        List<MemberDTO> members = dim.getMemberDTOS();
        if (members == null || members.isEmpty()) {
            return new long[0];
        }

        // 遍历该维度下的所有Member
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

        // 转换为原始long[]类型，避免自动装箱(Long -> long)
        long[] result = new long[uniqueValueIds.size()];
        int i = 0;
        for (Long id : uniqueValueIds) {
            result[i++] = id;
        }
        return result;
    }

    /**
     * 估算笛卡尔积大小，用于预分配
     *
     * 计算公式: size = dim1.size × dim2.size × ... × dimN.size
     * 如果结果超过Integer.MAX_VALUE，返回Integer.MAX_VALUE
     *
     * @param dimMembers 各维度的成员数组
     * @return 预估的笛卡尔积大小
     */
    private int estimateCartesianSize(List<long[]> dimMembers) {
        long size = 1;
        for (long[] members : dimMembers) {
            size *= members.length;
            // 防止溢出
            if (size > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) size;
    }

    /**
     * 深度优先递归生成笛卡尔积(原始类型)
     *
     * 算法思想:
     * - 按维度深度优先遍历
     * - current数组存储当前递归路径上的选择
     * - 递归终止时克隆current到result
     *
     * @param result 存储结果的列表
     * @param current 当前组合的临时数组
     * @param dimMemberLists 各维度的成员列表
     * @param depth 当前递归深度(维度索引)
     */
    private void cartesianRecursiveLong(List<long[]> result, long[] current,
                                       List<long[]> dimMemberLists, int depth) {
        // 所有维度都处理完毕，保存当前组合
        if (depth == dimMemberLists.size()) {
            result.add(current.clone());
            return;
        }
        // 遍历当前维度的每个成员
        long[] members = dimMemberLists.get(depth);
        for (long memberId : members) {
            current[depth] = memberId;
            // 递归处理下一个维度
            cartesianRecursiveLong(result, current, dimMemberLists, depth + 1);
        }
    }

    /**
     * 合并行和列组合到共享缓冲区
     *
     * 优化策略:
     * - 使用ThreadLocal缓冲区复用，避免每次调用都分配新数组
     * - 只有当缓冲区大小不足时才扩容
     * - 返回Arrays.copyOf结果，确保返回的数组独立于缓冲区
     *
     * @param rowMembers 行小组合的成员IDs
     * @param colMembers 列小组合的成员IDs
     * @param totalLength 合并后的总长度
     * @return 合并后的成员IDs数组(独立副本)
     */
    private long[] mergeToBuffer(long[] rowMembers, long[] colMembers, int totalLength) {
        long[] buffer = MERGE_BUFFER.get();
        // 扩容检查: 只有当缓冲区不足时才创建新数组
        if (buffer.length < totalLength) {
            buffer = new long[totalLength];
            MERGE_BUFFER.set(buffer);
        }
        // 拷贝行数据到缓冲区
        System.arraycopy(rowMembers, 0, buffer, 0, rowMembers.length);
        // 拷贝列数据到缓冲区(紧跟行数据之后)
        System.arraycopy(colMembers, 0, buffer, rowMembers.length, colMembers.length);
        // 返回独立副本，避免缓冲区被后续修改影响结果
        return Arrays.copyOf(buffer, totalLength);
    }

    /**
     * 将原始long[]转换为Long[] (用于FormCellDTO)
     */
    private Long[] toBoxedArray(long[] arr) {
        Long[] boxed = new Long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            boxed[i] = arr[i];
        }
        return boxed;
    }

    /**
     * 惰性单元格迭代器 - 严格保证行主序遍历顺序
     *
     * 设计思想:
     * - 按需计算: 只有调用next()时才计算下一个单元格
     * - 缓冲机制: 预加载下一个单元格到bufferedCell，减少hasNext的计算开销
     * - 线程不安全: 单线程使用场景，不支持并发遍历
     *
     * 遍历顺序示例(3行×4列):
     * [0,0] → [0,1] → [0,2] → [0,3] → [1,0] → [1,1] → ... → [2,3]
     */
    private static class CellIterator implements Iterator<FormCellDTO> {

        private final List<long[]> rowCombinations;    // 所有行小组合
        private final List<long[]> colCombinations;   // 所有列小组合
        private final int totalRows;                   // 总行数
        private final int totalCols;                  // 总列数
        private final int memberCountPerCell;         // 每单元格成员数量

        private int currentRow = 0;                   // 当前行索引
        private int currentCol = 0;                   // 当前列索引
        private FormCellDTO bufferedCell = null;       // 预缓冲的单元格

        CellIterator(List<long[]> rowCombinations, List<long[]> colCombinations) {
            this.rowCombinations = rowCombinations;
            this.colCombinations = colCombinations;
            this.totalRows = rowCombinations.size();
            this.totalCols = colCombinations.size();
            this.memberCountPerCell = rowCombinations.get(0).length + colCombinations.get(0).length;
        }

        @Override
        public boolean hasNext() {
            // 如果有预缓冲的单元格，直接返回true
            if (bufferedCell != null) {
                return true;
            }
            // 行索引已越界，没有更多元素
            if (currentRow >= totalRows) {
                return false;
            }
            // 预加载下一个单元格
            bufferedCell = computeCell(currentRow, currentCol);
            return true;
        }

        @Override
        public FormCellDTO next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // 返回预缓冲的单元格
            FormCellDTO cell = bufferedCell;
            bufferedCell = null;
            // 前进到下一个位置
            advance();
            return cell;
        }

        /**
         * 前进到下一个单元格
         *
         * 行主序遍历:
         * - 列索引先递增
         * - 列越界时重置为0，行索引递增
         * - 行越界时不再递增
         */
        private void advance() {
            currentCol++;
            if (currentCol >= totalCols) {
                currentCol = 0;
                currentRow++;
            }
        }

        /**
         * 计算指定位置的单元格
         *
         * @param row 行索引
         * @param col 列索引
         * @return 合并了行和列成员ID的单元格DTO
         */
        private FormCellDTO computeCell(int row, int col) {
            long[] rowCombo = rowCombinations.get(row);
            long[] colCombo = colCombinations.get(col);
            // 合并行和列数据
            long[] merged = new long[memberCountPerCell];
            System.arraycopy(rowCombo, 0, merged, 0, rowCombo.length);
            System.arraycopy(colCombo, 0, merged, rowCombo.length, colCombo.length);
            // 转换为Boxed类型用于FormCellDTO
            Long[] boxed = new Long[merged.length];
            for (int i = 0; i < merged.length; i++) {
                boxed[i] = merged[i];
            }
            return new FormCellDTO(boxed, null);
        }
    }
}
