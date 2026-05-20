import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表单公式回显管理器（主入口，高性能版本）
 * 
 * 功能：
 * - 管理表单单元格与位置映射
 * - 管理维度顺序配置
 * - 批量处理公式回显
 * - 验证公式有效性
 * 
 * 性能优化：
 * - 使用ConcurrentHashMap支持并发访问
 * - 使用不可变列表减少内存分配
 * - 缓存处理器实例
 * - 避免重复的配置验证
 * - 使用高效的数据结构
 * 
 * @author Hermes Agent
 * @version 2.0
 */
public final class FormulaEchoManager {
    
    /** 表单单元格映射：伪坐标 -> 表单位置列表 */
    private Map<String, List<int[]>> pseudoCoordinateToTablePositionMap;
    
    /** 单元格伪坐标拼接顺序（维度顺序ID） */
    private List<Long> cellDimensionOrderIds;
    
    /** 公式伪坐标拼接顺序（维度顺序ID） */
    private List<Long> formulaDimensionOrderIds;
    
    /** 是否已初始化 */
    private boolean isInitialized;
    
    /** 处理器缓存（避免重复创建） */
    private final Map<String, FormulaEchoProcessor> processorCache;
    
    /** 表单位置到伪坐标的反向映射缓存 */
    private final Map<String, String> tablePositionToPseudoCoordinateCache;
    
    /**
     * 创建公式回显管理器
     */
    public FormulaEchoManager() {
        this.pseudoCoordinateToTablePositionMap = new ConcurrentHashMap<>();
        this.cellDimensionOrderIds = List.of();
        this.formulaDimensionOrderIds = List.of();
        this.isInitialized = false;
        this.processorCache = new ConcurrentHashMap<>();
        this.tablePositionToPseudoCoordinateCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 设置表单单元格映射
     * 
     * @param pseudoCoordinateToTablePositionMap 单元格映射，key为伪坐标，value为表单位置列表
     */
    public void setPseudoCoordinateToTablePositionMap(
            Map<String, List<int[]>> pseudoCoordinateToTablePositionMap) {
        if (pseudoCoordinateToTablePositionMap == null) {
            throw new IllegalArgumentException("单元格映射不能为null");
        }
        
        // 创建不可变的副本
        Map<String, List<int[]>> immutableMap = new ConcurrentHashMap<>();
        for (Map.Entry<String, List<int[]>> entry : pseudoCoordinateToTablePositionMap.entrySet()) {
            List<int[]> immutablePositionList = new ArrayList<>();
            for (int[] position : entry.getValue()) {
                immutablePositionList.add(Arrays.copyOf(position, position.length));
            }
            immutableMap.put(entry.getKey(), List.copyOf(immutablePositionList));
        }
        
        this.pseudoCoordinateToTablePositionMap = immutableMap;
        
        // 清除缓存
        clearAllCaches();
        
        checkInitializationStatus();
    }
    
    /**
     * 设置维度顺序
     * 
     * @param cellDimensionOrderIds 单元格伪坐标拼接顺序
     * @param formulaDimensionOrderIds 公式伪坐标拼接顺序
     */
    public void setDimensionOrderIds(
            List<Long> cellDimensionOrderIds,
            List<Long> formulaDimensionOrderIds) {
        if (cellDimensionOrderIds == null || formulaDimensionOrderIds == null) {
            throw new IllegalArgumentException("维度顺序不能为null");
        }
        
        this.cellDimensionOrderIds = List.copyOf(cellDimensionOrderIds);
        this.formulaDimensionOrderIds = List.copyOf(formulaDimensionOrderIds);
        
        // 清除缓存
        clearAllCaches();
        
        checkInitializationStatus();
    }
    
    /**
     * 检查是否已初始化
     */
    private void checkInitializationStatus() {
        boolean hasCellMap = !pseudoCoordinateToTablePositionMap.isEmpty();
        boolean hasCellOrder = !cellDimensionOrderIds.isEmpty();
        boolean hasFormulaOrder = !formulaDimensionOrderIds.isEmpty();
        
        this.isInitialized = hasCellMap && hasCellOrder && hasFormulaOrder;
    }
    
    /**
     * 批量处理公式回显（主方法，高性能）
     * 
     * @param pseudoCoordinateToFormulaMap 包含伪坐标的公式映射
     *         key: 公式所在单元格的伪坐标
     *         value: 公式字符串（包含伪坐标标记）
     * @return 表单位置到公式的映射
     *         key: 表单位置字符串（如"3,4"）
     *         value: Excel公式字符串（如"=B5+H6"）
     */
    public Map<String, String> processBatchFormulaEcho(
            Map<String, String> pseudoCoordinateToFormulaMap) {
        if (!isInitialized) {
            throw new IllegalStateException("请先配置单元格映射和维度顺序");
        }
        
        if (pseudoCoordinateToFormulaMap == null || pseudoCoordinateToFormulaMap.isEmpty()) {
            return new HashMap<>();
        }
        
        // 获取或创建处理器
        FormulaEchoProcessor processor = getOrCreateProcessor();
        
        // 批量处理
        return processor.processBatchFormulaEcho(pseudoCoordinateToFormulaMap);
    }
    
    /**
     * 处理单个公式回显
     * 
     * @param sourcePseudoCoordinate 公式所在单元格的伪坐标
     * @param formulaWithPseudoCoordinates 包含伪坐标的公式字符串
     * @return 表单位置到公式的映射
     */
    public Map<String, String> processSingleFormula(
            String sourcePseudoCoordinate,
            String formulaWithPseudoCoordinates) {
        if (!isInitialized) {
            throw new IllegalStateException("请先配置单元格映射和维度顺序");
        }
        
        Map<String, String> pseudoCoordinateToFormulaMap = new HashMap<>();
        pseudoCoordinateToFormulaMap.put(sourcePseudoCoordinate, formulaWithPseudoCoordinates);
        
        return processBatchFormulaEcho(pseudoCoordinateToFormulaMap);
    }
    
    /**
     * 验证单个公式是否有效
     * 
     * @param formulaWithPseudoCoordinates 包含伪坐标的公式
     * @return true如果公式有效
     */
    public boolean validateFormula(String formulaWithPseudoCoordinates) {
        if (!isInitialized) {
            throw new IllegalStateException("请先配置单元格映射和维度顺序");
        }
        
        FormulaEchoProcessor processor = getOrCreateProcessor();
        return processor.validateFormula(formulaWithPseudoCoordinates);
    }
    
    /**
     * 批量验证公式
     * 
     * @param pseudoCoordinateToFormulaMap 包含伪坐标的公式映射
     * @return 验证结果映射，key为伪坐标，value为是否有效
     */
    public Map<String, Boolean> validateFormulas(
            Map<String, String> pseudoCoordinateToFormulaMap) {
        Map<String, Boolean> validationResultMap = new HashMap<>();
        
        for (Map.Entry<String, String> formulaEntry : pseudoCoordinateToFormulaMap.entrySet()) {
            boolean isValid = validateFormula(formulaEntry.getValue());
            validationResultMap.put(formulaEntry.getKey(), isValid);
        }
        
        return validationResultMap;
    }
    
    /**
     * 获取表单位置的伪坐标（带缓存）
     * 
     * @param tablePosition 表单位置，如[3,4]
     * @return 伪坐标，如果不存在则返回null
     */
    public String getPseudoCoordinateByTablePosition(int[] tablePosition) {
        // 检查缓存
        String positionKey = tablePosition[0] + "," + tablePosition[1];
        String cachedPseudoCoordinate = tablePositionToPseudoCoordinateCache.get(positionKey);
        if (cachedPseudoCoordinate != null) {
            return cachedPseudoCoordinate.isEmpty() ? null : cachedPseudoCoordinate;
        }
        
        // 遍历查找
        for (Map.Entry<String, List<int[]>> entry : pseudoCoordinateToTablePositionMap.entrySet()) {
            for (int[] currentPosition : entry.getValue()) {
                if (Arrays.equals(currentPosition, tablePosition)) {
                    String pseudoCoordinate = entry.getKey();
                    tablePositionToPseudoCoordinateCache.put(positionKey, pseudoCoordinate);
                    return pseudoCoordinate;
                }
            }
        }
        
        // 缓存未找到的结果
        tablePositionToPseudoCoordinateCache.put(positionKey, "");
        return null;
    }
    
    /**
     * 获取伪坐标的所有表单位置
     * 
     * @param pseudoCoordinate 伪坐标
     * @return 表单位置列表
     */
    public List<int[]> getTablePositionsByPseudoCoordinate(String pseudoCoordinate) {
        List<int[]> positionList = pseudoCoordinateToTablePositionMap.get(pseudoCoordinate);
        
        if (positionList == null || positionList.isEmpty()) {
            return List.of();
        }
        
        // 返回副本
        List<int[]> resultList = new ArrayList<>();
        for (int[] position : positionList) {
            resultList.add(Arrays.copyOf(position, position.length));
        }
        
        return resultList;
    }
    
    /**
     * 添加单元格映射（高性能版本）
     * 
     * @param pseudoCoordinate 伪坐标
     * @param tablePositionList 表单位置列表
     */
    public void addCellMapping(
            String pseudoCoordinate,
            List<int[]> tablePositionList) {
        if (pseudoCoordinate == null || pseudoCoordinate.isEmpty()) {
            throw new IllegalArgumentException("伪坐标不能为空");
        }
        
        if (tablePositionList == null || tablePositionList.isEmpty()) {
            throw new IllegalArgumentException("表单位置列表不能为空");
        }
        
        // 创建不可变的位置列表
        List<int[]> immutablePositionList = new ArrayList<>();
        for (int[] position : tablePositionList) {
            immutablePositionList.add(Arrays.copyOf(position, position.length));
        }
        
        pseudoCoordinateToTablePositionMap.put(
                pseudoCoordinate,
                List.copyOf(immutablePositionList));
        
        // 清除反向缓存
        clearReverseCache();
        
        checkInitializationStatus();
    }
    
    /**
     * 添加单个单元格映射（高性能版本）
     * 
     * @param pseudoCoordinate 伪坐标
     * @param tablePosition 表单位置
     */
    public void addSingleCellMapping(String pseudoCoordinate, int[] tablePosition) {
        if (pseudoCoordinate == null || pseudoCoordinate.isEmpty()) {
            throw new IllegalArgumentException("伪坐标不能为空");
        }
        
        if (tablePosition == null || tablePosition.length != 2) {
            throw new IllegalArgumentException("表单位置必须是长度为2的数组");
        }
        
        pseudoCoordinateToTablePositionMap.computeIfAbsent(
                pseudoCoordinate,
                key -> new ArrayList<>()
        ).add(Arrays.copyOf(tablePosition, tablePosition.length));
        
        // 清除反向缓存
        clearReverseCache();
        
        checkInitializationStatus();
    }
    
    /**
     * 清除所有配置
     */
    public void clearAllConfiguration() {
        pseudoCoordinateToTablePositionMap.clear();
        cellDimensionOrderIds = List.of();
        formulaDimensionOrderIds = List.of();
        isInitialized = false;
        clearAllCaches();
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCaches() {
        processorCache.clear();
        tablePositionToPseudoCoordinateCache.clear();
        ExcelCoordinateConverter.clearAllCaches();
    }
    
    /**
     * 清除反向缓存
     */
    private void clearReverseCache() {
        tablePositionToPseudoCoordinateCache.clear();
    }
    
    /**
     * 获取或创建处理器（带缓存）
     * 
     * @return 处理器实例
     */
    private FormulaEchoProcessor getOrCreateProcessor() {
        // 使用配置哈希作为缓存键
        String cacheKey = generateConfigurationHashKey();
        
        return processorCache.computeIfAbsent(cacheKey, key -> 
                new FormulaEchoProcessor(
                        pseudoCoordinateToTablePositionMap,
                        cellDimensionOrderIds,
                        formulaDimensionOrderIds));
    }
    
    /**
     * 生成配置哈希键
     * 
     * @return 哈希键
     */
    private String generateConfigurationHashKey() {
        int cellMapHashCode = pseudoCoordinateToTablePositionMap.hashCode();
        int cellOrderHashCode = cellDimensionOrderIds.hashCode();
        int formulaOrderHashCode = formulaDimensionOrderIds.hashCode();
        
        return cellMapHashCode + "_" + cellOrderHashCode + "_" + formulaOrderHashCode;
    }
    
    /**
     * 获取统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStatistics() {
        if (!isInitialized) {
            return "未初始化";
        }
        
        FormulaEchoProcessor processor = getOrCreateProcessor();
        String processorStatistics = processor.getCacheStatistics();
        String converterStatistics = ExcelCoordinateConverter.getCacheStatistics();
        
        return String.format("处理器: %s | 转换器: %s", processorStatistics, converterStatistics);
    }
    
    /**
     * 检查是否已初始化
     * 
     * @return true如果已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}