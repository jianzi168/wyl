package formula;

/**
 * 表单位置 {@code int[]} 与回显结果 Map 的字符串 key 之间的转换。
 *
 * <p>回显输出约定：{@code Map<String, String>} 的 key 为物理位置，如 {@code "3,4"} 表示行 3 列 4
 *（具体含义由业务定义，本类只做逗号拼接，不解释坐标语义）。
 *
 * <p>BI 表单多为二维 {@code int[]{row, col}}，{@link #format(int[])} 对长度 2 走快速路径，
 * 避免创建 {@link StringBuilder}。
 */
public final class TablePositionKey {

    private TablePositionKey() {
    }

    /**
     * 将表单位置格式化为回显 Map 的 key。
     *
     * @param position 业务传入的位置数组，常见为 {@code [row, col]}，也可更高维
     * @return 逗号拼接，如 {@code "3,4"}；空数组返回 {@code ""}
     */
    public static String format(int[] position) {
        if (position == null || position.length == 0) {
            return "";
        }
        // 绝大多数表单为行列二维，直接拼接两个 int，零对象分配
        if (position.length == 2) {
            return position[0] + "," + position[1];
        }
        return formatSlow(position);
    }

    private static String formatSlow(int[] position) {
        StringBuilder sb = new StringBuilder(position.length * 4);
        appendInto(sb, position);
        return sb.toString();
    }

    /**
     * 追加到已有 {@link StringBuilder}，便于将来在 Session 内复用 builder 批量写入。
     */
    public static void appendInto(StringBuilder sb, int[] position) {
        for (int i = 0; i < position.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(position[i]);
        }
    }
}
