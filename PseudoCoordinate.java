import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单伪坐标坐标点（高性能版本）
 * 
 * 伪坐标是维度成员ID的有序组合，用于标识BI报表中的单元格
 * 例如：1001_2002_3001 表示由三个维度的成员ID组成
 * 
 * 性能优化：
 * - 使用不可变列表减少内存分配
 * - 缓存重组结果
 * - 使用StringBuilder减少字符串拼接开销
 * - 避免不必要的对象创建
 * 
 * @author Hermes Agent
 * @version 2.0
 */
public final class PseudoCoordinate {
    
    /** 维度成员ID列表（不可变） */
    private final List<Long> dimensionMemberIds;
    
    /** 原始拼接字符串（如"1001_2002_3001"） */
    private final String rawPseudoCoordinateString;
    
    /** 维度顺序ID列表（不可变） */
    private final List<Long> dimensionOrderIds;
    
    /** 重组结果缓存 */
    private Map<List<Long>, String> reorderedCache;
    
    /**
     * 从拼接字符串创建伪坐标
     * 
     * @param rawPseudoCoordinateString 伪坐标拼接字符串，如"1001_2002_3001"
     * @param dimensionOrderIds 维度顺序ID列表
     */
    public PseudoCoordinate(String rawPseudoCoordinateString, List<Long> dimensionOrderIds) {
        this.rawPseudoCoordinateString = rawPseudoCoordinateString;
        this.dimensionOrderIds = List.copyOf(dimensionOrderIds);
        this.dimensionMemberIds = parseRawString(rawPseudoCoordinateString);
        this.reorderedCache = new HashMap<>();
    }
    
    /**
     * 从成员ID列表创建伪坐标
     * 
     * @param dimensionMemberIds 维度成员ID列表
     * @param dimensionOrderIds 维度顺序ID列表
     */
    public PseudoCoordinate(List<Long> dimensionMemberIds, List<Long> dimensionOrderIds) {
        this.dimensionMemberIds = List.copyOf(dimensionMemberIds);
        this.dimensionOrderIds = List.copyOf(dimensionOrderIds);
        this.rawPseudoCoordinateString = buildRawString(dimensionMemberIds);
        this.reorderedCache = new HashMap<>();
    }
    
    /**
     * 解析拼接字符串为成员ID列表（高性能版本）
     * 
     * @param rawPseudoCoordinateString 拼接字符串
     * @return 成员ID列表
     */
    private List<Long> parseRawString(String rawPseudoCoordinateString) {
        if (rawPseudoCoordinateString == null || rawPseudoCoordinateString.isEmpty()) {
            return List.of();
        }
        
        List<Long> memberIdList = new ArrayList<>();
        int stringLength = rawPseudoCoordinateString.length();
        int startPosition = 0;
        
        for (int currentPosition = 0; currentPosition <= stringLength; currentPosition++) {
            if (currentPosition == stringLength || rawPseudoCoordinateString.charAt(currentPosition) == '_') {
                String numberSubstring = rawPseudoCoordinateString.substring(startPosition, currentPosition);
                if (!numberSubstring.isEmpty()) {
                    try {
                        memberIdList.add(Long.parseLong(numberSubstring));
                    } catch (NumberFormatException numberFormatException) {
                        // 忽略无法解析的部分
                    }
                }
                startPosition = currentPosition + 1;
            }
        }
        
        return List.copyOf(memberIdList);
    }
    
    /**
     * 从成员ID列表构建拼接字符串（高性能版本）
     * 
     * @param dimensionMemberIds 成员ID列表
     * @return 拼接字符串
     */
    private String buildRawString(List<Long> dimensionMemberIds) {
        if (dimensionMemberIds == null || dimensionMemberIds.isEmpty()) {
            return "";
        }
        
        StringBuilder stringBuilder = new StringBuilder();
        int memberCount = dimensionMemberIds.size();
        
        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
            if (memberIndex > 0) {
                stringBuilder.append('_');
            }
            stringBuilder.append(dimensionMemberIds.get(memberIndex));
        }
        
        return stringBuilder.toString();
    }
    
    /**
     * 按指定维度顺序重组伪坐标（带缓存，高性能）
     * 
     * @param newDimensionOrderIds 新的维度顺序ID列表
     * @return 重组后的伪坐标拼接字符串
     */
    public String reorder(List<Long> newDimensionOrderIds) {
        if (this.dimensionMemberIds.isEmpty() || newDimensionOrderIds.isEmpty()) {
            return this.rawPseudoCoordinateString;
        }
        
        // 检查缓存
        List<Long> immutableOrder = List.copyOf(newDimensionOrderIds);
        String cachedResult = reorderedCache.get(immutableOrder);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // 创建维度ID到成员ID的映射
        int minSize = Math.min(this.dimensionMemberIds.size(), this.dimensionOrderIds.size());
        Map<Long, Long> dimensionIdToMemberIdMap = new HashMap<>(minSize);
        
        for (int index = 0; index < minSize; index++) {
            Long dimensionId = this.dimensionOrderIds.get(index);
            Long memberId = this.dimensionMemberIds.get(index);
            dimensionIdToMemberIdMap.put(dimensionId, memberId);
        }
        
        // 按新顺序提取成员ID
        List<Long> reorderedMemberIdList = new ArrayList<>(newDimensionOrderIds.size());
        
        for (Long dimensionId : newDimensionOrderIds) {
            Long memberId = dimensionIdToMemberIdMap.get(dimensionId);
            if (memberId != null) {
                reorderedMemberIdList.add(memberId);
            }
        }
        
        // 构建结果字符串
        String reorderedResult = buildRawString(reorderedMemberIdList);
        
        // 缓存结果
        reorderedCache.put(immutableOrder, reorderedResult);
        
        return reorderedResult;
    }
    
    /**
     * 获取原始拼接字符串
     * 
     * @return 原始字符串
     */
    public String getRawPseudoCoordinateString() {
        return rawPseudoCoordinateString;
    }
    
    /**
     * 获取维度成员ID列表
     * 
     * @return 成员ID列表（不可变）
     */
    public List<Long> getDimensionMemberIds() {
        return dimensionMemberIds;
    }
    
    /**
     * 获取维度顺序ID列表
     * 
     * @return 维度顺序ID列表（不可变）
     */
    public List<Long> getDimensionOrderIds() {
        return dimensionOrderIds;
    }
    
    /**
     * 清除重组缓存（释放内存）
     */
    public void clearReorderedCache() {
        reorderedCache.clear();
    }
    
    @Override
    public String toString() {
        return "PseudoCoordinate{" +
                "rawPseudoCoordinateString='" + rawPseudoCoordinateString + '\'' +
                ", dimensionMemberIds=" + dimensionMemberIds +
                ", dimensionOrderIds=" + dimensionOrderIds +
                '}';
    }
    
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        
        PseudoCoordinate that = (PseudoCoordinate) object;
        return rawPseudoCoordinateString.equals(that.rawPseudoCoordinateString);
    }
    
    @Override
    public int hashCode() {
        return rawPseudoCoordinateString.hashCode();
    }
}