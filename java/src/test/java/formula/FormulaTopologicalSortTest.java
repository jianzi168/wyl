package formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * {@link FormulaTopologicalSort} 的单元测试。
 *
 * <p>辅助方法说明：
 *
 * <ul>
 *   <li>{@link #flatten(List)}：将「按弱连通分量拆分」的执行顺序拼接成一条长序列，仅用于校验；真实调度应以分量为单位。
 *   <li>{@link #assertValidTopologicalOrder(Map, List)}：对每条「当前公式 → 前置列表」断言所有非 null 前置在长序列中的下标小于当前公式（分量间无边，故拼接序不破坏约束）。
 *   <li>{@link #printIo(String, Map, List)}：在控制台打印【入参】【出参】，便于本地 {@code mvn test} 肉眼验收。
 * </ul>
 */
class FormulaTopologicalSortTest {

    /**
     * 将各弱连通分量上的拓扑序按外层 List 顺序依次拼接为一条序列（仅用于断言；调度仍应以分量为单位）。
     *
     * @param executionOrderByWeakComponent {@link FormulaTopologicalSort#sort} 的返回值；{@code null} 时返回 {@code null}
     */
    private static List<Long> flatten(List<List<Long>> executionOrderByWeakComponent) {
        if (executionOrderByWeakComponent == null) {
            return null;
        }
        List<Long> flattenedExecutionOrder = new ArrayList<>();
        for (List<Long> singleWeakComponentOrder : executionOrderByWeakComponent) {
            flattenedExecutionOrder.addAll(singleWeakComponentOrder);
        }
        return flattenedExecutionOrder;
    }

    /**
     * 在控制台打印用例名称、入参 map、出参分量列表（便于 {@code mvn test} 肉眼验收）。
     *
     * @param scenarioName 用例标识
     * @param formulaIdToPrerequisiteFormulaIds 入参；可为 {@code null}
     * @param executionOrderByWeakComponent 出参；环场景下可能为 {@code null}
     */
    private static void printIo(
            String scenarioName,
            Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds,
            List<List<Long>> executionOrderByWeakComponent) {
        printScenarioBanner(scenarioName);
        printInputSection(formulaIdToPrerequisiteFormulaIds);
        printOutputSection(executionOrderByWeakComponent);
        printScenarioFooter();
    }

    /** 打印用例横幅（空行 + 标题）。 */
    private static void printScenarioBanner(String scenarioName) {
        System.out.println();
        System.out.println("========== " + scenarioName + " ==========");
    }

    /**
     * 打印入参区块：{@code null}、空 map、或非空时按键排序输出各 entry（null key 排在最前）。
     */
    private static void printInputSection(Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds) {
        System.out.println("【入参】formulaIdMap<Long, List<Long>>（key=公式ID，value=直接依赖的公式ID）:");
        if (formulaIdToPrerequisiteFormulaIds == null) {
            System.out.println("  null");
            return;
        }
        if (formulaIdToPrerequisiteFormulaIds.isEmpty()) {
            System.out.println("  {}");
            return;
        }
        printSortedEntries(formulaIdToPrerequisiteFormulaIds);
    }

    /** 将 map entry 按键排序后逐行打印。 */
    private static void printSortedEntries(Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds) {
        formulaIdToPrerequisiteFormulaIds.entrySet().stream()
                .sorted(
                        Map.Entry.<Long, List<Long>>comparingByKey(
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(
                        entry ->
                                System.out.println(
                                        "  " + entry.getKey() + " -> " + entry.getValue()));
    }

    /**
     * 打印出参区块：{@code null}（无合法序）、空列表、或非空时的整体 toString、逐分量行与总步数。
     */
    private static void printOutputSection(List<List<Long>> executionOrderByWeakComponent) {
        System.out.println("【出参】List<List<Long>>（外层=互不连通的子图，内层=该子图内 叶子→根）:");
        if (executionOrderByWeakComponent == null) {
            System.out.println("  null（无合法拓扑序，例如存在环）");
            return;
        }
        if (executionOrderByWeakComponent.isEmpty()) {
            System.out.println("  []");
            return;
        }
        printNonEmptyOutput(executionOrderByWeakComponent);
    }

    /** 非空出参：打印完整 list、各子图一行、公式总步数。 */
    private static void printNonEmptyOutput(List<List<Long>> executionOrderByWeakComponent) {
        System.out.println("  " + executionOrderByWeakComponent);
        int size = executionOrderByWeakComponent.size();
        for (int weakComponentIndex = 0; weakComponentIndex < size; weakComponentIndex++) {
            System.out.println(
                    "    子图["
                            + weakComponentIndex
                            + "]: "
                            + executionOrderByWeakComponent.get(weakComponentIndex));
        }
        int totalFormulaSteps =
                executionOrderByWeakComponent.stream().mapToInt(List::size).sum();
        System.out.println("  公式总步数: " + totalFormulaSteps);
    }

    /** 打印用例结束分隔线。 */
    private static void printScenarioFooter() {
        System.out.println("==========================================");
    }

    /**
     * 断言：对 map 中每个非 null key，其每个非 null 前置在扁平序列中的位置严格小于该 key 的位置。
     *
     * @param formulaIdToPrerequisiteFormulaIds 与 {@link FormulaTopologicalSort#sort} 入参一致
     * @param executionOrderByWeakComponent 与 {@link FormulaTopologicalSort#sort} 返回值一致
     */
    private static void assertValidTopologicalOrder(
            Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds,
            List<List<Long>> executionOrderByWeakComponent) {
        List<Long> flattenedOrder = flatten(executionOrderByWeakComponent);
        Map<Long, Integer> formulaIdToPositionInFlattenedOrder = indexPositions(flattenedOrder);
        for (Map.Entry<Long, List<Long>> formulaEntry : formulaIdToPrerequisiteFormulaIds.entrySet()) {
            assertEntryRespectsOrder(formulaEntry, formulaIdToPositionInFlattenedOrder, flattenedOrder);
        }
    }

    /** 将扁平序列中每个公式 ID 映射为其首次出现下标。 */
    private static Map<Long, Integer> indexPositions(List<Long> flattenedOrder) {
        Map<Long, Integer> formulaIdToPositionInFlattenedOrder = new HashMap<>();
        for (int position = 0; position < flattenedOrder.size(); position++) {
            formulaIdToPositionInFlattenedOrder.put(flattenedOrder.get(position), position);
        }
        return formulaIdToPositionInFlattenedOrder;
    }

    /** 校验单条 map entry：跳过 null key 与 null 整条 value。 */
    private static void assertEntryRespectsOrder(
            Map.Entry<Long, List<Long>> formulaEntry,
            Map<Long, Integer> formulaIdToPositionInFlattenedOrder,
            List<Long> flattenedOrder) {
        Long dependentFormulaId = formulaEntry.getKey();
        if (dependentFormulaId == null) {
            return;
        }
        List<Long> prerequisiteFormulaIds = formulaEntry.getValue();
        if (prerequisiteFormulaIds == null) {
            return;
        }
        int dependentPosition = formulaIdToPositionInFlattenedOrder.get(dependentFormulaId);
        for (Long prerequisiteFormulaId : prerequisiteFormulaIds) {
            assertPrerequisiteBeforeDependent(
                    prerequisiteFormulaId,
                    dependentFormulaId,
                    dependentPosition,
                    formulaIdToPositionInFlattenedOrder,
                    flattenedOrder);
        }
    }

    /**
     * 断言单个前置在扁平序中位于当前公式之前；{@code null} 前置跳过。
     */
    private static void assertPrerequisiteBeforeDependent(
            Long prerequisiteFormulaId,
            Long dependentFormulaId,
            int dependentPosition,
            Map<Long, Integer> formulaIdToPositionInFlattenedOrder,
            List<Long> flattenedOrder) {
        if (prerequisiteFormulaId == null) {
            return;
        }
        assertTrue(
                formulaIdToPositionInFlattenedOrder.get(prerequisiteFormulaId) < dependentPosition,
                () ->
                        "依赖 "
                                + prerequisiteFormulaId
                                + " 须在公式 "
                                + dependentFormulaId
                                + " 之前，顺序="
                                + flattenedOrder);
    }

    @Test
    void sort_nullMap_returnsEmpty() {
        List<List<Long>> groups = FormulaTopologicalSort.sort(null);
        printIo("sort_nullMap_returnsEmpty", null, groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    void sort_emptyMap_returnsEmpty() {
        List<List<Long>> groups = FormulaTopologicalSort.sort(Collections.emptyMap());
        printIo("sort_emptyMap_returnsEmpty", Collections.emptyMap(), groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    void sort_singleNode_noDeps() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, Collections.emptyList());
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_singleNode_noDeps", m, groups);
        assertEquals(List.of(List.of(1L)), groups);
        assertValidTopologicalOrder(m, groups);
    }

    @Test
    void sort_linearChain() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, Collections.emptyList());
        m.put(2L, List.of(1L));
        m.put(3L, List.of(2L));
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_linearChain", m, groups);
        assertEquals(List.of(List.of(1L, 2L, 3L)), groups);
        assertValidTopologicalOrder(m, groups);
    }

    @Test
    void sort_diamond() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, Collections.emptyList());
        m.put(2L, List.of(1L));
        m.put(3L, List.of(1L));
        m.put(4L, Arrays.asList(2L, 3L));
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_diamond", m, groups);
        assertEquals(List.of(List.of(1L, 2L, 3L, 4L)), groups);
        assertValidTopologicalOrder(m, groups);
        List<Long> order = flatten(groups);
        assertTrue(order.indexOf(1L) < order.indexOf(2L));
        assertTrue(order.indexOf(1L) < order.indexOf(3L));
        assertTrue(order.indexOf(2L) < order.indexOf(4L));
        assertTrue(order.indexOf(3L) < order.indexOf(4L));
    }

    @Test
    void sort_dependencyOnlyAsLeaf_notInKeys() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(20L, List.of(10L));
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_dependencyOnlyAsLeaf_notInKeys", m, groups);
        assertEquals(List.of(List.of(10L, 20L)), groups);
        assertValidTopologicalOrder(m, groups);
    }

    @Test
    void sort_nullDependencyList_treatedAsNoDeps() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, null);
        m.put(2L, List.of(1L));
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_nullDependencyList_treatedAsNoDeps", m, groups);
        assertValidTopologicalOrder(m, groups);
        List<Long> order = flatten(groups);
        assertTrue(order.indexOf(1L) < order.indexOf(2L));
    }

    @Test
    void sort_skipsNullKey_butStillIncludesOrphanDependencies() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(null, List.of(99L));
        m.put(1L, Collections.emptyList());
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_skipsNullKey_butStillIncludesOrphanDependencies", m, groups);
        assertEquals(List.of(List.of(1L), List.of(99L)), groups);
    }

    @Test
    void sort_skipsNullInDependencyList() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, Collections.emptyList());
        m.put(2L, Arrays.asList(null, 1L, null));
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_skipsNullInDependencyList", m, groups);
        assertValidTopologicalOrder(m, groups);
        List<Long> order = flatten(groups);
        assertTrue(order.indexOf(1L) < order.indexOf(2L));
    }

    @Test
    void sort_twoDisjointChains_twoInnerLists() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, Collections.emptyList());
        m.put(2L, List.of(1L));
        m.put(100L, Collections.emptyList());
        m.put(101L, List.of(100L));
        List<List<Long>> groups = FormulaTopologicalSort.sort(m);
        printIo("sort_twoDisjointChains_twoInnerLists", m, groups);
        assertEquals(2, groups.size());
        assertEquals(Set.of(1L, 2L), new HashSet<>(groups.get(0)));
        assertEquals(Set.of(100L, 101L), new HashSet<>(groups.get(1)));
        assertEquals(List.of(1L, 2L), groups.get(0));
        assertEquals(List.of(100L, 101L), groups.get(1));
        assertValidTopologicalOrder(m, groups);
    }

    @Test
    void sort_cycle_throws() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, List.of(2L));
        m.put(2L, List.of(1L));
        printIo("sort_cycle_throws（预期抛异常，无合法顺序）", m, null);
        assertThrows(IllegalStateException.class, () -> FormulaTopologicalSort.sort(m));
    }

    @Test
    void sort_selfLoop_throws() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, List.of(1L));
        printIo("sort_selfLoop_throws（预期抛异常）", m, null);
        assertThrows(IllegalStateException.class, () -> FormulaTopologicalSort.sort(m));
    }

    @Test
    void trySort_cycle_returnsNull() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, List.of(2L));
        m.put(2L, List.of(1L));
        List<List<Long>> groups = FormulaTopologicalSort.trySort(m);
        printIo("trySort_cycle_returnsNull", m, groups);
        assertNull(groups);
    }

    @Test
    void trySort_acyclic_returnsOrder() {
        Map<Long, List<Long>> m = new HashMap<>();
        m.put(1L, List.of(2L));
        m.put(2L, Collections.emptyList());
        List<List<Long>> groups = FormulaTopologicalSort.trySort(m);
        printIo("trySort_acyclic_returnsOrder", m, groups);
        assertNotNull(groups);
        assertEquals(List.of(List.of(2L, 1L)), groups);
        assertValidTopologicalOrder(m, groups);
    }

    @Test
    void sort_demo_familyBudgetStyleFormulas_printsIo() {
        Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds = buildFamilyBudgetDemoMap();
        List<List<Long>> executionOrderByWeakComponent =
                FormulaTopologicalSort.sort(formulaIdToPrerequisiteFormulaIds);
        printIo(
                "实际例子-家计类公式（两子图：汇总链 + 独立链）",
                formulaIdToPrerequisiteFormulaIds,
                executionOrderByWeakComponent);
        assertEquals(2, executionOrderByWeakComponent.size());
        assertValidTopologicalOrder(
                formulaIdToPrerequisiteFormulaIds, executionOrderByWeakComponent);
    }

    /**
     * 构造「家计」风格示例：一大子图（收入/支出汇总→结余）与另一独立链（5001→5002），预期外层 List 大小为 2。
     */
    private static Map<Long, List<Long>> buildFamilyBudgetDemoMap() {
        Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds = new HashMap<>();
        formulaIdToPrerequisiteFormulaIds.put(1001L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(1002L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(1100L, List.of(1001L, 1002L));
        formulaIdToPrerequisiteFormulaIds.put(2001L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(2002L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(2100L, List.of(2001L, 2002L));
        formulaIdToPrerequisiteFormulaIds.put(3000L, List.of(1100L, 2100L));
        formulaIdToPrerequisiteFormulaIds.put(5001L, List.of());
        formulaIdToPrerequisiteFormulaIds.put(5002L, List.of(5001L));
        return formulaIdToPrerequisiteFormulaIds;
    }
}
