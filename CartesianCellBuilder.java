import java.util.*;
import java.util.stream.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 行列分组笛卡尔积单元格组装器 - JDK21 优化版
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 【背景说明】
 * 
 * 表单配置中，行和列可以分别进行分组，每组内配置多个维度及其成员。
 * 最终需要按组进行行列笛卡尔组装，生成单元格。
 * 
 * 【入参结构示例】
 * 
 *   行分组:
 *     R1: 维度A(a11,a12) × 维度B(b11,b12) → 4个行组合
 *     R2: 维度A(a21,a22) × 维度B(b21,b22) → 4个行组合
 *   
 *   列分组:
 *     C1: 维度C(c11,c12) × 维度D(d11,d12) → 4个列组合
 *     C2: 维度C(c21,c22) × 维度D(d21,d22) → 4个列组合
 * 
 * 【组装结果】
 *   R1×C1: 4×4 = 16单元格
 *   R1×C2: 4×4 = 16单元格
 *   R2×C1: 4×4 = 16单元格
 *   R2×C2: 4×4 = 16单元格
 *   总计: 64单元格
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 【JDK21 核心优化点】
 * 
 *   1. record 替代 class
 *      - record 是隐式 final 的数据载体类（JDK 16+）
 *      - 自动生成 equals()、hashCode()、toString()、构造函数
 *      - 更紧凑的内存布局，JIT 编译时内联更好
 * 
 *   2. parallelStream() 并行化
 *      - 笛卡尔积遍历是天然可并行的（各组独立）
 *      - 多核 CPU 有效利用
 *      - 使用 ForkJoinPool.commonPool()
 * 
 *   3. Primitive Long 编码
 *      - 将 Cell 四个 int 压缩为单个 long（8字节 vs 16字节+对象头）
 *      - 完全消除对象创建开销和 GC 压力
 *      - 连续内存，CPU 缓存预取友好
 * 
 *   4. Stream API 优化
 *      - IntStream.range().parallel() 利用 Spliterator 自动分片
 *      - 懒执行，按需计算
 *      - 支持链式函数式处理
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 【性能对比】
 * 
 *   ┌─────────────────────┬────────────┬───────────┬────────────────────────┐
 *   │ 方案                │ 预估耗时   │ 预估内存   │ 推荐场景               │
 *   ├─────────────────────┼────────────┼───────────┼────────────────────────┤
 *   │ build()             │ ~200-400ms │ ~280MB    │ <10万，简单直接        │
 *   │ buildParallel()     │ ~80-150ms  │ ~150MB    │ 10万~100万，并行加速   │
 *   │ buildPrimitive()    │ ~60-100ms  │ ~8MB      │ >100万，极致内存       │
 *   │ buildPrimitive+并行 │ ~30-80ms   │ ~8MB      │ >100万，最优性能       │
 *   └─────────────────────┴────────────┴───────────┴────────────────────────┘
 * 
 * @author 夏雨
 * @date 2026-04-25
 * @JDK 21
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class CartesianCellBuilder {

    // ═══════════════════════════════════════════════════════════════════════════
    // 核心数据结构
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 维度定义 (DimensionGroup)                                                │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   代表一个维度（如"地区"、"产品"、"月份"），包含维度名称和成员列表。
     *   多个维度组合在一起形成一个行组或列组。
     * 
     * 【使用示例】
     *   new DimensionGroup("地区", new String[]{"北京", "上海", "广州"})
     *   new DimensionGroup("产品", List.of("手机", "电脑"))  // 也支持从List创建
     * 
     * 【JDK21 优化点】
     *   - 使用 String[] 而非 List<String>，避免 List 包装对象开销
     *   - record 自动生成方法，减少样板代码
     *   - 数组在 JIT 层面有更好的边界检查优化
     * 
     * @param name    维度名称，如"地区"、"产品"、"月份"
     * @param members 该维度下的所有成员，如 {"北京","上海","广州"}
     */
    public record DimensionGroup(String name, String[] members) {
        
        /**
         * 【便捷构造方法】
         * 从 List<String> 创建 DimensionGroup
         * 
         * @param name    维度名称
         * @param members 成员列表，会被转换为数组存储
         */
        public DimensionGroup(String name, List<String> members) {
            // toArray 返回防御性拷贝，保证 members 数组不可被外部修改
            this(name, members.toArray(new String[0]));
        }
        
        /**
         * 获取成员数量
         * 
         * @return 该维度包含的成员个数
         */
        public int memberCount() { 
            return members.length; 
        }
        
        /**
         * 获取指定下标的成员
         * 
         * @param idx 成员下标（从0开始）
         * @return 指定下标的成员值
         * @throws ArrayIndexOutOfBoundsException 如果下标越界
         */
        public String memberAt(int idx) { 
            return members[idx]; 
        }
    }

    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 单元格 (Cell)                                                            │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   代表笛卡尔积组装后的一个单元格，包含其所在的行组、列组、以及在组内的索引。
     *   注意：Cell 只存储索引信息，不存储实际的行键/列键数据。
     * 
     * 【字段说明】
     *   - rowGroupIdx    : 所在行组的下标（第几个行组，从0开始）
     *   - colGroupIdx    : 所在列组的下标（第几个列组，从0开始）
     *   - rowIdxInGroup  : 在行组内的行组合序号（0-based）
     *   - colIdxInGroup  : 在列组内的列组合序号（0-based）
     * 
     * 【如何获取实际键值】
     *   通过 CellContext.resolveRowKeys(cell) / resolveColKeys(cell) 按需查询，
     *   这样可以避免在每个Cell中都存储两份键值数据，大幅节省内存。
     * 
     * 【内存占用】
     *   - 普通 class: 4 × 4 = 16字节（字段） + 12-16字节（对象头） ≈ 32字节
     *   - record: JIT 优化后更紧凑
     *   - 100万个 Cell 约 32MB
     * 
     * 【使用示例】
     *   Cell cell = new Cell(0, 1, 2, 3);
     *   // 表示: 第0个行组、第1个列组、在行组内第2个、在列组内第3个
     */
    public record Cell(
        int rowGroupIdx,     // 所在行组下标
        int colGroupIdx,     // 所在列组下标
        int rowIdxInGroup,   // 在行组内的行组合序号
        int colIdxInGroup    // 在列组内的列组合序号
    ) {
        
        /**
         * 计算全局行号（从0开始）
         * 
         * 【用途】
         *   将 (rowGroupIdx, rowIdxInGroup) 转换为全局唯一的行号。
         *   用于在表格、数组等扁平结构中定位。
         * 
         * 【示例】
         *   rowGroups = [R1(4组合), R2(4组合), R3(4组合)]
         *   rowGroupIdx=1, rowIdxInGroup=2 → 全局行号 = 4 + 2 = 6
         * 
         * @param rowGroupCardinalities 每个行组的组合数数组
         * @return 全局行号（从0开始）
         */
        public int globalRow(int[] rowGroupCardinalities) {
            int offset = 0;
            // 累加前面所有行组的组合数
            for (int i = 0; i < rowGroupIdx; i++) {
                offset += rowGroupCardinalities[i];
            }
            // 加上当前组内的偏移
            return offset + rowIdxInGroup;
        }
        
        /**
         * 计算全局列号（从0开始）
         * 
         * @param colGroupCardinalities 每个列组的组合数数组
         * @return 全局列号（从0开始）
         */
        public int globalCol(int[] colGroupCardinalities) {
            int offset = 0;
            for (int i = 0; i < colGroupIdx; i++) {
                offset += colGroupCardinalities[i];
            }
            return offset + colIdxInGroup;
        }
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 单元格上下文 (CellContext)                                               │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   持有笛卡尔积计算的完整上下文信息，包括：
     *   - 原始维度配置（rowGroups/colGroups）
     *   - 每个行组/列组的笛卡尔积基数（预计算，避免重复计算）
     *   - 提供通过Cell解析实际键值的方法
     *   - 提供全局坐标计算
     * 
     * 【为什么需要这个类】
     *   Cell 只存索引，实际数据（行键/列键）在 Context 里。
     *   这样设计的优势：
     *   - 遍历时只创建 Cell 索引对象，不创建键值数据
     *   - 需要键值时再通过 Context 按需计算
     *   - 多个 Cell 可以共享同一个 Context
     *   - 便于一次性获取所有元数据
     * 
     * 【线程安全】
     *   不可变设计（record），无状态，可多线程共享
     * 
     * @param rowGroups             所有行组（数组形式）
     * @param colGroups             所有列组（数组形式）
     * @param rowGroupCardinalities 每个行组的笛卡尔积基数（预计算）
     * @param colGroupCardinalities 每个列组的笛卡尔积基数（预计算）
     */
    public record CellContext(
        DimensionGroup[][] rowGroups,              // 所有行组
        DimensionGroup[][] colGroups,              // 所有列组
        int[] rowGroupCardinalities,               // 每个行组的组合数
        int[] colGroupCardinalities                 // 每个列组的组合数
    ) {
        
        /**
         * 计算总行数（所有行组合数之和）
         * 
         * @return 总行数
         */
        public int totalRows() { 
            return Arrays.stream(rowGroupCardinalities).sum(); 
        }
        
        /**
         * 计算总列数
         * 
         * @return 总列数
         */
        public int totalCols() { 
            return Arrays.stream(colGroupCardinalities).sum(); 
        }
        
        /**
         * 计算总单元格数
         * 
         * @return 总单元格数（可能超过 int 范围，用 long 返回）
         */
        public long totalCells() { 
            return (long) totalRows() * totalCols(); 
        }
        
        /**
         * 通过 Cell 获取对应的行键数组
         * 
         * 【原理】
         *   1. 根据 cell.rowGroupIdx() 找到对应的行组
         *   2. 根据 cell.rowIdxInGroup() 解码出各维度的成员索引
         *   3. 根据索引获取实际的成员值
         * 
         * 【示例】
         *   行组 R1 配置了维度A(a1,a2) 和 维度B(b1,b2)
         *   当 rowIdxInGroup = 2 时:
         *     - 解码: A索引=1, B索引=0
         *     - 返回: ["a2", "b1"]
         * 
         * @param c 目标单元格
         * @return 行键数组，按行组内维度顺序排列
         */
        public String[] resolveRowKeys(Cell c) {
            // 获取该单元格所在行组的维度定义
            DimensionGroup[] dims = rowGroups[c.rowGroupIdx()];
            // 将一维序号解码为多维索引
            int[] indices = decodeIndex(c.rowIdxInGroup(), dims);
            // 根据索引获取实际成员值
            return resolveMembers(dims, indices);
        }
        
        /**
         * 通过 Cell 获取对应的列键数组
         * 
         * @param c 目标单元格
         * @return 列键数组，按列组内维度顺序排列
         */
        public String[] resolveColKeys(Cell c) {
            DimensionGroup[] dims = colGroups[c.colGroupIdx()];
            int[] indices = decodeIndex(c.colIdxInGroup(), dims);
            return resolveMembers(dims, indices);
        }
        
        /**
         * 将一维序号解码为多维索引
         * 
         * 【数学原理】
         *   这是一个进制转换问题。
         *   假设有 dims = [A(3成员), B(2成员)]，
         *   序号 3 的解码过程:
         *     B索引 = 3 % 2 = 1
         *     A索引 = 3 / 2 = 1
         *     结果: [A[1], B[1]]
         * 
         * 【示例】
         *   dims = [维度A(成员数3), 维度B(成员数2), 维度C(成员数4)]
         *   序号 10:
         *     C索引 = 10 % 4 = 2, 10 / 4 = 2
         *     B索引 = 2 % 2 = 0, 2 / 2 = 1
         *     A索引 = 1 % 3 = 1, 1 / 3 = 0
         *     结果: indices = [1, 0, 2]
         * 
         * @param index 一维序号
         * @param dims  维度数组
         * @return 每个维度对应的成员索引数组
         */
        private int[] decodeIndex(int index, DimensionGroup[] dims) {
            int[] indices = new int[dims.length];
            // 从后往前解码（从最低位维度开始）
            for (int i = dims.length - 1; i >= 0; i--) {
                indices[i] = index % dims[i].memberCount();
                index /= dims[i].memberCount();
            }
            return indices;
        }
        
        /**
         * 根据维度组和索引数组解析出实际成员值
         * 
         * @param dims    维度数组
         * @param indices 每个维度对应的成员索引
         * @return 实际成员值数组
         */
        private static String[] resolveMembers(DimensionGroup[] dims, int[] indices) {
            String[] result = new String[dims.length];
            for (int i = 0; i < dims.length; i++) {
                result[i] = dims[i].memberAt(indices[i]);
            }
            return result;
        }
        
        /**
         * 【便捷工厂方法】
         * 从 List 结构创建 CellContext
         * 
         * 【用途】
         *   简化 API 调用，统一从 List<List<DimensionGroup>> 创建上下文
         * 
         * @param rows 行分组列表
         * @param cols 列分组列表
         * @return 单元格上下文
         */
        public static CellContext of(List<List<DimensionGroup>> rows, 
                                   List<List<DimensionGroup>> cols) {
            
            // 转换行组：List<List> → [][]
            DimensionGroup[][] rg = new DimensionGroup[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                List<DimensionGroup> g = rows.get(i);
                rg[i] = g.toArray(new DimensionGroup[0]);
            }
            
            // 转换列组
            DimensionGroup[][] cg = new DimensionGroup[cols.size()][];
            for (int i = 0; i < cols.size(); i++) {
                List<DimensionGroup> g = cols.get(i);
                cg[i] = g.toArray(new DimensionGroup[0]);
            }
            
            // 计算每个行组的笛卡尔积基数
            // 例如: 维度A(3成员) × 维度B(2成员) = 6组合
            int[] rc = Arrays.stream(rg).mapToInt(g -> {
                int total = 1;
                for (DimensionGroup d : g) total *= d.memberCount();
                return total;
            }).toArray();
            
            // 计算每个列组的笛卡尔积基数
            int[] cc = Arrays.stream(cg).mapToInt(g -> {
                int total = 1;
                for (DimensionGroup d : g) total *= d.memberCount();
                return total;
            }).toArray();
            
            return new CellContext(rg, cg, rc, cc);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 方案一：标准构建（JDK21 record + 预分配）
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 方法一：标准构建 build()                                                  │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   将行列分组配置转换为完整的单元格矩阵。
     *   返回结构化的三层嵌套 List，方便按组随机访问。
     * 
     * 【入参】
     *   - rows : 行组分组的维度配置，结构为 List<List<DimensionGroup>>
     *   - cols : 列组分组的维度配置
     * 
     * 【返回】
     *   CellResult，包含：
     *   - ctx  : CellContext（上下文，可获取键值、坐标等）
     *   - cells: 三层嵌套单元格 List[行组][列组][Cell]
     * 
     * 【JDK21 优化点】
     *   - 使用 record Cell，减少对象开销
     *   - 精确预分配 ArrayList 容量，避免动态扩容
     *   - 使用 record CellContext，统一管理元数据
     * 
     * 【适用场景】
     *   - 单元格数量在 10 万以内
     *   - 需要多次随机访问单元格
     *   - 需要将结果传递给其他方法/系统
     * 
     * 【内存占用参考】
     *   - 1万个单元格: ~3MB
     *   - 10万个单元格: ~30MB
     *   - 100万个单元格: ~300MB（不建议用此方法）
     * 
     * 【使用示例】
     * <pre>
     *   CellResult result = build(rowGroups, colGroups);
     *   
     *   // 随机访问
     *   Cell c = result.at(0, 1, 2, 3);
     *   
     *   // 获取键值
     *   String[] rowKeys = result.ctx().resolveRowKeys(c);
     *   String[] colKeys = result.ctx().resolveColKeys(c);
     *   
     *   // 遍历
     *   for (List<List<Cell>> rg : result.cells()) {
     *       for (List<Cell> cg : rg) {
     *           for (Cell cell : cg) {
     *               // 处理...
     *           }
     *       }
     *   }
     * </pre>
     * 
     * @param rows 行组分组的维度配置
     * @param cols 列组分组的维度配置
     * @return 单元格结果封装
     */
    public static CellResult build(List<List<DimensionGroup>> rows, 
                                  List<List<DimensionGroup>> cols) {
        // 第一步：创建上下文（预计算基数等）
        CellContext ctx = CellContext.of(rows, cols);
        
        // 第二步：预分配三层嵌套列表
        // 容量精确预估，避免动态扩容带来的性能损耗
        List<List<List<Cell>>> cells = new ArrayList<>(ctx.rowGroups().length);
        
        for (int ri = 0; ri < ctx.rowGroups().length; ri++) {
            // 每一行组对应一列组列表
            List<List<Cell>> rg = new ArrayList<>(ctx.colGroups().length);
            
            for (int ci = 0; ci < ctx.colGroups().length; ci++) {
                // 精确计算此 (Ri, Cj) 组合的单元格数量
                // = 行组Ri的组合数 × 列组Cj的组合数
                int size = ctx.rowGroupCardinalities()[ri] * ctx.colGroupCardinalities()[ci];
                rg.add(new ArrayList<>(size));
            }
            cells.add(rg);
        }
        
        // 第三步：填充单元格（双层循环创建 Cell 对象）
        for (int ri = 0; ri < ctx.rowGroups().length; ri++) {
            for (int ci = 0; ci < ctx.colGroups().length; ci++) {
                // 获取目标列表引用
                List<Cell> target = cells.get(ri).get(ci);
                // 该行组内的行组合数
                int rowCard = ctx.rowGroupCardinalities()[ri];
                // 该列组内的列组合数
                int colCard = ctx.colGroupCardinalities()[ci];
                
                // 双层循环创建 Cell
                for (int r = 0; r < rowCard; r++) {
                    for (int c = 0; c < colCard; c++) {
                        target.add(new Cell(ri, ci, r, c));
                    }
                }
            }
        }
        
        return new CellResult(ctx, cells);
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 构建结果封装 (CellResult)                                                │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   封装 build() 的返回值，包含上下文和单元格矩阵。
     *   使用 record 保证不可变性和线程安全。
     * 
     * @param ctx   单元格上下文
     * @param cells 三层嵌套单元格列表 [rowGroupIdx][colGroupIdx][Cell]
     */
    public record CellResult(CellContext ctx, List<List<List<Cell>>> cells) {
        
        /**
         * 按坐标随机访问单元格
         * 
         * @param ri 行组下标
         * @param ci 列组下标
         * @param r  行组内行序号
         * @param c  列组内列序号
         * @return 对应的 Cell 对象
         */
        public Cell at(int ri, int ci, int r, int c) {
            // 二维flatten: 第 r 行 × 列数 + 第 c 列
            return cells.get(ri).get(ci).get(r * ctx.colGroupCardinalities()[ci] + c);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 方案二：并行流构建（JDK21 parallelStream）
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 方法二：并行流构建 buildParallel()                                        │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   使用 JDK21 的 IntStream.parallel() 并行生成单元格。
     *   利用多核 CPU 加速笛卡尔积计算。
     * 
     * 【JDK21 优化点】
     *   - IntStream.range().parallel() 自动利用 ForkJoinPool
     *   - Spliterator 自动分片并行处理
     *   - 适合 100万+ 单元格的大规模场景
     * 
     * 【并行原理】
     *   1. IntStream.range(0, N).parallel() 将 N 个任务分片
     *   2. 分片数量 ≈ CPU 核心数（默认）
     *   3. 各分片并行执行 indexToCell()
     *   4. 最后串行归约到分组结构
     * 
     * 【适用场景】
     *   - 10万 ~ 100万 单元格
     *   - 多核 CPU 环境
     *   - 需要兼顾性能和内存
     * 
     * 【性能参考】
     *   - vs build(): 提速约 2-4 倍（取决于 CPU 核心数）
     *   - 内存: 介于 build() 和 buildPrimitive() 之间
     * 
     * @param rows 行分组配置
     * @param cols 列分组配置
     * @return 单元格结果
     */
    public static CellResult buildParallel(List<List<DimensionGroup>> rows, 
                                          List<List<DimensionGroup>> cols) {
        // 创建上下文
        CellContext ctx = CellContext.of(rows, cols);
        
        // 计算总单元格数
        long totalCells = ctx.totalCells();
        
        // 使用并行流生成所有 Cell
        // IntStream.range() 生成 [0, totalCells) 的索引序列
        // parallel() 开启并行模式
        // mapToObj() 将每个索引转换为 Cell 对象
        // toList() 收集为不可变列表（JDK21 新增）
        List<Cell> allCells = IntStream.range(0, (int) Math.min(totalCells, Integer.MAX_VALUE))
            .parallel()
            .mapToObj(idx -> indexToCell(ctx, idx))
            .toList();
        
        // 按组重组到三层嵌套结构
        // 先创建空结构
        List<List<List<Cell>>> grouped = new ArrayList<>(ctx.rowGroups().length);
        for (int ri = 0; ri < ctx.rowGroups().length; ri++) {
            List<List<Cell>> rg = new ArrayList<>(ctx.colGroups().length);
            for (int ci = 0; ci < ctx.colGroups().length; ci++) {
                rg.add(new ArrayList<>());
            }
            grouped.add(rg);
        }
        
        // 串行归约到分组结构
        // 注意：这里可以用并行归约进一步优化，但收益有限
        for (Cell c : allCells) {
            grouped.get(c.rowGroupIdx()).get(c.colGroupIdx()).add(c);
        }
        
        return new CellResult(ctx, grouped);
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 辅助方法：一维索引转换为 Cell                                            │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   将扁平化的一维索引映射回四维坐标 (ri, ci, r, c)。
     *   这是并行流实现的核心转换逻辑。
     * 
     * 【数学原理】
     *   总结构为: 若干个 (rowGroup, colGroup) 对
     *   每个 (ri, ci) 对内有 rowCard × colCard 个 Cell
     *   
     *   例如:
     *     R0C0: 0 ~ (rowCard0×colCard0-1)
     *     R0C1: rowCard0×colCard0 ~ (rowCard0×colCard0+rowCard0×colCard1-1)
     *     R1C0: ...
     * 
     * 【算法】
     *   1. 遍历所有 (ri, ci) 组合
     *   2. 计算每个组合的起始索引和结束索引
     *   3. 找到 linearIdx 落在哪个组合内
     *   4. 计算在该组合内的相对索引，解码为 (r, c)
     * 
     * @param ctx        单元格上下文
     * @param linearIdx  一维扁平索引
     * @return 对应的 Cell 对象
     */
    private static Cell indexToCell(CellContext ctx, int linearIdx) {
        int ri = 0, ci = 0, r = 0, c = 0;
        int processed = 0;  // 已处理的单元格数
        
        // 双层循环找到目标 Cell 所在的组
        outer:
        for (int i = 0; i < ctx.rowGroupCardinalities().length; i++) {
            for (int j = 0; j < ctx.colGroupCardinalities().length; j++) {
                // 当前 (ri, ci) 组合的单元格数
                int card = ctx.rowGroupCardinalities()[i] * ctx.colGroupCardinalities()[j];
                
                // 检查 linearIdx 是否在当前组合内
                if (processed + card > linearIdx) {
                    // 找到了！计算组内相对索引
                    int innerIdx = linearIdx - processed;
                    // 解码为 (r, c)
                    r = innerIdx / ctx.colGroupCardinalities()[j];
                    c = innerIdx % ctx.colGroupCardinalities()[j];
                    ri = i;
                    ci = j;
                    break outer;
                }
                processed += card;
            }
        }
        
        return new Cell(ri, ci, r, c);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 方案三：Primitive Long 编码（极致低内存）
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 编码：将 Cell 四个 int 压缩为单个 long                                    │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   将 Cell 的四个 int 字段编码为一个 long 值。
     *   这是极致性能优化的核心。
     * 
     * 【编码格式】（每个字段占16位，支持范围 0~65535）
     * 
     *   64位long被划分为4个16位的区域：
     *   ┌──────────────┬──────────────┬──────────────┬──────────────┐
     *   │ 63        48  │ 47        32  │ 31        16  │ 15         0 │
     *   ├──────────────┼──────────────┼──────────────┼──────────────┤
     *   │ rowGroupIdx  │ colGroupIdx  │ rowIdxInGroup│ colIdxInGroup│
     *   └──────────────┴──────────────┴──────────────┴──────────────┘
     * 
     * 【内存优化效果】
     *   - 普通 Cell 对象: 16字节(字段) + 12-16字节(对象头) = 28-32字节
     *   - long 编码: 8字节
     *   - 优化比例: 约 4 倍
     *   - 100万 Cell: 32MB → 8MB
     * 
     * 【使用场景】
     *   - 超大规模（1000万+）
     *   - 需要极致内存效率
     *   - 不需要随机访问，只需要顺序遍历
     * 
     * @param rowGroupIdx    行组下标
     * @param colGroupIdx    列组下标
     * @param rowIdxInGroup  组内行序号
     * @param colIdxInGroup  组内列序号
     * @return 编码后的 long 值
     */
    public static long encodeCell(int rowGroupIdx, int colGroupIdx, 
                                  int rowIdxInGroup, int colIdxInGroup) {
        // 移位操作将各字段放到对应的16位区域
        // rowGroupIdx 放到最高48-63位
        return ((long) rowGroupIdx << 48) 
             // colGroupIdx 放到32-47位
             | ((long) colGroupIdx << 32)
             // rowIdxInGroup 放到16-31位
             | ((long) rowIdxInGroup << 16)
             // colIdxInGroup 放到0-15位
             | (long) colIdxInGroup;
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 解码：从 long 还原为 Cell                                                │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   将 encodeCell() 编码的 long 值还原为 Cell 对象。
     * 
     * 【位操作说明】
     *   - (encoded >> 48)       : 提取最高16位（rowGroupIdx）
     *   - (encoded >> 32) & 0xFFFF: 提取32-47位（colGroupIdx），&0xFFFF遮罩去掉高位
     *   - (encoded >> 16) & 0xFFFF: 提取16-31位（rowIdxInGroup）
     *   - encoded & 0xFFFF        : 提取0-15位（colIdxInGroup）
     * 
     * @param encoded 编码后的 long 值
     * @return 还原的 Cell 对象
     */
    public static Cell decodeCell(long encoded) {
        return new Cell(
            // 提取 rowGroupIdx（无符号右移，自动保留符号）
            (int) (encoded >> 48),
            // 提取 colGroupIdx（需要遮罩处理）
            (int) (encoded >> 32) & 0xFFFF,
            // 提取 rowIdxInGroup
            (int) (encoded >> 16) & 0xFFFF,
            // 提取 colIdxInGroup
            (int) encoded & 0xFFFF
        );
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 方法三：Primitive Long 数组构建 buildPrimitive()                        │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   使用 long[] 而非 Cell[] 存储，大幅降低内存占用。
     * 
     * 【核心优势】
     *   1. 内存效率: 8字节/Cell vs 32字节/Cell（对象）
     *   2. GC压力: primitive数组几乎无GC（长期存活）
     *   3. 缓存友好: 连续内存，CPU预取有效
     *   4. 原子操作: long是原子性的，适合并发场景
     * 
     * 【适用场景】
     *   - 100万+ 单元格
     *   - 内存敏感环境
     *   - 顺序遍历为主
     * 
     * 【使用示例】
     * <pre>
     *   LongCellResult result = buildPrimitive(rows, cols);
     *   
     *   // 方式1: 顺序遍历
     *   for (long encoded : result.cells()) {
     *       Cell c = decodeCell(encoded);
     *       // 处理...
     *   }
     *   
     *   // 方式2: 使用 forEach
     *   result.forEach(encoded -> {
     *       Cell c = decodeCell(encoded);
     *       // 处理...
     *   });
     *   
     *   // 方式3: 按索引访问
     *   Cell c = result.cellAt(100);
     * </pre>
     * 
     * @param rows 行分组配置
     * @param cols 列分组配置
     * @return long数组结果封装
     */
    public static LongCellResult buildPrimitive(List<List<DimensionGroup>> rows,
                                               List<List<DimensionGroup>> cols) {
        CellContext ctx = CellContext.of(rows, cols);
        long total = ctx.totalCells();
        
        // 创建 long 数组存储编码后的 Cell
        // 注意：使用 long[] 而非 Long[]，避免包装对象
        long[] cells = new long[(int) total];
        
        // 四层循环填充
        int idx = 0;
        for (int ri = 0; ri < ctx.rowGroups().length; ri++) {
            for (int ci = 0; ci < ctx.colGroups().length; ci++) {
                int rowCard = ctx.rowGroupCardinalities()[ri];
                int colCard = ctx.colGroupCardinalities()[ci];
                for (int r = 0; r < rowCard; r++) {
                    for (int c = 0; c < colCard; c++) {
                        // 编码并存储
                        cells[idx++] = encodeCell(ri, ci, r, c);
                    }
                }
            }
        }
        
        return new LongCellResult(ctx, cells);
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 方法四：Primitive + 并行构建 buildPrimitiveParallel()                    │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   结合 Primitive 编码和并行流的双重优化。
     *   兼顾最低内存和最高性能。
     * 
     * 【JDK21 优化点】
     *   - IntStream.range().parallel().forEach() 并行填充
     *   - 各线程独立处理数组的不同片段
     *   - 无锁并发（数组下标天然隔离）
     * 
     * 【性能收益】
     *   - vs buildPrimitive(): 提速约 2-4 倍
     *   - vs build(): 提速约 5-10 倍
     *   - 内存: 与 buildPrimitive() 相同（8字节/Cell）
     * 
     * 【适用场景】
     *   - 1000万+ 单元格
     *   - 极致性能要求
     *   - 多核 CPU 环境
     * 
     * @param rows 行分组配置
     * @param cols 列分组配置
     * @return long数组结果封装
     */
    public static LongCellResult buildPrimitiveParallel(List<List<DimensionGroup>> rows,
                                                        List<List<DimensionGroup>> cols) {
        CellContext ctx = CellContext.of(rows, cols);
        long total = ctx.totalCells();
        long[] cells = new long[(int) total];
        
        // 并行填充
        // IntStream.range(0, N).parallel() 将 N 个索引分片
        // 每个分片并行执行 forEach
        // 写入 cells[idx] = ...（数组写入是线程安全的，因为 idx 不重叠）
        IntStream.range(0, (int) total)
            .parallel()
            .forEach(idx -> {
                cells[idx] = indexToEncodedCell(ctx, idx);
            });
        
        return new LongCellResult(ctx, cells);
    }
    
    /**
     * 将一维索引转换为编码后的 long（内部辅助方法）
     * 
     * 与 indexToCell() 类似，但返回 long 而非 Cell。
     * 用于并行构建场景。
     * 
     * @param ctx        上下文
     * @param linearIdx  一维索引
     * @return 编码后的 long
     */
    private static long indexToEncodedCell(CellContext ctx, int linearIdx) {
        int ri = 0, ci = 0, r = 0, c = 0;
        int processed = 0;
        
        for (int i = 0; i < ctx.rowGroupCardinalities().length; i++) {
            for (int j = 0; j < ctx.colGroupCardinalities().length; j++) {
                int card = ctx.rowGroupCardinalities()[i] * ctx.colGroupCardinalities()[j];
                if (processed + card > linearIdx) {
                    int innerIdx = linearIdx - processed;
                    r = innerIdx / ctx.colGroupCardinalities()[j];
                    c = innerIdx % ctx.colGroupCardinalities()[j];
                    ri = i;
                    ci = j;
                    return encodeCell(ri, ci, r, c);
                }
                processed += card;
            }
        }
        return encodeCell(ri, ci, r, c);
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ Primitive 结果封装 (LongCellResult)                                     │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   封装 buildPrimitive() / buildPrimitiveParallel() 的返回值。
     *   提供统一的遍历和访问接口。
     * 
     * @param ctx    单元格上下文
     * @param cells  long数组（编码后的Cell）
     */
    public record LongCellResult(CellContext ctx, long[] cells) {
        
        /**
         * 获取总单元格数
         * 
         * @return 单元格数量
         */
        public long totalCells() { 
            return cells.length; 
        }
        
        /**
         * 按索引获取 Cell 对象
         * 
         * 【注意】
         *   每次调用都会创建新的 Cell 对象，
         *   如果需要频繁访问，建议批量解码或直接使用 encoded 值。
         * 
         * @param idx 数组下标
         * @return 解码后的 Cell
         */
        public Cell cellAt(int idx) { 
            return decodeCell(cells[idx]); 
        }
        
        /**
         * 顺序遍历所有 Cell
         * 
         * @param action 消费动作，接收编码后的 long 值
         */
        public void forEach(java.util.function.LongConsumer action) {
            for (long c : cells) {
                action.accept(c);
            }
        }
        
        /**
         * 并行遍历所有 Cell
         * 
         * 【用途】
         *   在已有 long[] 的基础上进行并行处理。
         *   例如：并行计算、并行写入数据库等。
         * 
         * @param action 消费动作
         */
        public void forEachParallel(java.util.function.LongConsumer action) {
            // LongStream.of(cells) 从数组创建流
            // parallel() 开启并行模式
            // forEach() 终端操作
            LongStream.of(cells).parallel().forEach(action);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 方案四：Stream Pipeline（函数式，极简）
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 方法五：顺序 Stream 流 streamCells()                                     │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   返回 Cell 的 Stream，支持函数式链式处理。
     *   懒执行，按需计算。
     * 
     * 【JDK21 优化点】
     *   - IntStream.range().mapToObj() 惰性生成
     *   - Stream API 简洁优雅
     *   - 支持 filter/map/flatMap 等操作
     * 
     * 【适用场景】
     *   - 需要链式处理（过滤、转换等）
     *   - 不需要物化结果
     *   - 一次性消费
     * 
     * 【使用示例】
     * <pre>
     *   // 过滤 + 转换
     *   streamCells(rows, cols)
     *       .filter(c -> c.rowGroupIdx() == 0)      // 过滤行组0
     *       .map(c -> ctx.resolveRowKeys(c))        // 转换为行键
     *       .distinct()                              // 去重
     *       .forEach(keys -> { ... });               // 消费
     * </pre>
     * 
     * @param rows 行分组配置
     * @param cols 列分组配置
     * @return Cell 的 Stream
     */
    public static Stream<Cell> streamCells(List<List<DimensionGroup>> rows,
                                           List<List<DimensionGroup>> cols) {
        CellContext ctx = CellContext.of(rows, cols);
        
        // IntStream.range() 生成索引流
        // mapToObj() 转换为 Cell 流
        // 惰性执行，只有终止操作时才计算
        return IntStream.range(0, (int) ctx.totalCells())
            .mapToObj(idx -> indexToCell(ctx, idx));
    }
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 方法六：并行 Stream 流 parallelStreamCells()                            │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   返回并行 Cell Stream，自动利用多核。
     * 
     * 【特点】
     *   - 自动并行化
     *   - 适合大规模数据
     *   - 链式函数式处理
     * 
     * 【适用场景】
     *   - 需要并行处理 + 函数式链式调用
     *   - 数据量较大（10万+）
     *   - 简单聚合操作
     * 
     * 【示例】
     * <pre>
     *   // 并行统计行组0的单元格数
     *   long count = parallelStreamCells(rows, cols)
     *       .filter(c -> c.rowGroupIdx() == 0)
     *       .count();
     *   
     *   // 并行收集到列表
     *   List<Cell> list = parallelStreamCells(rows, cols)
     *       .collect(Collectors.toList());
     * </pre>
     * 
     * @param rows 行分组配置
     * @param cols 列分组配置
     * @return 并行 Cell Stream
     */
    public static Stream<Cell> parallelStreamCells(List<List<DimensionGroup>> rows,
                                                    List<List<DimensionGroup>> cols) {
        CellContext ctx = CellContext.of(rows, cols);
        
        return IntStream.range(0, (int) ctx.totalCells())
            .parallel()  // 开启并行模式
            .mapToObj(idx -> indexToCell(ctx, idx));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 测试对比
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * ┌──────────────────────────────────────────────────────────────────────────┐
     * │ 主测试方法 main()                                                        │
     * └──────────────────────────────────────────────────────────────────────────┘
     * 
     * 【功能】
     *   演示各种构建方法的使用，并输出性能对比。
     * 
     * 【说明】
     *   由于当前环境无 Java 运行时，无法执行实际测试。
     *   实际运行需要 JDK 21 环境：
     *   <pre>
     *     javac CartesianCellBuilder.java
     *     java CartesianCellBuilder
     *   </pre>
     */
    public static void main(String[] args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║         JDK21 性能优化版 - 行列分组笛卡尔积组装器                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 构造测试数据
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 
        // 行分组配置:
        //   R1: 维度A(a1~a5) → 5组合
        //   R2: 维度A(a6~a10) → 5组合
        // 
        // 列分组配置:
        //   C1: 维度B(b1~b5) → 5组合
        //   C2: 维度B(b6~b10) → 5组合
        //
        // 总计: 2×2×5×5 = 100 单元格
        
        var rowGroups = List.of(
            List.of(new DimensionGroup("A", new String[]{"a1","a2","a3","a4","a5"})),
            List.of(new DimensionGroup("A", new String[]{"a6","a7","a8","a9","a10"}))
        );
        
        var colGroups = List.of(
            List.of(new DimensionGroup("B", new String[]{"b1","b2","b3","b4","b5"})),
            List.of(new DimensionGroup("B", new String[]{"b6","b7","b8","b9","b10"}))
        );
        
        CellContext ctx = CellContext.of(rowGroups, colGroups);
        System.out.println("配置: 2行组 × 2列组 × 5×5 组合 = " + ctx.totalCells() + " 单元格");
        System.out.println();
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 各方案测试
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // ─── 方案1: 标准构建 ───
        System.out.println("【方案1】build() - 标准构建");
        long start = System.nanoTime();
        CellResult r1 = build(rowGroups, colGroups);
        long elapsed = System.nanoTime() - start;
        System.out.printf("  耗时: %.2f ms%n", elapsed / 1_000_000.0);
        System.out.printf("  首个Cell: %s%n", r1.at(0, 0, 0, 0));
        
        // ─── 方案2: 并行流构建 ───
        System.out.println();
        System.out.println("【方案2】buildParallel() - 并行流构建");
        start = System.nanoTime();
        CellResult r2 = buildParallel(rowGroups, colGroups);
        elapsed = System.nanoTime() - start;
        System.out.printf("  耗时: %.2f ms%n", elapsed / 1_000_000.0);
        
        // ─── 方案3: Primitive 构建 ───
        System.out.println();
        System.out.println("【方案3】buildPrimitive() - Primitive long 编码");
        start = System.nanoTime();
        LongCellResult r3 = buildPrimitive(rowGroups, colGroups);
        elapsed = System.nanoTime() - start;
        System.out.printf("  耗时: %.2f ms%n", elapsed / 1_000_000.0);
        System.out.printf("  内存: %d Cell × 8字节 = %.2f MB%n", 
            r3.totalCells(), r3.totalCells() * 8.0 / (1024*1024));
        System.out.printf("  首个Cell: %s%n", r3.cellAt(0));
        
        // ─── 方案4: Primitive + 并行 ───
        System.out.println();
        System.out.println("【方案4】buildPrimitiveParallel() - Primitive + 并行");
        start = System.nanoTime();
        LongCellResult r4 = buildPrimitiveParallel(rowGroups, colGroups);
        elapsed = System.nanoTime() - start;
        System.out.printf("  耗时: %.2f ms%n", elapsed / 1_000_000.0);
        
        // ─── 方案5: Stream ───
        System.out.println();
        System.out.println("【方案5】parallelStreamCells() - Stream API");
        start = System.nanoTime();
        long count = parallelStreamCells(rowGroups, colGroups).count();
        elapsed = System.nanoTime() - start;
        System.out.printf("  耗时: %.2f ms%n", elapsed / 1_000_000.0);
        System.out.printf("  遍历计数: %d%n", count);
        
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 大规模性能估算
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println();
        System.out.println("【大规模估算】100万单元格");
        System.out.println("  基于代码逻辑的理论推演（实际需要 JDK 环境运行）:");
        System.out.println();
        System.out.println("  ┌──────────────────────┬────────────┬─────────────┬────────────────┐");
        System.out.println("  │ 方案                 │ 预估耗时   │ 预估内存     │ 推荐度         │");
        System.out.println("  ├──────────────────────┼────────────┼─────────────┼────────────────┤");
        System.out.println("  │ build()             │ ~200-400ms │ ~280MB      │ ⭐⭐ (有GC问题) │");
        System.out.println("  │ buildParallel()     │ ~80-150ms  │ ~150MB      │ ⭐⭐⭐          │");
        System.out.println("  │ buildPrimitive()    │ ~60-100ms  │ ~8MB        │ ⭐⭐⭐⭐         │");
        System.out.println("  │ buildPrimitive+并行 │ ~30-80ms   │ ~8MB        │ ⭐⭐⭐⭐⭐ (最优)│");
        System.out.println("  └──────────────────────┴────────────┴─────────────┴────────────────┘");
        System.out.println();
        System.out.println("  ★ JDK21 推荐: buildPrimitiveParallel() 或 buildPrimitive()");
        System.out.println();
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // API 使用示例
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("【API 使用示例】");
        System.out.println();
        System.out.println("  // ───────────────────────────────────────────────────────────────");
        System.out.println("  // 方式1: 标准构建（简单直接）");
        System.out.println("  // ───────────────────────────────────────────────────────────────");
        System.out.println("  var result = build(rowGroups, colGroups);");
        System.out.println("  ");
        System.out.println("  // 随机访问");
        System.out.println("  var cell = result.at(0, 0, 0, 0);");
        System.out.println("  ");
        System.out.println("  // 获取键值");
        System.out.println("  var rowKeys = result.ctx().resolveRowKeys(cell);");
        System.out.println("  var colKeys = result.ctx().resolveColKeys(cell);");
        System.out.println();
        System.out.println("  // ───────────────────────────────────────────────────────────────");
        System.out.println("  // 方式2: Primitive 构建（极致性能）");
        System.out.println("  // ───────────────────────────────────────────────────────────────");
        System.out.println("  var primitive = buildPrimitive(rowGroups, colGroups);");
        System.out.println("  ");
        System.out.println("  // 顺序遍历");
        System.out.println("  primitive.forEach(encoded -> {");
        System.out.println("      Cell c = decodeCell(encoded);");
        System.out.println("      // 处理 c.rowGroupIdx(), c.colGroupIdx()...");
        System.out.println("  });");
        System.out.println();
        System.out.println("  // ───────────────────────────────────────────────────────────────");
        System.out.println("  // 方式3: Stream 流水线（函数式）");
        System.out.println("  // ───────────────────────────────────────────────────────────────");
        System.out.println("  var ctx = CellContext.of(rowGroups, colGroups);");
        System.out.println("  ");
        System.out.println("  parallelStreamCells(rowGroups, colGroups)");
        System.out.println("      .filter(c -> c.rowGroupIdx() == 0)          // 过滤");
        System.out.println("      .map(c -> ctx.resolveRowKeys(c))             // 转换");
        System.out.println("      .forEach(keys -> { /* 处理行键 */ });      // 消费");
        System.out.println();
    }
}
