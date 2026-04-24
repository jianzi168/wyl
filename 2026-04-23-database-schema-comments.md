# 数据库表结构与详细注释

## 1. 表注释规范

### 1.1 PostgreSQL表注释语法

```sql
-- 添加表注释
COMMENT ON TABLE table_name IS '表的作用和用途';

-- 添加字段注释
COMMENT ON COLUMN table_name.column_name IS '字段的作用和描述';
```

---

## 2. 核心表结构（带注释）

### 2.1 维度表（dimensions）

```sql
-- 维度表：存储表单的行列维度定义
-- 作用：
--   - 定义表单的行维度和列维度（如时间、产品、区域等）
--   - 每个维度包含多个维度成员（如2024Q1、2024Q2、产品A、产品B）
--   - 支持多维度表单的伪坐标定位
CREATE TABLE dimensions (
    id              BIGSERIAL PRIMARY KEY,                  -- 维度主键，自增ID
    name            VARCHAR(100) NOT NULL UNIQUE,          -- 维度名称，如"时间"、"产品"、"区域"
    type            VARCHAR(20) NOT NULL,                  -- 维度类型，'ROW'（行维度）或'COLUMN'（列维度）
    description     TEXT,                                  -- 维度描述，说明维度的业务含义
    created_at      TIMESTAMP DEFAULT NOW(),                  -- 创建时间
    updated_at      TIMESTAMP DEFAULT NOW()                   -- 最后更新时间
);

-- 添加表注释
COMMENT ON TABLE dimensions IS '维度表：存储表单的行列维度定义，如时间、产品、区域等维度信息';

-- 添加字段注释
COMMENT ON COLUMN dimensions.id IS '维度主键，自增ID，唯一标识一个维度';
COMMENT ON COLUMN dimensions.name IS '维度名称，如"时间"、"产品"、"区域"，全局唯一';
COMMENT ON COLUMN dimensions.type IS '维度类型，ROW表示行维度，COLUMN表示列维度';
COMMENT ON COLUMN dimensions.description IS '维度描述，说明维度的业务含义，如"财务季度"、"产品类别"';
COMMENT ON COLUMN dimensions.created_at IS '创建时间，记录维度何时创建';
COMMENT ON COLUMN dimensions.updated_at IS '最后更新时间，记录维度何时修改';
```

### 2.2 维度成员表（dimension_members）

```sql
-- 维度成员表：存储维度的所有成员
-- 作用：
--   - 存储每个维度的具体成员（如2024Q1、2024Q2、产品A、产品B）
--   - 支持层级结构（如年 → 季度 → 月）
--   - 提供排序和扩展属性功能
--   - 与维度表关联，形成维度树
CREATE TABLE dimension_members (
    id              BIGSERIAL PRIMARY KEY,                  -- 成员主键，自增ID
    dimension_id     BIGINT NOT NULL REFERENCES dimensions(id),  -- 所属维度ID，外键关联dimensions表
    member_code     VARCHAR(100) NOT NULL,                  -- 成员编码，如'2024Q1'、'PROD_A'，用于伪坐标生成
    member_name     VARCHAR(200),                           -- 成员名称，如'2024年第一季度'、'产品A'，用于展示
    parent_id       BIGINT REFERENCES dimension_members(id),     -- 父成员ID，支持层级结构（如年 → 季度）
    sort_order      INT DEFAULT 0,                           -- 排序字段，控制成员显示顺序
    properties      JSONB,                                  -- 扩展属性，存储额外的业务信息
    created_at      TIMESTAMP DEFAULT NOW(),                  -- 创建时间
    updated_at      TIMESTAMP DEFAULT NOW(),                   -- 最后更新时间,
    UNIQUE(dimension_id, member_code)                       -- 同一维度内成员编码唯一
);

-- 添加表注释
COMMENT ON TABLE dimension_members IS '维度成员表：存储维度的所有成员，如时间节点、产品类别等，支持层级结构';

-- 添加字段注释
COMMENT ON COLUMN dimension_members.id IS '成员主键，自增ID，唯一标识一个维度成员';
COMMENT ON COLUMN dimension_members.dimension_id IS '所属维度ID，外键关联dimensions表，表示成员属于哪个维度';
COMMENT ON COLUMN dimension_members.member_code IS '成员编码，如2024Q1、PROD_A，用于伪坐标生成，全局唯一';
COMMENT ON COLUMN dimension_members.member_name IS '成员名称，如2024年第一季度、产品A，用于用户界面展示';
COMMENT ON COLUMN dimension_members.parent_id IS '父成员ID，支持层级结构，如2024年 → 2024Q1 → 2024Q1月';
COMMENT ON COLUMN dimension_members.sort_order IS '排序字段，控制成员显示顺序，数值越小越靠前';
COMMENT ON COLUMN dimension_members.properties IS '扩展属性，JSON格式，存储额外的业务信息，如颜色、图标等';
COMMENT ON COLUMN dimension_members.created_at IS '创建时间，记录成员何时添加';
COMMENT ON COLUMN dimension_members.updated_at IS '最后更新时间，记录成员何时修改';
COMMENT ON COLUMN dimension_members.dimension_id IS '约束：唯一约束，同一维度内成员编码不能重复';
```

### 2.3 Excel坐标映射表（excel_to_pseudo_mapping）

```sql
-- Excel坐标映射表：Excel坐标与伪坐标的双向映射
-- 作用：
--   - 建立Excel坐标（B2, C4）与伪坐标（[1]_[5]）的双向映射
--   - 支持Excel坐标的解析和生成
--   - 维护行列索引信息，支持坐标转换
--   - 提供用户友好的Excel坐标展示
CREATE TABLE excel_to_pseudo_mapping (
    id              BIGSERIAL PRIMARY KEY,                  -- 映射主键，自增ID
    sheet_id        BIGINT NOT NULL REFERENCES sheets(id),    -- 所属表单ID，外键关联sheets表
    excel_coord      VARCHAR(10) NOT NULL,                  -- Excel坐标，如'B2'、'C4'，符合Excel命名规则
    pseudo_coord     VARCHAR(100) NOT NULL,                  -- 伪坐标，格式为[行成员ID]_[列成员ID]，如[1]_[5]
    row_index       INT NOT NULL,                             -- Excel行索引（1-based），如2表示第2行
    col_index       INT NOT NULL,                             -- Excel列索引（1-based），A=1, B=2, C=3等
    UNIQUE(sheet_id, excel_coord),                          -- 同一表单内Excel坐标唯一
    UNIQUE(sheet_id, pseudo_coord)                         -- 同一表单内伪坐标唯一
);

-- 添加表注释
COMMENT ON TABLE excel_to_pseudo_mapping IS 'Excel坐标映射表：建立Excel坐标（B2, C4）与伪坐标（[1]_[5]）的双向映射，支持Excel坐标解析和生成';

-- 添加字段注释
COMMENT ON COLUMN excel_to_pseudo_mapping.id IS '映射主键，自增ID，唯一标识一个坐标映射';
COMMENT ON COLUMN excel_to_pseudo_mapping.sheet_id IS '所属表单ID，外键关联sheets表，表示坐标属于哪个表单';
COMMENT ON COLUMN excel_to_pseudo_mapping.excel_coord IS 'Excel坐标，如B2、C4，符合Excel命名规则，用于公式表达式和用户界面';
COMMENT ON COLUMN excel_to_pseudo_mapping.pseudo_coord IS '伪坐标，格式为[行成员ID]_[列成员ID]，如[1]_[5]，用于数据库内部标识';
COMMENT ON COLUMN excel_to_pseudo_mapping.row_index IS 'Excel行索引（1-based），如2表示第2行，用于坐标转换';
COMMENT ON COLUMN excel_to_pseudo_mapping.col_index IS 'Excel列索引（1-based），A=1, B=2, C=3，用于坐标转换';
COMMENT ON COLUMN excel_to_pseudo_mapping.sheet_id IS '约束1：同一表单内Excel坐标不能重复';
COMMENT ON COLUMN excel_to_pseudo_mapping.sheet_id IS '约束2：同一表单内伪坐标不能重复';
```

### 2.4 表单表（sheets）

```sql
-- 表单表：存储网格表单的基本信息
-- 作用：
--   - 定义表单的名称和行列维度
--   - 指定表单使用哪个行维度和列维度
--   - 存储表单的描述信息
--   - 作为单元格和公式的容器
CREATE TABLE sheets (
    id              BIGSERIAL PRIMARY KEY,                  -- 表单主键，自增ID
    name            VARCHAR(255) NOT NULL UNIQUE,          -- 表单名称，如"销售表"、"成本表"、"利润表"
    row_dim_id      BIGINT NOT NULL REFERENCES dimensions(id),  -- 行维度ID，外键关联dimensions表
    col_dim_id      BIGINT NOT NULL REFERENCES dimensions(id),  -- 列维度ID，外键关联dimensions表
    description     TEXT,                                  -- 表单描述，说明表单的用途和内容
    created_at      TIMESTAMP DEFAULT NOW(),                  -- 创建时间
    updated_at      TIMESTAMP DEFAULT NOW()                   -- 最后更新时间
);

-- 添加表注释
COMMENT ON TABLE sheets IS '表单表：存储网格表单的基本信息，定义表单的名称、行列维度和用途';

-- 添加字段注释
COMMENT ON COLUMN sheets.id IS '表单主键，自增ID，唯一标识一个表单';
COMMENT ON COLUMN sheets.name IS '表单名称，如"销售表"、"成本表"，全局唯一，用于引用';
COMMENT ON COLUMN sheets.row_dim_id IS '行维度ID，外键关联dimensions表，指定表单使用哪个维度作为行维度（如时间维度）';
COMMENT ON COLUMN sheets.col_dim_id IS '列维度ID，外键关联dimensions表，指定表单使用哪个维度作为列维度（如产品维度）';
COMMENT ON COLUMN sheets.description IS '表单描述，说明表单的用途、内容和业务含义';
COMMENT ON COLUMN sheets.created_at IS '创建时间，记录表单何时创建';
COMMENT ON COLUMN sheets.updated_at IS '最后更新时间，记录表单何时修改';
```

### 2.5 单元格表（cells）

```sql
-- 单元格表：存储表单的所有单元格数据
-- 作用：
--   - 存储每个单元格的值（数值、文本等）
--   - 维护单元格的伪坐标标识
--   - 支持POV（查询条件）配置
--   - 作为公式的基础数据源
--   - 支持批量更新和缓存
CREATE TABLE cells (
    id                  BIGSERIAL PRIMARY KEY,                  -- 单元格主键，自增ID
    sheet_id            BIGINT NOT NULL REFERENCES sheets(id),    -- 所属表单ID，外键关联sheets表
    row_member_id       BIGINT NOT NULL REFERENCES dimension_members(id),  -- 行成员ID，外键关联dimension_members表
    col_member_id       BIGINT NOT NULL REFERENCES dimension_members(id),  -- 列成员ID，外键关联dimension_members表
    pseudo_coord        VARCHAR(100) NOT NULL,                  -- 伪坐标，格式[行成员ID]_[列成员ID]，如[1]_[5]
    value               NUMERIC,                                 -- 单元格值，存储数值型数据
    pov                 JSONB,                                  -- POV（查询条件），JSON格式，如{"time":"2024Q1","product":"A"}
    created_at          TIMESTAMP DEFAULT NOW(),                  -- 创建时间
    updated_at          TIMESTAMP DEFAULT NOW(),                   -- 最后更新时间,
    UNIQUE(sheet_id, row_member_id, col_member_id),          -- 同一表单内行列组合唯一
    UNIQUE(sheet_id, pseudo_coord)                          -- 同一表单内伪坐标唯一
);

-- 添加表注释
COMMENT ON TABLE cells IS '单元格表：存储表单的所有单元格数据，包括值、伪坐标和POV条件，是公式计算的基础数据源';

-- 添加字段注释
COMMENT ON COLUMN cells.id IS '单元格主键，自增ID，唯一标识一个单元格';
COMMENT ON COLUMN cells.sheet_id IS '所属表单ID，外键关联sheets表，表示单元格属于哪个表单';
COMMENT ON COLUMN cells.row_member_id IS '行成员ID，外键关联dimension_members表，指定单元格所在的行（如2024Q1）';
COMMENT ON COLUMN cells.col_member_id IS '列成员ID，外键关联dimension_members表，指定单元格所在的列（如产品A）';
COMMENT ON COLUMN cells.pseudo_coord IS '伪坐标，格式[行成员ID]_[列成员ID]，如[1]_[5]，用于数据库快速查找';
COMMENT ON COLUMN cells.value IS '单元格值，存储数值型数据，如1000、200等，用于公式计算';
COMMENT ON COLUMN cells.pov IS 'POV（查询条件），JSON格式，如{"time":"2024Q1","product":"A"}，用于数据查询时的过滤条件';
COMMENT ON COLUMN cells.created_at IS '创建时间，记录单元格何时创建';
COMMENT ON COLUMN cells.updated_at IS '最后更新时间，记录单元格值何时修改';
COMMENT ON COLUMN cells.sheet_id IS '约束1：同一表单内行列组合不能重复（一个坐标只能有一个单元格）';
COMMENT ON COLUMN cells.sheet_id IS '约束2：同一表单内伪坐标不能重复';
```

### 2.6 公式表（formulas）

```sql
-- 公式表：存储单元格的公式定义
-- 作用：
--   - 存储单元格的公式表达式（如=B2+C4）
--   - 区分公式类型（单元格公式、计算字段等）
--   - 控制公式的激活状态
--   - 作为DAG构建的基础
--   - 支持公式的动态解析和执行
CREATE TABLE formulas (
    id                  BIGSERIAL PRIMARY KEY,                  -- 公式主键，自增ID
    cell_id             BIGINT NOT NULL REFERENCES cells(id) ON DELETE CASCADE,  -- 所属单元格ID，外键关联cells表
    expression          TEXT NOT NULL,                           -- 公式表达式，如=B2+C4, =G9+表A!D7，支持Excel坐标
    formula_type        VARCHAR(20) NOT NULL DEFAULT 'CELL',        -- 公式类型，CELL（单元格公式）或CALCULATED（计算字段）
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,              -- 激活状态，控制公式是否参与计算
    created_at          TIMESTAMP DEFAULT NOW(),                  -- 创建时间
    updated_at          TIMESTAMP DEFAULT NOW(),                   -- 最后更新时间,
    UNIQUE(cell_id)                                      -- 同一单元格只能有一个公式
);

-- 添加表注释
COMMENT ON TABLE formulas IS '公式表：存储单元格的公式定义，包括公式表达式、类型和激活状态，是DAG构建和公式执行的基础';

-- 添加字段注释
COMMENT ON COLUMN formulas.id IS '公式主键，自增ID，唯一标识一个公式';
COMMENT ON COLUMN formulas.cell_id IS '所属单元格ID，外键关联cells表，表示公式属于哪个单元格';
COMMENT ON COLUMN formulas.expression IS '公式表达式，如=B2+C4, =G9+表A!D7，支持Excel坐标和跨表引用';
COMMENT ON COLUMN formulas.formula_type IS '公式类型，CELL表示单元格公式，CALCULATED表示计算字段，影响执行策略';
COMMENT ON COLUMN formulas.is_active IS '激活状态，TRUE表示公式参与计算，FALSE表示不参与（可临时禁用公式）';
COMMENT ON COLUMN formulas.created_at IS '创建时间，记录公式何时创建';
COMMENT ON COLUMN formulas.updated_at IS '最后更新时间，记录公式何时修改';
COMMENT ON COLUMN formulas.cell_id IS '约束：同一单元格只能有一个公式，避免公式冲突';
COMMENT ON COLUMN formulas.cell_id IS '约束：级联删除，删除单元格时自动删除相关公式';
```

### 2.7 DAG边表（dag_edges）

```sql
-- DAG边表：存储公式之间的依赖关系
-- 作用：
--   - 构建公式DAG（有向无环图）
--   - 存储同表依赖和跨表依赖
--   - 支持拓扑排序和环路检测
--   - 作为剪支和执行的基础
--   - 维护完整的依赖关系图
CREATE TABLE dag_edges (
    id                  BIGSERIAL PRIMARY KEY,                  -- DAG边主键，自增ID
    formula_id          BIGINT NOT NULL REFERENCES formulas(id) ON DELETE CASCADE,  -- 源公式ID，外键关联formulas表
    dep_cell_id         BIGINT NOT NULL REFERENCES cells(id),  -- 依赖的单元格ID，外键关联cells表
    dep_type            VARCHAR(20) NOT NULL,                  -- 依赖类型，INTRA（同表）或CROSS（跨表）
    cross_sheet_id      BIGINT REFERENCES sheets(id),          -- 跨表依赖的目标表单ID（仅跨表依赖时有值）
    cross_cell_id       BIGINT REFERENCES cells(id),          -- 跨表依赖的目标单元格ID（仅跨表依赖时有值）
    cross_pov           JSONB,                                  -- 跨表依赖的POV条件（仅跨表依赖时有值）
    created_at          TIMESTAMP DEFAULT NOW(),                  -- 创建时间,
    UNIQUE(formula_id, dep_cell_id)                       -- 同一公式对同一单元格的依赖关系唯一
);

-- 添加表注释
COMMENT ON TABLE dag_edges IS 'DAG边表：存储公式之间的依赖关系，构建DAG（有向无环图），支持拓扑排序、环路检测和剪支';

-- 添加字段注释
COMMENT ON COLUMN dag_edges.id IS 'DAG边主键，自增ID，唯一标识一个依赖关系';
COMMENT ON COLUMN dag_edges.formula_id IS '源公式ID，外键关联formulas表，表示哪个公式有这个依赖';
COMMENT ON COLUMN dag_edges.dep_cell_id IS '依赖的单元格ID，外键关联cells表，表示公式依赖哪个单元格';
COMMENT ON COLUMN dag_edges.dep_type IS '依赖类型，INTRA表示同表依赖，CROSS表示跨表依赖，影响查询策略';
COMMENT ON COLUMN dag_edges.cross_sheet_id IS '跨表依赖的目标表单ID（仅跨表依赖时有值），外键关联sheets表，表示依赖的跨表表单';
COMMENT ON COLUMN dag_edges.cross_cell_id IS '跨表依赖的目标单元格ID（仅跨表依赖时有值），外键关联cells表，表示依赖的跨表单元格';
COMMENT ON COLUMN dag_edges.cross_pov IS '跨表依赖的POV条件（仅跨表依赖时有值），JSON格式，表示跨表查询时的过滤条件';
COMMENT ON COLUMN dag_edges.created_at IS '创建时间，记录依赖关系何时建立';
COMMENT ON COLUMN dag_edges.formula_id IS '约束：同一公式对同一单元格的依赖关系唯一，避免重复记录';
COMMENT ON COLUMN dag_edges.formula_id IS '约束：级联删除，删除公式时自动删除相关依赖关系';
```

### 2.8 DAG反向索引表（dag_backrefs）

```sql
-- DAG反向索引表：存储单元格被依赖的关系（反向索引）
-- 作用：
--   - 快速查找谁依赖某个单元格（O(1)查找）
--   - 支持剪支时的受影响公式收集
--   - 避免递归查询，提升性能
--   - 维护从依赖者到被依赖者的反向关系
--   - 是剪支算法的核心表
CREATE TABLE dag_backrefs (
    id                  BIGSERIAL PRIMARY KEY,                  -- 反向索引主键，自增ID
    source_formula_id   BIGINT NOT NULL REFERENCES formulas(id) ON DELETE CASCADE,  -- 源公式ID，外键关联formulas表
    target_cell_id      BIGINT NOT NULL REFERENCES cells(id) ON DELETE CASCADE,  -- 目标单元格ID，外键关联cells表
    target_sheet_id     BIGINT NOT NULL REFERENCES sheets(id),  -- 目标单元格所属表单ID，外键关联sheets表
    created_at          TIMESTAMP DEFAULT NOW(),                  -- 创建时间,
    UNIQUE(source_formula_id, target_cell_id)               -- 同一公式对同一单元格的反向关系唯一
);

-- 添加表注释
COMMENT ON TABLE dag_backrefs IS 'DAG反向索引表：存储单元格被依赖的关系，实现O(1)查找，是剪支算法的核心表';

-- 添加字段注释
COMMENT ON COLUMN dag_backrefs.id IS '反向索引主键，自增ID，唯一标识一个反向依赖关系';
COMMENT ON COLUMN dag_backrefs.source_formula_id IS '源公式ID，外键关联formulas表，表示哪个公式依赖目标单元格';
COMMENT ON COLUMN dag_backrefs.target_cell_id IS '目标单元格ID，外键关联cells表，表示哪个单元格被依赖';
COMMENT ON COLUMN dag_backrefs.target_sheet_id IS '目标单元格所属表单ID，外键关联sheets表，表示目标单元格在哪个表单';
COMMENT ON COLUMN dag_backrefs.created_at IS '创建时间，记录反向依赖关系何时建立';
COMMENT ON COLUMN dag_backrefs.source_formula_id IS '约束：同一公式对同一单元格的反向关系唯一，避免重复记录';
COMMENT ON COLUMN dag_backrefs.source_formula_id IS '约束：级联删除，删除公式时自动删除相关反向索引';
COMMENT ON COLUMN dag_backrefs.target_cell_id IS '约束：级联删除，删除单元格时自动删除相关反向索引';
```

### 2.9 DAG结构表（dag_structures）

```sql
-- DAG结构表：存储公式的完整DAG结构（JSON格式）
-- 作用：
--   - 持久化DAG结构，一次构建多次查询
--   - 支持可视化依赖树和嵌套关系
--   - 提供DAG统计信息（深度、节点数等）
--   - 支持DAG快照和版本管理
--   - 避免每次都重建DAG，提升性能
CREATE TABLE dag_structures (
    id                      BIGSERIAL PRIMARY KEY,                  -- DAG结构主键，自增ID
    formula_id              BIGINT NOT NULL REFERENCES formulas(id) ON DELETE CASCADE,  -- 公式ID，外键关联formulas表
    cell_pseudo_coord       VARCHAR(100) NOT NULL,                  -- 公式单元格的伪坐标，用于快速查找
    dag_structure           JSONB NOT NULL,                           -- DAG结构（JSON格式），包含完整的嵌套依赖关系
    structure_hash          VARCHAR(64) NOT NULL,                      -- DAG结构哈希，用于变更检测
    max_depth              INT NOT NULL,                               -- DAG最大深度，表示依赖链的最长路径
    node_count             INT NOT NULL,                               -- DAG节点数量，表示公式的依赖总数
    is_cross_sheet         BOOLEAN NOT NULL DEFAULT FALSE,              -- 是否跨表，TRUE表示依赖包含跨表公式
    created_at             TIMESTAMP DEFAULT NOW(),                  -- 创建时间
    updated_at             TIMESTAMP DEFAULT NOW(),                   -- 最后更新时间,
    UNIQUE(formula_id),                                  -- 同一公式只能有一个DAG结构
    UNIQUE(cell_pseudo_coord)                              -- 同一伪坐标只能有一个DAG结构
);

-- 添加表注释
COMMENT ON TABLE dag_structures IS 'DAG结构表：存储公式的完整DAG结构（JSON格式），支持依赖可视化、快照管理和性能优化';

-- 添加字段注释
COMMENT ON COLUMN dag_structures.id IS 'DAG结构主键，自增ID，唯一标识一个DAG结构';
COMMENT ON COLUMN dag_structures.formula_id IS '公式ID，外键关联formulas表，表示这个DAG结构属于哪个公式';
COMMENT ON COLUMN dag_structures.cell_pseudo_coord IS '公式单元格的伪坐标，用于快速查找，如[1]_[5]';
COMMENT ON COLUMN dag_structures.dag_structure IS 'DAG结构（JSON格式），包含完整的嵌套依赖关系，如{"node":"B2","dependencies":[...]}';
COMMENT ON COLUMN dag_structures.structure_hash IS 'DAG结构哈希，用于变更检测，避免重复保存未变化的DAG';
COMMENT ON COLUMN dag_structures.max_depth IS 'DAG最大深度，表示依赖链的最长路径，用于性能评估和优化';
COMMENT ON COLUMN dag_structures.node_count IS 'DAG节点数量，表示公式的依赖总数，包括嵌套依赖';
COMMENT ON COLUMN dag_structures.is_cross_sheet IS '是否跨表，TRUE表示依赖包含跨表公式，影响查询策略';
COMMENT ON COLUMN dag_structures.created_at IS '创建时间，记录DAG结构何时创建';
COMMENT ON COLUMN dag_structures.updated_at IS '最后更新时间，记录DAG结构何时修改';
COMMENT ON COLUMN dag_structures.formula_id IS '约束：同一公式只能有一个DAG结构，避免重复';
COMMENT ON COLUMN dag_structures.formula_id IS '约束：级联删除，删除公式时自动删除相关DAG结构';
```

### 2.10 DAG变更日志表（dag_change_log）

```sql
-- DAG变更日志表：记录DAG结构的变更历史
-- 作用：
--   - 记录DAG结构的所有变更（创建、更新、删除）
--   - 支持审计追踪和问题排查
--   - 提供变更历史查询
--   - 支持版本对比和回滚
CREATE TABLE dag_change_log (
    id                      BIGSERIAL PRIMARY KEY,                  -- 变更日志主键，自增ID
    formula_id              BIGINT NOT NULL REFERENCES formulas(id),  -- 公式ID，外键关联formulas表
    cell_pseudo_coord       VARCHAR(100) NOT NULL,                  -- 公式单元格的伪坐标
    old_structure           JSONB,                                  -- 旧DAG结构（创建时为NULL）
    new_structure           JSONB NOT NULL,                           -- 新DAG结构
    change_type            VARCHAR(20) NOT NULL,                      -- 变更类型，CREATE（创建）、UPDATE（更新）、DELETE（删除）
    old_hash               VARCHAR(64),                             -- 旧DAG结构哈希（创建时为NULL）
    new_hash               VARCHAR(64) NOT NULL,                      -- 新DAG结构哈希
    changed_by             VARCHAR(100),                             -- 操作人，记录谁执行的变更
    change_reason          TEXT,                                  -- 变更原因，说明变更的业务原因
    created_at             TIMESTAMP DEFAULT NOW()                   -- 变更时间
);

-- 添加表注释
COMMENT ON TABLE dag_change_log IS 'DAG变更日志表：记录DAG结构的变更历史，支持审计追踪、问题排查和版本对比';

-- 添加字段注释
COMMENT ON COLUMN dag_change_log.id IS '变更日志主键，自增ID，唯一标识一次DAG变更';
COMMENT ON COLUMN dag_change_log.formula_id IS '公式ID，外键关联formulas表，表示哪个公式的DAG发生了变更';
COMMENT ON COLUMN dag_change_log.cell_pseudo_coord IS '公式单元格的伪坐标，用于快速查找';
COMMENT ON COLUMN dag_change_log.old_structure IS '旧DAG结构（JSON格式），创建时为NULL，更新时记录变更前的结构';
COMMENT ON COLUMN dag_change_log.new_structure IS '新DAG结构（JSON格式），记录变更后的结构';
COMMENT ON COLUMN dag_change_log.change_type IS '变更类型，CREATE表示创建新DAG，UPDATE表示更新现有DAG，DELETE表示删除DAG';
COMMENT ON COLUMN dag_change_log.old_hash IS '旧DAG结构哈希（创建时为NULL），用于快速比较结构是否变化';
COMMENT ON COLUMN dag_change_log.new_hash IS '新DAG结构哈希，用于快速比较结构是否变化';
COMMENT ON COLUMN dag_change_log.changed_by IS '操作人，记录谁执行的变更，用于审计';
COMMENT ON COLUMN dag_change_log.change_reason IS '变更原因，说明变更的业务原因，如"添加新依赖"、"修改公式"';
COMMENT ON COLUMN dag_change_log.created_at IS '变更时间，记录变更何时发生';
```

### 2.11 DAG快照表（dag_snapshots）

```sql
-- DAG快照表：定期保存DAG快照，用于版本管理和回滚
-- 作用：
--   - 定期保存表单的所有DAG结构快照
--   - 支持快照恢复，回滚到历史版本
--   - 提供DAG版本管理
--   - 支持周期性备份
--   - 用于灾难恢复和测试环境
CREATE TABLE dag_snapshots (
    id                      BIGSERIAL PRIMARY KEY,                  -- 快照主键，自增ID
    snapshot_id            UUID NOT NULL UNIQUE,                     -- 快照ID，UUID格式，唯一标识一个快照
    sheet_id               BIGINT NOT NULL REFERENCES sheets(id),    -- 表单ID，外键关联sheets表
    snapshot_data          JSONB NOT NULL,                           -- 快照数据（JSON格式），包含表单的所有DAG结构
    snapshot_time          TIMESTAMP NOT NULL DEFAULT NOW(),       -- 快照时间，记录快照何时创建
    description            TEXT,                                  -- 快照描述，说明快照的用途，如"季度末备份"
    created_at             TIMESTAMP DEFAULT NOW()                   -- 创建时间
);

-- 添加表注释
COMMENT ON TABLE dag_snapshots IS 'DAG快照表：定期保存DAG快照，支持版本管理、回滚和周期性备份';

-- 添加字段注释
COMMENT ON COLUMN dag_snapshots.id IS '快照主键，自增ID，唯一标识一个快照';
COMMENT ON COLUMN dag_snapshots.snapshot_id IS '快照ID，UUID格式，唯一标识一个快照，用于快照恢复';
COMMENT ON COLUMN dag_snapshots.sheet_id IS '表单ID，外键关联sheets表，表示快照属于哪个表单';
COMMENT ON COLUMN dag_snapshots.snapshot_data IS '快照数据（JSON格式），包含表单的所有DAG结构，支持批量恢复';
COMMENT ON COLUMN dag_snapshots.snapshot_time IS '快照时间，记录快照何时创建';
COMMENT ON COLUMN dag_snapshots.description IS '快照描述，说明快照的用途，如"季度末备份"、"测试前快照"';
COMMENT ON COLUMN dag_snapshots.created_at IS '创建时间，记录快照元数据何时创建';
```

### 2.12 剪支配置表（pruning_config）

```sql
-- 剪支配置表：存储每个公式的剪支策略
-- 作用：
--   - 配置公式的剪支策略（阈值、优先级等）
--   - 控制剪支行为
--   - 支持动态调整剪支参数
--   - 提供个性化剪支策略
CREATE TABLE pruning_config (
    id                  BIGSERIAL PRIMARY KEY,                  -- 剪支配置主键，自增ID
    formula_id          BIGINT NOT NULL REFERENCES formulas(id) ON DELETE CASCADE,  -- 公式ID，外键关联formulas表
    prune_enabled       BOOLEAN NOT NULL DEFAULT TRUE,              -- 是否启用剪支，TRUE表示启用剪支，FALSE表示禁用
    change_threshold    NUMERIC(10, 6),                           -- 变化阈值，0-1之间的值，如0.05表示5%变化率
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',      -- 优先级，HIGH（高）、MEDIUM（中）、LOW（低），影响剪支决策
    cache_enabled       BOOLEAN NOT NULL DEFAULT FALSE,             -- 是否启用缓存，TRUE表示启用结果缓存
    cache_ttl_seconds   INT,                                      -- 缓存存活时间（秒），控制缓存有效时长
    created_at          TIMESTAMP DEFAULT NOW(),                  -- 创建时间
    updated_at          TIMESTAMP DEFAULT NOW(),                   -- 最后更新时间,
    UNIQUE(formula_id)                                   -- 同一公式只能有一个剪支配置
);

-- 添加表注释
COMMENT ON TABLE pruning_config IS '剪支配置表：存储每个公式的剪支策略，包括阈值、优先级、缓存等配置，控制剪支行为';

-- 添加字段注释
COMMENT ON COLUMN pruning_config.id IS '剪支配置主键，自增ID，唯一标识一个剪支配置';
COMMENT ON COLUMN pruning_config.formula_id IS '公式ID，外键关联formulas表，表示这个配置属于哪个公式';
COMMENT ON COLUMN pruning_config.prune_enabled IS '是否启用剪支，TRUE表示启用剪支，FALSE表示禁用（强制执行）';
COMMENT ON COLUMN pruning_config.change_threshold IS '变化阈值，0-1之间的值，如0.05表示5%变化率，低于此值的公式将被剪支';
COMMENT ON COLUMN pruning_config.priority IS '优先级，HIGH（高）、MEDIUM（中）、LOW（低），高优先级公式优先执行，资源受限时低优先级可能被剪支';
COMMENT ON COLUMN pruning_config.cache_enabled IS '是否启用缓存，TRUE表示启用结果缓存，避免重复计算';
COMMENT ON COLUMN pruning_config.cache_ttl_seconds IS '缓存存活时间（秒），控制缓存有效时长，如3600表示缓存1小时';
COMMENT ON COLUMN pruning_config.created_at IS '创建时间，记录剪支配置何时创建';
COMMENT ON COLUMN pruning_config.updated_at IS '最后更新时间，记录剪支配置何时修改';
COMMENT ON COLUMN pruning_config.formula_id IS '约束：同一公式只能有一个剪支配置，避免配置冲突';
COMMENT ON COLUMN pruning_config.formula_id IS '约束：级联删除，删除公式时自动删除相关剪支配置';
```

### 2.13 执行日志表（execution_log）

```sql
-- 执行日志表：记录公式执行过程和结果
-- 作用：
--   - 记录公式的执行过程（执行或剪支）
--   - 记录值的变化情况
--   - 记录执行时间，用于性能分析
--   - 支持审计追踪和问题排查
--   - 提供执行统计和报表
CREATE TABLE execution_log (
    id                  BIGSERIAL PRIMARY KEY,                  -- 执行日志主键，自增ID
    execution_id        UUID NOT NULL,                             -- 执行批次ID，UUID格式，标识一次完整的执行过程
    formula_id          BIGINT NOT NULL REFERENCES formulas(id),  -- 公式ID，外键关联formulas表
    cell_pseudo_coord   VARCHAR(100) NOT NULL,                  -- 单元格伪坐标，用于快速查找
    action              VARCHAR(20) NOT NULL,                      -- 执行动作，EXECUTED（执行）或PRUNED（剪支）
    prune_reason        VARCHAR(100),                             -- 剪支原因，如"变化低于阈值"、"不在关注范围内"
    value_before        NUMERIC,                                 -- 执行前的值（剪支时等于执行后的值）
    value_after         NUMERIC,                                  -- 执行后的值（剪支时等于执行前的值）
    execution_time_ms   INT,                                      -- 执行时间（毫秒），记录公式执行耗时
    created_at          TIMESTAMP DEFAULT NOW()                   -- 记录时间
);

-- 添加表注释
COMMENT ON TABLE execution_log IS '执行日志表：记录公式执行过程和结果，包括执行/剪支、值变化、执行时间等，支持审计追踪和性能分析';

-- 添加字段注释
COMMENT ON COLUMN execution_log.id IS '执行日志主键，自增ID，唯一标识一次执行记录';
COMMENT ON COLUMN execution_log.execution_id IS '执行批次ID，UUID格式，标识一次完整的执行过程，所有相关的执行记录共享此ID';
COMMENT ON COLUMN execution_log.formula_id IS '公式ID，外键关联formulas表，表示哪个公式被执行或剪支';
COMMENT ON COLUMN execution_log.cell_pseudo_coord IS '单元格伪坐标，用于快速查找和展示';
COMMENT ON COLUMN execution_log.action IS '执行动作，EXECUTED表示公式被执行，PRUNED表示公式被剪支';
COMMENT ON COLUMN execution_log.prune_reason IS '剪支原因，如"变化低于阈值"、"不在关注范围内"、"缓存命中"，用于分析剪支决策';
COMMENT ON COLUMN execution_log.value_before IS '执行前的值，记录公式执行前的单元格值，剪支时等于执行后的值';
COMMENT ON COLUMN execution_log.value_after IS '执行后的值，记录公式执行后的单元格值，剪支时等于执行前的值';
COMMENT ON COLUMN execution_log.execution_time_ms IS '执行时间（毫秒），记录公式执行耗时，用于性能分析和优化';
COMMENT ON COLUMN execution_log.created_at IS '记录时间，记录执行日志何时创建';
```

---

## 3. 索引与约束注释

### 3.1 核心索引

```sql
-- Excel坐标映射表索引
CREATE INDEX idx_excel_pseudo_excel_coord ON excel_to_pseudo_mapping(sheet_id, excel_coord);
CREATE INDEX idx_excel_pseudo_pseudo_coord ON excel_to_pseudo_mapping(sheet_id, pseudo_coord);

-- 单元格表索引
CREATE INDEX idx_cells_pseudo ON cells(sheet_id, pseudo_coord);
CREATE INDEX idx_cells_row_col ON cells(sheet_id, row_member_id, col_member_id);

-- 公式表索引
CREATE INDEX idx_formulas_cell ON formulas(cell_id);
CREATE INDEX idx_formulas_active ON formulas(is_active);

-- DAG边表索引
CREATE INDEX idx_dag_edges_formula ON dag_edges(formula_id);
CREATE INDEX idx_dag_edges_dep_cell ON dag_edges(dep_cell_id);
CREATE INDEX idx_dag_edges_type ON dag_edges(dep_type);
CREATE INDEX idx_dag_edges_cross ON dag_edges(cross_sheet_id, cross_cell_id) WHERE dep_type = 'CROSS';

-- DAG反向索引表索引（核心性能优化）
CREATE INDEX idx_dag_backrefs_target ON dag_backrefs(target_cell_id);
CREATE INDEX idx_dag_backrefs_sheet ON dag_backrefs(target_sheet_id);
CREATE INDEX idx_dag_backrefs_target_sheet ON dag_backrefs(target_cell_id, target_sheet_id);

-- DAG结构表索引
CREATE INDEX idx_dag_structures_formula ON dag_structures(formula_id);
CREATE INDEX idx_dag_structures_coord ON dag_structures(cell_pseudo_coord);
CREATE INDEX idx_dag_structures_hash ON dag_structures(structure_hash);

-- 执行日志表索引
CREATE INDEX idx_exec_log_id ON execution_log(execution_id);
CREATE INDEX idx_exec_log_formula ON execution_log(formula_id);
CREATE INDEX idx_exec_log_created ON execution_log(created_at);

-- 添加索引注释
COMMENT ON INDEX idx_excel_pseudo_excel_coord IS 'Excel坐标索引：通过Excel坐标快速查找伪坐标';
COMMENT ON INDEX idx_excel_pseudo_pseudo_coord IS '伪坐标索引：通过伪坐标快速查找Excel坐标';
COMMENT ON INDEX idx_cells_pseudo IS '单元格伪坐标索引：通过表单和伪坐标快速查找单元格';
COMMENT ON INDEX idx_cells_row_col IS '单元格行列索引：通过表单和行列成员ID快速查找单元格';
COMMENT ON INDEX idx_formulas_cell IS '公式单元格索引：通过单元格ID快速查找公式';
COMMENT ON INDEX idx_formulas_active IS '公式激活索引：快速查找激活或禁用的公式';
COMMENT ON INDEX idx_dag_edges_formula IS 'DAG边公式索引：通过公式ID快速查找依赖关系';
COMMENT ON INDEX idx_dag_edges_dep_cell IS 'DAG边依赖索引：通过依赖单元格ID反向查找公式';
COMMENT ON INDEX idx_dag_edges_type IS 'DAG边类型索引：快速查找同表或跨表依赖';
COMMENT ON INDEX idx_dag_edges_cross IS 'DAG边跨表索引：快速查找跨表依赖关系';
COMMENT ON INDEX idx_dag_backrefs_target IS 'DAG反向索引（核心）：通过单元格ID快速查找依赖者，O(1)查找';
COMMENT ON INDEX idx_dag_backrefs_sheet IS 'DAG反向索引表单索引：通过表单ID批量查找反向关系';
COMMENT ON INDEX idx_dag_backrefs_target_sheet IS 'DAG反向索引复合索引：通过单元格ID和表单ID快速查找跨表反向关系';
COMMENT ON INDEX idx_dag_structures_formula IS 'DAG结构公式索引：通过公式ID快速查找DAG结构';
COMMENT ON INDEX idx_dag_structures_coord IS 'DAG结构坐标索引：通过伪坐标快速查找DAG结构';
COMMENT ON INDEX idx_dag_structures_hash IS 'DAG结构哈希索引：通过结构哈希快速查找，用于变更检测';
COMMENT ON INDEX idx_exec_log_id IS '执行日志批次索引：通过执行批次ID快速查找同批次的所有执行记录';
COMMENT ON INDEX idx_exec_log_formula IS '执行日志公式索引：通过公式ID快速查找执行历史';
COMMENT ON INDEX idx_exec_log_created IS '执行日志时间索引：通过创建时间快速查找最近执行记录';
```

---

## 4. 外键约束注释

### 4.1 核心外键

```sql
-- 维度成员表 → 维度表
ALTER TABLE dimension_members
ADD CONSTRAINT fk_dim_members_dim_id
FOREIGN KEY (dimension_id) REFERENCES dimensions(id)
ON DELETE CASCADE;

-- Excel坐标映射表 → 表单表
ALTER TABLE excel_to_pseudo_mapping
ADD CONSTRAINT fk_excel_pseudo_sheet_id
FOREIGN KEY (sheet_id) REFERENCES sheets(id)
ON DELETE CASCADE;

-- 单元格表 → 表单表
ALTER TABLE cells
ADD CONSTRAINT fk_cells_sheet_id
FOREIGN KEY (sheet_id) REFERENCES sheets(id)
ON DELETE CASCADE;

-- 单元格表 → 维度成员表（行）
ALTER TABLE cells
ADD CONSTRAINT fk_cells_row_member_id
FOREIGN KEY (row_member_id) REFERENCES dimension_members(id)
ON DELETE CASCADE;

-- 单元格表 → 维度成员表（列）
ALTER TABLE cells
ADD CONSTRAINT fk_cells_col_member_id
FOREIGN KEY (col_member_id) REFERENCES dimension_members(id)
ON DELETE CASCADE;

-- 公式表 → 单元格表
ALTER TABLE formulas
ADD CONSTRAINT fk_formulas_cell_id
FOREIGN KEY (cell_id) REFERENCES cells(id)
ON DELETE CASCADE;

-- DAG边表 → 公式表
ALTER TABLE dag_edges
ADD CONSTRAINT fk_dag_edges_formula_id
FOREIGN KEY (formula_id) REFERENCES formulas(id)
ON DELETE CASCADE;

-- DAG边表 → 单元格表
ALTER TABLE dag_edges
ADD CONSTRAINT fk_dag_edges_dep_cell_id
FOREIGN KEY (dep_cell_id) REFERENCES cells(id)
ON DELETE CASCADE;

-- DAG边表 → 表单表（跨表）
ALTER TABLE dag_edges
ADD CONSTRAINT fk_dag_edges_cross_sheet_id
FOREIGN KEY (cross_sheet_id) REFERENCES sheets(id)
ON DELETE CASCADE;

-- DAG边表 → 单元格表（跨表）
ALTER TABLE dag_edges
ADD CONSTRAINT fk_dag_edges_cross_cell_id
FOREIGN KEY (cross_cell_id) REFERENCES cells(id)
ON DELETE CASCADE;

-- DAG反向索引表 → 公式表
ALTER TABLE dag_backrefs
ADD CONSTRAINT fk_dag_backrefs_source_formula_id
FOREIGN KEY (source_formula_id) REFERENCES formulas(id)
ON DELETE CASCADE;

-- DAG反向索引表 → 单元格表
ALTER TABLE dag_backrefs
ADD CONSTRAINT fk_dag_backrefs_target_cell_id
FOREIGN KEY (target_cell_id) REFERENCES cells(id)
ON DELETE CASCADE;

-- DAG反向索引表 → 表单表
ALTER TABLE dag_backrefs
ADD CONSTRAINT fk_dag_backrefs_target_sheet_id
FOREIGN KEY (target_sheet_id) REFERENCES sheets(id)
ON DELETE CASCADE;

-- DAG结构表 → 公式表
ALTER TABLE dag_structures
ADD CONSTRAINT fk_dag_structures_formula_id
FOREIGN KEY (formula_id) REFERENCES formulas(id)
ON DELETE CASCADE;

-- 剪支配置表 → 公式表
ALTER TABLE pruning_config
ADD CONSTRAINT fk_pruning_config_formula_id
FOREIGN KEY (formula_id) REFERENCES formulas(id)
ON DELETE CASCADE;

-- 执行日志表 → 公式表
ALTER TABLE execution_log
ADD CONSTRAINT fk_exec_log_formula_id
FOREIGN KEY (formula_id) REFERENCES formulas(id);

-- 添加外键约束注释
COMMENT ON CONSTRAINT fk_dim_members_dim_id ON dimension_members IS '外键约束：维度成员所属维度，删除维度时级联删除所有成员';
COMMENT ON CONSTRAINT fk_excel_pseudo_sheet_id ON excel_to_pseudo_mapping IS '外键约束：坐标映射所属表单，删除表单时级联删除所有映射';
COMMENT ON CONSTRAINT fk_cells_sheet_id ON cells IS '外键约束：单元格所属表单，删除表单时级联删除所有单元格';
COMMENT ON CONSTRAINT fk_cells_row_member_id ON cells IS '外键约束：单元格行维度成员，删除成员时级联删除相关单元格';
COMMENT ON CONSTRAINT fk_cells_col_member_id ON cells IS '外键约束：单元格列维度成员，删除成员时级联删除相关单元格';
COMMENT ON CONSTRAINT fk_formulas_cell_id ON formulas IS '外键约束：公式所属单元格，删除单元格时级联删除公式';
COMMENT ON CONSTRAINT fk_dag_edges_formula_id ON dag_edges IS '外键约束：DAG边所属公式，删除公式时级联删除所有依赖关系';
COMMENT ON CONSTRAINT fk_dag_edges_dep_cell_id ON dag_edges IS '外键约束：DAG边依赖的单元格，删除单元格时级联删除相关依赖关系';
COMMENT ON CONSTRAINT fk_dag_edges_cross_sheet_id ON dag_edges IS '外键约束：DAG边跨表目标表单，删除表单时级联删除相关依赖关系';
COMMENT ON CONSTRAINT fk_dag_edges_cross_cell_id ON dag_edges IS '外键约束：DAG边跨表目标单元格，删除单元格时级联删除相关依赖关系';
COMMENT ON CONSTRAINT fk_dag_backrefs_source_formula_id ON dag_backrefs IS '外键约束：反向索引源公式，删除公式时级联删除所有反向索引';
COMMENT ON CONSTRAINT fk_dag_backrefs_target_cell_id ON dag_backrefs IS '外键约束：反向索引目标单元格，删除单元格时级联删除所有反向索引';
COMMENT ON CONSTRAINT fk_dag_backrefs_target_sheet_id ON dag_backrefs IS '外键约束：反向索引目标表单，删除表单时级联删除所有反向索引';
COMMENT ON CONSTRAINT fk_dag_structures_formula_id ON dag_structures IS '外键约束：DAG结构所属公式，删除公式时级联删除所有DAG结构';
COMMENT ON CONSTRAINT fk_pruning_config_formula_id ON pruning_config IS '外键约束：剪支配置所属公式，删除公式时级联删除剪支配置';
COMMENT ON CONSTRAINT fk_exec_log_formula_id ON execution_log IS '外键约束：执行日志关联公式，删除公式时保留日志（用于审计）';
```

---

## 5. 使用示例

### 5.1 查询表注释

```sql
-- 查询所有表的注释
SELECT
    schemaname as schema_name,
    tablename as table_name,
    obj_description as table_comment
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- 查询指定表的注释
SELECT
    pg_description.obj_description as table_comment
FROM pg_description
JOIN pg_class ON pg_description.objoid = pg_class.oid
JOIN pg_namespace ON pg_class.relnamespace = pg_namespace.oid
WHERE pg_namespace.nspname = 'public'
  AND pg_class.relname = 'cells';
```

### 5.2 查询字段注释

```sql
-- 查询指定表的所有字段注释
SELECT
    a.attname as column_name,
    pg_catalog.format_type(a.atttypid, a.atttypmod) as data_type,
    pg_catalog.col_description(a.attrelid, a.attnum) as column_comment
FROM pg_catalog.pg_attribute a
JOIN pg_catalog.pg_class c ON a.attrelid = c.oid
JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
WHERE n.nspname = 'public'
  AND c.relname = 'cells'
  AND a.attnum > 0
  AND NOT a.attisdropped
ORDER BY a.attnum;
```

---

## 6. 总结

### 6.1 表结构概览

| 表名 | 数量 | 主要作用 |
|------|------|---------|
| `dimensions` | 1 | 维度定义 |
| `dimension_members` | 1 | 维度成员 |
| `excel_to_pseudo_mapping` | 1 | Excel坐标映射 |
| `sheets` | 1 | 表单定义 |
| `cells` | 1 | 单元格数据 |
| `formulas` | 1 | 公式定义 |
| `dag_edges` | 1 | DAG依赖关系 |
| `dag_backrefs` | 1 | DAG反向索引（核心） |
| `dag_structures` | 1 | DAG结构持久化 |
| `dag_change_log` | 1 | DAG变更日志 |
| `dag_snapshots` | 1 | DAG快照管理 |
| `pruning_config` | 1 | 剪支配置 |
| `execution_log` | 1 | 执行日志 |

### 6.2 核心表关系

```
dimensions (维度）
  └── dimension_members (维度成员）
  └── sheets (表单，使用行列维度）
        └── cells (单元格）
              ├── formulas (公式）
              ├── dag_edges (DAG边）
              ├── dag_backrefs (DAG反向索引，核心）
              └── dag_structures (DAG结构）

execution_log (执行日志，关联formulas）
pruning_config (剪支配置，关联formulas）
dag_change_log (变更日志，关联formulas）
dag_snapshots (快照，关联sheets）
```

### 6.3 性能优化要点

- ✅ **反向索引**：`dag_backrefs` 表实现O(1)查找依赖者
- ✅ **复合索引**：多个复合索引支持复杂查询
- ✅ **GIN索引**：JSONB字段支持JSON查询
- ✅ **级联删除**：自动清理相关数据
- ✅ **唯一约束**：保证数据一致性

### 6.4 数据完整性

- ✅ **外键约束**：保证关系完整性
- ✅ **唯一约束**：避免重复数据
- ✅ **非空约束**：保证关键字段必填
- ✅ **级联删除**：自动维护关系

这个文档提供了完整的表结构和详细注释，便于理解数据库设计和维护！
