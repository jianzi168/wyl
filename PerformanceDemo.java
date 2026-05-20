import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Excel公式替换单元格引用性能测试示例
 * 
 * 展示三种方案的性能对比：
 * 1. 预编译正则 - 简单高效
 * 2. 精确正则 - 避开函数名
 * 3. 手动解析 - 极致性能
 * 
 * @author Hermes Agent
 * @version 1.0
 */
public class PerformanceDemo {
    
    public static void main(String[] args) {
        // 测试公式：包含复杂函数和多个单元格引用
        String formula = "=B5*(G3+B5+H4)+SUM(A1:B5)+IF(C5>0,MAX(D5,E5),MIN(F5,G5))";
        
        // 替换映射
        Map<String, String> replacements = new HashMap<>();
        replacements.put("B5", "Sheet2!X10");
        replacements.put("G3", "Sheet2!Y20");
        replacements.put("H4", "Sheet2!Z30");
        replacements.put("A1", "Sheet2!A1");
        replacements.put("C5", "Sheet2!C5");
        replacements.put("D5", "Sheet2!D5");
        replacements.put("E5", "Sheet2!E5");
        replacements.put("F5", "Sheet2!F5");
        
        System.out.println("========================================");
        System.out.println("Excel公式单元格引用替换性能测试");
        System.out.println("========================================\n");
        
        System.out.println("原始公式: " + formula);
        System.out.println("替换映射: " + replacements);
        System.out.println();
        
        // 方案1：正则表达式
        System.out.println("----------------------------------------");
        System.out.println("方案1: 预编译正则表达式");
        System.out.println("----------------------------------------");
        long start = System.nanoTime();
        String result1 = ExcelFormulaReplacer.replaceCellReferences(formula, replacements);
        long time1 = System.nanoTime() - start;
        System.out.println("替换结果: " + result1);
        System.out.println("单次耗时: " + time1 / 1000.0 + " μs");
        System.out.println();
        
        // 方案2：精确版本（避开函数名）
        System.out.println("----------------------------------------");
        System.out.println("方案2: 精确正则（避开函数名）");
        System.out.println("----------------------------------------");
        start = System.nanoTime();
        String result2 = ExcelFormulaReplacer.PreciseReplacer.replace(formula, replacements);
        long time2 = System.nanoTime() - start;
        System.out.println("替换结果: " + result2);
        System.out.println("单次耗时: " + time2 / 1000.0 + " μs");
        System.out.println();
        
        // 方案3：手动解析
        System.out.println("----------------------------------------");
        System.out.println("方案3: 手动遍历解析");
        System.out.println("----------------------------------------");
        start = System.nanoTime();
        String result3 = FormulaParser.replaceReferences(formula, replacements);
        long time3 = System.nanoTime() - start;
        System.out.println("替换结果: " + result3);
        System.out.println("单次耗时: " + time3 / 1000.0 + " μs");
        System.out.println();
        
        // 提取单元格引用
        System.out.println("----------------------------------------");
        System.out.println("提取公式中的单元格引用");
        System.out.println("----------------------------------------");
        java.util.Set<String> refs = FormulaParser.extractCellReferences(formula);
        System.out.println("单元格引用: " + refs);
        System.out.println();
        
        // 批量性能测试
        System.out.println("----------------------------------------");
        System.out.println("批量性能测试 (10000次迭代)");
        System.out.println("----------------------------------------");
        benchmarkBatch(formula, replacements, 10000);
        System.out.println();
        
        // 更大批量测试
        System.out.println("----------------------------------------");
        System.out.println("大批量测试 (100000次迭代)");
        System.out.println("----------------------------------------");
        benchmarkBatch(formula, replacements, 100000);
        System.out.println();
        
        // 批量公式测试
        System.out.println("----------------------------------------");
        System.out.println("批量公式测试 (1000个公式 × 100次)");
        System.out.println("----------------------------------------");
        benchmarkBatchFormulas(1000, 100);
        System.out.println();
        
        System.out.println("========================================");
        System.out.println("性能对比总结");
        System.out.println("========================================");
        System.out.println("方案    | 10000次 | 平均/次 | 推荐度");
        System.out.println("--------|---------|---------|--------");
        System.out.printf("正则    | %-7d | %-6.2fμs | ⭐⭐⭐⭐⭐\n", 
            TimeUnit.NANOSECONDS.toMillis(benchmarkSingle(formula, replacements, 10000)),
            benchmarkSingle(formula, replacements, 10000) / 10000.0 / 1000.0);
        System.out.printf("精确正则 | %-7d | %-6.2fμs | ⭐⭐⭐⭐\n", 
            TimeUnit.NANOSECONDS.toMillis(benchmarkPrecise(formula, replacements, 10000)),
            benchmarkPrecise(formula, replacements, 10000) / 10000.0 / 1000.0);
        System.out.printf("手动解析 | %-7d | %-6.2fμs | ⭐⭐⭐⭐⭐\n", 
            TimeUnit.NANOSECONDS.toMillis(benchmarkManual(formula, replacements, 10000)),
            benchmarkManual(formula, replacements, 10000) / 10000.0 / 1000.0);
        System.out.println("========================================");
        System.out.println("\n推荐选择：");
        System.out.println("- 日常使用：方案一（预编译正则）- 简单高效");
        System.out.println("- 极限性能：方案二（手动解析）- 适合高并发场景");
        System.out.println("- 需要验证：方案三（Apache POI）- 准确但较慢");
        System.out.println("========================================");
    }
    
    /**
     * 批量性能测试
     */
    private static void benchmarkBatch(String formula, Map<String, String> replacements, int iterations) {
        // 预热
        for (int i = 0; i < 1000; i++) {
            ExcelFormulaReplacer.replaceCellReferences(formula, replacements);
            FormulaParser.replaceReferences(formula, replacements);
        }
        
        // 测试正则方案
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ExcelFormulaReplacer.replaceCellReferences(formula, replacements);
        }
        long regexTime = System.nanoTime() - start;
        
        // 测试精确正则方案
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ExcelFormulaReplacer.PreciseReplacer.replace(formula, replacements);
        }
        long preciseTime = System.nanoTime() - start;
        
        // 测试手动解析方案
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            FormulaParser.replaceReferences(formula, replacements);
        }
        long manualTime = System.nanoTime() - start;
        
        System.out.printf("正则方案  : %5d ms (平均 %6.2f μs/次)\n",
            TimeUnit.NANOSECONDS.toMillis(regexTime),
            regexTime / (iterations * 1000.0));
        System.out.printf("精确正则  : %5d ms (平均 %6.2f μs/次)\n",
            TimeUnit.NANOSECONDS.toMillis(preciseTime),
            preciseTime / (iterations * 1000.0));
        System.out.printf("手动解析  : %5d ms (平均 %6.2f μs/次)\n",
            TimeUnit.NANOSECONDS.toMillis(manualTime),
            manualTime / (iterations * 1000.0));
    }
    
    /**
     * 批量公式性能测试
     */
    private static void benchmarkBatchFormulas(int formulaCount, int iterations) {
        // 生成测试公式
        String[] formulas = new String[formulaCount];
        for (int i = 0; i < formulaCount; i++) {
            formulas[i] = "=A" + (i + 1) + "+B" + (i + 1) + "*C" + (i + 1);
        }
        
        Map<String, String> replacements = new HashMap<>();
        replacements.put("A1", "X1");
        replacements.put("B1", "Y1");
        replacements.put("C1", "Z1");
        
        // 预热
        for (int i = 0; i < 100; i++) {
            ExcelFormulaReplacer.replaceCellReferences(formulas[0], replacements);
            FormulaParser.replaceReferences(formulas[0], replacements);
        }
        
        // 测试正则批量方案
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ExcelFormulaReplacer.replaceBatch(formulas, replacements);
        }
        long regexTime = System.nanoTime() - start;
        
        // 测试手动解析批量方案
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            FormulaParser.replaceBatch(formulas, replacements);
        }
        long manualTime = System.nanoTime() - start;
        
        int totalOps = formulaCount * iterations;
        System.out.printf("正则批量  : %5d ms (平均 %6.2f μs/个)\n",
            TimeUnit.NANOSECONDS.toMillis(regexTime),
            regexTime / (totalOps * 1000.0));
        System.out.printf("手动批量  : %5d ms (平均 %6.2f μs/个)\n",
            TimeUnit.NANOSECONDS.toMillis(manualTime),
            manualTime / (totalOps * 1000.0));
    }
    
    // 单独测试方法（用于表格输出）
    private static long benchmarkSingle(String formula, Map<String, String> replacements, int iterations) {
        // 预热
        for (int i = 0; i < 1000; i++) {
            ExcelFormulaReplacer.replaceCellReferences(formula, replacements);
        }
        
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ExcelFormulaReplacer.replaceCellReferences(formula, replacements);
        }
        return System.nanoTime() - start;
    }
    
    private static long benchmarkPrecise(String formula, Map<String, String> replacements, int iterations) {
        // 预热
        for (int i = 0; i < 1000; i++) {
            ExcelFormulaReplacer.PreciseReplacer.replace(formula, replacements);
        }
        
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ExcelFormulaReplacer.PreciseReplacer.replace(formula, replacements);
        }
        return System.nanoTime() - start;
    }
    
    private static long benchmarkManual(String formula, Map<String, String> replacements, int iterations) {
        // 预热
        for (int i = 0; i < 1000; i++) {
            FormulaParser.replaceReferences(formula, replacements);
        }
        
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            FormulaParser.replaceReferences(formula, replacements);
        }
        return System.nanoTime() - start;
    }
}