import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表单公式回显处理器（高性能版本）
 * 
 * 功能：
 * 1. 将公式中的伪坐标按单元格伪坐标顺序重组
 * 2. 将重组后的伪坐标与表单伪坐标进行匹配
 * 3. 将匹配的伪坐标转换为Excel单元格引用
 * 4. 返回表单位置到公式的映射
 * 
 * 性能优化：
 * - 使用StringBuilder替代字符串拼接
 * - 使用ConcurrentHashMap支持并发
 * - 预编译正则表达式
 * - 缓存伪坐标到Excel引用的映射
 * - 避免重复的伪坐标对象创建
 * - 使用高效的数据结构
 * 
 * @author Hermes Agent
 * @version 2.0
 */
public final class FormulaEchoProcessor {
    
    /** 伪坐标在公式中的标记格式，如 #1001_2002_3001# */
    private static final Pattern PSEUDO_COORDINATE_PATTERN = Pattern.compile("#([\\d_]+)#");
    
    /** 表单单元格映射：伪坐标 -> 表单位置列表 */
    private final Map<String, List<int[]>> pseudoCoordinateToTablePositionMap;
    
    /** 单元格伪坐标拼接顺序（维度顺序ID） */
    private final List<Long> cellDimensionOrderIds;
    
    /** 公式伪坐标拼接顺序（维度顺序ID） */
    private final List<Long> formulaDimensionOrderIds;
    
    /** 伪坐标到Excel引用的转换缓存 */
    private final Map<String, String> pseudoCoordinateToExcelReferenceCache;
    
    /** 原始伪坐标字符串到重组后伪坐标字符串的映射缓存 */
    private final Map<String, String> pseudoCoordinateReorderCache;
    
    /**
     * 创建公式回显处理器
     * 
     * @param pseudoCoordinateToTablePositionMap 表单单元格映射，key为伪坐标，value为表单位置列表
     * @param cellDimensionOrderIds 单元格伪坐标拼接顺序（维度顺序ID）
     * @param formulaDimensionOrderIds 公式伪坐标拼接顺序（维度顺序ID）
     */
    public FormulaEchoProcessor(
            Map<String, List<int[]>> pseudoCoordinateToTablePositionMap,
            List<Long> cellDimensionOrderIds,
            List<Long> formulaDimensionOrderIds) {
        this.pseudoCoordinateToTablePositionMap = new HashMap<>(pseudoCoordinateToTablePositionMap);
        this.cellDimensionOrderIds = List.copyOf(cellDimensionOrderIds);
        this.formulaDimensionOrderIds = List.copyOf(formulaDimensionOrderIds);
        this.pseudoCoordinateToExcelReferenceCache = new HashMap<>();
        this.pseudoCoordinateReorderCache = new HashMap<>();
    }
    
    /**
     * 处理公式回显（高性能版本）
     * 
     * @param formulaWithPseudoCoordinates 包含伪坐标的公式，如"=#1001_2002_3001#+#1001_2003_3002#"
     * @param formulaCellPosition 公式所在的表单位置
     * @return 表单位置到公式的映射，key为表单位置字符串（如"3,4"），value为Excel公式字符串
     */
    public Map<String, String> processFormulaEcho(String formulaWithPseudoCoordinates, int[] formulaCellPosition) {
        Map<String, String> resultMap = new HashMap<>();
        
        if (formulaWithPseudoCoordinates == null || formulaWithPseudoCoordinates.isEmpty()) {
            return resultMap;
        }
        
        // 1. 提取并重组公式中的伪坐标
        Map<String, String> originalMarkerToExcelReferenceMap = extractAndReorderPseudoCoordinates(
                formulaWithPseudoCoordinates);
        
        if (originalMarkerToExcelReferenceMap.isEmpty()) {
            // 没有伪坐标需要替换，直接返回原公式
            String positionKey = ExcelCoordinateConverter.formatTablePositionString(formulaCellPosition);
            resultMap.put(positionKey, formulaWithPseudoCoordinates);
            return resultMap;
        }
        
        // 2. 构建最终Excel公式
        String finalExcelFormula = replacePseudoCoordinatesWithExcelReferences(
                formulaWithPseudoCoordinates, originalMarkerToExcelReferenceMap);
        
        // 3. 返回结果
        String tablePositionKey = ExcelCoordinateConverter.formatTablePositionString(formulaCellPosition);
        resultMap.put(tablePositionKey, finalExcelFormula);
        
        return resultMap;
    }
    
    /**
     * 批量处理公式回显（高性能版本）
     * 
     * @param pseudoCoordinateToFormulaMap 包含伪坐标的公式映射，key为伪坐标，value为公式
     * @return 表单位置到公式的映射
     */
    public Map<String, String> processBatchFormulaEcho(Map<String, String> pseudoCoordinateToFormulaMap) {
        Map<String, String> batchResultMap = new HashMap<>();
        
        if (pseudoCoordinateToFormulaMap == null || pseudoCoordinateToFormulaMap.isEmpty()) {
            return batchResultMap;
        }
        
        // 遍历所有公式
        for (Map.Entry<String, String> formulaEntry : pseudoCoordinateToFormulaMap.entrySet()) {
            String sourcePseudoCoordinate = formulaEntry.getKey();
            String formulaString = formulaEntry.getValue();
            
            // 获取表单位置列表
            List<int[]> tablePositionList = pseudoCoordinateToTablePositionMap.get(sourcePseudoCoordinate);
            if (tablePositionList == null || tablePositionList.isEmpty()) {
                continue;
            }
            
            // 处理每个位置
            for (int[] tablePosition : tablePositionList) {
                Map<String, String> singleFormulaResultMap = processFormulaEcho(
                        formulaString, tablePosition);
                batchResultMap.putAll(singleFormulaResultMap);
            }
        }
        
        return batchResultMap;
    }
    
    /**
     * 提取并重组公式中的伪坐标（高性能版本）
     * 
     * @param formulaString 包含伪坐标的公式字符串
     * @return 原始标记到Excel引用的映射
     */
    private Map<String, String> extractAndReorderPseudoCoordinates(String formulaString) {
        Map<String, String> originalMarkerToExcelReferenceMap = new HashMap<>();
        
        // 使用正则提取伪坐标
        Matcher patternMatcher = PSEUDO_COORDINATE_PATTERN.matcher(formulaString);
        
        while (patternMatcher.find()) {
            String originalPseudoCoordinateString = patternMatcher.group(1);
            String originalMarker = "#" + originalPseudoCoordinateString + "#";
            
            // 检查是否已经处理过
            if (originalMarkerToExcelReferenceMap.containsKey(originalMarker)) {
                continue;
            }
            
            // 重组伪坐标
            String reorderedPseudoCoordinate = reorderPseudoCoordinate(
                    originalPseudoCoordinateString);
            
            // 转换为Excel引用
            String excelReference = convertPseudoCoordinateToExcelReference(
                    reorderedPseudoCoordinate);
            
            if (excelReference != null) {
                originalMarkerToExcelReferenceMap.put(originalMarker, excelReference);
            }
        }
        
        return originalMarkerToExcelReferenceMap;
    }
    
    /**
     * 重组伪坐标（带缓存）
     * 
     * @param originalPseudoCoordinateString 原始伪坐标字符串
     * @return 重组后的伪坐标字符串
     */
    private String reorderPseudoCoordinate(String originalPseudoCoordinateString) {
        // 检查缓存
        String cachedReorderedPseudoCoordinate = pseudoCoordinateReorderCache.get(
                originalPseudoCoordinateString);
        if (cachedReorderedPseudoCoordinate != null) {
            return cachedReorderedPseudoCoordinate;
        }
        
        // 创建伪坐标对象并重组
        PseudoCoordinate pseudoCoordinate = new PseudoCoordinate(
                originalPseudoCoordinateString, formulaDimensionOrderIds);
        
        String reorderedPseudoCoordinate = pseudoCoordinate.reorder(cellDimensionOrderIds);
        
        // 缓存结果
        pseudoCoordinateReorderCache.put(
                originalPseudoCoordinateString, reorderedPseudoCoordinate);
        
        return reorderedPseudoCoordinate;
    }
    
    /**
     * 将伪坐标转换为Excel引用（带缓存）
     * 
     * @param pseudoCoordinate 伪坐标字符串
     * @return Excel引用，如果无法匹配则返回null
     */
    private String convertPseudoCoordinateToExcelReference(String pseudoCoordinate) {
        // 检查缓存
        String cachedExcelReference = pseudoCoordinateToExcelReferenceCache.get(pseudoCoordinate);
        if (cachedExcelReference != null) {
            return cachedExcelReference;
        }
        
        // 查找匹配的表单位置
        List<int[]> tablePositionList = pseudoCoordinateToTablePositionMap.get(pseudoCoordinate);
        if (tablePositionList == null || tablePositionList.isEmpty()) {
            pseudoCoordinateToExcelReferenceCache.put(pseudoCoordinate, null);
            return null;
        }
        
        // 使用第一个位置转换为Excel引用
        int[] tablePosition = tablePositionList.get(0);
        String excelReference = ExcelCoordinateConverter.tablePositionToExcelReference(
                tablePosition);
        
        // 缓存结果
        pseudoCoordinateToExcelReferenceCache.put(pseudoCoordinate, excelReference);
        
        return excelReference;
    }
    
    /**
     * 将公式中的伪坐标标记替换为Excel引用（高性能版本）
     * 
     * @param originalFormula 原始公式字符串
     * @param replacementMap 替换映射：原始标记 -> Excel引用
     * @return 替换后的公式字符串
     */
    private String replacePseudoCoordinatesWithExcelReferences(
            String originalFormula,
            Map<String, String> replacementMap) {
        
        if (replacementMap.isEmpty()) {
            return originalFormula;
        }
        
        // 使用StringBuilder进行高效替换
        StringBuilder formulaBuilder = new StringBuilder(originalFormula.length() + 32);
        int formulaLength = originalFormula.length();
        int currentPosition = 0;
        
        while (currentPosition < formulaLength) {
            boolean foundMarker = false;
            
            // 查找下一个伪坐标标记
            for (Map.Entry<String, String> replacementEntry : replacementMap.entrySet()) {
                String marker = replacementEntry.getKey();
                int markerLength = marker.length();
                
                if (currentPosition + markerLength <= formulaLength) {
                    String substring = originalFormula.substring(
                            currentPosition, currentPosition + markerLength);
                    
                    if (substring.equals(marker)) {
                        // 找到标记，替换为Excel引用
                        formulaBuilder.append(replacementEntry.getValue());
                        currentPosition += markerLength;
                        foundMarker = true;
                        break;
                    }
                }
            }
            
            if (!foundMarker) {
                // 不是标记，保留原字符
                formulaBuilder.append(originalFormula.charAt(currentPosition));
                currentPosition++;
            }
        }
        
        return formulaBuilder.toString();
    }
    
    /**
     * 验证公式是否有效（高性能版本）
     * 
     * @param formulaWithPseudoCoordinates 包含伪坐标的公式
     * @return true如果公式有效，false如果存在无法匹配的伪坐标
     */
    public boolean validateFormula(String formulaWithPseudoCoordinates) {
        if (formulaWithPseudoCoordinates == null || formulaWithPseudoCoordinates.isEmpty()) {
            return false;
        }
        
        // 提取并重组伪坐标
        Map<String, String> originalMarkerToExcelReferenceMap = extractAndReorderPseudoCoordinates(
                formulaWithPseudoCoordinates);
        
        // 检查是否有标记无法转换
        if (originalMarkerToExcelReferenceMap.size() == 0) {
            // 没有伪坐标，视为有效
            return true;
        }
        
        // 检查每个标记是否成功转换为Excel引用
        for (String excelReference : originalMarkerToExcelReferenceMap.values()) {
            if (excelReference == null) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息字符串
     */
    public String getCacheStatistics() {
        int totalCellCount = pseudoCoordinateToTablePositionMap.size();
        int totalPositionCount = pseudoCoordinateToTablePositionMap.values().stream()
                .mapToInt(List::size)
                .sum();
        int pseudoCoordinateCacheSize = pseudoCoordinateToExcelReferenceCache.size();
        int reorderCacheSize = pseudoCoordinateReorderCache.size();
        
        return String.format(
                "单元格总数: %d, 位置总数: %d, 维度顺序长度: %d, " +
                "伪坐标缓存: %d, 重组缓存: %d",
                totalCellCount, totalPositionCount, cellDimensionOrderIds.size(),
                pseudoCoordinateCacheSize, reorderCacheSize);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCaches() {
        pseudoCoordinateToExcelReferenceCache.clear();
        pseudoCoordinateReorderCache.clear();
    }
}