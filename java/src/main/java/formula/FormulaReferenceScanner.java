package formula;

import java.util.ArrayList;
import java.util.Objects;

/**
 * 公式引用扫描器：在线性扫描中识别 Excel 类单元格引用（本表 A1、区域、跨表 {@code 表名!A1}）。
 *
 * <p>与 {@link FormulaCoordinateReplacer} 共用同一套词法规则，保证「哪些是引用」的判定一致。
 * 回显模块使用 {@link #scanLocalRefCountOrCrossSheet} 做单次扫描校验；替换模块使用
 * {@link #collectLocalReferenceSpans} 收集引用区间。
 *
 * <p>实现上不使用正则，全程 {@code charAt} 遍历，跳过双引号字符串字面量内的假引用。
 */
public final class FormulaReferenceScanner {

    private FormulaReferenceScanner() {
    }

    /**
     * 是否含跨表引用（如 {@code 表单2!B6}），此类公式不可回显。
     */
    public static boolean containsCrossSheetReference(String formula) {
        Objects.requireNonNull(formula);
        int n = formula.length();
        int i = 0;
        while (i < n) {
            char c = formula.charAt(i);
            if (c == '"') {
                i = skipDoubleQuotedString(formula, i, n);
                continue;
            }
            RefToken token = tryParseReference(formula, i, n);
            if (token != null) {
                if (token.sheetStart() >= 0) {
                    return true;
                }
                i = token.end();
                continue;
            }
            i++;
        }
        return false;
    }

    /**
     * 按从左到右扫描顺序收集本表单元格引用（不含跨表、不含区域两端拆分）。
     */
    public static int[] collectLocalReferenceSpans(String formula, ArrayList<RefSpan> out) {
        Objects.requireNonNull(formula);
        Objects.requireNonNull(out);
        out.clear();
        int n = formula.length();
        int i = 0;
        while (i < n) {
            char c = formula.charAt(i);
            if (c == '"') {
                i = skipDoubleQuotedString(formula, i, n);
                continue;
            }
            RefToken token = tryParseReference(formula, i, n);
            if (token == null) {
                i++;
                continue;
            }
            if (token.sheetStart() < 0) {
                out.add(new RefSpan(token.refStart(), token.end()));
            }
            i = token.end();
        }
        int[] spans = new int[out.size() * 2];
        for (int j = 0; j < out.size(); j++) {
            RefSpan s = out.get(j);
            spans[j * 2] = s.start();
            spans[j * 2 + 1] = s.end();
        }
        return spans;
    }

    public static int countLocalReferences(String formula) {
        ArrayList<RefSpan> buf = new ArrayList<>(8);
        collectLocalReferenceSpans(formula, buf);
        return buf.size();
    }

    /**
     * 回显专用：对公式做<strong>单次线性扫描</strong>，同时完成跨表检测与本表引用计数。
     *
     * <p>背景：旧逻辑先 {@link #containsCrossSheetReference} 再 {@link #collectLocalReferenceSpans}，
     * 同一公式扫两遍。打开表单时公式量大，合并扫描可减少约一半字符遍历。
     *
     * <p>返回值约定：
     * <ul>
     *   <li>{@code -1} — 发现跨表引用（如 {@code 表单2!B6}），该公式不允许回显</li>
     *   <li>{@code >= 0} — 本表引用个数（区域引用如 {@code C4:C8} 计为 1 个 token），
     *       须与数据表 {@code dag} 长度一致才继续校验</li>
     * </ul>
     *
     * <p>不分配 {@link RefSpan} 列表，仅计数，降低 GC。
     */
    public static int scanLocalRefCountOrCrossSheet(String formula) {
        Objects.requireNonNull(formula);
        int localCount = 0;
        int n = formula.length();
        int i = 0;
        while (i < n) {
            char c = formula.charAt(i);
            // 字符串字面量内的 "B3" 不是单元格引用，整段跳过
            if (c == '"') {
                i = skipDoubleQuotedString(formula, i, n);
                continue;
            }
            RefToken token = tryParseReference(formula, i, n);
            if (token == null) {
                i++;
                continue;
            }
            // sheetStart >= 0 表示 token 含「表名!」前缀
            if (token.sheetStart() >= 0) {
                return -1;
            }
            localCount++;
            i = token.end();
        }
        return localCount;
    }

    public record RefSpan(int start, int end) {
    }

    private record RefToken(int start, int end, int sheetStart, int refStart) {
    }

    private static int skipDoubleQuotedString(String s, int start, int n) {
        int i = start + 1;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '"') {
                if (i + 1 < n && s.charAt(i + 1) == '"') {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return n;
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

    private static boolean followedByReferenceBoundary(String s, int pos, int n) {
        if (pos >= n) {
            return true;
        }
        char c = s.charAt(pos);
        return c == ')' || c == '(' || c == ',' || c == '+' || c == '-' || c == '*'
                || c == '/' || c == '^' || c == '&' || c == '=' || c == '<' || c == '>'
                || c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

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
