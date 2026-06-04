package com.od.formula;

import java.util.Objects;

/**
 * Excel 风格单元格坐标与数字坐标互转（面向 JDK 21 热路径优化）。
 * <p>
 * Excel 坐标形如 {@code B5}、{@code $G$6}（{@code $} 会被忽略）。<br>
 * 数字坐标为长度为 2 的 {@code int[]}：{@code [0]} 为行索引，{@code [1]} 为列索引，均从 0 起算。
 * 例如 {@code A1} ↔ {@code [0, 0]}，{@code B5} ↔ {@code [4, 1]}。
 * <p>
 * 高频批量转换请使用 {@link Buffer} 复用内部数组，避免重复分配。
 */
public final class CellCoordinateConverter {

    /** Excel 最大列数（XFD）。 */
    public static final int MAX_COLUMN_INDEX = 16_383;
    /** Excel 最大行数。 */
    public static final int MAX_ROW_INDEX = 1_048_575;
    /** 数字坐标数组长度。 */
    public static final int NUMERIC_COORDINATE_LENGTH = 2;
    /** Excel 列名最大长度（XFD）。 */
    private static final int MAX_COLUMN_NAME_LENGTH = 3;
    /** 最长地址长度：{@code XFD1048576}。 */
    private static final int MAX_ADDRESS_LENGTH = MAX_COLUMN_NAME_LENGTH + 7;

    private static final int[] COLUMN_NAME_OFFSETS = new int[MAX_COLUMN_INDEX + 1];
    private static final byte[] COLUMN_NAME_LENGTHS = new byte[MAX_COLUMN_INDEX + 1];
    private static final char[] COLUMN_NAME_DATA;

    static {
        int totalChars = 0;
        for (int columnIndex = 0; columnIndex <= MAX_COLUMN_INDEX; columnIndex++) {
            COLUMN_NAME_OFFSETS[columnIndex] = totalChars;
            int length = computeColumnNameLength(columnIndex);
            COLUMN_NAME_LENGTHS[columnIndex] = (byte) length;
            totalChars += length;
        }
        COLUMN_NAME_DATA = new char[totalChars];
        for (int columnIndex = 0; columnIndex <= MAX_COLUMN_INDEX; columnIndex++) {
            writeColumnName(columnIndex, COLUMN_NAME_DATA, COLUMN_NAME_OFFSETS[columnIndex]);
        }
    }

    private CellCoordinateConverter() {
    }

    /**
     * 将 Excel 坐标转为数字坐标 {@code [行索引, 列索引]}。
     */
    public static int[] toNumeric(String excelAddress) {
        int[] numeric = new int[NUMERIC_COORDINATE_LENGTH];
        parseNumeric(excelAddress, numeric, 0);
        return numeric;
    }

    /**
     * 解析 Excel 坐标并写入调用方提供的缓冲区，避免分配 {@code int[]}。
     *
     * @param excelAddress Excel 地址
     * @param dest         至少长度为 2 的目标数组
     * @param destOffset   写入起始下标，结果写入 {@code dest[destOffset]}（行）与 {@code dest[destOffset + 1]}（列）
     */
    public static void parseNumeric(String excelAddress, int[] dest, int destOffset) {
        Objects.requireNonNull(excelAddress, "excelAddress");
        Objects.requireNonNull(dest, "dest");
        if (dest.length - destOffset < NUMERIC_COORDINATE_LENGTH) {
            throw new IllegalArgumentException("dest must hold at least 2 elements from destOffset");
        }

        int index = 0;
        int length = excelAddress.length();
        if (length == 0) {
            throw invalidAddress(excelAddress);
        }

        index = skipChar(excelAddress, index, length, '$');
        int columnStart = index;
        while (index < length && isColumnLetter(excelAddress.charAt(index))) {
            index++;
        }
        int columnLength = index - columnStart;
        if (columnLength == 0 || columnLength > MAX_COLUMN_NAME_LENGTH) {
            throw invalidAddress(excelAddress);
        }

        index = skipChar(excelAddress, index, length, '$');
        int rowStart = index;
        while (index < length && isDigit(excelAddress.charAt(index))) {
            index++;
        }
        if (rowStart == index || index != length) {
            throw invalidAddress(excelAddress);
        }

        int columnIndex = parseColumnByLength(excelAddress, columnStart, columnLength);
        int rowIndex = parseRowByLength(excelAddress, rowStart, index - rowStart);
        if (!isValidIndices(rowIndex, columnIndex)) {
            throw invalidAddress(excelAddress);
        }

        dest[destOffset] = rowIndex;
        dest[destOffset + 1] = columnIndex;
    }

    /**
     * 将数字坐标转为 Excel 坐标（大写列字母）。
     */
    public static String toExcel(int[] numeric) {
        Objects.requireNonNull(numeric, "numeric");
        if (numeric.length != NUMERIC_COORDINATE_LENGTH) {
            throw new IllegalArgumentException(
                    "numeric length must be " + NUMERIC_COORDINATE_LENGTH + ", got " + numeric.length);
        }
        return toExcel(numeric[0], numeric[1]);
    }

    /**
     * 将行、列索引转为 Excel 坐标（大写列字母）。
     */
    public static String toExcel(int rowIndex, int columnIndex) {
        if (!isValidIndices(rowIndex, columnIndex)) {
            throw rowIndex < 0 || rowIndex > MAX_ROW_INDEX
                    ? new IllegalArgumentException("rowIndex out of range: " + rowIndex)
                    : new IllegalArgumentException("columnIndex out of range: " + columnIndex);
        }
        char[] buffer = new char[MAX_ADDRESS_LENGTH];
        int length = formatAddress(rowIndex, columnIndex, buffer, 0);
        return new String(buffer, 0, length);
    }

    /**
     * 将行、列索引写入缓冲区并返回写入长度，供批量场景复用 {@link Buffer}。
     */
    public static int formatAddress(int rowIndex, int columnIndex, char[] buffer, int offset) {
        if (!isValidIndices(rowIndex, columnIndex)) {
            throw rowIndex < 0 || rowIndex > MAX_ROW_INDEX
                    ? new IllegalArgumentException("rowIndex out of range: " + rowIndex)
                    : new IllegalArgumentException("columnIndex out of range: " + columnIndex);
        }
        int columnOffset = COLUMN_NAME_OFFSETS[columnIndex];
        int columnLength = COLUMN_NAME_LENGTHS[columnIndex] & 0xFF;
        System.arraycopy(COLUMN_NAME_DATA, columnOffset, buffer, offset, columnLength);
        return writeRowNumber(rowIndex, buffer, offset + columnLength);
    }

    /**
     * 可复用缓冲，适用于循环中的大批量坐标转换。
     */
    public static final class Buffer {

        private final int[] numeric = new int[NUMERIC_COORDINATE_LENGTH];
        private final char[] address = new char[MAX_ADDRESS_LENGTH];

        /**
         * 解析 Excel 地址，结果保存在内部 {@code int[2]} 中。
         *
         * @return 内部数字坐标数组（勿长期持有引用，下次调用会被覆盖）
         */
        public int[] parse(String excelAddress) {
            parseNumeric(excelAddress, numeric, 0);
            return numeric;
        }

        /**
         * 将内部数字坐标格式化为 Excel 地址字符串。
         */
        public String format(int rowIndex, int columnIndex) {
            int length = formatAddress(rowIndex, columnIndex, address, 0);
            return new String(address, 0, length);
        }

        /**
         * 将内部数字坐标格式化为 Excel 地址字符串。
         */
        public String format() {
            return format(numeric[0], numeric[1]);
        }
    }

    private static int parseColumnByLength(String text, int start, int length) {
        int first = toUpperAscii(text.charAt(start));
        if (first < 'A' || first > 'Z') {
            return -1;
        }
        return switch (length) {
            case 1 -> first - 'A';
            case 2 -> {
                int second = toUpperAscii(text.charAt(start + 1));
                if (second < 'A' || second > 'Z') {
                    yield -1;
                }
                int columnIndex = (first - 'A' + 1) * 26 + (second - 'A');
                yield columnIndex > MAX_COLUMN_INDEX ? -1 : columnIndex;
            }
            case 3 -> {
                int second = toUpperAscii(text.charAt(start + 1));
                int third = toUpperAscii(text.charAt(start + 2));
                if (second < 'A' || second > 'Z' || third < 'A' || third > 'Z') {
                    yield -1;
                }
                int columnIndex = (first - 'A' + 1) * 676 + (second - 'A' + 1) * 26 + (third - 'A');
                yield columnIndex > MAX_COLUMN_INDEX ? -1 : columnIndex;
            }
            default -> -1;
        };
    }

    private static int parseRowByLength(String text, int start, int length) {
        int rowNumber = 0;
        for (int i = 0; i < length; i++) {
            char digit = text.charAt(start + i);
            if (!isDigit(digit)) {
                return -1;
            }
            rowNumber = rowNumber * 10 + (digit - '0');
            if (rowNumber > MAX_ROW_INDEX + 1) {
                return -1;
            }
        }
        if (rowNumber < 1) {
            return -1;
        }
        return rowNumber - 1;
    }

    private static int writeRowNumber(int rowIndex, char[] buffer, int offset) {
        int rowNumber = rowIndex + 1;
        if (rowNumber < 10) {
            buffer[offset] = (char) ('0' + rowNumber);
            return offset + 1;
        }
        int digitCount = rowNumber < 100
                ? 2
                : rowNumber < 1_000
                        ? 3
                        : rowNumber < 10_000
                                ? 4
                                : rowNumber < 100_000
                                        ? 5
                                        : rowNumber < 1_000_000
                                                ? 6
                                                : 7;
        int writeIndex = offset + digitCount;
        int end = writeIndex;
        while (rowNumber > 0) {
            buffer[--writeIndex] = (char) ('0' + (rowNumber % 10));
            rowNumber /= 10;
        }
        return end;
    }

    private static int computeColumnNameLength(int columnIndex) {
        int oneBased = columnIndex + 1;
        int length = 0;
        while (oneBased > 0) {
            oneBased--;
            length++;
            oneBased /= 26;
        }
        return length;
    }

    private static void writeColumnName(int columnIndex, char[] data, int offset) {
        int oneBased = columnIndex + 1;
        int length = computeColumnNameLength(columnIndex);
        int writeIndex = offset + length;
        while (oneBased > 0) {
            oneBased--;
            data[--writeIndex] = (char) ('A' + (oneBased % 26));
            oneBased /= 26;
        }
    }

    private static int skipChar(String text, int index, int length, char expected) {
        while (index < length && text.charAt(index) == expected) {
            index++;
        }
        return index;
    }

    private static boolean isValidIndices(int rowIndex, int columnIndex) {
        return rowIndex >= 0
                && rowIndex <= MAX_ROW_INDEX
                && columnIndex >= 0
                && columnIndex <= MAX_COLUMN_INDEX;
    }

    private static IllegalArgumentException invalidAddress(String excelAddress) {
        return new IllegalArgumentException("invalid excel address: " + excelAddress);
    }

    private static int toUpperAscii(char letter) {
        return letter & 0xDF;
    }

    private static boolean isColumnLetter(char character) {
        int upper = character & 0xDF;
        return upper >= 'A' && upper <= 'Z';
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }
}
