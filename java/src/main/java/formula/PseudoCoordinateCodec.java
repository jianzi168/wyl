package formula;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * 伪坐标的解析、维度重排与字符串格式化工具。
 *
 * <p>伪坐标：用逗号拼接的维度<strong>成员 ID</strong>（非维度 ID），
 * 各段顺序由对应的 {@code dim_ids} 数组定义。
 *
 * <p>回显热路径优先使用：
 * <ul>
 *   <li>{@link #parseCommaSeparatedInto} — 写入调用方缓冲区，零 substring</li>
 *   <li>{@link DimReorderMapping#appendCellKey} — 预计算下标重排</li>
 * </ul>
 * 非热路径或测试可使用 {@link #parseCommaSeparated}、{@link #reorder} 等便捷方法。
 */
public final class PseudoCoordinateCodec {

    private PseudoCoordinateCodec() {
    }

    /**
     * 将伪坐标解析为新分配的 {@code long[]}（会分配数组，非批量热路径首选）。
     */
    public static long[] parseCommaSeparated(String pseudo) {
        if (pseudo == null || pseudo.isEmpty()) {
            return new long[0];
        }
        long[] buf = new long[countCommas(pseudo) + 1];
        int n = parseCommaSeparatedInto(pseudo, buf);
        return n == buf.length ? buf : Arrays.copyOf(buf, n);
    }

    /**
     * 高性能解析：将 {@code pseudo} 中的成员 ID 写入 {@code buffer}，返回个数。
     *
     * <p>规则：
     * <ul>
     *   <li>逗号分隔；允许数字两侧空白</li>
     *   <li>仅支持非负整数成员 ID（与 BI 成员主键一致）</li>
     *   <li>不使用 {@link String#split} / {@link Long#parseLong(String)}，避免正则与临时字符串</li>
     * </ul>
     *
     * @param pseudo 原始伪坐标，如 {@code "1001,2001,3001"}
     * @param buffer 调用方提供，长度建议 ≥ 维度数；不足时抛 {@link IllegalArgumentException}
     * @return 写入的成员个数
     */
    public static int parseCommaSeparatedInto(String pseudo, long[] buffer) {
        if (pseudo == null || pseudo.isEmpty()) {
            return 0;
        }
        int len = pseudo.length();
        int idx = 0;
        long acc = 0;
        boolean inNumber = false;
        for (int i = 0; i <= len; i++) {
            if (i == len || pseudo.charAt(i) == ',') {
                if (inNumber) {
                    if (idx >= buffer.length) {
                        throw new IllegalArgumentException("member buffer too small");
                    }
                    buffer[idx++] = acc;
                    acc = 0;
                    inNumber = false;
                }
            } else {
                char c = pseudo.charAt(i);
                if (c == ' ') {
                    continue;
                }
                if (c < '0' || c > '9') {
                    throw new NumberFormatException("invalid pseudo coordinate: " + pseudo);
                }
                inNumber = true;
                acc = acc * 10 + (c - '0');
            }
        }
        return idx;
    }

    /** 预分配解析缓冲区时，按逗号个数估算成员数量 */
    private static int countCommas(String pseudo) {
        int commas = 0;
        for (int i = 0, n = pseudo.length(); i < n; i++) {
            if (pseudo.charAt(i) == ',') {
                commas++;
            }
        }
        return commas;
    }

    /**
     * 通用重排：按 {@code sourceDimIds} 理解 {@code memberIds}，再按 {@code targetDimIds} 输出新数组。
     * <p>每次调用会分配 {@link HashMap}，批量回显请用 {@link DimReorderMapping}。
     */
    public static long[] reorder(long[] memberIds, long[] sourceDimIds, long[] targetDimIds) {
        if (memberIds.length == 0 || targetDimIds.length == 0) {
            return memberIds;
        }
        int mapSize = Math.min(memberIds.length, sourceDimIds.length);
        HashMap<Long, Long> dimToMember = HashMap.newHashMap(mapSize);
        for (int i = 0; i < mapSize; i++) {
            dimToMember.put(sourceDimIds[i], memberIds[i]);
        }
        long[] out = new long[targetDimIds.length];
        int w = 0;
        for (long dimId : targetDimIds) {
            Long member = dimToMember.get(dimId);
            if (member != null) {
                out[w++] = member;
            }
        }
        return w == out.length ? out : Arrays.copyOf(out, w);
    }

    /** 比较两个成员数组是否逐元素相等（用于测试或工具方法） */
    public static boolean sameMembers(long[] left, long[] right) {
        if (left.length != right.length) {
            return false;
        }
        for (int i = 0; i < left.length; i++) {
            if (left[i] != right[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将成员 ID 格式化为逗号拼接串（单元格维度序），作为表单 Map 的 key。
     */
    public static String formatCommaSeparated(long[] memberIds) {
        if (memberIds.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(memberIds.length * 8);
        for (int i = 0; i < memberIds.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(memberIds[i]);
        }
        return sb.toString();
    }

    /**
     * 将公式侧伪坐标规范化为单元格侧 key（独立调用版，内部会 new {@link DimReorderMapping}）。
     * <p>批量回显应走 {@link FormulaEchoSession#toCellKey} 以复用映射与缓存。
     */
    public static String canonicalOnForm(String pseudo, long[] sourceDimIds, long[] formDimIds) {
        Objects.requireNonNull(pseudo);
        DimReorderMapping mapping = DimReorderMapping.of(sourceDimIds, formDimIds);
        long[] buf = new long[countCommas(pseudo) + 1];
        int n = parseCommaSeparatedInto(pseudo, buf);
        StringBuilder sb = new StringBuilder(pseudo.length());
        mapping.appendCellKey(buf, n, sb);
        return sb.toString();
    }

    /**
     * 判断两段伪坐标在「表单维度序」下是否表示同一组维度成员（工具/测试用）。
     */
    public static boolean matchesOnForm(
            String savedPseudo,
            long[] savedDimIds,
            String currentPseudo,
            long[] formDimIds) {
        long[] saved = parseCommaSeparated(savedPseudo);
        long[] current = parseCommaSeparated(currentPseudo);
        return sameMembers(
                reorder(saved, savedDimIds, formDimIds),
                reorder(current, formDimIds, formDimIds));
    }
}
