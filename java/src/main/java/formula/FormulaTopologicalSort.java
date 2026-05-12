package formula;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 公式依赖图的拓扑排序工具：给定「每个公式依赖哪些前置公式」，产出<strong>可执行顺序</strong>。
 *
 * <h2>业务语义（与 Excel / 表格引擎一致）</h2>
 *
 * <ul>
 *   <li>{@code formulaIdMap} 的 <strong>key</strong>：某个待计算的公式（或单元格）ID。
 *   <li><strong>value</strong>（map 的 value）：该公式<strong>直接依赖</strong>的其它公式 ID 列表；这些前置必须先于 key 计算完成。
 *   <li>有向边约定：<strong>前置 → 当前公式</strong>（先执行前置，再执行依赖它的公式）。
 * </ul>
 *
 * <h2>返回结构 {@code List<List<Long>>}</h2>
 *
 * <ul>
 *   <li><strong>外层 List</strong>：每个元素对应依赖图的一个<strong>弱连通分量</strong>（在无向意义下，通过「公式—其依赖」边能互相到达的一批节点）。
 *       不同分量之间没有依赖边，可并行计算或任意先后执行；本实现将分量拆开返回，便于按子图调度。
 *   <li><strong>内层 List</strong>：该分量上的<strong>拓扑序</strong>，顺序为「叶子 → 根」：越靠前越先执行（无未满足前置的公式在前）。
 *   <li>外层各分量的先后：按「该分量中<strong>最小公式 ID</strong>」升序排列，保证输出稳定、便于对账与日志。
 * </ul>
 *
 * <h2>算法概要（分四步）</h2>
 *
 * <ol>
 *   <li><strong>收集顶点</strong>：遍历 map 的 key 与 value 中出现的所有非 null Long，得到顶点全集（含只出现在依赖里、未作为 key 的公式）。
 *   <li><strong>划分弱连通分量</strong>：对每条「当前公式—其某一前置」在无向图上连边，用并查集合并；同一集合即同一弱连通分量。
 *   <li><strong>分量排序</strong>：计算每个分量内的最小公式 ID，按该最小值对分量排序。
 *   <li><strong>分量内 Kahn 拓扑排序</strong>：在每个分量上建「前置 → 当前」有向边，入度为 0 先入队；出队时缩减后继入度，直至排完或发现环。
 * </ol>
 *
 * <h2>异常与边界</h2>
 *
 * <ul>
 *   <li>{@code formulaIdMap == null} 或空 map：返回不可变的空列表 {@link List#of()}。
 *   <li>map 中存在 <strong>有向环</strong>（含自环 {@code A 依赖 A}）：{@link #sort(Map)} 抛 {@link IllegalStateException}；{@link #trySort(Map)} 返回 {@code null}。
 *   <li>{@code null} 的 key：跳过，不参与边；但其 value 里出现的公式 ID 仍会进入顶点集（可能成为孤立点）。
 *   <li>{@code null} 或空的依赖列表：视为无直接依赖（叶子）。
 *   <li>依赖列表中的 {@code null} 元素：忽略。
 *   <li>若某条依赖指向<strong>另一弱连通分量</strong>的公式：在<strong>当前分量</strong>的子图上不建该边（无法在同一执行链中满足）；调用方应保证依赖与公式同属一连通区域，否则排序结果可能不符合业务预期。
 * </ul>
 *
 * <h2>实现与性能（JDK 21）</h2>
 *
 * <ul>
 *   <li>并查集：<strong>按集合 size 合并</strong> + <strong>路径压缩</strong>，均摊接近 O(α(n))。
 *   <li>分量内：公式 ID 映射为稠密下标 {@code 0..n-1}，用 {@code int[]} 入度与定长环形队列做 Kahn，减少 {@code Integer} 装箱与 TreeMap。
 *   <li>{@link HashMap#newHashMap(int)} / {@link HashSet#newHashSet(int)} 按预估规模预分配，降低扩容次数。
 * </ul>
 *
 * <p>时间复杂度：设 V 为顶点数、E 为 map 中「非空依赖」展开后的有向边条数，整体约 O(V + E + V·α(V))；分量内拓扑为 O(V + E)。
 */
public final class FormulaTopologicalSort {

    private FormulaTopologicalSort() {
    }

    /**
     * 对整张公式依赖图做拓扑排序，并按弱连通分量拆分结果。
     *
     * <p>执行流程与 {@linkplain FormulaTopologicalSort 类注释} 中「算法概要」一致：先收集顶点 → 并查集划分子图
     * → 按分量最小 ID 排序分量 → 对每个分量单独 Kahn。
     *
     * @param formulaIdMap 公式 ID → 该公式直接依赖的公式 ID 列表；{@code null} 或空则返回空列表
     * @return 不可变或新构建的列表；外层每个内层列表是一条弱连通分量上的「叶子 → 根」执行顺序
     * @throws IllegalStateException 任一弱连通分量上存在有向环（含自环），无法拓扑排序时
     */
    public static List<List<Long>> sort(Map<Long, List<Long>> formulaIdMap) {
        // ---------- 边界：无输入则无顶点 ----------
        if (formulaIdMap == null || formulaIdMap.isEmpty()) {
            return List.of();
        }

        // ---------- 步骤 1：收集顶点全集 ----------
        // 原因：依赖里可能引用未在 key 集合中出现的公式（仅作为被引用方），也必须参与排序与分量划分。
        // 预估容量取 max(16, 2*entry数)：一条公式常见「若干依赖」，顶点数通常大于 entry 数但不会无限大。
        int estimatedNodeCount = Math.max(16, formulaIdMap.size() * 2);
        HashSet<Long> allFormulaIds = HashSet.newHashSet(estimatedNodeCount);
        for (Map.Entry<Long, List<Long>> formulaEntry : formulaIdMap.entrySet()) {
            Long formulaId = formulaEntry.getKey();
            if (formulaId != null) {
                allFormulaIds.add(formulaId);
            }
            List<Long> prerequisiteFormulaIds = formulaEntry.getValue();
            if (prerequisiteFormulaIds != null) {
                for (Long prerequisiteId : prerequisiteFormulaIds) {
                    if (prerequisiteId != null) {
                        allFormulaIds.add(prerequisiteId);
                    }
                }
            }
        }

        // ---------- 步骤 2：无向并查集，得到弱连通分量 ----------
        // 对每条「当前公式 — 其某一前置」在无向意义上连边：二者必属同一弱连通分量（可互相通过无向路径到达）。
        // 注意：这里用的是无向合并；有向边（谁先算）留在步骤 4 的 Kahn 里处理。
        WeaklyConnectedComponents unionFind = new WeaklyConnectedComponents(allFormulaIds.size());
        // 确保仅出现在「别人依赖列表」里的孤立 ID 也有单元素集合，后续分组不会丢顶点。
        for (Long formulaId : allFormulaIds) {
            unionFind.findRepresentative(formulaId);
        }
        for (Map.Entry<Long, List<Long>> formulaEntry : formulaIdMap.entrySet()) {
            Long dependentFormulaId = formulaEntry.getKey();
            if (dependentFormulaId == null) {
                // key 为 null 的条目不参与构图（无法作为「当前公式」标识）
                continue;
            }
            List<Long> prerequisiteFormulaIds = formulaEntry.getValue();
            if (prerequisiteFormulaIds == null || prerequisiteFormulaIds.isEmpty()) {
                continue;
            }
            for (Long prerequisiteFormulaId : prerequisiteFormulaIds) {
                if (prerequisiteFormulaId == null) {
                    continue;
                }
                unionFind.union(
                        dependentFormulaId.longValue(), prerequisiteFormulaId.longValue());
            }
        }

        // ---------- 步骤 3：按代表元把公式分桶，并记录「桶内最小公式 ID」用于稳定排序 ----------
        // formulaIdsByComponentRoot：key = 并查集当前代表元（任意 ID），value = 该分量中所有公式 ID。
        // smallestFormulaIdInComponent：与上面 key 对齐，存该分量里最小的公式 ID（用于外层 List 的顺序，与「按最小 ID 代表分量」语义一致）。
        HashMap<Long, ArrayList<Long>> formulaIdsByComponentRoot =
                HashMap.newHashMap(allFormulaIds.size());
        HashMap<Long, Long> smallestFormulaIdInComponent = HashMap.newHashMap(allFormulaIds.size());
        for (Long formulaId : allFormulaIds) {
            long componentRepresentative = unionFind.findRepresentative(formulaId);
            formulaIdsByComponentRoot
                    .computeIfAbsent(componentRepresentative, k -> new ArrayList<>(8))
                    .add(formulaId);
            smallestFormulaIdInComponent.merge(componentRepresentative, formulaId, Math::min);
        }

        // 将各分量的代表元排成一行：比较键是「该分量最小公式 ID」，而不是并查集内部根是谁（根会随 union 变化）。
        ArrayList<Long> componentRootsOrderedByMinFormulaId =
                new ArrayList<>(formulaIdsByComponentRoot.keySet());
        componentRootsOrderedByMinFormulaId.sort(
                Comparator.comparingLong(smallestFormulaIdInComponent::get));

        // ---------- 步骤 4：对每个弱连通分量单独做 Kahn，结果依次追加到外层 List ----------
        ArrayList<List<Long>> executionOrderPerWeakComponent =
                new ArrayList<>(componentRootsOrderedByMinFormulaId.size());
        for (Long componentRoot : componentRootsOrderedByMinFormulaId) {
            ArrayList<Long> formulaIdsInThisComponent =
                    formulaIdsByComponentRoot.get(componentRoot);
            executionOrderPerWeakComponent.add(
                    topologicalSortSingleWeakComponent(formulaIdMap, formulaIdsInThisComponent));
        }
        return executionOrderPerWeakComponent;
    }

    /**
     * 与 {@link #sort(Map)} 相同计算逻辑，但不向调用方抛出「存在环」异常。
     *
     * <p>适用场景：编排前希望先探测是否 DAG；返回 {@code null} 表示整张图（某一分量）上无法拓扑排序。
     * 注意：此处仅捕获 {@link IllegalStateException}，其它运行时异常仍会向上抛出。
     *
     * @param formulaIdMap 同 {@link #sort(Map)}
     * @return 与 {@link #sort(Map)} 相同结构的结果；若存在环则 {@code null}
     */
    public static List<List<Long>> trySort(Map<Long, List<Long>> formulaIdMap) {
        try {
            return sort(formulaIdMap);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    /**
     * 在<strong>单个弱连通分量</strong>上执行 Kahn 算法，得到一条合法的线性拓扑序。
     *
     * <p><strong>图模型</strong>
     *
     * <ul>
     *   <li>顶点：{@code formulaIdsInWeakComponent} 中的每个公式 ID。
     *   <li>有向边：对每个 map 条目「当前公式 f → 依赖列表中的 p」，若 f 与 p 均属于本分量，则加边 <strong>p → f</strong>（先算 p，再算 f）。
     *   <li>入度：指向顶点 f 的边数 = f 还有多少<strong>尚未排在前面的</strong>直接前置；初始入度由边表统计得到。
     * </ul>
     *
     * <p><strong>Kahn 过程</strong>
     *
     * <ol>
     *   <li>将所有入度为 0 的顶点下标放入队列（这些公式没有未满足的直接前置，可立即执行）。
     *   <li>反复取出队首顶点 u，将 u 对应的公式 ID 追加到结果序列末尾。
     *   <li>对 u 的每条出边 u→v（v 依赖 u 已算完），将 v 的入度减 1；若 v 入度变为 0，将 v 入队。
     *   <li>若最终结果长度 &lt; 顶点数，说明残留子图入度均非 0，即存在有向环。
     * </ol>
     *
     * <p><strong>为何做「公式 ID → 下标」映射</strong>：公式 ID 为稀疏 Long，用稠密下标可把入度、队列存成 {@code int[]}，
     * 避免大量 {@code Integer} 与频繁哈希在热路径上带来的开销。
     *
     * @param formulaIdMap 全图依赖；本方法只读取「当前公式属于本分量」的条目，并只建两端都在本分量内的边
     * @param formulaIdsInWeakComponent 本分量的顶点集合（来自并查集分桶，顺序任意，不影响拓扑合法性）
     * @return 本分量的一个拓扑序（叶子在前，依赖链末端在后）
     * @throws IllegalStateException 本分量子图上存在有向环时
     */
    private static List<Long> topologicalSortSingleWeakComponent(
            Map<Long, List<Long>> formulaIdMap, ArrayList<Long> formulaIdsInWeakComponent) {
        final int vertexCount = formulaIdsInWeakComponent.size();
        if (vertexCount == 0) {
            return List.of();
        }

        // --- 稠密编号：Long 公式 ID ↔ 0..vertexCount-1，便于后续用 int[] 维护入度与队列 ---
        HashMap<Long, Integer> formulaIdToVertexIndex = HashMap.newHashMap(vertexCount);
        long[] vertexIndexToFormulaId = new long[vertexCount];
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            long formulaId = formulaIdsInWeakComponent.get(vertexIndex);
            vertexIndexToFormulaId[vertexIndex] = formulaId;
            formulaIdToVertexIndex.put(formulaId, vertexIndex);
        }

        // --- 建图：邻接表含义为「前置顶点 → 依赖它的当前公式顶点」---
        // adjacencyOutgoingDependentVertices[i] = 所有「直接依赖公式 i」的顶点下标列表。
        @SuppressWarnings("unchecked")
        ArrayList<Integer>[] adjacencyOutgoingDependentVertices = new ArrayList[vertexCount];
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            adjacencyOutgoingDependentVertices[vertexIndex] = new ArrayList<>(4);
        }
        // inDegreeByVertexIndex[v] = 顶点 v（当前公式）尚未被「排队前面」消掉的前置个数；减到 0 表示可执行 v。
        int[] inDegreeByVertexIndex = new int[vertexCount];

        for (Map.Entry<Long, List<Long>> formulaEntry : formulaIdMap.entrySet()) {
            Long dependentFormulaId = formulaEntry.getKey();
            if (dependentFormulaId == null) {
                continue;
            }
            Integer dependentVertexIndex = formulaIdToVertexIndex.get(dependentFormulaId);
            if (dependentVertexIndex == null) {
                // 该 key 不是本分量的顶点，跳过
                continue;
            }
            List<Long> prerequisiteFormulaIds = formulaEntry.getValue();
            if (prerequisiteFormulaIds == null || prerequisiteFormulaIds.isEmpty()) {
                continue;
            }
            for (Long prerequisiteFormulaId : prerequisiteFormulaIds) {
                if (prerequisiteFormulaId == null) {
                    continue;
                }
                Integer prerequisiteVertexIndex =
                        formulaIdToVertexIndex.get(prerequisiteFormulaId);
                if (prerequisiteVertexIndex == null) {
                    // 前置不在本分量子图内（例如跨弱连通分量的错误引用）：本方法不建边，相当于忽略该依赖
                    continue;
                }
                adjacencyOutgoingDependentVertices[prerequisiteVertexIndex].add(dependentVertexIndex);
                inDegreeByVertexIndex[dependentVertexIndex]++;
            }
        }

        // --- 初始化 Kahn 队列：所有入度为 0 的顶点一次性入队（可并行执行的「当前层」）---
        // 使用定长 int[] + 头尾指针模拟队列，避免 ArrayDeque<Integer> 装箱。
        int[] readyVertexIndexRingBuffer = new int[vertexCount];
        int queueHead = 0;
        int queueTail = 0;
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            if (inDegreeByVertexIndex[vertexIndex] == 0) {
                readyVertexIndexRingBuffer[queueTail++] = vertexIndex;
            }
        }

        ArrayList<Long> executionOrderLeafToRoot = new ArrayList<>(vertexCount);
        while (queueHead < queueTail) {
            int readyFormulaVertexIndex = readyVertexIndexRingBuffer[queueHead++];
            executionOrderLeafToRoot.add(vertexIndexToFormulaId[readyFormulaVertexIndex]);
            ArrayList<Integer> dependentVertexIndices =
                    adjacencyOutgoingDependentVertices[readyFormulaVertexIndex];
            // 当前公式已执行完：所有「直接依赖它」的公式入度减 1
            for (int edgeIndex = 0, outgoingCount = dependentVertexIndices.size();
                    edgeIndex < outgoingCount;
                    edgeIndex++) {
                int dependentVertexIndex = dependentVertexIndices.get(edgeIndex);
                if (--inDegreeByVertexIndex[dependentVertexIndex] == 0) {
                    readyVertexIndexRingBuffer[queueTail++] = dependentVertexIndex;
                }
            }
        }

        // 正常 DAG：恰好访问 vertexCount 个顶点。若少于 vertexCount，说明环上顶点入度永不为 0，队列提前耗尽。
        if (executionOrderLeafToRoot.size() != vertexCount) {
            throw new IllegalStateException(
                    "公式依赖图中存在环，无法确定执行顺序；请检查 formulaIdMap。");
        }
        return executionOrderLeafToRoot;
    }

    /**
     * 无向并查集（Disjoint Set Union），用于<strong>弱连通分量</strong>划分。
     *
     * <p>与「强连通」区别：这里把「公式 A 依赖 B」看成无向边 A—B；同一无向连通块内的公式可能并不互相可达（有向），
     * 但属于同一张无向子图，本工具将它们在<strong>外层 List</strong>里拆成一组单独排序。
     *
     * <p><strong>内部策略</strong>
     *
     * <ul>
     *   <li><strong>路径压缩</strong>：find 时把途经节点直接挂到根，缩短以后 find 链长。
     *   <li><strong>按 size 合并</strong>：把小根树挂到大根树下，使树高均摊更小；根 ID 不一定是集合中最小公式 ID。
     *   <li>外层排序不依赖「根是谁」，而是用 {@code smallestFormulaIdInComponent} 按业务最小 ID 排分量顺序。
     * </ul>
     */
    private static final class WeaklyConnectedComponents {
        /** 父指针：parentByFormulaId[x]=x 表示 x 是当前集合代表元 */
        private final HashMap<Long, Long> parentByFormulaId;
        /**
         * 仅当某节点当前是集合代表元时，记录该集合元素个数；用于 union 时比大小。
         * 非根节点上的 size 条目会在 union 后被 remove，避免歧义。
         */
        private final HashMap<Long, Integer> componentSizeByRepresentative;

        WeaklyConnectedComponents(int expectedDistinctFormulaIds) {
            this.parentByFormulaId = HashMap.newHashMap(expectedDistinctFormulaIds);
            this.componentSizeByRepresentative = HashMap.newHashMap(expectedDistinctFormulaIds);
        }

        /**
         * 查找 {@code formulaId} 所在集合的代表元；若首次出现则创建单元素集合。
         *
         * @param formulaId 公式 ID
         * @return 该集合代表元（合并过程中可能变化，但同一集合内所有 ID 的 find 结果最终一致）
         */
        long findRepresentative(long formulaId) {
            Long parentOrSelf = parentByFormulaId.get(formulaId);
            if (parentOrSelf == null) {
                parentByFormulaId.put(formulaId, formulaId);
                componentSizeByRepresentative.put(formulaId, 1);
                return formulaId;
            }
            long parentFormulaId = parentOrSelf;
            if (parentFormulaId != formulaId) {
                // 递归找到根后，把当前节点直接指向根（路径压缩）
                long rootRepresentative = findRepresentative(parentFormulaId);
                parentByFormulaId.put(formulaId, rootRepresentative);
                return rootRepresentative;
            }
            return formulaId;
        }

        /**
         * 将包含 {@code formulaIdA} 与 {@code formulaIdB} 的两个集合合并（若已在同一集合则 noop）。
         *
         * @param formulaIdA 公式 ID
         * @param formulaIdB 公式 ID
         */
        void union(long formulaIdA, long formulaIdB) {
            long rootA = findRepresentative(formulaIdA);
            long rootB = findRepresentative(formulaIdB);
            if (rootA == rootB) {
                return;
            }
            int sizeA = componentSizeByRepresentative.getOrDefault(rootA, 1);
            int sizeB = componentSizeByRepresentative.getOrDefault(rootB, 1);
            // 小树挂大树：合并后只保留新根的 size，旧根的 size 删除，避免误用旧根统计
            if (sizeA < sizeB) {
                parentByFormulaId.put(rootA, rootB);
                componentSizeByRepresentative.put(rootB, sizeA + sizeB);
                componentSizeByRepresentative.remove(rootA);
            } else {
                parentByFormulaId.put(rootB, rootA);
                componentSizeByRepresentative.put(rootA, sizeA + sizeB);
                componentSizeByRepresentative.remove(rootB);
            }
        }
    }
}
