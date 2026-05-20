/**
 * 单元格引用提取功能测试
 * 验证FormulaParser.extractCellReferences()是否能正确提取各种情况下的单元格引用
 */
public class CellReferenceExtractTest {
    
    public static void main(String[] args) {
        testCase1();
        testCase2();
        testCase3();
        testCase4();
        testCase5();
    }
    
    /**
     * 测试用例1：复杂公式（PerformanceDemo中的示例）
     */
    private static void testCase1() {
        System.out.println("========================================");
        System.out.println("测试用例1: 复杂公式");
        System.out.println("========================================");
        
        String formula = "=B5*(G3+B5+H4)+SUM(A1:B5)+IF(C5>0,MAX(D5,E5),MIN(F5,G5))";
        
        System.out.println("公式: " + formula);
        System.out.println("应该包含: B5, G3, H4, A1, C5, D5, E5, F5, G5");
        
        java.util.Set<String> refs = FormulaParser.extractCellReferences(formula);
        System.out.println("实际提取: " + refs);
        System.out.println("提取数量: " + refs.size());
        
        // 验证
        java.util.Set<String> expected = new java.util.HashSet<>();
        expected.add("B5");
        expected.add("G3");
        expected.add("H4");
        expected.add("A1");
        expected.add("C5");
        expected.add("D5");
        expected.add("E5");
        expected.add("F5");
        expected.add("G5");
        
        boolean passed = refs.equals(expected);
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例2：简单公式
     */
    private static void testCase2() {
        System.out.println("========================================");
        System.out.println("测试用例2: 简单公式");
        System.out.println("========================================");
        
        String formula = "=A1+B2*C3";
        
        System.out.println("公式: " + formula);
        System.out.println("应该包含: A1, B2, C3");
        
        java.util.Set<String> refs = FormulaParser.extractCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        java.util.Set<String> expected = new java.util.HashSet<>();
        expected.add("A1");
        expected.add("B2");
        expected.add("C3");
        
        boolean passed = refs.equals(expected);
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例3：嵌套函数
     */
    private static void testCase3() {
        System.out.println("========================================");
        System.out.println("测试用例3: 嵌套函数");
        System.out.println("========================================");
        
        String formula = "=IF(A1>0,SUM(B1:B5),MAX(C1,D1))";
        
        System.out.println("公式: " + formula);
        System.out.println("应该包含: A1, B1, B5, C1, D1");
        
        java.util.Set<String> refs = FormulaParser.extractCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        java.util.Set<String> expected = new java.util.HashSet<>();
        expected.add("A1");
        expected.add("B1");
        expected.add("B5");
        expected.add("C1");
        expected.add("D1");
        
        boolean passed = refs.equals(expected);
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例4：只含函数名，不应被提取
     */
    private static void testCase4() {
        System.out.println("========================================");
        System.out.println("测试用例4: 函数名不应被提取");
        System.out.println("========================================");
        
        String formula = "=SUM(AVERAGE(B1:B5),MAX(C1,D1))";
        
        System.out.println("公式: " + formula);
        System.out.println("应该包含: B1, B5, C1, D1");
        System.out.println("不应包含: SUM, AVERAGE, MAX");
        
        java.util.Set<String> refs = FormulaParser.extractCellReferences(formula);
        System.out.println("实际提取: " + refs);
        
        java.util.Set<String> expected = new java.util.HashSet<>();
        expected.add("B1");
        expected.add("B5");
        expected.add("C1");
        expected.add("D1");
        
        boolean passed = refs.equals(expected);
        System.out.println("测试结果: " + (passed ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }
    
    /**
     * 测试用例5：空公式和null
     */
    private static void testCase5() {
        System.out.println("========================================");
        System.out.println("测试用例5: 边界情况");
        System.out.println("========================================");
        
        // 空字符串
        java.util.Set<String> refs1 = FormulaParser.extractCellReferences("");
        System.out.println("空字符串: " + refs1 + " - " + (refs1.isEmpty() ? "✅" : "❌"));
        
        // null
        java.util.Set<String> refs2 = FormulaParser.extractCellReferences(null);
        System.out.println("null: " + refs2 + " - " + (refs2.isEmpty() ? "✅" : "❌"));
        
        // 只有操作符
        java.util.Set<String> refs3 = FormulaParser.extractCellReferences("=+1+2");
        System.out.println("只有操作符: " + refs3 + " - " + (refs3.isEmpty() ? "✅" : "❌"));
        
        System.out.println();
    }
}