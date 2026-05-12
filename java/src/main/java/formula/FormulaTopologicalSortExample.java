package formula;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令行演示：构造一张「多子图」公式依赖表，调用 {@link FormulaTopologicalSort#sort(Map)} 并打印入参与出参。
 *
 * <p>本示例刻意包含三类结构，便于对照 {@link FormulaTopologicalSort} 的文档：
 *
 * <ul>
 *   <li><strong>链</strong>：1→2→3，唯一拓扑序（在同层无分叉的前提下）。
 *   <li><strong>另一链</strong>：100→101，与上一链无向不连通 → 会出现在外层 List 的不同元素中。
 *   <li><strong>菱形</strong>：10 为公共前置，11、12 并行依赖 10，13 同时依赖 11 与 12 → 11 与 12 的相对先后可交换，但必在 10 之后、13 之前。
 * </ul>
 *
 * <p><strong>如何运行</strong>（需先在本模块执行 {@code mvn compile}）：
 *
 * <pre>{@code
 * java -cp target/classes formula.FormulaTopologicalSortExample
 * }</pre>
 *
 * （在 Maven 模块 {@code java/} 目录下，{@code target/classes} 为编译输出。）
 */
public final class FormulaTopologicalSortExample {

    /**
     * 组装示例 {@code formulaIdMap}，调用拓扑排序，将「入参 / 出参」打印到标准输出。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        // key = 待求公式 ID；value = 必须先算完的公式 ID 列表（空列表表示无公式依赖）
        Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds = new HashMap<>();

        // ----- 子图 A：线性依赖链（模拟逐级汇总） -----
        formulaIdToPrerequisiteFormulaIds.put(1L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(2L, List.of(1L));
        formulaIdToPrerequisiteFormulaIds.put(3L, List.of(2L));

        // ----- 子图 B：与 A 无任何无向连接，故为另一弱连通分量 -----
        formulaIdToPrerequisiteFormulaIds.put(100L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(101L, List.of(100L));

        // ----- 子图 C：菱形 DAG（公共底 + 分叉 + 汇聚） -----
        formulaIdToPrerequisiteFormulaIds.put(10L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(11L, List.of(10L));
        formulaIdToPrerequisiteFormulaIds.put(12L, List.of(10L));
        formulaIdToPrerequisiteFormulaIds.put(13L, List.of(11L, 12L));

        System.out.println("========== 拓扑排序示例 ==========");
        System.out.println("【入参】formulaIdMap（key=公式ID, value=该公式直接依赖的公式ID列表）:");
        formulaIdToPrerequisiteFormulaIds.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry ->
                                System.out.println(
                                        "  " + entry.getKey() + " -> " + entry.getValue()));
        System.out.println();

        // 出参：外层每个 List 对应一个弱连通分量；内层为「叶子→根」的某一合法拓扑序
        List<List<Long>> executionOrderGroupedByWeakComponent =
                FormulaTopologicalSort.sort(formulaIdToPrerequisiteFormulaIds);

        System.out.println(
                "【出参】List<List<Long>>（外层=弱连通子图，按分量最小公式ID排序；内层=子图内 叶子→根）:");
        for (int componentIndex = 0;
                componentIndex < executionOrderGroupedByWeakComponent.size();
                componentIndex++) {
            System.out.println(
                    "  子图["
                            + componentIndex
                            + "]: "
                            + executionOrderGroupedByWeakComponent.get(componentIndex));
        }
        System.out.println("  完整结构: " + executionOrderGroupedByWeakComponent);
        System.out.println("==================================");
    }

    private FormulaTopologicalSortExample() {}
}
