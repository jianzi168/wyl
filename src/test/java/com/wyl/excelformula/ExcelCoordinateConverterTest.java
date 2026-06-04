package com.wyl.excelformula;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelCoordinateConverter 单元测试
 *
 * @author wyl
 */
@DisplayName("Excel坐标转换工具类测试")
class ExcelCoordinateConverterTest {

    // ========== letterToNumber 测试 ==========

    @Test
    @DisplayName("单字母坐标转换 - 基础测试")
    void testLetterToNumber_SingleLetter() {
        assertEquals(1, ExcelCoordinateConverter.letterToNumber("A1").col());
        assertEquals(2, ExcelCoordinateConverter.letterToNumber("B1").col());
        assertEquals(26, ExcelCoordinateConverter.letterToNumber("Z1").col());
    }

    @Test
    @DisplayName("单字母坐标转换 - 行号测试")
    void testLetterToNumber_RowNumbers() {
        assertEquals(1, ExcelCoordinateConverter.letterToNumber("A1").row());
        assertEquals(5, ExcelCoordinateConverter.letterToNumber("A5").row());
        assertEquals(100, ExcelCoordinateConverter.letterToNumber("A100").row());
        assertEquals(9999, ExcelCoordinateConverter.letterToNumber("A9999").row());
    }

    @Test
    @DisplayName("双字母坐标转换")
    void testLetterToNumber_DoubleLetter() {
        assertEquals(27, ExcelCoordinateConverter.letterToNumber("AA1").col());
        assertEquals(28, ExcelCoordinateConverter.letterToNumber("AB1").col());
        assertEquals(52, ExcelCoordinateConverter.letterToNumber("AZ1").col());
        assertEquals(53, ExcelCoordinateConverter.letterToNumber("BA1").col());
        assertEquals(702, ExcelCoordinateConverter.letterToNumber("ZZ1").col());
    }

    @Test
    @DisplayName("三字母坐标转换")
    void testLetterToNumber_TripleLetter() {
        assertEquals(703, ExcelCoordinateConverter.letterToNumber("AAA1").col());
        assertEquals(704, ExcelCoordinateConverter.letterToNumber("AAB1").col());
        assertEquals(728, ExcelCoordinateConverter.letterToNumber("AAZ1").col());
        assertEquals(729, ExcelCoordinateConverter.letterToNumber("ABA1").col());
        assertEquals(18278, ExcelCoordinateConverter.letterToNumber("ZZZ1").col());
    }

    @Test
    @DisplayName("完整坐标转换测试")
    void testLetterToNumber_FullCoordinate() {
        var coord1 = ExcelCoordinateConverter.letterToNumber("B5");
        assertEquals(2, coord1.col());
        assertEquals(5, coord1.row());

        var coord2 = ExcelCoordinateConverter.letterToNumber("AB123");
        assertEquals(28, coord2.col());
        assertEquals(123, coord2.row());
    }

    @Test
    @DisplayName("空坐标抛异常")
    void testLetterToNumber_NullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.letterToNumber(null));
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.letterToNumber(""));
    }

    @Test
    @DisplayName("无效坐标格式抛异常")
    void testLetterToNumber_InvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.letterToNumber("123")); // 缺少列
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.letterToNumber("A")); // 缺少行
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.letterToNumber("A0")); // 行号不能为0
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.letterToNumber("a1")); // 小写字母
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.letterToNumber("A1B")); // 格式错误
    }

    // ========== numberToLetter 测试 ==========

    @Test
    @DisplayName("数字转字母 - 单字母")
    void testNumberToLetter_SingleLetter() {
        assertEquals("A1", ExcelCoordinateConverter.numberToLetter(1, 1));
        assertEquals("B1", ExcelCoordinateConverter.numberToLetter(2, 1));
        assertEquals("Z1", ExcelCoordinateConverter.numberToLetter(26, 1));
    }

    @Test
    @DisplayName("数字转字母 - 行号测试")
    void testNumberToLetter_RowNumbers() {
        assertEquals("A1", ExcelCoordinateConverter.numberToLetter(1, 1));
        assertEquals("A5", ExcelCoordinateConverter.numberToLetter(1, 5));
        assertEquals("A100", ExcelCoordinateConverter.numberToLetter(1, 100));
        assertEquals("A9999", ExcelCoordinateConverter.numberToLetter(1, 9999));
    }

    @Test
    @DisplayName("数字转字母 - 双字母")
    void testNumberToLetter_DoubleLetter() {
        assertEquals("AA1", ExcelCoordinateConverter.numberToLetter(27, 1));
        assertEquals("AB1", ExcelCoordinateConverter.numberToLetter(28, 1));
        assertEquals("AZ1", ExcelCoordinateConverter.numberToLetter(52, 1));
        assertEquals("BA1", ExcelCoordinateConverter.numberToLetter(53, 1));
        assertEquals("ZZ1", ExcelCoordinateConverter.numberToLetter(702, 1));
    }

    @Test
    @DisplayName("数字转字母 - 三字母")
    void testNumberToLetter_TripleLetter() {
        assertEquals("AAA1", ExcelCoordinateConverter.numberToLetter(703, 1));
        assertEquals("AAB1", ExcelCoordinateConverter.numberToLetter(704, 1));
        assertEquals("AAZ1", ExcelCoordinateConverter.numberToLetter(728, 1));
        assertEquals("ABA1", ExcelCoordinateConverter.numberToLetter(729, 1));
        assertEquals("ZZZ1", ExcelCoordinateConverter.numberToLetter(18278, 1));
    }

    @Test
    @DisplayName("数字转字母 - 完整坐标")
    void testNumberToLetter_FullCoordinate() {
        assertEquals("B5", ExcelCoordinateConverter.numberToLetter(2, 5));
        assertEquals("AB123", ExcelCoordinateConverter.numberToLetter(28, 123));
        assertEquals("XFD1048576", ExcelCoordinateConverter.numberToLetter(16384, 1048576)); // Excel最大坐标
    }

    @Test
    @DisplayName("零或负数坐标抛异常")
    void testNumberToLetter_ZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.numberToLetter(0, 1));
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.numberToLetter(1, 0));
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.numberToLetter(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> ExcelCoordinateConverter.numberToLetter(1, -1));
    }

    @Test
    @DisplayName("record对象重载测试")
    void testNumberToLetter_Record() {
        var coord = new ExcelCoordinateConverter.Coordinate(2, 5);
        assertEquals("B5", ExcelCoordinateConverter.numberToLetter(coord));
    }

    // ========== 双向转换测试 ==========

    @Test
    @DisplayName("双向转换一致性")
    void testBidirectionalConversion() {
        // 常用坐标测试
        String[] testCoords = {"A1", "B5", "Z26", "AA1", "AB100", "AZ999", "BA1", "ZZ1", "AAA1", "XFD1048576"};

        for (String letterCoord : testCoords) {
            var numberCoord = ExcelCoordinateConverter.letterToNumber(letterCoord);
            String backToLetter = ExcelCoordinateConverter.numberToLetter(numberCoord.col(), numberCoord.row());
            assertEquals(letterCoord, backToLetter, "双向转换不一致: " + letterCoord);
        }
    }

    // ========== 兼容API测试 ==========

    @Test
    @DisplayName("letterToIntArray 兼容API")
    void testLetterToIntArray() {
        int[] result = ExcelCoordinateConverter.letterToIntArray("B5");
        assertArrayEquals(new int[]{2, 5}, result);

        result = ExcelCoordinateConverter.letterToIntArray("AA100");
        assertArrayEquals(new int[]{27, 100}, result);
    }

    @Test
    @DisplayName("intArrayToLetter 兼容API")
    void testIntArrayToLetter() {
        assertEquals("B5", ExcelCoordinateConverter.intArrayToLetter(new int[]{2, 5}));
        assertEquals("AA100", ExcelCoordinateConverter.intArrayToLetter(new int[]{27, 100}));
    }

    @Test
    @DisplayName("兼容API双向转换")
    void testCompatibilityAPI_Bidirectional() {
        int[] coords1 = {2, 5};
        String letter1 = ExcelCoordinateConverter.intArrayToLetter(coords1);
        int[] back1 = ExcelCoordinateConverter.letterToIntArray(letter1);
        assertArrayEquals(coords1, back1);

        int[] coords2 = {703, 12345};
        String letter2 = ExcelCoordinateConverter.intArrayToLetter(coords2);
        int[] back2 = ExcelCoordinateConverter.letterToIntArray(letter2);
        assertArrayEquals(coords2, back2);
    }

    // ========== 边界测试 ==========

    @Test
    @DisplayName("Excel最大坐标测试")
    void testExcelMaxCoordinate() {
        // Excel 2007+ 最大列: XFD = 16384
        // Excel 2007+ 最大行: 1048576
        String maxCoord = "XFD1048576";
        var numberCoord = ExcelCoordinateConverter.letterToNumber(maxCoord);
        assertEquals(16384, numberCoord.col());
        assertEquals(1048576, numberCoord.row());

        String backToLetter = ExcelCoordinateConverter.numberToLetter(numberCoord.col(), numberCoord.row());
        assertEquals(maxCoord, backToLetter);
    }

    @Test
    @DisplayName("坐标1,1测试")
    void testCoordinateOneOne() {
        var coord = ExcelCoordinateConverter.letterToNumber("A1");
        assertEquals(1, coord.col());
        assertEquals(1, coord.row());
        assertEquals("A1", ExcelCoordinateConverter.numberToLetter(coord.col(), coord.row()));
    }

    // ========== 缓存测试 ==========

    @Test
    @DisplayName("缓存功能测试")
    void testCache() {
        ExcelCoordinateConverter.clearCache();

        // 第一次调用，计算并缓存
        String coord1 = ExcelCoordinateConverter.numberToLetter(100, 100);
        String stats1 = ExcelCoordinateConverter.getCacheStats();

        // 第二次调用，应该命中缓存
        String coord2 = ExcelCoordinateConverter.numberToLetter(100, 100);
        String stats2 = ExcelCoordinateConverter.getCacheStats();

        assertEquals(coord1, coord2);
        assertTrue(stats1.equals(stats2), "缓存数量应该相同");
    }

    @Test
    @DisplayName("清除缓存测试")
    void testClearCache() {
        // 调用几次生成缓存
        ExcelCoordinateConverter.letterToNumber("A1");
        ExcelCoordinateConverter.letterToNumber("B2");
        ExcelCoordinateConverter.numberToLetter(1, 1);

        String statsBefore = ExcelCoordinateConverter.getCacheStats();
        assertFalse(statsBefore.contains("0"), "清除前缓存应该不为空");

        ExcelCoordinateConverter.clearCache();

        String statsAfter = ExcelCoordinateConverter.getCacheStats();
        assertTrue(statsAfter.contains("0"), "清除后缓存应该为空");
    }

    // ========== 性能基准测试示例 ==========

    @Test
    @DisplayName("性能测试 - 缓存效果")
    void testPerformance_CacheEffect() {
        ExcelCoordinateConverter.clearCache();

        int iterations = 10000;

        // 无缓存性能
        long start1 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ExcelCoordinateConverter.letterToNumber("AB123");
            ExcelCoordinateConverter.numberToLetter(28, 123);
        }
        long time1 = System.nanoTime() - start1;

        // 有缓存性能（同一坐标重复转换）
        long start2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ExcelCoordinateConverter.letterToNumber("AB123");
            ExcelCoordinateConverter.numberToLetter(28, 123);
        }
        long time2 = System.nanoTime() - start2;

        System.out.printf("无缓存: %d ns, 有缓存: %d ns, 提升: %.2fx%n",
                time1 / iterations, time2 / iterations,
                (double) time1 / time2);
    }

    @Test
    @DisplayName("批量转换性能测试")
    void testPerformance_BatchConversion() {
        int size = 1000;
        String[] letters = new String[size];
        for (int i = 0; i < size; i++) {
            letters[i] = ExcelCoordinateConverter.numberToLetter(i + 1, i + 1);
        }

        long start = System.nanoTime();
        for (String letter : letters) {
            ExcelCoordinateConverter.letterToNumber(letter);
        }
        long time = System.nanoTime() - start;

        System.out.printf("批量转换 %d 个坐标耗时: %.3f ms (平均 %.2f ns/个)%n",
                size, time / 1_000_000.0, (double) time / size);
    }
}