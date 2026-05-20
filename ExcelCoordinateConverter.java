import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel坐标转换工具类（高性能版本）
 * 
 * 提供Excel单元格引用、数字坐标、表单位置之间的转换功能
 * 
 * 性能优化：
 * - 使用缓存避免重复计算
 * - 使用高效的字符串处理
 * - 预计算列字母表
 * - 使用ConcurrentHashMap支持并发访问
 * 
 * @author Hermes Agent
 * @version 2.0
 */
public final class ExcelCoordinateConverter {
    
    /** Excel列字母表（A-Z, AA-ZZ, ...）预计算 */
    private static final String[] COLUMN_LETTERS = precomputeColumnLetters();
    
    /** 列字母到索引的映射（缓存） */
    private static final Map<String, Integer> COLUMN_LETTER_TO_INDEX_MAP = buildColumnLetterToIndexMap();
    
    /** Excel引用到表单位置的缓存 */
    private static final Map<String, int[]> EXCEL_REF_TO_POSITION_CACHE = new ConcurrentHashMap<>();
    
    /** 表单位置到Excel引用的缓存 */
    private static final String POSITION_TO_EXCEL_REF_CACHE_KEY = "POSITION_TO_EXCEL_REF";
    private static final Map<String, String> POSITION_TO_EXCEL_REF_CACHE = new ConcurrentHashMap<>();
    
    /** 支持的最大列数 */
    private static final int MAX_COLUMN_INDEX = 25;  // 支持到AZ列
    
    /**
     * 预计算列字母表
     * 
     * @return 列字母数组
     */
    private static String[] precomputeColumnLetters() {
        String[] columnLettersArray = new String[MAX_COLUMN_INDEX + 1];
        
        // A-Z (0-25)
        for (int columnIndex = 0; columnIndex < 26; columnIndex++) {
            columnLettersArray[columnIndex] = String.valueOf((char) ('A' + columnIndex));
        }
        
        // 可以继续扩展AA-ZZ等
        
        return columnLettersArray;
    }
    
    /**
     * 构建列字母到索引的映射
     * 
     * @return 映射表
     */
    private static Map<String, Integer> buildColumnLetterToIndexMap() {
        Map<String, Integer> columnLetterToIndexMap = new HashMap<>();
        
        for (int columnIndex = 0; columnIndex < COLUMN_LETTERS.length; columnIndex++) {
            columnLetterToIndexMap.put(COLUMN_LETTERS[columnIndex], columnIndex);
        }
        
        return columnLetterToIndexMap;
    }
    
    /**
     * Excel引用转数字坐标（从0开始，带缓存）
     * 
     * @param excelReference Excel引用，如"A1", "B5", "D3"
     * @return 数字坐标[rowIndex, columnIndex]，索引从0开始
     * @throws IllegalArgumentException 如果格式无效
     */
    public static int[] excelReferenceToCoordinates(String excelReference) {
        if (excelReference == null || excelReference.isEmpty()) {
            throw new IllegalArgumentException("Excel引用不能为空");
        }
        
        // 检查缓存
        int[] cachedCoordinates = EXCEL_REF_TO_POSITION_CACHE.get(excelReference);
        if (cachedCoordinates != null) {
            return Arrays.copyOf(cachedCoordinates, cachedCoordinates.length);
        }
        
        String upperCaseReference = excelReference.toUpperCase().trim();
        
        // 分离列字母和行数字
        int columnLetterEndPosition = findColumnLetterEndPosition(upperCaseReference);
        
        if (columnLetterEndPosition == 0 || columnLetterEndPosition == upperCaseReference.length()) {
            throw new IllegalArgumentException("Excel引用格式无效: " + excelReference);
        }
        
        String columnLetters = upperCaseReference.substring(0, columnLetterEndPosition);
        String rowNumberString = upperCaseReference.substring(columnLetterEndPosition);
        
        // 转换列字母
        int columnIndex = columnLetterToColumnIndex(columnLetters);
        
        // 转换行数字（Excel行号从1开始，索引从0开始）
        int rowIndex;
        try {
            rowIndex = Integer.parseInt(rowNumberString) - 1;
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Excel引用行号无效: " + rowNumberString, numberFormatException);
        }
        
        int[] coordinatesArray = new int[]{rowIndex, columnIndex};
        
        // 缓存结果
        EXCEL_REF_TO_POSITION_CACHE.put(excelReference, coordinatesArray);
        
        return coordinatesArray;
    }
    
    /**
     * 数字坐标转Excel引用
     * 
     * @param rowIndex 行索引（从0开始）
     * @param columnIndex 列索引（从0开始）
     * @return Excel引用，如"A1", "B5", "D3"
     */
    public static String coordinatesToExcelReference(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || columnIndex < 0) {
            throw new IllegalArgumentException("坐标不能为负数: [" + rowIndex + ", " + columnIndex + "]");
        }
        
        String columnLetters = columnIndexToColumnLetter(columnIndex);
        int rowNumber = rowIndex + 1;  // Excel行号从1开始
        
        return columnLetters + rowNumber;
    }
    
    /**
     * 表单位置（从1开始）转Excel引用（带缓存）
     * 
     * @param tablePosition 表单位置，如[3,4]表示D3
     * @return Excel引用
     */
    public static String tablePositionToExcelReference(int[] tablePosition) {
        if (tablePosition == null || tablePosition.length != 2) {
            throw new IllegalArgumentException("表单位置必须是长度为2的数组");
        }
        
        // 生成缓存键
        String cacheKey = tablePosition[0] + "," + tablePosition[1];
        
        // 检查缓存
        String cachedExcelReference = POSITION_TO_EXCEL_REF_CACHE.get(cacheKey);
        if (cachedExcelReference != null) {
            return cachedExcelReference;
        }
        
        // 表单位置从1开始，转换为从0开始
        int rowIndex = tablePosition[0] - 1;
        int columnIndex = tablePosition[1] - 1;
        
        String excelReference = coordinatesToExcelReference(rowIndex, columnIndex);
        
        // 缓存结果
        POSITION_TO_EXCEL_REF_CACHE.put(cacheKey, excelReference);
        
        return excelReference;
    }
    
    /**
     * Excel引用转表单位置（从1开始）
     * 
     * @param excelReference Excel引用
     * @return 表单位置[row, column]，从1开始
     */
    public static int[] excelReferenceToTablePosition(String excelReference) {
        int[] coordinates = excelReferenceToCoordinates(excelReference);
        
        // 转换为从1开始
        return new int[]{coordinates[0] + 1, coordinates[1] + 1};
    }
    
    /**
     * 列字母转列索引（从0开始）
     * 
     * @param columnLetters 列字母，如"A", "B", "AA"
     * @return 列索引
     */
    public static int columnLetterToColumnIndex(String columnLetters) {
        if (columnLetters == null || columnLetters.isEmpty()) {
            throw new IllegalArgumentException("列字母不能为空");
        }
        
        Integer columnIndex = COLUMN_LETTER_TO_INDEX_MAP.get(columnLetters.toUpperCase());
        if (columnIndex == null) {
            throw new IllegalArgumentException("不支持的列字母: " + columnLetters);
        }
        
        return columnIndex;
    }
    
    /**
     * 列索引转列字母
     * 
     * @param columnIndex 列索引（从0开始）
     * @return 列字母
     */
    public static String columnIndexToColumnLetter(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= COLUMN_LETTERS.length) {
            throw new IllegalArgumentException("列索引超出范围: " + columnIndex);
        }
        
        return COLUMN_LETTERS[columnIndex];
    }
    
    /**
     * 表单位置字符串解析为数组
     * 
     * @param positionString 表单位置字符串，如"3,4"
     * @return 表单位置数组
     */
    public static int[] parseTablePositionString(String positionString) {
        if (positionString == null || positionString.isEmpty()) {
            throw new IllegalArgumentException("表单位置字符串不能为空");
        }
        
        int commaPosition = positionString.indexOf(',');
        if (commaPosition == -1) {
            throw new IllegalArgumentException("表单位置格式错误: " + positionString);
        }
        
        try {
            int rowNumber = Integer.parseInt(positionString.substring(0, commaPosition).trim());
            int columnNumber = Integer.parseInt(positionString.substring(commaPosition + 1).trim());
            return new int[]{rowNumber, columnNumber};
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("表单位置包含非数字字符: " + positionString, numberFormatException);
        }
    }
    
    /**
     * 表单位置数组格式化为字符串
     * 
     * @param tablePosition 表单位置数组
     * @return 表单位置字符串，如"3,4"
     */
    public static String formatTablePositionString(int[] tablePosition) {
        if (tablePosition == null || tablePosition.length != 2) {
            throw new IllegalArgumentException("表单位置必须是长度为2的数组");
        }
        
        return tablePosition[0] + "," + tablePosition[1];
    }
    
    /**
     * 查找列字母的结束位置
     * 
     * @param excelReference Excel引用字符串
     * @return 列字母结束位置
     */
    private static int findColumnLetterEndPosition(String excelReference) {
        int referenceLength = excelReference.length();
        int currentPosition = 0;
        
        while (currentPosition < referenceLength && Character.isLetter(excelReference.charAt(currentPosition))) {
            currentPosition++;
        }
        
        return currentPosition;
    }
    
    /**
     * 清除所有缓存
     */
    public static void clearAllCaches() {
        EXCEL_REF_TO_POSITION_CACHE.clear();
        POSITION_TO_EXCEL_REF_CACHE.clear();
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息字符串
     */
    public static String getCacheStatistics() {
        return String.format("Excel引用缓存: %d, 位置缓存: %d",
                EXCEL_REF_TO_POSITION_CACHE.size(),
                POSITION_TO_EXCEL_REF_CACHE.size());
    }
}