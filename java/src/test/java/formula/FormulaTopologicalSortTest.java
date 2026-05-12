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
     * 将多个弱连通分量上的拓扑序依次拼接为一条 {@code List<Long>}。
     *
     * <p>注意：拼接顺序必须与 {@link FormulaTopologicalSort#sort} 返回的外层 List 顺序一致（本测试直接 flatten 排序结果）。
     * 任意两个不同分量之间没有依赖边，因此「分量 A 全部在前、分量 B 全部在后」的扁平序列仍满足每条边的先后约束。
     *
     * @param executionOrderByWeakComponent {@link FormulaTopologicalSort#sort} 的返回值；{@code null} 时返回 {@code null}
     * @return 扁平化后的公式 ID 序列；若入参为 {@code null} 则返回 {@code null}
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
     * 在标准输出打印单条用例的入参（依赖图）与出参（排序结果）。
     *
     * <p>入参按 key 排序打印（含 {@code null} key 时排在最前），便于与业务日志对齐；出参除整体 {@code toString()} 外，逐个子图打印一行。
     *
     * @param scenarioName 用例名称（建议与 {@code @Test} 方法名一致）
     * @param formulaIdToPrerequisiteFormulaIds 与生产 API 相同的 map；可为 {@code null}
     * @param executionOrderByWeakComponent 排序结果；环检测场景下可能为 {@code null}（{@link FormulaTopologicalSort#trySort}）
     */
    private static void printIo(
            String scenarioName,
            Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds,
            List<List<Long>> executionOrderByWeakComponent) {
        System.out.println();
        System.out.println("========== " + scenarioName + " ==========");
        System.out.println("【入参】formulaIdMap<Long, List<Long>>（key=公式ID，value=直接依赖的公式ID）:");
        if (formulaIdToPrerequisiteFormulaIds == null) {
            System.out.println("  null");
        } else if (formulaIdToPrerequisiteFormulaIds.isEmpty()) {
            System.out.println("  {}");
        } else {
            formulaIdToPrerequisiteFormulaIds.entrySet().stream()
                    .sorted(
                            Map.Entry.<Long, List<Long>>comparingByKey(
                                    Comparator.nullsFirst(Comparator.naturalOrder())))
                    .forEach(
                            entry ->
                                    System.out.println(
                                            "  " + entry.getKey() + " -> " + entry.getValue()));
        }
        System.out.println("【出参】List<List<Long>>（外层=互不连通的子图，内层=该子图内 叶子→根）:");
        if (executionOrderByWeakComponent == null) {
            System.out.println("  null（无合法拓扑序，例如存在环）");
        } else if (executionOrderByWeakComponent.isEmpty()) {
            System.out.println("  []");
        } else {
            System.out.println("  " + executionOrderByWeakComponent);
            for (int weakComponentIndex = 0;
                    weakComponentIndex < executionOrderByWeakComponent.size();
                    weakComponentIndex++) {
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
        System.out.println("==========================================");
    }

    /**
     * 断言：对 {@code formulaIdToPrerequisiteFormulaIds} 中每个非 null 的 key（当前公式），其每个非 null 前置在
     * {@code flattenedOrder} 中的位置必须<strong>严格小于</strong>当前公式的位置。
     *
     * <p>不检查 key 为 {@code null} 的条目；不检查 value 为 {@code null} 的整条依赖（与主实现「无依赖」语义一致）。
     * 若前置未出现在 flatten 序列中（理论上不应发生于与主实现一致的输入），{@link Map#get} 将抛 NPE 或断言失败，便于暴露数据错误。
     *
     * @param formulaIdToPrerequisiteFormulaIds 与 {@link FormulaTopologicalSort#sort} 入参相同
     * @param executionOrderByWeakComponent 与 {@link FormulaTopologicalSort#sort} 返回值相同
     */
    private static void assertValidTopologicalOrder(
            Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds,
            List<List<Long>> executionOrderByWeakComponent) {
        List<Long> flattenedOrder = flatten(executionOrderByWeakComponent);
        Map<Long, Integer> formulaIdToPositionInFlattenedOrder = new HashMap<>();
        for (int position = 0; position < flattenedOrder.size(); position++) {
            formulaIdToPositionInFlattenedOrder.put(flattenedOrder.get(position), position);
        }
        for (Map.Entry<Long, List<Long>> formulaEntry : formulaIdToPrerequisiteFormulaIds.entrySet()) {
            Long dependentFormulaId = formulaEntry.getKey();
            if (dependentFormulaId == null) {
                continue;
            }
            List<Long> prerequisiteFormulaIds = formulaEntry.getValue();
            if (prerequisiteFormulaIds == null) {
                continue;
            }
            int dependentPosition =
                    formulaIdToPositionInFlattenedOrder.get(dependentFormulaId);
            for (Long prerequisiteFormulaId : prerequisiteFormulaIds) {
                if (prerequisiteFormulaId == null) {
                    continue;
                }
                assertTrue(
                        formulaIdToPositionInFlattenedOrder.get(prerequisiteFormulaId)
                                < dependentPosition,
                        () ->
                                "依赖 "
                                        + prerequisiteFormulaId
                                        + " 须在公式 "
                                        + dependentFormulaId
                                        + " 之前，顺序="
                                        + flattenedOrder);
            }
        }
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

    /**
     * 实际编排示例：一张「家计」表里多段公式——收入/支出汇总与结余相连为一大子图，另一段独立公式链为另一子图。
     */
    @Test
    void sort_demo_familyBudgetStyleFormulas_printsIo() {
        Map<Long, List<Long>> formulaIdToPrerequisiteFormulaIds = new HashMap<>();
        formulaIdToPrerequisiteFormulaIds.put(1001L, List.of()); // 工资
        formulaIdToPrerequisiteFormulaIds.put(1002L, List.of()); // 奖金
        formulaIdToPrerequisiteFormulaIds.put(1100L, List.of(1001L, 1002L)); // 收入小计
        formulaIdToPrerequisiteFormulaIds.put(2001L, List.of()); // 房租
        formulaIdToPrerequisiteFormulaIds.put(2002L, List.of()); // 餐饮
        formulaIdToPrerequisiteFormulaIds.put(2100L, List.of(2001L, 2002L)); // 支出小计
        formulaIdToPrerequisiteFormulaIds.put(3000L, List.of(1100L, 2100L)); // 结余
        formulaIdToPrerequisiteFormulaIds.put(5001L, List.of()); // 另一模块：基础值
        formulaIdToPrerequisiteFormulaIds.put(5002L, List.of(5001L)); // 依赖基础值

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
}
