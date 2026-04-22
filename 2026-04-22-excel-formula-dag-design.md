# Excel 公式链路 DAG 系统设计

## 1. 概述

### 1.1 项目目标

实现一个独立的公式引擎，解析类似 Excel 的公式语法，构建依赖关系 DAG，并支持跨表公式的链路追踪与执行。

### 1.2 核心需求

- 支持 Excel 兼容语法：`=B3+H1+表单B!D2`
- 跨表公式的 DAG 分开存储，执行时合并相关子 DAG
- 非递归遍历：链路查询和拓扑排序均使用迭代方式
- 高性能：内存加载 + 反向索引优化跨表查找

---

## 2. 功能设计

### 2.1 概念定义

**节点 (FormulaNode)**：单个单元格的公式
- 本表直接依赖的单元格列表
- 跨表引用的单元格列表

**边 (Edge)**：分两类
- `intra_dag_edge`：同表引用边
- `cross_dag_edge`：跨表引用边，带 `(source_sheet, source_cell) → (target_sheet, target_cell)` 信息

### 2.2 公式语法

```
=B3+H1+表单B!D2
```

- 同表引用：`B3`、`H1`
- 跨表引用：`表单B!D2`（格式：`表单名!单元格`）

### 2.3 链路查找流程（追溯依赖）

当 `表单C的B4` 触发时：
1. 查询本表 DAG，得到 `G3`
2. 根据 `G3` 的跨表引用，找到 `表单B!D2`
3. 切换到 `表单B`，查其本表 DAG，得到 `D3`
4. 根据 `D3` 的跨表引用，找到 `表单C!G3`... 形成环路，停止
5. 继续找到 `表单A!B3`，链路结束

使用 **BFS 队列**实现，非递归。

### 2.4 执行流程

1. 根据触发点收集所有涉及的跨表引用，合并相关子 DAG
2. 对合并后的 DAG 做 **Kahn's algorithm**（迭代式拓扑排序）
3. 按拓扑序逐个求值

### 2.5 性能优化

| 优化点 | 方案 |
|--------|------|
| 避免递归 | BFS 队列遍历依赖，Kahn's algorithm 做拓扑排序 |
| 跨表快速查找 | `cross_sheet_backrefs` 反向索引，O(1) 查找依赖者 |
| 内存友好 | 仅加载涉及的子 DAG，不加载全量图 |
| 批量更新 | 计算结果最后批量写回数据库 |
| 缓存 | 公式解析结果缓存，避免重复解析 |

---

## 3. 数据表结构

### 3.1 ER 图

```
sheets
  │
  └── formulas
          │
          ├── formula_intra_deps (同表依赖)
          │
          └── formula_cross_deps (跨表依赖)
                  │
                  └── cross_sheet_backrefs (反向索引)
```

### 3.2 表结构

```sql
-- 表单元数据：存储所有表单的基本信息
CREATE TABLE sheets (
    id          BIGINT PRIMARY KEY,                    -- 主键，自增
    name        VARCHAR(255) NOT NULL UNIQUE,         -- 表单名称，全局唯一
    created_at  TIMESTAMP DEFAULT NOW(),              -- 创建时间
    updated_at  TIMESTAMP DEFAULT NOW()               -- 最后更新时间
);

-- 公式定义：存储每个单元格的公式表达式
CREATE TABLE formulas (
    id          BIGINT PRIMARY KEY,                   -- 主键，自增
    sheet_id    BIGINT NOT NULL REFERENCES sheets(id), -- 所属表单ID，外键关联sheets.id
    cell_ref    VARCHAR(50) NOT NULL,                 -- 单元格引用，如 "B4"、"G3"
    expression  TEXT NOT NULL,                        -- 公式表达式，如 "=A5+B4"
    created_at  TIMESTAMP DEFAULT NOW(),             -- 创建时间
    updated_at  TIMESTAMP DEFAULT NOW(),             -- 最后更新时间
    UNIQUE(sheet_id, cell_ref)                       -- 同一表单内单元格引用唯一
);

-- 本表依赖：存储公式直接依赖的同表单元格（用于构建本表DAG）
CREATE TABLE formula_intra_deps (
    id          BIGINT PRIMARY KEY,                   -- 主键，自增
    formula_id  BIGINT NOT NULL REFERENCES formulas(id), -- 公式ID，外键关联formulas.id
    dep_cell    VARCHAR(50) NOT NULL                   -- 被依赖的同表单元格，如 "D3"、"A5"
);

-- 跨表依赖：存储公式引用的其他表单单元格（用于构建跨表DAG边）
CREATE TABLE formula_cross_deps (
    id              BIGINT PRIMARY KEY,               -- 主键，自增
    formula_id      BIGINT NOT NULL REFERENCES formulas(id), -- 公式ID，外键关联formulas.id
    target_sheet    VARCHAR(255) NOT NULL,            -- 被引用单元格所在的表单名
    target_cell     VARCHAR(50) NOT NULL              -- 被引用的单元格，如 "D2"、"G3"
);

-- 跨表反向索引：快速查找"谁依赖这个单元格"，避免执行时递归查找
CREATE TABLE cross_sheet_backrefs (
    id                  BIGINT PRIMARY KEY,           -- 主键，自增
    target_sheet        VARCHAR(255) NOT NULL,        -- 被依赖的单元格所在表单名
    target_cell         VARCHAR(50) NOT NULL,          -- 被依赖的单元格
    source_formula_id   BIGINT NOT NULL REFERENCES formulas(id), -- 依赖者的公式ID
    UNIQUE(target_sheet, target_cell, source_formula_id) -- 联合唯一，防止重复索引
);
```

---

## 4. Java 内存结构

### 4.1 核心类

```java
// 跨表引用
public class CrossRef {
    private String sheetName;
    private String cellRef;
    private long formulaId;
}

// 公式节点
public class FormulaNode {
    private long formulaId;
    private String sheetName;
    private String cellRef;
    private String expression;
    private List<String> intraDeps;      // 同表依赖: ["D3", "A5"]
    private List<CrossRef> crossDeps;   // 跨表依赖
}

// 跨表反向索引
public class CrossSheetIndex {
    // key: "sheetName!cellRef" → value: 依赖它的 formulaId 列表
    private Map<String, List<Long>> backrefs = new HashMap<>();

    public List<Long> getDependents(String sheetName, String cellRef) {
        return backrefs.getOrDefault(sheetName + "!" + cellRef, Collections.emptyList());
    }

    public void add(String targetSheet, String targetCell, long sourceFormulaId) {
        String key = targetSheet + "!" + targetCell;
        backrefs.computeIfAbsent(key, k -> new ArrayList<>()).add(sourceFormulaId);
    }
}

// 公式引擎
public class FormulaEngine {
    // sheetName → SheetDAG
    private Map<String, SheetDAG> sheets = new HashMap<>();
    private CrossSheetIndex crossIndex = new CrossSheetIndex();

    public record SheetDAG(String sheetName, Map<String, FormulaNode> cells) {}
}
```

### 4.2 链路查询（非递归 BFS）

```java
public class FormulaEngine {

    public List<FormulaNode> collectAffectedFormulas(String triggerSheet, String triggerCell) {
        Map<String, Boolean> visited = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(triggerSheet + "!" + triggerCell);

        List<FormulaNode> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (Boolean.TRUE.equals(visited.get(current))) {
                continue;
            }
            visited.put(current, true);

            String[] parts = current.split("!", 2);
            String sheetName = parts[0];
            String cellRef = parts[1];

            FormulaNode node = findFormulaNode(sheetName, cellRef);
            if (node == null) {
                continue;
            }
            result.add(node);

            // 1. 处理本表依赖：直接加入队列
            for (String dep : node.getIntraDeps()) {
                String key = sheetName + "!" + dep;
                if (!Boolean.TRUE.equals(visited.get(key))) {
                    queue.add(key);
                }
            }

            // 2. 处理跨表依赖：用反向索引查找
            for (CrossRef crossDep : node.getCrossDeps()) {
                List<Long> dependentIds = crossIndex.getDependents(
                    crossDep.getSheetName(), crossDep.getCellRef()
                );
                for (Long depFormulaId : dependentIds) {
                    FormulaNode depNode = findFormulaById(depFormulaId);
                    if (depNode != null) {
                        String key = depNode.getSheetName() + "!" + depNode.getCellRef();
                        if (!Boolean.TRUE.equals(visited.get(key))) {
                            queue.add(key);
                        }
                    }
                }
            }
        }
        return result;
    }
}
```

### 4.3 执行流程（非递归拓扑排序）

```java
public class FormulaEngine {

    public void executeFormulas(List<FormulaNode> nodes) {
        // 1. 构建局部 DAG
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>();

        for (FormulaNode node : nodes) {
            String key = key(node);
            inDegree.put(key, 0);
            adjList.put(key, new ArrayList<>());
        }

        for (FormulaNode node : nodes) {
            String key = key(node);
            // 本表依赖
            for (String dep : node.getIntraDeps()) {
                String depKey = node.getSheetName() + "!" + dep;
                if (inDegree.containsKey(depKey)) {
                    adjList.computeIfAbsent(depKey, k -> new ArrayList<>()).add(key);
                    inDegree.merge(key, 1, Integer::sum);
                }
            }
            // 跨表依赖
            for (CrossRef crossDep : node.getCrossDeps()) {
                String depKey = crossDep.getSheetName() + "!" + crossDep.getCellRef();
                if (inDegree.containsKey(depKey)) {
                    adjList.computeIfAbsent(depKey, k -> new ArrayList<>()).add(key);
                    inDegree.merge(key, 1, Integer::sum);
                }
            }
        }

        // 2. Kahn's algorithm（迭代拓扑排序）
        Queue<String> zeroQueue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                zeroQueue.add(entry.getKey());
            }
        }

        List<String> sorted = new ArrayList<>();
        while (!zeroQueue.isEmpty()) {
            String current = zeroQueue.poll();
            sorted.add(current);

            for (String next : adjList.getOrDefault(current, Collections.emptyList())) {
                int newDegree = inDegree.get(next) - 1;
                inDegree.put(next, newDegree);
                if (newDegree == 0) {
                    zeroQueue.add(next);
                }
            }
        }

        // 3. 按拓扑序求值
        Map<String, Object> cellValues = new HashMap<>();
        for (String k : sorted) {
            FormulaNode node = findFormulaNodeByKey(k);
            Object value = evaluate(node, cellValues);
            cellValues.put(k, value);
        }

        // 批量更新
        batchUpdateCellValues(cellValues);
    }

    private String key(FormulaNode node) {
        return node.getSheetName() + "!" + node.getCellRef();
    }
}
```

### 4.4 求值器接口

```java
public interface FormulaEvaluator {
    Object evaluate(FormulaNode node, Map<String, Object> currentValues);
}

// 示例：四则运算求值器
public class SimpleEvaluator implements FormulaEvaluator {

    @Override
    public Object evaluate(FormulaNode node, Map<String, Object> currentValues) {
        // 解析表达式并求值
        // 实际实现可用 JavaScript 引擎 (Nashorn / GraalJS) 或 Antlr
        String expr = node.getExpression();
        // ... 解析 expr 中的单元格引用，替换为 currentValues 中的值
        // ... 执行四则运算
        return result;
    }
}
```

---

## 5. 数据层（Spring Data JDBC / JPA）

### 5.1 实体类

```java
@Entity
@Table(name = "sheets")
public class Sheet {
    @Id private Long id;                    // 主键
    private String name;                   // 表单名称
    private LocalDateTime createdAt;       // 创建时间
    private LocalDateTime updatedAt;       // 更新时间
}

@Entity
@Table(name = "formulas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sheet_id", "cell_ref"})
})
public class Formula {
    @Id private Long id;                  // 主键
    private Long sheetId;                  // 所属表单ID
    private String cellRef;                // 单元格引用
    private String expression;             // 公式表达式
    private LocalDateTime createdAt;       // 创建时间
    private LocalDateTime updatedAt;       // 更新时间

    @OneToMany(mappedBy = "formulaId", cascade = CascadeType.ALL)
    private List<FormulaIntraDep> intraDeps = new ArrayList<>();  // 本表依赖列表

    @OneToMany(mappedBy = "formulaId", cascade = CascadeType.ALL)
    private List<FormulaCrossDep> crossDeps = new ArrayList<>();  // 跨表依赖列表
}

@Entity
@Table(name = "formula_intra_deps")
public class FormulaIntraDep {
    @Id private Long id;                  // 主键
    private Long formulaId;                // 关联的公式ID
    private String depCell;                // 被依赖的同表单元格
}

@Entity
@Table(name = "formula_cross_deps")
public class FormulaCrossDep {
    @Id private Long id;                  // 主键
    private Long formulaId;                // 关联的公式ID
    private String targetSheet;            // 被引用单元格所在表单
    private String targetCell;            // 被引用的单元格
}

@Entity
@Table(name = "cross_sheet_backrefs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"target_sheet", "target_cell", "source_formula_id"})
})
public class CrossSheetBackref {
    @Id private Long id;                  // 主键
    private String targetSheet;            // 被依赖的单元格所在表单
    private String targetCell;             // 被依赖的单元格
    private Long sourceFormulaId;         // 依赖者的公式ID
}
```

---

## 6. DAG 构建流程

解析公式 `=D3+表单C!G3`：

1. 解析表达式，提取所有单元格引用
2. 同表的 → 加入 `formula_intra_deps`
3. 跨表的 → 加入 `formula_cross_deps`，同时建立反向索引 `cross_sheet_backrefs`
4. 持久化到数据库
5. 更新内存中的 `SheetDAG` 和 `CrossIndex`

---

## 7. 示例链路

### 7.1 示例数据

现有表单 A / B / C，触发点为 `表单C的B4`。

### 7.2 数据填充

#### sheets 表

| id | name | created_at | updated_at |
|----|------|------------|------------|
| 1 | 表单A | ... | ... |
| 2 | 表单B | ... | ... |
| 3 | 表单C | ... | ... |

#### formulas 表

| id | sheet_id | cell_ref | expression | created_at | updated_at |
|----|----------|----------|------------|------------|------------|
| 1 | 3 | G3 | `=A5+B4` | ... | ... |
| 2 | 2 | D2 | `=D3+表单C!G3` | ... | ... |
| 3 | 1 | B3 | `=H1+表单B!D2` | ... | ... |

#### formula_intra_deps 表

| id | formula_id | dep_cell |
|----|------------|----------|
| 1 | 1 | A5 |
| 2 | 1 | B4 |
| 3 | 2 | D3 |

#### formula_cross_deps 表

| id | formula_id | target_sheet | target_cell |
|----|------------|--------------|-------------|
| 1 | 2 | 表单C | G3 |
| 2 | 3 | 表单B | D2 |

#### cross_sheet_backrefs 表

| id | target_sheet | target_cell | source_formula_id |
|----|--------------|-------------|-------------------|
| 1 | 表单C | G3 | 2 |
| 2 | 表单B | D2 | 3 |

### 7.3 链路查找过程

**触发点**：表单C的B4

1. **查询 `cross_sheet_backrefs`**，找 `target_sheet=表单C AND target_cell=G3` 的记录 → 得到 `source_formula_id=2`（表单B的D2）

2. **D2的formula_cross_deps** 包含 `表单C!G3`，但G3本身依赖B4（环路，停止本分支）

3. **继续查询 `cross_sheet_backrefs`**，找 `target_sheet=表单B AND target_cell=D2` → 得到 `source_formula_id=3`（表单A的B3）

4. **B3** 无更上游跨表依赖，链路结束

### 7.4 最终链路

`B4 → G3 → D2 → B3`

---

### 7.5 新增公式场景

在 `表单C` 新增公式 `G4 = A8 + B4`，触发点仍为 `表单C的B4`。

**新增公式**：

| 表单 | 单元格 | 公式 |
|------|--------|------|
| 表单C | G4 | `=A8+B4` |

### 7.6 补充数据填充

#### sheets 表（不变）

| id | name | created_at | updated_at |
|----|------|------------|------------|
| 1 | 表单A | ... | ... |
| 2 | 表单B | ... | ... |
| 3 | 表单C | ... | ... |

#### formulas 表（新增第4条）

| id | sheet_id | cell_ref | expression | created_at | updated_at |
|----|----------|----------|------------|------------|------------|
| 1 | 3 | G3 | `=A5+B4` | ... | ... |
| 2 | 2 | D2 | `=D3+表单C!G3` | ... | ... |
| 3 | 1 | B3 | `=H1+表单B!D2` | ... | ... |
| 4 | 3 | G4 | `=A8+B4` | ... | ... |

#### formula_intra_deps 表（新增第4、5条）

| id | formula_id | dep_cell |
|----|------------|----------|
| 1 | 1 | A5 |
| 2 | 1 | B4 |
| 3 | 2 | D3 |
| 4 | 4 | A8 |
| 5 | 4 | B4 |

#### formula_cross_deps 表（不变）

| id | formula_id | target_sheet | target_cell |
|----|------------|--------------|-------------|
| 1 | 2 | 表单C | G3 |
| 2 | 3 | 表单B | D2 |

#### cross_sheet_backrefs 表（不变）

| id | target_sheet | target_cell | source_formula_id |
|----|--------------|-------------|-------------------|
| 1 | 表单C | G3 | 2 |
| 2 | 表单B | D2 | 3 |

### 7.7 链路查找过程（新增G4后）

**触发点**：表单C的B4

B4 被两个公式依赖：`G3`（intra_deps）和 `G4`（intra_deps），BFS 从 B4 出发：

1. **起点**：B4，查询 `formula_intra_deps` 中 `dep_cell=B4` 的记录 → 得到 `formula_id=1（G3）` 和 `formula_id=4（G4）`

2. **分支一**：G3
   - G3 的 `formula_cross_deps` 包含 `表单C!G3`
   - 查询 `cross_sheet_backrefs` 找 `target_sheet=表单C AND target_cell=G3` → 得到 `source_formula_id=2`（表单B的D2）
   - D2 的 `formula_intra_deps` 包含 `D3`
   - D3 无公式，跳过
   - D2 的 `formula_cross_deps` 包含 `表单C!G3`，但 G3 已在链路中（环路，停止本分支）

3. **分支二**：G4
   - G4 的 `formula_cross_deps` 为空，无跨表依赖，结束

4. **从D2继续追溯**：
   - 查询 `cross_sheet_backrefs` 找 `target_sheet=表单B AND target_cell=D2` → 得到 `source_formula_id=3`（表单A的B3）
   - B3 的 `formula_cross_deps` 包含 `表单B!D2`，但 D2 已在链路中（环路，停止）
   - B3 无更上游依赖，链路结束

### 7.8 最终链路

两条链路：
- **链路1**：`B4 → G3 → D2 → B3`
- **链路2**：`B4 → G4`

合并后：**B4 → G3 → D2 → B3** 和 **B4 → G4**

---

## 8. SQL 查询语句

### 8.1 基础 CRUD

```sql
-- 插入表单
INSERT INTO sheets (name, created_at, updated_at)
VALUES ('表单A', NOW(), NOW());

-- 插入公式（包含同表依赖和跨表依赖）
INSERT INTO formulas (sheet_id, cell_ref, expression, created_at, updated_at)
VALUES (3, 'G3', '=A5+B4', NOW(), NOW())
RETURNING id;  -- 假设返回 id=1

-- 插入同表依赖
INSERT INTO formula_intra_deps (formula_id, dep_cell) VALUES
(1, 'A5'),
(1, 'B4');

-- 插入跨表依赖（同时建立反向索引）
INSERT INTO formula_cross_deps (formula_id, target_sheet, target_cell) VALUES
(1, '表单B', 'D2');

INSERT INTO cross_sheet_backrefs (target_sheet, target_cell, source_formula_id) VALUES
('表单B', 'D2', 1);

-- 查询某表单所有公式
SELECT f.*, s.name AS sheet_name
FROM formulas f
JOIN sheets s ON f.sheet_id = s.id
WHERE s.name = '表单C';
```

### 8.2 链路查询（追溯依赖）

```sql
-- 1. 查询某单元格的公式（用于BFS遍历起点）
SELECT f.*
FROM formulas f
JOIN sheets s ON f.sheet_id = s.id
WHERE s.name = '表单C' AND f.cell_ref = 'G3';

-- 2. 查询某公式的同表依赖
SELECT dep_cell
FROM formula_intra_deps
WHERE formula_id = 1;

-- 3. 查询某公式的跨表依赖
SELECT target_sheet, target_cell
FROM formula_cross_deps
WHERE formula_id = 1;

-- 4. 反向查找：谁依赖这个单元格（核心查询，用于跨表追溯）
SELECT f.id, f.sheet_id, f.cell_ref, f.expression, s.name AS sheet_name
FROM cross_sheet_backrefs c
JOIN formulas f ON c.source_formula_id = f.id
JOIN sheets s ON f.sheet_id = s.id
WHERE c.target_sheet = '表单C' AND c.target_cell = 'G3';
```

### 8.3 环路检测

```sql
-- 检测新增跨表依赖是否形成环路
-- 使用递归CTE检测从 target 能否追溯回 source
WITH RECURSIVE cycle_check AS (
    -- 起点：目标单元格
    SELECT
        f.id AS formula_id,
        f.sheet_id,
        f.cell_ref,
        ARRAY[f.id] AS path
    FROM formulas f
    JOIN sheets s ON f.sheet_id = s.id
    WHERE s.name = '表单C' AND f.cell_ref = 'G3'

    UNION ALL

    -- 递归：沿着被依赖方向向上追溯
    SELECT
        f.id,
        f.sheet_id,
        f.cell_ref,
        cc.path || f.id
    FROM cycle_check cc
    JOIN formula_cross_deps cd ON cd.formula_id = cc.formula_id
    JOIN formulas f ON f.id = cd.formula_id
    JOIN sheets s ON f.sheet_id = s.id
    WHERE NOT f.id = ANY(cc.path)  -- 避免死循环
)
SELECT formula_id FROM cycle_check WHERE formula_id = 3;  -- 3是源公式ID
-- 如果有结果，说明存在环路
```

### 8.4 拓扑排序（ Kahn's Algorithm 的 SQL 实现）

```sql
-- 对指定链路中的公式进行拓扑排序（按依赖顺序）
WITH RECURSIVE topo AS (
    -- 起点：没有任何依赖的公式（入度为0）
    SELECT
        f.id,
        f.cell_ref,
        s.name AS sheet_name,
        0 AS depth
    FROM formulas f
    JOIN sheets s ON f.sheet_id = s.id
    WHERE f.id IN (1, 2, 3)  -- 指定要排序的公式ID列表
      AND NOT EXISTS (
          SELECT 1 FROM formula_intra_deps d
          WHERE d.formula_id = f.id
          UNION ALL
          SELECT 1 FROM formula_cross_deps d
          WHERE d.formula_id = f.id
      )

    UNION ALL

    -- 递归：选取依赖已完成的公式
    SELECT
        f.id,
        f.cell_ref,
        s.name AS sheet_name,
        t.depth + 1
    FROM topo t
    JOIN formulas f ON f.id = t.id
    JOIN sheets s ON f.sheet_id = s.id
    WHERE NOT EXISTS (
        -- 检查是否所有依赖都已被访问
        SELECT 1 FROM (
            SELECT dep_cell AS dep_ref, NULL AS dep_sheet
            FROM formula_intra_deps
            WHERE formula_id = f.id
            UNION ALL
            SELECT target_cell AS dep_ref, target_sheet AS dep_sheet
            FROM formula_cross_deps
            WHERE formula_id = f.id
        ) deps
        WHERE NOT (
            -- 检查这个依赖是否在已访问列表中
            EXISTS (
                SELECT 1 FROM topo visited
                WHERE visited.sheet_name = COALESCE(deps.dep_sheet, s.name)
                  AND visited.cell_ref = deps.dep_ref
            )
        )
    )
)
SELECT sheet_name, cell_ref, depth
FROM topo
ORDER BY depth;
```

### 8.5 批量更新

```sql
-- 批量更新公式（修改表达式时，需重建依赖关系）
UPDATE formulas
SET expression = '=A5+B4+C1',
    updated_at = NOW()
WHERE id = 1;

-- 删除旧依赖重建新依赖（事务中执行）
DELETE FROM formula_intra_deps WHERE formula_id = 1;
DELETE FROM formula_cross_deps WHERE formula_id = 1;
DELETE FROM cross_sheet_backrefs WHERE source_formula_id = 1;

INSERT INTO formula_intra_deps (formula_id, dep_cell) VALUES (1, 'A5'), (1, 'B4'), (1, 'C1');
```

### 8.6 索引建议

```sql
-- 核心查询性能优化索引
CREATE INDEX idx_formulas_sheet_id ON formulas(sheet_id);
CREATE INDEX idx_formulas_sheet_cell ON formulas(sheet_id, cell_ref);
CREATE INDEX idx_intra_deps_formula_id ON formula_intra_deps(formula_id);
CREATE INDEX idx_cross_deps_formula_id ON formula_cross_deps(formula_id);
CREATE INDEX idx_cross_deps_target ON formula_cross_deps(target_sheet, target_cell);
CREATE INDEX idx_backrefs_target ON cross_sheet_backrefs(target_sheet, target_cell);
```

---

## 9. 内存优化策略

### 9.1 问题分析

DAG 合并阶段可能存在的内存压力：

| 场景 | 风险 |
|------|------|
| 链路节点过多 | 一次收集全部节点，内存占用高 |
| 跨表链路深 | 多表单数据同时加载 |
| 高频触发 | 每次执行都重建 DAG，频繁 GC |

### 9.2 优化方案

#### 方案一：惰性加载 + 流式合并（推荐）

不一次性加载全部节点，采用迭代器模式，按需从数据库拉取：

```java
public class LazyFormulaCollector implements Iterator<FormulaNode> {
    private Queue<String> queue = new LinkedList<>();
    private Iterator<String> currentBatch;
    private static final int BATCH_SIZE = 100;

    @Override
    public boolean hasNext() {
        return !queue.isEmpty() || (currentBatch != null && currentBatch.hasNext());
    }

    @Override
    public FormulaNode next() {
        if (currentBatch == null || !currentBatch.hasNext()) {
            // 分批从数据库加载
            currentBatch = loadNextBatch(queue, BATCH_SIZE).iterator();
        }
        String key = currentBatch.next();
        return findFormulaNodeByKey(key);
    }
}
```

#### 方案二：拓扑排序增量执行

不等待全部链路收集完成，边收集边执行（类似流水线）：

```java
public void executeFormulasStreaming(String triggerSheet, String triggerCell) {
    Queue<String> queue = new LinkedList<>();
    queue.add(triggerSheet + "!" + triggerCell);

    Map<String, Integer> inDegree = new HashMap<>();
    Map<String, List<String>> adjList = new HashMap<>();
    Map<String, Object> cellValues = new HashMap<>();
    Set<String> processed = new HashSet<>();

    while (!queue.isEmpty() || !inDegree.isEmpty()) {
        // 1. 加载新节点
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (processed.contains(current)) continue;

            FormulaNode node = loadNodeFromDB(current);
            if (node == null) continue;

            // 更新入度和邻接表
            updateDAGStruct(node, inDegree, adjList);

            // 加载依赖节点到队列
            for (String dep : node.getIntraDeps()) {
                queue.add(node.getSheetName() + "!" + dep);
            }
            for (CrossRef crossDep : node.getCrossDeps()) {
                List<Long> ids = crossIndex.getDependents(crossDep.getSheetName(), crossDep.getCellRef());
                for (Long id : ids) {
                    FormulaNode depNode = findFormulaById(id);
                    if (depNode != null) {
                        queue.add(depNode.getSheetName() + "!" + depNode.getCellRef());
                    }
                }
            }
        }

        // 2. 执行入度为0的节点
        List<String> ready = inDegree.entrySet().stream()
            .filter(e -> e.getValue() == 0)
            .map(Map.Entry::getKey)
            .toList();

        for (String key : ready) {
            FormulaNode node = findFormulaNodeByKey(key);
            Object value = evaluate(node, cellValues);
            cellValues.put(key, value);
            processed.add(key);
            inDegree.remove(key);

            // 后继节点入度减1
            for (String next : adjList.getOrDefault(key, Collections.emptyList())) {
                inDegree.merge(next, -1, Integer::sum);
            }
        }
    }
}
```

#### 方案三：链路深度限制

防止极端情况下链路过长：

```java
public class FormulaEngine {
    private static final int MAX_CHAIN_DEPTH = 1000;

    public List<FormulaNode> collectAffectedFormulas(String triggerSheet, String triggerCell) {
        Map<String, Boolean> visited = new HashMap<>();
        Queue<NodeWithDepth> queue = new LinkedList<>();
        queue.add(new NodeWithDepth(triggerSheet + "!" + triggerCell, 0));

        List<FormulaNode> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            NodeWithDepth current = queue.poll();

            if (current.depth > MAX_CHAIN_DEPTH) {
                throw new FormulaChainTooDeepException(
                    "公式链路深度超过限制: " + MAX_CHAIN_DEPTH);
            }

            // ... 其余逻辑不变
        }
    }
}
```

### 9.3 内存安全检查

```java
// 执行前预估内存占用
public class MemoryGuard {
    private static final long MAX_MEMORY_PER_EXECUTION = 100 * 1024 * 1024; // 100MB

    public void checkMemoryBeforeExecution(List<FormulaNode> nodes) {
        long estimated = nodes.size() * 2048L; // 估算每个节点约2KB
        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory();
        if (estimated > MAX_MEMORY_PER_EXECUTION || estimated > free) {
            throw new OutOfMemoryError(
                "公式执行预估内存: " + estimated / 1024 + "KB, 可用: " + free / 1024 + "KB"
            );
        }
    }
}
```

---

## 10. 技术栈

- **语言**：Java
- **持久化**：Spring Data JDBC / JPA + PostgreSQL
- **公式解析**：Antlr4 或 JavaScript 引擎（GraalJS）
- **内存加载**：启动时全量加载，或惰性按需加载
