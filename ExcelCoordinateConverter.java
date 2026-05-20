import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel坐标转换工具类（高性能版本，无列数限制）
 * 
 * 提供Excel单元格引用、数字坐标、表单位置之间的转换功能
 * 
 * 支持任意数量的Excel列（A-Z, AA-ZZ, AAA-ZZZ等）
 * Excel最大支持16,384列（XFD）
 * 
 * 性能优化：
 * - 使用缓存避免重复计算
 * - 使用高效的字符串处理
 * - 使用ConcurrentHashMap支持并发访问
 * - 动态列字母转换算法，无预计算限制
 * 
 * @author Hermes Agent
 * @version 2.1
 */
public final class ExcelCoordinateConverter {
    
    /** Excel引用到表单位置的缓存 */
    private static final Map<String, int[]> EXCEL_REF_TO_POSITION_CACHE = new ConcurrentHashMap<>();
    
    /** 表单位置到Excel引用的缓存 */
    private static final Map<String, String> POSITION_TO_EXCEL_REF_CACHE = new ConcurrentHashMap<>();
    
    /** 列索引到列字母的转换缓存 */
    private static final Map<Integer, String> COLUMN_INDEX_TO_LETTER_CACHE = new ConcurrentHashMap<>();
    
    /** 列字母到列索引的转换缓存 */
    private static final Map<String, Integer> COLUMN_LETTER_TO_INDEX_CACHE = new ConcurrentHashMap<>();
    
    /** Excel最大支持列数（16,384列，XFD） */
    private static final int EXCEL_MAX_COLUMNS = 16384;
    
    /**
     * Excel引用转数字坐标（从0开始，带缓存）
     * 
     * @param excelReference Excel引用，如"A1", "B5", "D3", "AA1", "XFD1"
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
     * @return Excel引用，如"A1", "B5", "D3", "AA1"
     * @throws IllegalArgumentException 如果坐标无效
     */
    public static String coordinatesToExcelReference(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || columnIndex < 0) {
            throw new IllegalArgumentException("坐标不能为负数: [" + rowIndex + ", " + columnIndex + "]");
        }
        
        if (columnIndex >= EXCEL_MAX_COLUMNS) {
            throw new IllegalArgumentException("列索引超出Excel最大限制: " + columnIndex + 
                    " (最大支持: " + (EXCEL_MAX_COLUMNS - 1) + ")");
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
     * 列字母转列索引（从0开始，带缓存）
     * 
     * 支持任意格式的Excel列字母，如A、Z、AA、AZ、BA、ZZ、AAA等
     * 
     * @param columnLetters 列字母，如"A", "B", "AA", "XFD"
     * @return 列索引（从0开始）
     * @throws IllegalArgumentException 如果列字母无效
     */
    public static int columnLetterToColumnIndex(String columnLetters) {
        if (columnLetters == null || columnLetters.isEmpty()) {
            throw new IllegalArgumentException("列字母不能为空");
        }
        
        String upperCaseLetters = columnLetters.toUpperCase().trim();
        
        // 检查缓存
        Integer cachedColumnIndex = COLUMN_LETTER_TO_INDEX_CACHE.get(upperCaseLetters);
        if (cachedColumnIndex != null) {
            return cachedColumnIndex;
        }
        
        // 验证格式
        for (int charPosition = 0; charPosition < upperCaseLetters.length(); charPosition++) {
            char currentChar = upperCaseLetters.charAt(charPosition);
            if (currentChar < 'A' || currentChar > 'Z') {
                throw new IllegalArgumentException("列字母格式无效: " + columnLetters);
            }
        }
        
        // 转换算法：类似26进制，但是A=1, B=2, ..., Z=26
        int columnIndex = 0;
        int stringLength = upperCaseLetters.length();
        
        for (int charPosition = 0; charPosition < stringLength; charPosition++) {
            char currentChar = upperCaseLetters.charAt(charPosition);
            int charValue = currentChar - 'A' + 1;  // A=1, B=2, ..., Z=26
            columnIndex = columnIndex * 26 + charValue;
        }
        
        columnIndex -= 1;  // 转换为从0开始的索引
        
        // 验证是否超出Excel最大限制
        if (columnIndex >= EXCEL_MAX_COLUMNS) {
            throw new IllegalArgumentException("列字母超出Excel最大限制: " + columnLetters + 
                    " (最大支持: XFD)");
        }
        
        // 缓存结果
        COLUMN_LETTER_TO_INDEX_CACHE.put(upperCaseLetters, columnIndex);
        
        return columnIndex;
    }
    
    /**
     * 列索引转列字母（带缓存）
     * 
     * 支持任意列索引，自动转换为对应的Excel列字母
     * 
     * @param columnIndex 列索引（从0开始）
     * @return 列字母，如"A", "B", "AA", "XFD"
     * @throws IllegalArgumentException 如果列索引无效
     */
    public static String columnIndexToColumnLetter(int columnIndex) {
        if (columnIndex < 0) {
            throw new IllegalArgumentException("列索引不能为负数: " + columnIndex);
        }
        
        if (columnIndex >= EXCEL_MAX_COLUMNS) {
            throw new IllegalArgumentException("列索引超出Excel最大限制: " + columnIndex + 
                    " (最大支持: " + (EXCEL_MAX_COLUMNS - 1) + ", 即XFD)");
        }
        
        // 检查缓存
        String cachedColumnLetter = COLUMN_INDEX_TO_LETTER_CACHE.get(columnIndex);
        if (cachedColumnLetter != null) {
            return cachedColumnLetter;
        }
        
        // 转换算法：类似26进制，但是没有0，需要特殊处理
        StringBuilder columnLetterBuilder = new StringBuilder();
        int remainingIndex = columnIndex + 1;  // A=1, B=2, ..., Z=26
        
        while (remainingIndex > 0) {
            remainingIndex--;  // 转换为从0开始计算
            int remainder = remainingIndex % 26;
            columnLetterBuilder.insert(0, (char) ('A' + remainder));
            remainingIndex = remainingIndex / 26;
        }
        
        String columnLetter = columnLetterBuilder.toString();
        
        // 缓存结果
        COLUMN_INDEX_TO_LETTER_CACHE.put(columnIndex, columnLetter);
        
        return columnLetter;
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
        COLUMN_INDEX_TO_LETTER_CACHE.clear();
        COLUMN_LETTER_TO_INDEX_CACHE.clear();
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息字符串
     */
    public static String getCacheStatistics() {
        return String.format("Excel引用缓存: %d, 位置缓存: %d, 列索引->字母缓存: %d, 列字母->索引缓存: %d",
                EXCEL_REF_TO_POSITION_CACHE.size(),
                POSITION_TO_EXCEL_REF_CACHE.size(),
                COLUMN_INDEX_TO_LETTER_CACHE.size(),
                COLUMN_LETTER_TO_INDEX_CACHE.size());
    }
    
    /**
     * 获取Excel最大支持列数
     * 
     * @return 最大列数
     */
    public static int getMaxColumns() {
        return EXCEL_MAX_COLUMNS;
    }
    
    /**
     * 获取Excel最大列字母
     * 
     * @return 最大列字母（XFD）
     */
    public static String getMaxColumnLetter() {
        return "XFD";
    }
}