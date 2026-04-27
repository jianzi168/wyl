# 表单笛卡尔积单元格生成设计

## 1. 需求概述

表单配置功能允许配置大行大列，大行/大列内可配置多个维度成员，维度成员之间做笛卡尔积展开。表单打开时需要按大行大列里边的维度成员进行笛卡尔组装单元格，支持100万单元格，要求高性能低内存。

## 2. 输入输出

### 输入
- `List<FormFieldInfoDTO> rowFieldInfoList` - 大行配置列表
- `List<FormFieldInfoDTO> colFieldInfoList` - 大列配置列表

### FormFieldInfoDTO 结构
```java
String dimCode;        // 维度编码
String dimId;          // 维度ID
String dimCnName;      // 维度中文名
String dimEnName;      // 维度英文名
List<MemberDTO> members; // 该维度下的成员列表
```

### MemberDTO 结构
```java
String memberId;       // 成员ID
String memberCnName;   // 成员中文名
String memberEnName;   // 成员英文名
```

### 输出
```java
List<List<CellDTO>>  // 外层大行，内层该大行×所有大列展开后的单元格
```

### CellDTO 结构
```java
String[] memberIds;    // 维度成员ID数组，row维度成员在前，col维度成员在后
String dataValue;     // 数据值
```

## 3. 展开规则

### 示例配置

**大行 (R)**:
- R1: 产品{P11,P12} × 版本{V11,V12} → 4个小行: P11/V11, P11/V12, P12/V11, P12/V12
- R2: 产品{P21,P22} × 版本{V21,V22} → 4个小行: P21/V21, P21/V22, P22/V21, P22/V22

**大列 (C)**:
- C1: 年{2023,2024} × 期间{Q1,Q2} → 4个小列: 2023/Q1, 2023/Q2, 2024/Q1, 2024/Q2
- C2: 年{2025,2026} × 期间{Q3,Q4} → 4个小列: 2025/Q3, 2025/Q4, 2026/Q3, 2026/Q4

### 单元格生成顺序

1. 大行遍历顺序: R1 → R2
2. 大列遍历顺序: C1 → C2
3. 每个大行内维度笛卡尔展开: 按配置顺序递归组合
4. 最终笛卡尔: (R1×C1) → (R1×C2) → (R2×C1) → (R2×C2)

### 输出结构示例

```
Row 0 (R1): [C1×R1的16格] + [C2×R1的16格] = 32格
Row 1 (R2): [C1×R2的16格] + [C2×R2的16格] = 32格
```

内层顺序: 先C1的16格(P11/V11×2023/Q1, P11/V11×2023/Q2...)，再C2的16格

## 4. 核心算法

### 笛卡尔积递归算法

```java
void cartesian(List<int[]> result, int[] current,
               List<MemberDTO>[] dimMembers, int depth) {
    if (depth == dimMembers.length) {
        result.add(current.clone());
        return;
    }
    for (MemberDTO m : dimMembers[depth]) {
        current[depth] = m.memberId;
        cartesian(result, current, dimMembers, depth + 1);
    }
}
```

关键点:
- 按配置顺序遍历，保证结果有序
- 深度优先遍历
- row成员在前，col成员在后合并到memberIds

### 性能优化

| 策略 | 说明 |
|------|------|
| 预计算总单元格数 | 先统计避免动态扩容 |
| 预分配数组容量 | estimatedSize = rowMembersCount × colMembersCount |
| 连续内存布局 | CellDTO[] 而非 List<List<CellDTO>> 内部优化 |
| ID映射表 | String memberId → int index，减少内存 |

### 内存估算

```
100万单元格 × 8维 × 4字节 = 32MB (memberIds)
CellDTO对象 ~16MB
总计 ~50MB
```

## 5. API设计

```java
public class FormCartesianBuilder {

    /**
     * 生成笛卡尔积单元格
     * @param rowFieldInfoList 大行配置
     * @param colFieldInfoList 大列配置
     * @return 二维单元格矩阵，外层大行，内层该大行×所有大列的笛卡尔结果
     */
    public List<List<CellDTO>> buildCrossProduct(
            List<FormFieldInfoDTO> rowFieldInfoList,
            List<FormFieldInfoDTO> colFieldInfoList);

    /**
     * 计算总单元格数（用于预分配内存）
     */
    public long estimateCellCount(
            List<FormFieldInfoDTO> rowFieldInfoList,
            List<FormFieldInfoDTO> colFieldInfoList);
}
```

## 6. 类结构

```
FormCartesianBuilder           // 主入口
├── buildCrossProduct()        // 核心方法
├── buildRowCartesian()        // 单个大行×所有大列的笛卡尔
├── mergeRowColMembers()       // 合并row/col成员到memberIds
└── estimateTotalSize()        // 预计算总大小

IdIndexMap                     // String→int 映射表
CartesianIterator              // 迭代器实现
CellDTO                         // 数据模型
FormFieldInfoDTO               // 输入模型(已有)
MemberDTO                      // 输入模型(已有)
```

## 7. 实现要点

1. **顺序保证**: 递归遍历时按配置顺序，不允许跨维度重排
2. **内存预分配**: 避免ArrayList动态扩容
3. **线程安全**: 多线程可并行处理不同大行
4. **接口兼容**: 返回List<List<CellDTO>>满足前端遍历需求