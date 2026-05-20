package formula;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单次「打开表单」操作的回显会话上下文。
 *
 * <p>生命周期：与 {@link FormulaEchoMatcher#buildEchoMapForFormOpen} 一次调用绑定，
 * 处理完本批所有 {@link FormulaEchoMatcher.FormulaRecord} 后 {@link #close()} 释放缓存。
 *
 * <h2>为何需要 Session</h2>
 * <p>打开表单时往往有数百～数千条公式，且大量公式共享相同的 {@code formulaDimIds}、
 * 重复的 {@code formulaPseudo} / {@code dagPseudos} 字符串。若每条公式都：
 * <ul>
 *   <li>重新 {@link DimReorderMapping#of} 构建维度映射</li>
 *   <li>重新解析伪坐标并拼接 cellKey 字符串</li>
 * </ul>
 * 会产生大量重复 CPU 与临时对象。Session 在批内复用映射实例与「伪坐标 → cellKey」缓存。
 *
 * <h2>内部资源</h2>
 * <ul>
 *   <li>{@link #mappingInterner} — 按 {@code formulaDimIds} 去重 {@link DimReorderMapping}</li>
 *   <li>{@link #pseudoToCellKeyByMapping} — 二级缓存：先按 mapping，再按原始伪坐标串</li>
 *   <li>{@link #memberParseBuffer} / {@link #cellKeyBuilder} — 解析与拼串的复用缓冲区，无 per-call 分配</li>
 * </ul>
 */
public final class FormulaEchoSession implements AutoCloseable {

    /** 单格维度成员数上限（超出则解析抛异常），正常 BI 表单维度数远小于此值 */
    private static final int MAX_MEMBER_SLOTS = 32;

    /** 当前表单的单元格 dim_ids，与 pseudoToTablePositions 的 key 顺序一致 */
    private final long[] cellDimIds;

    /** 表单侧：单元格维度序伪坐标 → 物理位置列表 */
    private final Map<String, List<int[]>> pseudoToTablePositions;

    /** 将各公式的 formulaDimIds 映射到唯一的 DimReorderMapping 实例 */
    private final DimReorderMapping.Interner mappingInterner;

    /**
     * 伪坐标重排缓存。
     * <p>外层 key：某套 formulaDimIds→cellDimIds 的映射；
     * 内层 key：表中原始的 formula_pseudo / 结点伪坐标串；
     * value：重排后与表单 Map 对齐的 key（可直接用于 get）。
     */
    private final HashMap<DimReorderMapping, HashMap<String, String>> pseudoToCellKeyByMapping;

    /** 解析 {@code "1001,2001,3001"} 时复用，避免每次 new long[]} */
    private final long[] memberParseBuffer;

    /** 拼接 cellKey 时复用，避免在热路径上频繁 new StringBuilder */
    private final StringBuilder cellKeyBuilder;

    private FormulaEchoSession(
            long[] cellDimIds,
            Map<String, List<int[]>> pseudoToTablePositions) {
        this.cellDimIds = cellDimIds;
        this.pseudoToTablePositions = pseudoToTablePositions;
        this.mappingInterner = new DimReorderMapping.Interner(cellDimIds);
        this.pseudoToCellKeyByMapping = HashMap.newHashMap(8);
        this.memberParseBuffer = new long[MAX_MEMBER_SLOTS];
        this.cellKeyBuilder = new StringBuilder(64);
    }

    /**
     * 创建会话；请用 try-with-resources 或在批量结束后 {@link #close()}。
     */
    public static FormulaEchoSession open(
            long[] cellDimIds,
            Map<String, List<int[]>> pseudoToTablePositions) {
        return new FormulaEchoSession(cellDimIds, pseudoToTablePositions);
    }

    /**
     * 获取「公式维度序 → 单元格维度序」的重排映射；相同 {@code formulaDimIds} 数组内容返回同一实例。
     */
    DimReorderMapping mappingFor(long[] formulaDimIds) {
        return mappingInterner.forFormulaDims(formulaDimIds);
    }

    /**
     * 将<strong>公式侧</strong>伪坐标字符串转换为<strong>单元格侧</strong> key，供查询 {@code pseudoToTablePositions}。
     *
     * <p>示例：公式保存时 {@code formulaDimIds=[20,10,30]}，伪坐标 {@code "2001,1001,3001"}；
     * 表单 {@code cellDimIds=[10,20,30]}，则 cellKey 为 {@code "1001,2001,3001"}。
     *
     * @param formulaPseudo 表中读出的原始伪坐标串（未重排）
     * @param mapping         本条公式对应的维度映射
     * @return 与表单 Map 的 key 同格式的字符串
     */
    String toCellKey(String formulaPseudo, DimReorderMapping mapping) {
        // 按 mapping 分桶，避免不同 formulaDimIds 下相同伪坐标串产生错误缓存
        HashMap<String, String> lane = pseudoToCellKeyByMapping.get(mapping);
        if (lane == null) {
            lane = HashMap.newHashMap(128);
            pseudoToCellKeyByMapping.put(mapping, lane);
        }
        String cached = lane.get(formulaPseudo);
        if (cached != null) {
            return cached;
        }
        // 未命中缓存：解析 → 按下标重排 → 拼成 cellKey
        int count = PseudoCoordinateCodec.parseCommaSeparatedInto(formulaPseudo, memberParseBuffer);
        mapping.appendCellKey(memberParseBuffer, count, cellKeyBuilder);
        String key = cellKeyBuilder.toString();
        lane.put(formulaPseudo, key);
        return key;
    }

    /**
     * 判断重排后的伪坐标是否在当前表单中存在至少一个单元格。
     * <p>用于 DAG 引用结点校验，不关心具体位置，只关心 key 是否存在。
     */
    boolean pseudoExistsOnForm(String formulaPseudo, DimReorderMapping mapping) {
        String cellKey = toCellKey(formulaPseudo, mapping);
        List<int[]> positions = pseudoToTablePositions.get(cellKey);
        return positions != null && !positions.isEmpty();
    }

    /**
     * 取公式锚点伪坐标对应的所有表单位置（可能多个）。
     * <p>内部会调用 {@link #toCellKey}，锚点伪坐标只重排/cache 一次。
     */
    List<int[]> positionsOnForm(String formulaPseudo, DimReorderMapping mapping) {
        return pseudoToTablePositions.get(toCellKey(formulaPseudo, mapping));
    }

    @Override
    public void close() {
        // 释放批内缓存，避免 Session 被长期持有时占用内存
        pseudoToCellKeyByMapping.clear();
    }
}
