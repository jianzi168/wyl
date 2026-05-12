package formula;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令行演示：打印拓扑排序的入参与出参。运行方式（在 {@code java} 目录下已编译后）:
 * {@code java -cp target/classes formula.FormulaTopologicalSortExample}
 */
public final class FormulaTopologicalSortExample {

    public static void main(String[] args) {
        Map<Long, List<Long>> formulaIdMap = new HashMap<>();
        // 子图 A：链 1 → 2 → 3（3 依赖 2，2 依赖 1）
        formulaIdMap.put(1L, List.of());
        formulaIdMap.put(2L, List.of(1L));
        formulaIdMap.put(3L, List.of(2L));
        // 子图 B：与 A 无交集，链 100 → 101
        formulaIdMap.put(100L, List.of());
        formulaIdMap.put(101L, List.of(100L));
        // 子图 C：菱形 10 → 11、12 → 13
        formulaIdMap.put(10L, List.of());
        formulaIdMap.put(11L, List.of(10L));
        formulaIdMap.put(12L, List.of(10L));
        formulaIdMap.put(13L, List.of(11L, 12L));

        System.out.println("========== 拓扑排序示例 ==========");
        System.out.println("入参 formulaIdMap（key=公式ID, value=该公式直接依赖的公式ID列表）:");
        formulaIdMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println("  " + e.getKey() + " -> " + e.getValue()));
        System.out.println();

        List<List<Long>> result = FormulaTopologicalSort.sort(formulaIdMap);

        System.out.println("出参 List<List<Long>>（外层=互不连通的子图，按分量最小ID排序；内层=该子图内 叶子→根）:");
        for (int i = 0; i < result.size(); i++) {
            System.out.println("  子图[" + i + "]: " + result.get(i));
        }
        System.out.println("完整结构: " + result);
        System.out.println("==================================");
    }

    private FormulaTopologicalSortExample() {}
}
