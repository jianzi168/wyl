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
     * 入口：构造演示依赖图，打印排序入参/出参。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds = buildDemoFormulaMap();
        printHeader();
        printInputMap(formulaIdToPrerequisiteFormulaIds);
        List<List<Long>> executionOrderGroupedByWeakComponent =
                FormulaTopologicalSort.sort(formulaIdToPrerequisiteFormulaIds);
        printExecutionResult(executionOrderGroupedByWeakComponent);
        printFooter();
    }

    /** 组装含链、独立链、菱形三类结构的示例 {@link Map}。 */
    private static Map<Long, List<Long>> buildDemoFormulaMap() {
        Map<Long, List<Long>> m = new HashMap<>();
        addLinearChainDemo(m);
        addDisjointChainDemo(m);
        addDiamondDemo(m);
        return m;
    }

    /** 子图 A：1→2→3 线性链。 */
    private static void addLinearChainDemo(Map<Long, List<Long>> m) {
        m.put(1L, List.of());
        m.put(2L, List.of(1L));
        m.put(3L, List.of(2L));
    }

    /** 子图 B：100→101，与 A 无向不连通，将落入外层 List 的另一元素。 */
    private static void addDisjointChainDemo(Map<Long, List<Long>> m) {
        m.put(100L, List.of());
        m.put(101L, List.of(100L));
    }

    /** 子图 C：菱形 DAG（10 为底，11/12 并行，13 汇聚）。 */
    private static void addDiamondDemo(Map<Long, List<Long>> m) {
        m.put(10L, List.of());
        m.put(11L, List.of(10L));
        m.put(12L, List.of(10L));
        m.put(13L, List.of(11L, 12L));
    }

    /** 打印示例标题与入参说明行。 */
    private static void printHeader() {
        System.out.println("========== 拓扑排序示例 ==========");
        System.out.println("【入参】formulaIdMap（key=公式ID, value=该公式直接依赖的公式ID列表）:");
    }

    /** 按公式 ID 升序打印 map 条目。 */
    private static void printInputMap(Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds) {
        formulaIdToPrerequisiteFormulaIds.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry ->
                                System.out.println(
                                        "  " + entry.getKey() + " -> " + entry.getValue()));
        System.out.println();
    }

    /** 逐行打印各弱连通分量及完整 {@code List<List<Long>>}。 */
    private static void printExecutionResult(List<List<Long>> executionOrderGroupedByWeakComponent) {
        System.out.println(
                "【出参】List<List<Long>>（外层=弱连通子图，按分量最小公式ID排序；内层=子图内 叶子→根）:");
        int n = executionOrderGroupedByWeakComponent.size();
        for (int componentIndex = 0; componentIndex < n; componentIndex++) {
            System.out.println(
                    "  子图["
                            + componentIndex
                            + "]: "
                            + executionOrderGroupedByWeakComponent.get(componentIndex));
        }
        System.out.println("  完整结构: " + executionOrderGroupedByWeakComponent);
    }

    /** 打印底部分隔线。 */
    private static void printFooter() {
        System.out.println("==================================");
    }

    private FormulaTopologicalSortExample() {}
}
