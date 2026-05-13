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
     * <p>实现步骤与 {@linkplain FormulaTopologicalSort 类注释} 中「算法概要」一致：收集顶点全集 → 无向并查集合并
     * 「当前公式—前置」边以划分弱连通分量 → 按各分量最小公式 ID 对分量排序 → 在每个分量上有向建边并做 Kahn。
     *
     * @param formulaIdMap 公式 ID → 该公式直接依赖的公式 ID 列表；{@code null} 或空则返回 {@link List#of()}
     * @return 外层 List 每个元素对应一个弱连通分量；内层 List 为该分量上的「叶子 → 根」拓扑序
     * @throws IllegalStateException 任一弱连通分量上存在有向环（含自环）时
     */
    public static List<List<Long>> sort(Map<Long, List<Long>> formulaIdMap) {
        if (formulaIdMap == null || formulaIdMap.isEmpty()) {
            return List.of();
        }
        HashSet<Long> allFormulaIds = collectVertices(formulaIdMap);
        WeaklyConnectedComponents unionFind = unionFindFromMap(formulaIdMap, allFormulaIds);
        ComponentBuckets buckets = bucketFormulasByRepresentative(allFormulaIds, unionFind);
        List<Long> rootsOrdered = orderRootsBySmallestFormulaId(buckets);
        return sortEachWeakComponent(formulaIdMap, buckets, rootsOrdered);
    }

    /**
     * 与 {@link #sort(Map)} 相同逻辑；若存在环则返回 {@code null} 而不抛异常。
     *
     * <p>仅吞掉 {@link IllegalStateException}；其它运行时异常仍会向上抛出。
     *
     * @param formulaIdMap 同 {@link #sort(Map)}
     * @return 与 {@link #sort(Map)} 相同结构；无法拓扑排序（含环）时为 {@code null}
     */
    public static List<List<Long>> trySort(Map<Long, List<Long>> formulaIdMap) {
        try {
            return sort(formulaIdMap);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    /**
     * 从 map 的 key 与 value 中收集所有非 null 的公式 ID，得到顶点全集（含仅出现在依赖列表中的 ID）。
     */
    private static HashSet<Long> collectVertices(Map<Long, List<Long>> formulaIdMap) {
        // 顶点数通常大于 entry 数；2*size 为经验预分配，减少 HashSet 扩容
        int estimatedNodeCount = Math.max(16, formulaIdMap.size() * 2);
        HashSet<Long> allFormulaIds = HashSet.newHashSet(estimatedNodeCount);
        for (Map.Entry<Long, List<Long>> formulaEntry : formulaIdMap.entrySet()) {
            addKeyIfPresent(allFormulaIds, formulaEntry.getKey());
            addNonNullPrerequisites(allFormulaIds, formulaEntry.getValue());
        }
        return allFormulaIds;
    }

    /** 将非 null 的公式 key 加入顶点集。 */
    private static void addKeyIfPresent(HashSet<Long> allFormulaIds, Long formulaId) {
        if (formulaId != null) {
            allFormulaIds.add(formulaId);
        }
    }

    /** 将依赖列表中的非 null 公式 ID 加入顶点集；{@code null} 列表视为无依赖项可收集。 */
    private static void addNonNullPrerequisites(HashSet<Long> allFormulaIds, List<Long> prerequisiteFormulaIds) {
        if (prerequisiteFormulaIds == null) {
            return;
        }
        for (Long prerequisiteId : prerequisiteFormulaIds) {
            if (prerequisiteId != null) {
                allFormulaIds.add(prerequisiteId);
            }
        }
    }

    /**
     * 为每个已知顶点建单元素集，再按 map 中「当前公式—各前置」在无向意义上 {@code union}，用于弱连通分量划分。
     */
    private static WeaklyConnectedComponents unionFindFromMap(
            Map<Long, List<Long>> formulaIdMap, HashSet<Long> allFormulaIds) {
        WeaklyConnectedComponents unionFind = new WeaklyConnectedComponents(allFormulaIds.size());
        for (Long formulaId : allFormulaIds) {
            unionFind.findRepresentative(formulaId);
        }
        for (Map.Entry<Long, List<Long>> formulaEntry : formulaIdMap.entrySet()) {
            unionDependentWithPrerequisites(unionFind, formulaEntry.getKey(), formulaEntry.getValue());
        }
        return unionFind;
    }

    /**
     * 对单条 map 条目：若 key 非 null 且依赖列表非空，则将当前公式与各非 null 前置在无向并查集中合并。
     */
    private static void unionDependentWithPrerequisites(
            WeaklyConnectedComponents unionFind, Long dependentFormulaId, List<Long> prerequisiteFormulaIds) {
        if (dependentFormulaId == null) {
            return;
        }
        if (prerequisiteFormulaIds == null || prerequisiteFormulaIds.isEmpty()) {
            return;
        }
        long dependent = dependentFormulaId.longValue();
        for (Long prerequisiteFormulaId : prerequisiteFormulaIds) {
            if (prerequisiteFormulaId == null) {
                continue;
            }
            unionFind.union(dependent, prerequisiteFormulaId.longValue());
        }
    }

    /**
     * 按并查集代表元将顶点分桶，并维护每个桶内最小公式 ID（用于外层分量稳定排序）。
     */
    private static ComponentBuckets bucketFormulasByRepresentative(
            HashSet<Long> allFormulaIds, WeaklyConnectedComponents unionFind) {
        HashMap<Long, ArrayList<Long>> formulaIdsByComponentRoot =
                HashMap.newHashMap(allFormulaIds.size());
        HashMap<Long, Long> smallestFormulaIdInComponent = HashMap.newHashMap(allFormulaIds.size());
        for (Long formulaId : allFormulaIds) {
            long representative = unionFind.findRepresentative(formulaId);
            formulaIdsByComponentRoot
                    .computeIfAbsent(representative, k -> new ArrayList<>(8))
                    .add(formulaId);
            smallestFormulaIdInComponent.merge(representative, formulaId, Math::min);
        }
        return new ComponentBuckets(formulaIdsByComponentRoot, smallestFormulaIdInComponent);
    }

    /**
     * 将各弱连通分量的代表元排序；比较键为该分量中的最小公式 ID，而非并查集内部根 ID。
     */
    private static List<Long> orderRootsBySmallestFormulaId(ComponentBuckets buckets) {
        ArrayList<Long> roots = new ArrayList<>(buckets.formulaIdsByComponentRoot.keySet());
        roots.sort(Comparator.comparingLong(buckets.smallestFormulaIdInComponent::get));
        return roots;
    }

    /**
     * 按已排序的分量顺序，依次对每桶顶点调用分量内 Kahn，拼成外层 {@code List<List<Long>>}。
     */
    private static List<List<Long>> sortEachWeakComponent(
            Map<Long, List<Long>> formulaIdMap,
            ComponentBuckets buckets,
            List<Long> componentRootsOrderedByMinFormulaId) {
        ArrayList<List<Long>> executionOrderPerWeakComponent =
                new ArrayList<>(componentRootsOrderedByMinFormulaId.size());
        for (Long componentRoot : componentRootsOrderedByMinFormulaId) {
            ArrayList<Long> idsInComponent = buckets.formulaIdsByComponentRoot.get(componentRoot);
            executionOrderPerWeakComponent.add(
                    topologicalSortSingleWeakComponent(formulaIdMap, idsInComponent));
        }
        return executionOrderPerWeakComponent;
    }

    /**
     * 在单个弱连通分量上：稠密编号 → 只建两端均在本分量内的有向边（前置→当前）→ Kahn 拓扑序。
     *
     * @param formulaIdMap 全图依赖；仅读取属于本分量的条目建边
     * @param formulaIdsInWeakComponent 本分量的顶点列表
     * @return 该分量上的「叶子→根」拓扑序
     * @throws IllegalStateException 本分量子图存在有向环时
     */
    private static List<Long> topologicalSortSingleWeakComponent(
            Map<Long, List<Long>> formulaIdMap, ArrayList<Long> formulaIdsInWeakComponent) {
        int vertexCount = formulaIdsInWeakComponent.size();
        if (vertexCount == 0) {
            return List.of();
        }
        DenseVertexGraph graph = DenseVertexGraph.fromComponent(formulaIdsInWeakComponent);
        graph.addIntraComponentEdges(formulaIdMap);
        return graph.runKahnOrThrow();
    }

    /**
     * 弱连通分量分桶结果：按并查集代表元索引顶点列表，并记录每桶最小公式 ID。
     *
     * @param formulaIdsByComponentRoot key 为代表元，value 为该分量全部公式 ID
     * @param smallestFormulaIdInComponent 与上式 key 对齐，存该分量最小公式 ID
     */
    private record ComponentBuckets(
            HashMap<Long, ArrayList<Long>> formulaIdsByComponentRoot,
            HashMap<Long, Long> smallestFormulaIdInComponent) {
    }

    /**
     * 单个弱连通分量上的有向图 + Kahn 状态：将稀疏 {@link Long} 公式 ID 映射为 {@code 0..n-1} 下标，用热路径友好的
     * {@code int[]} 与定长队列实现拓扑排序。
     */
    private static final class DenseVertexGraph {
        private final int vertexCount;
        /** 公式 ID → 稠密顶点下标。 */
        private final HashMap<Long, Integer> formulaIdToVertexIndex;
        /** 稠密下标 → 公式 ID，出队时写回结果序列。 */
        private final long[] vertexIndexToFormulaId;
        /**
         * 邻接表：下标 {@code i} 为「前置公式」顶点，列表内存「直接依赖该前置」的顶点下标（有向边 前置→当前）。
         */
        private final ArrayList<Integer>[] adjacencyOutgoingDependentVertices;
        /** 各顶点（当前公式）尚未被排队消解的直接前置个数；Kahn 中减至 0 即可入队。 */
        private final int[] inDegreeByVertexIndex;

        /**
         * @param vertexCount 分量顶点数 n
         * @param formulaIdToVertexIndex 公式 ID → 0..n-1
         * @param vertexIndexToFormulaId 反向映射，输出序时还原 Long ID
         */
        @SuppressWarnings("unchecked")
        private DenseVertexGraph(
                int vertexCount,
                HashMap<Long, Integer> formulaIdToVertexIndex,
                long[] vertexIndexToFormulaId) {
            this.vertexCount = vertexCount;
            this.formulaIdToVertexIndex = formulaIdToVertexIndex;
            this.vertexIndexToFormulaId = vertexIndexToFormulaId;
            this.adjacencyOutgoingDependentVertices = new ArrayList[vertexCount];
            this.inDegreeByVertexIndex = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
                adjacencyOutgoingDependentVertices[i] = new ArrayList<>(4);
            }
        }

        /**
         * 根据分量顶点列表建立 ID↔下标双向映射及空邻接表。
         */
        static DenseVertexGraph fromComponent(ArrayList<Long> formulaIdsInWeakComponent) {
            int n = formulaIdsInWeakComponent.size();
            HashMap<Long, Integer> idToIndex = HashMap.newHashMap(n);
            long[] indexToId = new long[n];
            for (int i = 0; i < n; i++) {
                long formulaId = formulaIdsInWeakComponent.get(i);
                indexToId[i] = formulaId;
                idToIndex.put(formulaId, i);
            }
            return new DenseVertexGraph(n, idToIndex, indexToId);
        }

        /** 遍历全图 map，仅当「当前公式」与「前置」均映射到本分量下标时，添加有向边并更新入度。 */
        void addIntraComponentEdges(Map<Long, List<Long>> formulaIdMap) {
            for (Map.Entry<Long, List<Long>> entry : formulaIdMap.entrySet()) {
                addEdgesForEntry(entry.getKey(), entry.getValue());
            }
        }

        /** 处理 map 单条 entry：跳过 null key、非本分量顶点、空依赖。 */
        private void addEdgesForEntry(Long dependentFormulaId, List<Long> prerequisiteFormulaIds) {
            if (dependentFormulaId == null) {
                return;
            }
            Integer dependentVertexIndex = formulaIdToVertexIndex.get(dependentFormulaId);
            if (dependentVertexIndex == null) {
                return;
            }
            if (prerequisiteFormulaIds == null || prerequisiteFormulaIds.isEmpty()) {
                return;
            }
            for (Long prerequisiteFormulaId : prerequisiteFormulaIds) {
                addEdgeFromPrerequisiteToDependent(prerequisiteFormulaId, dependentVertexIndex);
            }
        }

        /**
         * 添加边「前置 → 当前」：前置、当前必须均在本分量内；跨分量或 null 前置不建边（与主类文档约定一致）。
         */
        private void addEdgeFromPrerequisiteToDependent(Long prerequisiteFormulaId, int dependentVertexIndex) {
            if (prerequisiteFormulaId == null) {
                return;
            }
            Integer prerequisiteVertexIndex = formulaIdToVertexIndex.get(prerequisiteFormulaId);
            if (prerequisiteVertexIndex == null) {
                return;
            }
            adjacencyOutgoingDependentVertices[prerequisiteVertexIndex].add(dependentVertexIndex);
            inDegreeByVertexIndex[dependentVertexIndex]++;
        }

        /**
         * Kahn 算法：入度为 0 先入队，反复出队并松弛出边；若最终访问顶点数不足则判环并抛异常。
         */
        List<Long> runKahnOrThrow() {
            int[] queue = new int[vertexCount];
            int tail = enqueueZeroInDegreeVertices(queue);
            ArrayList<Long> order = drainQueue(queue, tail);
            if (order.size() != vertexCount) {
                throw new IllegalStateException(
                        "公式依赖图中存在环，无法确定执行顺序；请检查 formulaIdMap。");
            }
            return order;
        }

        /** 将所有入度为 0 的顶点下标写入环形缓冲区队尾，返回初始队尾指针。 */
        private int enqueueZeroInDegreeVertices(int[] readyVertexIndexRingBuffer) {
            int queueTail = 0;
            for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
                if (inDegreeByVertexIndex[vertexIndex] == 0) {
                    readyVertexIndexRingBuffer[queueTail++] = vertexIndex;
                }
            }
            return queueTail;
        }

        /** 反复出队、追加公式 ID 到结果、对出边做入度减一并推送新入度为 0 的顶点。 */
        private ArrayList<Long> drainQueue(int[] readyVertexIndexRingBuffer, int initialTail) {
            ArrayList<Long> executionOrderLeafToRoot = new ArrayList<>(vertexCount);
            int queueHead = 0;
            int queueTail = initialTail;
            while (queueHead < queueTail) {
                int u = readyVertexIndexRingBuffer[queueHead++];
                executionOrderLeafToRoot.add(vertexIndexToFormulaId[u]);
                queueTail = relaxOutgoing(u, readyVertexIndexRingBuffer, queueTail);
            }
            return executionOrderLeafToRoot;
        }

        /** 对刚出队顶点松弛所有出边，返回更新后的队尾指针。 */
        private int relaxOutgoing(int readyVertexIndex, int[] ringBuffer, int queueTail) {
            ArrayList<Integer> outs = adjacencyOutgoingDependentVertices[readyVertexIndex];
            int outgoingCount = outs.size();
            for (int edgeIndex = 0; edgeIndex < outgoingCount; edgeIndex++) {
                int v = outs.get(edgeIndex);
                if (--inDegreeByVertexIndex[v] == 0) {
                    ringBuffer[queueTail++] = v;
                }
            }
            return queueTail;
        }
    }

    /**
     * 无向并查集：用于「当前公式—前置」无向连边后的弱连通分量划分；按集合 size 合并并路径压缩。
     */
    private static final class WeaklyConnectedComponents {
        /** 父指针：值为自身时表示当前结点为代表元。 */
        private final HashMap<Long, Long> parentByFormulaId;
        /** 仅根结点维护集合元素个数，供 union 时比大小。 */
        private final HashMap<Long, Integer> componentSizeByRepresentative;

        /**
         * @param expectedDistinctFormulaIds 预估不同公式 ID 数量，用于 {@link HashMap} 初始容量
         */
        WeaklyConnectedComponents(int expectedDistinctFormulaIds) {
            this.parentByFormulaId = HashMap.newHashMap(expectedDistinctFormulaIds);
            this.componentSizeByRepresentative = HashMap.newHashMap(expectedDistinctFormulaIds);
        }

        /**
         * 查找 {@code formulaId} 所在集合代表元；首次访问时创建单元素集合。
         */
        long findRepresentative(long formulaId) {
            Long parentOrSelf = parentByFormulaId.get(formulaId);
            if (parentOrSelf == null) {
                return makeSingleton(formulaId);
            }
            return compressIfNeeded(formulaId, parentOrSelf);
        }

        /** 初始化单元素集合：父指向自己，size 为 1。 */
        private long makeSingleton(long formulaId) {
            parentByFormulaId.put(formulaId, formulaId);
            componentSizeByRepresentative.put(formulaId, 1);
            return formulaId;
        }

        /** 若非根则递归找根并把当前结点直接挂到根（路径压缩）。 */
        private long compressIfNeeded(long formulaId, Long parentOrSelf) {
            long parentFormulaId = parentOrSelf;
            if (parentFormulaId == formulaId) {
                return formulaId;
            }
            long rootRepresentative = findRepresentative(parentFormulaId);
            parentByFormulaId.put(formulaId, rootRepresentative);
            return rootRepresentative;
        }

        /** 合并两公式所在集合；已在同一集合则为空操作。 */
        void union(long formulaIdA, long formulaIdB) {
            long rootA = findRepresentative(formulaIdA);
            long rootB = findRepresentative(formulaIdB);
            if (rootA == rootB) {
                return;
            }
            int sizeA = componentSizeByRepresentative.getOrDefault(rootA, 1);
            int sizeB = componentSizeByRepresentative.getOrDefault(rootB, 1);
            mergeBySize(rootA, rootB, sizeA, sizeB);
        }

        /** 小树挂大树，保持合并后高度均摊更小。 */
        private void mergeBySize(long rootA, long rootB, int sizeA, int sizeB) {
            if (sizeA < sizeB) {
                attach(rootA, rootB, sizeA + sizeB);
            } else {
                attach(rootB, rootA, sizeA + sizeB);
            }
        }

        /** 将 {@code childRoot} 挂到 {@code parentRoot}，更新新根 size 并删除旧根 size 记录。 */
        private void attach(long childRoot, long parentRoot, int mergedSize) {
            parentByFormulaId.put(childRoot, parentRoot);
            componentSizeByRepresentative.put(parentRoot, mergedSize);
            componentSizeByRepresentative.remove(childRoot);
        }
    }
}
