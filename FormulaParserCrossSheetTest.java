/**
 * 跨表单元格引用提取功能测试
 * 验证FormulaParser.extractCrossSheetCellReferences()是否能正确提取跨表引用并返回位置索引
 */
public class FormulaParserCrossSheetTest {
    
    public static void main(String[] args) {
        testCase1();
        testCase2();
        testCase3();
        testCase4();
        testCase5();
        testCase6();
        testCase7();
        testCase8();
        testPerformance();
    }
    
    /**
     * 测试用例1：用户示例 - 基本跨表引用
     */
    private static void testCase1() {
        System.out.println("========================================");
        System.out.println("测试用例1: 用户示例 - 基本跨表引用");
        System.out.println("========================================");
        
        String formula = "=B3+SUM(C4:C10)+表单1!B3";
        
        System.out.println("公式: " + formula);
        System.out.println("预期跨表引用: {4: \"表单1!B3\"}");
        System.out.println("解释: B3=1, C4=2, C10=3, 表单1!B3=4");
        
        java.util.Map<Integer, String> refs = FormulaParser.extractCrossSheetCellReferences(formula);
        System.out.println("实际提取: " + refs);
        System.out.println("提取数量: " + refs.size());
        
        boolean passed = refs.size() == 1 && 
                        refs.containsKey(4) && 
                        "表单1!B3".equals(refs.get(4));
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例2：多个跨表引用
     */
    private static void testCase2() {
        System.out.println("========================================");
        System.out.println("测试用例2: 多个跨表引用");
        System.out.println("========================================");
        
        String formula = "=Sheet1!A1+Sheet2!B2+Sheet3!C3";
        
        System.out.println("公式: " + formula);
        System.out.println("预期跨表引用: {1: \"Sheet1!A1\", 2: \"Sheet2!B2\", 3: \"Sheet3!C3\"}");
        
        java.util.Map<Integer, String> refs = FormulaParser.extractCrossSheetCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        boolean passed = refs.size() == 3 &&
                        "Sheet1!A1".equals(refs.get(1)) &&
                        "Sheet2!B2".equals(refs.get(2)) &&
                        "Sheet3!C3".equals(refs.get(3));
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例3：混合本地和跨表引用
     */
    private static void testCase3() {
        System.out.println("========================================");
        System.out.println("测试用例3: 混合本地和跨表引用");
        System.out.println("========================================");
        
        String formula = "=A1+Sheet2!B1+C1+Sheet3!D1+E1";
        
        System.out.println("公式: " + formula);
        System.out.println("预期跨表引用: {2: \"Sheet2!B1\", 4: \"Sheet3!D1\"}");
        System.out.println("说明: A1=1(本地), Sheet2!B1=2(跨表), C1=3(本地), Sheet3!D1=4(跨表), E1=5(本地)");
        
        java.util.Map<Integer, String> refs = FormulaParser.extractCrossSheetCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        boolean passed = refs.size() == 2 &&
                        "Sheet2!B1".equals(refs.get(2)) &&
                        "Sheet3!D1".equals(refs.get(4));
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例4：引号表名
     */
    private static void testCase4() {
        System.out.println("========================================");
        System.out.println("测试用例4: 引号表名");
        System.out.println("========================================");
        
        String formula = "='My Sheet'!A1+'Another Sheet'!B2";
        
        System.out.println("公式: " + formula);
        System.out.println("预期跨表引用: {1: \"'My Sheet'!A1\", 2: \"'Another Sheet'!B2\"}");
        
        java.util.Map<Integer, String> refs = FormulaParser.extractCrossSheetCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        boolean passed = refs.size() == 2 &&
                        "'My Sheet'!A1".equals(refs.get(1)) &&
                        "'Another Sheet'!B2".equals(refs.get(2));
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例5：绝对引用
     */
    private static void testCase5() {
        System.out.println("========================================");
        System.out.println("测试用例5: 绝对引用");
        System.out.println("========================================");
        
        String formula = "=Sheet1!$A$1+Sheet2!$B$2+Sheet3!C$3";
        
        System.out.println("公式: " + formula);
        
        java.util.Map<Integer, String> refs = FormulaParser.extractCrossSheetCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        boolean passed = refs.size() == 3 &&
                        "Sheet1!$A$1".equals(refs.get(1)) &&
                        "Sheet2!$B$2".equals(refs.get(2)) &&
                        "Sheet3!C$3".equals(refs.get(3));
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例6：函数参数中的跨表引用
     */
    private static void testCase6() {
        System.out.println("========================================");
        System.out.println("测试用例6: 函数参数中的跨表引用");
        System.out.println("========================================");
        
        String formula = "=SUM(Sheet1!A1,Sheet2!B2)+IF(C3>0,Sheet3!D3,0)";
        
        System.out.println("公式: " + formula);
        System.out.println("预期跨表引用: {1: \"Sheet1!A1\", 2: \"Sheet2!B2\", 3: \"Sheet3!D3\"}");
        
        java.util.Map<Integer, String> refs = FormulaParser.extractCrossSheetCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        boolean passed = refs.size() == 3 &&
                        "Sheet1!A1".equals(refs.get(1)) &&
                        "Sheet2!B2".equals(refs.get(2)) &&
                        "Sheet3!D3".equals(refs.get(3));
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例7：边界条件
     */
    private static void testCase7() {
        System.out.println("========================================");
        System.out.println("测试用例7: 边界条件");
        System.out.println("========================================");
        
        // null
        java.util.Map<Integer, String> refs1 = FormulaParser.extractCrossSheetCellReferences(null);
        System.out.println("null: " + refs1 + " - " + (refs1.isEmpty() ? "✅" : "❌"));
        
        // 空字符串
        java.util.Map<Integer, String> refs2 = FormulaParser.extractCrossSheetCellReferences("");
        System.out.println("空字符串: " + refs2 + " - " + (refs2.isEmpty() ? "✅" : "❌"));
        
        // 无单元格引用
        java.util.Map<Integer, String> refs3 = FormulaParser.extractCrossSheetCellReferences("=1+2+3");
        System.out.println("无单元格引用: " + refs3 + " - " + (refs3.isEmpty() ? "✅" : "❌"));
        
        // 只有本地引用
        java.util.Map<Integer, String> refs4 = FormulaParser.extractCrossSheetCellReferences("=A1+B2+C3");
        System.out.println("只有本地引用: " + refs4 + " - " + (refs4.isEmpty() ? "✅" : "❌"));
        
        // 字符串中的跨表引用（不应被提取）
        java.util.Map<Integer, String> refs5 = FormulaParser.extractCrossSheetCellReferences("=\"Sheet1!A1\"+B1");
        System.out.println("字符串中的跨表引用: " + refs5 + " - " + (refs5.isEmpty() ? "✅" : "❌"));
        
        System.out.println();
    }
    
    /**
     * 测试用例8：复杂表名（中文、点、下划线）
     */
    private static void testCase8() {
        System.out.println("========================================");
        System.out.println("测试用例8: 复杂表名");
        System.out.println("========================================");
        
        String formula = "=数据表1!A1+报表_2.0!B1+中文表单!C1";
        
        System.out.println("公式: " + formula);
        System.out.println("预期跨表引用: {1: \"数据表1!A1\", 2: \"报表_2.0!B1\", 3: \"中文表单!C1\"}");
        
        java.util.Map<Integer, String> refs = FormulaParser.extractCrossSheetCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        boolean passed = refs.size() == 3 &&
                        "数据表1!A1".equals(refs.get(1)) &&
                        "报表_2.0!B1".equals(refs.get(2)) &&
                        "中文表单!C1".equals(refs.get(3));
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 性能测试
     */
    private static void testPerformance() {
        System.out.println("========================================");
        System.out.println("性能测试");
        System.out.println("========================================");
        
        // 简单公式
        String simpleFormula = "=Sheet1!A1+Sheet2!B2+Sheet3!C3";
        int iterations = 100_000;
        
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            FormulaParser.extractCrossSheetCellReferences(simpleFormula);
        }
        long endTime = System.nanoTime();
        double avgTimeMicros = (endTime - startTime) / 1000.0 / iterations;
        
        System.out.printf("简单公式性能: %.3f μs/次 (%d 次迭代)\n", avgTimeMicros, iterations);
        System.out.printf("性能目标: < 10 μs，结果: %s\n\n", avgTimeMicros < 10.0 ? "✅ 达标" : "❌ 未达标");
        
        // 复杂公式（20个引用）
        StringBuilder complexFormula = new StringBuilder("=");
        for (int i = 1; i <= 20; i++) {
            if (i > 1) complexFormula.append("+");
            complexFormula.append(String.format("Sheet%d!A%d", i, i));
        }
        
        iterations = 10_000;
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            FormulaParser.extractCrossSheetCellReferences(complexFormula.toString());
        }
        endTime = System.nanoTime();
        avgTimeMicros = (endTime - startTime) / 1000.0 / iterations;
        
        System.out.printf("复杂公式性能: %.3f μs/次 (%d 次迭代, 20个引用)\n", avgTimeMicros, iterations);
        System.out.printf("性能目标: < 30 μs，结果: %s\n\n", avgTimeMicros < 30.0 ? "✅ 达标" : "❌ 未达标");
        
        // 混合公式
        String mixedFormula = "=A1+Sheet1!B1+C2+Sheet2!D2+E3+Sheet3!F3+G4+Sheet4!H4";
        
        iterations = 50_000;
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            FormulaParser.extractCrossSheetCellReferences(mixedFormula);
        }
        endTime = System.nanoTime();
        avgTimeMicros = (endTime - startTime) / 1000.0 / iterations;
        
        System.out.printf("混合公式性能: %.3f μs/次 (%d 次迭代)\n", avgTimeMicros, iterations);
        System.out.printf("性能目标: < 15 μs，结果: %s\n", avgTimeMicros < 15.0 ? "✅ 达标" : "❌ 未达标");
        System.out.println();
    }
}