import java.util.*;

/**
 * 性能测试类 - 验证优化效果
 * 
 * 测试场景：
 * 1. 基础功能性能测试
 * 2. 批量处理性能测试
 * 3. 缓存效果验证
 * 4. 内存使用分析
 * 
 * @author Hermes Agent
 * @version 2.0
 */
public final class FormulaEchoPerformanceTest {
    
    /** 测试预热迭代次数 */
    private static final int WARMUP_ITERATIONS = 1000;
    
    /** 测试迭代次数 */
    private static final int TEST_ITERATIONS = 10000;
    
    /** 大规模测试数据量 */
    private static final int LARGE_DATA_SIZE = 1000;
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("表单公式回显 - 性能测试");
        System.out.println("========================================\n");
        
        performanceTestBasicFormulaEcho();
        performanceTestBatchProcessing();
        performanceTestCacheEffectiveness();
        performanceTestLargeScaleData();
        performanceTestMemoryUsage();
        
        System.out.println("\n========================================");
        System.out.println("所有性能测试完成");
        System.out.println("========================================");
    }
    
    /**
     * 测试场景1：基础公式回显性能
     */
    private static void performanceTestBasicFormulaEcho() {
        System.out.println("【性能测试1】基础公式回显");
        System.out.println("----------------------------------------");
        
        try {
            // 准备测试数据
            Map<String, List<int[]>> cellPositionMap = createTestCellPositionMap(10);
            List<Long> cellDimensionOrderIds = Arrays.asList(1001L, 2002L, 3001L);
            List<Long> formulaDimensionOrderIds = Arrays.asList(2002L, 3001L, 1001L);
            
            // 创建管理器
            FormulaEchoManager manager = new FormulaEchoManager();
            manager.setPseudoCoordinateToTablePositionMap(cellPositionMap);
            manager.setDimensionOrderIds(cellDimensionOrderIds, formulaDimensionOrderIds);
            
            // 准备测试公式
            String testFormula = "=#2002_3001_1001#+#2002_3002_1001#+#2002_3003_1001#";
            String sourcePseudoCoordinate = "1001_2002_3001";
            
            System.out.println("测试公式: " + testFormula);
            System.out.println("预热次数: " + WARMUP_ITERATIONS);
            System.out.println("测试次数: " + TEST_ITERATIONS);
            
            // 预热
            long warmupStartTime = System.nanoTime();
            for (int iterationCount = 0; iterationCount < WARMUP_ITERATIONS; iterationCount++) {
                manager.processSingleFormula(sourcePseudoCoordinate, testFormula);
            }
            long warmupDuration = System.nanoTime() - warmupStartTime;
            
            // 测试
            long testStartTime = System.nanoTime();
            for (int iterationCount = 0; iterationCount < TEST_ITERATIONS; iterationCount++) {
                manager.processSingleFormula(sourcePseudoCoordinate, testFormula);
            }
            long testDuration = System.nanoTime() - testStartTime;
            
            // 计算统计数据
            double warmupTimeMillis = warmupDuration / 1_000_000.0;
            double testTimeMillis = testDuration / 1_000_000.0;
            double averageTimeMicroseconds = testDuration / (TEST_ITERATIONS * 1_000.0);
            double throughput = TEST_ITERATIONS / (testDuration / 1_000_000_000.0);
            
            System.out.println("\n性能数据:");
            System.out.printf("  预热耗时: %.2f ms%n", warmupTimeMillis);
            System.out.printf("  测试耗时: %.2f ms%n", testTimeMillis);
            System.out.printf("  平均耗时: %.3f μs/次%n", averageTimeMicroseconds);
            System.out.printf("  吞吐量: %.0f 次/秒%n", throughput);
            
            // 性能评价
            if (averageTimeMicroseconds < 10.0) {
                System.out.println("  ✅ 性能优秀");
            } else if (averageTimeMicroseconds < 50.0) {
                System.out.println("  ✅ 性能良好");
            } else {
                System.out.println("  ⚠️ 性能需要优化");
            }
            
            // 缓存统计
            System.out.println("\n" + manager.getStatistics());
            
        } catch (Exception exception) {
            System.out.println("❌ 测试异常: " + exception.getMessage());
            exception.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 测试场景2：批量处理性能
     */
    private static void performanceTestBatchProcessing() {
        System.out.println("【性能测试2】批量处理");
        System.out.println("----------------------------------------");
        
        try {
            // 准备大规模测试数据
            int batchSize = 100;
            Map<String, List<int[]>> cellPositionMap = createTestCellPositionMap(batchSize);
            List<Long> cellDimensionOrderIds = Arrays.asList(1001L, 2002L, 3001L);
            List<Long> formulaDimensionOrderIds = Arrays.asList(2002L, 3001L, 1001L);
            
            // 创建管理器
            FormulaEchoManager manager = new FormulaEchoManager();
            manager.setPseudoCoordinateToTablePositionMap(cellPositionMap);
            manager.setDimensionOrderIds(cellDimensionOrderIds, formulaDimensionOrderIds);
            
            // 准备批量公式
            Map<String, String> batchFormulaMap = createBatchTestFormulas(batchSize);
            
            System.out.println("批量大小: " + batchSize);
            System.out.println("测试次数: " + 100);
            
            // 预热
            for (int iterationCount = 0; iterationCount < 10; iterationCount++) {
                manager.processBatchFormulaEcho(batchFormulaMap);
            }
            
            // 测试
            long testStartTime = System.nanoTime();
            for (int iterationCount = 0; iterationCount < 100; iterationCount++) {
                manager.processBatchFormulaEcho(batchFormulaMap);
            }
            long testDuration = System.nanoTime() - testStartTime;
            
            // 计算统计数据
            double testTimeMillis = testDuration / 1_000_000.0;
            double averageTimeMilliseconds = testDuration / (100 * 1_000_000.0);
            double throughputPerSecond = (batchSize * 100) / (testDuration / 1_000_000_000.0);
            
            System.out.println("\n性能数据:");
            System.out.printf("  总耗时: %.2f ms%n", testTimeMillis);
            System.out.printf("  平均耗时: %.3f ms/批%n", averageTimeMilliseconds);
            System.out.printf("  单个公式平均: %.3f μs%n", averageTimeMilliseconds * 1000 / batchSize);
            System.out.printf("  吞吐量: %.0f 个公式/秒%n", throughputPerSecond);
            
            System.out.println("\n✅ 批量处理性能测试完成");
            
        } catch (Exception exception) {
            System.out.println("❌ 测试异常: " + exception.getMessage());
            exception.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 测试场景3：缓存效果验证
     */
    private static void performanceTestCacheEffectiveness() {
        System.out.println("【性能测试3】缓存效果");
        System.out.println("----------------------------------------");
        
        try {
            // 准备测试数据
            Map<String, List<int[]>> cellPositionMap = createTestCellPositionMap(5);
            List<Long> cellDimensionOrderIds = Arrays.asList(1001L, 2002L, 3001L);
            List<Long> formulaDimensionOrderIds = Arrays.asList(2002L, 3001L, 1001L);
            
            // 创建管理器
            FormulaEchoManager manager = new FormulaEchoManager();
            manager.setPseudoCoordinateToTablePositionMap(cellPositionMap);
            manager.setDimensionOrderIds(cellDimensionOrderIds, formulaDimensionOrderIds);
            
            // 准备测试公式
            String testFormula = "=#2002_3001_1001#+#2002_3002_1001#";
            String sourcePseudoCoordinate = "1001_2002_3001";
            
            System.out.println("测试公式: " + testFormula);
            System.out.println("测试次数: " + TEST_ITERATIONS);
            
            // 清除缓存
            manager.clearAllCaches();
            
            // 首次执行（无缓存）
            long firstExecutionStartTime = System.nanoTime();
            Map<String, String> firstResult = manager.processSingleFormula(
                    sourcePseudoCoordinate, testFormula);
            long firstExecutionDuration = System.nanoTime() - firstExecutionStartTime;
            
            // 重复执行（有缓存）
            long cachedExecutionStartTime = System.nanoTime();
            Map<String, String> cachedResult = manager.processSingleFormula(
                    sourcePseudoCoordinate, testFormula);
            long cachedExecutionDuration = System.nanoTime() - cachedExecutionStartTime;
            
            // 批量执行
            long batchStartTime = System.nanoTime();
            for (int iterationCount = 0; iterationCount < TEST_ITERATIONS; iterationCount++) {
                manager.processSingleFormula(sourcePseudoCoordinate, testFormula);
            }
            long batchDuration = System.nanoTime() - batchStartTime;
            
            // 计算统计数据
            double firstExecutionMicroseconds = firstExecutionDuration / 1_000.0;
            double cachedExecutionMicroseconds = cachedExecutionDuration / 1_000.0;
            double averageExecutionMicroseconds = batchDuration / (TEST_ITERATIONS * 1_000.0);
            double cacheSpeedupRatio = firstExecutionMicroseconds / cachedExecutionMicroseconds;
            
            System.out.println("\n缓存效果:");
            System.out.printf("  首次执行: %.3f μs (无缓存)%n", firstExecutionMicroseconds);
            System.out.printf("  缓存执行: %.3f μs (有缓存)%n", cachedExecutionMicroseconds);
            System.out.printf("  平均执行: %.3f μs/次%n", averageExecutionMicroseconds);
            System.out.printf("  缓存加速比: %.2fx%n", cacheSpeedupRatio);
            
            if (cacheSpeedupRatio > 2.0) {
                System.out.println("  ✅ 缓存效果显著");
            } else if (cacheSpeedupRatio > 1.5) {
                System.out.println("  ✅ 缓存效果良好");
            } else {
                System.out.println("  ⚠️ 缓存效果一般");
            }
            
        } catch (Exception exception) {
            System.out.println("❌ 测试异常: " + exception.getMessage());
            exception.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 测试场景4：大规模数据测试
     */
    private static void performanceTestLargeScaleData() {
        System.out.println("【性能测试4】大规模数据");
        System.out.println("----------------------------------------");
        
        try {
            int dataSize = LARGE_DATA_SIZE;
            
            System.out.println("数据规模: " + dataSize);
            System.out.println("创建测试数据...");
            
            // 准备大规模测试数据
            long dataCreationStartTime = System.nanoTime();
            Map<String, List<int[]>> cellPositionMap = createTestCellPositionMap(dataSize);
            List<Long> cellDimensionOrderIds = Arrays.asList(1001L, 2002L, 3001L);
            List<Long> formulaDimensionOrderIds = Arrays.asList(2002L, 3001L, 1001L);
            long dataCreationDuration = System.nanoTime() - dataCreationStartTime;
            
            System.out.printf("数据创建耗时: %.2f ms%n", dataCreationDuration / 1_000_000.0);
            
            // 创建管理器
            long managerCreationStartTime = System.nanoTime();
            FormulaEchoManager manager = new FormulaEchoManager();
            manager.setPseudoCoordinateToTablePositionMap(cellPositionMap);
            manager.setDimensionOrderIds(cellDimensionOrderIds, formulaDimensionOrderIds);
            long managerCreationDuration = System.nanoTime() - managerCreationStartTime;
            
            System.out.printf("管理器初始化耗时: %.2f ms%n", managerCreationDuration / 1_000_000.0);
            
            // 准备批量公式
            int batchSize = 100;
            Map<String, String> batchFormulaMap = createBatchTestFormulas(batchSize);
            
            // 执行批量处理
            long processingStartTime = System.nanoTime();
            Map<String, String> result = manager.processBatchFormulaEcho(batchFormulaMap);
            long processingDuration = System.nanoTime() - processingStartTime;
            
            System.out.println("\n处理结果:");
            System.out.printf("  批量大小: %d%n", batchSize);
            System.out.printf("  处理耗时: %.2f ms%n", processingDuration / 1_000_000.0);
            System.out.printf("  结果数量: %d%n", result.size());
            System.out.printf("  平均耗时: %.3f μs/个%n", processingDuration / (batchSize * 1_000.0));
            
            System.out.println("\n✅ 大规模数据测试完成");
            
        } catch (Exception exception) {
            System.out.println("❌ 测试异常: " + exception.getMessage());
            exception.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 测试场景5：内存使用分析
     */
    private static void performanceTestMemoryUsage() {
        System.out.println("【性能测试5】内存使用分析");
        System.out.println("----------------------------------------");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // 强制GC
            System.gc();
            Thread.sleep(100);
            
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("初始内存: %.2f MB%n", initialMemory / 1_048_576.0);
            
            // 创建测试数据
            int dataSize = 100;
            Map<String, List<int[]>> cellPositionMap = createTestCellPositionMap(dataSize);
            List<Long> cellDimensionOrderIds = Arrays.asList(1001L, 2002L, 3001L);
            List<Long> formulaDimensionOrderIds = Arrays.asList(2002L, 3001L, 1001L);
            
            // 创建管理器
            FormulaEchoManager manager = new FormulaEchoManager();
            manager.setPseudoCoordinateToTablePositionMap(cellPositionMap);
            manager.setDimensionOrderIds(cellDimensionOrderIds, formulaDimensionOrderIds);
            
            long afterInitMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("初始化后内存: %.2f MB%n", afterInitMemory / 1_048_576.0);
            System.out.printf("初始化内存增长: %.2f KB%n", 
                    (afterInitMemory - initialMemory) / 1_024.0);
            
            // 执行批量处理
            int batchSize = 50;
            Map<String, String> batchFormulaMap = createBatchTestFormulas(batchSize);
            manager.processBatchFormulaEcho(batchFormulaMap);
            
            long afterProcessingMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("\n处理后内存: %.2f MB%n", afterProcessingMemory / 1_048_576.0);
            System.out.printf("处理内存增长: %.2f KB%n", 
                    (afterProcessingMemory - afterInitMemory) / 1_024.0);
            
            // 显示缓存统计
            System.out.println("\n" + manager.getStatistics());
            
            // 清除缓存
            manager.clearAllCaches();
            
            // 强制GC
            System.gc();
            Thread.sleep(100);
            
            long afterClearMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("\n清除缓存后内存: %.2f MB%n", afterClearMemory / 1_048_576.0);
            System.out.printf("内存回收: %.2f KB%n", 
                    (afterProcessingMemory - afterClearMemory) / 1_024.0);
            
            System.out.println("\n✅ 内存使用分析完成");
            
        } catch (Exception exception) {
            System.out.println("❌ 测试异常: " + exception.getMessage());
            exception.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 创建测试用的单元格位置映射
     * 
     * @param size 大小
     * @return 单元格映射
     */
    private static Map<String, List<int[]>> createTestCellPositionMap(int size) {
        Map<String, List<int[]>> cellPositionMap = new HashMap<>();
        
        for (int index = 0; index < size; index++) {
            String pseudoCoordinate = "1001_" + (2002 + index) + "_3001";
            int row = 3 + index;
            int column = 4 + index;
            cellPositionMap.put(pseudoCoordinate, Arrays.asList(new int[]{row, column}));
        }
        
        return cellPositionMap;
    }
    
    /**
     * 创建批量测试公式
     * 
     * @param size 大小
     * @return 批量公式映射
     */
    private static Map<String, String> createBatchTestFormulas(int size) {
        Map<String, String> batchFormulaMap = new HashMap<>();
        
        for (int index = 0; index < size; index++) {
            String sourcePseudoCoordinate = "1001_" + (2002 + index) + "_3001";
            String formula = "=#2002_3001_1001#+#2002_3002_1001#";
            batchFormulaMap.put(sourcePseudoCoordinate, formula);
        }
        
        return batchFormulaMap;
    }
}