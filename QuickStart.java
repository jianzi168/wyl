import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 快速使用示例 - 最简单的使用方式
 * 
 * @author Hermes Agent
 */
public class QuickStart {
    
    public static void main(String[] args) {
        // ========== 场景1: 简单替换 ==========
        System.out.println("场景1: 简单替换");
        String formula1 = "=B5*(G3+B5+H4)";
        
        Map<String, String> replacements1 = new HashMap<>();
        replacements1.put("B5", "Sheet2!X10");
        replacements1.put("G3", "Sheet2!Y20");
        replacements1.put("H4", "Sheet2!Z30");
        
        String result1 = ExcelFormulaReplacer.replaceCellReferences(formula1, replacements1);
        System.out.println("原公式: " + formula1);
        System.out.println("结果: " + result1);
        System.out.println();
        
        // ========== 场景2: 复杂公式带函数 ==========
        System.out.println("场景2: 复杂公式带函数");
        String formula2 = "=SUM(B5:D5)+IF(A5>0,MAX(B5,C5),MIN(D5,E5))";
        
        Map<String, String> replacements2 = new HashMap<>();
        replacements2.put("B5", "新表!B10");
        replacements2.put("C5", "新表!C10");
        replacements2.put("D5", "新表!D10");
        replacements2.put("E5", "新表!E10");
        replacements2.put("A5", "新表!A10");
        
        // 使用精确版本，避免误替换函数名
        String result2 = ExcelFormulaReplacer.PreciseReplacer.replace(formula2, replacements2);
        System.out.println("原公式: " + formula2);
        System.out.println("结果: " + result2);
        System.out.println();
        
        // ========== 场景3: 批量替换 ==========
        System.out.println("场景3: 批量替换");
        String[] formulas = {
            "=A1+B1",
            "=SUM(C1:C10)",
            "=IF(D1>0,E1,F1)"
        };
        
        Map<String, String> replacements3 = new HashMap<>();
        replacements3.put("A1", "目标!X1");
        replacements3.put("B1", "目标!Y1");
        replacements3.put("C1", "目标!Z1");
        replacements3.put("D1", "目标!W1");
        replacements3.put("E1", "目标!V1");
        replacements3.put("F1", "目标!U1");
        
        String[] results3 = ExcelFormulaReplacer.replaceBatch(formulas, replacements3);
        for (int i = 0; i < formulas.length; i++) {
            System.out.println(formulas[i] + " → " + results3[i]);
        }
        System.out.println();
        
        // ========== 场景4: 提取单元格引用 ==========
        System.out.println("场景4: 提取单元格引用");
        String formula4 = "=A1+B2*SUM(C3:D4)+IF(E5>0,F5,G5)";
        Set<String> refs = FormulaParser.extractCellReferences(formula4);
        System.out.println("公式: " + formula4);
        System.out.println("单元格引用: " + refs);
        System.out.println();
        
        // ========== 场景5: 性能敏感场景 ==========
        System.out.println("场景5: 性能敏感场景（使用手动解析）");
        String formula5 = "=A1+B1*C1/D1+E1*F1";
        
        Map<String, String> replacements5 = new HashMap<>();
        replacements5.put("A1", "X1");
        replacements5.put("B1", "Y1");
        replacements5.put("C1", "Z1");
        replacements5.put("D1", "W1");
        replacements5.put("E1", "V1");
        replacements5.put("F1", "U1");
        
        String result5 = FormulaParser.replaceReferences(formula5, replacements5);
        System.out.println("原公式: " + formula5);
        System.out.println("结果: " + result5);
        System.out.println();
        
        // ========== 场景6: 部分替换 ==========
        System.out.println("场景6: 部分替换（只替换指定的引用）");
        String formula6 = "=A1+B1+C1+D1";
        
        Map<String, String> replacements6 = new HashMap<>();
        replacements6.put("B1", "新B1");
        replacements6.put("D1", "新D1");
        // 注意：A1和C1不在映射中，会保持不变
        
        String result6 = ExcelFormulaReplacer.replaceCellReferences(formula6, replacements6);
        System.out.println("原公式: " + formula6);
        System.out.println("结果: " + result6);
        System.out.println("说明: 只替换了B1和D1，A1和C1保持不变");
    }
}