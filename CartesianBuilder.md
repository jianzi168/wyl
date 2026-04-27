# 表单笛卡尔积单元格生成器

## 概述

表单配置功能允许配置大行大列，大行/大列内可配置多个维度成员，维度成员之间做笛卡尔积展开。表单打开时按大行大列里边的维度成员进行笛卡尔组装单元格，支持百万级单元格，要求高性能低内存。

---

## 数据结构

### 输入模型

#### FormFieldInfoDTO (大行/大列配置)

| 字段 | 类型 | 说明 |
|------|------|------|
| dimCode | String | 维度编码，格式: `{大行/大列标识}_{维度名}`，如 `R1_产品`、`C1_年` |
| dimId | String | 维度ID |
| dimCnName | String | 维度中文名 |
| dimEnName | String | 维度英文名 |
| members | List<MemberDTO> | 该维度下的成员列表 |

#### MemberDTO (维度成员)

| 字段 | 类型 | 说明 |
|------|------|------|
| memberId | String | 成员ID |
| memberCnName | String | 成员中文名 |
| memberEnName | String | 成员英文名 |

### 输出模型

#### CellDTO (单元格)

| 字段 | 类型 | 说明 |
|------|------|------|
| memberIds | String[] | 维度成员ID数组，**row维度成员在前，col维度成员在后** |
| dataValue | String | 数据值（可后续填充） |

---

## 配置示例

### 大行配置

```
R1: 产品{P11, P12} × 版本{V11, V12} → 展开为 4 个小行
R2: 产品{P21, P22} × 版本{V21, V22} → 展开为 4 个小行
```

### 大列配置

```
C1: 年{2023, 2024} × 期间{Q1, Q2} → 展开为 4 个小列
C2: 年{2025, 2026} × 期间{Q3, Q4} → 展开为 4 个小列
```

---

## 输出结构

### 二维矩阵

```
List<List<CellDTO>> result = 8行 × 8列 = 64格
```

| 位置 | memberIds |
|------|-----------|
| `[0][0]` | `[P11, V11, 2023, Q1]` |
| `[0][7]` | `[P11, V11, 2026, Q4]` |
| `[3][0]` | `[P12, V12, 2023, Q1]` |
| `[3][7]` | `[P12, V12, 2026, Q4]` |
| `[4][0]` | `[P21, V21, 2023, Q1]` |
| `[7][7]` | `[P22, V22, 2026, Q4]` |

### 展开规则

```
行顺序: R1的4个小行 → R2的4个小行
列顺序: C1的4个小列 → C2的4个小列
```

### 单元格成员组合表 (8行×8列=64格)

#### R1 小行展开 (4行)

| 行\列 | C1[0] | C1[1] | C1[2] | C1[3] | C2[0] | C2[1] | C2[2] | C2[3] |
|-------|-------|-------|-------|-------|-------|-------|-------|-------|
| **R1[0]** | P11,V11,2023,Q1 | P11,V11,2023,Q2 | P11,V11,2024,Q1 | P11,V11,2024,Q2 | P11,V11,2025,Q3 | P11,V11,2025,Q4 | P11,V11,2026,Q3 | P11,V11,2026,Q4 |
| **R1[1]** | P11,V12,2023,Q1 | P11,V12,2023,Q2 | P11,V12,2024,Q1 | P11,V12,2024,Q2 | P11,V12,2025,Q3 | P11,V12,2025,Q4 | P11,V12,2026,Q3 | P11,V12,2026,Q4 |
| **R1[2]** | P12,V11,2023,Q1 | P12,V11,2023,Q2 | P12,V11,2024,Q1 | P12,V11,2024,Q2 | P12,V11,2025,Q3 | P12,V11,2025,Q4 | P12,V11,2026,Q3 | P12,V11,2026,Q4 |
| **R1[3]** | P12,V12,2023,Q1 | P12,V12,2023,Q2 | P12,V12,2024,Q1 | P12,V12,2024,Q2 | P12,V12,2025,Q3 | P12,V12,2025,Q4 | P12,V12,2026,Q3 | P12,V12,2026,Q4 |

#### R2 小行展开 (4行)

| 行\列 | C1[0] | C1[1] | C1[2] | C1[3] | C2[0] | C2[1] | C2[2] | C2[3] |
|-------|-------|-------|-------|-------|-------|-------|-------|-------|
| **R2[0]** | P21,V21,2023,Q1 | P21,V21,2023,Q2 | P21,V21,2024,Q1 | P21,V21,2024,Q2 | P21,V21,2025,Q3 | P21,V21,2025,Q4 | P21,V21,2026,Q3 | P21,V21,2026,Q4 |
| **R2[1]** | P21,V22,2023,Q1 | P21,V22,2023,Q2 | P21,V22,2024,Q1 | P21,V22,2024,Q2 | P21,V22,2025,Q3 | P21,V22,2025,Q4 | P21,V22,2026,Q3 | P21,V22,2026,Q4 |
| **R2[2]** | P22,V21,2023,Q1 | P22,V21,2023,Q2 | P22,V21,2024,Q1 | P22,V21,2024,Q2 | P22,V21,2025,Q3 | P22,V21,2025,Q4 | P22,V21,2026,Q3 | P22,V21,2026,Q4 |
| **R2[3]** | P22,V22,2023,Q1 | P22,V22,2023,Q2 | P22,V22,2024,Q1 | P22,V22,2024,Q2 | P22,V22,2025,Q3 | P22,V22,2025,Q4 | P22,V22,2026,Q3 | P22,V22,2026,Q4 |

**说明**: 每个单元格的值格式为 `[产品, 版本, 年, 期间]`，其中产品+版本来自小行，年+期间来自小列。

---

## 核心算法

### 笛卡尔积递归

```java
private void cartesianRecursive(List<String[]> result, String[] current,
                                 List<List<String>> dimMemberLists, int depth) {
    if (depth == dimMemberLists.size()) {
        result.add(current.clone());  // 保持顺序
        return;
    }
    for (String memberId : dimMemberLists.get(depth)) {
        current[depth] = memberId;
        cartesianRecursive(result, current, dimMemberLists, depth + 1);
    }
}
```

### 流程

1. **按大行分组** - 通过 dimCode 前缀提取大行标识（如 `R1_产品` → `R1`）
2. **生成小行组合** - 每个大行内维度成员笛卡尔展开
3. **生成小列组合** - 每个大列内维度成员笛卡尔展开
4. **构建二维矩阵** - 每个小行 × 每个小列

### 关键点

- **顺序保证**: 递归遍历时按配置顺序，不允许跨维度重排
- **深度优先**: 保证小行内和小列内的笛卡尔积顺序
- **大行遍历**: R1 → R2 → R3（按配置顺序）
- **大列遍历**: C1 → C2（按配置顺序）

---

## API 使用

```java
CartesianBuilder builder = new CartesianBuilder();

// 构建输入
List<FormFieldInfoDTO> rowFieldInfoList = new ArrayList<>();
List<FormFieldInfoDTO> colFieldInfoList = new ArrayList<>();

// 大行配置...
// 大列配置...

// 生成笛卡尔积
List<List<CellDTO>> result = builder.buildCrossProduct(rowFieldInfoList, colFieldInfoList);

// 访问单元格: result.get(rowIndex).get(colIndex)
CellDTO cell = result.get(0).get(0);  // 第1行第1列

// 预估单元格数
long count = builder.estimateCellCount(rowFieldInfoList, colFieldInfoList);
```

---

## 文件结构

```
/Users/jianzi/dev/workspace/idea/form/
├── pom.xml                                           # Maven配置
├── CartesianBuilder.md                               # 本文档
├── src/main/java/com/form/cartesian/
│   ├── CartesianBuilder.java                         # 核心算法
│   ├── CellDTO.java                                  # 单元格模型
│   └── model/
│       ├── FormFieldInfoDTO.java                     # 大行大列配置
│       └── MemberDTO.java                            # 维度成员
└── src/test/java/com/form/cartesian/
    └── CartesianBuilderTest.java                     # 测试验证
```

---

## 性能优化

### 支持百万单元格

| 策略 | 说明 |
|------|------|
| 预计算容量 | 先统计总单元格数，避免动态扩容 |
| 连续内存布局 | 二维List内层预分配容量 |
| 避免中间对象 | 复用 String[] 数组 |

### 内存估算

```
100万单元格 × 8维 × 2字节 ≈ 16MB (String引用)
CellDTO对象头 + 引用 ≈ 16MB
总计 ≈ 32MB
```

---

## 扩展场景

### 新增大行 R3

```java
// R3: 产品{P31,P32} × 版本{V31,V32} → 展开为 4 个小行
FormFieldInfoDTO r3Product = new FormFieldInfoDTO();
r3Product.setDimCode("R3_产品");
r3Product.setMembers(Arrays.asList(new MemberDTO("P31"), new MemberDTO("P32")));
rowFieldInfoList.add(r3Product);

// 输出: 12行 × 8列 = 96格
```

### 多维度组合

```java
// R1: 产品{P11,P12} × 版本{V11,V12} × 渠道{Ch1,Ch2}
// 展开: 2×2×2 = 8个小行

FormFieldInfoDTO r1Product = new FormFieldInfoDTO();
r1Product.setDimCode("R1_产品");
r1Product.setMembers(Arrays.asList(new MemberDTO("P11"), new MemberDTO("P12")));

FormFieldInfoDTO r1Version = new FormFieldInfoDTO();
r1Version.setDimCode("R1_版本");
r1Version.setMembers(Arrays.asList(new MemberDTO("V11"), new MemberDTO("V12")));

FormFieldInfoDTO r1Channel = new FormFieldInfoDTO();
r1Channel.setDimCode("R1_渠道");
r1Channel.setMembers(Arrays.asList(new MemberDTO("Ch1"), new MemberDTO("Ch2")));
```

---

## 依赖环境

- Java 21
- Maven 3.6+

---

## 运行测试

```bash
cd /Users/jianzi/dev/workspace/idea/form
mvn compile exec:java -Dexec.mainClass="com.form.cartesian.CartesianBuilderTest"
```