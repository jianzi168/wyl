package formula;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
/**
 * 根据公式依赖关系（类 Excel：当前公式依赖哪些前置公式）计算执行顺序。
 * <p>
 * 本实现在 JDK 21 下针对大图做了常见热点优化：并查集按 size 合并 + 路径压缩；对 {@code formulaIdMap}
 * 单次扫描建各分量边表；分量内拓扑排序使用整型下标与定长 {@code int[]} 队列，减少装箱与 TreeMap 开销。
 * <p>
 * {@code formulaIdMap} 语义：key = 公式节点 ID，value = 该公式直接依赖的公式 ID 列表（须先于 key 计算）。
 * 返回值按<strong>弱连通分量</strong>拆分；外层按各分量中<strong>最小节点 ID</strong>升序排列。
 */
public final class FormulaTopologicalSort {

    private FormulaTopologicalSort() {
    }

    /**
     * Kahn 算法拓扑排序；存在环时抛出 {@link IllegalStateException}。
     *
     * @param formulaIdMap 公式 ID → 依赖的公式 ID 列表（可为 null 或空列表表示无依赖）
     * @return 每个无交集的连通子图对应一个列表，列表内为叶子 → 根的执行顺序
     */
    public static List<List<Long>> sort(Map<Long, List<Long>> formulaIdMap) {
        if (formulaIdMap == null || formulaIdMap.isEmpty()) {
            return List.of();
        }

        int estNodes = Math.max(16, formulaIdMap.size() * 2);
        HashSet<Long> allIds = HashSet.newHashSet(estNodes);
        for (Map.Entry<Long, List<Long>> e : formulaIdMap.entrySet()) {
            Long node = e.getKey();
            if (node != null) {
                allIds.add(node);
            }
            List<Long> deps = e.getValue();
            if (deps != null) {
                for (Long d : deps) {
                    if (d != null) {
                        allIds.add(d);
                    }
                }
            }
        }

        UnionFind uf = new UnionFind(allIds.size());
        for (Long id : allIds) {
            uf.find(id);
        }
        for (Map.Entry<Long, List<Long>> e : formulaIdMap.entrySet()) {
            Long formulaId = e.getKey();
            if (formulaId == null) {
                continue;
            }
            List<Long> deps = e.getValue();
            if (deps == null || deps.isEmpty()) {
                continue;
            }
            for (Long dep : deps) {
                if (dep == null) {
                    continue;
                }
                uf.union(formulaId.longValue(), dep.longValue());
            }
        }

        HashMap<Long, ArrayList<Long>> membersByRoot = HashMap.newHashMap(allIds.size());
        HashMap<Long, Long> minIdByRoot = HashMap.newHashMap(allIds.size());
        for (Long id : allIds) {
            long root = uf.find(id);
            membersByRoot.computeIfAbsent(root, k -> new ArrayList<>(8)).add(id);
            minIdByRoot.merge(root, id, Math::min);
        }

        ArrayList<Long> roots = new ArrayList<>(membersByRoot.keySet());
        roots.sort(Comparator.comparingLong(minIdByRoot::get));

        ArrayList<List<Long>> result = new ArrayList<>(roots.size());
        for (Long root : roots) {
            ArrayList<Long> members = membersByRoot.get(root);
            result.add(topologicalSortComponent(formulaIdMap, members));
        }
        return result;
    }

    /**
     * 与 {@link #sort(Map)} 相同语义，但检测到环时返回 null 而不抛异常。
     */
    public static List<List<Long>> trySort(Map<Long, List<Long>> formulaIdMap) {
        try {
            return sort(formulaIdMap);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    /**
     * 对单个弱连通分量做 Kahn 拓扑排序；{@code members} 为该分量中所有节点（已去重收集）。
     */
    private static List<Long> topologicalSortComponent(
            Map<Long, List<Long>> formulaIdMap, ArrayList<Long> members) {
        final int n = members.size();
        if (n == 0) {
            return List.of();
        }

        HashMap<Long, Integer> index = HashMap.newHashMap(n);
        long[] ids = new long[n];
        for (int i = 0; i < n; i++) {
            long id = members.get(i);
            ids[i] = id;
            index.put(id, i);
        }

        @SuppressWarnings("unchecked")
        ArrayList<Integer>[] succ = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            succ[i] = new ArrayList<>(4);
        }
        int[] indeg = new int[n];

        for (Map.Entry<Long, List<Long>> e : formulaIdMap.entrySet()) {
            Long fObj = e.getKey();
            if (fObj == null) {
                continue;
            }
            Integer fi = index.get(fObj);
            if (fi == null) {
                continue;
            }
            List<Long> deps = e.getValue();
            if (deps == null || deps.isEmpty()) {
                continue;
            }
            for (Long depObj : deps) {
                if (depObj == null) {
                    continue;
                }
                Integer di = index.get(depObj);
                if (di == null) {
                    continue;
                }
                succ[di].add(fi);
                indeg[fi]++;
            }
        }

        int[] queue = new int[n];
        int qh = 0;
        int qt = 0;
        for (int i = 0; i < n; i++) {
            if (indeg[i] == 0) {
                queue[qt++] = i;
            }
        }

        ArrayList<Long> order = new ArrayList<>(n);
        while (qh < qt) {
            int u = queue[qh++];
            order.add(ids[u]);
            ArrayList<Integer> outs = succ[u];
            for (int j = 0, sz = outs.size(); j < sz; j++) {
                int v = outs.get(j);
                if (--indeg[v] == 0) {
                    queue[qt++] = v;
                }
            }
        }

        if (order.size() != n) {
            throw new IllegalStateException(
                    "公式依赖图中存在环，无法确定执行顺序；请检查 formulaIdMap。");
        }
        return order;
    }

    /** 并查集：按集合 size 合并 + 路径压缩；根不一定是 min ID，由外层用 minIdByRoot 排序。 */
    private static final class UnionFind {
        private final HashMap<Long, Long> parent;
        private final HashMap<Long, Integer> sz;

        UnionFind(int expected) {
            this.parent = HashMap.newHashMap(expected);
            this.sz = HashMap.newHashMap(expected);
        }

        long find(long x) {
            Long p = parent.get(x);
            if (p == null) {
                parent.put(x, x);
                sz.put(x, 1);
                return x;
            }
            long pl = p;
            if (pl != x) {
                long r = find(pl);
                parent.put(x, r);
                return r;
            }
            return x;
        }

        void union(long a, long b) {
            long ra = find(a);
            long rb = find(b);
            if (ra == rb) {
                return;
            }
            int sa = sz.getOrDefault(ra, 1);
            int sb = sz.getOrDefault(rb, 1);
            if (sa < sb) {
                parent.put(ra, rb);
                sz.put(rb, sa + sb);
                sz.remove(ra);
            } else {
                parent.put(rb, ra);
                sz.put(ra, sa + sb);
                sz.remove(rb);
            }
        }
    }
}
