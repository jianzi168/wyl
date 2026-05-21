package com.wyl.excelformula;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Excel单元格坐标转换高性能实现（JDK21）
 * - Excel列名→0基准索引（A→0，Z→25，AA→26，B→1）
 * - 行号串→0基准索引
 * - 支持 B2→(2,2) 或 [2,2]
 * - 公式上下文解析，避免函数名误判
 */
public final class ExcelCoordinateUtil {

    // 预编译正则：匹配连续字母后跟连续数字，如 B2、AA123
    private static final java.util.regex.Pattern CELL_PATTERN = java.util.regex.Pattern.compile("\\b([A-Za-z]+)(\\d+)\\b");

    // 防止实例化
    private ExcelCoordinateUtil() {}

    // Excel列名→0基准索引（A→0，Z→25，AA→26，B→1）
    public static int columnLetterToIndex(String columnLetter) {
        int result = 0;
        int length = columnLetter.length();
        for (int i = 0; i < length; i++) {
            char ch = Character.toUpperCase(columnLetter.charAt(i));
            result = result * 26 + (ch - 'A' + 1);
        }
        return result - 1;
    }

    // 0基准索引→Excel列名（0→A，1→B，25→Z，26→AA）
    public static String columnIndexToLetter(int columnIndex) {
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex must be >= 0");
        }
        StringBuilder sb = new StringBuilder();
        columnIndex++; // 转为1基数便于26进制转换
        while (columnIndex > 0) {
            columnIndex--;
            sb.insert(0, (char) ('A' + columnIndex % 26));
            columnIndex = columnIndex / 26;
        }
        return sb.toString();
    }

    // 行号串→0基准索引（"2"→1）
    public static int rowToIndex(String row) {
        if (row == null || row.isEmpty()) {
            throw new IllegalArgumentException("row cannot be null or empty");
        }
        return Integer.parseInt(row) - 1;
    }

    // 0基准索引→行号串（1→"2"）
    public static String indexToRow(int rowIndex) {
        return String.valueOf(rowIndex + 1);
    }

    // B2→int[2]{col,row}（0基准索引）
    public static int[] excelToIndices(String cell) {
        java.util.regex.Matcher m = CELL_PATTERN.matcher(cell);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid Excel cell reference: " + cell);
        }
        String column = m.group(1);
        String row = m.group(2);
        return new int[]{columnLetterToIndex(column), rowToIndex(row)};
    }

    // int[2]{col,row}→B2（0基准索引）
    public static String indicesToExcel(int col, int row) {
        return columnIndexToLetter(col) + indexToRow(row);
    }

    // B2→[2,2]（1基数）
    public static String excelToPair(String cell) {
        int[] indices = excelToIndices(cell);
        return "[" + (indices[0] + 1) + "," + (indices[1] + 1) + "]";
    }

    // [2,2]或(2,2)→B2（1基数）
    public static String pairToExcel(String pair) {
        String normalized = pair.replace("[", "(").replace("]", ")");
        if (!normalized.matches("\\(\\d+,\\d+\\)")) {
            throw new IllegalArgumentException("Invalid pair format, expected (row,col) or [row,col]: " + pair);
        }
        String[] parts = normalized.substring(1, normalized.length() - 1).split(",");
        int col = Integer.parseInt(parts[0].trim()) - 1; // 按你描述：B2→2,2 表示先列后行（第2列第2行）
        int row = Integer.parseInt(parts[1].trim()) - 1;
        return indicesToExcel(col, row);
    }

    // 公式上下文感知解析：提取所有单元格引用，避免函数名误判
    public static Set<String> extractCellReferencesFromFormula(String formula) {
        Set<String> refs = new LinkedHashSet<>();
        if (formula == null || formula.isEmpty()) return refs;
        java.util.regex.Matcher m = CELL_PATTERN.matcher(formula);
        while (m.find()) {
            String candidate = m.group(1) + m.group(2);
            if (!isLikelyFunctionName(formula, m.start())) {
                refs.add(candidate);
            }
        }
        return refs;
    }

    // 公式中批量替换单元格引用（高性能，避免误改函数名）
    public static String replaceCellReferencesInFormula(String formula, Map<String, String> replacements) {
        if (formula == null || formula.isEmpty() || replacements == null || replacements.isEmpty()) {
            return formula;
        }
        java.util.regex.Matcher m = CELL_PATTERN.matcher(formula);
        StringBuffer sb = new StringBuffer(formula.length() + 32);
        while (m.find()) {
            String candidate = m.group(1) + m.group(2);
            if (!isLikelyFunctionName(formula, m.start())) {
                String repl = replacements.get(candidate);
                m.appendReplacement(sb, repl != null ? java.util.regex.Matcher.quoteReplacement(repl) : candidate);
            } else {
                m.appendReplacement(sb, candidate);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // 上下文感知判断：函数名（SUM/IF/MAX/MIN/VLOOKUP等）排除
    private static boolean isLikelyFunctionName(String formula, int endIndex) {
        if (endIndex < 1 || formula.charAt(endIndex - 1) != '(') {
            return false;
        }
        int i = endIndex - 2;
        while (i >= 0 && isLetter(formula.charAt(i))) {
            i--;
        }
        String potential = formula.substring(i + 1, endIndex - 1).toUpperCase();
        return EXCEL_FUNCTIONS.contains(potential);
    }

    private static boolean isLetter(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    // 常见Excel函数名白名单（可扩展）
    private static final Set<String> EXCEL_FUNCTIONS = Set.of(
            "SUM", "AVERAGE", "IF", "MAX", "MIN", "VLOOKUP", "HLOOKUP", "COUNT", "COUNTA",
            "ROUND", "ROUNDUP", "ROUNDDOWN", "INT", "ABS", "POWER", "SQRT", "MOD", "LEN",
            "LEFT", "RIGHT", "MID", "UPPER", "LOWER", "PROPER", "TRIM", "CONCATENATE",
            "INDEX", "MATCH", "OFFSET", "INDIRECT", "ADDRESS", "ROW", "COLUMN", "CELL",
            "TODAY", "NOW", "DATE", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND",
            "TEXT", "VALUE", "NUMBERVALUE", "ERROR.TYPE", "ISERROR", "ISNA", "ISBLANK",
            "AND", "OR", "NOT", "IFERROR", "IFS", "SWITCH", "CHOOSE", "LOOKUP"
    );
}