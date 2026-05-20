/**
 * Excel坐标转换测试 - 验证无列数限制
 * 
 * 测试各种列数：A-Z, AA-ZZ, AAA-ZZZ等
 * 
 * @author Hermes Agent
 * @version 2.1
 */
public final class ExcelCoordinateConverterTest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Excel坐标转换测试 - 无列数限制");
        System.out.println("========================================\n");
        
        testBasicColumns();
        testDoubleLetterColumns();
        testTripleLetterColumns();
        testEdgeCases();
        testCaching();
        
        System.out.println("\n========================================");
        System.out.println("所有测试通过 ✅");
        System.out.println("========================================");
    }
    
    /**
     * 测试基础列：A-Z
     */
    private static void testBasicColumns() {
        System.out.println("【测试1】基础列 A-Z");
        System.out.println("----------------------------------------");
        
        // 测试列索引转列字母
        System.out.println("列索引 -> 列字母:");
        System.out.println("  0 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(0) + " (预期: A)");
        System.out.println("  1 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(1) + " (预期: B)");
        System.out.println("  25 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(25) + " (预期: Z)");
        
        // 验证
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(0).equals("A") : "错误: 0 应该是 A";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(1).equals("B") : "错误: 1 应该是 B";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(25).equals("Z") : "错误: 25 应该是 Z";
        
        // 测试列字母转列索引
        System.out.println("\n列字母 -> 列索引:");
        System.out.println("  A -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("A") + " (预期: 0)");
        System.out.println("  B -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("B") + " (预期: 1)");
        System.out.println("  Z -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("Z") + " (预期: 25)");
        
        // 验证
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("A") == 0 : "错误: A 应该是 0";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("B") == 1 : "错误: B 应该是 1";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("Z") == 25 : "错误: Z 应该是 25";
        
        System.out.println("✅ 基础列测试通过\n");
    }
    
    /**
     * 测试双字母列：AA-ZZ
     */
    private static void testDoubleLetterColumns() {
        System.out.println("【测试2】双字母列 AA-ZZ");
        System.out.println("----------------------------------------");
        
        // 测试列索引转列字母
        System.out.println("列索引 -> 列字母:");
        System.out.println("  26 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(26) + " (预期: AA)");
        System.out.println("  27 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(27) + " (预期: AB)");
        System.out.println("  51 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(51) + " (预期: AZ)");
        System.out.println("  52 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(52) + " (预期: BA)");
        System.out.println("  701 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(701) + " (预期: ZZ)");
        
        // 验证
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(26).equals("AA") : "错误: 26 应该是 AA";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(27).equals("AB") : "错误: 27 应该是 AB";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(51).equals("AZ") : "错误: 51 应该是 AZ";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(52).equals("BA") : "错误: 52 应该是 BA";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(701).equals("ZZ") : "错误: 701 应该是 ZZ";
        
        // 测试列字母转列索引
        System.out.println("\n列字母 -> 列索引:");
        System.out.println("  AA -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("AA") + " (预期: 26)");
        System.out.println("  AB -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("AB") + " (预期: 27)");
        System.out.println("  AZ -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("AZ") + " (预期: 51)");
        System.out.println("  BA -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("BA") + " (预期: 52)");
        System.out.println("  ZZ -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("ZZ") + " (预期: 701)");
        
        // 验证
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("AA") == 26 : "错误: AA 应该是 26";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("AB") == 27 : "错误: AB 应该是 27";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("AZ") == 51 : "错误: AZ 应该是 51";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("BA") == 52 : "错误: BA 应该是 52";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("ZZ") == 701 : "错误: ZZ 应该是 701";
        
        System.out.println("✅ 双字母列测试通过\n");
    }
    
    /**
     * 测试三字母列：AAA-ZZZ
     */
    private static void testTripleLetterColumns() {
        System.out.println("【测试3】三字母列 AAA-ZZZ");
        System.out.println("----------------------------------------");
        
        // 测试列索引转列字母
        System.out.println("列索引 -> 列字母:");
        System.out.println("  702 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(702) + " (预期: AAA)");
        System.out.println("  703 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(703) + " (预期: AAB)");
        System.out.println("  726 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(726) + " (预期: AAZ)");
        System.out.println("  727 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(727) + " (预期: ABA)");
        System.out.println("  1378 -> " + ExcelCoordinateConverter.columnIndexToColumnLetter(1378) + " (预期: ZZZ)");
        
        // 验证
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(702).equals("AAA") : "错误: 702 应该是 AAA";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(703).equals("AAB") : "错误: 703 应该是 AAB";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(726).equals("AAZ") : "错误: 726 应该是 AAZ";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(727).equals("ABA") : "错误: 727 应该是 ABA";
        assert ExcelCoordinateConverter.columnIndexToColumnLetter(1378).equals("ZZZ") : "错误: 1378 应该是 ZZZ";
        
        // 测试列字母转列索引
        System.out.println("\n列字母 -> 列索引:");
        System.out.println("  AAA -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("AAA") + " (预期: 702)");
        System.out.println("  AAB -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("AAB") + " (预期: 703)");
        System.out.println("  AAZ -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("AAZ") + " (预期: 726)");
        System.out.println("  ABA -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("ABA") + " (预期: 727)");
        System.out.println("  ZZZ -> " + ExcelCoordinateConverter.columnLetterToColumnIndex("ZZZ") + " (预期: 1378)");
        
        // 验证
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("AAA") == 702 : "错误: AAA 应该是 702";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("AAB") == 703 : "错误: AAB 应该是 703";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("AAZ") == 726 : "错误: AAZ 应该是 726";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("ABA") == 727 : "错误: ABA 应该是 727";
        assert ExcelCoordinateConverter.columnLetterToColumnIndex("ZZZ") == 1378 : "错误: ZZZ 应该是 1378";
        
        System.out.println("✅ 三字母列测试通过\n");
    }
    
    /**
     * 测试边界情况
     */
    private static void testEdgeCases() {
        System.out.println("【测试4】边界情况");
        System.out.println("----------------------------------------");
        
        // Excel最大列数测试
        int maxColumns = ExcelCoordinateConverter.getMaxColumns();
        System.out.println("Excel最大列数: " + maxColumns);
        System.out.println("Excel最大列字母: " + ExcelCoordinateConverter.getMaxColumnLetter());
        
        // 测试XFD（Excel最大列）
        int xfdIndex = ExcelCoordinateConverter.columnLetterToColumnIndex("XFD");
        System.out.println("\nXFD 列索引: " + xfdIndex + " (预期: 16383)");
        assert xfdIndex == 16383 : "错误: XFD 应该是 16383";
        
        String xfdLetter = ExcelCoordinateConverter.columnIndexToColumnLetter(16383);
        System.out.println("索引 16383 列字母: " + xfdLetter + " (预期: XFD)");
        assert xfdLetter.equals("XFD") : "错误: 16383 应该是 XFD";
        
        // 测试超限列（应该抛出异常）
        System.out.println("\n测试超限列索引（应该抛出异常）:");
        try {
            ExcelCoordinateConverter.columnIndexToColumnLetter(16384);
            System.out.println("❌ 错误: 应该抛出异常");
            assert false;
        } catch (IllegalArgumentException exception) {
            System.out.println("✅ 正确抛出异常: " + exception.getMessage());
        }
        
        // 测试无效列字母（应该抛出异常）
        System.out.println("\n测试无效列字母（应该抛出异常）:");
        try {
            ExcelCoordinateConverter.columnLetterToColumnIndex("XFE");
            System.out.println("❌ 错误: 应该抛出异常");
            assert false;
        } catch (IllegalArgumentException exception) {
            System.out.println("✅ 正确抛出异常: " + exception.getMessage());
        }
        
        System.out.println("\n✅ 边界情况测试通过\n");
    }
    
    /**
     * 测试缓存功能
     */
    private static void testCaching() {
        System.out.println("【测试5】缓存功能");
        System.out.println("----------------------------------------");
        
        // 清除缓存
        ExcelCoordinateConverter.clearAllCaches();
        
        System.out.println("清除缓存后统计:");
        System.out.println("  " + ExcelCoordinateConverter.getCacheStatistics());
        
        // 执行一些转换
        System.out.println("\n执行转换:");
        String result1 = ExcelCoordinateConverter.columnIndexToColumnLetter(0);
        int result2 = ExcelCoordinateConverter.columnLetterToColumnIndex("AA");
        String result3 = ExcelCoordinateConverter.coordinatesToExcelReference(0, 0);
        int[] result4 = ExcelCoordinateConverter.excelReferenceToCoordinates("B5");
        
        System.out.println("  索引0 -> " + result1);
        System.out.println("  AA -> " + result2);
        System.out.println("  [0,0] -> " + result3);
        System.out.println("  B5 -> [" + result4[0] + ", " + result4[1] + "]");
        
        // 再次执行相同转换（应该命中缓存）
        System.out.println("\n再次执行相同转换（应该命中缓存）:");
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            ExcelCoordinateConverter.columnIndexToColumnLetter(0);
            ExcelCoordinateConverter.columnLetterToColumnIndex("AA");
        }
        long endTime = System.nanoTime();
        
        System.out.println("  1000次转换耗时: " + (endTime - startTime) / 1000 + " μs");
        System.out.println("  平均: " + (endTime - startTime) / 1000.0 / 1000 + " μs/次");
        
        System.out.println("\n缓存统计:");
        System.out.println("  " + ExcelCoordinateConverter.getCacheStatistics());
        
        System.out.println("✅ 缓存功能测试通过\n");
    }
}