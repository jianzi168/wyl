package formula;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 打开表单时，将数据表中已配置的公式批量回显到当前表单单元格上。
 *
 * <h2>业务场景</h2>
 * <p>BI 表单单元格用「伪坐标」标识：多个维度成员 ID 按约定顺序逗号拼接（如 {@code "1001,2001,3001"}）。
 * 配置公式时，伪坐标可能按<strong>公式维度序</strong>{@code formulaDimIds} 拼接；打开表单时，
 * 单元格按<strong>单元格维度序</strong>{@code cellDimIds} 拼接。二者成员相同但顺序可能不同，
 * 回显前必须把公式侧伪坐标重排为单元格侧 key，再与表单映射比对。
 *
 * <h2>表单入参：{@code Map<String, List<int[]>> pseudoToTablePositions}</h2>
 * <ul>
 *   <li><strong>key</strong> — 已是 {@code cellDimIds} 顺序的伪坐标字符串</li>
 *   <li><strong>value</strong> — 该伪坐标在当前表单上的物理位置；同一 key 可对应多个格子
 *       （合并单元格、重复维度组合等），回显时每个位置都会写入同一条公式</li>
 * </ul>
 *
 * <h2>数据表入参：{@link FormulaRecord}</h2>
 * <p>由持久层查询组装，本类不负责保存逻辑。字段与表列对应关系见 {@link FormulaRecord} 各字段注释。
 *
 * <h2>校验通过条件（全部满足才回显）</h2>
 * <ol>
 *   <li>公式非空，且不含跨表引用</li>
 *   <li>公式内本表引用个数 = {@code dagNodeIds.length}，且 {@code dagPseudos} 与 DAG 一一对应</li>
 *   <li>公式格 {@code formulaPseudo} 重排后，在 {@code pseudoToTablePositions} 中存在且位置列表非空</li>
 *   <li>每个 {@code dagPseudos[i]} 重排后，同样在表单映射中存在（表示引用格仍在当前表单上）</li>
 * </ol>
 *
 * <h2>出参</h2>
 * {@code Map<String, String>}：key 为表单位置（如 {@code "3,4"}），value 为公式原文（如 {@code "=B3+B4"}）。
 *
 * <h2>高性能设计（见 {@link FormulaEchoSession}）</h2>
 * <ul>
 *   <li>公式 {@link FormulaReferenceScanner#scanLocalRefCountOrCrossSheet} 只扫一遍</li>
 *   <li>{@link DimReorderMapping} 预计算下标，避免每条伪坐标 new HashMap</li>
 *   <li>会话内缓存「原始公式伪坐标串 → 单元格 key 串」</li>
 * </ul>
 */
public final class FormulaEchoMatcher {

    private FormulaEchoMatcher() {
    }

    /**
     * 数据表中的一条公式配置行（打开表单前由 SQL/ORM 查出）。
     *
     * @param formulaId      公式主键
     * @param formula        展示用公式文本，如 {@code =B3+B4}
     * @param formulaPseudo  公式所在格的 {@code formula_pseudo}（按 {@code formulaDimIds} 顺序拼接成员 ID）
     * @param formulaDimIds  公式伪坐标的 {@code dim_ids}，定义 {@code formulaPseudo} 中各段的维度含义
     * @param dagNodeIds     {@code dag} 列：公式右侧引用的单元格结点 ID；顺序须与公式中本表引用从左到右一致
     * @param dagPseudos     与 {@code dagNodeIds} 同序的各引用格伪坐标；用于与表单 Map 的 key 比对
     */
    public record FormulaRecord(
            long formulaId,
            String formula,
            String formulaPseudo,
            long[] formulaDimIds,
            long[] dagNodeIds,
            String[] dagPseudos) {

        public FormulaRecord {
            Objects.requireNonNull(formula);
            Objects.requireNonNull(formulaPseudo);
            // 防御性拷贝：避免调用方后续修改数组影响匹配结果
            formulaDimIds = formulaDimIds == null ? new long[0] : formulaDimIds.clone();
            dagNodeIds = dagNodeIds == null ? new long[0] : dagNodeIds.clone();
            dagPseudos = dagPseudos == null ? new String[0] : dagPseudos.clone();
        }
    }

    /**
     * 打开表单时的<strong>推荐入口</strong>：一次创建 {@link FormulaEchoSession}，遍历全部公式行。
     *
     * @param cellDimIds             当前表单单元格的 {@code dim_ids}（拼接顺序）
     * @param pseudoToTablePositions 表单伪坐标 → 表单位置列表（见类说明）
     * @param formulas               本表单已配置的全部公式行
     * @return 通过校验的「表单位置 → 公式」映射；未通过校验的公式不会出现在结果中
     */
    public static Map<String, String> buildEchoMapForFormOpen(
            long[] cellDimIds,
            Map<String, List<int[]>> pseudoToTablePositions,
            List<FormulaRecord> formulas) {
        Objects.requireNonNull(cellDimIds);
        Objects.requireNonNull(pseudoToTablePositions);
        Objects.requireNonNull(formulas);
        if (formulas.isEmpty()) {
            return Map.of();
        }
        // 按采样估算容量，减少 HashMap 扩容
        HashMap<String, String> result =
                HashMap.newHashMap(estimatePositionCount(pseudoToTablePositions, formulas.size()));
        // try-with-resources：批量结束后释放会话内伪坐标缓存
        try (FormulaEchoSession session = FormulaEchoSession.open(cellDimIds, pseudoToTablePositions)) {
            for (FormulaRecord row : formulas) {
                tryPutEchoForFormOpen(result, session, row);
            }
        }
        return result;
    }

    /**
     * 单条公式回显（便捷重载）：内部临时创建 Session，适合调试或仅处理一条的场景。
     * <p>批量场景请用 {@link #tryPutEchoForFormOpen(Map, FormulaEchoSession, FormulaRecord)} 复用 Session。
     */
    public static boolean tryPutEchoForFormOpen(
            Map<String, String> result,
            long[] cellDimIds,
            Map<String, List<int[]>> pseudoToTablePositions,
            FormulaRecord row) {
        try (FormulaEchoSession session = FormulaEchoSession.open(cellDimIds, pseudoToTablePositions)) {
            return tryPutEchoForFormOpen(result, session, row);
        }
    }

    /**
     * 单条公式回显：校验通过后，向 {@code result} 写入该公式锚点伪坐标对应的<strong>所有</strong>表单位置。
     *
     * @param result  累积回显结果，调用方传入可变 Map
     * @param session 与本次「打开表单」绑定的会话（含缓存与复用缓冲区）
     * @param row     单条公式记录
     * @return {@code true} 表示至少写入了一个位置；{@code false} 表示本条公式无效，未写入
     */
    public static boolean tryPutEchoForFormOpen(
            Map<String, String> result,
            FormulaEchoSession session,
            FormulaRecord row) {
        List<int[]> positions = resolveAnchorPositionsIfValid(session, row);
        if (positions == null) {
            return false;
        }
        String formula = row.formula;
        // 同一伪坐标可能映射多个物理格，每个位置都要显示该公式
        for (int[] position : positions) {
            result.put(TablePositionKey.format(position), formula);
        }
        return true;
    }

    /**
     * 仅判断一条公式是否满足回显条件，不写入结果 Map。
     */
    public static boolean isValidForFormOpen(
            long[] cellDimIds,
            Map<String, List<int[]>> pseudoToTablePositions,
            FormulaRecord row) {
        try (FormulaEchoSession session = FormulaEchoSession.open(cellDimIds, pseudoToTablePositions)) {
            return resolveAnchorPositionsIfValid(session, row) != null;
        }
    }

    /**
     * 核心校验逻辑：通过则返回<strong>公式锚点格</strong>在表单上的位置列表；否则 {@code null}。
     *
     * <p>步骤概要：
     * <pre>
     * 1. 扫描 formula → 跨表则失败；本表引用数须等于 dag 长度
     * 2. 取 formulaDimIds → cellDimIds 的 DimReorderMapping（会话内复用）
     * 3. 公式格 formulaPseudo 重排为 cellKey，查 pseudoToTablePositions 得 anchorPositions
     * 4. 每个 dagPseudos 重排后都须在 pseudoToTablePositions 中存在（引用格仍有效）
     * </pre>
     */
    private static List<int[]> resolveAnchorPositionsIfValid(FormulaEchoSession session, FormulaRecord row) {
        String formula = row.formula;
        if (formula == null || formula.isEmpty()) {
            return null;
        }

        // 一次扫描：localRefCount < 0 表示含跨表引用
        int localRefCount = FormulaReferenceScanner.scanLocalRefCountOrCrossSheet(formula);
        if (localRefCount < 0
                || localRefCount != row.dagNodeIds.length
                || row.dagPseudos.length != row.dagNodeIds.length) {
            return null;
        }

        // 本条公式若使用与默认不同的 formulaDimIds，mapping 在 Interner 中按数组内容去重
        DimReorderMapping mapping = session.mappingFor(row.formulaDimIds);

        // 锚点：公式写在哪个「维度组合」上；必须在当前表单能找到对应单元格
        List<int[]> anchorPositions = session.positionsOnForm(row.formulaPseudo, mapping);
        if (anchorPositions == null || anchorPositions.isEmpty()) {
            return null;
        }

        // 引用侧：B3、B4 等对应结点伪坐标也必须在表单 Map 中存在，保证「两边单元格结点一致」
        for (String dagPseudo : row.dagPseudos) {
            if (dagPseudo == null || !session.pseudoExistsOnForm(dagPseudo, mapping)) {
                return null;
            }
        }
        return anchorPositions;
    }

    /**
     * 根据表单映射采样，估算回显结果 Map 的初始容量（避免频繁 rehash）。
     * <p>只扫描前 16 个 entry 求平均位置数，再与公式条数加权，上限做了裁剪。
     */
    private static int estimatePositionCount(
            Map<String, List<int[]>> pseudoToTablePositions, int formulaCount) {
        if (pseudoToTablePositions.isEmpty()) {
            return formulaCount;
        }
        int sample = 0;
        int n = 0;
        for (List<int[]> list : pseudoToTablePositions.values()) {
            if (list != null) {
                sample += list.size();
                n++;
            }
            if (n >= 16) {
                break;
            }
        }
        int avg = n == 0 ? 1 : Math.max(1, sample / n);
        return Math.min(pseudoToTablePositions.size() * avg, formulaCount * avg * 4);
    }
}
