package com.wyl.excelformula;

import java.util.regex.Pattern;

/**
 * Excel坐标转换工具（JDK21）。
 * 提供“类Excel公式坐标”（如 B5）与数字坐标（如 [1,4]）的双向高性能转换。
 * 坐标约定：
 * - 列：0基准（A=0, B=1, ..., AA=26）
 * - 行：0基准（1→0, 2→1, ...）
 */
public final class ExcelCoordinateUtil {

    private ExcelCoordinateUtil() {}

    /**
     * 预编译正则：匹配 Excel 单元格引用，如 B5、A1。
     * 不匹配纯数字或纯字母，以避免误判常量或函数名。
     */
    private static final Pattern CELL_PATTERN = Pattern.compile("\\b([A-Za-z]+)(\\d+)\\b");

    /**
     * 将 Excel 列名（如 "A", "Z", "AA"）转换为 0 基准列索引。
     *
     * @param columnLetter 大小写不敏感的列名（A-Z）
     * @return 0 基准列索引；非法格式抛出 IllegalArgumentException
     */
    public static int columnLetterToIndex(String columnLetter) {
        if (columnLetter == null || columnLetter.isEmpty()) {
            throw new IllegalArgumentException("columnLetter must not be null or empty");
        }
        int result = 0;
        for (int i = 0; i < columnLetter.length(); i++) {
            char ch = Character.toUpperCase(columnLetter.charAt(i));
            if (ch < 'A' || ch > 'Z') {
                throw new IllegalArgumentException("Invalid column letter: " + columnLetter);
            }
            result = result * 26 + (ch - 'A' + 1);
        }
        return result - 1;
    }

    /**
     * 将 0 基准列索引转换为 Excel 列名。
     *
     * @param columnIndex 0 基准列索引（>=0）
     * @return Excel 列名（如 A, B, AA）
     */
    public static String columnIndexToLetter(int columnIndex) {
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex must be >= 0");
        }
        StringBuilder sb = new StringBuilder();
        int n = columnIndex + 1; // 转为 1 基准，便于 26 进制处理
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26;
        }
        return sb.toString();
    }

    /**
     * 将 Excel 行号（从 1 开始）转换为 0 基准行索引。
     *
     * @param rowNumber 行号（>=1）
     * @return 0 基准行索引
     */
    public static int rowNumberToIndex(int rowNumber) {
        if (rowNumber < 1) {
            throw new IllegalArgumentException("rowNumber must be >= 1");
        }
        return rowNumber - 1;
    }

    /**
     * 将 0 基准行索引转换为 Excel 行号（从 1 开始）。
     *
     * @param rowIndex 0 基准行索引（>=0）
     * @return 行号
     */
    public static int rowIndexToNumber(int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must be >= 0");
        }
        return rowIndex + 1;
    }

    /**
     * 将 Excel 单元格坐标（如 "B5"）转换为数字坐标 [列索引, 行索引]。
     *
     * @param cellRef 单元格引用（如 B5）
     * @return int[2]，第0位为列索引（0基准），第1位为行索引（0基准）
     * @throws IllegalArgumentException 格式非法时抛出
     */
    public static int[] toNumericCoordinates(String cellRef) {
        if (cellRef == null || cellRef.isEmpty()) {
            throw new IllegalArgumentException("cellRef must not be null or empty");
        }
        var m = CELL_PATTERN.matcher(cellRef);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid cell reference format: " + cellRef);
        }
        String col = m.group(1);
        String row = m.group(2);
        int colIndex = columnLetterToIndex(col);
        int rowIndex = rowNumberToIndex(Integer.parseInt(row));
        return new int[]{colIndex, rowIndex};
    }

    /**
     * 将数字坐标 [列索引, 行索引] 转换为 Excel 单元格坐标（如 "B5"）。
     *
     * @param coordinates 数组 [列索引, 行索引]，均为 0 基准
     * @return Excel 单元格引用（如 B5）
     */
    public static String toCellReference(int[] coordinates) {
        if (coordinates == null || coordinates.length != 2) {
            throw new IllegalArgumentException("coordinates must be int[2]");
        }
        String col = columnIndexToLetter(coordinates[0]);
        int row = rowIndexToNumber(coordinates[1]);
        return col + row;
    }

    /**
     * 将 Excel 单元格坐标（如 "B5"）转换为列索引与行索引（均为 0 基准）。
     *
     * @param cellRef 单元格引用（如 B5）
     * @return 2元数组 [列索引, 行索引]
     */
    public static int[] toIndexCoordinates(String cellRef) {
        return toNumericCoordinates(cellRef);
    }

    /**
     * 将列索引与行索引（均为 0 基准）转换为 Excel 单元格坐标（如 "B5"）。
     *
     * @param colIndex 列索引（0基准）
     * @param rowIndex 行索引（0基准）
     * @return Excel 单元格引用（如 B5）
     */
    public static String fromIndexCoordinates(int colIndex, int rowIndex) {
        return toCellReference(new int[]{colIndex, rowIndex});
    }

    /**
     * 快速判断字符串是否符合 Excel 单元格引用格式（字母+数字，不含函数调用上下文）。
     * 注意：本工具不进行公式上下文感知，简单匹配即可满足基础转换需求。
     *
     * @param str 待检测字符串
     * @return 是否符合基础单元格引用格式
     */
    public static boolean isCellReferenceFormat(String str) {
        return str != null && CELL_PATTERN.matcher(str).matches();
    }
}