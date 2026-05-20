package formula;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Excel 类公式坐标替换：单次扫描识别引用 token，按映射表替换，避免子串误替换（如 B3 命中 B31）。
 *
 * <p>两类映射（查找顺序：先跨表整 token，再本表引用）：
 * <ul>
 *   <li>{@link ReplacementTable#sheetRefs()}：整段跨表引用，如 {@code 表单2!G8} → {@code 表单2!K8}（含引号表名 {@code 'Sheet 1'!G8}）</li>
 *   <li>{@link ReplacementTable#localRefs()}：当前表 A1 / 区域，如 {@code B3} → {@code C3}、{@code C4:C8} → {@code D4:D8}</li>
 * </ul>
 *
 * <p>性能：无正则热路径；{@code charAt} 线性扫描；{@link StringBuilder} 预分配；映射 {@link HashMap} O(1) 查找。
 * 批量场景请复用 {@link ReplacementTable} 并对多条公式顺序调用 {@link #replace(String, ReplacementTable)}。
 */
public final class FormulaCoordinateReplacer {

    private FormulaCoordinateReplacer() {
    }

    /**
     * 不可变替换表；{@link #builder()} 预分配容量，减少扩容。
     */
    public record ReplacementTable(
            Map<String, String> localRefs,
            Map<String, String> sheetRefs) {

        public ReplacementTable {
            localRefs = Map.copyOf(localRefs);
            sheetRefs = Map.copyOf(sheetRefs);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final HashMap<String, String> local = HashMap.newHashMap(64);
            private final HashMap<String, String> sheet = HashMap.newHashMap(16);

            public Builder local(String from, String to) {
                Objects.requireNonNull(from);
                Objects.requireNonNull(to);
                local.put(from, to);
                return this;
            }

            public Builder sheet(String from, String to) {
                Objects.requireNonNull(from);
                Objects.requireNonNull(to);
                sheet.put(from, to);
                return this;
            }

            public ReplacementTable build() {
                return new ReplacementTable(local, sheet);
            }
        }
    }

    /**
     * 替换公式中的坐标引用；无映射命中则原样保留 token。
     *
     * @param formula 含前导 {@code =} 的公式字符串
     * @param table   替换表
     * @return 替换后的公式
     */
    public static String replace(String formula, ReplacementTable table) {
        Objects.requireNonNull(formula);
        Objects.requireNonNull(table);
        if (table.localRefs().isEmpty() && table.sheetRefs().isEmpty()) {
            return formula;
        }
        int n = formula.length();
        StringBuilder out = new StringBuilder(n + 32);
        int i = 0;
        while (i < n) {
            char c = formula.charAt(i);
            if (c == '"') {
                i = copyDoubleQuotedString(formula, i, n, out);
                continue;
            }
            RefToken token = tryParseReference(formula, i, n);
            if (token == null) {
                out.append(c);
                i++;
                continue;
            }
            String replacement = resolveReplacement(formula, token, table);
            if (replacement != null) {
                out.append(replacement);
            } else {
                out.append(formula, token.start(), token.end());
            }
            i = token.end();
        }
        return out.toString();
    }

    private static String resolveReplacement(String formula, RefToken token, ReplacementTable table) {
        if (token.sheetStart() >= 0) {
            String sheetKey = formula.substring(token.sheetStart(), token.end());
            String hit = table.sheetRefs().get(sheetKey);
            if (hit != null) {
                return hit;
            }
            // 跨表 token 未命中时整段保留，不对 G8 等后缀单独走 localRefs
            return null;
        }
        String localKey = formula.substring(token.refStart(), token.end());
        String hit = table.localRefs().get(localKey);
        if (hit != null) {
            return hit;
        }
        String normalized = normalizeRefKey(localKey);
        hit = table.localRefs().get(normalized);
        if (hit == null) {
            return null;
        }
        if (localKey.indexOf('$') >= 0) {
            return preserveAbsoluteMarkers(localKey, hit);
        }
        return hit;
    }

    /** 映射命中规范化 key 时，把原 token 上的 $ 位置套到替换结果上（$B$3 + B3→C3 ⇒ $C$3）。 */
    static String preserveAbsoluteMarkers(String source, String target) {
        if (source.indexOf(':') >= 0) {
            int colon = source.indexOf(':');
            String left = preserveAbsoluteMarkers(source.substring(0, colon), target.substring(0, target.indexOf(':')));
            String right = preserveAbsoluteMarkers(source.substring(colon + 1), target.substring(target.indexOf(':') + 1));
            return left + ':' + right;
        }
        return preserveSingleCellDollars(source, target);
    }

    private static String preserveSingleCellDollars(String source, String target) {
        boolean absCol = !source.isEmpty() && source.charAt(0) == '$';
        int srcColStart = absCol ? 1 : 0;
        int srcColEnd = columnEnd(source, srcColStart);
        boolean absRow = srcColEnd < source.length() && source.charAt(srcColEnd) == '$';

        int tgtColStart = !target.isEmpty() && target.charAt(0) == '$' ? 1 : 0;
        int tgtColEnd = columnEnd(target, tgtColStart);
        int tgtRowStart = tgtColEnd;
        if (tgtRowStart < target.length() && target.charAt(tgtRowStart) == '$') {
            tgtRowStart++;
        }

        StringBuilder sb = new StringBuilder(source.length());
        if (absCol) {
            sb.append('$');
        }
        sb.append(target, tgtColStart, tgtColEnd);
        if (absRow) {
            sb.append('$');
        }
        sb.append(target, tgtRowStart, target.length());
        return sb.toString();
    }

    private static int columnEnd(String ref, int from) {
        int p = from;
        int n = ref.length();
        while (p < n && isColumnLetter(ref.charAt(p))) {
            p++;
        }
        return p;
    }

    /** 去掉 $ 并列字母大写，便于 B3 / $B$3 共用映射。 */
    static String normalizeRefKey(String ref) {
        int len = ref.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char ch = ref.charAt(i);
            if (ch != '$') {
                sb.append(ch);
            }
        }
        int colon = sb.indexOf(":");
        if (colon < 0) {
            upperColumnLetters(sb, 0, sb.length());
            return sb.toString();
        }
        upperColumnLetters(sb, 0, colon);
        int secondStart = colon + 1;
        upperColumnLetters(sb, secondStart, sb.length());
        return sb.toString();
    }

    private static void upperColumnLetters(StringBuilder sb, int from, int to) {
        int i = from;
        while (i < to && isColumnLetter(sb.charAt(i))) {
            sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
            i++;
        }
    }

    private static int copyDoubleQuotedString(String s, int start, int n, StringBuilder out) {
        out.append('"');
        int i = start + 1;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '"') {
                if (i + 1 < n && s.charAt(i + 1) == '"') {
                    out.append('"').append('"');
                    i += 2;
                    continue;
                }
                out.append('"');
                return i + 1;
            }
            out.append(c);
            i++;
        }
        out.append('"');
        return n;
    }

    /**
     * @param sheetStart 跨表前缀起始（含引号表名或裸表名），无表则为 -1
     * @param refStart   A1 或区域起始（含可选 $）
     */
    private record RefToken(int start, int end, int sheetStart, int refStart) {
    }

    private static RefToken tryParseReference(String s, int i, int n) {
        int start = i;
        int sheetStart = -1;
        int refStart = i;

        int sheetEnd = tryParseSheetPrefix(s, i, n);
        if (sheetEnd > i) {
            sheetStart = i;
            refStart = sheetEnd;
            i = sheetEnd;
        }

        int refEnd = tryParseA1OrRange(s, refStart, n);
        if (refEnd <= refStart) {
            return null;
        }
        if (!followedByReferenceBoundary(s, refEnd, n)) {
            return null;
        }
        return new RefToken(start, refEnd, sheetStart, refStart);
    }

    /** 引用后必须是运算符、括号、逗号、空白或结尾，避免把函数名尾部误判为引用。 */
    private static boolean followedByReferenceBoundary(String s, int pos, int n) {
        if (pos >= n) {
            return true;
        }
        char c = s.charAt(pos);
        return c == ')' || c == '(' || c == ',' || c == '+' || c == '-' || c == '*'
                || c == '/' || c == '^' || c == '&' || c == '=' || c == '<' || c == '>'
                || c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * 解析 {@code 'Sheet''s'!} 或 {@code 表单2!}；成功返回 ! 后下标，否则返回 {@code i}。
     */
    private static int tryParseSheetPrefix(String s, int i, int n) {
        if (i >= n) {
            return i;
        }
        if (s.charAt(i) == '\'') {
            int j = i + 1;
            while (j < n) {
                char c = s.charAt(j);
                if (c == '\'') {
                    if (j + 1 < n && s.charAt(j + 1) == '\'') {
                        j += 2;
                        continue;
                    }
                    if (j + 1 < n && s.charAt(j + 1) == '!') {
                        return j + 2;
                    }
                    return i;
                }
                j++;
            }
            return i;
        }
        int j = i;
        while (j < n && s.charAt(j) != '!') {
            char c = s.charAt(j);
            if (!isSheetNameChar(c)) {
                return i;
            }
            j++;
        }
        if (j >= n || j == i || s.charAt(j) != '!') {
            return i;
        }
        return j + 1;
    }

    private static boolean isSheetNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c >= 0x4E00 && c <= 0x9FFF;
    }

    private static int tryParseA1OrRange(String s, int i, int n) {
        int firstEnd = tryParseA1Cell(s, i, n);
        if (firstEnd <= i) {
            return i;
        }
        if (firstEnd < n && s.charAt(firstEnd) == ':') {
            int secondEnd = tryParseA1Cell(s, firstEnd + 1, n);
            if (secondEnd > firstEnd + 1) {
                return secondEnd;
            }
            return i;
        }
        return firstEnd;
    }

    private static int tryParseA1Cell(String s, int i, int n) {
        int p = i;
        if (p < n && s.charAt(p) == '$') {
            p++;
        }
        int colStart = p;
        int colEnd = parseColumnLetters(s, p, n);
        if (colEnd <= colStart) {
            return i;
        }
        p = colEnd;
        if (p < n && s.charAt(p) == '$') {
            p++;
        }
        int rowStart = p;
        int rowEnd = parseRowDigits(s, p, n);
        if (rowEnd <= rowStart) {
            return i;
        }
        return rowEnd;
    }

    private static int parseColumnLetters(String s, int i, int n) {
        int p = i;
        int count = 0;
        while (p < n && count < 3) {
            char c = s.charAt(p);
            if (!isColumnLetter(c)) {
                break;
            }
            p++;
            count++;
        }
        return count > 0 ? p : i;
    }

    private static boolean isColumnLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static int parseRowDigits(String s, int i, int n) {
        int p = i;
        if (p >= n || !Character.isDigit(s.charAt(p))) {
            return i;
        }
        while (p < n && Character.isDigit(s.charAt(p))) {
            p++;
        }
        return p;
    }
}
