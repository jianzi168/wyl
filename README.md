# Cartesian Cell Builder

行列分组笛卡尔积单元格组装器 - Java 高性能实现

## 背景

表单配置中，行和列可以分别进行分组，每组内配置多个维度及其成员，最终需要按组进行行列笛卡尔组装，生成单元格。

```
行分组:
  R1: 维度A(a11,a12) × 维度B(b11,b12) → 4个行组合
  R2: 维度A(a21,a22) × 维度B(b21,b22) → 4个行组合

列分组:
  C1: 维度C(c11,c12) × 维度D(d11,d12) → 4个列组合
  C2: 维度C(c21,c22) × 维度D(d21,d22) → 4个列组合

组装结果:
  R1×C1: 4×4 = 16单元格
  R1×C2: 4×4 = 16单元格
  R2×C1: 4×4 = 16单元格
  R2×C2: 4×4 = 16单元格
  总计: 64单元格
```

## 性能优化策略

| 策略 | 说明 |
|------|------|
| Cell只存索引 | 每个Cell只存4个int，不存rowKeys/colKeys列表 |
| String[]替代List | DimensionGroup.members用数组，减少包装对象 |
| 预分配容量 | ArrayList精确预分配，避免动态扩容 |
| 迭代器模式 | 惰性计算，零物化 |
| 回调模式 | 完全零分配，适用于100万+单元格 |

## 快速开始

### 编译 & 运行

```bash
javac CartesianCellBuilder.java
java CartesianCellBuilder
```

### 三种 API 用法

#### 1. 标准构建（10万以内）

```java
CellResult result = CartesianCellBuilder.build(rowGroups, colGroups);

// 遍历
for (int ri = 0; ri < result.cells.size(); ri++) {
    for (int ci = 0; ci < result.cells.get(ri).size(); ci++) {
        for (Cell cell : result.cells.get(ri).get(ci)) {
            String[] rowKeys = result.ctx.resolveRowKeys(cell);
            String[] colKeys = result.ctx.resolveColKeys(cell);
            // 处理...
        }
    }
}
```

#### 2. 迭代器模式（10万~100万）

```java
CellContext ctx = new CellContext(rowGroups, colGroups);
for (Cell c : CartesianCellBuilder.iterator(rowGroups, colGroups)) {
    String[] rowKeys = ctx.resolveRowKeys(c);
    String[] colKeys = ctx.resolveColKeys(c);
    // 处理...
}
```

#### 3. 回调模式（100万以上）

```java
CartesianCellBuilder.forEachCell(rowGroups, colGroups, cell -> {
    // 完全零分配，极致性能
    process(cell.rowGroupIdx, cell.colGroupIdx, 
            cell.rowIdxInGroup, cell.colIdxInGroup);
});
```

## 核心数据结构

### DimensionGroup

```java
DimensionGroup {
    String name;      // 维度名称，如"地区"
    String[] members; // 成员数组，如{"北京","上海","广州"}
}
```

### Cell

```java
Cell {
    int rowGroupIdx;    // 所在行组下标
    int colGroupIdx;    // 所在列组下标
    int rowIdxInGroup;  // 组内行序号
    int colIdxInGroup;  // 组内列序号
}
```

## 内存占用参考

| 规模 | 推荐方案 | 内存占用 |
|------|---------|---------|
| <10万 | build() | ~30MB |
| 10万~100万 | iterator() | ~10MB |
| >100万 | forEachCell() | ~1MB |

## License

MIT
