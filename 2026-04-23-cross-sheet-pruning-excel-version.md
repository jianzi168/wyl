# 跨表公式剪支执行示例（Excel坐标版）

## 1. 概述

### 1.1 核心概念

- **Excel坐标**：用于公式表达式（如 B2, C4, D7）
- **伪坐标**：用于数据库内部标识（如 [1]_[5]）
- **公式ID**：用于DAG关系（如 1, 2, 3）
- **单元格ID**：用于物理存储和反向索引

### 1.2 坐标映射关系

```
Excel坐标 ↔ 伪坐标 ↔ 单元格ID

示例：
表A的B2 ↔ [1]_[5] ↔ 单元格ID=1
表A的C4 ↔ [1]_[6] ↔ 单元格ID=2
```

### 1.3 场景设定

**表A**：
- D7 = B2 + C4
- C4 = D5 + B8

**表B**：
- F3 = G4 + H3
- H3 = G9 + 表A!D7

**表C**：
- I7 = J8 + 表B!F3

**触发**：表A的单元格B2

---

## 2. 数据填充

### 2.1 Excel坐标与伪坐标映射表

```sql
-- Excel坐标到伪坐标映射表
CREATE TABLE excel_to_pseudo_mapping (
    id                      BIGSERIAL PRIMARY KEY,
    sheet_id              BIGINT NOT NULL REFERENCES sheets(id),
    excel_coord          VARCHAR(10) NOT NULL,  -- Excel坐标：B2, C4, D7
    pseudo_coord          VARCHAR(100) NOT NULL, -- 伪坐标：[1]_[5]
    row_index             INT NOT NULL,             -- Excel行索引（1-based）
    col_index             INT NOT NULL,             -- Excel列索引（1-based, A=1, B=2）
    UNIQUE(sheet_id, excel_coord),
    UNIQUE(sheet_id, pseudo_coord)
);

-- 插入映射数据（表A）
INSERT INTO excel_to_pseudo_mapping (sheet_id, excel_coord, pseudo_coord, row_index, col_index)
VALUES
-- B2: 行2, 列2
(1, 'B2', '[1]_[5]', 2, 2),

-- C4: 行4, 列3
(1, 'C4', '[1]_[6]', 4, 3),

-- D5: 行5, 列4
(1, 'D5', '[1]_[7]', 5, 4),

-- B8: 行8, 列2
(1, 'B8', '[2]_[5]', 8, 2),

-- D7: 行7, 列4
(1, 'D7', '[1]_[7]', 7, 4);

-- 插入映射数据（表B）
INSERT INTO excel_to_pseudo_mapping (sheet_id, excel_coord, pseudo_coord, row_index, col_index)
VALUES
-- F3: 行3, 列6
(2, 'F3', '[1]_[9]', 3, 6),

-- G4: 行4, 列7
(2, 'G4', '[1]_[10]', 4, 7),

-- H3: 行3, 列8
(2, 'H3', '[1]_[11]', 3, 8),

-- G9: 行9, 列7
(2, 'G9', '[2]_[10]', 9, 7);

-- 插入映射数据（表C）
INSERT INTO excel_to_pseudo_mapping (sheet_id, excel_coord, pseudo_coord, row_index, col_index)
VALUES
-- I7: 行7, 列9
(3, 'I7', '[1]_[12]', 7, 9),

-- J8: 行8, 列10
(3, 'J8', '[1]_[13]', 8, 10);

-- 查询映射关系
SELECT * FROM excel_to_pseudo_mapping ORDER BY sheet_id, row_index, col_index;

/*
id | sheet_id | excel_coord | pseudo_coord | row_index | col_index
----+----------+-------------+--------------+-----------+----------
1   | 1        | B2          | [1]_[5]      | 2         | 2
2   | 1        | C4          | [1]_[6]      | 4         | 3
3   | 1        | D5          | [1]_[7]      | 5         | 4
4   | 1        | B8          | [2]_[5]      | 8         | 2
5   | 1        | D7          | [1]_[7]      | 7         | 4
6   | 2        | F3          | [1]_[9]      | 3         | 6
7   | 2        | G4          | [1]_[10]     | 4         | 7
8   | 2        | H3          | [1]_[11]     | 3         | 8
9   | 2        | G9          | [2]_[10]     | 9         | 7
10  | 3        | I7          | [1]_[12]     | 7         | 9
11  | 3        | J8          | [1]_[13]     | 8         | 10
*/
```

### 2.2 单元格表（使用Excel坐标）

```sql
-- 删除旧数据
DELETE FROM cells WHERE sheet_id IN (1, 2, 3);

-- 插入单元格（使用Excel坐标）
-- cells表保持使用pseudo_coord，但通过excel_to_pseudo_mapping映射
INSERT INTO cells (sheet_id, row_member_id, col_member_id, pseudo_coord, value, pov)
VALUES
-- 表A
(1, 1, 5, '[1]_[5]', 1000, NULL),  -- B2 (触发单元格）
(1, 1, 6, '[1]_[6]', NULL, NULL),   -- C4
(1, 1, 7, '[1]_[7]', 200, NULL),   -- D5
(1, 2, 5, '[2]_[5]', 300, NULL),   -- B8
(1, 1, 7, '[1]_[7]', NULL, NULL),   -- D7 (重复，需要使用不同的伪坐标）

-- 修正：D7应该使用不同的伪坐标，比如[1]_[8]
-- 重新插入表A单元格
DELETE FROM cells WHERE sheet_id = 1;

INSERT INTO cells (sheet_id, row_member_id, col_member_id, pseudo_coord, value, pov)
VALUES
(1, 1, 5, '[1]_[5]', 1000, NULL),  -- B2
(1, 1, 6, '[1]_[6]', NULL, NULL),   -- C4
(1, 1, 7, '[1]_[7]', 200, NULL),   -- D5
(1, 2, 5, '[2]_[5]', 300, NULL),   -- B8
(1, 1, 8, '[1]_[8]', NULL, NULL);   -- D7 (新增列成员）

-- 表B
INSERT INTO cells (sheet_id, row_member_id, col_member_id, pseudo_coord, value, pov)
VALUES
(2, 1, 9, '[1]_[9]', NULL, NULL),   -- F3
(2, 1, 10, '[1]_[10]', 150, NULL), -- G4
(2, 1, 11, '[1]_[11]', NULL, NULL), -- H3
(2, 2, 10, '[2]_[10]', 400, NULL); -- G9

-- 表C
INSERT INTO cells (sheet_id, row_member_id, col_member_id, pseudo_coord, value, pov)
VALUES
(3, 1, 12, '[1]_[12]', NULL, NULL), -- I7
(3, 1, 13, '[1]_[13]', 250, NULL);  -- J8

-- 更新维度成员表，添加D7的列成员
INSERT INTO dimension_members (dimension_id, member_code, member_name, sort_order)
VALUES
(2, 'D2', '指标D2', 8);
```

### 2.3 公式表（使用Excel坐标）

```sql
-- 插入公式（表达式使用Excel坐标）
INSERT INTO formulas (cell_id, expression, formula_type, is_active)
VALUES
-- 表A: D7 = B2 + C4
(5, '=B2+C4', 'CALCULATED', TRUE),

-- 表A: C4 = D5 + B8
(2, '=D5+B8', 'CALCULATED', TRUE),

-- 表B: F3 = G4 + H3
(6, '=G4+H3', 'CALCULATED', TRUE),

-- 表B: H3 = G9 + 表A!D7
(8, '=G9+表A!D7', 'CALCULATED', TRUE),

-- 表C: I7 = J8 + 表B!F3
(10, '=J8+表B!F3', 'CALCULATED', TRUE);

-- 查询公式（包含Excel坐标）
SELECT
    f.id as formula_id,
    s.name as sheet_name,
    m.excel_coord,
    f.expression,
    f.formula_type
FROM formulas f
JOIN cells c ON f.cell_id = c.id
JOIN sheets s ON c.sheet_id = s.id
JOIN excel_to_pseudo_mapping m ON c.sheet_id = m.sheet_id AND c.pseudo_coord = m.pseudo_coord
ORDER BY f.id;

/*
formula_id | sheet_name | excel_coord | expression          | formula_type
-----------+------------+-------------+---------------------+-------------
1          | 表A        | D7          | =B2+C4              | CALCULATED
2          | 表A        | C4          | =D5+B8              | CALCULATED
3          | 表B        | F3          | =G4+H3              | CALCULATED
4          | 表B        | H3          | =G9+表A!D7          | CALCULATED
5          | 表C        | I7          | =J8+表B!F3          | CALCULATED
*/
```

---

## 3. DAG依赖关系（使用公式ID）

### 3.1 DAG边表（使用单元格ID）

```sql
-- 删除旧DAG边
DELETE FROM dag_edges;

-- 插入DAG边（使用单元格ID）
INSERT INTO dag_edges (formula_id, dep_cell_id, dep_type, cross_sheet_id, cross_cell_id)
VALUES
-- 表A: D7 = B2 + C4 (公式ID=1)
(1, 1, 'INTRA', NULL, NULL),  -- 依赖 B2 (单元格ID=1)
(1, 2, 'INTRA', NULL, NULL),  -- 依赖 C4 (单元格ID=2)

-- 表A: C4 = D5 + B8 (公式ID=2)
(2, 3, 'INTRA', NULL, NULL),  -- 依赖 D5 (单元格ID=3)
(2, 4, 'INTRA', NULL, NULL),  -- 依赖 B8 (单元格ID=4)

-- 表B: F3 = G4 + H3 (公式ID=3)
(3, 7, 'INTRA', NULL, NULL),  -- 依赖 G4 (单元格ID=7)
(3, 8, 'INTRA', NULL, NULL),  -- 依赖 H3 (单元格ID=8)

-- 表B: H3 = G9 + 表A!D7 (公式ID=4)
(4, 9, 'INTRA', NULL, NULL),  -- 依赖 G9 (单元格ID=9)
(4, 5, 'CROSS', 1, 5),       -- 依赖 表A!D7 (单元格ID=5)

-- 表C: I7 = J8 + 表B!F3 (公式ID=5)
(5, 11, 'INTRA', NULL, NULL), -- 依赖 J8 (单元格ID=11)
(5, 6, 'CROSS', 2, 6);       -- 依赖 表B!F3 (单元格ID=6)

-- 查询DAG边（显示Excel坐标）
SELECT
    f.id as formula_id,
    m.excel_coord as formula_excel_coord,
    m2.excel_coord as dep_excel_coord,
    de.dep_type,
    s2.name as dep_sheet_name
FROM dag_edges de
JOIN formulas f ON de.formula_id = f.id
JOIN cells c ON f.cell_id = c.id
JOIN excel_to_pseudo_mapping m ON c.sheet_id = m.sheet_id AND c.pseudo_coord = m.pseudo_coord
LEFT JOIN cells dep_c ON de.dep_cell_id = dep_c.id
LEFT JOIN sheets s2 ON dep_c.sheet_id = s2.id
LEFT JOIN excel_to_pseudo_mapping m2 ON dep_c.sheet_id = m2.sheet_id AND dep_c.pseudo_coord = m2.pseudo_coord
ORDER BY de.formula_id;

/*
formula_id | formula_excel_coord | dep_excel_coord | dep_type | dep_sheet_name
-----------+-------------------+-----------------+----------+---------------
1          | D7                | B2              | INTRA    | 表A
1          | D7                | C4              | INTRA    | 表A
2          | C4                | D5              | INTRA    | 表A
2          | C4                | B8              | INTRA    | 表A
3          | F3                | G4              | INTRA    | 表B
3          | F3                | H3              | INTRA    | 表B
4          | H3                | G9              | INTRA    | 表B
4          | H3                | D7              | CROSS    | 表A
5          | I7                | J8              | INTRA    | 表C
5          | I7                | F3              | CROSS    | 表B
*/
```

### 3.2 反向索引表（使用单元格ID）

```sql
-- 删除旧反向索引
DELETE FROM dag_backrefs;

-- 插入反向索引（使用单元格ID）
INSERT INTO dag_backrefs (source_formula_id, target_cell_id, target_sheet_id)
VALUES
-- 表A: B2 (单元格ID=1) 被公式1 (D7) 依赖
(1, 1, 1),

-- 表A: C4 (单元格ID=2) 被公式1 (D7) 依赖
(1, 2, 1),

-- 表A: D5 (单元格ID=3) 被公式2 (C4) 依赖
(2, 3, 1),

-- 表A: B8 (单元格ID=4) 被公式2 (C4) 依赖
(2, 4, 1),

-- 表B: G4 (单元格ID=7) 被公式3 (F3) 依赖
(3, 7, 2),

-- 表B: H3 (单元格ID=8) 被公式3 (F3) 依赖
(3, 8, 2),

-- 表B: G9 (单元格ID=9) 被公式4 (H3) 依赖
(4, 9, 2),

-- 表A: D7 (单元格ID=5) 被公式4 (H3) 依赖
(4, 5, 1),

-- 表B: F3 (单元格ID=6) 被公式5 (I7) 依赖
(5, 6, 2),

-- 表C: J8 (单元格ID=11) 被公式5 (I7) 依赖
(5, 11, 3);

-- 查询反向索引（显示Excel坐标）
SELECT
    s.name as target_sheet,
    m.excel_coord as target_excel_coord,
    db.source_formula_id,
    m2.excel_coord as source_formula_excel_coord
FROM dag_backrefs db
JOIN cells target_c ON db.target_cell_id = target_c.id
JOIN sheets s ON target_c.sheet_id = s.id
JOIN excel_to_pseudo_mapping m ON target_c.sheet_id = m.sheet_id AND target_c.pseudo_coord = m.pseudo_coord
JOIN formulas f ON db.source_formula_id = f.id
JOIN cells source_c ON f.cell_id = source_c.id
JOIN excel_to_pseudo_mapping m2 ON source_c.sheet_id = m2.sheet_id AND source_c.pseudo_coord = m2.pseudo_coord
ORDER BY db.target_sheet_id, db.target_cell_id;

/*
target_sheet | target_excel_coord | source_formula_id | source_formula_excel_coord
------------+-------------------+-------------------+-------------------------
表A         | B2                | 1                 | D7
表A         | C4                | 1                 | D7
表A         | D5                | 2                 | C4
表A         | B8                | 2                 | C4
表A         | D7                | 4                 | H3
表B         | G4                | 3                 | F3
表B         | H3                | 3                 | F3
表B         | G9                | 4                 | H3
表B         | F3                | 5                 | I7
表C         | J8                | 5                 | I7
*/
```

---

## 4. DAG结构持久化（使用Excel坐标）

### 4.1 表A D7的DAG结构

```json
{
  "formula_id": 1,
  "excel_coord": "D7",
  "sheet_name": "表A",
  "type": "FORMULA",
  "formula_type": "CALCULATED",
  "expression": "=B2+C4",
  "depth": 2,
  "dependencies": [
    {
      "cell_id": 1,
      "excel_coord": "B2",
      "sheet": "表A",
      "type": "CELL",
      "value": 1000
    },
    {
      "formula_id": 2,
      "excel_coord": "C4",
      "sheet": "表A",
      "type": "FORMULA",
      "formula_type": "CALCULATED",
      "expression": "=D5+B8",
      "depth": 1,
      "dependencies": [
        {
          "cell_id": 3,
          "excel_coord": "D5",
          "sheet": "表A",
          "type": "CELL",
          "value": 200
        },
        {
          "cell_id": 4,
          "excel_coord": "B8",
          "sheet": "表A",
          "type": "CELL",
          "value": 300
        }
      ]
    }
  ],
  "statistics": {
    "total_nodes": 5,
    "max_depth": 2,
    "cross_sheet_count": 0
  }
}
```

**文本表示（使用Excel坐标）**：
```
[表A:D7:[B2:[表A:C4:[[D5]:[B8]]]]
```

### 4.2 表B H3的DAG结构

```json
{
  "formula_id": 4,
  "excel_coord": "H3",
  "sheet_name": "表B",
  "type": "FORMULA",
  "formula_type": "CALCULATED",
  "expression": "=G9+表A!D7",
  "depth": 2,
  "dependencies": [
    {
      "cell_id": 9,
      "excel_coord": "G9",
      "sheet": "表B",
      "type": "CELL",
      "value": 400
    },
    {
      "formula_id": 1,
      "excel_coord": "D7",
      "sheet": "表A",
      "type": "FORMULA",
      "formula_type": "CALCULATED",
      "expression": "=B2+C4",
      "depth": 2,
      "dependencies": [
        {
          "cell_id": 1,
          "excel_coord": "B2",
          "sheet": "表A",
          "type": "CELL",
          "value": 1000
        },
        {
          "formula_id": 2,
          "excel_coord": "C4",
          "sheet": "表A",
          "type": "FORMULA",
          "depth": 1,
          "dependencies": [
            {"cell_id": 3, "excel_coord": "D5", "type": "CELL", "value": 200},
            {"cell_id": 4, "excel_coord": "B8", "type": "CELL", "value": 300}
          ]
        }
      ]
    }
  ],
  "statistics": {
    "total_nodes": 6,
    "max_depth": 2,
    "cross_sheet_count": 1
  }
}
```

**文本表示（使用Excel坐标）**：
```
[表B:H3:[G9:[表A:D7:[B2:[表A:C4:[[D5]:[B8]]]]]]
```

---

## 5. 剪支逻辑（使用Excel坐标）

### 5.1 触发点

```
触发单元格：表A的B2 (Excel坐标: B2)
单元格ID：1
旧值：1000
新值：1100 (假设变化10%）
```

### 5.2 收集受影响公式（使用反向索引 + 单元格ID）

#### 5.2.1 收集流程概览

```
触发：表A的单元格 B2
    ↓
第1步：通过Excel坐标查找单元格ID
    ↓
第2步：通过单元格ID查询反向索引（初始触发点）
    ↓
第3步：BFS遍历（非递归，使用反向索引）
    ↓
第4步：收集所有受影响的公式ID
```

#### 5.2.2 详细步骤

##### 第1步：通过Excel坐标查找单元格ID

```sql
SELECT id, pseudo_coord
FROM cells c
JOIN excel_to_pseudo_mapping m ON c.sheet_id = m.sheet_id AND c.pseudo_coord = m.pseudo_coord
WHERE c.sheet_id = (SELECT id FROM sheets WHERE name = '表A')
  AND m.excel_coord = 'B2';

/*
id | pseudo_coord
----+--------------
1   | [1]_[5]
*/

结果：触发单元格ID = 1（表A的B2）
```

##### 第2步：通过单元格ID查询反向索引（初始触发点）

```sql
SELECT
    db.source_formula_id,
    f.expression,
    m.excel_coord as source_excel_coord
FROM dag_backrefs db
JOIN formulas f ON db.source_formula_id = f.id
JOIN cells source_c ON f.cell_id = source_c.id
JOIN excel_to_pseudo_mapping m ON source_c.sheet_id = m.sheet_id AND source_c.pseudo_coord = m.pseudo_coord
WHERE db.target_cell_id = 1;

/*
source_formula_id | expression       | source_excel_coord
-----------------+-----------------+--------------------
1                | =B2+C4          | D7
*/

结果：初始受影响公式ID = [1] (表A的D7)
```

##### 第3步：BFS遍历（非递归，使用反向索引）

**BFS（广度优先搜索）遍历原理：**

1. **使用队列**：存储待处理的公式ID
2. **使用集合**：存储已访问的公式ID，避免重复处理
3. **迭代处理**：从队列中取出一个公式，查找依赖该公式单元格的所有公式，添加到队列
4. **反向索引**：通过 `dag_backrefs` 表查找"谁依赖这个单元格"

**BFS遍历过程（详细）：**

```java
/**
 * 收集受影响的公式（非递归，使用反向索引 + BFS）
 */
private Set<Long> collectAffectedFormulas(Long triggerCellId) {
    Set<Long> visitedFormulaIds = new HashSet<>();  // 已访问的公式ID
    Queue<Long> formulaQueue = new LinkedList<>();   // 待处理的公式队列

    // ===== 第1轮：初始触发点 =====
    // 查询：谁依赖触发单元格ID=1？
    // SQL: SELECT source_formula_id FROM dag_backrefs WHERE target_cell_id = 1;
    /*
    查询结果：
    source_formula_id
    -----------------
    1
    */
    List<DagBackref> initialBackrefs = backrefRepo.findByTargetCellId(triggerCellId);
    // 结果：公式ID=1 (D7, 表A)

    for (DagBackref backref : initialBackrefs) {
        formulaQueue.add(backref.getSourceFormulaId());
        visitedFormulaIds.add(backref.getSourceFormulaId());
    }
    // 队列：[1]
    // 已访问：{1}

    // ===== 第2轮：处理公式ID=1 (D7) =====
    currentFormulaId = formulaQueue.poll();  // 取出：1
    // 查找公式ID=1的单元格
    // SQL: SELECT cell_id FROM formulas WHERE id = 1;
    /*
    查询结果：
    cell_id
    -------
    5
    */
    Cell currentCell = cellRepo.findById(formulaRepo.findById(currentFormulaId).get().getCellId()).get();
    // 当前单元格：单元格ID=5 (D7)

    // 查询：谁依赖单元格ID=5？
    // SQL: SELECT source_formula_id FROM dag_backrefs WHERE target_cell_id = 5;
    /*
    查询结果：
    source_formula_id
    -----------------
    4
    */
    List<DagBackref> backrefs = backrefRepo.findByTargetCellId(currentCell.getId());
    // 结果：公式ID=4 (H3, 表B)

    for (DagBackref backref : backrefs) {
        Long depFormulaId = backref.getSourceFormulaId();  // 4
        if (!visitedFormulaIds.contains(depFormulaId)) {
            formulaQueue.add(depFormulaId);   // 添加到队列
            visitedFormulaIds.add(depFormulaId);  // 标记为已访问
        }
    }
    // 队列：[4]
    // 已访问：{1, 4}

    // ===== 第3轮：处理公式ID=4 (H3) =====
    currentFormulaId = formulaQueue.poll();  // 取出：4
    // 查找公式ID=4的单元格
    // SQL: SELECT cell_id FROM formulas WHERE id = 4;
    /*
    查询结果：
    cell_id
    -------
    8
    */
    currentCell = cellRepo.findById(formulaRepo.findById(currentFormulaId).get().getCellId()).get();
    // 当前单元格：单元格ID=8 (H3)

    // 查询：谁依赖单元格ID=8？
    // SQL: SELECT source_formula_id FROM dag_backrefs WHERE target_cell_id = 8;
    /*
    查询结果：
    source_formula_id
    -----------------
    3
    */
    backrefs = backrefRepo.findByTargetCellId(currentCell.getId());
    // 结果：公式ID=3 (F3, 表B)

    for (DagBackref backref : backrefs) {
        Long depFormulaId = backref.getSourceFormulaId();  // 3
        if (!visitedFormulaIds.contains(depFormulaId)) {
            formulaQueue.add(depFormulaId);   // 添加到队列
            visitedFormulaIds.add(depFormulaId);  // 标记为已访问
        }
    }
    // 队列：[3]
    // 已访问：{1, 4, 3}

    // ===== 第4轮：处理公式ID=3 (F3) =====
    currentFormulaId = formulaQueue.poll();  // 取出：3
    // 查找公式ID=3的单元格
    // SQL: SELECT cell_id FROM formulas WHERE id = 3;
    /*
    查询结果：
    cell_id
    -------
    6
    */
    currentCell = cellRepo.findById(formulaRepo.findById(currentFormulaId).get().getCellId()).get();
    // 当前单元格：单元格ID=6 (F3)

    // 查询：谁依赖单元格ID=6？
    // SQL: SELECT source_formula_id FROM dag_backrefs WHERE target_cell_id = 6;
    /*
    查询结果：
    source_formula_id
    -----------------
    5
    */
    backrefs = backrefRepo.findByTargetCellId(currentCell.getId());
    // 结果：公式ID=5 (I7, 表C)

    for (DagBackref backref : backrefs) {
        Long depFormulaId = backref.getSourceFormulaId();  // 5
        if (!visitedFormulaIds.contains(depFormulaId)) {
            formulaQueue.add(depFormulaId);   // 添加到队列
            visitedFormulaIds.add(depFormulaId);  // 标记为已访问
        }
    }
    // 队列：[5]
    // 已访问：{1, 4, 3, 5}

    // ===== 第5轮：处理公式ID=5 (I7) =====
    currentFormulaId = formulaQueue.poll();  // 取出：5
    // 查找公式ID=5的单元格
    // SQL: SELECT cell_id FROM formulas WHERE id = 5;
    /*
    查询结果：
    cell_id
    -------
    12
    */
    currentCell = cellRepo.findById(formulaRepo.findById(currentFormulaId).get().getCellId()).get();
    // 当前单元格：单元格ID=12 (I7)

    // 查询：谁依赖单元格ID=12？
    // SQL: SELECT source_formula_id FROM dag_backrefs WHERE target_cell_id = 12;
    /*
    查询结果：
    （空）
    */
    backrefs = backrefRepo.findByTargetCellId(currentCell.getId());
    // 结果：无

    // 队列为空，停止遍历
    // 队列：[]
    // 已访问：{1, 4, 3, 5}

    return visitedFormulaIds;
}
```

**BFS遍历图解：**

```
触发单元格：表A的B2 (单元格ID=1)
    ↓
    └─→ 公式ID=1 (D7, 表A) ← 初始触发点
            └─→ 公式ID=4 (H3, 表B)
                    └─→ 公式ID=3 (F3, 表B)
                            └─→ 公式ID=5 (I7, 表C)
                                    └─→ 无依赖（停止）
```

**BFS遍历关键点：**

1. **非递归**：使用队列迭代，避免递归导致的栈溢出
2. **反向索引**：通过 `dag_backrefs` 表快速查找"谁依赖这个单元格"
3. **去重**：使用 `visitedFormulaIds` 集合，避免重复处理同一个公式
4. **广度优先**：按层级处理，先处理直接依赖，再处理间接依赖

##### 第4步：收集所有受影响的公式ID

```
受影响公式ID列表：[1, 4, 3, 5]
对应Excel坐标：[D7, H3, F3, I7]
对应表单：[表A, 表B, 表B, 表C]
```

#### 5.2.3 BFS遍历伪代码

```python
def collect_affected_formulas(trigger_cell_id):
    visited = set()           # 已访问的公式ID
    queue = []               # 待处理的公式队列

    # 初始触发点：查找谁依赖触发单元格
    backrefs = query_backrefs(trigger_cell_id)
    for backref in backrefs:
        queue.append(backref.source_formula_id)
        visited.add(backref.source_formula_id)

    # BFS遍历
    while queue:
        formula_id = queue.pop(0)  # 取出队首

        # 查找公式所在的单元格
        cell = get_cell_by_formula_id(formula_id)

        # 查找谁依赖这个单元格
        backrefs = query_backrefs(cell.id)

        for backref in backrefs:
            dep_formula_id = backref.source_formula_id

            # 如果未访问过，添加到队列
            if dep_formula_id not in visited:
                queue.append(dep_formula_id)
                visited.add(dep_formula_id)

    return visited
```

#### 5.2.4 性能分析

**时间复杂度：**
- O(V + E)，其中 V 是公式数量，E 是依赖关系数量
- 每个公式只访问一次，每条边只遍历一次

**空间复杂度：**
- O(V)，存储已访问的公式ID集合

**性能优化：**
- **反向索引**：`dag_backrefs` 表通过 `target_cell_id` 快速查找
- **去重**：避免重复处理同一个公式
- **非递归**：避免递归栈溢出，内存占用稳定

### 5.3 剪支决策

```java
// 剪支决策（使用公式ID）
public PruningDecision shouldPrune(
    Long formulaId,
    Long triggerCellId,
    Map<Long, Double> cellValues
) {
    // 1. 获取公式
    Formula formula = formulaRepo.findById(formulaId).get();

    // 2. 解析公式表达式（Excel坐标）
    ParseResult result = parser.parse(formula.getExpression());
    // 表达式：=B2+C4 (公式ID=1, D7)

    // 3. 检查每个依赖
    for (String depExcelCoord : result.getIntraDependencies()) {
        // 4. 将Excel坐标转换为单元格ID
        Long depCellId = getCellIdByExcelCoord(
            formula.getSheetId(),
            depExcelCoord
        );

        // 5. 检查依赖是否受影响
        if (isDepAffected(depCellId, triggerCellId, cellValues)) {
            // 受影响，不能剪支
            continue;
        } else {
            // 不受影响，可以剪支
            return PruningDecision.prune(
                "NOT_AFFECTED",
                "依赖 " + depExcelCoord + " 不受触发点影响",
                50
            );
        }
    }

    // 所有依赖都受影响，不能剪支
    return PruningDecision.keep();
}

// 检查依赖是否受影响
private boolean isDepAffected(
    Long depCellId,
    Long triggerCellId,
    Map<Long, Double> cellValues
) {
    // 1. 如果依赖就是触发点，受影响
    if (depCellId.equals(triggerCellId)) {
        return true;
    }

    // 2. 检查依赖的值是否变化
    Double oldValue = cellValues.getOrDefault(depCellId, 0.0);
    Double newValue = getCellValue(depCellId);

    double changeRate = calculateChangeRate(oldValue, newValue);

    // 3. 如果变化率大于阈值，受影响
    return changeRate >= CHANGE_THRESHOLD;
}

// 通过Excel坐标获取单元格ID
private Long getCellIdByExcelCoord(Long sheetId, String excelCoord) {
    return cellRepo.findByExcelCoord(sheetId, excelCoord)
        .orElseThrow(() -> new CellNotFoundException("单元格不存在: " + excelCoord))
        .getId();
}
```

### 5.4 剪支结果

| 公式ID | Excel坐标 | 表单 | 决策 | 原因 |
|--------|-----------|------|------|------|
| 2 | C4 | 表A | ⚡ **剪支** | 依赖D5和B8，都不依赖B2 |
| 1 | D7 | 表A | ✓ **执行** | 直接依赖B2 |
| 4 | H3 | 表B | ✓ **执行** | 依赖表A!D7 |
| 3 | F3 | 表B | ✓ **执行** | 依赖H3 |
| 5 | I7 | 表C | ✓ **执行** | 依赖表B!F3 |

---

## 6. 执行链路（使用Excel坐标）

### 6.1 执行顺序（拓扑排序）

```
第1步：公式ID=2 (C4)
- Excel坐标：C4
- 表达式：=D5+B8
- ⚡ 剪支：不执行
- 使用旧值：500 (D5=200, B8=300）

第2步：公式ID=1 (D7)
- Excel坐标：D7
- 表达式：=B2+C4
- ✓ 执行：D7 = 1100 (新B2) + 500 (旧C4) = 1600

第3步：公式ID=4 (H3)
- Excel坐标：H3
- 表达式：=G9+表A!D7
- ✓ 执行：H3 = 400 (G9) + 1600 (新D7) = 2000

第4步：公式ID=3 (F3)
- Excel坐标：F3
- 表达式：=G4+H3
- ✓ 执行：F3 = 150 (G4) + 2000 (新H3) = 2150

第5步：公式ID=5 (I7)
- Excel坐标：I7
- 表达式：=J8+表B!F3
- ✓ 执行：I7 = 250 (J8) + 2150 (新F3) = 2400
```

### 6.2 完整执行日志

```sql
-- 插入执行日志（使用Excel坐标）
INSERT INTO execution_log (execution_id, formula_id, cell_pseudo_coord, action, value_before, value_after, execution_time_ms)
VALUES
('uuid-1', 2, '[1]_[6]', 'PRUNED', 500, 500, 0),  -- C4: 剪支
('uuid-1', 1, '[1]_[8]', 'EXECUTED', 1500, 1600, 5), -- D7: 执行
('uuid-1', 4, '[1]_[11]', 'EXECUTED', 1800, 2000, 5), -- H3: 执行
('uuid-1', 3, '[1]_[9]', 'EXECUTED', 1950, 2150, 5),  -- F3: 执行
('uuid-1', 5, '[1]_[12]', 'EXECUTED', 2200, 2400, 5); -- I7: 执行

-- 查询执行日志（显示Excel坐标）
SELECT
    el.execution_id,
    el.formula_id,
    m.excel_coord,
    s.name as sheet_name,
    el.action,
    el.value_before,
    el.value_after,
    el.execution_time_ms,
    CASE
        WHEN el.action = 'PRUNED' THEN '⚡'
        ELSE '✓'
    END as status
FROM execution_log el
JOIN formulas f ON el.formula_id = f.id
JOIN cells c ON f.cell_id = c.id
JOIN sheets s ON c.sheet_id = s.id
JOIN excel_to_pseudo_mapping m ON c.sheet_id = m.sheet_id AND c.pseudo_coord = m.pseudo_coord
WHERE el.execution_id = 'uuid-1'
ORDER BY el.id;

/*
execution_id | formula_id | excel_coord | sheet_name | action  | value_before | value_after | execution_time_ms | status
-------------+-------------+-------------+------------+---------+-------------+-------------+------------------+--------
uuid-1       | 2           | C4          | 表A       | PRUNED  | 500         | 500         | 0                | ⚡
uuid-1       | 1           | D7          | 表A       | EXECUTED | 1500        | 1600        | 5                | ✓
uuid-1       | 4           | H3          | 表B       | EXECUTED | 1800        | 2000        | 5                | ✓
uuid-1       | 3           | F3          | 表B       | EXECUTED | 1950        | 2150        | 5                | ✓
uuid-1       | 5           | I7          | 表C       | EXECUTED | 2200        | 2400        | 5                | ✓
*/
```

### 6.3 完整执行流程图

```
触发：表A.B2 (Excel坐标: B2, 单元格ID: 1) 变化: 1000 → 1100
  ↓
🔍 收集受影响公式（反向索引 + BFS，使用公式ID）
  ├─ 公式ID=1 (D7) → 依赖B2
  │   └─ 公式ID=4 (H3, 表B) → 依赖D7
  │       └─ 公式ID=3 (F3, 表B) → 依赖H3
  │           └─ 公式ID=5 (I7, 表C) → 依赖F3
  │
  └─ 公式ID=2 (C4) → 依赖D5和B8（但不依赖B2）
  ↓
✂️ 剪支决策（使用Excel坐标）
  ├─ 公式ID=2 (C4): ⚡ 剪支 (依赖D5和B8，都不依赖B2）
  ├─ 公式ID=1 (D7): ✓ 执行 (直接依赖B2)
  ├─ 公式ID=4 (H3): ✓ 执行 (依赖表A!D7)
  ├─ 公式ID=3 (F3): ✓ 执行 (依赖H3)
  └─ 公式ID=5 (I7): ✓ 执行 (依赖表B!F3)
  ↓
📋 拓扑排序（使用公式ID）
  顺序：公式ID=2 → 1 → 4 → 3 → 5
  ↓
⚡ 执行公式（使用Excel坐标表达式）
  1. 公式ID=2 (C4): ⚡ 剪支 (=D5+B8 = 500)
  2. 公式ID=1 (D7): ✓ 执行 (=B2+C4 = 1100+500 = 1600)
  3. 公式ID=4 (H3): ✓ 执行 (=G9+表A!D7 = 400+1600 = 2000)
  4. 公式ID=3 (F3): ✓ 执行 (=G4+H3 = 150+2000 = 2150)
  5. 公式ID=5 (I7): ✓ 执行 (=J8+表B!F3 = 250+2150 = 2400)
  ↓
💾 批量更新数据库
  ├─ UPDATE cells SET value = 1600 WHERE id = 5 (D7)
  ├─ UPDATE cells SET value = 2000 WHERE id = 8 (H3)
  ├─ UPDATE cells SET value = 2150 WHERE id = 6 (F3)
  └─ UPDATE cells SET value = 2400 WHERE id = 10 (I7)
```

---

## 7. 性能统计

```
📊 执行统计:
  ├─ 总公式数: 5个
  ├─ 剪支数: 1个 (20%)
  ├─ 执行数: 4个 (80%)
  ├─ 总耗时: 20ms
  ├─ 平均耗时: 5ms/公式
  ├─ 节省时间: 5ms (20%)
  └─ 跨表层数: 3层（表A → 表B → 表C）
```

---

## 8. 总结

### 8.1 核心设计

✅ **三套坐标系统**：
- **Excel坐标**：公式表达式使用（B2, C4, D7）
- **伪坐标**：数据库内部标识（[1]_[5]）
- **公式ID/单元格ID**：DAG关系使用（1, 2, 3）

✅ **坐标映射**：
- Excel坐标 ↔ 伪坐标 ↔ 单元格ID
- 通过 `excel_to_pseudo_mapping` 表维护

✅ **DAG关系**：
- 使用单元格ID建立依赖关系
- 使用公式ID建立反向索引
- 完全避免递归

### 8.2 优势

- **用户友好**：公式表达式使用Excel坐标，符合习惯
- **性能优化**：使用ID进行DAG查询，避免字符串匹配
- **灵活扩展**：支持复杂的公式表达式
- **可观测性**：完整的执行日志和统计

### 8.3 关键指标

- **剪支率**：20% (1/5)
- **执行时间**：20ms (4个公式）
- **平均时间**：5ms/公式
- **跨表层数**：3层（表A → 表B → 表C）

这个设计完美结合了Excel坐标的用户友好性和ID查询的高性能！
