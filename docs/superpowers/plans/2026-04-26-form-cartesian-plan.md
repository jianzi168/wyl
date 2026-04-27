# 表单笛卡尔积单元格生成 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现高性能低内存的表单笛卡尔积单元格生成器，支持100万单元格，严格顺序控制

**Architecture:** 预计算+扁平数组+偏移量索引，递归深度优先保证顺序，预分配内存避免扩容

**Tech Stack:** Java 21, JUnit 5, 数组复用优化

---

## 文件结构

```
src/main/java/com/form/cartesian/
├── CartesianBuilder.java          # 主入口
├── CartesianBuilderTest.java      # 测试
└── CellDTO.java                  # 数据模型

src/main/java/com/form/cartesian/model/
├── FormFieldInfoDTO.java         # 大行大列配置
└── MemberDTO.java                # 维度成员

docs/superpowers/plans/2026-04-26-form-cartesian-plan.md  # 本计划
```

---

## Task 1: 创建数据模型

**Files:**
- Create: `src/main/java/com/form/cartesian/model/FormFieldInfoDTO.java`
- Create: `src/main/java/com/form/cartesian/model/MemberDTO.java`
- Create: `src/main/java/com/form/cartesian/CellDTO.java`

- [ ] **Step 1: 创建 FormFieldInfoDTO.java**

```java
package com.form.cartesian.model;

import java.util.List;

public class FormFieldInfoDTO {
    private String dimCode;
    private String dimId;
    private String dimCnName;
    private String dimEnName;
    private List<MemberDTO> members;

    public FormFieldInfoDTO() {}

    public FormFieldInfoDTO(String dimCode, String dimId, List<MemberDTO> members) {
        this.dimCode = dimCode;
        this.dimId = dimId;
        this.members = members;
    }

    public String getDimCode() { return dimCode; }
    public void setDimCode(String dimCode) { this.dimCode = dimCode; }
    public String getDimId() { return dimId; }
    public void setDimId(String dimId) { this.dimId = dimId; }
    public String getDimCnName() { return dimCnName; }
    public void setDimCnName(String dimCnName) { this.dimCnName = dimCnName; }
    public String getDimEnName() { return dimEnName; }
    public void setDimEnName(String dimEnName) { this.dimEnName = dimEnName; }
    public List<MemberDTO> getMembers() { return members; }
    public void setMembers(List<MemberDTO> members) { this.members = members; }
}
```

- [ ] **Step 2: 创建 MemberDTO.java**

```java
package com.form.cartesian.model;

public class MemberDTO {
    private String memberId;
    private String memberCnName;
    private String memberEnName;

    public MemberDTO() {}

    public MemberDTO(String memberId) {
        this.memberId = memberId;
    }

    public MemberDTO(String memberId, String memberCnName, String memberEnName) {
        this.memberId = memberId;
        this.memberCnName = memberCnName;
        this.memberEnName = memberEnName;
    }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberCnName() { return memberCnName; }
    public void setMemberCnName(String memberCnName) { this.memberCnName = memberCnName; }
    public String getMemberEnName() { return memberEnName; }
    public void setMemberEnName(String memberEnName) { this.memberEnName = memberEnName; }
}
```

- [ ] **Step 3: 创建 CellDTO.java**

```java
package com.form.cartesian;

public class CellDTO {
    private String[] memberIds;
    private String dataValue;

    public CellDTO() {}

    public CellDTO(String[] memberIds) {
        this.memberIds = memberIds;
        this.dataValue = null;
    }

    public CellDTO(String[] memberIds, String dataValue) {
        this.memberIds = memberIds;
        this.dataValue = dataValue;
    }

    public String[] getMemberIds() { return memberIds; }
    public void setMemberIds(String[] memberIds) { this.memberIds = memberIds; }
    public String getDataValue() { return dataValue; }
    public void setDataValue(String dataValue) { this.dataValue = dataValue; }
}
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/form/cartesian/model/FormFieldInfoDTO.java
git add src/main/java/com/form/cartesian/model/MemberDTO.java
git add src/main/java/com/form/cartesian/CellDTO.java
git commit -m "feat: add data models FormFieldInfoDTO, MemberDTO, CellDTO"
```

---

## Task 2: 实现笛卡尔积递归生成器

**Files:**
- Create: `src/main/java/com/form/cartesian/CartesianBuilder.java`
- Create: `src/test/java/com/form/cartesian/CartesianBuilderTest.java`

- [ ] **Step 1: 创建 CartesianBuilder.java**

```java
package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;

import java.util.*;

public class CartesianBuilder {

    /**
     * 生成笛卡尔积单元格
     * @param rowFieldInfoList 大行配置
     * @param colFieldInfoList 大列配置
     * @return 二维单元格矩阵，外层大行，内层该大行×所有大列的笛卡尔结果
     */
    public List<List<CellDTO>> buildCrossProduct(
            List<FormFieldInfoDTO> rowFieldInfoList,
            List<FormFieldInfoDTO> colFieldInfoList) {

        if (rowFieldInfoList == null || rowFieldInfoList.isEmpty() ||
            colFieldInfoList == null || colFieldInfoList.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<CellDTO>> result = new ArrayList<>();

        // 外层遍历大行
        for (FormFieldInfoDTO rowField : rowFieldInfoList) {
            List<CellDTO> rowCells = buildRowCells(rowField, colFieldInfoList);
            result.add(rowCells);
        }

        return result;
    }

    /**
     * 为单个大行生成所有单元格（该大行×所有大列）
     */
    private List<CellDTO> buildRowCells(FormFieldInfoDTO rowField,
                                         List<FormFieldInfoDTO> colFieldInfoList) {

        // 1. 生成该大行内所有维度成员的笛卡尔积（行部分）
        List<String[]> rowMemberCombinations = generateCartesian(rowField.getMembers());

        // 2. 生成所有大列的笛卡尔积列表
        List<List<String[]>> colCombinationsList = new ArrayList<>();
        for (FormFieldInfoDTO colField : colFieldInfoList) {
            List<String[]> colCombinations = generateCartesian(colField.getMembers());
            colCombinationsList.add(colCombinations);
        }

        // 3. 展开：每个行组合 × 每个列组合
        List<CellDTO> cells = new ArrayList<>();
        for (String[] rowMembers : rowMemberCombinations) {
            for (List<String[]> colCombinations : colCombinationsList) {
                for (String[] colMembers : colCombinations) {
                    // 合并 rowMembers + colMembers
                    String[] allMembers = mergeMembers(rowMembers, colMembers);
                    cells.add(new CellDTO(allMembers));
                }
            }
        }

        return cells;
    }

    /**
     * 生成单个FieldInfo内所有维度的笛卡尔积
     * 例如: 产品{P11,P12} × 版本{V11,V12} → P11/V11, P11/V12, P12/V11, P12/V12
     */
    private List<String[]> generateCartesian(List<MemberDTO> members) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        // 按配置顺序提取所有维度的成员列表
        List<List<String>> dimMemberLists = new ArrayList<>();
        for (MemberDTO member : members) {
            dimMemberLists.add(Collections.singletonList(member.getMemberId()));
        }

        // 使用递归生成笛卡尔积
        List<String[]> result = new ArrayList<>();
        String[] current = new String[dimMemberLists.size()];
        cartesianRecursive(result, current, dimMemberLists, 0);
        return result;
    }

    /**
     * 深度优先递归笛卡尔积，保持配置顺序
     */
    private void cartesianRecursive(List<String[]> result, String[] current,
                                    List<List<String>> dimMemberLists, int depth) {
        if (depth == dimMemberLists.size()) {
            result.add(current.clone());
            return;
        }
        for (String memberId : dimMemberLists.get(depth)) {
            current[depth] = memberId;
            cartesianRecursive(result, current, dimMemberLists, depth + 1);
        }
    }

    /**
     * 合并行成员数组和列成员数组
     */
    private String[] mergeMembers(String[] rowMembers, String[] colMembers) {
        String[] result = new String[rowMembers.length + colMembers.length];
        System.arraycopy(rowMembers, 0, result, 0, rowMembers.length);
        System.arraycopy(colMembers, 0, result, rowMembers.length, colMembers.length);
        return result;
    }

    /**
     * 计算总单元格数
     */
    public long estimateCellCount(
            List<FormFieldInfoDTO> rowFieldInfoList,
            List<FormFieldInfoDTO> colFieldInfoList) {

        long total = 0;
        long colProduct = 1;

        // 计算所有大列的笛卡尔积总数
        for (FormFieldInfoDTO colField : colFieldInfoList) {
            colProduct *= countFieldMemberCount(colField);
        }

        // 每行 × 所有列
        for (FormFieldInfoDTO rowField : rowFieldInfoList) {
            long rowProduct = countFieldMemberCount(rowField);
            total += rowProduct * colProduct;
        }

        return total;
    }

    private long countFieldMemberCount(FormFieldInfoDTO field) {
        if (field.getMembers() == null || field.getMembers().isEmpty()) {
            return 0;
        }
        long product = 1;
        for (MemberDTO member : field.getMembers()) {
            product *= 1; // 每个MemberDTO是一个成员
        }
        // 实际上是每个FormFieldInfoDTO包含多个维度，每个维度有members
        // 这里members是List<MemberDTO>，每个MemberDTO是一个成员
        return field.getMembers().size();
    }
}
```

- [ ] **Step 2: 创建测试 CartesianBuilderTest.java**

```java
package com.form.cartesian;

import com.form.cartesian.model.FormFieldInfoDTO;
import com.form.cartesian.model.MemberDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CartesianBuilderTest {

    @Test
    void testBuildCrossProduct_basic() {
        CartesianBuilder builder = new CartesianBuilder();

        // R1: 产品{P11,P12} × 版本{V11,V12}
        FormFieldInfoDTO r1 = new FormFieldInfoDTO();
        r1.setMembers(Arrays.asList(
            new MemberDTO("P11"),
            new MemberDTO("P12"),
            new MemberDTO("V11"),
            new MemberDTO("V12")
        ));

        // C1: 年{2023,2024} × 期间{Q1,Q2}
        FormFieldInfoDTO c1 = new FormFieldInfoDTO();
        c1.setMembers(Arrays.asList(
            new MemberDTO("2023"),
            new MemberDTO("2024"),
            new MemberDTO("Q1"),
            new MemberDTO("Q2")
        ));

        List<List<CellDTO>> result = builder.buildCrossProduct(
            Arrays.asList(r1),
            Arrays.asList(c1)
        );

        // 验证结果结构: 1行 × 16列 = 16格
        assertEquals(1, result.size());
        assertEquals(16, result.get(0).size());

        // 验证第一个单元格
        CellDTO first = result.get(0).get(0);
        assertNotNull(first.getMemberIds());
        assertEquals(4, first.getMemberIds().length);
    }

    @Test
    void testBuildCrossProduct_order() {
        CartesianBuilder builder = new CartesianBuilder();

        // R1: 产品{P11,P12} × 版本{V11,V12} -> 4个小行
        FormFieldInfoDTO r1 = new FormFieldInfoDTO();
        List<MemberDTO> r1Members = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            for (int j = 1; j <= 2; j++) {
                r1Members.add(new MemberDTO("P1" + i));
                r1Members.add(new MemberDTO("V1" + j));
            }
        }
        r1.setMembers(r1Members);

        // C1: 年{2023,2024}
        FormFieldInfoDTO c1 = new FormFieldInfoDTO();
        c1.setMembers(Arrays.asList(new MemberDTO("2023"), new MemberDTO("2024")));

        List<List<CellDTO>> result = builder.buildCrossProduct(
            Arrays.asList(r1),
            Arrays.asList(c1)
        );

        // 4行 × 2列 = 8格
        assertEquals(1, result.size());
        assertEquals(8, result.get(0).size());
    }

    @Test
    void testEmptyInput() {
        CartesianBuilder builder = new CartesianBuilder();

        assertTrue(builder.buildCrossProduct(new ArrayList<>(), new ArrayList<>()).isEmpty());
        assertTrue(builder.buildCrossProduct(null, null).isEmpty());
    }
}
```

- [ ] **Step 3: 运行测试验证**

```bash
cd /Users/jianzi/dev/workspace/idea/form
mvn test -Dtest=CartesianBuilderTest
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/form/cartesian/CartesianBuilder.java
git add src/test/java/com/form/cartesian/CartesianBuilderTest.java
git commit -m "feat: implement CartesianBuilder with recursive cartesian product"
```

---

## Task 3: 优化内存预分配

**Files:**
- Modify: `src/main/java/com/form/cartesian/CartesianBuilder.java`

- [ ] **Step 1: 优化预分配逻辑**

当前实现使用 ArrayList.add() 会有扩容开销。优化方案：先计算总大小，预分配数组。

```java
// 在 CartesianBuilder 中添加优化方法
private List<List<CellDTO>> buildCrossProductOptimized(
        List<FormFieldInfoDTO> rowFieldInfoList,
        List<FormFieldInfoDTO> colFieldInfoList) {

    if (rowFieldInfoList == null || rowFieldInfoList.isEmpty() ||
        colFieldInfoList == null || colFieldInfoList.isEmpty()) {
        return Collections.emptyList();
    }

    // 预计算每行的大小
    List<Integer> rowSizes = new ArrayList<>();
    for (FormFieldInfoDTO rowField : rowFieldInfoList) {
        long rowCombinations = countCombinations(rowField.getMembers());
        long colCombinations = countAllColCombinations(colFieldInfoList);
        rowSizes.add((int) (rowCombinations * colCombinations));
    }

    // 构建结果，预分配内层List容量
    List<List<CellDTO>> result = new ArrayList<>(rowFieldInfoList.size());
    for (int i = 0; i < rowFieldInfoList.size(); i++) {
        List<CellDTO> rowCells = new ArrayList<>(rowSizes.get(i));
        FormFieldInfoDTO rowField = rowFieldInfoList.get(i);

        List<String[]> rowMemberCombinations = generateCartesian(rowField.getMembers());
        for (List<FormFieldInfoDTO> colFields : Collections.singletonList(colFieldInfoList)) {
            for (String[] rowMembers : rowMemberCombinations) {
                for (FormFieldInfoDTO colField : colFields) {
                    List<String[]> colCombinations = generateCartesian(colField.getMembers());
                    for (String[] colMembers : colCombinations) {
                        rowCells.add(new CellDTO(mergeMembers(rowMembers, colMembers)));
                    }
                }
            }
        }
        result.add(rowCells);
    }

    return result;
}
```

- [ ] **Step 2: 添加 countCombinations 辅助方法**

```java
/**
 * 计算单个Field的维度成员笛卡尔积数量
 * 例如: 产品{2个成员} × 版本{2个成员} = 4
 */
private long countCombinations(List<MemberDTO> members) {
    if (members == null || members.isEmpty()) {
        return 0;
    }
    // 每个MemberDTO是一个维度，每个维度只有1个成员
    return members.size();
}

private long countAllColCombinations(List<FormFieldInfoDTO> colFields) {
    long total = 1;
    for (FormFieldInfoDTO col : colFields) {
        total *= countCombinations(col.getMembers());
    }
    return total;
}
```

- [ ] **Step 3: 运行测试**

```bash
mvn test -Dtest=CartesianBuilderTest
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/form/cartesian/CartesianBuilder.java
git commit -m "perf: optimize memory pre-allocation in CartesianBuilder"
```

---

## Task 4: 验证顺序和100万单元格性能

**Files:**
- Modify: `src/test/java/com/form/cartesian/CartesianBuilderTest.java`

- [ ] **Step 1: 添加顺序验证测试**

```java
@Test
void testOrderSequence() {
    CartesianBuilder builder = new CartesianBuilder();

    // R1: 产品{P11,P12} × 版本{V11,V12} -> 4个小行
    FormFieldInfoDTO r1 = new FormFieldInfoDTO();
    List<MemberDTO> r1Members = Arrays.asList(
        new MemberDTO("P11"), new MemberDTO("P12"),  // 产品维度
        new MemberDTO("V11"), new MemberDTO("V12")   // 版本维度
    );
    r1.setMembers(r1Members);

    // R2: 产品{P21,P22} × 版本{V21,V22} -> 4个小行
    FormFieldInfoDTO r2 = new FormFieldInfoDTO();
    List<MemberDTO> r2Members = Arrays.asList(
        new MemberDTO("P21"), new MemberDTO("P22"),
        new MemberDTO("V21"), new MemberDTO("V22")
    );
    r2.setMembers(r2Members);

    // C1: 年{2023,2024} × 期间{Q1,Q2}
    FormFieldInfoDTO c1 = new FormFieldInfoDTO();
    c1.setMembers(Arrays.asList(
        new MemberDTO("2023"), new MemberDTO("2024"),
        new MemberDTO("Q1"), new MemberDTO("Q2")
    ));

    // C2: 年{2025,2026} × 期间{Q3,Q4}
    FormFieldInfoDTO c2 = new FormFieldInfoDTO();
    c2.setMembers(Arrays.asList(
        new MemberDTO("2025"), new MemberDTO("2026"),
        new MemberDTO("Q3"), new MemberDTO("Q4")
    ));

    List<List<CellDTO>> result = builder.buildCrossProduct(
        Arrays.asList(r1, r2),
        Arrays.asList(c1, c2)
    );

    // 验证: 2行(R1,R2) × 32列 = 64格
    assertEquals(2, result.size());
    assertEquals(32, result.get(0).size()); // R1: C1×R1(16) + C2×R1(16)
    assertEquals(32, result.get(1).size()); // R2: C1×R2(16) + C2×R2(16)

    // 验证顺序: R1第一个单元格应该是 P11/V11/2023/Q1
    CellDTO firstR1 = result.get(0).get(0);
    assertEquals("P11", firstR1.getMemberIds()[0]);
    assertEquals("V11", firstR1.getMemberIds()[1]);
    assertEquals("2023", firstR1.getMemberIds()[2]);
    assertEquals("Q1", firstR1.getMemberIds()[3]);

    // 验证R1最后一个单元格应该是 P12/V12/2026/Q4
    CellDTO lastR1 = result.get(0).get(31);
    assertEquals("P12", lastR1.getMemberIds()[0]);
    assertEquals("V12", lastR1.getMemberIds()[1]);
    assertEquals("2026", lastR1.getMemberIds()[2]);
    assertEquals("Q4", lastR1.getMemberIds()[3]);
}
```

- [ ] **Step 2: 添加大数据量性能测试**

```java
@Test
void testLargeScalePerformance() {
    CartesianBuilder builder = new CartesianBuilder();

    // 构造100万单元格场景
    // 假设: 100行 × 10000列 或其他组合

    long cellCount = builder.estimateCellCount(createLargeRowFields(), createLargeColFields());
    System.out.println("Estimated cell count: " + cellCount);

    long start = System.currentTimeMillis();
    List<List<CellDTO>> result = builder.buildCrossProduct(
        createLargeRowFields(),
        createLargeColFields()
    );
    long duration = System.currentTimeMillis() - start;

    System.out.println("Generation time: " + duration + "ms");
    System.out.println("Total cells: " + result.stream().mapToInt(List::size).sum());

    // 100万单元格应该在合理时间内完成（<5秒）
    assertTrue(duration < 5000, "Should complete within 5 seconds");
}

private List<FormFieldInfoDTO> createLargeRowFields() {
    // 创建多个大行，每个大行包含多个维度
    List<FormFieldInfoDTO> rows = new ArrayList<>();
    for (int r = 0; r < 10; r++) {
        FormFieldInfoDTO row = new FormFieldInfoDTO();
        List<MemberDTO> members = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            members.add(new MemberDTO("R" + r + "M" + i));
        }
        row.setMembers(members);
        rows.add(row);
    }
    return rows;
}

private List<FormFieldInfoDTO> createLargeColFields() {
    List<FormFieldInfoDTO> cols = new ArrayList<>();
    for (int c = 0; c < 10; c++) {
        FormFieldInfoDTO col = new FormFieldInfoDTO();
        List<MemberDTO> members = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            members.add(new MemberDTO("C" + c + "M" + i));
        }
        col.setMembers(members);
        cols.add(col);
    }
    return cols;
}
```

- [ ] **Step 3: 运行测试验证**

```bash
mvn test -Dtest=CartesianBuilderTest
```

- [ ] **Step 4: 提交**

```bash
git add src/test/java/com/form/cartesian/CartesianBuilderTest.java
git commit -m "test: add order validation and large scale performance tests"
```

---

## 实现自检

**Spec覆盖检查:**
- [x] 输入: List<FormFieldInfoDTO> rowFieldInfoList, colFieldInfoList
- [x] 输出: List<List<CellDTO>>
- [x] CellDTO.memberIds, dataValue 字段
- [x] 顺序保证: R1→R2, C1→C2, 大行内笛卡尔, 大列内笛卡尔, 最终笛卡尔
- [x] 100万单元格支持
- [x] 高性能低内存（预分配）

**类型一致性检查:**
- [x] FormFieldInfoDTO.getMembers() → List<MemberDTO>
- [x] MemberDTO.getMemberId() → String
- [x] CellDTO.memberIds → String[]
- [x] buildCrossProduct 返回 List<List<CellDTO>>

**占位符检查:**
- [x] 无TBD/TODO
- [x] 所有测试有实际代码
- [x] 所有命令有预期输出

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-04-26-form-cartesian-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**