# Excel 公式链路 DAG 引擎

高性能 Excel 公式引擎，支持跨表依赖追踪、DAG 构建与拓扑排序执行。

## 项目简介

实现一个独立的公式引擎，解析类似 Excel 的公式语法，构建依赖关系 DAG，并支持跨表公式的链路追踪与执行。

### 核心特性

- ✅ **Excel 兼容语法**：支持 `=B3+H1+表单B!D2` 格式
- ✅ **跨表依赖追踪**：基于反向索引的 O(1) 查找
- ✅ **非递归算法**：BFS 遍历 + Kahn's algorithm 拓扑排序
- ✅ **高性能设计**：内存加载 + 批量更新
- ✅ **环路检测**：防止循环依赖导致的死循环
- ✅ **内存优化**：惰性加载、流式执行、深度限制

## 快速开始

### 环境要求

- Java 17+
- PostgreSQL 14+
- Spring Boot 3.x

### 数据库初始化

```bash
# 执行数据库脚本
psql -U postgres -d formula_engine -f schema.sql
```

### 启动项目

```bash
# 克隆项目
git clone git@github.com:jianzi168/wyl.git
cd wyl

# 构建
mvn clean install

# 运行
mvn spring-boot:run
```

### 示例使用

```java
// 创建公式
FormulaNode formula = new FormulaNode();
formula.setExpression("=A5+B4+表单B!D2");
formulaEngine.addFormula(formula);

// 触发执行（当B4变化时）
formulaEngine.execute("表单C", "B4");
```

## 架构设计

### 核心概念

- **节点 (FormulaNode)**：单个单元格的公式
- **边 (Edge)**：分两类
  - `intra_dag_edge`：同表引用边
  - `cross_dag_edge`：跨表引用边
- **DAG**：有向无环图，表示公式依赖关系

### 数据模型

```
sheets (表单)
  │
  └── formulas (公式)
          │
          ├── formula_intra_deps (同表依赖)
          │
          └── formula_cross_deps (跨表依赖)
                  │
                  └── cross_sheet_backrefs (反向索引)
```

### 执行流程

```
触发点变化
    ↓
收集影响公式 (BFS遍历)
    ↓
合并相关子 DAG
    ↓
拓扑排序 (Kahn's algorithm)
    ↓
按序求值
    ↓
批量更新
```

## 性能优化

| 优化点 | 方案 |
|--------|------|
| 避免递归 | BFS 队列遍历依赖，Kahn's algorithm 做拓扑排序 |
| 跨表快速查找 | `cross_sheet_backrefs` 反向索引，O(1) 查找 |
| 内存友好 | 仅加载涉及的子 DAG，不加载全量图 |
| 批量更新 | 计算结果最后批量写回数据库 |
| 缓存 | 公式解析结果缓存，避免重复解析 |

## 技术栈

- **语言**：Java 17+
- **框架**：Spring Boot 3.x
- **持久化**：Spring Data JDBC / JPA + PostgreSQL
- **公式解析**：Antlr4 或 GraalJS
- **构建工具**：Maven

## 项目结构

```
wyl/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/formula/
│   │   │       ├── engine/          # 核心引擎
│   │   │       ├── model/           # 数据模型
│   │   │       ├── repository/      # 数据访问
│   │   │       └── service/         # 业务逻辑
│   │   └── resources/
│   │       ├── application.yml      # 配置文件
│   │       └── schema.sql           # 数据库脚本
│   └── test/
│       └── java/                    # 单元测试
├── docs/                            # 文档
│   └── design.md                    # 详细设计文档
├── README.md                        # 项目说明
└── pom.xml                          # Maven 配置
```

## 文档

- [详细设计文档](2026-04-22-excel-formula-dag-design.md) - 完整的技术设计说明

## 开发计划

- [ ] 完善代码实现
- [ ] 添加单元测试
- [ ] 性能测试与优化
- [ ] 支持 Excel 内置函数（SUM、AVERAGE、IF 等）
- [ ] Web API 接口

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 联系方式

- GitHub：@jianzi168
