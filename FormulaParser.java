import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Excel 公式单元格引用替换与提取工具。
 * <p>
 * <b>实现思路</b>：对公式做一次线性扫描（{@code char[]}），在每个位置按固定优先级尝试识别 token，
 * 不做完整语法树解析，也不使用正则，以保证大批量场景下的吞吐。
 * <p>
 * <b>扫描优先级</b>（同一 {@code pos} 从上到下，命中即前进）：
 * <ol>
 *   <li>单元格引用 — {@link #tryParseCellReference}</li>
 *   <li>函数名形态 — {@link #tryParseFunctionLikeToken}（如 {@code LOG10(}）</li>
 *   <li>字符串字面量 — {@link #skipStringLiteral}（跳过，不参与替换）</li>
 *   <li>纯字母段 — {@link #scanLetters}（如 {@code SUM}、{@code IF}，无行号）</li>
 *   <li>其它单字符 — 原样输出</li>
 * </ol>
 * <b>支持的引用形态</b>：{@code A1}、{@code $A$1}、{@code Sheet1!A1}、{@code 'My Sheet'!A1}；
 * 列最多 3 个字母，行最多 7 位数字（与 Excel 列行上限量级一致）。
 */
public final class FormulaParser {

    /** Excel 列标最大长度（XFD 为 3 个字母） */
    private static final int MAX_COLUMN_LETTERS = 3;
    /** Excel 行号最大位数（1048576 为 7 位） */
    private static final int MAX_ROW_DIGITS = 7;

    private FormulaParser() {
    }

    // -------------------------------------------------------------------------
    // 对外 API
    // -------------------------------------------------------------------------

    /**
     * 替换公式中的单元格引用。
     * <p>
     * 仅替换能通过 {@link #tryParseCellReference} 且通过边界校验的片段；
     * 映射表未命中时保留原文。
     *
     * @param formula      原始公式（可含前导 {@code =}）
     * @param replacements 替换映射；key 须与公式中的引用文本<b>完全一致</b>（含 {@code $}、表名、引号表名）
     * @return 替换后的公式
     */
    public static String replaceReferences(String formula, Map<String, String> replacements) {
        if (formula == null || formula.isEmpty() || replacements == null || replacements.isEmpty()) {
            return formula;
        }

        char[] chars = formula.toCharArray();
        int length = chars.length;
        StringBuilder out = new StringBuilder(length + 32);
        int pos = 0;

        while (pos < length) {
            // 1) 单元格引用：命中则查表替换
            int refEnd = tryParseCellReference(chars, length, pos);
            if (refEnd > pos) {
                String key = new String(chars, pos, refEnd - pos);
                String replacement = replacements.get(key);
                out.append(replacement != null ? replacement : key);
                pos = refEnd;
                continue;
            }

            // 2) LOG10( 一类：字母+数字+紧接 '('，避免被误判为列行引用
            int funcTokenEnd = tryParseFunctionLikeToken(chars, length, pos);
            if (funcTokenEnd > pos) {
                // 注意：StringBuilder.append(char[], offset, len) 第三参数是「长度」，不是结束下标
                out.append(chars, pos, funcTokenEnd - pos);
                pos = funcTokenEnd;
                continue;
            }

            char c = chars[pos];

            // 3) 双引号字符串：整体原样拷贝（其中形似 A1 的文本不处理）
            if (c == '"') {
                int stringEnd = skipStringLiteral(chars, length, pos);
                out.append(chars, pos, stringEnd - pos);
                pos = stringEnd;
                continue;
            }

            // 4) 纯字母（函数名、TRUE/FALSE 等）：无后续数字则不当引用
            if (isLetter(c)) {
                int lettersEnd = scanLetters(chars, length, pos);
                out.append(chars, pos, lettersEnd - pos);
                pos = lettersEnd;
                continue;
            }

            // 5) 运算符、括号、数字等
            out.append(c);
            pos++;
        }

        return out.toString();
    }

    /**
     * 批量替换；对每条公式独立调用 {@link #replaceReferences}。
     */
    public static String[] replaceBatch(String[] formulas, Map<String, String> replacements) {
        if (formulas == null || formulas.length == 0) {
            return formulas;
        }

        String[] result = new String[formulas.length];
        for (int i = 0; i < formulas.length; i++) {
            result[i] = replaceReferences(formulas[i], replacements);
        }
        return result;
    }

    /**
     * 提取公式中所有单元格引用。
     * <p>
     * 扫描逻辑与 {@link #replaceReferences} 一致，但命中引用时只收集、不修改文本。
     * 使用 {@link LinkedHashSet}：去重且保持首次出现顺序。
     */
    public static Set<String> extractCellReferences(String formula) {
        Set<String> refs = new LinkedHashSet<>();
        if (formula == null || formula.isEmpty()) {
            return refs;
        }

        char[] chars = formula.toCharArray();
        int length = chars.length;
        int pos = 0;

        while (pos < length) {
            if (chars[pos] == '"') {
                pos = skipStringLiteral(chars, length, pos);
                continue;
            }

            int refEnd = tryParseCellReference(chars, length, pos);
            if (refEnd > pos) {
                refs.add(new String(chars, pos, refEnd - pos));
                pos = refEnd;
                continue;
            }

            int funcTokenEnd = tryParseFunctionLikeToken(chars, length, pos);
            if (funcTokenEnd > pos) {
                pos = funcTokenEnd;
                continue;
            }

            pos++;
        }

        return refs;
    }

    // -------------------------------------------------------------------------
    // 引用解析：结构匹配 + 边界校验
    // -------------------------------------------------------------------------

    /**
     * 尝试从 {@code pos} 解析一段完整的单元格引用。
     * <p>
     * 结构（可选部分用 {@code []} 标注）：
     * <pre>
     *   [表名!]  [$] 列字母{1~3}  [$]  行数字{1~7}  [$]
     *   例：Sheet1!$A$1
     * </pre>
     * 结构匹配成功后，还需通过 {@link #isValidReferenceBoundary} 排除误识别。
     *
     * @return 引用结束下标（不含）；解析失败则返回 {@code pos}，表示当前位置不是引用起点
     */
    private static int tryParseCellReference(char[] chars, int length, int pos) {
        int cursor = pos;

        // 表名前缀：Sheet1! 或 'My Sheet'!
        int afterSheet = tryConsumeSheetPrefix(chars, length, cursor);
        if (afterSheet > cursor) {
            cursor = afterSheet;
        }

        int refStart = cursor;

        // 列前的绝对引用符：$A1
        if (cursor < length && chars[cursor] == '$') {
            cursor++;
        }

        // 列字母：A ~ XFD，至少 1 个、至多 MAX_COLUMN_LETTERS 个
        int colCount = 0;
        while (cursor < length && isLetter(chars[cursor]) && colCount < MAX_COLUMN_LETTERS) {
            colCount++;
            cursor++;
        }
        if (colCount == 0) {
            return pos;
        }

        // 列与行之间的绝对引用符：A$1
        if (cursor < length && chars[cursor] == '$') {
            cursor++;
        }

        // 行号：至少一位数字
        if (cursor >= length || !isDigit(chars[cursor])) {
            return pos;
        }

        int rowDigits = 0;
        while (cursor < length && isDigit(chars[cursor]) && rowDigits < MAX_ROW_DIGITS) {
            rowDigits++;
            cursor++;
        }

        // 行后的绝对引用符：A1$（少见，但允许）
        if (cursor < length && chars[cursor] == '$') {
            cursor++;
        }

        // 前后字符必须符合 Excel 公式中的「断开」规则
        if (!isValidReferenceBoundary(chars, length, refStart, cursor)) {
            return pos;
        }

        return cursor;
    }

    /**
     * 尝试消费工作表前缀，使 cursor 停在单元格列标起始处。
     * <p>
     * 无 {@code !} 时不消费（例如 {@code SUM} 后的 {@code (} 不会把 SUM 当表名）。
     *
     * @return 若存在合法 {@code 表名!}，返回 {@code !} 之后的位置；否则返回 {@code pos}
     */
    private static int tryConsumeSheetPrefix(char[] chars, int length, int pos) {
        if (pos >= length) {
            return pos;
        }

        // 带引号表名：'My Sheet'!
        if (chars[pos] == '\'') {
            int i = pos + 1;
            while (i < length) {
                if (chars[i] == '\'') {
                    // Excel 中用 '' 表示引号转义
                    if (i + 1 < length && chars[i + 1] == '\'') {
                        i += 2;
                        continue;
                    }
                    i++;
                    break;
                }
                i++;
            }
            if (i < length && chars[i] == '!') {
                return i + 1;
            }
            return pos;
        }

        // 无引号表名：字母/数字/下划线/点 组成，以 ! 结束
        if (!isSheetNameStart(chars[pos])) {
            return pos;
        }

        int i = pos + 1;
        while (i < length) {
            char c = chars[i];
            if (c == '!') {
                return i + 1;
            }
            if (isSheetNameChar(c)) {
                i++;
                continue;
            }
            break;
        }
        return pos;
    }

    private static boolean isSheetNameStart(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }

    private static boolean isSheetNameChar(char c) {
        return isSheetNameStart(c) || c == '.';
    }

    /**
     * 校验引用与公式其它部分的「边界」，防止误把函数名、标识符片段当成单元格。
     * <p>
     * <b>后侧</b>（紧跟引用结束位置 {@code refEnd} 的字符）：
     * <ul>
     *   <li>{@code (} → 拒绝（{@code LOG10(} 中的 {@code LOG10} 由函数 token 处理）</li>
     *   <li>字母/数字 → 拒绝（避免与后续标识符粘连）</li>
     *   <li>其它 → 须在允许集合内（运算符、{@code :} 范围、{@code )}、逗号、空白等）</li>
     * </ul>
     * <b>前侧</b>（{@code refStart} 之前，跳过空白）：
     * <ul>
     *   <li>公式开头、运算符、{@code (}、{@code ,}、{@code :}、{@code !}、{@code $} → 允许</li>
     *   <li>字母/数字/{@code )} → 拒绝（说明与左侧标识符粘连）</li>
     * </ul>
     */
    private static boolean isValidReferenceBoundary(char[] chars, int length, int refStart, int refEnd) {
        // --- 后侧检查 ---
        if (refEnd < length && chars[refEnd] == '(') {
            return false;
        }
        if (refEnd < length && (isLetter(chars[refEnd]) || isDigit(chars[refEnd]))) {
            return false;
        }
        if (refEnd < length && !isAcceptableFollowingChar(chars[refEnd])) {
            return false;
        }

        // --- 前侧检查 ---
        int before = refStart - 1;
        while (before >= 0 && isWhitespace(chars[before])) {
            before--;
        }
        if (before < 0) {
            return true;
        }

        char prev = chars[before];
        if (prev == '$' || prev == '!' || prev == ':' || prev == '(' || prev == ',') {
            return true;
        }
        if (isFormulaOperator(prev)) {
            return true;
        }
        if (isLetter(prev) || isDigit(prev) || prev == ')' || prev == '}') {
            return false;
        }

        return true;
    }

    /** 引用后允许的「分隔」字符（含范围运算符 {@code :}） */
    private static boolean isAcceptableFollowingChar(char c) {
        return c == ':' || c == ')' || c == ',' || c == ' ' || c == '\t'
                || isFormulaOperator(c);
    }

    /** 公式中的运算符及比较符相关符号 */
    private static boolean isFormulaOperator(char c) {
        switch (c) {
            case '=':
            case '+':
            case '-':
            case '*':
            case '/':
            case '^':
            case '&':
            case '<':
            case '>':
            case '%':
                return true;
            default:
                return false;
        }
    }

    // -------------------------------------------------------------------------
    // 辅助扫描
    // -------------------------------------------------------------------------

    /**
     * 跳过双引号字符串字面量，返回结束下标（不含）。
     * <p>
     * 约定：{@code pos} 指向开头的 {@code "}；支持 {@code ""} 转义为一个引号字符。
     */
    private static int skipStringLiteral(char[] chars, int length, int pos) {
        int i = pos + 1;
        while (i < length) {
            if (chars[i] == '"') {
                if (i + 1 < length && chars[i + 1] == '"') {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return length;
    }

    /**
     * 识别「字母 + 数字 + 紧接左括号」的函数名 token（如 {@code LOG10}）。
     * <p>
     * 仅当数字段之后<b>直接</b>是 {@code (} 时成立，返回括号下标（不含）；
     * 否则返回 {@code pos}，交由后续逻辑处理（例如 {@code B5} 后接 {@code *} 仍按引用解析）。
     */
    private static int tryParseFunctionLikeToken(char[] chars, int length, int pos) {
        if (pos >= length || !isLetter(chars[pos])) {
            return pos;
        }

        int afterLetters = scanLetters(chars, length, pos);
        if (afterLetters >= length || !isDigit(chars[afterLetters])) {
            return pos;
        }

        int i = afterLetters;
        while (i < length && isDigit(chars[i])) {
            i++;
        }
        if (i < length && chars[i] == '(') {
            return i;
        }
        return pos;
    }

    /**
     * 扫描连续字母，返回结束下标（不含）。
     * 用于纯函数名 {@code SUM}、{@code IF} 等（无行号部分）。
     */
    private static int scanLetters(char[] chars, int length, int pos) {
        int i = pos + 1;
        while (i < length && isLetter(chars[i])) {
            i++;
        }
        return i;
    }

    private static boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }
}
