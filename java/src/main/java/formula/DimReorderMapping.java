package formula;

import java.util.Arrays;
import java.util.HashMap;

/**
 * 公式伪坐标维度序 → 单元格伪坐标维度序 的<strong>预计算重排表</strong>。
 *
 * <h2>问题背景</h2>
 * <p>伪坐标字符串 {@code "2001,1001,3001"} 中各段的含义由 {@code dim_ids} 顺序决定。
 * 若公式配置时 {@code formulaDimIds = [20, 10, 30]}，则段序为「维度20成员, 维度10成员, 维度30成员」；
 * 表单单元格 {@code cellDimIds = [10, 20, 30]} 则期望 key 为 {@code "1001,2001,3001"}。
 * 比对前必须把<strong>同一组成员 ID</strong>按单元格维度序重新排列。
 *
 * <h2>实现思路</h2>
 * <p>构建时建立 {@link #sourceIndexByTarget}：对单元格维度序的每个位置 {@code t}，
 * 记录在<strong>公式成员数组</strong>中的下标 {@code s}；若某维度在公式侧不存在则为 {@code -1}。
 * 热路径 {@link #appendCellKey} 只做数组下标读取与 {@link StringBuilder} 追加，不再分配 {@link HashMap}。
 *
 * <h2>示例</h2>
 * <pre>
 * formulaDimIds = [20, 10, 30]   members 解析为 [2001, 1001, 3001]  （下标 0,1,2）
 * cellDimIds    = [10, 20, 30]
 * sourceIndexByTarget = [1, 0, 2]   // cell 维10→members[1], 维20→members[0], 维30→members[2]
 * appendCellKey → "1001,2001,3001"
 * </pre>
 */
public final class DimReorderMapping {

    /**
     * {@code sourceIndexByTarget[t]} = 公式侧成员数组下标，用于填充单元格维度序第 {@code t} 段；
     * {@code -1} 表示该单元格维度在公式侧无对应成员。
     */
    private final int[] sourceIndexByTarget;

    private final int targetLength;

    /** 缓存 hashCode，避免 equals 时重复计算 */
    private final int hashCode;

    private DimReorderMapping(int[] sourceIndexByTarget, int targetLength, int hashCode) {
        this.sourceIndexByTarget = sourceIndexByTarget;
        this.targetLength = targetLength;
        this.hashCode = hashCode;
    }

    /**
     * 根据两套 dim_ids 构建映射。维度数量通常 &lt; 20，双重循环 O(n²) 可接受，且每条公式 dim 组合有限。
     */
    public static DimReorderMapping of(long[] formulaDimIds, long[] cellDimIds) {
        int srcLen = formulaDimIds.length;
        int tgtLen = cellDimIds.length;
        int[] indexByTarget = new int[tgtLen];
        for (int t = 0; t < tgtLen; t++) {
            long dimId = cellDimIds[t];
            int srcIdx = -1;
            for (int s = 0; s < srcLen; s++) {
                if (formulaDimIds[s] == dimId) {
                    srcIdx = s;
                    break;
                }
            }
            indexByTarget[t] = srcIdx;
        }
        int h = 31 * Arrays.hashCode(formulaDimIds) + Arrays.hashCode(cellDimIds);
        return new DimReorderMapping(indexByTarget, tgtLen, h);
    }

    /**
     * 将已解析的成员 ID 数组，按单元格维度序写入 {@code keyBuilder}（逗号分隔，无空格）。
     *
     * @param members      由 {@link PseudoCoordinateCodec#parseCommaSeparatedInto} 填入
     * @param memberCount  有效成员个数
     * @param keyBuilder   会话级复用的 StringBuilder，调用前会被 {@code setLength(0)}
     */
    public void appendCellKey(long[] members, int memberCount, StringBuilder keyBuilder) {
        keyBuilder.setLength(0);
        for (int t = 0; t < targetLength; t++) {
            int srcIdx = sourceIndexByTarget[t];
            if (srcIdx < 0 || srcIdx >= memberCount) {
                // 公式侧缺少该维度成员，跳过（与旧逻辑 reorder 中 get(null) 行为一致）
                continue;
            }
            if (keyBuilder.length() > 0) {
                keyBuilder.append(',');
            }
            keyBuilder.append(members[srcIdx]);
        }
    }

    public int targetLength() {
        return targetLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DimReorderMapping other)) {
            return false;
        }
        return targetLength == other.targetLength
                && Arrays.equals(sourceIndexByTarget, other.sourceIndexByTarget);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * 在 {@link FormulaEchoSession} 内对 {@link DimReorderMapping} 实例去重。
     *
     * <p>同一表单打开过程中，{@code cellDimIds} 固定；多条公式可能共享同一 {@code formulaDimIds}，
     * 通过 {@link HashMap#computeIfAbsent} 保证相同维度配置只保留一份 {@code sourceIndexByTarget}。
     */
    static final class Interner {
        private final HashMap<DimReorderMapping, DimReorderMapping> pool = new HashMap<>(8);
        private final long[] cellDimIds;

        Interner(long[] cellDimIds) {
            this.cellDimIds = cellDimIds;
        }

        DimReorderMapping forFormulaDims(long[] formulaDimIds) {
            DimReorderMapping probe = DimReorderMapping.of(formulaDimIds, cellDimIds);
            return pool.computeIfAbsent(probe, k -> probe);
        }
    }
}
