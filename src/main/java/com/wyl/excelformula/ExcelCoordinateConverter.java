package com.wyl.excelformula;

import java.util.HashMap;
import java.util.Map;

/**
 * Excel坐标转换工具类 - 高性能实现
 * <p>
 * 功能：
 * 1. 字母坐标转数字坐标: B5 -> [2,5]
 * 2. 数字坐标转字母坐标: [2,5] -> B5
 * <p>
 * 性能优化：
 * - 使用缓存机制避免重复计算
 * - 位运算优化字母坐标转换
 * - 预计算常用映射（A-Z）
 * <p>
 * JDK 21特性：
 * - 使用record作为数据载体
 *
 * @author wyl
 */
public final class ExcelCoordinateConverter {

    // ========== 常量定义 ==========

    /** 缓存容量 */
    private static final int CACHE_SIZE = 128;

    /** 字母'A'的ASCII码 */
    private static final int CHAR_A = 'A';

    /** 字母'Z'的ASCII码 */
    private static final int CHAR_Z = 'Z';

    /** 26进制基数 */
    private static final int BASE = 26;

    /** 最大缓存键长度（对应ZZZ列 = 18278列） */
    private static final int MAX_CACHE_KEY_LENGTH = 3;

    // ========== 预计算缓存 ==========

    /** 单字母缓存: A->1, B->2, ..., Z->26 */
    private static final int[] SINGLE_LETTER_CACHE = new int[CHAR_Z - CHAR_A + 1];

    /** 数字转单字母缓存: 1->A, 2->B, ..., 26->Z */
    private static final char[] DIGIT_TO_LETTER_CACHE = new char[BASE + 1];

    /** 字母坐标转数字坐标缓存 */
    private static final Map<String, Coordinate> LETTER_TO_NUMBER_CACHE = new HashMap<>(CACHE_SIZE);

    /** 数字坐标转字母坐标缓存 */
    private static final Map<String, String> NUMBER_TO_LETTER_CACHE = new HashMap<>(CACHE_SIZE);

    static {
        // 预计算单字母映射
        for (int i = 0; i <= CHAR_Z - CHAR_A; i++) {
            SINGLE_LETTER_CACHE[i] = i + 1;
            DIGIT_TO_LETTER_CACHE[i + 1] = (char) (CHAR_A + i);
        }
    }

    // ========== 数据结构 ==========

    /**
     * 坐标记录（JDK 21 record特性）
     *
     * @param col 列索引（从1开始）
     * @param row 行索引（从1开始）
     */
    public record Coordinate(int col, int row) {

        @Override
        public String toString() {
            return "[%d,%d]".formatted(col, row);
        }
    }

    // ========== 公共方法 ==========

    /**
     * 字母坐标转数字坐标
     * <p>
     * 示例: B5 -> Coordinate(2, 5)
     * <p>
     * 性能优化：
     * - 缓存命中直接返回
     * - 使用位运算处理字符转换
     * - 预计算单字母映射
     *
     * @param letterCoord 字母坐标（如B5、AA123）
     * @return 坐标对象（列从1开始，行从1开始）
     * @throws IllegalArgumentException 坐标格式错误
     */
    public static Coordinate letterToNumber(String letterCoord) {
        if (letterCoord == null || letterCoord.isEmpty()) {
            throw new IllegalArgumentException("字母坐标不能为空");
        }

        // 检查缓存
        Coordinate cached = LETTER_TO_NUMBER_CACHE.get(letterCoord);
        if (cached != null) {
            return cached;
        }

        // 解析坐标
        int i = 0;
        int col = 0;
        final int len = letterCoord.length();

        // 解析列字母部分
        while (i < len && isUpperCaseLetter(letterCoord.charAt(i))) {
            char c = letterCoord.charAt(i);
            // 使用位运算：c - 'A' 替代 ASCII转换
            int digit = (c & 0x1F); // 大写字母ASCII码的低5位就是 A=1, B=2, ...
            col = col * BASE + digit;
            i++;
        }

        if (i == 0) {
            throw new IllegalArgumentException("字母坐标缺少列部分: " + letterCoord);
        }

        // 解析行数字部分
        if (i >= len) {
            throw new IllegalArgumentException("字母坐标缺少行部分: " + letterCoord);
        }

        int row = 0;
        while (i < len) {
            char c = letterCoord.charAt(i);
            if (!Character.isDigit(c)) {
                throw new IllegalArgumentException("字母坐标格式错误: " + letterCoord);
            }
            row = row * 10 + (c - '0');
            i++;
        }

        if (row == 0) {
            throw new IllegalArgumentException("行号必须大于0: " + letterCoord);
        }

        Coordinate result = new Coordinate(col, row);

        // 缓存结果（只缓存较短的键）
        if (letterCoord.length() <= MAX_CACHE_KEY_LENGTH + 4) {
            LETTER_TO_NUMBER_CACHE.put(letterCoord, result);
        }

        return result;
    }

    /**
     * 数字坐标转字母坐标
     * <p>
     * 示例: Coordinate(2, 5) -> "B5"
     * <p>
     * 性能优化：
     * - 缓存命中直接返回
     * - 预计算常用映射
     * - 使用StringBuilder优化字符串拼接
     *
     * @param col 列索引（从1开始）
     * @param row 行索引（从1开始）
     * @return 字母坐标（如B5、AA123）
     * @throws IllegalArgumentException 坐标越界
     */
    public static String numberToLetter(int col, int row) {
        if (col <= 0 || row <= 0) {
            throw new IllegalArgumentException("列号和行号必须大于0: col=%d, row=%d".formatted(col, row));
        }

        String cacheKey = "%d,%d".formatted(col, row);
        String cached = NUMBER_TO_LETTER_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 列转字母（类似26进制转换，但没有0）
        StringBuilder sb = new StringBuilder();
        int temp = col;

        while (temp > 0) {
            temp--; // 转换为0-based（A=0, B=1, ...）
            int remainder = temp % BASE;
            sb.append(DIGIT_TO_LETTER_CACHE[remainder + 1]);
            temp = temp / BASE;
        }

        sb.reverse();

        // 添加行号
        sb.append(row);

        String result = sb.toString();

        // 缓存结果
        if (col <= 1000) { // 只缓存较小的列号
            NUMBER_TO_LETTER_CACHE.put(cacheKey, result);
        }

        return result;
    }

    /**
     * 数字坐标转字母坐标（重载版本）
     *
     * @param coord 坐标对象
     * @return 字母坐标
     */
    public static String numberToLetter(Coordinate coord) {
        return numberToLetter(coord.col(), coord.row());
    }

    /**
     * 字母坐标转数字坐标数组（兼容旧API）
     *
     * @param letterCoord 字母坐标
     * @return int数组 [列, 行]
     */
    public static int[] letterToIntArray(String letterCoord) {
        Coordinate coord = letterToNumber(letterCoord);
        return new int[]{coord.col(), coord.row()};
    }

    /**
     * 数字坐标数组转字母坐标（兼容旧API）
     *
     * @param coords 坐标数组 [列, 行]
     * @return 字母坐标
     */
    public static String intArrayToLetter(int[] coords) {
        if (coords == null || coords.length != 2) {
            throw new IllegalArgumentException("坐标数组必须包含2个元素");
        }
        return numberToLetter(coords[0], coords[1]);
    }

    // ========== 私有方法 ==========

    /**
     * 判断是否为大写字母
     * <p>
     * 位运算优化：大写字母ASCII范围是 65-90，二进制 01000001-01011010
     * 判断逻辑：是否在 A-Z 之间
     */
    private static boolean isUpperCaseLetter(char c) {
        return c >= CHAR_A && c <= CHAR_Z;
    }

    // ========== 缓存管理 ==========

    /**
     * 清除所有缓存
     */
    public static void clearCache() {
        LETTER_TO_NUMBER_CACHE.clear();
        NUMBER_TO_LETTER_CACHE.clear();
    }

    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        return "letterToNumber缓存: %d, numberToLetter缓存: %d".formatted(
                LETTER_TO_NUMBER_CACHE.size(),
                NUMBER_TO_LETTER_CACHE.size()
        );
    }
}